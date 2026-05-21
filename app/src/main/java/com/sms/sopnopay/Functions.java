package com.sms.sopnopay;

import android.app.NotificationManager;
import android.content.Context;

public class Functions {
    private static final int MAX_NOTIFICATION_CHARACTERS = 100;

    public static void createNotification(Context context, String title, String messageBody) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        String CHANNEL_ID = "sms_channel";

        // Check if the message exceeds the character limit
        if (messageBody.length() <= MAX_NOTIFICATION_CHARACTERS) {
            // Message is short, create a single notification
            createSingleNotification(context, title, messageBody, CHANNEL_ID, notificationManager);
        } else {
            // Message is long, handle it accordingly (e.g., truncate or create expandable notification)
            handleLongMessage(context, title, messageBody, CHANNEL_ID, notificationManager);
        }
    }

    private static void createSingleNotification(Context context, String title, String messageBody, String channelId, NotificationManager notificationManager) {
        // Your existing code for creating a single notification
        // ...
    }

    private static void handleLongMessage(Context context, String title, String messageBody, String channelId, NotificationManager notificationManager) {
        // You can handle long messages here, such as truncating or creating an expandable notification
        // For example, truncate the message to fit within the character limit
        if (messageBody.length() > MAX_NOTIFICATION_CHARACTERS) {
            messageBody = messageBody.substring(0, MAX_NOTIFICATION_CHARACTERS) + "..."; // Truncate the message
        }

        // Create an expanded notification
        createSingleNotification(context, title, messageBody, channelId, notificationManager);
    }


}
