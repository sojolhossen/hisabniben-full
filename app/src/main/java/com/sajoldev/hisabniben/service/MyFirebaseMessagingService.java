package com.sajoldev.hisabniben.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.sajoldev.hisabniben.MainActivity;
import com.sajoldev.hisabniben.R;
import com.sajoldev.hisabniben.activity.NotificationActivity;
import com.sajoldev.hisabniben.util.SessionManager;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCMService";
    public static final String CHANNEL_ID = "hisabniben_push_channel";
    public static final String CHANNEL_NAME = "HisabNiben Push Notifications";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "Refreshed FCM Token: " + token);

        SessionManager sessionManager = SessionManager.getInstance(this);
        sessionManager.setFcmToken(token);

        String userId = sessionManager.getUserId();
        if (userId != null && !userId.isEmpty()) {
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(userId)
                    .update("fcmToken", token, "fcm_token", token)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "FCM token updated in Firestore"))
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to update FCM token in Firestore", e));
        }
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        String title = null;
        String body = null;
        String imageUrl = null;
        String actionType = null;
        String deepLink = null;

        // Check if message contains notification payload
        if (remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle();
            body = remoteMessage.getNotification().getBody();
            if (remoteMessage.getNotification().getImageUrl() != null) {
                imageUrl = remoteMessage.getNotification().getImageUrl().toString();
            }
        }

        // Check data payload
        Map<String, String> data = remoteMessage.getData();
        if (data.size() > 0) {
            if (title == null || title.isEmpty()) title = data.get("title");
            if (body == null || body.isEmpty()) body = data.get("message");
            if (body == null || body.isEmpty()) body = data.get("body");
            if (imageUrl == null || imageUrl.isEmpty()) imageUrl = data.get("imageUrl");
            actionType = data.get("actionType");
            deepLink = data.get("deepLink");
        }

        if (title == null || title.isEmpty()) {
            title = getString(R.string.app_name);
        }
        if (body == null || body.isEmpty()) {
            body = "আপনার জন্য একটি নতুন বার্তা এসেছে।";
        }

        sendNotification(title, body, imageUrl, actionType, deepLink);
    }

    private void sendNotification(String title, String message, String imageUrl, String actionType, String deepLink) {
        createNotificationChannel();

        Intent intent = new Intent(this, NotificationActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (actionType != null) intent.putExtra("actionType", actionType);
        if (deepLink != null) intent.putExtra("deepLink", deepLink);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                (int) System.currentTimeMillis(),
                intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(title)
                        .setContentText(message)
                        .setAutoCancel(true)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setDefaults(NotificationCompat.DEFAULT_ALL)
                        .setContentIntent(pendingIntent);

        if (imageUrl != null && !imageUrl.isEmpty()) {
            Bitmap bitmap = getBitmapFromUrl(imageUrl);
            if (bitmap != null) {
                notificationBuilder.setStyle(new NotificationCompat.BigPictureStyle().bigPicture(bitmap));
            }
        } else {
            notificationBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(message));
        }

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify((int) System.currentTimeMillis(), notificationBuilder.build());
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        CHANNEL_NAME,
                        NotificationManager.IMPORTANCE_HIGH
                );
                channel.setDescription("HisabNiben Push Notifications");
                channel.enableVibration(true);
                channel.enableLights(true);
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private Bitmap getBitmapFromUrl(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            return BitmapFactory.decodeStream(input);
        } catch (Exception e) {
            Log.e(TAG, "Error fetching notification image: " + e.getMessage());
            return null;
        }
    }
}
