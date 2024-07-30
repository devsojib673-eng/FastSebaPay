package com.sms.sohojpaybd;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;

import com.blogspot.atifsoftwares.animatoolib.Animatoo;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class qrcodescan extends AppCompatActivity {

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrcodescan);

        scanCode();
    }

    private void scanCode() {
        ScanOptions scanOptions = new ScanOptions();
        scanOptions.setPrompt("");
        scanOptions.setBeepEnabled(true);
        scanOptions.setOrientationLocked(true);
        scanOptions.setBarcodeImageEnabled(true);
        scanOptions.setCaptureActivity(CaptureAct.class);
        barLauncher.launch(scanOptions);
    }

    ActivityResultLauncher<ScanOptions> barLauncher = registerForActivityResult(new ScanContract(), result -> {
        if (result.getContents() != null) {
            try {
                // URL decode the content
                String decodedContent = URLDecoder.decode(result.getContents(), StandardCharsets.UTF_8.name());

                // Parse the decoded content
                String baseUrl = decodedContent.substring(0, decodedContent.indexOf("/device-connect"));
                String queryParams = decodedContent.substring(decodedContent.indexOf("?") + 1);
                String[] params = queryParams.split("&");

                String username = null;
                String password = null;

                for (String param : params) {
                    if (param.startsWith("email=")) {
                        username = param.substring("email=".length());
                    } else if (param.startsWith("device_key=")) {
                        password = param.substring("device_key=".length());
                    }
                }

                if (username != null && password != null) {
                    LoginActivity.STATUS = "1";

                    Intent intent = new Intent(qrcodescan.this, LoginActivity.class);
                    intent.putExtra("EMAIL", username);
                    intent.putExtra("DEVICEKEY", password);
                    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);  // Ensure only one instance is used
                    startActivity(intent);
                    Animatoo.animateSwipeLeft(qrcodescan.this);
                } else {
                    Toast.makeText(this, "Invalid QR code data", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(this, "Error processing QR code data", Toast.LENGTH_SHORT).show();
            }
        }
    });
}
