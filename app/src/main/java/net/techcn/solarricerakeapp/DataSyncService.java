package net.techcn.solarricerakeapp;

import static net.techcn.solarricerakeapp.App.CHANNEL_1_ID;
import static net.techcn.solarricerakeapp.App.CHANNEL_3_ID;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class DataSyncService extends Service {

    Timer timer;
    private static final String LOG_TAG = DataSyncService.class.getSimpleName();
    private boolean running = false;

    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;

    GlobalObject globalObject;

    private long lastExecutionTime = 10000;

    private NotificationManagerCompat notificationManager;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sharedPreferences = getApplicationContext().getSharedPreferences("PREFS_DATA", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
        globalObject = new GlobalObject(getApplicationContext());

        timer = new Timer();

        notificationManager = NotificationManagerCompat.from(this);

        globalObject.solarPanelVoltStatusRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.getValue() != null) {
                    if (snapshot.getValue().equals("normal")) {
                        createNotification("Solar Panel Status", "The output voltage of solar panel is normal");
                        saveNotification("Solar Panel Status", "The output voltage is normal");
                    } else if (snapshot.getValue().equals("low")) {
                        createNotification("Solar Panel Status", "The output voltage of solar panel is low. Make sure the device exposed to the sunlight to continue charging");
                        saveNotification("Solar Panel Status", "The output voltage of solar panel is low. Make sure the device exposed to the sunlight to continue charging");
                    } else if (snapshot.getValue().equals("critical")) {
                        createNotification("Solar Panel Status", "The output voltage of solar panel is critical. Check the sunlight exposure or visit the service center");
                        saveNotification("Solar Panel Status", "The output voltage of solar panel is critical. Check the sunlight exposure or visit the service center.");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(LOG_TAG, "Failed to read value.", error.toException());
            }
        });

        globalObject.lastActiveTimeRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (sharedPreferences.getString("connection_mode", "").equals("WiFi")) {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastExecutionTime >= 10000) {
                        Log.d(LOG_TAG, "DataSyncService: 10 sec passed");
                        if (snapshot.exists()) {
                            String strValue = snapshot.getValue(String.class);
                            Log.d(LOG_TAG, "snapshot.getValue(String.class): " + strValue);
                            if (timer != null) {
                                timer.cancel();
                                timer.purge();
                            }
                            timer = new Timer();
                            startTimer();
                        }
                        lastExecutionTime = currentTime;
                    } else {
                        Log.d(LOG_TAG, "Not enough time has passed, setting device status failed.");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(LOG_TAG, "Failed to read value.", error.toException());
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    private void startTimer() {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    if (sharedPreferences.getString("connection_mode", "").equals("WiFi")) {
                        if (globalObject.isInternetAvailable()) {
                            Log.d(LOG_TAG, "DataSyncService: Running Inside Timer");
                            checkDeviceConnection();
                            Log.d(LOG_TAG, "DataSyncService: #######################################################");
                        } else {
                            editor.putString("device_status", "OFF");
                            editor.commit();
                        }
                    }
                } catch (Exception e) {
                    Log.e("DashboardFragment", e.toString().trim());
                }
            }
        }, 0, 20000);
    }

    private void checkDeviceConnection() {
        Log.d(LOG_TAG, "isInternetAvailable: " + globalObject.isInternetAvailable());
        try {
            Log.d(LOG_TAG, "aFTERRR isInternetAvailable: true");
            globalObject.lastActiveTimeRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String strValue = snapshot.getValue(String.class);
                        String currentDeviceStatus = sharedPreferences.getString("device_status", "");
                        String prevDeviceActiveTime = sharedPreferences.getString("prev_device_active_time", null);
                        if ((prevDeviceActiveTime == null || prevDeviceActiveTime.equals(strValue)) && currentDeviceStatus.equals("ON")  ) {
                            editor.putString("device_status", "OFF");
                            editor.commit();
                            saveActivityLog("Device Status: OFF");
                        } else {
                            editor.putString("device_status", "ON");
                            editor.commit();
                            saveActivityLog("Device Status: ON");
                        }
                        editor.putString("prev_device_active_time", strValue);
                        editor.commit();
                    } else {
                        editor.putString("device_status", "OFF");
                        editor.commit();
                        saveActivityLog("Device Status: OFF");
//                        Log.d(LOG_TAG, "No data available in FRTDB location: " + lastActiveTimeRef.getRef());
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.w(LOG_TAG, "Failed to read value.", error.toException());
                    editor.putString("device_status", "OFF");
                    editor.commit();
                }
            });
        } catch (Exception e) {
            editor.putString("device_status", "OFF");
            editor.commit();
            Log.e(LOG_TAG, e.toString().trim());
        }
    }

    private void saveNotification(String title, String message) {
        String currentDate = getDate();

        HashMap<String, Object> notifToSave = new HashMap<>();
        notifToSave.put("timestamp", currentDate);
        notifToSave.put("message", message);
        notifToSave.put("title", title);
        globalObject.notifRef.child(currentDate).setValue(notifToSave);
    }

    private void saveActivityLog(String message) {
        String timestamp = getDate();

        HashMap<String, Object> dataToSave = new HashMap<>();
        dataToSave.put("timestamp", timestamp);
        dataToSave.put("activity", message);
        globalObject.activityLogRef.child(timestamp).setValue(dataToSave);
    }

    private String getDate() {
        Instant instant = Instant.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd;HH:mm:ss·SSS").withZone(ZoneId.systemDefault());
        return formatter.format(instant);
    }

    public void createNotification(String title, String message) {
        Bitmap img = BitmapFactory.decodeResource(getResources(), R.drawable.information);
        Notification notification = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_3_ID)
                .setLargeIcon(img)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .build();

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        notificationManager.notify(2, notification);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}