package net.techcn.solarricerakeapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.bluetooth.BluetoothAdapter;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class GlobalObject {

    private Context context;

    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;

    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference myRef;
    DatabaseReference parentRef;

    DatabaseReference isAutomaticModeRef;
    DatabaseReference automaticStatusRef;
    DatabaseReference isNoObstacleRef;
    DatabaseReference deviceRef;
    DatabaseReference dryingHistoryRef;
    DatabaseReference notifRef;
    DatabaseReference activityLogRef;
    DatabaseReference batteryStatusHistory;
    DatabaseReference moistureRef;
    DatabaseReference lastActiveTimeRef;
    DatabaseReference solarPanelVoltStatusRef;
    DatabaseReference weatherReportRef;
    DatabaseReference lastForecastRef;
    DatabaseReference releasedDeviceRef;

    public GlobalObject(Context context) {
        this.context = context.getApplicationContext();

        sharedPreferences = context.getSharedPreferences("PREFS_DATA", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();

        String user_account = sharedPreferences.getString("current_email","");
        parentRef = database.getReference();
        myRef = database.getReference().child(user_account);

        isAutomaticModeRef = myRef.child("Device").child("is_automatic_mode");
        automaticStatusRef = myRef.child("Device").child("automatic_status");
        isNoObstacleRef = myRef.child("Device").child("is_no_obstacle");
        deviceRef = parentRef.child("Device").child("command_code");
        dryingHistoryRef = myRef.child("Drying History");
        notifRef = myRef.child("Notifications");
        activityLogRef = myRef.child("Activity Log");
        batteryStatusHistory = myRef.child("Battery");
        moistureRef = myRef.child("Moisture Level");
        lastActiveTimeRef = myRef.child("Device").child("last_active_time");
        solarPanelVoltStatusRef = myRef.child("Device").child("solar_panel_volt_status");
        weatherReportRef = myRef.child("Weather").child("Weather Report");
        lastForecastRef = myRef.child("Weather").child("last_forecast").child("forecast");
        releasedDeviceRef = parentRef.child("Released SolaRice");
    }

    public boolean isInternetAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    public boolean isBluetoothAvailable() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        boolean isBluetoothEnabled = bluetoothAdapter != null && bluetoothAdapter.isEnabled();
        return isBluetoothEnabled;
    }
    
    
}
