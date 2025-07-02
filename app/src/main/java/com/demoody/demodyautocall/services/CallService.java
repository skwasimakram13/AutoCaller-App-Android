package com.demoody.demodyautocall.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.telephony.TelephonyManager;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.ArrayList;

public class CallService extends Service {

    public static class CallLogEntry {
        public String number;
        public String status;
        public int retryCount;

        public CallLogEntry(String number, String status, int retryCount) {
            this.number = number;
            this.status = status;
            this.retryCount = retryCount;
        }
    }

    public static ArrayList<CallLogEntry> callLog = new ArrayList<>();

    private static class Contact {
        String number;
        int retryCount = 0;

        Contact(String number) {
            this.number = number;
        }
    }

    private ArrayList<Contact> contacts = new ArrayList<>();
    private int currentIndex = 0;
    private Uri audioUri;
    private boolean isCallConnected = false;
    private long callStartTime = 0;

    public static boolean skip = false;
    public static boolean stopped = false;

    public static void skipCurrentCall() {
        skip = true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        ArrayList<String> rawNumbers = intent.getStringArrayListExtra("CONTACTS");
        audioUri = Uri.parse(intent.getStringExtra("AUDIO_URI"));
        contacts.clear();
        for (String num : rawNumbers) {
            contacts.add(new Contact(num));
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
        registerReceiver(callReceiver, filter);

        startForeground(1, createNotification("Auto calling started"));
        startNextCall();
        return START_NOT_STICKY;
    }

    private void startNextCall() {
        if (stopped || currentIndex >= contacts.size()) {
            stopSelf();
            return;
        }

        Contact contact = contacts.get(currentIndex);
        Intent callIntent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + contact.number));
        callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(callIntent);

        callStartTime = System.currentTimeMillis();
    }

//    private final BroadcastReceiver callReceiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
//
//            if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state)) {
//                isCallConnected = true;
//
//                new Handler().postDelayed(() -> {
//                    if (!skip) {
//                        AudioPlayer.playAudio(getApplicationContext(), audioUri);
//                    } else {
//                        skip = false;
//                        endCall();
//                    }
//                }, 2000); // Play 2 second after connected
//            }
//
//            if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {
//                if (isCallConnected) {
//                    isCallConnected = false;
//                    AudioPlayer.stop();
//
//                    long duration = System.currentTimeMillis() - callStartTime;
//                    Contact current = contacts.get(currentIndex);
//
//                    if (duration < 5000 && current.retryCount < 2) {
//                        current.retryCount++;
//                        contacts.add(currentIndex + 1, current); // Retry after currentIndex + 1
//                        callLog.add(new CallLogEntry(current.number, "Retried", current.retryCount));
//                    } else {
//                        String status = (duration < 5000) ? "Failed" : "Success";
//                        callLog.add(new CallLogEntry(current.number, status, current.retryCount));
//                    }
//
//                    currentIndex++;
//                    new Handler().postDelayed(CallService.this::startNextCall, 5000);
//                }
//            }
//        }
//    };

    private final BroadcastReceiver callReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);

            if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state)) {
                isCallConnected = true;
                callStartTime = System.currentTimeMillis(); // mark time
            }

            if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {
                long duration = System.currentTimeMillis() - callStartTime;

                if (isCallConnected) {
                    isCallConnected = false;

                    // ✅ Only play audio if call lasted > 3 seconds (meaning it was probably answered)
                    if (duration > 2000 && !skip) {
                        AudioPlayer.playAudio(getApplicationContext(), audioUri);
                    } else {
                        skip = false;
                        endCall();
                    }

                    Contact current = contacts.get(currentIndex);
                    if (duration < 5000 && current.retryCount < 2) {
                        current.retryCount++;
                        contacts.add(currentIndex + 1, current);
                        callLog.add(new CallLogEntry(current.number, "Retried", current.retryCount));
                    } else {
                        String status = (duration < 5000) ? "Failed" : "Success";
                        callLog.add(new CallLogEntry(current.number, status, current.retryCount));
                    }

                    currentIndex++;
                    new Handler().postDelayed(CallService.this::startNextCall, 5000);
                }
            }
        }
    };


    private void endCall() {
        AudioPlayer.stop();
    }

    private Notification createNotification(String content) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("call_channel", "Call Service", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) manager.createNotificationChannel(channel);
        }
        return new NotificationCompat.Builder(this, "call_channel")
                .setContentTitle("Auto Calling")
                .setContentText(content)
                .setSmallIcon(android.R.drawable.sym_call_outgoing)
                .build();
    }

    @Override
    public void onDestroy() {
        try {
            unregisterReceiver(callReceiver);
        } catch (IllegalArgumentException ignored) {}
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
