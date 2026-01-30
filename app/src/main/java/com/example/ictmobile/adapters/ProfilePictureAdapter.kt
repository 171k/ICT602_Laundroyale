package com.example.ictmobile.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.ictmobile.R
import java.io.IOException

class ProfilePictureAdapter(
    private val pictures: List<String>,
    private val onPictureClick: (String) -> Unit
) : RecyclerView.Adapter<ProfilePictureAdapter.PictureViewHolder>() {

    class PictureViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.ivProfilePicture)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PictureViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_profile_picture, parent, false)
        return PictureViewHolder(view)
    }

    override fun onBindViewHolder(holder: PictureViewHolder, position: Int) {
        val pictureName = pictures[position]
        
        try {
            val inputStream = holder.itemView.context.assets.open("profilepictures/$pictureName")
            holder.imageView.setImageBitmap(android.graphics.BitmapFactory.decodeStream(inputStream))
            inputStream.close()
        } catch (e: IOException) {
            // Use default if not found
            try {
                val defaultStream = holder.itemView.context.assets.open("profilepictures/king.png")
                holder.imageView.setImageBitmap(android.graphics.BitmapFactory.decodeStream(defaultStream))
                defaultStream.close()
            } catch (e2: Exception) {
                // Ignore
            }
        }
        
        holder.itemView.setOnClickListener {
            onPictureClick(pictureName)
        }
    }

    override fun getItemCount() = pictures.size
}
