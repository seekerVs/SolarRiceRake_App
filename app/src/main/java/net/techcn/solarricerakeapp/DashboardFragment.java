package net.techcn.solarricerakeapp;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import net.techcn.solarricerakeapp.databinding.FragmentDashboardBinding;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.SignInButton;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class DashboardFragment extends Fragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String LOG_TAG = DashboardFragment.class.getSimpleName();
    FragmentDashboardBinding binding;
    private final String url = "https://api.openweathermap.org/data/2.5/weather";
    private final String appid = "e53301e27efa0b66d05045d91b2742d3";
    DecimalFormat df = new DecimalFormat("#.##");

    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;

    GlobalObject globalObject;
    AlertDialog alertDialoShow;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        Log.d(LOG_TAG, "DashboardFragment onCreateView!!!!!!!!!!!!!!!!");
        return binding.getRoot();

    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        sharedPreferences = context.getSharedPreferences("PREFS_DATA", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);

        globalObject = new GlobalObject(context.getApplicationContext());
        Log.d(LOG_TAG, "DashboardFragment onAttach!!!!!!!!!!!!!!!!");
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Initialize UI components and set click listeners
        Log.d(LOG_TAG, "DashboardFragment onViewCreated!!!!!!!!!!!!!!!!");

        globalObject.batteryStatusHistory.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.getValue() != null) {
                    List<DataSnapshot> childSnapshots = new ArrayList<>();
                    for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                        childSnapshots.add(childSnapshot);
                    }
                    //Reverse
                    Collections.reverse(childSnapshots);

                    String percentage = childSnapshots.get(0).child("percentage").getValue(String.class);
                    editor.putString("percentage", "");
                    editor.commit();
                    editor.putString("percentage", percentage + "%");
                    editor.commit();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        binding.notificationImageButton.setOnClickListener(v ->
                getParentFragmentManager().beginTransaction().replace(R.id.framelayout, new NotificationFragment())
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .addToBackStack(null)
                .commit());

        binding.PowerLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String connectionMode = sharedPreferences.getString("device_status",null);
            }
        });

        binding.ModeLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String connectionMode = sharedPreferences.getString("connection_mode",null);
                if (connectionMode == null || connectionMode.equals("BT")) {
                    editor.putString("connection_mode", "WiFi");
                    editor.commit();
                    if (globalObject.isInternetAvailable()) {
                        String deviceStatus = sharedPreferences.getString("device_status","OFF");
                        if (deviceStatus.equals("ON")) {
                            positive_dialog("Connection Mode", "Connection mode changed to Internet");
                        } else {
                            negative_dialog("Connection Mode", "Error: Device connection through Internet unavailable. Turn on SolaRice Device WiFi and try again.");
                        }
                    } else {
                        negative_dialog("Connection Mode", "Error: Internet is not available. Connect to reliable internet source and try again.");
                    }
                } else {
                    editor.putString("connection_mode", "BT");
                    editor.commit();
                    if (globalObject.isBluetoothAvailable()) {
                        String btStatus = sharedPreferences.getString("bt_status", "Disconnected");
                        if (btStatus.equals("Connected")) {
                            positive_dialog("Connection Mode", "Connection mode changed to Bluetooth");
                        } else {
                            AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
                            alertDialog.setTitle("Connection Error");
                            alertDialog.setMessage("Bluetooth is not available. continue to setup Bluetooth?");
                            alertDialog.setIcon(R.drawable.error_solid);
                            alertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.dismiss();
                                    getParentFragmentManager().beginTransaction().replace(R.id.framelayout, new DevicesFragment())
                                            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                                            .addToBackStack(null)
                                            .commit();
                                }
                            });
                            alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                            if (alertDialoShow != null) {
                                alertDialoShow.dismiss();
                            }
                            alertDialoShow = alertDialog.create();
                            alertDialoShow.show();
                        }
                    } else {
                        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
                        if (!adapter.isEnabled()) {
                            Intent enable = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                            if (ActivityCompat.checkSelfPermission(requireContext(), android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                                ActivityCompat.requestPermissions(requireActivity(),
                                        new String[]{android.Manifest.permission.BLUETOOTH_CONNECT}, 1);
                                return;
                            }
                            startActivityForResult(enable, 0);
                        }
                        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
                        alertDialog.setTitle("Connection Error");
                        alertDialog.setMessage("Bluetooth is disabled on this device. Turn on Bluetooth?");
                        alertDialog.setIcon(R.drawable.error_solid);
                        alertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                                Intent intent = new Intent();
                                intent.setAction(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
                                startActivity(intent);
                            }
                        });
                        alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                        if (alertDialoShow != null) {
                            alertDialoShow.dismiss();
                        }
                        alertDialoShow = alertDialog.create();
                        alertDialoShow.show();
                    }
                }
                updateModeUI();
                setPreferenceBasedValues();
            }
        });

        Log.d(LOG_TAG, "isInternetAvailable: " + globalObject.isInternetAvailable());
        if (globalObject.isInternetAvailable()) {
            startBackgroundService();
            getWeatherData();
        } else {
            binding.wifiImageview.setImageResource(R.drawable.fluent_wifi_2_24_filled);
            binding.bluetoothImageView.setImageResource(R.drawable.fluent_bluetooth_24_filled);
            negative_dialog("Connection Mode", "Error: Internet is not available. Connect to reliable internet source and try again.");
        }
        onSharedPreferenceChanged(sharedPreferences, "device_status");
        setPreferenceBasedValues();
        updateModeUI();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(LOG_TAG, "DashboardFragment onCreate!!!!!!!!!!!!!!!!");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(LOG_TAG, "DashboardFragment onResume!!!!!!!!!!!!!!!!");
        startDataSyncService();
    }

    private void updateModeUI() {
        String connectionMode = sharedPreferences.getString("connection_mode","");
        if (!connectionMode.isEmpty()) {
            if (connectionMode.equals("BT")) {
                binding.wifiImageview.setImageResource(R.drawable.fluent_wifi_2_24_filled);
                binding.bluetoothImageView.setImageResource(R.drawable.fluent_bluetooth_24_filled2);
            } else {
                binding.wifiImageview.setImageResource(R.drawable.fluent_wifi_2_24_filled2);
                binding.bluetoothImageView.setImageResource(R.drawable.fluent_bluetooth_24_filled);
            }
        } else {
            binding.wifiImageview.setImageResource(R.drawable.fluent_wifi_2_24_filled);
            binding.bluetoothImageView.setImageResource(R.drawable.fluent_bluetooth_24_filled);
        }
    }

    private void setPreferenceBasedValues() {
        String connectionMode = sharedPreferences.getString("connection_mode","");
        if (connectionMode.isBlank()) {
            connectionMode = "WiFi";
            editor.putString("connection_mode", "WiFi");
            editor.commit();
        }

        String powerStatus = sharedPreferences.getString("device_status","OFF");
        String batteryStatus = sharedPreferences.getString("battery_level","N/A");

        binding.modeTextview.setText(connectionMode);
        binding.PowerTextview.setText(powerStatus);
        binding.BatteryTextview.setText(batteryStatus);
    }

    public void getWeatherData() {
        binding.progressLayout.setVisibility(View.VISIBLE);
        binding.progressLayout.getBackground().setAlpha(180);
        binding.progressBar.setProgress(10,true);
        if (globalObject.isInternetAvailable()) {
            try {
                String latitude = "14.1122";
                String longitude = "122.9553";
                String tempUrl = url + "?lat=" + latitude + "&lon=" + longitude + "&appid=" + appid;
                binding.progressBar.setProgress(20,true);
                binding.progressTextview.setText("Fetching API Data");
                StringRequest stringRequest = new StringRequest(Request.Method.POST, tempUrl, new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        binding.progressBar.setProgress(40,true);
                        binding.progressTextview.setText("Data request completed");
                        try {
                            binding.progressBar.setProgress(50,true);
                            binding.progressTextview.setText("Extracting JSON Data");
                            JSONObject jsonResponse = new JSONObject(response);
                            JSONArray jsonArray = jsonResponse.getJSONArray("weather");
                            JSONObject jsonObjectWeather = jsonArray.getJSONObject(0);
                            String mainDescription = jsonObjectWeather.getString("main");
                            String description = jsonObjectWeather.getString("description");
                            JSONObject jsonObjectMain = jsonResponse.getJSONObject("main");
                            double temp = jsonObjectMain.getDouble("temp") - 273.15;
                            double feelsLike = jsonObjectMain.getDouble("feels_like") - 273.15;
                            float pressure = jsonObjectMain.getInt("pressure");
                            int humidity = jsonObjectMain.getInt("humidity");
                            JSONObject jsonObjectWind = jsonResponse.getJSONObject("wind");
                            String wind = jsonObjectWind.getString("speed");
                            JSONObject jsonObjectClouds = jsonResponse.getJSONObject("clouds");
                            String clouds = jsonObjectClouds.getString("all");

                            binding.progressBar.setProgress(70,true);
                            binding.progressTextview.setText("Extraction successful");
                            // Set the textview text
                            binding.progressBar.setProgress(80,true);
                            binding.progressTextview.setText("Updating UI views");

                            setWeatherConditionImage(mainDescription);

                            // format description/condition
                            String firstLetter = description.substring(0, 1).toUpperCase();
                            String remainingText = description.substring(1).toLowerCase();
                            description = firstLetter + remainingText;

                            binding.conditionTextview.setText(description);
                            binding.conditionTextview2.setText(description);
                            binding.tempTextview.setText(String.format("%s °C", df.format(temp)));
                            binding.tempTextview2.setText(String.format("%s °C", df.format(temp)));
                            binding.feelsLikeTextview.setText(String.format("%s °C", df.format(feelsLike)));
                            binding.windSpeedTextview.setText(String.format("%s m/s", wind));
                            binding.windSpeedTextview2.setText(String.format("%s m/s", wind));
                            binding.pressureTextview.setText(String.format("%s hPa", pressure));
                            binding.humidityTextview.setText(String.format("%s %%", humidity));
                            binding.cloudnessTextview.setText(String.format("%s %%", clouds));

                            binding.progressBar.setProgress(100,true);
                            binding.progressTextview.setText("Weather Data Fetching Successful");

                        } catch (JSONException e) {
                            binding.progressTextview.setText("Weather Data Fetching Failed");
                            Toast.makeText(getActivity(), e.toString().trim(), Toast.LENGTH_SHORT).show();
                        }
                    }
                }, new Response.ErrorListener(){

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(getActivity(), error.toString().trim(), Toast.LENGTH_SHORT).show();
                    }
                });
                RequestQueue requestQueue = Volley.newRequestQueue(getContext());
                requestQueue.add(stringRequest);
            } catch (Exception e) {
                Log.d(LOG_TAG, "getWeatherData: " + e);
            }
        }

        binding.progressLayout.getBackground().setAlpha(255);
        binding.progressBar.setProgress(0,true);
        binding.progressLayout.setVisibility(View.GONE);
    }

    public void setWeatherConditionImage(String Description) {
        switch (Description) {
            case "Thunderstorm":
                binding.headerLayout.setBackgroundResource(R.drawable.thunderstorm_photo);
                binding.conditionImageview.setImageResource(R.drawable._11d);
                break;
            case "Rain":
                binding.headerLayout.setBackgroundResource(R.drawable.rain_photo);
                binding.conditionImageview.setImageResource(R.drawable._10d);
                break;
            case "Snow":
                binding.headerLayout.setBackgroundResource(R.drawable.snowy_photo);
                binding.conditionImageview.setImageResource(R.drawable._13d);
                break;
            case "Clear":
                binding.headerLayout.setBackgroundResource(R.drawable.clear_sky_photo);
                binding.conditionImageview.setImageResource(R.drawable._01d);
                break;
            case "Clouds":
                binding.headerLayout.setBackgroundResource(R.drawable.cloudy_photo);
                binding.conditionImageview.setImageResource(R.drawable._02d);
                break;
            case "Drizzle":
                binding.headerLayout.setBackgroundResource(R.drawable.drizzle_photo);
                binding.conditionImageview.setImageResource(R.drawable._09d);
                break;
            case "Mist":
            case "Smoke":
            case "Haze":
            case "Dust":
            case "Fog":
            case "Sand":
            case "Ash":
            case "Squall":
            case "Tornado":
                binding.headerLayout.setBackgroundResource(R.drawable.atmosphere_photo);
                binding.conditionImageview.setImageResource(R.drawable._50d);
                break;
            default:
                binding.headerLayout.setBackgroundResource(R.drawable.dashboard_header_bg);
                binding.conditionImageview.setImageResource(R.drawable.ic_launcher_foreground);
                break;
        }
    }

    private void startDataSyncService() {
        try {
            Context context = getContext();
            Intent intent = new Intent(getActivity(), DataSyncService.class);

            if (context != null) {
                context.startService(intent);
            }
        } catch (Exception e) {
            Log.d(LOG_TAG, "startDataSyncService: " + e);
        }
    }

    private void startBackgroundService() {
        try {
            String status = sharedPreferences.getString("is_notif_enabled", null);
            Log.d(LOG_TAG, "InventoryActivity isNotifEnabled: " + status + ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");

            if (status == null || status.equals("true")) {
                if (!isServiceRunning(ForegroundService.class)) {
                    // if notif is on and service is not running, start service
                    editor.putString("is_notif_enabled", "true");
                    editor.commit();
                    Context context = getContext();
                    Intent intent = new Intent(getActivity(), ForegroundService.class);

                    if (context != null) {
                        context.startForegroundService(intent);
                    }
                }
            }
        } catch (Exception e) {
            Log.d(LOG_TAG, "startBackgroundService: " + e);
        }
    }

    private boolean isServiceRunning(Class<ForegroundService> serviceClass) {
        ActivityManager activityManager = (ActivityManager) requireActivity().getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo serviceInfo : activityManager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceInfo.service.getClassName().equals(serviceClass.getName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String key) {
        if (key.equals("device_status")) {
            if (sharedPreferences.getString("connection_mode", "N/A").equals("BT")) {
                if (sharedPreferences.getString("device_status", "OFF").equals("ON") ) {
                    binding.PowerTextview.setText("ON");
                    positive_dialog("Connection Status", "SolaRice device is connected to Bluetooth");
                } else {
                    binding.PowerTextview.setText("OFF");
                    negative_dialog("Connection Status", "SolaRice device cannot be accessed through Bluetooth");
                }
            } else {
                if (sharedPreferences.getString("device_status", "OFF").equals("ON")) {
                    binding.PowerTextview.setText("ON");
                    positive_dialog("Connection Status", "SolaRice device is connected to internet");
                } else {
                    if (!globalObject.isInternetAvailable()) {
                        binding.PowerTextview.setText("OFF");
                        negative_dialog("Connection Status", "You are not connected to the internet. Please make sure you are connected to a stable internet connection.");
                    } else {
                        binding.PowerTextview.setText("OFF");
                        negative_dialog("Connection Status", "SolaRice device is not online. Make sure it is connected to stable internet!");
                    }
                }
            }
            Log.d(LOG_TAG, "onSharedPreferenceChanged: " + key +":  "+ sharedPreferences.getString(key, "OFF"));
        } else if (key.equals("percentage")) {
            binding.BatteryTextview.setText(sharedPreferences.getString(key, "N/A"));
        }
    }

    public void positive_dialog(String title, String text) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
        alertDialog.setTitle(title);
        alertDialog.setMessage(text);
        alertDialog.setIcon(R.drawable.success_filled);
        alertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        AlertDialog dialog_create = alertDialog.create();
        dialog_create.show();
    }

    public void fyi_dialog(String title, String text) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
        alertDialog.setTitle(title);
        alertDialog.setMessage(text);
        alertDialog.setIcon(R.drawable.information);
        alertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        if (alertDialoShow != null) {
            alertDialoShow.dismiss();
        }
        alertDialoShow= alertDialog.create();
        alertDialoShow.show();
    }

    public void negative_dialog(String title, String text) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
        alertDialog.setTitle(title);
        alertDialog.setMessage(text);
        alertDialog.setIcon(R.drawable.error_solid);
        alertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        if (alertDialoShow != null) {
            alertDialoShow.dismiss();
        }
        alertDialoShow = alertDialog.create();
        alertDialoShow.show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(LOG_TAG, "DashboardFragment onDestroy!!!!!!!!!!!!!!!!");
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        Context context = getContext();
        Intent intent = new Intent(getActivity(), DataSyncService.class);
        context.stopService(intent);
    }
}
