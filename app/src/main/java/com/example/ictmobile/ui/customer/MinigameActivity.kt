package com.example.ictmobile.ui.customer

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.ictmobile.R
import com.example.ictmobile.databinding.ActivityMinigameBinding
import com.example.ictmobile.services.FirebaseService
import kotlin.random.Random

class MinigameActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMinigameBinding
    private val firebaseService = FirebaseService.getInstance()
    private var availableTokens = 0
    private var isFreeplay = false
    private var gameStarted = false
    private var gameEnded = false
    
    // Card data
    private val suits = listOf("clover", "diamond", "love", "spade")
    private val ranks = listOf("A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K")
    private val deck = mutableListOf<Card>()
    private val dealerHand = mutableListOf<Card>()
    private val playerHand = mutableListOf<Card>()
    
    data class Card(val rank: String, val suit: String) {
        val value: Int
            get() = when (rank) {
                "A" -> 11 // Will be adjusted in score calculation
                "J", "Q", "K" -> 10
                else -> rank.toIntOrNull() ?: 0
            }
        
        val imageName: String
            get() {
                // For numbered cards (2-10), use card_ prefix
                return if (rank.matches(Regex("[0-9]+"))) {
                    "card_${rank}_${suit}.png"
                } else {
                    val rankName = when (rank) {
                        "A" -> "Ace"
                        "J" -> "jack"
                        "Q" -> "queen"
                        "K" -> "king"
                        else -> rank
                    }
                    "${rankName.lowercase()}_${suit}.png"
                }
            }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMinigameBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        binding.btnBack.setOnClickListener {
            finish()
        }
        
        loadTokenCount()
        loadUserProfilePicture()
        setupClickListeners()
    }
    
    private fun loadUserProfilePicture() {
        val currentUser = firebaseService.getCurrentUser()
        if (currentUser != null) {
            firebaseService.getCurrentUserData()
                .addOnSuccessListener { user ->
                    try {
                        val pictureName = user.profilePicture.ifEmpty { "king.png" }
                        val inputStream = assets.open("profilepictures/$pictureName")
                        binding.ivPlayerAvatar.setImageBitmap(android.graphics.BitmapFactory.decodeStream(inputStream))
                        inputStream.close()
                    } catch (e: Exception) {
                        // Use default if not found
                        try {
                            val defaultStream = assets.open("profilepictures/king.png")
                            binding.ivPlayerAvatar.setImageBitmap(android.graphics.BitmapFactory.decodeStream(defaultStream))
                            defaultStream.close()
                        } catch (e2: Exception) {
                            android.util.Log.e("Minigame", "Failed to load profile picture: ${e2.message}")
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    android.util.Log.e("Minigame", "Failed to load user data: ${exception.message}")
                    // Load default picture
                    try {
                        val defaultStream = assets.open("profilepictures/king.png")
                        binding.ivPlayerAvatar.setImageBitmap(android.graphics.BitmapFactory.decodeStream(defaultStream))
                        defaultStream.close()
                    } catch (e: Exception) {
                        // Ignore
                    }
                }
        }
    }
    
    private fun loadTokenCount() {
        val currentUser = firebaseService.getCurrentUser()
        if (currentUser != null) {
            firebaseService.getAvailableTokensCount(currentUser.uid)
                .addOnSuccessListener { count ->
                    availableTokens = count
                    binding.tvTokenCount.text = "Tokens Available: $count"
                }
        }
    }
    
    private fun setupClickListeners() {
        binding.btnPlayWithToken.setOnClickListener { startGame(false) }
        binding.btnFreeplay.setOnClickListener { startGame(true) }
        binding.btnHit.setOnClickListener { hit() }
        binding.btnStand.setOnClickListener { stand() }
        binding.btnPlayAgain.setOnClickListener { resetGame() }
    }
    
    private fun createDeck() {
        deck.clear()
        for (rank in ranks) {
            for (suit in suits) {
                deck.add(Card(rank, suit))
            }
        }
        deck.shuffle()
    }
    
    private fun dealCard(): Card {
        if (deck.isEmpty()) {
            createDeck()
        }
        return deck.removeAt(deck.size - 1)
    }
    
    private fun calculateScore(hand: List<Card>): Int {
        var score = 0
        var aces = 0
        
        for (card in hand) {
            if (card.rank == "A") {
                aces++
                score += 11
            } else {
                score += card.value
            }
        }
        
        // Adjust for aces
        while (score > 21 && aces > 0) {
            score -= 10
            aces--
        }
        
        return score
    }
    
    private fun getCardResource(card: Card): Int {
        // Remove .png extension and use the image name directly
        val resourceName = card.imageName.replace(".png", "")
        return resources.getIdentifier(resourceName, "drawable", packageName)
    }
    
    private fun displayCard(container: android.widget.LinearLayout, card: Card, hidden: Boolean) {
        val imageView = android.widget.ImageView(this)
        // Make cards bigger - 120dp instead of app_icon_size
        val cardSize = (120 * resources.displayMetrics.density).toInt()
        val layoutParams = android.widget.LinearLayout.LayoutParams(
            cardSize,
            cardSize
        ).apply {
            marginEnd = 12
        }
        imageView.layoutParams = layoutParams
        imageView.scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
        
        if (hidden) {
            imageView.setImageResource(R.drawable.cardback)
        } else {
            val resId = getCardResource(card)
            if (resId != 0) {
                imageView.setImageResource(resId)
            } else {
                imageView.setImageResource(R.drawable.cardback)
            }
        }
        
        container.addView(imageView)
    }
    
    private fun updateDisplay() {
        val playerScore = calculateScore(playerHand)
        binding.tvPlayerScore.text = "Your Score: $playerScore"
        
        if (gameEnded) {
            val dealerScore = calculateScore(dealerHand)
            binding.tvDealerScore.text = "Dealer Score: $dealerScore"
            // Reveal dealer's hidden card
            if (binding.llDealerHand.childCount > 0) {
                val firstCardView = binding.llDealerHand.getChildAt(0) as? android.widget.ImageView
                if (firstCardView != null && dealerHand.isNotEmpty()) {
                    val resId = getCardResource(dealerHand[0])
                    if (resId != 0) {
                        firstCardView.setImageResource(resId)
                    }
                }
            }
        } else {
            // Show only first card value for dealer
            val visibleScore = if (dealerHand.isNotEmpty()) calculateScore(listOf(dealerHand[0])) else 0
            binding.tvDealerScore.text = "Dealer Score: $visibleScore+"
        }
    }
    
    private fun startGame(freeplay: Boolean) {
        isFreeplay = freeplay
        
        if (!freeplay && availableTokens < 1) {
            Toast.makeText(this, "You do not have any tokens to play. Book a machine to earn tokens!", Toast.LENGTH_SHORT).show()
            return
        }
        
        gameStarted = true
        gameEnded = false
        createDeck()
        dealerHand.clear()
        playerHand.clear()
        
        // Clear hands
        binding.llDealerHand.removeAllViews()
        binding.llPlayerHand.removeAllViews()
        
        // Deal initial cards
        playerHand.add(dealCard())
        displayCard(binding.llPlayerHand, playerHand.last(), false)
        
        dealerHand.add(dealCard())
        displayCard(binding.llDealerHand, dealerHand.last(), true) // Hidden
        
        playerHand.add(dealCard())
        displayCard(binding.llPlayerHand, playerHand.last(), false)
        
        dealerHand.add(dealCard())
        displayCard(binding.llDealerHand, dealerHand.last(), false)
        
        updateDisplay()
        
        binding.startScreen.visibility = android.view.View.GONE
        binding.gameScreen.visibility = android.view.View.VISIBLE
        binding.resultScreen.visibility = android.view.View.GONE
        
        // Check for blackjack
        if (calculateScore(playerHand) == 21) {
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                stand()
            }, 1000)
        }
    }
    
    private fun hit() {
        if (!gameStarted || gameEnded) return
        
        val newCard = dealCard()
        playerHand.add(newCard)
        displayCard(binding.llPlayerHand, newCard, false)
        updateDisplay()
        
        val score = calculateScore(playerHand)
        if (score > 21) {
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                endGame("lose")
            }, 500)
        }
    }
    
    private fun stand() {
        if (!gameStarted || gameEnded) return
        
        gameEnded = true
        updateDisplay()
        
        // Dealer draws until 17 or higher
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            drawDealerCards()
        }, 500)
    }
    
    private fun drawDealerCards() {
        var dealerScore = calculateScore(dealerHand)
        
        while (dealerScore < 17) {
            val newCard = dealCard()
            dealerHand.add(newCard)
            displayCard(binding.llDealerHand, newCard, false)
            dealerScore = calculateScore(dealerHand)
            updateDisplay()
        }
        
        val playerScore = calculateScore(playerHand)
        val finalDealerScore = calculateScore(dealerHand)
        
        val result = when {
            finalDealerScore > 21 -> "win"
            playerScore > finalDealerScore -> "win"
            playerScore < finalDealerScore -> "lose"
            else -> "push"
        }
        
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            endGame(result)
        }, 500)
    }
    
    private fun endGame(result: String) {
        gameEnded = true
        binding.gameScreen.visibility = android.view.View.GONE
        binding.resultScreen.visibility = android.view.View.VISIBLE
        
        val message = when (result) {
            "win" -> if (isFreeplay) "You won! (Freeplay mode - no voucher awarded)" else "Congratulations! You won! A RM5 off voucher has been added to your account."
            "lose" -> "You lost. Better luck next time!"
            else -> "It's a tie! Better luck next time!"
        }
        
        binding.tvResultTitle.text = when (result) {
            "win" -> "YOU WIN!"
            "lose" -> "YOU LOSE"
            else -> "PUSH"
        }
        binding.tvResultMessage.text = message
        
        // Submit game result to Firebase
        if (!isFreeplay) {
            submitGameResult(result)
        } else {
            loadTokenCount() // Refresh token count
        }
    }
    
    private fun submitGameResult(result: String) {
        val currentUser = firebaseService.getCurrentUser()
        if (currentUser == null) return
        
        val dealerHandData = dealerHand.map { mapOf("rank" to it.rank, "suit" to it.suit) }
        val playerHandData = playerHand.map { mapOf("rank" to it.rank, "suit" to it.suit) }
        val playerScore = calculateScore(playerHand)
        val dealerScore = calculateScore(dealerHand)
        
        // Use token first
        firebaseService.useToken(currentUser.uid)
            .addOnSuccessListener {
                // Token used, now submit result
                // Note: FirebaseService needs a playMinigame method
                // For now, we'll just use the token and create voucher if win
                if (result == "win") {
                    firebaseService.createVoucher(currentUser.uid, "rm5_off")
                        .addOnSuccessListener {
                            loadTokenCount()
                        }
                } else {
                    loadTokenCount()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Failed to process game: ${exception.message}", Toast.LENGTH_SHORT).show()
                loadTokenCount()
            }
    }
    
    private fun resetGame() {
        gameStarted = false
        gameEnded = false
        dealerHand.clear()
        playerHand.clear()
        binding.llDealerHand.removeAllViews()
        binding.llPlayerHand.removeAllViews()
        
        binding.startScreen.visibility = android.view.View.VISIBLE
        binding.gameScreen.visibility = android.view.View.GONE
        binding.resultScreen.visibility = android.view.View.GONE
    }
}
