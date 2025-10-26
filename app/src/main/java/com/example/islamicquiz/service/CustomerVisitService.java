package com.example.islamicquiz.service;

import android.content.Context;

import com.example.islamicquiz.UserSession;
import com.example.islamicquiz.model.Customer;
import com.example.islamicquiz.model.CustomerVisit;
import com.example.islamicquiz.model.User;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * خدمة لتسجيل واستعلام زيارات العملاء
 */
public class CustomerVisitService {
    private static final String TAG = "CustomerVisitService";
    private static final String COLLECTION_NAME = "customer_visits";

    private static CustomerVisitService instance;
    private final FirebaseFirestore db;
    private final Context context;

    private CustomerVisitService(Context context) {
        this.context = context.getApplicationContext();
        this.db = FirebaseFirestore.getInstance();
    }

    public static synchronized CustomerVisitService getInstance(Context context) {
        if (instance == null) {
            instance = new CustomerVisitService(context);
        }
        return instance;
    }

    /**
     * يسجل زيارة واحدة (بائع → عميل) مع الوقت الحالي
     */
    public CompletableFuture<Void> logVisit(Customer customer, String source) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        try {
            UserSession session = UserSession.getInstance(context);
            User currentUser = session != null ? session.getCurrentUser() : null;
            if (currentUser == null) {
                future.complete(null);
                return future;
            }

            CustomerVisit visit = new CustomerVisit(
                    currentUser.getId(),
                    currentUser.getFullName(),
                    customer != null ? customer.getId() : null,
                    customer != null ? customer.getName() : null,
                    Timestamp.now(),
                    source
            );

            DocumentReference docRef = db.collection(COLLECTION_NAME).document();
            visit.setId(docRef.getId());

            docRef.set(visit)
                    .addOnSuccessListener(unused -> future.complete(null))
                    .addOnFailureListener(future::completeExceptionally);
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    /**
     * يجلب عدد الزيارات في نطاق زمني، مع خيار احتساب العملاء الفريدين
     */
    public CompletableFuture<Map<String, Integer>> getVisitsCountForDay(Timestamp start, Timestamp end, String sellerId) {
        CompletableFuture<Map<String, Integer>> future = new CompletableFuture<>();
        Query query = db.collection(COLLECTION_NAME)
                .whereGreaterThanOrEqualTo("timestamp", start)
                .whereLessThanOrEqualTo("timestamp", end);
        if (sellerId != null) {
            query = query.whereEqualTo("sellerId", sellerId);
        }
        query.get()
                .addOnSuccessListener(snapshot -> future.complete(countFromSnapshot(snapshot)))
                .addOnFailureListener(future::completeExceptionally);
        return future;
    }

    private Map<String, Integer> countFromSnapshot(QuerySnapshot snapshot) {
        Map<String, Integer> result = new HashMap<>();
        result.put("total", snapshot != null ? snapshot.size() : 0);
        java.util.Set<String> uniqueCustomers = new java.util.HashSet<>();
        if (snapshot != null) {
            snapshot.getDocuments().forEach(doc -> {
                String customerId = doc.getString("customerId");
                if (customerId != null) uniqueCustomers.add(customerId);
            });
        }
        result.put("uniqueCustomers", uniqueCustomers.size());
        return result;
    }
}

