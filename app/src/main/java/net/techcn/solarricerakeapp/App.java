package net.techcn.solarricerakeapp;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;


public class App extends Application {
    public static final String CHANNEL_1_ID = "channel1";
    public static final String CHANNEL_2_ID = "channel2";
    public static final String CHANNEL_3_ID = "channel3";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannels();
    }

    private void createNotificationChannels() {
        NotificationChannel channel1 = new NotificationChannel(
                CHANNEL_1_ID,
                "Connection Status",
                NotificationManager.IMPORTANCE_HIGH
        );
        channel1.setDescription("For connection status updates");

        NotificationChannel channel2 = new NotificationChannel(
                CHANNEL_2_ID,
                "Weather Alerts",
                NotificationManager.IMPORTANCE_HIGH
        );
        channel2.setDescription("For control alerts");

        NotificationChannel channel3 = new NotificationChannel(
                CHANNEL_3_ID,
                "In-App Activity",
                NotificationManager.IMPORTANCE_HIGH
        );
        channel3.setDescription("For weather alerts and reminders");

        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(channel1);
        manager.createNotificationChannel(channel2);
        manager.createNotificationChannel(channel3);
    }
}