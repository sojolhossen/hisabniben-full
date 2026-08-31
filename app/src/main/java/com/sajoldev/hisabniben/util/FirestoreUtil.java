package com.sajoldev.hisabniben.util;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

public class FirestoreUtil {

    public static Long getLongOrTimestamp(DocumentSnapshot doc, String fieldName) {
        if (doc == null || fieldName == null || !doc.contains(fieldName)) return null;
        Object val = doc.get(fieldName);
        if (val == null) return null;

        if (val instanceof Number) {
            return ((Number) val).longValue();
        } else if (val instanceof Timestamp) {
            return ((Timestamp) val).toDate().getTime();
        } else if (val instanceof String) {
            try {
                return Long.parseLong((String) val);
            } catch (Exception ignored) {}
        }
        return null;
    }

    public static long getLongOrDefault(DocumentSnapshot doc, String fieldName, long defaultValue) {
        Long val = getLongOrTimestamp(doc, fieldName);
        return val != null ? val : defaultValue;
    }
}
