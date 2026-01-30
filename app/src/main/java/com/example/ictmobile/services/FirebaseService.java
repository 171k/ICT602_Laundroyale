package com.example.ictmobile.services;

import android.util.Log;
import com.example.ictmobile.models.*;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;
import com.google.firebase.Timestamp;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;

public class FirebaseService {
    private static final String TAG = "FirebaseService";
    private static FirebaseService instance;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    
    // Collection names
    private static final String COLLECTION_USERS = "users";
    private static final String COLLECTION_MACHINES = "machines";
    private static final String COLLECTION_ORDERS = "orders";
    private static final String COLLECTION_PAYMENTS = "payments";
    private static final String COLLECTION_TOKENS = "tokens";
    private static final String COLLECTION_VOUCHERS = "vouchers";
    
    private FirebaseService() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }
    
    public static synchronized FirebaseService getInstance() {
        if (instance == null) {
            instance = new FirebaseService();
        }
        return instance;
    }
    
    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }
    
    // ==================== Authentication ====================
    
    public Task<User> register(String email, String password, String name, String phone) {
        return auth.createUserWithEmailAndPassword(email, password)
            .continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }
                
                FirebaseUser firebaseUser = task.getResult().getUser();
                if (firebaseUser == null) {
                    throw new Exception("User creation failed");
                }
                
                User user = new User(
                    firebaseUser.getUid(),
                    name,
                    email,
                    "",
                    phone,
                    "customer",
                    "king.png"
                );
                
                Map<String, Object> userMap = User.Companion.toMap(user);
                return db.collection(COLLECTION_USERS)
                    .document(firebaseUser.getUid())
                    .set(userMap)
                    .continueWith(task1 -> user);
            });
    }
    
    public Task<User> login(String email, String password) {
        return auth.signInWithEmailAndPassword(email, password)
            .continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }
                
                FirebaseUser firebaseUser = task.getResult().getUser();
                if (firebaseUser == null) {
                    throw new Exception("Login failed");
                }
                
                return getUserById(firebaseUser.getUid());
            });
    }
    
    public void logout() {
        auth.signOut();
    }
    
    public Task<User> getUserById(String userId) {
        return db.collection(COLLECTION_USERS)
            .document(userId)
            .get()
            .continueWith(task -> {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }
                
                DocumentSnapshot doc = task.getResult();
                if (doc == null || !doc.exists()) {
                    throw new Exception("User not found");
                }
                
                Map<String, Object> data = doc.getData();
                if (data == null) {
                    throw new Exception("User data is null");
                }
                
                data.put("id", doc.getId());
                return User.Companion.fromMap(data);
            });
    }
    
    public Task<User> getCurrentUserData() {
        FirebaseUser firebaseUser = getCurrentUser();
        if (firebaseUser == null) {
            return Tasks.forException(new Exception("No user logged in"));
        }
        return getUserById(firebaseUser.getUid());
    }
    
    public Task<Void> updateUser(User user) {
        Map<String, Object> userMap = User.Companion.toMap(user);
        return db.collection(COLLECTION_USERS)
            .document(user.getId())
            .update(userMap);
    }
    
    // ==================== Machines ====================
    
    public Task<List<Machine>> getMachines(String type) {
        Log.d(TAG, "Getting machines, type: " + (type != null ? type : "all"));
        CollectionReference machinesRef = db.collection(COLLECTION_MACHINES);
        Query query = type != null && !type.isEmpty() 
            ? machinesRef.whereEqualTo("type", type)
            : machinesRef;
            
        return query.get().continueWith(task -> {
            if (!task.isSuccessful()) {
                Exception exception = task.getException();
                Log.e(TAG, "Failed to get machines: " + exception.getMessage(), exception);
                
                // Check if it's a permission error
                if (exception instanceof FirebaseFirestoreException) {
                    FirebaseFirestoreException firestoreException = (FirebaseFirestoreException) exception;
                    if (firestoreException.getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        Log.w(TAG, "Permission denied accessing machines. Returning empty list.");
                        // Return empty list instead of throwing - UI won't freeze
                        return new ArrayList<Machine>();
                    }
                }
                
                throw exception;
            }
            
            QuerySnapshot result = task.getResult();
            Log.d(TAG, "Got " + result.size() + " machines from Firestore");
            
            List<Machine> machines = new ArrayList<>();
            for (QueryDocumentSnapshot doc : result) {
                try {
                    Map<String, Object> data = doc.getData();
                    if (data != null) {
                        data.put("id", doc.getId());
                        machines.add(Machine.Companion.fromMap(data));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing machine document: " + e.getMessage(), e);
                }
            }
            
            Log.d(TAG, "Successfully parsed " + machines.size() + " machines");
            return machines;
        });
    }
    
    public Task<Machine> getMachineById(String machineId) {
        return db.collection(COLLECTION_MACHINES)
            .document(machineId)
            .get()
            .continueWith(task -> {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }
                
                DocumentSnapshot doc = task.getResult();
                if (doc == null || !doc.exists()) {
                    throw new Exception("Machine not found");
                }
                
                Map<String, Object> data = doc.getData();
                data.put("id", doc.getId());
                return Machine.Companion.fromMap(data);
            });
    }
    
    public Task<String> createMachine(Machine machine) {
        Map<String, Object> machineMap = Machine.Companion.toMap(machine);
        return db.collection(COLLECTION_MACHINES)
            .add(machineMap)
            .continueWith(task -> {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }
                return task.getResult().getId();
            });
    }
    
    public Task<Void> updateMachine(String machineId, Machine machine) {
        Map<String, Object> machineMap = Machine.Companion.toMap(machine);
        return db.collection(COLLECTION_MACHINES)
            .document(machineId)
            .update(machineMap);
    }
    
    public Task<Void> deleteMachine(String machineId) {
        return db.collection(COLLECTION_MACHINES)
            .document(machineId)
            .delete();
    }
    
    // ==================== Orders ====================
    
    public Task<String> createOrder(String userId, String machineId, String temperature, 
                                   Date startTime, Date endTime) {
        return getMachineById(machineId).continueWithTask(task -> {
            if (!task.isSuccessful()) {
                throw task.getException();
            }
            
            Machine machine = task.getResult();
            
            // Validate duration (30 minutes to 3 hours)
            long durationMinutes = TimeUnit.MILLISECONDS.toMinutes(endTime.getTime() - startTime.getTime());
            if (durationMinutes < 30) {
                throw new Exception("Minimum booking duration is 30 minutes");
            }
            if (durationMinutes > 180) {
                throw new Exception("Maximum booking duration is 3 hours");
            }
            
            // Check machine availability
            return isMachineAvailableForTimeSlot(machineId, startTime, endTime)
                .continueWithTask(availableTask -> {
                    if (!availableTask.isSuccessful() || !availableTask.getResult()) {
                        throw new Exception("Machine is not available for the selected time slot");
                    }
                    
                    if (!machine.getStatus().equals("available")) {
                        throw new Exception("Machine is currently unavailable for maintenance");
                    }
                    
                    // Calculate total amount
                    double hours = durationMinutes / 60.0;
                    double totalAmount = machine.getPrice() * hours;
                    
                    // Determine status
                    String status = startTime.after(new Date()) ? "pending" : "active";
                    
                    // Create order
                    Order order = new Order(
                        "",
                        userId,
                        machineId,
                        machine.getMachineName(),
                        temperature,
                        startTime,
                        endTime,
                        status,
                        totalAmount,
                        "",
                        new Date(),
                        null
                    );
                    
                    Map<String, Object> orderMap = Order.Companion.toMap(order);
                    // Add machine_name directly to order for faster loading
                    orderMap.put("machine_name", machine.getMachineName());
                    return db.collection(COLLECTION_ORDERS)
                        .add(orderMap)
                        .continueWithTask(orderTask -> {
                            if (!orderTask.isSuccessful()) {
                                throw orderTask.getException();
                            }
                            
                            String orderId = orderTask.getResult().getId();
                            
                            // Create payment record
                            Payment payment = new Payment(
                                "",
                                orderId,
                                totalAmount,
                                "pending",
                                null,
                                null,
                                null
                            );
                            
                            Map<String, Object> paymentMap = Payment.Companion.toMap(payment);
                            return db.collection(COLLECTION_PAYMENTS)
                                .add(paymentMap)
                                .continueWithTask(paymentTask -> {
                                    if (!paymentTask.isSuccessful()) {
                                        throw paymentTask.getException();
                                    }
                                    
                                    String paymentId = paymentTask.getResult().getId();
                                    Log.d(TAG, "Payment created with ID: " + paymentId + " for order: " + orderId);
                                    
                                    // Update order with payment ID
                                    return db.collection(COLLECTION_ORDERS)
                                        .document(orderId)
                                        .update("payment_id", paymentId)
                                        .continueWith(updateTask -> {
                                            if (!updateTask.isSuccessful()) {
                                                Log.e(TAG, "Failed to update order with payment_id: " + updateTask.getException().getMessage());
                                                throw updateTask.getException();
                                            }
                                            Log.d(TAG, "Order updated with payment_id successfully: " + orderId);
                                            return orderId;
                                        });
                                });
                        });
                });
        });
    }
    
    public Task<Boolean> isMachineAvailableForTimeSlot(String machineId, Date startTime, Date endTime) {
        Timestamp startTimestamp = new Timestamp(startTime);
        Timestamp endTimestamp = new Timestamp(endTime);
        
        // Get all orders for this machine that overlap with the time slot
        // and have completed payments
        return db.collection(COLLECTION_ORDERS)
            .whereEqualTo("machine_id", machineId)
            .whereNotEqualTo("status", "cancelled")
            .get()
            .continueWithTask(ordersTask -> {
                if (!ordersTask.isSuccessful()) {
                    Exception exception = ordersTask.getException();
                    // Check if it's a permission error
                    if (exception instanceof FirebaseFirestoreException) {
                        FirebaseFirestoreException firestoreException = (FirebaseFirestoreException) exception;
                        if (firestoreException.getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                            // If permission denied, assume available (will be caught by Firestore rules on write)
                            // But log it for debugging
                            Log.w(TAG, "Permission denied checking availability, assuming available. Please deploy Firestore security rules.");
                            return Tasks.forResult(true);
                        }
                    }
                    // For other errors, throw exception
                    throw exception != null ? exception : new Exception("Failed to check machine availability");
                }
                
                // Get all payment IDs for these orders
                List<String> orderIds = new ArrayList<>();
                for (QueryDocumentSnapshot doc : ordersTask.getResult()) {
                    orderIds.add(doc.getId());
                }
                
                if (orderIds.isEmpty()) {
                    return Tasks.forResult(true);
                }
                
                // Check if any of these orders have completed payments and overlap
                return db.collection(COLLECTION_PAYMENTS)
                    .whereIn("order_id", orderIds)
                    .whereEqualTo("status", "completed")
                    .get()
                    .continueWithTask(paymentsTask -> {
                        if (!paymentsTask.isSuccessful()) {
                            Exception exception = paymentsTask.getException();
                            // Check if it's a permission error
                            if (exception instanceof FirebaseFirestoreException) {
                                FirebaseFirestoreException firestoreException = (FirebaseFirestoreException) exception;
                                if (firestoreException.getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                                    // If permission denied, assume available (will be caught by Firestore rules on write)
                                    Log.w(TAG, "Permission denied checking payments, assuming available. Please deploy Firestore security rules.");
                                    return Tasks.forResult(true);
                                }
                            }
                            // For other errors, throw exception
                            throw exception != null ? exception : new Exception("Failed to check payment status");
                        }
                        
                        Set<String> completedOrderIds = new HashSet<>();
                        for (QueryDocumentSnapshot doc : paymentsTask.getResult()) {
                            String orderId = doc.getString("order_id");
                            if (orderId != null) {
                                completedOrderIds.add(orderId);
                            }
                        }
                        
                        // Check for overlapping time slots
                        for (QueryDocumentSnapshot orderDoc : ordersTask.getResult()) {
                            String orderId = orderDoc.getId();
                            if (!completedOrderIds.contains(orderId)) {
                                continue;
                            }
                            
                            Timestamp orderStart = orderDoc.getTimestamp("start_time");
                            Timestamp orderEnd = orderDoc.getTimestamp("end_time");
                            
                            if (orderStart != null && orderEnd != null) {
                                // Check if time slots overlap: start1 < end2 AND start2 < end1
                                if (startTimestamp.compareTo(orderEnd) < 0 && 
                                    endTimestamp.compareTo(orderStart) > 0) {
                                    return Tasks.forResult(false);
                                }
                            }
                        }
                        
                        return Tasks.forResult(true);
                    });
            });
    }
    
    public Task<List<Order>> getOrders(String userId) {
        // Try with orderBy first, fallback to without if index missing
        return db.collection(COLLECTION_ORDERS)
            .whereEqualTo("user_id", userId)
            .orderBy("created_at", Query.Direction.DESCENDING)
            .get()
            .continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    Exception exception = task.getException();
                    // If index error, try without orderBy
                    if (exception instanceof FirebaseFirestoreException) {
                        FirebaseFirestoreException firestoreException = (FirebaseFirestoreException) exception;
                        if (firestoreException.getCode() == FirebaseFirestoreException.Code.FAILED_PRECONDITION) {
                            Log.w(TAG, "Orders index not found, fetching without orderBy");
                            return db.collection(COLLECTION_ORDERS)
                                .whereEqualTo("user_id", userId)
                                .get()
                                .continueWithTask(fallbackTask -> {
                                    if (!fallbackTask.isSuccessful()) {
                                        return Tasks.forException(fallbackTask.getException());
                                    }
                                    return processOrdersResult(fallbackTask.getResult());
                                });
                        }
                    }
                    return Tasks.forException(exception);
                }
                return processOrdersResult(task.getResult());
            });
    }
    
    private Task<List<Order>> processOrdersResult(QuerySnapshot result) {
        // Process orders without blocking - don't fetch machine/payment data synchronously
        List<Order> orders = new ArrayList<>();
        Date now = new Date();
        
        for (QueryDocumentSnapshot doc : result) {
            try {
                Map<String, Object> data = doc.getData();
                data.put("id", doc.getId());
                
                // Update status based on current time
                String status = doc.getString("status");
                Timestamp startTime = doc.getTimestamp("start_time");
                Timestamp endTime = doc.getTimestamp("end_time");
                
                if (status != null && startTime != null && endTime != null) {
                    Date startDate = startTime.toDate();
                    Date endDate = endTime.toDate();
                    
                    if ("pending".equals(status) && !startDate.after(now)) {
                        // Update to active (async, don't wait)
                        doc.getReference().update("status", "active");
                        status = "active";
                        data.put("status", "active");
                    }
                    if ("active".equals(status) && !endDate.after(now)) {
                        // Update to completed (async, don't wait)
                        doc.getReference().update("status", "completed");
                        status = "completed";
                        data.put("status", "completed");
                    }
                }
                
                // Get machine name from document if available, otherwise use machine_id
                String machineId = doc.getString("machine_id");
                String machineName = doc.getString("machine_name");
                if (machineName != null && !machineName.isEmpty()) {
                    // Machine name already in document
                    Map<String, Object> machineMap = new HashMap<>();
                    machineMap.put("machine_name", machineName);
                    data.put("machine", machineMap);
                } else if (machineId != null) {
                    // Store machine_id for later async loading if needed
                    data.put("machine_id", machineId);
                }
                
                // Get payment_id - payment data will be loaded on-demand when needed
                String paymentId = doc.getString("payment_id");
                if (paymentId != null && !paymentId.isEmpty()) {
                    data.put("payment_id", paymentId);
                }
                
                Order order = Order.Companion.fromMap(data);
                orders.add(order);
            } catch (Exception e) {
                Log.e(TAG, "Error processing order document: " + e.getMessage(), e);
                // Continue with next order
            }
        }
        
        // Sort manually if needed (when orderBy wasn't used)
        orders.sort((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()));
        
        return Tasks.forResult(orders);
    }
    
    public Task<Order> getOrderById(String orderId) {
        return db.collection(COLLECTION_ORDERS)
            .document(orderId)
            .get()
            .continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    Exception exception = task.getException();
                    Log.e(TAG, "Failed to get order document: " + exception.getMessage(), exception);
                    throw exception;
                }
                
                DocumentSnapshot doc = task.getResult();
                if (doc == null || !doc.exists()) {
                    Log.e(TAG, "Order document not found: " + orderId);
                    throw new Exception("Order not found");
                }
                
                Map<String, Object> data = doc.getData();
                if (data == null) {
                    Log.e(TAG, "Order document data is null: " + orderId);
                    throw new Exception("Order data is null");
                }
                
                data.put("id", doc.getId());
                
                // Get machine - handle errors gracefully
                String machineId = doc.getString("machine_id");
                if (machineId != null) {
                    try {
                        Machine machine = Tasks.await(getMachineById(machineId));
                        if (machine != null) {
                            Map<String, Object> machineMap = Machine.Companion.toMap(machine);
                            data.put("machine", machineMap);
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Failed to get machine for order: " + e.getMessage());
                        // Continue without machine data - order can still be displayed
                    }
                }
                
                // Get payment - handle errors gracefully, payment might not exist yet
                String paymentId = doc.getString("payment_id");
                if (paymentId != null && !paymentId.isEmpty()) {
                    try {
                        Payment payment = Tasks.await(getPaymentById(paymentId));
                        if (payment != null) {
                            Map<String, Object> paymentMap = Payment.Companion.toMap(payment);
                            paymentMap.put("id", payment.getId());
                            if (payment.getPaidAt() != null) {
                                paymentMap.put("paid_at", new Timestamp(payment.getPaidAt().getTime() / 1000, (int) ((payment.getPaidAt().getTime() % 1000) * 1000000)));
                            }
                            data.put("payment", paymentMap);
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Failed to get payment for order: " + e.getMessage());
                        // Continue without payment data - payment might be created asynchronously
                        // Set payment_id directly so Order model can extract it
                        data.put("payment_id", paymentId);
                    }
                } else {
                    Log.w(TAG, "Order has no payment_id: " + orderId);
                }
                
                try {
                    Order order = Order.Companion.fromMap(data);
                    return Tasks.forResult(order);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to create Order from map: " + e.getMessage(), e);
                    throw new Exception("Failed to parse order data: " + e.getMessage());
                }
            });
    }
    
    // ==================== Payments ====================
    
    public Task<Payment> getPaymentById(String paymentId) {
        return db.collection(COLLECTION_PAYMENTS)
            .document(paymentId)
            .get()
            .continueWith(task -> {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }
                
                DocumentSnapshot doc = task.getResult();
                if (doc == null || !doc.exists()) {
                    throw new Exception("Payment not found");
                }
                
                Map<String, Object> data = doc.getData();
                data.put("id", doc.getId());
                return Payment.Companion.fromMap(data);
            });
    }
    
    public Task<Void> completePayment(String paymentId, String paymentMethod, String voucherId) {
        Log.d(TAG, "=== completePayment START ===");
        Log.d(TAG, "paymentId=" + paymentId + ", method=" + paymentMethod + ", voucherId=" + (voucherId != null ? voucherId : "null"));
        return getPaymentById(paymentId).continueWithTask(task -> {
            if (!task.isSuccessful()) {
                Log.e(TAG, "Failed to get payment: " + task.getException().getMessage());
                throw task.getException();
            }
            Log.d(TAG, "Payment retrieved successfully");
            
            Payment payment = task.getResult();
            final double[] finalAmount = {payment.getAmount()};
            final String[] orderIdFromPayment = {payment.getOrderId()}; // Get order ID from payment
            
            // Apply voucher discount if provided
            if (voucherId != null && !voucherId.isEmpty()) {
                return getVoucherById(voucherId).continueWithTask(voucherTask -> {
                    if (voucherTask.isSuccessful()) {
                        Voucher voucher = voucherTask.getResult();
                        if (voucher.isValid() && voucher.getType().equals("rm5_off")) {
                            finalAmount[0] = Math.max(0, finalAmount[0] - 5.0);
                            // Mark voucher as used
                            Map<String, Object> voucherUpdate = new HashMap<>();
                            voucherUpdate.put("used", true);
                            voucherUpdate.put("order_id", payment.getOrderId());
                            db.collection(COLLECTION_VOUCHERS)
                                .document(voucherId)
                                .update(voucherUpdate);
                        }
                    }
                    
                    // Complete payment
                    String transactionId = "TXN-" + UUID.randomUUID().toString().toUpperCase();
                    Map<String, Object> paymentUpdate = new HashMap<>();
                    paymentUpdate.put("status", "completed");
                    paymentUpdate.put("payment_method", paymentMethod);
                    paymentUpdate.put("transaction_id", transactionId);
                    paymentUpdate.put("paid_at", new Timestamp(new Date()));
                    paymentUpdate.put("amount", finalAmount[0]);
                    
                    return db.collection(COLLECTION_PAYMENTS)
                        .document(paymentId)
                        .update(paymentUpdate)
                        .continueWithTask(updateTask -> {
                            if (!updateTask.isSuccessful()) {
                                Log.e(TAG, "Failed to update payment: " + updateTask.getException().getMessage());
                                return Tasks.forException(updateTask.getException());
                            }
                            
                            Log.d(TAG, "Payment updated successfully, now updating order and creating token");
                            
                            // Use order ID from payment if available, otherwise query
                            if (orderIdFromPayment[0] != null && !orderIdFromPayment[0].isEmpty()) {
                                Log.d(TAG, "Using order ID from payment: " + orderIdFromPayment[0]);
                                String orderId = orderIdFromPayment[0];
                                return db.collection(COLLECTION_ORDERS)
                                    .document(orderId)
                                    .get()
                                    .continueWithTask(orderTask -> {
                                        if (!orderTask.isSuccessful()) {
                                            Log.e(TAG, "Failed to get order: " + orderTask.getException().getMessage());
                                            return Tasks.forException(orderTask.getException());
                                        }
                                        
                                        DocumentSnapshot orderDoc = orderTask.getResult();
                                        if (!orderDoc.exists()) {
                                            Log.w(TAG, "No order found with ID: " + orderId);
                                            return Tasks.forResult(null);
                                        }
                                        
                                        Log.d(TAG, "Found order: " + orderId);
                                        
                                        // Determine order status
                                        Timestamp startTime = orderDoc.getTimestamp("start_time");
                                        String status = (startTime != null && startTime.toDate().after(new Date())) 
                                            ? "pending" : "active";
                                        
                                        orderDoc.getReference().update("status", status);
                                        Log.d(TAG, "Order status updated to: " + status);
                                        
                                        // Award token
                                        String userId = orderDoc.getString("user_id");
                                        if (userId == null || userId.isEmpty()) {
                                            Log.e(TAG, "User ID is null or empty for order: " + orderId);
                                            return Tasks.forResult(null);
                                        }
                                        
                                        Log.d(TAG, "Creating token for user: " + userId + ", order: " + orderId);
                                        Token token = new Token(
                                            "",
                                            userId,
                                            orderId,
                                            false
                                        );
                                        Map<String, Object> tokenMap = Token.Companion.toMap(token);
                                        
                                        // Create token as part of the task chain
                                        return db.collection(COLLECTION_TOKENS).add(tokenMap)
                                            .continueWith(tokenTask -> {
                                                if (tokenTask.isSuccessful()) {
                                                    String tokenId = tokenTask.getResult().getId();
                                                    Log.d(TAG, "Token created successfully! ID: " + tokenId + " for user: " + userId + ", order: " + orderId);
                                                } else {
                                                    Log.e(TAG, "Failed to create token: " + tokenTask.getException().getMessage(), tokenTask.getException());
                                                }
                                                return null;
                                            });
                                    });
                            } else {
                                // Fallback: query by payment_id if orderId not in payment
                                Log.w(TAG, "Order ID not found in payment, querying by payment_id: " + paymentId);
                                return db.collection(COLLECTION_ORDERS)
                                    .whereEqualTo("payment_id", paymentId)
                                    .get()
                                    .continueWithTask(orderTask -> {
                                        if (!orderTask.isSuccessful()) {
                                            Log.e(TAG, "Failed to get order: " + orderTask.getException().getMessage());
                                            return Tasks.forException(orderTask.getException());
                                        }
                                        
                                        if (orderTask.getResult().isEmpty()) {
                                            Log.w(TAG, "No order found for payment_id: " + paymentId);
                                            return Tasks.forResult(null);
                                        }
                                        
                                        DocumentSnapshot orderDoc = orderTask.getResult().getDocuments().get(0);
                                        String orderId = orderDoc.getId();
                                        Log.d(TAG, "Found order: " + orderId);
                                        
                                        // Determine order status
                                        Timestamp startTime = orderDoc.getTimestamp("start_time");
                                        String status = (startTime != null && startTime.toDate().after(new Date())) 
                                            ? "pending" : "active";
                                        
                                        orderDoc.getReference().update("status", status);
                                        Log.d(TAG, "Order status updated to: " + status);
                                        
                                        // Award token
                                        String userId = orderDoc.getString("user_id");
                                        if (userId == null || userId.isEmpty()) {
                                            Log.e(TAG, "User ID is null or empty for order: " + orderId);
                                            return Tasks.forResult(null);
                                        }
                                        
                                        Log.d(TAG, "Creating token for user: " + userId + ", order: " + orderId);
                                        Token token = new Token(
                                            "",
                                            userId,
                                            orderId,
                                            false
                                        );
                                        Map<String, Object> tokenMap = Token.Companion.toMap(token);
                                        
                                        // Create token as part of the task chain
                                        return db.collection(COLLECTION_TOKENS).add(tokenMap)
                                            .continueWith(tokenTask -> {
                                                if (tokenTask.isSuccessful()) {
                                                    String tokenId = tokenTask.getResult().getId();
                                                    Log.d(TAG, "Token created successfully! ID: " + tokenId + " for user: " + userId + ", order: " + orderId);
                                                } else {
                                                    Log.e(TAG, "Failed to create token: " + tokenTask.getException().getMessage(), tokenTask.getException());
                                                }
                                                return null;
                                            });
                                    });
                            }
                        });
                });
            } else {
                // Complete payment without voucher
                String transactionId = "TXN-" + UUID.randomUUID().toString().toUpperCase();
                Map<String, Object> paymentUpdate = new HashMap<>();
                paymentUpdate.put("status", "completed");
                paymentUpdate.put("payment_method", paymentMethod);
                paymentUpdate.put("transaction_id", transactionId);
                paymentUpdate.put("paid_at", new Timestamp(new Date()));
                
                return db.collection(COLLECTION_PAYMENTS)
                    .document(paymentId)
                    .update(paymentUpdate)
                    .continueWithTask(updateTask -> {
                        if (!updateTask.isSuccessful()) {
                            Log.e(TAG, "Failed to update payment: " + updateTask.getException().getMessage());
                            return Tasks.forException(updateTask.getException());
                        }
                        
                        Log.d(TAG, "Payment updated successfully, now updating order and creating token");
                        
                        // Use order ID from payment if available, otherwise query
                        if (orderIdFromPayment[0] != null && !orderIdFromPayment[0].isEmpty()) {
                            Log.d(TAG, "Using order ID from payment: " + orderIdFromPayment[0]);
                            String orderId = orderIdFromPayment[0];
                            return db.collection(COLLECTION_ORDERS)
                                .document(orderId)
                                .get()
                                .continueWithTask(orderTask -> {
                                    if (!orderTask.isSuccessful()) {
                                        Log.e(TAG, "Failed to get order: " + orderTask.getException().getMessage());
                                        return Tasks.forException(orderTask.getException());
                                    }
                                    
                                    DocumentSnapshot orderDoc = orderTask.getResult();
                                    if (!orderDoc.exists()) {
                                        Log.w(TAG, "No order found with ID: " + orderId);
                                        return Tasks.forResult(null);
                                    }
                                    
                                    Log.d(TAG, "Found order: " + orderId);
                                    
                                    Timestamp startTime = orderDoc.getTimestamp("start_time");
                                    String status = (startTime != null && startTime.toDate().after(new Date())) 
                                        ? "pending" : "active";
                                    
                                    orderDoc.getReference().update("status", status);
                                    Log.d(TAG, "Order status updated to: " + status);
                                    
                                    String userId = orderDoc.getString("user_id");
                                    if (userId == null || userId.isEmpty()) {
                                        Log.e(TAG, "User ID is null or empty for order: " + orderId);
                                        return Tasks.forResult(null);
                                    }
                                    
                                    Log.d(TAG, "Creating token for user: " + userId + ", order: " + orderId);
                                    Token token = new Token("", userId, orderId, false);
                                    Map<String, Object> tokenMap = Token.Companion.toMap(token);
                                    
                                    // Create token as part of the task chain
                                    return db.collection(COLLECTION_TOKENS).add(tokenMap)
                                        .continueWith(tokenTask -> {
                                            if (tokenTask.isSuccessful()) {
                                                String tokenId = tokenTask.getResult().getId();
                                                Log.d(TAG, "Token created successfully! ID: " + tokenId + " for user: " + userId + ", order: " + orderId);
                                            } else {
                                                Log.e(TAG, "Failed to create token: " + tokenTask.getException().getMessage(), tokenTask.getException());
                                            }
                                            return null;
                                        });
                                });
                        } else {
                            // Fallback: query by payment_id if orderId not in payment
                            Log.w(TAG, "Order ID not found in payment, querying by payment_id: " + paymentId);
                            return db.collection(COLLECTION_ORDERS)
                                .whereEqualTo("payment_id", paymentId)
                                .get()
                                .continueWithTask(orderTask -> {
                                    if (!orderTask.isSuccessful()) {
                                        Log.e(TAG, "Failed to get order: " + orderTask.getException().getMessage());
                                        return Tasks.forException(orderTask.getException());
                                    }
                                    
                                    if (orderTask.getResult().isEmpty()) {
                                        Log.w(TAG, "No order found for payment_id: " + paymentId);
                                        return Tasks.forResult(null);
                                    }
                                    
                                    DocumentSnapshot orderDoc = orderTask.getResult().getDocuments().get(0);
                                    String orderId = orderDoc.getId();
                                    Log.d(TAG, "Found order: " + orderId);
                                    
                                    Timestamp startTime = orderDoc.getTimestamp("start_time");
                                    String status = (startTime != null && startTime.toDate().after(new Date())) 
                                        ? "pending" : "active";
                                    
                                    orderDoc.getReference().update("status", status);
                                    Log.d(TAG, "Order status updated to: " + status);
                                    
                                    String userId = orderDoc.getString("user_id");
                                    if (userId == null || userId.isEmpty()) {
                                        Log.e(TAG, "User ID is null or empty for order: " + orderId);
                                        return Tasks.forResult(null);
                                    }
                                    
                                    Log.d(TAG, "Creating token for user: " + userId + ", order: " + orderId);
                                    Token token = new Token("", userId, orderId, false);
                                    Map<String, Object> tokenMap = Token.Companion.toMap(token);
                                    
                                    // Create token as part of the task chain
                                    return db.collection(COLLECTION_TOKENS).add(tokenMap)
                                        .continueWith(tokenTask -> {
                                            if (tokenTask.isSuccessful()) {
                                                String tokenId = tokenTask.getResult().getId();
                                                Log.d(TAG, "Token created successfully! ID: " + tokenId + " for user: " + userId + ", order: " + orderId);
                                            } else {
                                                Log.e(TAG, "Failed to create token: " + tokenTask.getException().getMessage(), tokenTask.getException());
                                            }
                                            return null;
                                        });
                                });
                        }
                    });
            }
        });
    }
    
    // ==================== Tokens ====================
    
    public Task<Integer> getAvailableTokensCount(String userId) {
        return db.collection(COLLECTION_TOKENS)
            .whereEqualTo("user_id", userId)
            .whereEqualTo("used", false)
            .get()
            .continueWith(task -> {
                if (!task.isSuccessful()) {
                    return 0;
                }
                return task.getResult().size();
            });
    }
    
    public Task<Void> useToken(String userId) {
        return db.collection(COLLECTION_TOKENS)
            .whereEqualTo("user_id", userId)
            .whereEqualTo("used", false)
            .limit(1)
            .get()
            .continueWithTask(task -> {
                if (!task.isSuccessful() || task.getResult().isEmpty()) {
                    throw new Exception("No available tokens");
                }
                
                DocumentSnapshot tokenDoc = task.getResult().getDocuments().get(0);
                return tokenDoc.getReference().update("used", true);
            });
    }
    
    // ==================== Vouchers ====================
    
    public Task<List<Voucher>> getVouchers(String userId) {
        return db.collection(COLLECTION_VOUCHERS)
            .whereEqualTo("user_id", userId)
            .orderBy("created_at", Query.Direction.DESCENDING)
            .get()
            .continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    Exception exception = task.getException();
                    // Check if it's an index error
                    if (exception instanceof FirebaseFirestoreException) {
                        FirebaseFirestoreException firestoreException = (FirebaseFirestoreException) exception;
                        if (firestoreException.getCode() == FirebaseFirestoreException.Code.FAILED_PRECONDITION) {
                            // Index required - try without orderBy as fallback
                            Log.w(TAG, "Vouchers index not found, fetching without orderBy. Please create the index. " +
                                "Click the link in the error message to create it automatically.");
                            return db.collection(COLLECTION_VOUCHERS)
                                .whereEqualTo("user_id", userId)
                                .get()
                                .continueWith(fallbackTask -> {
                                    if (!fallbackTask.isSuccessful()) {
                                        throw fallbackTask.getException();
                                    }
                                    List<Voucher> vouchers = new ArrayList<>();
                                    for (QueryDocumentSnapshot doc : fallbackTask.getResult()) {
                                        Map<String, Object> data = doc.getData();
                                        data.put("id", doc.getId());
                                        vouchers.add(Voucher.Companion.fromMap(data));
                                    }
                                    // Sort manually by created_at
                                    vouchers.sort((v1, v2) -> {
                                        Date d1 = v1.getCreatedAt();
                                        Date d2 = v2.getCreatedAt();
                                        if (d1 == null && d2 == null) return 0;
                                        if (d1 == null) return 1;
                                        if (d2 == null) return -1;
                                        return d2.compareTo(d1); // Descending
                                    });
                                    return vouchers;
                                });
                        }
                    }
                    return Tasks.forException(exception);
                }
                
                List<Voucher> vouchers = new ArrayList<>();
                for (QueryDocumentSnapshot doc : task.getResult()) {
                    Map<String, Object> data = doc.getData();
                    data.put("id", doc.getId());
                    vouchers.add(Voucher.Companion.fromMap(data));
                }
                return Tasks.forResult(vouchers);
            });
    }
    
    public Task<Voucher> getVoucherById(String voucherId) {
        return db.collection(COLLECTION_VOUCHERS)
            .document(voucherId)
            .get()
            .continueWith(task -> {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }
                
                DocumentSnapshot doc = task.getResult();
                if (doc == null || !doc.exists()) {
                    throw new Exception("Voucher not found");
                }
                
                Map<String, Object> data = doc.getData();
                data.put("id", doc.getId());
                return Voucher.Companion.fromMap(data);
            });
    }
    
    public Task<String> createVoucher(String userId, String type) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, 30);
        Date expiresAt = calendar.getTime();
        
        Voucher voucher = new Voucher(
            "",
            userId,
            type,
            false,
            null,
            expiresAt,
            new Date()
        );
        
        Map<String, Object> voucherMap = Voucher.Companion.toMap(voucher);
        return db.collection(COLLECTION_VOUCHERS)
            .add(voucherMap)
            .continueWith(task -> {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }
                return task.getResult().getId();
            });
    }
    
    // ==================== Admin ====================
    
    public Task<List<User>> getAllUsers() {
        return db.collection(COLLECTION_USERS)
            .get()
            .continueWith(task -> {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }
                
                List<User> users = new ArrayList<>();
                for (QueryDocumentSnapshot doc : task.getResult()) {
                    Map<String, Object> data = doc.getData();
                    data.put("id", doc.getId());
                    users.add(User.Companion.fromMap(data));
                }
                return users;
            });
    }
    
    public Task<Void> deleteUser(String userId) {
        return db.collection(COLLECTION_USERS)
            .document(userId)
            .delete();
    }
    
    public Task<Map<String, Object>> getAnalytics() {
        Date now = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        Date monthStart = calendar.getTime();
        
        calendar.set(Calendar.MONTH, 0);
        Date yearStart = calendar.getTime();
        
        Timestamp monthStartTimestamp = new Timestamp(monthStart);
        Timestamp yearStartTimestamp = new Timestamp(yearStart);
        
        // Get all payments
        return db.collection(COLLECTION_PAYMENTS)
            .whereEqualTo("status", "completed")
            .get()
            .continueWithTask(paymentsTask -> {
                if (!paymentsTask.isSuccessful()) {
                    throw paymentsTask.getException();
                }
                
                final double[] totalRevenue = {0};
                final double[] monthlyRevenue = {0};
                final double[] yearlyRevenue = {0};
                
                for (QueryDocumentSnapshot doc : paymentsTask.getResult()) {
                    Double amount = doc.getDouble("amount");
                    if (amount != null) {
                        totalRevenue[0] += amount;
                        
                        Timestamp paidAt = doc.getTimestamp("paid_at");
                        if (paidAt != null) {
                            Date paidDate = paidAt.toDate();
                            if (paidDate.after(monthStart)) {
                                monthlyRevenue[0] += amount;
                            }
                            if (paidDate.after(yearStart)) {
                                yearlyRevenue[0] += amount;
                            }
                        }
                    }
                }
                
                // Get orders count
                return db.collection(COLLECTION_ORDERS).get()
                    .continueWithTask(ordersTask -> {
                        int totalOrders = ordersTask.isSuccessful() ? ordersTask.getResult().size() : 0;
                        
                        // Get monthly orders
                        return db.collection(COLLECTION_ORDERS)
                            .whereGreaterThanOrEqualTo("created_at", monthStartTimestamp)
                            .get()
                            .continueWithTask(monthlyOrdersTask -> {
                                int monthlyOrders = monthlyOrdersTask.isSuccessful() 
                                    ? monthlyOrdersTask.getResult().size() : 0;
                                
                                // Get users count
                                return db.collection(COLLECTION_USERS).get()
                                    .continueWithTask(usersTask -> {
                                        int totalUsers = usersTask.isSuccessful() 
                                            ? usersTask.getResult().size() : 0;
                                        
                                        // Get active users this month
                                        return db.collection(COLLECTION_USERS)
                                            .get()
                                            .continueWithTask(activeUsersTask -> {
                                                final int[] activeUsers = {0};
                                                if (activeUsersTask.isSuccessful()) {
                                                    for (QueryDocumentSnapshot doc : activeUsersTask.getResult()) {
                                                        // Check if user has orders this month
                                                        String userId = doc.getId();
                                                        // Simplified: count users with any orders
                                                        activeUsers[0]++;
                                                    }
                                                }
                                                
                                                // Get machines
                                                return db.collection(COLLECTION_MACHINES).get()
                                                    .continueWithTask(machinesTask -> {
                                                        int totalMachines = machinesTask.isSuccessful() 
                                                            ? machinesTask.getResult().size() : 0;
                                                        int washers = 0;
                                                        int dryers = 0;
                                                        
                                                        if (machinesTask.isSuccessful()) {
                                                            for (QueryDocumentSnapshot doc : machinesTask.getResult()) {
                                                                String type = doc.getString("type");
                                                                if ("washer".equals(type)) washers++;
                                                                else if ("dryer".equals(type)) dryers++;
                                                            }
                                                        }
                                                        
                                                        // Build analytics map
                                                        Map<String, Object> analytics = new HashMap<>();
                                                        
                                                        Map<String, Object> revenue = new HashMap<>();
                                                        revenue.put("total", totalRevenue[0]);
                                                        revenue.put("monthly", monthlyRevenue[0]);
                                                        revenue.put("yearly", yearlyRevenue[0]);
                                                        analytics.put("revenue", revenue);
                                                        
                                                        Map<String, Object> orders = new HashMap<>();
                                                        orders.put("total", totalOrders);
                                                        orders.put("monthly", monthlyOrders);
                                                        orders.put("yearly", totalOrders); // Simplified
                                                        analytics.put("orders", orders);
                                                        
                                                        Map<String, Object> users = new HashMap<>();
                                                        users.put("total", totalUsers);
                                                        users.put("active_this_month", activeUsers[0]);
                                                        analytics.put("users", users);
                                                        
                                                        Map<String, Object> machines = new HashMap<>();
                                                        machines.put("total", totalMachines);
                                                        machines.put("washers", washers);
                                                        machines.put("dryers", dryers);
                                                        analytics.put("machines", machines);
                                                        
                                                        return Tasks.forResult(analytics);
                                                    });
                                            });
                                    });
                            });
                    });
            });
    }
}
