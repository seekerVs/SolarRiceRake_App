package net.techcn.solarricerakeapp;

import static net.techcn.solarricerakeapp.App.CHANNEL_1_ID;

import android.app.AlertDialog;
import android.app.Notification;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import net.techcn.solarricerakeapp.databinding.FragmentAutomaticBinding;
import net.techcn.solarricerakeapp.databinding.FragmentJoystickBinding;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;

public class AutomaticFragment extends Fragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String LOG_TAG = AutomaticFragment.class.getSimpleName();

    FragmentAutomaticBinding binding;

    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;

//    FirebaseDatabase database = FirebaseDatabase.getInstance();
//    DatabaseReference myRef;

    GlobalObject globalObject;

    FragmentManager fragmentManager;
    ConnectedFragment connectedFragment;

    String autoStartTimestamp = "";
    String controlMode = "Automatic";

    private NotificationManagerCompat notificationManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(LOG_TAG, "AutomaticFragment onCreate!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! ");
        fragmentManager = getParentFragmentManager();
        connectedFragment = (ConnectedFragment) fragmentManager.findFragmentByTag("connectedAcitivity");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentAutomaticBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        sharedPreferences = requireContext().getSharedPreferences("PREFS_DATA", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();

        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        globalObject = new GlobalObject(context.getApplicationContext());

        notificationManager = NotificationManagerCompat.from(context);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        globalObject.automaticStatusRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (sharedPreferences.getString("connection_mode", "").equals("WiFi")) {
                    if (snapshot.exists() && snapshot.getValue() != null) {
                        if (binding.autoModeSwitch.isChecked()) {
                            editor.putString("automatic_status", "");
                            editor.commit();
                            if (snapshot.getValue().equals("false")) {
                                editor.putString("automatic_status", "failed");
                                editor.commit();
                            } else if (snapshot.getValue().equals("success")) {
                                editor.putString("automatic_status", "success");
                                editor.commit();
                            }
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d(LOG_TAG, "AutomaticFragment onCancelled!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! ");
            }
        });

        globalObject.isAutomaticModeRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (sharedPreferences.getString("connection_mode", "").equals("WiFi")) {
                    if (snapshot.exists() && snapshot.getValue() != null) {
                        if (snapshot.getValue().equals("false")) {
                            editor.putString("is_automatic_mode", "");
                            editor.commit();
                            editor.putString("is_automatic_mode", "false");
                            editor.commit();
                            binding.autoModeSwitch.setChecked(false);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        globalObject.isNoObstacleRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (sharedPreferences.getString("connection_mode", "").equals("WiFi")) {
                    if (snapshot.exists() && snapshot.getValue() != null) {
                        if (snapshot.getValue().equals("false")) {
                            editor.putString("is_no_obstacle", "");
                            editor.commit();
                            editor.putString("is_no_obstacle", "false");
                            editor.commit();
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        binding.autoModeSwitch.setOnCheckedChangeListener(new SwitchCompat.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (!sharedPreferences.getString("is_automatic_mode", "false").equals("true")) {
                        Log.d(LOG_TAG, "connectedFragment: isChecked");
                        String msg = "M";
                        if (sharedPreferences.getString("connection_mode", "").equals("BT")) {
                            if (globalObject.isBluetoothAvailable() && sharedPreferences.getString("bt_status", "Disconnected").equals("Connected")) {
                                bluetoothMode(msg);
                                editor.putString("is_automatic_mode", "true");
                                editor.commit();
                                editor.putString("is_no_obstacle", "true");
                                editor.commit();

//                                autoStartTimestamp = getDate();

                                Log.d(LOG_TAG, "AutomaticFragment is_automatic_mode: " + sharedPreferences.getString("is_automatic_mode", "false"));
                                positive_dialog("Automatic Mode", "Automatic mode is started successfully");
                            } else {
                                binding.autoModeSwitch.setChecked(false);
                                Toast.makeText(getActivity(), "Bluetooth is not available", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            if (globalObject.isInternetAvailable()) {
                                globalObject.deviceRef.setValue(msg);
                                globalObject.isAutomaticModeRef.setValue("true");

                                editor.putString("is_automatic_mode", "true");
                                editor.commit();
                                editor.putString("is_no_obstacle", "true");
                                editor.commit();

                                autoStartTimestamp = getDate();
                                saveActivityLog("Automatic Mode Started");
                                positive_dialog("Automatic Mode", "Automatic mode is started successfully");

                            } else {
                                binding.autoModeSwitch.setChecked(false);
                                Toast.makeText(getActivity(), "WiFi is not available", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                } else {
                    if (!sharedPreferences.getString("is_automatic_mode", "false").equals("false")) {
                        Log.d(LOG_TAG, "connectedFragment: notChecked");
                        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
                        alertDialog.setTitle("Automatic Mode");
                        alertDialog.setMessage("Are you sure to stop automatic mode?");
                        alertDialog.setIcon(R.drawable.error_solid);
                        alertDialog.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                if (sharedPreferences.getString("connection_mode", "").equals("BT")) {
                                    bluetoothMode("S");
                                    editor.putString("is_automatic_mode", "false");
                                    editor.commit();

                                    Log.d(LOG_TAG, "AutomaticFragment is_automatic_mode: " + sharedPreferences.getString("is_automatic_mode", "false"));
                                } else {
                                    globalObject.deviceRef.setValue("S");
                                    globalObject.isAutomaticModeRef.setValue("false");
                                    saveDryingTime();
                                    saveActivityLog("Automatic Mode Stopped");
                                    editor.putString("is_automatic_mode", "false");
                                    editor.commit();
                                }

                                dialog.dismiss();
                            }
                        });
                        alertDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                binding.autoModeSwitch.setChecked(true);
                                dialog.dismiss();
                            }
                        });
                        AlertDialog dialog_create = alertDialog.create();
                        dialog_create.show();
                    }
                }
            }
        });

        if (sharedPreferences.getString("is_automatic_mode", "false").equals("true")) {
            binding.autoModeSwitch.setChecked(true);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String key) {
        String val = sharedPreferences.getString(key, "none");

        Log.d(LOG_TAG, "AutomaticFragment s: " + key + ":" + val);
//        Toast.makeText(getActivity(), "AutomaticFragment onSharedPreferenceChanged: " + key + ":" + val, Toast.LENGTH_SHORT).show();
        if (key != null) {
            if (key.equals("is_automatic_mode")) {
//                Log.d(LOG_TAG, "AutomaticFragment is_automatic_mode..contains(\"\\n\")): " + sharedPreferences.getString("is_automatic_mode", "false").contains("\n"));
                Log.d(LOG_TAG, "sharedPreferences.getString(\"is_automatic_mode\", \"false\").equals(\"false\"): " + sharedPreferences.getString("is_automatic_mode", "false").equals("false"));
                if (sharedPreferences.getString("is_automatic_mode", "false").equals("false")) {
                    positive_dialog("Automatic Mode", "Automatic mode is stopped successfully");
                    Log.d(LOG_TAG, "AutomaticFragment is_automatic_mode: Automatic mode is stopped successfully");
                    binding.autoModeSwitch.setChecked(false);
                }
            } else if (key.equals("is_no_obstacle")) {
                Log.d(LOG_TAG, "AutomaticFragment is_no_obstacle: " + sharedPreferences.getString("is_no_obstacle", "false"));
                if (sharedPreferences.getString("is_no_obstacle", "true").equals("false")) {
                    AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
                    alertDialog.setTitle("Automatic Mode");
                    alertDialog.setMessage("Obstacle is detected. Remove the obstacle before clicking OK");
                    alertDialog.setIcon(R.drawable.success_filled);
                    alertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            if (sharedPreferences.getString("connection_mode", "").equals("BT")) {
                                bluetoothMode("T");
                                editor.putString("is_no_obstacle", "true");
                                editor.commit();
                            }
                            if (sharedPreferences.getString("connection_mode", "").equals("WiFi")) {
                                globalObject.isNoObstacleRef.setValue("");
                                globalObject.isNoObstacleRef.setValue("true");
                                editor.putString("is_no_obstacle", "true");
                                editor.commit();
                            }
                            dialog.dismiss();
                        }
                    });
                    AlertDialog dialog_create = alertDialog.create();
                    dialog_create.show();

                    createNotification("Obstacle Detected", "Obstacle is detected. Remove the obstacle before clicking OK");
                    if (globalObject.isInternetAvailable()) {
                        saveNotification("Obstacle Detected", "Obstacle is detected in front of the device.");
                    }
                }
            } else if (key.equals("automatic_status")) {
                binding.autoModeSwitch.setChecked(false);
                Log.d(LOG_TAG, "AutomaticFragment automatic_status: " + sharedPreferences.getString("automatic_status", "failed"));
                if (sharedPreferences.getString("automatic_status", "failed").equals("success")) {
                    positive_dialog("Automatic Mode", "Automatic mode drying is complete!");
                    Log.d(LOG_TAG, "AutomaticFragment automatic_status: Automatic mode is completed successfully");
                } else {
                    negative_dialog("Automatic Mode", "No rice grain detected during initial detection. Automatic mode drying is cancelled! No data saved");
                    Log.d(LOG_TAG, "AutomaticFragment automatic_status: Automatic mode is stopped successfully");
                }
            }
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

    public void createNotification(String title, String message) {
        Bitmap img = BitmapFactory.decodeResource(getResources(), R.drawable.information);
        Notification notification = new NotificationCompat.Builder(getContext(), CHANNEL_1_ID)
                .setLargeIcon(img)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .build();

        if (ActivityCompat.checkSelfPermission(getContext(), android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
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

    private void saveActivityLog(String message) {
        String timestamp = getDate();

        HashMap<String, Object> dataToSave = new HashMap<>();
        dataToSave.put("timestamp", timestamp);
        dataToSave.put("activity", message);
        globalObject.activityLogRef.child(timestamp).setValue(dataToSave);
    }

    private void saveDryingTime() {
        String startTimestamp = autoStartTimestamp;
        String endTimestamp = getDate();
        String dryingDuration = getDryingDuration(startTimestamp, endTimestamp);

//        if(dryingDuration.equals("00:03:00.000")) {
//
//        }
        HashMap<String, Object> dataToSave = new HashMap<>();
        dataToSave.put("start_timestamp", startTimestamp);
        dataToSave.put("end_timestamp", endTimestamp);
        dataToSave.put("duration", dryingDuration);
        dataToSave.put("mode", controlMode);
        globalObject.dryingHistoryRef.child(endTimestamp).setValue(dataToSave).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                HashMap<String, Object> notifToSave = new HashMap<>();
                notifToSave.put("timestamp", endTimestamp);
                notifToSave.put("message", "Rice drying in manual mode is completed.");
                notifToSave.put("title", "Drying Status");
                globalObject.notifRef.child(endTimestamp).setValue(notifToSave);
            }
        });
    }

    private String getDryingDuration(String startTimestamp, String endTimestamp) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd:HH-mm:ss·SSS");

        LocalDateTime startDateTime = LocalDateTime.parse(startTimestamp, formatter);
        LocalDateTime endDateTime = LocalDateTime.parse(endTimestamp, formatter);

        Duration duration = Duration.between(startDateTime, endDateTime);

        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;
        long seconds = duration.getSeconds() % 60;
        long millis = duration.toMillis() % 1000;

        String result = String.format(Locale.ROOT, "%02d:%02d:%02d.%03d", hours, minutes, seconds, millis);
        Log.d(LOG_TAG, "Duration: " + result);
        return result;
    }

    private void bluetoothMode(String msg) {
//        Log.d(LOG_TAG, "Data sent through btmode!!!!!");
        ConnectedFragment connectedFragment = (ConnectedFragment) getParentFragmentManager().findFragmentByTag("connectedAcitivity");
        Log.d(ControlsFragment.class.getSimpleName(), "connectedFragment != null " + String.valueOf(connectedFragment != null));
        if (connectedFragment != null) {
            connectedFragment.send(msg);
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
        AlertDialog dialog_create = alertDialog.create();
        dialog_create.show();
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
        AlertDialog dialog_create = alertDialog.create();
        dialog_create.show();
    }

    private String getDate() {
        Instant instant = Instant.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd:HH-mm:ss·SSS").withZone(ZoneId.systemDefault());
        return formatter.format(instant);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(LOG_TAG, "AutomaticFragment onDestroy!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! ");
//        editor.putString("is_automatic_mode", "false");
//        editor.commit();
        // Set the is_automatic_mode to false in SharedPreferences
        // Send "S" to current mode of communication to stop automatic mode
        // if in WiFi mode and is connected, add data to
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(LOG_TAG, LOG_TAG + " onDestroyView");
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }
}