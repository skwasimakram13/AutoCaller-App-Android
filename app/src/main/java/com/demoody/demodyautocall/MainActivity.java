package com.demoody.demodyautocall;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.demoody.demodyautocall.services.CallService;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSIONS = 101;
    private static final int REQUEST_AUDIO = 100;
    private static final int REQUEST_CSV_FILE = 200;

    private Uri audioUri;
    private ArrayList<String> contactList = new ArrayList<>();
    private TextView textStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        checkAndRequestPermissions();

        Button btnSelectAudio = findViewById(R.id.btn_select_audio);
        Button btnStartCall = findViewById(R.id.btn_start_call);
        Button btnImportCsv = findViewById(R.id.btn_import_csv);
        Button btnStop = findViewById(R.id.btn_stop);
        Button btnSkip = findViewById(R.id.btn_skip);
        Button btnExportLog = findViewById(R.id.btn_export_log);
        textStatus = findViewById(R.id.text_status);

        btnSelectAudio.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("audio/*");
            startActivityForResult(Intent.createChooser(intent, "Select Audio"), REQUEST_AUDIO);
        });

        btnImportCsv.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            String[] mimeTypes = {
                    "text/comma-separated-values",
                    "text/csv",
                    "application/csv",
                    "text/plain"
            };
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(Intent.createChooser(intent, "Select CSV File"), REQUEST_CSV_FILE);
        });


        btnStartCall.setOnClickListener(v -> {
            if (audioUri != null && !contactList.isEmpty()) {
                try {
                    Intent intent = new Intent(this, com.demoody.demodyautocall.services.CallService.class);
                    intent.putExtra("AUDIO_URI", audioUri.toString());
                    intent.putStringArrayListExtra("CONTACTS", contactList);
                    ContextCompat.startForegroundService(this, intent);
                } catch (Exception e) {
                    textStatus.setText("Failed to start service");
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(this, "Select audio and add contacts", Toast.LENGTH_SHORT).show();
            }
        });

        btnStop.setOnClickListener(v -> {
            stopService(new Intent(this, com.demoody.demodyautocall.services.CallService.class));
            textStatus.setText("Status: Stopped by user");
        });

        btnSkip.setOnClickListener(v -> com.demoody.demodyautocall.services.CallService.skipCurrentCall());
        btnExportLog.setOnClickListener(v -> {
            File logFile = new File(getExternalFilesDir(null), "call_log.csv");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile))) {
                writer.write("Number,Status,RetryCount\n");
                for (CallService.CallLogEntry entry : CallService.callLog) {
                    writer.write(entry.number + "," + entry.status + "," + entry.retryCount + "\n");
                }
                Toast.makeText(this, "Log exported to: " + logFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Export failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkAndRequestPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED)
            permissionsNeeded.add(Manifest.permission.CALL_PHONE);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED)
            permissionsNeeded.add(Manifest.permission.READ_PHONE_STATE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED)
                permissionsNeeded.add(Manifest.permission.READ_MEDIA_AUDIO);
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                permissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }

        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toArray(new String[0]), REQUEST_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "All permissions are required!", Toast.LENGTH_LONG).show();
                    finish();
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data == null) return;

        if (requestCode == REQUEST_AUDIO && resultCode == RESULT_OK) {
            audioUri = data.getData();
            textStatus.setText("Audio selected: " + audioUri.getLastPathSegment());
            Toast.makeText(this, "Audio selected", Toast.LENGTH_SHORT).show();
        }

        if (requestCode == REQUEST_CSV_FILE && resultCode == RESULT_OK) {
            importCsvContacts(data.getData());
            textStatus.setText("Contacts imported: " + contactList.size());
        }
    }

    private void importCsvContacts(Uri uri) {
        contactList.clear();
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split(",");
                for (String token : tokens) {
                    String phone = token.trim().replaceAll("[^0-9]", "");
                    if (phone.length() >= 10) {
                        contactList.add(phone);
                    }
                }
            }
            Toast.makeText(this, "Imported " + contactList.size() + " contacts", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to read CSV", Toast.LENGTH_SHORT).show();
        }
    }
}
