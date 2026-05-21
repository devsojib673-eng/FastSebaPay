package com.sms.sopnopay;
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class ToastHelper {

    public static void showCustomToast(Context context, String message, Integer iconResource) {
        // Inflate the custom layout
        View layout = LayoutInflater.from(context).inflate(R.layout.custom_toast, null);

        // Find views in the custom layout
        ImageView imageView = layout.findViewById(R.id.toast_icon);
        TextView textView = layout.findViewById(R.id.toast_text);

        // Set the icon (use a default icon if iconResource is null)
        if (iconResource != null) {
            imageView.setImageResource(iconResource);
        }
        // Set the message
        textView.setText(message);

        // Create and display the custom toast with custom gravity (top-right corner)
        Toast toast = new Toast(context);
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setView(layout);

        // Set the custom gravity to top-right corner
        toast.setGravity(Gravity.BOTTOM | Gravity.CENTER, 0, 0);

        toast.show();
    }
}
