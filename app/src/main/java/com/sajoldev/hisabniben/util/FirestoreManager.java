package com.sajoldev.hisabniben.util;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.sajoldev.hisabniben.model.Customer;
import com.sajoldev.hisabniben.model.CustomerLedger;
import com.sajoldev.hisabniben.model.Expense;
import com.sajoldev.hisabniben.model.Purchase;
import com.sajoldev.hisabniben.model.PurchaseItem;
import com.sajoldev.hisabniben.model.RiceProduct;
import com.sajoldev.hisabniben.model.RiceReturn;
import com.sajoldev.hisabniben.model.Sale;
import com.sajoldev.hisabniben.model.SaleItem;
import com.sajoldev.hisabniben.model.StockMovement;
import com.sajoldev.hisabniben.model.SubscriptionPackage;
import com.sajoldev.hisabniben.model.Supplier;
import com.sajoldev.hisabniben.model.SupplierLedger;
import com.sajoldev.hisabniben.model.Transaction;
import com.sajoldev.hisabniben.model.User;
import com.sajoldev.hisabniben.model.WalletAccount;
import com.sajoldev.hisabniben.model.WalletTransaction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirestoreManager {
    private static final String TAG = "FirestoreManager";
    private static FirestoreManager instance;
    private final FirebaseFirestore db;
    
    public static final String COLLECTION_USERS = "users";
    public static final String COLLECTION_CUSTOMERS = "customers";
    public static final String COLLECTION_TRANSACTIONS = "transactions";
    public static final String COLLECTION_PACKAGES = "packages";
    public static final String COLLECTION_SUPPLIERS = "suppliers";
    public static final String COLLECTION_TUTORIAL_VIDEOS = "tutorial_videos";
    public static final String COLLECTION_RICE_PRODUCTS = "riceProducts";
    public static final String COLLECTION_PURCHASES = "purchases";
    public static final String COLLECTION_SALES = "sales";
    public static final String COLLECTION_STOCK_MOVEMENTS = "stockMovements";
    public static final String COLLECTION_EXPENSES = "expenses";
    public static final String COLLECTION_CUSTOMER_LEDGER = "customerLedger";
    public static final String COLLECTION_SUPPLIER_LEDGER = "supplierLedger";
    public static final String COLLECTION_RICE_RETURNS = "riceReturns";
    public static final String COLLECTION_WALLET_ACCOUNTS = "walletAccounts";
    public static final String COLLECTION_WALLET_TRANSACTIONS = "walletTransactions";

    
    public static final int TRIAL_DAYS = 7;
    public static final int MAX_CUSTOMERS_TRIAL = 5;
    public static final int MAX_TRANSACTIONS_TRIAL = 20;

    private FirestoreManager() {
        db = FirebaseFirestore.getInstance();
    }

    public static synchronized FirestoreManager getInstance() {
        if (instance == null) {
            instance = new FirestoreManager();
        }
        return instance;
    }

    public FirebaseFirestore getDb() {
        return db;
    }

    public interface FirestoreCallback<T> {
        void onSuccess(T result);
        void onFailure(String error);
    }

    public interface FirestoreListCallback<T> {
        void onSuccess(List<T> result);
        void onFailure(String error);
    }

    public interface FirestoreBooleanCallback {
        void onSuccess(boolean result);
        void onFailure(String error);
    }

    // User Methods
    public void createUser(User user, FirestoreCallback<String> callback) {
        db.collection(COLLECTION_USERS)
            .document(user.getUid())
            .set(user.toMap())
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    callback.onSuccess(user.getUid());
                } else {
                    String error = task.getException() != null ? 
                        task.getException().getMessage() : "Failed to create user";
                    callback.onFailure(error);
                }
            });
    }

    public void getUser(String userId, FirestoreCallback<User> callback) {
        db.collection(COLLECTION_USERS)
            .document(userId)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        User user = document.toObject(User.class);
                        callback.onSuccess(user);
                    } else {
                        callback.onFailure("User not found");
                    }
                } else {
                    callback.onFailure(task.getException() != null ? 
                        task.getException().getMessage() : "Failed to get user");
                }
            });
    }

    public void updateUser(String userId, Map<String, Object> data, FirestoreCallback<Void> callback) {
        db.collection(COLLECTION_USERS)
            .document(userId)
            .update(data)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    callback.onSuccess(null);
                } else {
                    callback.onFailure(task.getException() != null ? 
                        task.getException().getMessage() : "Failed to update user");
                }
            });
    }

    public void upgradeUserToPremium(String userId, String subscriptionId, long expiryDate, String packageName, FirestoreCallback<Void> callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("isPremium", true);
        data.put("premium", true);
        data.put("subscriptionStatus", "ACTIVE");
        data.put("subscriptionId", subscriptionId);
        data.put("subscriptionExpiryDate", expiryDate);
        if (packageName != null && !packageName.isEmpty()) {
            data.put("subscriptionPackageName", packageName);
            data.put("packageName", packageName);
        }
        updateUser(userId, data, callback);
    }

    public void upgradeUserToPremium(String userId, String subscriptionId, long expiryDate, FirestoreCallback<Void> callback) {
        upgradeUserToPremium(userId, subscriptionId, expiryDate, "", callback);
    }

    public void activateTrial(String userId, FirestoreCallback<Void> callback) {
        long currentTime = System.currentTimeMillis();
        long trialEnd = currentTime + (TRIAL_DAYS * 24L * 60L * 60L * 1000L);
        
        Map<String, Object> data = new HashMap<>();
        data.put("trialStart", currentTime);
        data.put("trialEnd", trialEnd);
        
        updateUser(userId, data, callback);
    }

    // Customer Methods
    public void createCustomer(Customer customer, FirestoreCallback<String> callback) {
        DocumentReference docRef = db.collection(COLLECTION_CUSTOMERS).document();
        customer.setId(docRef.getId());
        customer.setCreatedAt(System.currentTimeMillis());
        customer.setUpdatedAt(System.currentTimeMillis());
        
        docRef.set(customer.toMap())
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    callback.onSuccess(customer.getId());
                } else {
                    callback.onFailure(task.getException() != null ? 
                        task.getException().getMessage() : "Failed to create customer");
                }
            });
    }

    public void getCustomersByUser(String userId, FirestoreListCallback<Customer> callback) {
        db.collection(COLLECTION_CUSTOMERS)
            .whereEqualTo("userId", userId)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    List<Customer> customers = new ArrayList<>();
                    for (DocumentSnapshot document : task.getResult()) {
                        Customer customer = document.toObject(Customer.class);
                        if (customer.getId() == null) {
                            customer.setId(document.getId());
                        }
                        customers.add(customer);
                    }
                    customers.sort((c1, c2) -> Long.compare(c2.getCreatedAt(), c1.getCreatedAt()));
                    callback.onSuccess(customers);
                } else {
                    callback.onFailure(task.getException() != null ? 
                        task.getException().getMessage() : "Failed to get customers");
                }
            });
    }

    public void getCustomer(String customerId, FirestoreCallback<Customer> callback) {
        db.collection(COLLECTION_CUSTOMERS)
            .document(customerId)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Customer customer = document.toObject(Customer.class);
                        callback.onSuccess(customer);
                    } else {
                        callback.onFailure("Customer not found");
                    }
                } else {
                    callback.onFailure(task.getException() != null ? 
                        task.getException().getMessage() : "Failed to get customer");
                }
            });
    }

    public void updateCustomer(Customer customer, FirestoreCallback<Void> callback) {
        customer.setUpdatedAt(System.currentTimeMillis());
        db.collection(COLLECTION_CUSTOMERS)
            .document(customer.getId())
            .update(customer.toMap())
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    callback.onSuccess(null);
                } else {
                    callback.onFailure(task.getException() != null ? 
                        task.getException().getMessage() : "Failed to update customer");
                }
            });
    }

    public void updateCustomerBaki(String customerId, double newBaki, FirestoreCallback<Void> callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("baki", newBaki);
        data.put("updatedAt", System.currentTimeMillis());
        
        db.collection(COLLECTION_CUSTOMERS)
            .document(customerId)
            .update(data)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    callback.onSuccess(null);
                } else {
                    callback.onFailure(task.getException() != null ? 
                        task.getException().getMessage() : "Failed to update baki");
                }
            });
    }

    public void deleteCustomer(String customerId, FirestoreCallback<Void> callback) {
        db.collection(COLLECTION_CUSTOMERS)
            .document(customerId)
            .delete()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    callback.onSuccess(null);
                } else {
                    callback.onFailure(task.getException() != null ? 
                        task.getException().getMessage() : "Failed to delete customer");
                }
            });
    }

    public void getCustomerCount(String userId, FirestoreCallback<Integer> callback) {
        db.collection(COLLECTION_CUSTOMERS)
            .whereEqualTo("userId", userId)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    callback.onSuccess(task.getResult().size());
                } else {
                    callback.onFailure(task.getException() != null ? 
                        task.getException().getMessage() : "Failed to get customer count");
                }
            });
    }

    // Transaction Methods
    public void createTransaction(Transaction transaction, FirestoreCallback<String> callback) {
        DocumentReference docRef = db.collection(COLLECTION_TRANSACTIONS).document();
        transaction.setId(docRef.getId());
        transaction.setCreatedAt(System.currentTimeMillis());
        
        docRef.set(transaction.toMap())
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    callback.onSuccess(transaction.getId());
                } else {
                    callback.onFailure(task.getException() != null ? 
                        task.getException().getMessage() : "Failed to create transaction");
                }
            });
    }

    public void getTransactionsByUser(String userId, FirestoreListCallback<Transaction> callback) {
        db.collection(COLLECTION_TRANSACTIONS)
            .whereEqualTo("userId", userId)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    List<Transaction> transactions = new ArrayList<>();
                    for (DocumentSnapshot document : task.getResult()) {
                        Transaction transaction = document.toObject(Transaction.class);
                        transactions.add(transaction);
                    }
                    transactions.sort((t1, t2) -> Long.compare(t2.getDate(), t1.getDate()));
                    callback.onSuccess(transactions);
                } else {
                    callback.onFailure(task.getException() != null ? 
                        task.getException().getMessage() : "Failed to get transactions");
                }
            });
    }

    public void getTransactionsByCustomer(String customerId, FirestoreListCallback<Transaction> callback) {
        db.collection(COLLECTION_TRANSACTIONS)
            .whereEqualTo("customerId", customerId)
            .orderBy("date", Query.Direction.DESCENDING)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    List<Transaction> transactions = new ArrayList<>();
                    for (DocumentSnapshot document : task.getResult()) {
                        Transaction transaction = document.toObject(Transaction.class);
                        transactions.add(transaction);
                    }
                    callback.onSuccess(transactions);
                } else {
                    callback.onFailure(task.getException() != null ? 
                        task.getException().getMessage() : "Failed to get transactions");
                }
            });
    }

    public void getTransactionCount(String userId, FirestoreCallback<Integer> callback) {
        db.collection(COLLECTION_TRANSACTIONS)
            .whereEqualTo("userId", userId)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    callback.onSuccess(task.getResult().size());
                } else {
                    callback.onFailure(task.getException() != null ? 
                        task.getException().getMessage() : "Failed to get transaction count");
                }
            });
    }

    public void deleteTransaction(String transactionId, FirestoreCallback<Void> callback) {
        db.collection(COLLECTION_TRANSACTIONS)
            .document(transactionId)
            .delete()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    callback.onSuccess(null);
                } else {
                    callback.onFailure(task.getException() != null ? 
                        task.getException().getMessage() : "Failed to delete transaction");
                }
            });
    }

    // Package Methods
    public void getActivePackages(FirestoreListCallback<SubscriptionPackage> callback) {
        Log.e(TAG, "Fetching all packages from Firestore (removing status filter for debug)...");
        db.collection(COLLECTION_PACKAGES)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    List<SubscriptionPackage> packages = new ArrayList<>();
                    QuerySnapshot querySnapshot = task.getResult();
                    if (querySnapshot != null) {
                        Log.e(TAG, "Query successful, found " + querySnapshot.size() + " documents in 'packages' collection");
                        for (DocumentSnapshot document : querySnapshot) {
                            Log.e(TAG, "Found document: " + document.getId() + " Data: " + document.getData());
                            try {
                                SubscriptionPackage pkg = document.toObject(SubscriptionPackage.class);
                                if (pkg != null) {
                                    if (pkg.getId() == null) pkg.setId(document.getId());
                                    // Check status manually for debugging
                                    if (SubscriptionPackage.STATUS_ACTIVE.equals(pkg.getStatus())) {
                                        packages.add(pkg);
                                        Log.e(TAG, "Added active package: " + pkg.getName());
                                    } else {
                                        Log.w(TAG, "Package " + pkg.getName() + " is NOT active (status: " + pkg.getStatus() + ")");
                                    }
                                } else {
                                    Log.e(TAG, "Document " + document.getId() + " could not be converted to SubscriptionPackage");
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing package " + document.getId() + ": " + e.getMessage());
                            }
                        }
                    } else {
                        Log.e(TAG, "Query snapshot is null");
                    }
                    Collections.sort(packages, (a, b) -> Double.compare(a.getPrice(), b.getPrice()));
                    Log.e(TAG, "Returning " + packages.size() + " active packages to activity");
                    callback.onSuccess(packages);
                } else {
                    String error = task.getException() != null ? 
                        task.getException().getMessage() : "Unknown Firestore error";
                    Log.e(TAG, "Firestore query FAILED: " + error);
                    callback.onFailure(error);
                }
            });
    }

    public void getAllPackages(FirestoreListCallback<SubscriptionPackage> callback) {
        db.collection(COLLECTION_PACKAGES)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    List<SubscriptionPackage> packages = new ArrayList<>();
                    for (DocumentSnapshot document : task.getResult()) {
                        SubscriptionPackage pkg = document.toObject(SubscriptionPackage.class);
                        packages.add(pkg);
                    }
                    callback.onSuccess(packages);
                } else {
                    callback.onFailure(task.getException() != null ? 
                        task.getException().getMessage() : "Failed to get packages");
                }
            });
    }

    // Admin Methods
    public void getAllUsers(FirestoreListCallback<User> callback) {
        db.collection(COLLECTION_USERS)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    List<User> users = new ArrayList<>();
                    for (DocumentSnapshot document : task.getResult()) {
                        User user = document.toObject(User.class);
                        users.add(user);
                    }
                    callback.onSuccess(users);
                } else {
                    callback.onFailure(task.getException() != null ? 
                        task.getException().getMessage() : "Failed to get users");
                }
            });
    }

    public void getAllTransactions(FirestoreListCallback<Transaction> callback) {
        db.collection(COLLECTION_TRANSACTIONS)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    List<Transaction> transactions = new ArrayList<>();
                    for (DocumentSnapshot document : task.getResult()) {
                        Transaction transaction = document.toObject(Transaction.class);
                        transactions.add(transaction);
                    }
                    callback.onSuccess(transactions);
                } else {
                    callback.onFailure(task.getException() != null ? 
                        task.getException().getMessage() : "Failed to get transactions");
                }
            });
    }

    public void getAllCustomers(FirestoreListCallback<Customer> callback) {
        db.collection(COLLECTION_CUSTOMERS)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    List<Customer> customers = new ArrayList<>();
                    for (DocumentSnapshot document : task.getResult()) {
                        Customer customer = document.toObject(Customer.class);
                        customers.add(customer);
                    }
                    callback.onSuccess(customers);
                } else {
                    callback.onFailure(task.getException() != null ? 
                        task.getException().getMessage() : "Failed to get customers");
                }
            });
    }

    public void checkPhoneExists(String phone, FirestoreBooleanCallback callback) {
        db.collection(COLLECTION_USERS)
            .whereEqualTo("phone", phone)
            .limit(1)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    callback.onSuccess(!task.getResult().isEmpty());
                } else {
                    callback.onFailure(task.getException() != null ? 
                        task.getException().getMessage() : "Failed to check phone");
                }
            });
    }

    // Supplier Methods
    public void createSupplier(Supplier supplier, FirestoreCallback<String> callback) {
        DocumentReference docRef = db.collection(COLLECTION_SUPPLIERS).document();
        supplier.setId(docRef.getId());
        supplier.setCreatedAt(System.currentTimeMillis());
        supplier.setUpdatedAt(System.currentTimeMillis());

        docRef.set(supplier.toMap())
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    callback.onSuccess(supplier.getId());
                } else {
                    callback.onFailure(task.getException() != null ? task.getException().getMessage() : "Failed to create supplier");
                }
            });
    }

    public void getSuppliersByUser(String userId, FirestoreListCallback<Supplier> callback) {
        db.collection(COLLECTION_SUPPLIERS)
            .whereEqualTo("userId", userId)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    List<Supplier> suppliers = new ArrayList<>();
                    for (DocumentSnapshot document : task.getResult()) {
                        Supplier supplier = document.toObject(Supplier.class);
                        if (supplier != null) {
                            if (supplier.getId() == null) supplier.setId(document.getId());
                            suppliers.add(supplier);
                        }
                    }
                    suppliers.sort((s1, s2) -> Long.compare(s2.getCreatedAt(), s1.getCreatedAt()));
                    callback.onSuccess(suppliers);
                } else {
                    callback.onFailure(task.getException() != null ? task.getException().getMessage() : "Failed to get suppliers");
                }
            });
    }

    public void updateSupplier(Supplier supplier, FirestoreCallback<Void> callback) {
        supplier.setUpdatedAt(System.currentTimeMillis());
        db.collection(COLLECTION_SUPPLIERS)
            .document(supplier.getId())
            .update(supplier.toMap())
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    callback.onSuccess(null);
                } else {
                    callback.onFailure(task.getException() != null ? task.getException().getMessage() : "Failed to update supplier");
                }
            });
    }

    public void deleteSupplier(String supplierId, FirestoreCallback<Void> callback) {
        db.collection(COLLECTION_SUPPLIERS)
            .document(supplierId)
            .delete()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    callback.onSuccess(null);
                } else {
                    callback.onFailure(task.getException() != null ? task.getException().getMessage() : "Failed to delete supplier");
                }
            });
    }

    // Rice Product Methods
    public void createRiceProduct(RiceProduct product, FirestoreCallback<String> callback) {
        DocumentReference docRef = db.collection(COLLECTION_RICE_PRODUCTS).document();
        product.setId(docRef.getId());
        product.setCreatedAt(System.currentTimeMillis());
        product.setUpdatedAt(System.currentTimeMillis());

        docRef.set(product.toMap())
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    callback.onSuccess(product.getId());
                } else {
                    callback.onFailure(task.getException() != null ? task.getException().getMessage() : "Failed to create rice product");
                }
            });
    }

    public void getRiceProductsByUser(String userId, FirestoreListCallback<RiceProduct> callback) {
        db.collection(COLLECTION_RICE_PRODUCTS)
            .whereEqualTo("userId", userId)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    List<RiceProduct> products = new ArrayList<>();
                    for (DocumentSnapshot document : task.getResult()) {
                        RiceProduct product = document.toObject(RiceProduct.class);
                        if (product != null) {
                            if (product.getId() == null) product.setId(document.getId());
                            products.add(product);
                        }
                    }
                    callback.onSuccess(products);
                } else {
                    callback.onFailure(task.getException() != null ? task.getException().getMessage() : "Failed to get rice products");
                }
            });
    }

    public void updateRiceProduct(RiceProduct product, FirestoreCallback<Void> callback) {
        product.setUpdatedAt(System.currentTimeMillis());
        db.collection(COLLECTION_RICE_PRODUCTS)
            .document(product.getId())
            .update(product.toMap())
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    callback.onSuccess(null);
                } else {
                    callback.onFailure(task.getException() != null ? task.getException().getMessage() : "Failed to update rice product");
                }
            });
    }

    // Purchase Workflow
    public void createPurchase(Purchase purchase, FirestoreCallback<String> callback) {
        DocumentReference docRef = db.collection(COLLECTION_PURCHASES).document();
        purchase.setId(docRef.getId());
        if (purchase.getInvoiceNo() == null || purchase.getInvoiceNo().isEmpty()) {
            purchase.setInvoiceNo("PUR-" + System.currentTimeMillis() / 1000);
        }
        purchase.setCreatedAt(System.currentTimeMillis());
        purchase.setUpdatedAt(System.currentTimeMillis());

        db.runTransaction(transaction -> {
            // STEP 1: ALL READS FIRST (No writes allowed here!)
            // 1. Read Supplier Document
            DocumentSnapshot suppSnap = null;
            DocumentReference suppRef = null;
            if (purchase.getSupplierId() != null && !purchase.getSupplierId().isEmpty()) {
                suppRef = db.collection(COLLECTION_SUPPLIERS).document(purchase.getSupplierId());
                suppSnap = transaction.get(suppRef);
            }

            // 2. Read All Product Documents
            Map<String, DocumentSnapshot> prodSnapMap = new HashMap<>();
            if (purchase.getItems() != null) {
                for (PurchaseItem item : purchase.getItems()) {
                    if (item.getProductId() != null && !item.getProductId().isEmpty()) {
                        DocumentReference prodRef = db.collection(COLLECTION_RICE_PRODUCTS).document(item.getProductId());
                        prodSnapMap.put(item.getProductId(), transaction.get(prodRef));
                    }
                }
            }

            // 3. Read Wallet Account Document if paidAmount > 0
            DocumentSnapshot walletAccSnap = null;
            DocumentReference walletAccRef = null;
            if (purchase.getPaidAmount() > 0 && purchase.getUserId() != null) {
                String targetAccId = resolveAccountIdForMethod(purchase.getPaymentMethod(), null);
                walletAccRef = db.collection(COLLECTION_USERS)
                    .document(purchase.getUserId())
                    .collection(COLLECTION_WALLET_ACCOUNTS)
                    .document(targetAccId);
                walletAccSnap = transaction.get(walletAccRef);
            }

            // STEP 2: ALL WRITES AFTER (No reads allowed here!)
            // 1. Update Wallet Balance & Log Wallet Transaction if paidAmount > 0
            if (walletAccRef != null && walletAccSnap != null && walletAccSnap.exists()) {
                WalletAccount walletAcc = walletAccSnap.toObject(WalletAccount.class);
                if (walletAcc != null) {
                    double oldBal = walletAcc.getCurrentBalance();
                    if (oldBal < purchase.getPaidAmount()) {
                        throw new FirebaseFirestoreException(
                            "এই অ্যাকাউন্টে (" + walletAcc.getAccountName() + ") পর্যাপ্ত টাকা নেই। (বর্তমান ব্যালেন্স: ৳" + String.format("%.0f", oldBal) + ", পরিশোধ: ৳" + String.format("%.0f", purchase.getPaidAmount()) + ")",
                            FirebaseFirestoreException.Code.ABORTED
                        );
                    }
                    double newBal = oldBal - purchase.getPaidAmount();
                    transaction.update(walletAccRef, "currentBalance", newBal, "updatedAt", System.currentTimeMillis());

                    DocumentReference wtRef = db.collection(COLLECTION_USERS)
                        .document(purchase.getUserId())
                        .collection(COLLECTION_WALLET_TRANSACTIONS)
                        .document();

                    WalletTransaction wt = new WalletTransaction();
                    wt.setTransactionId(wtRef.getId());
                    wt.setAccountId(walletAcc.getAccountId());
                    wt.setAccountName(walletAcc.getAccountName());
                    wt.setUserId(purchase.getUserId());
                    wt.setType(WalletTransaction.TYPE_PURCHASE_PAYMENT);
                    wt.setDirection(WalletTransaction.DIRECTION_OUT);
                    wt.setCategory("Purchase Payment");
                    wt.setAmount(purchase.getPaidAmount());
                    wt.setBalanceBefore(oldBal);
                    wt.setBalanceAfter(newBal);
                    wt.setTitle("মহাজনকে চালের টাকা পরিশোধ (" + (purchase.getSupplierName() != null ? purchase.getSupplierName() : "মহাজন") + ")");
                    wt.setPurchaseId(purchase.getId());
                    wt.setSupplierId(purchase.getSupplierId());
                    wt.setSupplierName(purchase.getSupplierName());
                    wt.setPaymentMethod(walletAcc.getAccountName());
                    wt.setReference(purchase.getInvoiceNo());
                    wt.setCreatedAt(System.currentTimeMillis());
                    wt.setTransactionDate(purchase.getPurchaseDate() > 0 ? purchase.getPurchaseDate() : System.currentTimeMillis());

                    transaction.set(wtRef, wt.toMap());
                }
            }

            // 2. Save Purchase Document
            transaction.set(docRef, purchase.toMap());

            // 3. Update Supplier Payable & Ledger
            if (suppRef != null && suppSnap != null && suppSnap.exists()) {
                Supplier supplier = suppSnap.toObject(Supplier.class);
                if (supplier != null) {
                    double currentPayable = supplier.getCurrentPayable() + purchase.getDueAmount();
                    double totalPurchase = supplier.getTotalPurchase() + purchase.getGrandTotal();
                    double totalPaid = supplier.getTotalPaid() + purchase.getPaidAmount();
                    transaction.update(suppRef, 
                        "currentPayable", currentPayable,
                        "totalPurchase", totalPurchase,
                        "totalPaid", totalPaid,
                        "lastTransaction", purchase.getPurchaseDate(),
                        "updatedAt", System.currentTimeMillis()
                    );

                    // Supplier Ledger Entry
                    DocumentReference slRef = db.collection("supplierLedger").document();
                    SupplierLedger ledger = new SupplierLedger(
                        purchase.getSupplierId(), SupplierLedger.TYPE_CREDIT_PURCHASE,
                        purchase.getPaidAmount(), purchase.getGrandTotal(), currentPayable,
                        "চাল ক্রয় (Memo #" + purchase.getInvoiceNo() + ")"
                    );
                    ledger.setUserId(purchase.getUserId());
                    transaction.set(slRef, ledger.toMap());
                }
            }

            // 4. Update RiceProduct Stock & WAC
            if (purchase.getItems() != null) {
                for (PurchaseItem item : purchase.getItems()) {
                    if (item.getProductId() != null && prodSnapMap.containsKey(item.getProductId())) {
                        DocumentReference prodRef = db.collection(COLLECTION_RICE_PRODUCTS).document(item.getProductId());
                        DocumentSnapshot prodSnap = prodSnapMap.get(item.getProductId());
                        if (prodSnap != null && prodSnap.exists()) {
                            RiceProduct product = prodSnap.toObject(RiceProduct.class);
                            if (product != null) {
                                double newStockKg = product.getCurrentStockKg() + item.getTotalKg();
                                double newWac = UnitConverterHelper.calculateWeightedAverageCost(
                                    product.getCurrentStockKg(), product.getPurchaseRatePerKg(),
                                    item.getTotalKg(), item.getEffectiveCostPerKg()
                                );
                                transaction.update(prodRef, 
                                    "currentStockKg", newStockKg,
                                    "currentStockBags", UnitConverterHelper.kgToBags(newStockKg, product.getDefaultBagWeight()),
                                    "purchaseRatePerKg", newWac,
                                    "updatedAt", System.currentTimeMillis()
                                );

                                // Audit Stock Movement
                                DocumentReference smRef = db.collection("stockMovements").document();
                                StockMovement sm = new StockMovement(
                                    prodRef.getId(), product.getName(), product.getVariety(),
                                    StockMovement.TYPE_PURCHASE, item.getTotalKg(), newStockKg,
                                    "চাল ক্রয় (Memo #" + purchase.getInvoiceNo() + ")"
                                );
                                sm.setUserId(purchase.getUserId());
                                transaction.set(smRef, sm.toMap());
                            }
                        }
                    }
                }
            }
            return purchase.getId();
        }).addOnSuccessListener(callback::onSuccess)
        .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void cancelPurchase(String purchaseId, String reason, FirestoreCallback<Void> callback) {
        DocumentReference purRef = db.collection(COLLECTION_PURCHASES).document(purchaseId);

        db.runTransaction(transaction -> {
            // STEP 1: ALL READS FIRST
            DocumentSnapshot purSnap = transaction.get(purRef);
            if (!purSnap.exists()) {
                throw new FirebaseFirestoreException("চাল ক্রয় তথ্য পাওয়া যায়নি", FirebaseFirestoreException.Code.NOT_FOUND);
            }

            Purchase purchase = purSnap.toObject(Purchase.class);
            if (purchase == null || Purchase.PURCHASE_STATUS_CANCELLED.equals(purchase.getPurchaseStatus())) {
                throw new FirebaseFirestoreException("চাল ক্রয় ইতিমধ্যেই বাতিল করা হয়েছে", FirebaseFirestoreException.Code.ALREADY_EXISTS);
            }

            Map<String, DocumentSnapshot> prodSnapMap = new HashMap<>();
            if (purchase.getItems() != null) {
                for (PurchaseItem item : purchase.getItems()) {
                    if (item.getProductId() != null && !item.getProductId().isEmpty()) {
                        DocumentReference prodRef = db.collection(COLLECTION_RICE_PRODUCTS).document(item.getProductId());
                        prodSnapMap.put(item.getProductId(), transaction.get(prodRef));
                    }
                }
            }

            DocumentSnapshot suppSnap = null;
            DocumentReference suppRef = null;
            if (purchase.getSupplierId() != null && !purchase.getSupplierId().isEmpty()) {
                suppRef = db.collection(COLLECTION_SUPPLIERS).document(purchase.getSupplierId());
                suppSnap = transaction.get(suppRef);
            }

            // STEP 2: ALL WRITES AFTER
            // 1. Reverse Stock in KG
            if (purchase.getItems() != null) {
                for (PurchaseItem item : purchase.getItems()) {
                    if (item.getProductId() != null && prodSnapMap.containsKey(item.getProductId())) {
                        DocumentReference prodRef = db.collection(COLLECTION_RICE_PRODUCTS).document(item.getProductId());
                        DocumentSnapshot prodSnap = prodSnapMap.get(item.getProductId());
                        if (prodSnap != null && prodSnap.exists()) {
                            RiceProduct product = prodSnap.toObject(RiceProduct.class);
                            if (product != null) {
                                double reversedStockKg = Math.max(0, product.getCurrentStockKg() - item.getTotalKg());
                                transaction.update(prodRef, 
                                    "currentStockKg", reversedStockKg,
                                    "currentStockBags", UnitConverterHelper.kgToBags(reversedStockKg, product.getDefaultBagWeight()),
                                    "updatedAt", System.currentTimeMillis()
                                );

                                // Audit Stock Movement
                                DocumentReference smRef = db.collection("stockMovements").document();
                                StockMovement sm = new StockMovement(
                                    prodRef.getId(), product.getName(), product.getVariety(),
                                    StockMovement.TYPE_ADJUSTMENT, -item.getTotalKg(), reversedStockKg,
                                    "চাল ক্রয় বাতিল স্টক সমন্বয় (Memo #" + purchase.getInvoiceNo() + ")"
                                );
                                sm.setUserId(purchase.getUserId());
                                transaction.set(smRef, sm.toMap());
                            }
                        }
                    }
                }
            }

            // 2. Reverse Supplier Payable
            if (suppRef != null && suppSnap != null && suppSnap.exists()) {
                Supplier supplier = suppSnap.toObject(Supplier.class);
                if (supplier != null) {
                    double currentPayable = Math.max(0, supplier.getCurrentPayable() - purchase.getDueAmount());
                    transaction.update(suppRef, 
                        "currentPayable", currentPayable,
                        "updatedAt", System.currentTimeMillis()
                    );
                }
            }

            // 3. Mark Purchase as CANCELLED
            transaction.update(purRef, 
                "purchaseStatus", Purchase.PURCHASE_STATUS_CANCELLED,
                "notes", (purchase.getNotes() != null ? purchase.getNotes() : "") + " [বাতিল: " + reason + "]",
                "updatedAt", System.currentTimeMillis()
            );

            return null;
        }).addOnSuccessListener(result -> callback.onSuccess(null))
        .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void getPurchasesByUser(String userId, FirestoreListCallback<Purchase> callback) {
        db.collection(COLLECTION_PURCHASES)
            .whereEqualTo("userId", userId)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    List<Purchase> purchases = new ArrayList<>();
                    for (DocumentSnapshot doc : task.getResult()) {
                        Purchase p = doc.toObject(Purchase.class);
                        if (p != null) purchases.add(p);
                    }
                    purchases.sort((p1, p2) -> Long.compare(p2.getPurchaseDate(), p1.getPurchaseDate()));
                    callback.onSuccess(purchases);
                } else {
                    callback.onFailure(task.getException() != null ? task.getException().getMessage() : "Failed to get purchases");
                }
            });
    }

    // Sale Workflow
    public void createSale(Sale sale, FirestoreCallback<String> callback) {
        DocumentReference docRef = db.collection(COLLECTION_SALES).document();
        sale.setId(docRef.getId());
        if (sale.getInvoiceNo() == null || sale.getInvoiceNo().isEmpty()) {
            sale.setInvoiceNo("INV-" + System.currentTimeMillis() / 1000);
        }
        sale.setCreatedAt(System.currentTimeMillis());
        sale.setUpdatedAt(System.currentTimeMillis());

        db.runTransaction(transaction -> {
            // STEP 1: ALL READS FIRST (No writes allowed here!)
            // 1. Read Product Snapshots
            Map<String, DocumentSnapshot> prodSnapMap = new HashMap<>();
            if (sale.getItems() != null) {
                for (SaleItem item : sale.getItems()) {
                    if (item.getProductId() != null && !item.getProductId().isEmpty()) {
                        DocumentReference prodRef = db.collection(COLLECTION_RICE_PRODUCTS).document(item.getProductId());
                        prodSnapMap.put(item.getProductId(), transaction.get(prodRef));
                    }
                }
            }

            // 2. Read Customer Snapshot if non-cash customer
            DocumentSnapshot custSnap = null;
            DocumentReference custRef = null;
            if (sale.getCustomerId() != null && !Sale.CASH_CUSTOMER_ID.equals(sale.getCustomerId())) {
                custRef = db.collection(COLLECTION_CUSTOMERS).document(sale.getCustomerId());
                custSnap = transaction.get(custRef);
            }

            // 3. Read Wallet Account Document if paidAmount > 0
            DocumentSnapshot walletAccSnap = null;
            DocumentReference walletAccRef = null;
            if (sale.getPaidAmount() > 0 && sale.getUserId() != null) {
                String targetAccId = resolveAccountIdForMethod(sale.getPaymentMethod(), null);
                walletAccRef = db.collection(COLLECTION_USERS)
                    .document(sale.getUserId())
                    .collection(COLLECTION_WALLET_ACCOUNTS)
                    .document(targetAccId);
                walletAccSnap = transaction.get(walletAccRef);
            }

            // STEP 2: ALL WRITES AFTER (No reads allowed here!)
            // 1. Update Wallet Balance & Log Wallet Transaction if paidAmount > 0
            if (walletAccRef != null && walletAccSnap != null && walletAccSnap.exists()) {
                WalletAccount walletAcc = walletAccSnap.toObject(WalletAccount.class);
                if (walletAcc != null) {
                    double oldBal = walletAcc.getCurrentBalance();
                    double newBal = oldBal + sale.getPaidAmount();
                    transaction.update(walletAccRef, "currentBalance", newBal, "updatedAt", System.currentTimeMillis());

                    DocumentReference wtRef = db.collection(COLLECTION_USERS)
                        .document(sale.getUserId())
                        .collection(COLLECTION_WALLET_TRANSACTIONS)
                        .document();

                    WalletTransaction wt = new WalletTransaction();
                    wt.setTransactionId(wtRef.getId());
                    wt.setAccountId(walletAcc.getAccountId());
                    wt.setAccountName(walletAcc.getAccountName());
                    wt.setUserId(sale.getUserId());
                    wt.setType(WalletTransaction.TYPE_CUSTOMER_PAYMENT);
                    wt.setDirection(WalletTransaction.DIRECTION_IN);
                    wt.setCategory("Sale Payment");
                    wt.setAmount(sale.getPaidAmount());
                    wt.setBalanceBefore(oldBal);
                    wt.setBalanceAfter(newBal);
                    wt.setTitle("কাস্টমার থেকে টাকা জমা (" + sale.getCustomerName() + ")");
                    wt.setSaleId(sale.getId());
                    wt.setCustomerId(sale.getCustomerId());
                    wt.setCustomerName(sale.getCustomerName());
                    wt.setPaymentMethod(walletAcc.getAccountName());
                    wt.setReference(sale.getInvoiceNo());
                    wt.setCreatedAt(System.currentTimeMillis());
                    wt.setTransactionDate(sale.getSaleDate() > 0 ? sale.getSaleDate() : System.currentTimeMillis());

                    transaction.set(wtRef, wt.toMap());
                }
            }

            // 2. Stock Check & Updates
            if (sale.getItems() != null) {
                for (SaleItem item : sale.getItems()) {
                    if (item.getProductId() != null && prodSnapMap.containsKey(item.getProductId())) {
                        DocumentReference prodRef = db.collection(COLLECTION_RICE_PRODUCTS).document(item.getProductId());
                        DocumentSnapshot prodSnap = prodSnapMap.get(item.getProductId());
                        if (prodSnap != null && prodSnap.exists()) {
                            RiceProduct product = prodSnap.toObject(RiceProduct.class);
                            if (product != null) {
                                double availableStockKg = product.getCurrentStockKg();
                                if (item.getTotalKg() > availableStockKg) {
                                    throw new FirebaseFirestoreException(
                                        "স্টকে পর্যাপ্ত চাল নেই! " + item.getProductNameSnapshot() + " এর বর্তমান স্টক " + UnitConverterHelper.formatKg(availableStockKg),
                                        FirebaseFirestoreException.Code.ABORTED
                                    );
                                }
                                double newStockKg = availableStockKg - item.getTotalKg();
                                transaction.update(prodRef, 
                                    "currentStockKg", newStockKg,
                                    "currentStockBags", UnitConverterHelper.kgToBags(newStockKg, product.getDefaultBagWeight()),
                                    "updatedAt", System.currentTimeMillis()
                                );

                                // Audit Stock Movement
                                DocumentReference smRef = db.collection("stockMovements").document();
                                StockMovement sm = new StockMovement(
                                    prodRef.getId(), product.getName(), product.getVariety(),
                                    StockMovement.TYPE_SALE, -item.getTotalKg(), newStockKg,
                                    "চাল বিক্রি (Invoice #" + sale.getInvoiceNo() + ")"
                                );
                                sm.setUserId(sale.getUserId());
                                transaction.set(smRef, sm.toMap());
                            }
                        }
                    }
                }
            }

            // 3. Save Sale Document
            transaction.set(docRef, sale.toMap());

            // 3. Update Customer Baki & Ledger
            if (custRef != null && custSnap != null && custSnap.exists()) {
                Customer customer = custSnap.toObject(Customer.class);
                if (customer != null) {
                    double newBaki = customer.getBaki() + sale.getDueAmount();
                    transaction.update(custRef, 
                        "baki", newBaki,
                        "updatedAt", System.currentTimeMillis()
                    );

                    // Ledger Entry
                    DocumentReference clRef = db.collection("customerLedger").document();
                    CustomerLedger ledger = new CustomerLedger(
                        sale.getCustomerId(), CustomerLedger.TYPE_DEBIT_SALE, sale.getGrandTotal(),
                        sale.getPaidAmount(), newBaki, "চাল বিক্রি (Memo #" + sale.getInvoiceNo() + ")"
                    );
                    ledger.setUserId(sale.getUserId());
                    transaction.set(clRef, ledger.toMap());
                }
            }

            // 4. Create Legacy Transaction Document for Backward Compatibility
            DocumentReference legacyTxRef = db.collection(COLLECTION_TRANSACTIONS).document();
            Transaction legacyTx = new Transaction(
                sale.getCustomerId(),
                sale.getCustomerName(),
                sale.getDueAmount() > 0 ? Transaction.TYPE_BAKI : Transaction.TYPE_PAYMENT,
                sale.getGrandTotal(),
                sale.getNotes() != null && !sale.getNotes().isEmpty() ? sale.getNotes() : "চাল বিক্রি #" + sale.getInvoiceNo()
            );
            legacyTx.setId(legacyTxRef.getId());
            legacyTx.setUserId(sale.getUserId());
            legacyTx.setDate(sale.getSaleDate());
            transaction.set(legacyTxRef, legacyTx.toMap());

            return sale.getId();
        }).addOnSuccessListener(callback::onSuccess)
        .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void cancelSale(String saleId, String reason, FirestoreCallback<Void> callback) {
        DocumentReference saleRef = db.collection(COLLECTION_SALES).document(saleId);

        db.runTransaction(transaction -> {
            // STEP 1: ALL READS FIRST
            DocumentSnapshot saleSnap = transaction.get(saleRef);
            if (!saleSnap.exists()) {
                throw new FirebaseFirestoreException("চাল বিক্রি তথ্য পাওয়া যায়নি", FirebaseFirestoreException.Code.NOT_FOUND);
            }

            Sale sale = saleSnap.toObject(Sale.class);
            if (sale == null || Sale.SALE_STATUS_CANCELLED.equals(sale.getSaleStatus())) {
                throw new FirebaseFirestoreException("চাল বিক্রি ইতিমধ্যেই বাতিল করা হয়েছে", FirebaseFirestoreException.Code.ALREADY_EXISTS);
            }

            Map<String, DocumentSnapshot> prodSnapMap = new HashMap<>();
            if (sale.getItems() != null) {
                for (SaleItem item : sale.getItems()) {
                    if (item.getProductId() != null && !item.getProductId().isEmpty()) {
                        DocumentReference prodRef = db.collection(COLLECTION_RICE_PRODUCTS).document(item.getProductId());
                        prodSnapMap.put(item.getProductId(), transaction.get(prodRef));
                    }
                }
            }

            DocumentSnapshot custSnap = null;
            DocumentReference custRef = null;
            if (sale.getCustomerId() != null && !Sale.CASH_CUSTOMER_ID.equals(sale.getCustomerId()) && sale.getDueAmount() > 0) {
                custRef = db.collection(COLLECTION_CUSTOMERS).document(sale.getCustomerId());
                custSnap = transaction.get(custRef);
            }

            // STEP 2: ALL WRITES AFTER
            // 1. Restore Stock in KG
            if (sale.getItems() != null) {
                for (SaleItem item : sale.getItems()) {
                    if (item.getProductId() != null && prodSnapMap.containsKey(item.getProductId())) {
                        DocumentReference prodRef = db.collection(COLLECTION_RICE_PRODUCTS).document(item.getProductId());
                        DocumentSnapshot prodSnap = prodSnapMap.get(item.getProductId());
                        if (prodSnap != null && prodSnap.exists()) {
                            RiceProduct product = prodSnap.toObject(RiceProduct.class);
                            if (product != null) {
                                double restoredStockKg = product.getCurrentStockKg() + item.getTotalKg();
                                transaction.update(prodRef, 
                                    "currentStockKg", restoredStockKg,
                                    "currentStockBags", UnitConverterHelper.kgToBags(restoredStockKg, product.getDefaultBagWeight()),
                                    "updatedAt", System.currentTimeMillis()
                                );

                                // Audit Stock Movement
                                DocumentReference smRef = db.collection("stockMovements").document();
                                StockMovement sm = new StockMovement(
                                    prodRef.getId(), product.getName(), product.getVariety(),
                                    StockMovement.TYPE_ADJUSTMENT, item.getTotalKg(), restoredStockKg,
                                    "বিক্রি বাতিল স্টক ফেরত (Invoice #" + sale.getInvoiceNo() + ")"
                                );
                                sm.setUserId(sale.getUserId());
                                transaction.set(smRef, sm.toMap());
                            }
                        }
                    }
                }
            }

            // 2. Reverse Customer Due
            if (custRef != null && custSnap != null && custSnap.exists()) {
                Customer customer = custSnap.toObject(Customer.class);
                if (customer != null) {
                    double reversedBaki = Math.max(0, customer.getBaki() - sale.getDueAmount());
                    transaction.update(custRef, 
                        "baki", reversedBaki,
                        "updatedAt", System.currentTimeMillis()
                    );
                }
            }

            // 3. Mark Sale as CANCELLED
            transaction.update(saleRef, 
                "saleStatus", Sale.SALE_STATUS_CANCELLED,
                "notes", (sale.getNotes() != null ? sale.getNotes() : "") + " [বাতিল: " + reason + "]",
                "updatedAt", System.currentTimeMillis()
            );

            return null;
        }).addOnSuccessListener(result -> callback.onSuccess(null))
        .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void getSalesByUser(String userId, FirestoreListCallback<Sale> callback) {
        db.collection(COLLECTION_SALES)
            .whereEqualTo("userId", userId)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    List<Sale> sales = new ArrayList<>();
                    for (DocumentSnapshot doc : task.getResult()) {
                        Sale s = doc.toObject(Sale.class);
                        if (s != null) sales.add(s);
                    }
                    sales.sort((s1, s2) -> Long.compare(s2.getSaleDate(), s1.getSaleDate()));
                    callback.onSuccess(sales);
                } else {
                    callback.onFailure(task.getException() != null ? task.getException().getMessage() : "Failed to get sales");
                }
            });
    }

    // Expense & Cash Flow Methods
    public void saveCashFlowExpense(Expense expense, Transaction transactionObj, FirestoreCallback<String> callback) {
        DocumentReference expRef = db.collection(COLLECTION_EXPENSES).document();
        expense.setId(expRef.getId());
        expense.setCreatedAt(System.currentTimeMillis());

        DocumentReference txRef = db.collection(COLLECTION_TRANSACTIONS).document();
        if (transactionObj != null) {
            transactionObj.setId(txRef.getId());
            transactionObj.setCreatedAt(System.currentTimeMillis());
        }

        db.runTransaction(transaction -> {
            // STEP 1: ALL READS FIRST
            DocumentSnapshot walletAccSnap = null;
            DocumentReference walletAccRef = null;
            if (expense.getAmount() > 0 && expense.getUserId() != null) {
                String targetAccId = resolveAccountIdForMethod(expense.getPaymentMethod(), null);
                walletAccRef = db.collection(COLLECTION_USERS)
                    .document(expense.getUserId())
                    .collection(COLLECTION_WALLET_ACCOUNTS)
                    .document(targetAccId);
                walletAccSnap = transaction.get(walletAccRef);
            }

            // STEP 2: ALL WRITES AFTER
            if (walletAccRef != null && walletAccSnap != null && walletAccSnap.exists()) {
                WalletAccount walletAcc = walletAccSnap.toObject(WalletAccount.class);
                if (walletAcc != null) {
                    double oldBal = walletAcc.getCurrentBalance();
                    if (oldBal < expense.getAmount()) {
                        throw new FirebaseFirestoreException(
                            "এই অ্যাকাউন্টে (" + walletAcc.getAccountName() + ") পর্যাপ্ত টাকা নেই। (বর্তমান ব্যালেন্স: ৳" + String.format("%.0f", oldBal) + ", খরচ: ৳" + String.format("%.0f", expense.getAmount()) + ")",
                            FirebaseFirestoreException.Code.ABORTED
                        );
                    }
                    double newBal = oldBal - expense.getAmount();
                    transaction.update(walletAccRef, "currentBalance", newBal, "updatedAt", System.currentTimeMillis());

                    DocumentReference wtRef = db.collection(COLLECTION_USERS)
                        .document(expense.getUserId())
                        .collection(COLLECTION_WALLET_TRANSACTIONS)
                        .document();

                    WalletTransaction wt = new WalletTransaction();
                    wt.setTransactionId(wtRef.getId());
                    wt.setAccountId(walletAcc.getAccountId());
                    wt.setAccountName(walletAcc.getAccountName());
                    wt.setUserId(expense.getUserId());
                    wt.setType(WalletTransaction.TYPE_EXPENSE);
                    wt.setDirection(WalletTransaction.DIRECTION_OUT);
                    wt.setCategory(expense.getCategory() != null ? expense.getCategory() : "Expense");
                    wt.setAmount(expense.getAmount());
                    wt.setBalanceBefore(oldBal);
                    wt.setBalanceAfter(newBal);
                    wt.setTitle("ব্যবসার খরচ: " + expense.getCategory());
                    wt.setDescription(expense.getDescription());
                    wt.setExpenseId(expense.getId());
                    wt.setPaymentMethod(walletAcc.getAccountName());
                    wt.setCreatedAt(System.currentTimeMillis());
                    wt.setTransactionDate(expense.getDate() > 0 ? expense.getDate() : System.currentTimeMillis());

                    transaction.set(wtRef, wt.toMap());
                }
            }

            transaction.set(expRef, expense.toMap());
            if (transactionObj != null) {
                transaction.set(txRef, transactionObj.toMap());
            }
            return expense.getId();
        }).addOnSuccessListener(callback::onSuccess)
        .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void saveCashFlowReceive(Transaction tx, String customerId, String supplierId, FirestoreCallback<String> callback) {
        DocumentReference txRef = db.collection(COLLECTION_TRANSACTIONS).document();
        tx.setId(txRef.getId());
        if (tx.getCreatedAt() <= 0) tx.setCreatedAt(System.currentTimeMillis());

        db.runTransaction(transaction -> {
            // STEP 1: ALL READS FIRST
            DocumentSnapshot custSnap = null;
            DocumentReference custRef = null;
            if (customerId != null && !customerId.isEmpty() && !Sale.CASH_CUSTOMER_ID.equals(customerId)) {
                custRef = db.collection(COLLECTION_CUSTOMERS).document(customerId);
                custSnap = transaction.get(custRef);
            }

            DocumentSnapshot suppSnap = null;
            DocumentReference suppRef = null;
            if (supplierId != null && !supplierId.isEmpty()) {
                suppRef = db.collection(COLLECTION_SUPPLIERS).document(supplierId);
                suppSnap = transaction.get(suppRef);
            }

            DocumentSnapshot walletAccSnap = null;
            DocumentReference walletAccRef = null;
            if (tx.getAmount() > 0 && tx.getUserId() != null) {
                String targetAccId = resolveAccountIdForMethod(tx.getPaymentMethod(), null);
                walletAccRef = db.collection(COLLECTION_USERS)
                    .document(tx.getUserId())
                    .collection(COLLECTION_WALLET_ACCOUNTS)
                    .document(targetAccId);
                walletAccSnap = transaction.get(walletAccRef);
            }

            // STEP 2: ALL WRITES AFTER
            if (walletAccRef != null && walletAccSnap != null && walletAccSnap.exists()) {
                WalletAccount walletAcc = walletAccSnap.toObject(WalletAccount.class);
                if (walletAcc != null) {
                    double oldBal = walletAcc.getCurrentBalance();
                    double newBal = oldBal + tx.getAmount();
                    transaction.update(walletAccRef, "currentBalance", newBal, "updatedAt", System.currentTimeMillis());

                    DocumentReference wtRef = db.collection(COLLECTION_USERS)
                        .document(tx.getUserId())
                        .collection(COLLECTION_WALLET_TRANSACTIONS)
                        .document();

                    WalletTransaction wt = new WalletTransaction();
                    wt.setTransactionId(wtRef.getId());
                    wt.setAccountId(walletAcc.getAccountId());
                    wt.setAccountName(walletAcc.getAccountName());
                    wt.setUserId(tx.getUserId());
                    wt.setType(tx.getType() != null ? tx.getType() : WalletTransaction.TYPE_CUSTOMER_PAYMENT);
                    wt.setDirection(WalletTransaction.DIRECTION_IN);
                    wt.setCategory("Money Receive");
                    wt.setAmount(tx.getAmount());
                    wt.setBalanceBefore(oldBal);
                    wt.setBalanceAfter(newBal);
                    wt.setTitle(tx.getNote() != null && !tx.getNote().isEmpty() ? tx.getNote() : "টাকা জমা");
                    wt.setCustomerId(customerId);
                    wt.setCustomerName(tx.getCustomerName());
                    wt.setSupplierId(supplierId);
                    wt.setSupplierName(tx.getSupplierName());
                    wt.setPaymentMethod(walletAcc.getAccountName());
                    wt.setCreatedAt(System.currentTimeMillis());
                    wt.setTransactionDate(tx.getDate() > 0 ? tx.getDate() : System.currentTimeMillis());

                    transaction.set(wtRef, wt.toMap());
                }
            }

            // 1. Customer due update & ledger
            if (custRef != null && custSnap != null && custSnap.exists()) {
                Customer customer = custSnap.toObject(Customer.class);
                if (customer != null) {
                    double currentBaki = customer.getBaki();
                    double newBaki = Math.max(0, currentBaki - tx.getAmount());
                    tx.setPreviousBaki(currentBaki);
                    tx.setNewBaki(newBaki);

                    transaction.update(custRef, "baki", newBaki, "updatedAt", System.currentTimeMillis());

                    // Customer Ledger
                    DocumentReference clRef = db.collection("customerLedger").document();
                    CustomerLedger ledger = new CustomerLedger(
                        customerId, CustomerLedger.TYPE_PAYMENT, 0, tx.getAmount(), newBaki,
                        tx.getNote() != null && !tx.getNote().isEmpty() ? tx.getNote() : "টাকা জমা (Payment Received)"
                    );
                    ledger.setUserId(tx.getUserId());
                    transaction.set(clRef, ledger.toMap());
                }
            }

            // 2. Supplier refund update & ledger
            if (suppRef != null && suppSnap != null && suppSnap.exists()) {
                Supplier supplier = suppSnap.toObject(Supplier.class);
                if (supplier != null) {
                    double currentPayable = supplier.getCurrentPayable();
                    double newPayable = Math.max(0, currentPayable - tx.getAmount());

                    transaction.update(suppRef, "currentPayable", newPayable, "updatedAt", System.currentTimeMillis());

                    // Supplier Ledger
                    DocumentReference slRef = db.collection("supplierLedger").document();
                    SupplierLedger ledger = new SupplierLedger(
                        supplierId, "REFUND", tx.getAmount(), 0, newPayable,
                        tx.getNote() != null && !tx.getNote().isEmpty() ? tx.getNote() : "মহাজন রিফান্ড (Supplier Refund)"
                    );
                    ledger.setUserId(tx.getUserId());
                    transaction.set(slRef, ledger.toMap());
                }
            }

            // 3. Save main transaction document
            transaction.set(txRef, tx.toMap());

            return tx.getId();
        }).addOnSuccessListener(callback::onSuccess)
        .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void createExpense(Expense expense, FirestoreCallback<String> callback) {
        DocumentReference docRef = db.collection(COLLECTION_EXPENSES).document();
        expense.setId(docRef.getId());
        expense.setCreatedAt(System.currentTimeMillis());

        docRef.set(expense.toMap())
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    callback.onSuccess(expense.getId());
                } else {
                    callback.onFailure(task.getException() != null ? task.getException().getMessage() : "Failed to create expense");
                }
            });
    }

    public void getExpensesByUser(String userId, FirestoreListCallback<Expense> callback) {
        db.collection(COLLECTION_EXPENSES)
            .whereEqualTo("userId", userId)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    List<Expense> expenses = new ArrayList<>();
                    for (DocumentSnapshot doc : task.getResult()) {
                        Expense e = doc.toObject(Expense.class);
                        if (e != null) expenses.add(e);
                    }
                    expenses.sort((e1, e2) -> Long.compare(e2.getDate(), e1.getDate()));
                    callback.onSuccess(expenses);
                } else {
                    callback.onFailure(task.getException() != null ? task.getException().getMessage() : "Failed to get expenses");
                }
            });
    }

    // =========================================================================
    // BUSINESS WALLET & CASH ACCOUNT METHODS
    // =========================================================================

    public static String resolveAccountIdForMethod(String paymentMethod, String accountId) {
        if (accountId != null && !accountId.isEmpty()) {
            return accountId;
        }
        if (paymentMethod == null) return "account_cash";
        String pm = paymentMethod.trim().toLowerCase();
        if (pm.contains("bkash")) return "account_bkash";
        if (pm.contains("nagad")) return "account_nagad";
        if (pm.contains("bank")) return "account_bank";
        return "account_cash";
    }

    public void ensureDefaultWalletAccounts(String userId, FirestoreListCallback<WalletAccount> callback) {
        if (userId == null || userId.isEmpty()) {
            callback.onFailure("User ID is null");
            return;
        }

        CollectionReference accountsRef = db.collection(COLLECTION_USERS)
            .document(userId)
            .collection(COLLECTION_WALLET_ACCOUNTS);

        accountsRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                List<WalletAccount> existingAccounts = new ArrayList<>();
                Map<String, WalletAccount> accountMap = new HashMap<>();

                for (DocumentSnapshot doc : task.getResult()) {
                    WalletAccount acc = doc.toObject(WalletAccount.class);
                    if (acc != null) {
                        if (acc.getAccountId() == null) acc.setAccountId(doc.getId());
                        existingAccounts.add(acc);
                        accountMap.put(acc.getAccountId(), acc);
                        if (acc.getAccountType() != null) {
                            accountMap.put(acc.getAccountType().toUpperCase(), acc);
                        }
                    }
                }

                List<WalletAccount> defaultsToCreate = new ArrayList<>();
                if (!accountMap.containsKey("account_cash") && !accountMap.containsKey(WalletAccount.TYPE_CASH)) {
                    defaultsToCreate.add(new WalletAccount("account_cash", "Cash", WalletAccount.TYPE_CASH, 0, 0, userId));
                }
                if (!accountMap.containsKey("account_bkash") && !accountMap.containsKey(WalletAccount.TYPE_BKASH)) {
                    defaultsToCreate.add(new WalletAccount("account_bkash", "bKash", WalletAccount.TYPE_BKASH, 0, 0, userId));
                }
                if (!accountMap.containsKey("account_nagad") && !accountMap.containsKey(WalletAccount.TYPE_NAGAD)) {
                    defaultsToCreate.add(new WalletAccount("account_nagad", "Nagad", WalletAccount.TYPE_NAGAD, 0, 0, userId));
                }
                if (!accountMap.containsKey("account_bank") && !accountMap.containsKey(WalletAccount.TYPE_BANK)) {
                    defaultsToCreate.add(new WalletAccount("account_bank", "Bank", WalletAccount.TYPE_BANK, 0, 0, userId));
                }

                if (defaultsToCreate.isEmpty()) {
                    callback.onSuccess(existingAccounts);
                } else {
                    com.google.firebase.firestore.WriteBatch batch = db.batch();
                    for (WalletAccount newAcc : defaultsToCreate) {
                        DocumentReference docRef = accountsRef.document(newAcc.getAccountId());
                        batch.set(docRef, newAcc.toMap());
                        existingAccounts.add(newAcc);
                    }
                    batch.commit().addOnCompleteListener(batchTask -> {
                        if (batchTask.isSuccessful()) {
                            callback.onSuccess(existingAccounts);
                        } else {
                            callback.onFailure(batchTask.getException() != null ? batchTask.getException().getMessage() : "Failed to create default accounts");
                        }
                    });
                }
            } else {
                callback.onFailure(task.getException() != null ? task.getException().getMessage() : "Failed to query wallet accounts");
            }
        });
    }

    public void getWalletAccounts(String userId, FirestoreListCallback<WalletAccount> callback) {
        ensureDefaultWalletAccounts(userId, new FirestoreListCallback<WalletAccount>() {
            @Override
            public void onSuccess(List<WalletAccount> result) {
                result.sort((a, b) -> Long.compare(a.getCreatedAt(), b.getCreatedAt()));
                callback.onSuccess(result);
            }

            @Override
            public void onFailure(String error) {
                callback.onFailure(error);
            }
        });
    }

    public void getActiveWalletAccounts(String userId, FirestoreListCallback<WalletAccount> callback) {
        getWalletAccounts(userId, new FirestoreListCallback<WalletAccount>() {
            @Override
            public void onSuccess(List<WalletAccount> result) {
                List<WalletAccount> activeAccounts = new ArrayList<>();
                for (WalletAccount acc : result) {
                    if (acc.isActive()) {
                        activeAccounts.add(acc);
                    }
                }
                callback.onSuccess(activeAccounts);
            }

            @Override
            public void onFailure(String error) {
                callback.onFailure(error);
            }
        });
    }

    public void createWalletAccount(String userId, WalletAccount account, FirestoreCallback<String> callback) {
        DocumentReference docRef = db.collection(COLLECTION_USERS)
            .document(userId)
            .collection(COLLECTION_WALLET_ACCOUNTS)
            .document();
        account.setAccountId(docRef.getId());
        account.setUserId(userId);
        account.setCreatedAt(System.currentTimeMillis());
        account.setUpdatedAt(System.currentTimeMillis());
        account.setCurrentBalance(account.getOpeningBalance());

        db.runTransaction(transaction -> {
            transaction.set(docRef, account.toMap());

            if (account.getOpeningBalance() > 0) {
                DocumentReference txRef = db.collection(COLLECTION_USERS)
                    .document(userId)
                    .collection(COLLECTION_WALLET_TRANSACTIONS)
                    .document();
                WalletTransaction wt = new WalletTransaction();
                wt.setTransactionId(txRef.getId());
                wt.setAccountId(account.getAccountId());
                wt.setAccountName(account.getAccountName());
                wt.setUserId(userId);
                wt.setType(WalletTransaction.TYPE_OPENING_BALANCE);
                wt.setDirection(WalletTransaction.DIRECTION_IN);
                wt.setCategory("Opening Balance");
                wt.setAmount(account.getOpeningBalance());
                wt.setBalanceBefore(0);
                wt.setBalanceAfter(account.getOpeningBalance());
                wt.setTitle("প্রারম্ভিক ব্যালেন্স (Opening Balance)");
                wt.setDescription("অ্যাকাউন্ট তৈরির প্রারম্ভিক ব্যালেন্স");
                wt.setPaymentMethod(account.getAccountName());
                wt.setCreatedAt(System.currentTimeMillis());
                wt.setTransactionDate(System.currentTimeMillis());

                transaction.set(txRef, wt.toMap());
            }
            return account.getAccountId();
        }).addOnSuccessListener(callback::onSuccess)
        .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void setOpeningBalances(String userId, Map<String, Double> balances, FirestoreCallback<Void> callback) {
        ensureDefaultWalletAccounts(userId, new FirestoreListCallback<WalletAccount>() {
            @Override
            public void onSuccess(List<WalletAccount> accounts) {
                db.runTransaction(transaction -> {
                    long now = System.currentTimeMillis();
                    for (WalletAccount acc : accounts) {
                        String accId = acc.getAccountId();
                        Double openingVal = balances.get(accId);
                        if (openingVal == null && acc.getAccountType() != null) {
                            openingVal = balances.get(acc.getAccountType().toUpperCase());
                        }
                        if (openingVal != null && openingVal > 0) {
                            DocumentReference accRef = db.collection(COLLECTION_USERS)
                                .document(userId)
                                .collection(COLLECTION_WALLET_ACCOUNTS)
                                .document(accId);

                            DocumentSnapshot accSnap = transaction.get(accRef);
                            double current = accSnap.exists() && accSnap.getDouble("currentBalance") != null ? accSnap.getDouble("currentBalance") : 0.0;
                            double newOpening = openingVal;
                            double newCurrent = current + newOpening;

                            transaction.update(accRef,
                                "openingBalance", newOpening,
                                "currentBalance", newCurrent,
                                "updatedAt", now
                            );

                            DocumentReference txRef = db.collection(COLLECTION_USERS)
                                .document(userId)
                                .collection(COLLECTION_WALLET_TRANSACTIONS)
                                .document();
                            WalletTransaction wt = new WalletTransaction();
                            wt.setTransactionId(txRef.getId());
                            wt.setAccountId(accId);
                            wt.setAccountName(acc.getAccountName());
                            wt.setUserId(userId);
                            wt.setType(WalletTransaction.TYPE_OPENING_BALANCE);
                            wt.setDirection(WalletTransaction.DIRECTION_IN);
                            wt.setCategory("Opening Balance");
                            wt.setAmount(newOpening);
                            wt.setBalanceBefore(current);
                            wt.setBalanceAfter(newCurrent);
                            wt.setTitle("প্রারম্ভিক ব্যালেন্স (Opening Balance)");
                            wt.setDescription("অ্যাকাউন্ট সেটিংস প্রারম্ভিক ব্যালেন্স");
                            wt.setPaymentMethod(acc.getAccountName());
                            wt.setCreatedAt(now);
                            wt.setTransactionDate(now);

                            transaction.set(txRef, wt.toMap());
                        }
                    }
                    return null;
                }).addOnSuccessListener(res -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
            }

            @Override
            public void onFailure(String error) {
                callback.onFailure(error);
            }
        });
    }

    public void updateWalletAccount(String userId, WalletAccount account, FirestoreCallback<Void> callback) {
        account.setUpdatedAt(System.currentTimeMillis());
        db.collection(COLLECTION_USERS)
            .document(userId)
            .collection(COLLECTION_WALLET_ACCOUNTS)
            .document(account.getAccountId())
            .update(account.toMap())
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) callback.onSuccess(null);
                else callback.onFailure(task.getException() != null ? task.getException().getMessage() : "Failed to update account");
            });
    }

    public void toggleWalletAccountStatus(String userId, String accountId, boolean isActive, FirestoreCallback<Void> callback) {
        db.collection(COLLECTION_USERS)
            .document(userId)
            .collection(COLLECTION_WALLET_ACCOUNTS)
            .document(accountId)
            .update("isActive", isActive, "updatedAt", System.currentTimeMillis())
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) callback.onSuccess(null);
                else callback.onFailure(task.getException() != null ? task.getException().getMessage() : "Failed to update account status");
            });
    }

    public void deleteWalletAccount(String userId, String accountId, FirestoreCallback<Void> callback) {
        db.collection(COLLECTION_USERS)
            .document(userId)
            .collection(COLLECTION_WALLET_ACCOUNTS)
            .document(accountId)
            .delete()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) callback.onSuccess(null);
                else callback.onFailure(task.getException() != null ? task.getException().getMessage() : "Failed to delete account");
            });
    }

    public void executeManualCashIn(String userId, String accountId, double amount, String category, String notes, String reference, long transactionDate, FirestoreCallback<String> callback) {
        String targetAccountId = resolveAccountIdForMethod(null, accountId);
        DocumentReference accRef = db.collection(COLLECTION_USERS)
            .document(userId)
            .collection(COLLECTION_WALLET_ACCOUNTS)
            .document(targetAccountId);

        DocumentReference txRef = db.collection(COLLECTION_USERS)
            .document(userId)
            .collection(COLLECTION_WALLET_TRANSACTIONS)
            .document();

        db.runTransaction(transaction -> {
            DocumentSnapshot accSnap = transaction.get(accRef);
            if (!accSnap.exists()) {
                throw new FirebaseFirestoreException("অ্যাকাউন্ট পাওয়া যায়নি", FirebaseFirestoreException.Code.NOT_FOUND);
            }
            WalletAccount account = accSnap.toObject(WalletAccount.class);
            if (account == null) {
                throw new FirebaseFirestoreException("অ্যাকাউন্ট ডেটা পাওয়া যায়নি", FirebaseFirestoreException.Code.NOT_FOUND);
            }

            double oldBalance = account.getCurrentBalance();
            double newBalance = oldBalance + amount;

            transaction.update(accRef, "currentBalance", newBalance, "updatedAt", System.currentTimeMillis());

            WalletTransaction wt = new WalletTransaction();
            wt.setTransactionId(txRef.getId());
            wt.setAccountId(account.getAccountId());
            wt.setAccountName(account.getAccountName());
            wt.setUserId(userId);
            wt.setType(WalletTransaction.TYPE_MANUAL_CASH_IN);
            wt.setDirection(WalletTransaction.DIRECTION_IN);
            wt.setCategory(category != null && !category.isEmpty() ? category : "Manual Cash In");
            wt.setAmount(amount);
            wt.setBalanceBefore(oldBalance);
            wt.setBalanceAfter(newBalance);
            wt.setTitle(notes != null && !notes.isEmpty() ? notes : "ব্যবসায় টাকা জমা (Cash In)");
            wt.setDescription(notes);
            wt.setReference(reference);
            wt.setPaymentMethod(account.getAccountName());
            wt.setCreatedAt(System.currentTimeMillis());
            wt.setTransactionDate(transactionDate > 0 ? transactionDate : System.currentTimeMillis());

            transaction.set(txRef, wt.toMap());
            return txRef.getId();
        }).addOnSuccessListener(callback::onSuccess)
        .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void executeManualCashOut(String userId, String accountId, double amount, String category, String notes, String reference, long transactionDate, FirestoreCallback<String> callback) {
        String targetAccountId = resolveAccountIdForMethod(null, accountId);
        DocumentReference accRef = db.collection(COLLECTION_USERS)
            .document(userId)
            .collection(COLLECTION_WALLET_ACCOUNTS)
            .document(targetAccountId);

        DocumentReference txRef = db.collection(COLLECTION_USERS)
            .document(userId)
            .collection(COLLECTION_WALLET_TRANSACTIONS)
            .document();

        db.runTransaction(transaction -> {
            DocumentSnapshot accSnap = transaction.get(accRef);
            if (!accSnap.exists()) {
                throw new FirebaseFirestoreException("অ্যাকাউন্ট পাওয়া যায়নি", FirebaseFirestoreException.Code.NOT_FOUND);
            }
            WalletAccount account = accSnap.toObject(WalletAccount.class);
            if (account == null) {
                throw new FirebaseFirestoreException("অ্যাকাউন্ট ডেটা পাওয়া যায়নি", FirebaseFirestoreException.Code.NOT_FOUND);
            }

            double oldBalance = account.getCurrentBalance();
            if (oldBalance < amount) {
                throw new FirebaseFirestoreException("এই অ্যাকাউন্টে (" + account.getAccountName() + ") পর্যাপ্ত টাকা নেই। (বর্তমান ব্যালেন্স: ৳" + String.format("%.0f", oldBalance) + ", প্রয়োজন: ৳" + String.format("%.0f", amount) + ")", FirebaseFirestoreException.Code.ABORTED);
            }
            double newBalance = oldBalance - amount;

            transaction.update(accRef, "currentBalance", newBalance, "updatedAt", System.currentTimeMillis());

            WalletTransaction wt = new WalletTransaction();
            wt.setTransactionId(txRef.getId());
            wt.setAccountId(account.getAccountId());
            wt.setAccountName(account.getAccountName());
            wt.setUserId(userId);
            wt.setType(WalletTransaction.TYPE_MANUAL_CASH_OUT);
            wt.setDirection(WalletTransaction.DIRECTION_OUT);
            wt.setCategory(category != null && !category.isEmpty() ? category : "Manual Cash Out");
            wt.setAmount(amount);
            wt.setBalanceBefore(oldBalance);
            wt.setBalanceAfter(newBalance);
            wt.setTitle(notes != null && !notes.isEmpty() ? notes : "ব্যবসা থেকে টাকা খরচ (Cash Out)");
            wt.setDescription(notes);
            wt.setReference(reference);
            wt.setPaymentMethod(account.getAccountName());
            wt.setCreatedAt(System.currentTimeMillis());
            wt.setTransactionDate(transactionDate > 0 ? transactionDate : System.currentTimeMillis());

            transaction.set(txRef, wt.toMap());
            return txRef.getId();
        }).addOnSuccessListener(callback::onSuccess)
        .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void executeAccountTransfer(String userId, String sourceAccountId, String destAccountId, double amount, String notes, String reference, FirestoreCallback<Void> callback) {
        DocumentReference srcAccRef = db.collection(COLLECTION_USERS)
            .document(userId)
            .collection(COLLECTION_WALLET_ACCOUNTS)
            .document(sourceAccountId);

        DocumentReference destAccRef = db.collection(COLLECTION_USERS)
            .document(userId)
            .collection(COLLECTION_WALLET_ACCOUNTS)
            .document(destAccountId);

        DocumentReference srcTxRef = db.collection(COLLECTION_USERS)
            .document(userId)
            .collection(COLLECTION_WALLET_TRANSACTIONS)
            .document();

        DocumentReference destTxRef = db.collection(COLLECTION_USERS)
            .document(userId)
            .collection(COLLECTION_WALLET_TRANSACTIONS)
            .document();

        db.runTransaction(transaction -> {
            DocumentSnapshot srcSnap = transaction.get(srcAccRef);
            DocumentSnapshot destSnap = transaction.get(destAccRef);

            if (!srcSnap.exists() || !destSnap.exists()) {
                throw new FirebaseFirestoreException("ট্রান্সফারের জন্য অ্যাকাউন্ট পাওয়া যায়নি", FirebaseFirestoreException.Code.NOT_FOUND);
            }

            WalletAccount srcAcc = srcSnap.toObject(WalletAccount.class);
            WalletAccount destAcc = destSnap.toObject(WalletAccount.class);

            if (srcAcc == null || destAcc == null) {
                throw new FirebaseFirestoreException("অ্যাকাউন্ট ডেটা পাওয়া যায়নি", FirebaseFirestoreException.Code.NOT_FOUND);
            }

            double srcOldBalance = srcAcc.getCurrentBalance();
            if (srcOldBalance < amount) {
                throw new FirebaseFirestoreException("উৎস অ্যাকাউন্টে (" + srcAcc.getAccountName() + ") পর্যাপ্ত টাকা নেই। (বর্তমান ব্যালেন্স: ৳" + String.format("%.0f", srcOldBalance) + ", ট্রান্সফার: ৳" + String.format("%.0f", amount) + ")", FirebaseFirestoreException.Code.ABORTED);
            }

            double srcNewBalance = srcOldBalance - amount;
            double destOldBalance = destAcc.getCurrentBalance();
            double destNewBalance = destOldBalance + amount;

            long now = System.currentTimeMillis();

            transaction.update(srcAccRef, "currentBalance", srcNewBalance, "updatedAt", now);
            transaction.update(destAccRef, "currentBalance", destNewBalance, "updatedAt", now);

            String transferPairId = "TRF-" + now;

            // Source Tx (OUT)
            WalletTransaction srcWt = new WalletTransaction();
            srcWt.setTransactionId(srcTxRef.getId());
            srcWt.setAccountId(srcAcc.getAccountId());
            srcWt.setAccountName(srcAcc.getAccountName());
            srcWt.setUserId(userId);
            srcWt.setType(WalletTransaction.TYPE_TRANSFER_OUT);
            srcWt.setDirection(WalletTransaction.DIRECTION_TRANSFER);
            srcWt.setCategory("Transfer");
            srcWt.setAmount(amount);
            srcWt.setBalanceBefore(srcOldBalance);
            srcWt.setBalanceAfter(srcNewBalance);
            srcWt.setTitle("টাকা ট্রান্সফার (" + srcAcc.getAccountName() + " → " + destAcc.getAccountName() + ")");
            srcWt.setDescription(notes != null && !notes.isEmpty() ? notes : srcAcc.getAccountName() + " থেকে " + destAcc.getAccountName() + " এ ট্রান্সফার");
            srcWt.setTransferId(transferPairId);
            srcWt.setReference(reference);
            srcWt.setPaymentMethod(srcAcc.getAccountName());
            srcWt.setCreatedAt(now);
            srcWt.setTransactionDate(now);

            // Dest Tx (IN)
            WalletTransaction destWt = new WalletTransaction();
            destWt.setTransactionId(destTxRef.getId());
            destWt.setAccountId(destAcc.getAccountId());
            destWt.setAccountName(destAcc.getAccountName());
            destWt.setUserId(userId);
            destWt.setType(WalletTransaction.TYPE_TRANSFER_IN);
            destWt.setDirection(WalletTransaction.DIRECTION_TRANSFER);
            destWt.setCategory("Transfer");
            destWt.setAmount(amount);
            destWt.setBalanceBefore(destOldBalance);
            destWt.setBalanceAfter(destNewBalance);
            destWt.setTitle("টাকা গ্রহণ (" + srcAcc.getAccountName() + " → " + destAcc.getAccountName() + ")");
            destWt.setDescription(notes != null && !notes.isEmpty() ? notes : srcAcc.getAccountName() + " থেকে " + destAcc.getAccountName() + " এ ট্রান্সফার");
            destWt.setTransferId(transferPairId);
            destWt.setReference(reference);
            destWt.setPaymentMethod(destAcc.getAccountName());
            destWt.setCreatedAt(now);
            destWt.setTransactionDate(now);

            transaction.set(srcTxRef, srcWt.toMap());
            transaction.set(destTxRef, destWt.toMap());

            return null;
        }).addOnSuccessListener(res -> callback.onSuccess(null))
        .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void getWalletTransactions(String userId, FirestoreListCallback<WalletTransaction> callback) {
        db.collection(COLLECTION_USERS)
            .document(userId)
            .collection(COLLECTION_WALLET_TRANSACTIONS)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    List<WalletTransaction> list = new ArrayList<>();
                    for (DocumentSnapshot doc : task.getResult()) {
                        WalletTransaction wt = doc.toObject(WalletTransaction.class);
                        if (wt != null) {
                            if (wt.getTransactionId() == null) wt.setTransactionId(doc.getId());
                            list.add(wt);
                        }
                    }
                    list.sort((t1, t2) -> Long.compare(t2.getTransactionDate(), t1.getTransactionDate()));
                    callback.onSuccess(list);
                } else {
                    callback.onFailure(task.getException() != null ? task.getException().getMessage() : "Failed to get wallet transactions");
                }
            });
    }

    public void reverseWalletTransaction(String userId, String transactionId, String reason, FirestoreCallback<Void> callback) {
        DocumentReference origTxRef = db.collection(COLLECTION_USERS)
            .document(userId)
            .collection(COLLECTION_WALLET_TRANSACTIONS)
            .document(transactionId);

        DocumentReference reversalTxRef = db.collection(COLLECTION_USERS)
            .document(userId)
            .collection(COLLECTION_WALLET_TRANSACTIONS)
            .document();

        db.runTransaction(transaction -> {
            DocumentSnapshot origSnap = transaction.get(origTxRef);
            if (!origSnap.exists()) {
                throw new FirebaseFirestoreException("লেনদেন তথ্য পাওয়া যায়নি", FirebaseFirestoreException.Code.NOT_FOUND);
            }
            WalletTransaction origWt = origSnap.toObject(WalletTransaction.class);
            if (origWt == null) {
                throw new FirebaseFirestoreException("লেনদেন তথ্য পাওয়া যায়নি", FirebaseFirestoreException.Code.NOT_FOUND);
            }
            if (WalletTransaction.STATUS_REVERSED.equals(origWt.getStatus())) {
                throw new FirebaseFirestoreException("এই লেনদেনটি ইতিমধ্যেই রিভার্স (বাতিল) করা হয়েছে", FirebaseFirestoreException.Code.ALREADY_EXISTS);
            }

            DocumentReference accRef = db.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_WALLET_ACCOUNTS)
                .document(origWt.getAccountId());

            DocumentSnapshot accSnap = transaction.get(accRef);
            if (!accSnap.exists()) {
                throw new FirebaseFirestoreException("সংক্রান্ত অ্যাকাউন্ট পাওয়া যায়নি", FirebaseFirestoreException.Code.NOT_FOUND);
            }
            WalletAccount account = accSnap.toObject(WalletAccount.class);
            if (account == null) {
                throw new FirebaseFirestoreException("অ্যাকাউন্ট পাওয়া যায়নি", FirebaseFirestoreException.Code.NOT_FOUND);
            }

            double oldBalance = account.getCurrentBalance();
            double newBalance;
            String revDirection;

            if (WalletTransaction.DIRECTION_IN.equals(origWt.getDirection())) {
                revDirection = WalletTransaction.DIRECTION_OUT;
                if (oldBalance < origWt.getAmount()) {
                    throw new FirebaseFirestoreException("অ্যাকাউন্টে পর্যাপ্ত ব্যালেন্স না থাকায় রিভার্স করা যাচ্ছে না", FirebaseFirestoreException.Code.ABORTED);
                }
                newBalance = oldBalance - origWt.getAmount();
            } else {
                revDirection = WalletTransaction.DIRECTION_IN;
                newBalance = oldBalance + origWt.getAmount();
            }

            long now = System.currentTimeMillis();

            // Update account balance
            transaction.update(accRef, "currentBalance", newBalance, "updatedAt", now);

            // Mark original transaction REVERSED
            transaction.update(origTxRef,
                "status", WalletTransaction.STATUS_REVERSED,
                "reversalTransactionId", reversalTxRef.getId(),
                "description", (origWt.getDescription() != null ? origWt.getDescription() + " " : "") + "[রিভার্স করা হয়েছে: " + reason + "]"
            );

            // Create Reversal Transaction
            WalletTransaction revWt = new WalletTransaction();
            revWt.setTransactionId(reversalTxRef.getId());
            revWt.setAccountId(account.getAccountId());
            revWt.setAccountName(account.getAccountName());
            revWt.setUserId(userId);
            revWt.setType(WalletTransaction.TYPE_REVERSAL);
            revWt.setDirection(revDirection);
            revWt.setCategory("Reversal");
            revWt.setAmount(origWt.getAmount());
            revWt.setBalanceBefore(oldBalance);
            revWt.setBalanceAfter(newBalance);
            revWt.setTitle("লেনদেন রিভার্স (" + origWt.getTitle() + ")");
            revWt.setDescription("ভুল সংশোধন / বাতিল (কারণ: " + reason + ")");
            revWt.setReference("Reversal of #" + origWt.getTransactionId());
            revWt.setPaymentMethod(account.getAccountName());
            revWt.setCreatedAt(now);
            revWt.setTransactionDate(now);

            transaction.set(reversalTxRef, revWt.toMap());
            return null;
        }).addOnSuccessListener(res -> callback.onSuccess(null))
        .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // Tutorial Video Methods
    public void getPublishedTutorialVideos(FirestoreListCallback<com.sajoldev.hisabniben.model.TutorialVideo> callback) {
        db.collection(COLLECTION_TUTORIAL_VIDEOS)
            .whereEqualTo("isPublished", true)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    List<com.sajoldev.hisabniben.model.TutorialVideo> list = new ArrayList<>();
                    for (DocumentSnapshot document : task.getResult()) {
                        com.sajoldev.hisabniben.model.TutorialVideo video = document.toObject(com.sajoldev.hisabniben.model.TutorialVideo.class);
                        if (video != null) {
                            if (video.getId() == null || video.getId().isEmpty()) {
                                video.setId(document.getId());
                            }
                            list.add(video);
                        }
                    }
                    list.sort((v1, v2) -> {
                        int comp = Integer.compare(v1.getSortOrder(), v2.getSortOrder());
                        if (comp != 0) return comp;
                        return Long.compare(v2.getCreatedAt(), v1.getCreatedAt());
                    });
                    callback.onSuccess(list);
                } else {
                    callback.onFailure(task.getException() != null ? task.getException().getMessage() : "Failed to get tutorial videos");
                }
            });
    }
}


