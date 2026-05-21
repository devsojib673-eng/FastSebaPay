package com.sms.sopnopay;

import static android.content.Context.MODE_PRIVATE;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class SmsReceiver extends BroadcastReceiver {

    private sqlite databaseHelper;
    private static final long AGGREGATION_DELAY = 1000; // 1000 milliseconds
    private static long lastAggregationTime = 0;
    private static String lastAggregatedTitle = "";
    private static StringBuilder aggregatedBody = new StringBuilder();

    private RequestQueue requestQueue;
    private static final String CHANNEL_ID = "SMS_CHANNEL";

    @Override
    public void onReceive(Context context, Intent intent) {
        // Initialize the databaseHelper
        databaseHelper = new sqlite(context);

        if (intent != null && "android.provider.Telephony.SMS_RECEIVED".equals(intent.getAction())) {
            // Implement your SMS reading logic here
            // Access SMS content from the intent extras
            SmsMessage[] messages = Telephony.Sms.Intents.getMessagesFromIntent(intent);
            // Process the messages as needed
            StringBuilder fullMessage = new StringBuilder();
            String smsAddress = "";
            for (SmsMessage message : messages) {
                fullMessage.append(message.getDisplayMessageBody());
                smsAddress = message.getOriginatingAddress().toString();
            }

            String smsBody = fullMessage.toString().trim();
            sendSmsToServer(context, smsBody, smsAddress);
        }
    }

    private void sendSmsToServer(final Context context, final String body, final String title) {
        SharedPreferences preferences = context.getSharedPreferences(context.getString(R.string.app_name), MODE_PRIVATE);
        String user_email = preferences.getString("user_email", "");
        String device_key = preferences.getString("device_key", "");
        String device_ip = preferences.getString("device_ip", "");

    //   String url = "what should be  the url fix it here chatgpt";

        String url = "https://selfnumberpay.mcmmadaripur.com/api/add-data";

        StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        handleVerificationResponse(context, response, title, body);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        saveSmsToDatabase(context, title, body); // Save to database on failure
                    }
                }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("user_email", user_email);
                params.put("device_key", device_key);
                params.put("device_ip", device_ip);
                params.put("address", title);
                params.put("message", body);
                return params;
            }

            @Override
            public String getBodyContentType() {
                return "application/x-www-form-urlencoded; charset=UTF-8";
            }
        };

        requestQueue = Volley.newRequestQueue(context);
        requestQueue.add(postRequest);
    }

    private void handleVerificationResponse(Context context, String response, String title, String body) {
        try {
            if (response != null && !response.isEmpty()) {
                Log.d("handleVerificationResponse", "Response: " + response);
                JSONObject jsonResponse = new JSONObject(response);
                int status = jsonResponse.getInt("status");

                if (status == 1) {


                } else if (status == 0){

                    saveSmsToDatabase(context, title, body); // Save to database on failure



                } else if (status == 2){

                    SharedPreferences preferences = context.getSharedPreferences(context.getString(R.string.app_name), MODE_PRIVATE);
                    SharedPreferences.Editor editorx = preferences.edit();
                    editorx.clear();
                    editorx.apply();


                };







            } else {

            }
        } catch (JSONException e) {


        }
    }

    private void saveSmsToDatabase(Context context, String title, String body) {
        try {
            SQLiteDatabase db = databaseHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(sqlite.COLUMN_TITLE, title);
            values.put(sqlite.COLUMN_BODY, body);

            // Insert the new SMS into the database
            long result = db.insert(sqlite.TABLE_SMS, null, values);
            db.close();


            // Show a notification based on the result of saving to the database
            if (result != -1) {
            } else {
            }
        } catch (Exception e) {

        }
    }


}
