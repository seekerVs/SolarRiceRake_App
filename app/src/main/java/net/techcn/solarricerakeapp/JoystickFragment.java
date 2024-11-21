package net.techcn.solarricerakeapp;

import static android.content.DialogInterface.BUTTON_POSITIVE;

import static net.techcn.solarricerakeapp.App.CHANNEL_1_ID;

import android.app.AlertDialog;
import android.app.Notification;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import net.techcn.solarricerakeapp.databinding.FragmentControlsBinding;
import net.techcn.solarricerakeapp.databinding.FragmentJoystickBinding;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;

import io.github.controlwear.virtual.joystick.android.JoystickView;


public class JoystickFragment extends Fragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String LOG_TAG = JoystickFragment.class.getSimpleName();
    FragmentJoystickBinding binding;
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;

    GlobalObject globalObject;

    FragmentManager fragmentManager;
    ConnectedFragment connectedFragment;

    String manualStartTimestamp = "";
    String controlMode = "Manual";

    AlertDialog alertDialogCreate;

    private NotificationManagerCompat notificationManager;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentJoystickBinding.inflate(inflater, container, false);
        fragmentManager = getParentFragmentManager();
        connectedFragment = (ConnectedFragment) fragmentManager.findFragmentByTag("connectedAcitivity");

        return binding.getRoot();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        sharedPreferences = context.getSharedPreferences("PREFS_DATA", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);

        globalObject = new GlobalObject(context.getApplicationContext());

        notificationManager = NotificationManagerCompat.from(context);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

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

        binding.joystickView.setOnMoveListener(new JoystickView.OnMoveListener() {
            @Override
            public void onMove(int angle, int strength) {
                if (manualStartTimestamp.isEmpty()) {
                    AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
                    alertDialog.setTitle("Manual Mode");
                    alertDialog.setMessage("Do you want to start manual mode?");
                    alertDialog.setIcon(R.drawable.success_filled);
                    alertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            manualStartTimestamp = getDate();
                            binding.endManualButton.setVisibility(View.VISIBLE);
                            positive_dialog("Manual Mode", "Manual Mode Started");
                            editor.putString("manual_start_timestamp", manualStartTimestamp);
                            editor.commit();
                            dialog.dismiss();

                            sendCode("Z");
                            saveActivityLog("Manual Mode Started");
                        }
                    });
                    alertDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
//                    AlertDialog dialog_create = alertDialog.create();
//                    dialog_create.show();
                    if (alertDialogCreate != null) {
                        alertDialogCreate.dismiss();
                    }
                    alertDialogCreate = alertDialog.create();
                    alertDialogCreate.show();
                } else {
                    String msg = "";

                    if (angle == 0) {
                        msg = "S";
                        Log.d(LOG_TAG, "Stop");
                    } else {
                        Log.d(ControlsFragment.class.getSimpleName(), "connectedFragment: entryy");

                        if (angle > 45 && angle < 135) {
                            msg = "F";
//                            Toast.makeText(getContext(), "Forward", Toast.LENGTH_SHORT).show();
                            Log.d(LOG_TAG, "Forward");
                        } else if (angle > 135 && angle < 225) {
                            msg = "L";
//                            Toast.makeText(getContext(), "Left", Toast.LENGTH_SHORT).show();
                            Log.d(LOG_TAG, "Left");
                        } else if (angle < 45 || angle > 315) {
                            msg = "R";
//                            Toast.makeText(getContext(), "Right", Toast.LENGTH_SHORT).show();
                            Log.d(LOG_TAG, "Right");
                        } else if (angle > 225 && angle < 315) {
                            msg = "B";
//                            Toast.makeText(getContext(), "Backward", Toast.LENGTH_SHORT).show();
                            Log.d(LOG_TAG, "Backward");
                        }
                    }
                    sendCode(msg);
                }

            }
        });

        binding.endManualButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveDryingTime();
                editor.putString("manual_start_timestamp", "");
                editor.commit();
                binding.endManualButton.setVisibility(View.GONE);
                negative_dialog("Manual Mode", "Manual Mode Stopped");

                sendCode("X");
                saveActivityLog("Manual Mode Stopped");
            }
        });
    }

    private void sendCode(String msg) {
        if (sharedPreferences.getString("connection_mode", "").equals("BT")) {
            if (globalObject.isBluetoothAvailable() && sharedPreferences.getString("bt_status", "Disconnected").equals("Connected")) {
                bluetoothMode(msg);
            } else {
                Toast.makeText(getContext().getApplicationContext(), "Bluetooth is not available", Toast.LENGTH_SHORT).show();
            }
        } else {
            if (globalObject.isInternetAvailable()) {
                wifiMode(msg);
            } else {
                Toast.makeText(getContext().getApplicationContext(), "WiFi is not available", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void saveDryingTime() {
        String startTimestamp = sharedPreferences.getString("manual_start_timestamp", "");
        String endTimestamp = getDate();
        String dryingDuration = getDryingDuration(startTimestamp, endTimestamp);

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
        String result = "";
        try {
            Log.d(LOG_TAG, "startTimestamp: " + startTimestamp);
            Log.d(LOG_TAG, "endTimestamp: " + endTimestamp);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd;HH:mm:ss·SSS");

            LocalDateTime startDateTime = LocalDateTime.parse(startTimestamp, formatter);
            LocalDateTime endDateTime = LocalDateTime.parse(endTimestamp, formatter);

            Duration duration = Duration.between(startDateTime, endDateTime);

            long hours = duration.toHours();
            long minutes = duration.toMinutes() % 60;
            long seconds = duration.getSeconds() % 60;
            long millis = duration.toMillis() % 1000;

            result = String.format(Locale.ROOT, "%02d:%02d:%02d.%03d", hours, minutes, seconds, millis);
            Log.d(LOG_TAG, "Duration: " + result);
        } catch (Exception e) {
            Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        return result;
    }

    private void wifiMode(String msg) {
        Log.d(LOG_TAG, "Data sent through wifiMode!!!!!");
        globalObject.deviceRef.setValue("");
        globalObject.deviceRef.setValue(msg);
    }

    private void bluetoothMode(String msg) {
//        Log.d(LOG_TAG, "Data sent through btmode!!!!!");
        ConnectedFragment connectedFragment = (ConnectedFragment) getParentFragmentManager().findFragmentByTag("connectedAcitivity");
        Log.d(ControlsFragment.class.getSimpleName(), "connectedFragment != null " + String.valueOf(connectedFragment != null));
        if (connectedFragment != null) {
            connectedFragment.send(msg);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String key) {
        Log.d(LOG_TAG, "onSharedPreferenceChanged: " + key);
        if (key != null) {
            if (key.equals("is_no_obstacle")) {
                Log.d(LOG_TAG, "JoystickFragment is_no_obstacle: " + sharedPreferences.getString("is_no_obstacle", "false"));
                if (sharedPreferences.getString("is_no_obstacle", "true").equals("false")) {
                    AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
                    alertDialog.setTitle("Manual Mode");
                    alertDialog.setMessage("Obstacle is detected. Remove the obstacle before clicking OK");
                    alertDialog.setIcon(R.drawable.information);
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
            }
        }

    }

    private void saveActivityLog(String message) {
        String timestamp = getDate();

        HashMap<String, Object> dataToSave = new HashMap<>();
        dataToSave.put("timestamp", timestamp);
        dataToSave.put("activity", message);
        globalObject.activityLogRef.child(timestamp).setValue(dataToSave);
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
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd;HH:mm:ss·SSS").withZone(ZoneId.systemDefault());
        return formatter.format(instant);
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(LOG_TAG, LOG_TAG + " onDestroyView");
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.d(LOG_TAG, LOG_TAG + " onDetach");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(LOG_TAG, LOG_TAG + " onStop");
        sendCode("X");
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(LOG_TAG, LOG_TAG + " onCreate");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(LOG_TAG, LOG_TAG + " onPause");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(LOG_TAG, LOG_TAG + " onResume");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(LOG_TAG, LOG_TAG + " onDestroy");
    }
}