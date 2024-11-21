package net.techcn.solarricerakeapp;

import static java.security.AccessController.getContext;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import static net.techcn.solarricerakeapp.App.CHANNEL_1_ID;
import static net.techcn.solarricerakeapp.App.CHANNEL_2_ID;

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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class ForegroundService extends Service {

    Timer timer;
    private static final String LOG_TAG = ForegroundService.class.getSimpleName();
    private boolean running = false;

    private NotificationManagerCompat notificationManager;

    private final String url = "https://api.openweathermap.org/data/2.5/weather";
    private final String appid = "e53301e27efa0b66d05045d91b2742d3";
    DecimalFormat df = new DecimalFormat("#.##");

    String warningMessage = "It looks like weather is not good today. Please take necessary precautions to protect the crops and SolarRiceRake Device.";
    String reminderMessage = "The weather is good today. It is the best time to dry your rice harvest using SolarRiceRake Device.";
    String alertMessage = "Extreme weather condition detected. Please take necessary precautions to protect the crops and SolarRiceRake Device.";

    GlobalObject globalObject;


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        globalObject = new GlobalObject(getApplicationContext());

        timer = new Timer();
        notificationManager = NotificationManagerCompat.from(this);
        createNotificationChannel();
        startForeground(1, buildNotification());
        launchWeatherNotification();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!running) {
            running = true;
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        initiateWeatherMonitoring();
                    } catch (Exception e) {
                        Log.e("DashboardFragment", e.toString().trim());
                    }
                }
            }, 0, 10000);
        }

        return super.onStartCommand(intent, flags, startId);
    }

    private void launchWeatherNotification() {
        globalObject.lastForecastRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String value = snapshot.getValue(String.class);
                    switch (value) {
                        case "mist":
                        case "smoke":
                        case "haze":
                        case "fog":
                        case "overcast clouds":
                        case "broken clouds":
                            sendWarningNotification(value);
                            break;
                        case "clear sky":
                        case "few clouds":
                        case "scattered clouds":
                            sendReminderNotification(value);
                            break;
                        default:
                            sendAlertNotification(value);
                            break;
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(LOG_TAG, error.toString().trim());
            }
        });
    }

    public void initiateWeatherMonitoring() {
        String latitude = "14.1122";
        String longitude = "122.9553";
        String tempUrl = url + "?lat=" + latitude + "&lon=" + longitude + "&appid=" + appid;
        StringRequest stringRequest = new StringRequest(Request.Method.POST, tempUrl, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject jsonResponse = new JSONObject(response);
                    JSONArray jsonArray = jsonResponse.getJSONArray("weather");
                    JSONObject jsonObjectWeather = jsonArray.getJSONObject(0);
                    String mainDescription = jsonObjectWeather.getString("main");
                    String description = jsonObjectWeather.getString("description");
                    JSONObject jsonObjectMain = jsonResponse.getJSONObject("main");
                    double temp = jsonObjectMain.getDouble("temp") - 273.15;
//                    double feelsLike = jsonObjectMain.getDouble("feels_like") - 273.15;
//                    float pressure = jsonObjectMain.getInt("pressure");
//                    int humidity = jsonObjectMain.getInt("humidity");
//                    JSONObject jsonObjectWind = jsonResponse.getJSONObject("wind");
//                    String wind = jsonObjectWind.getString("speed");
//                    JSONObject jsonObjectClouds = jsonResponse.getJSONObject("clouds");
//                    String clouds = jsonObjectClouds.getString("all");

                    String msg = "";
                    // send notification if weather is not good
                    if (mainDescription.equals("Mist") || mainDescription.equals("Smoke") || mainDescription.equals("Haze") || mainDescription.equals("Fog") || description.equals("overcast clouds") || description.equals("broken clouds")) {
//                        Log.d(LOG_TAG, "Weather is not good");
                        msg = "Weather Forecast: " + description + "\n" + warningMessage;
                    }
                    // else send notification if weather is good
                    else if (mainDescription.equals("Clear") || description.equals("few clouds") || description.equals("scattered clouds")) {
//                        Log.d(LOG_TAG, "Weather is good");
                        msg = "Weather Forecast: " + description + "\n" + reminderMessage;
                    } else {
//                        Log.d(LOG_TAG, "Extreme weather condition detected");
                        msg = "Weather Forecast: " + description + "\n" + alertMessage;
                    }

                    String finalMsg = msg;
                    globalObject.lastForecastRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()) {
                                String value = dataSnapshot.getValue(String.class);
//                                Log.d(LOG_TAG, "Value is: " + value);
//                                Log.d(LOG_TAG, "Description is: " + description);
//                                Log.d(LOG_TAG, "value == null: " + (value == null));
//                                Log.d(LOG_TAG, "!(value.equals(description)): " + (!(value.equals(description))));
                                if (!(value.equals(description))) {
                                    globalObject.lastForecastRef.setValue(description);

                                    launchWeatherNotification();

                                    String dateKey = getDate();
                                    HashMap<String, Object> dataToSave = new HashMap<>();
                                    dataToSave.put("timestamp", dateKey);
                                    dataToSave.put("forecast", description);
                                    dataToSave.put("temperature", String.format("%s °C", df.format(temp)));
                                    globalObject.weatherReportRef.child(dateKey).setValue(dataToSave).addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            HashMap<String, Object> notifToSave = new HashMap<>();
                                            notifToSave.put("timestamp", dateKey);
                                            notifToSave.put("message", finalMsg);
                                            notifToSave.put("title", "Weather Report");
                                            globalObject.notifRef.child(dateKey).setValue(notifToSave);
                                        }
                                    });
                                }
                            } else {
                                globalObject.lastForecastRef.setValue(description);
                                Log.e(LOG_TAG, "No data available");
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            // Handle errors
                        }
                    });

                } catch (JSONException e) {
                    Log.e("DashboardFragment", e.toString().trim());
                }
            }
        }, new Response.ErrorListener(){

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("DashboardFragment", error.toString().trim());
            }
        });
        RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
        requestQueue.add(stringRequest);
    }

    private String getDate() {
        Instant instant = Instant.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd;HH:mm:ss·SSS").withZone(ZoneId.systemDefault());
        return formatter.format(instant);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    public void sendWarningNotification(String message) {
        Bitmap img = BitmapFactory.decodeResource(getResources(), R.drawable.warning);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_1_ID)
                .setLargeIcon(img)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle("Weather Report")
                .setContentText("Weather Forecast: " + message + "\n" + warningMessage)
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

    public void sendAlertNotification(String message) {
        Bitmap img = BitmapFactory.decodeResource(getResources(), R.drawable.warning);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_1_ID)
                .setLargeIcon(img)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle("Weather Report")
                .setContentText("Weather Forecast: " + message + "\n" + alertMessage)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
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

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "General", "General", NotificationManager.IMPORTANCE_LOW);
            channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "General");
        builder.setSmallIcon(R.mipmap.ic_launcher_round);
        builder.setContentTitle("SolarRiceRake App");
        builder.setContentText("Background Service of SolarRiceRake App is running...");
        builder.setOngoing(true);
        builder.setPriority(NotificationCompat.PRIORITY_DEFAULT);
        return builder.build();
    }

    private void sendReminderNotification(String message) {
        Bitmap img = BitmapFactory.decodeResource(getResources(), R.drawable.warning);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_1_ID)
                .setLargeIcon(img)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle("Weather Report")
                .setContentText("Weather Forecast: " + message + "\n" + reminderMessage)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .build();

        if (notificationManager.areNotificationsEnabled()) {
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
    }
}