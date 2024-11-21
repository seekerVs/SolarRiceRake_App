package net.techcn.solarricerakeapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.material.tabs.TabLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import net.techcn.solarricerakeapp.databinding.FragmentControlsBinding;

import org.checkerframework.checker.units.qual.A;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import io.github.controlwear.virtual.joystick.android.JoystickView;

public class ControlsFragment extends Fragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String LOG_TAG = ControlsFragment.class.getSimpleName();

    FragmentControlsBinding binding;
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;

    GlobalObject globalObject;

    AlertDialog alertDialogShow;

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String key) {
        Log.d(LOG_TAG, "Shared preference changed: " + key);
//        Toast.makeText(getContext().getApplicationContext(),LOG_TAG + ":" + key, Toast.LENGTH_SHORT).show();
        if (key != null) {
            if(key.equals("moisture")) {
                String val = sharedPreferences.getString("moisture", "N/A");
                String desc = sharedPreferences.getString("moisture_status", "");
                binding.latestMoistureTextView.setText("Latest Moisture Level: " + val);
                fyi_dialog("Moisture Status", "Moisture value: " + val + "\n" + desc);
            }
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        globalObject.moistureRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (sharedPreferences.getString("connection_mode", "").equals("WiFi")) {
                    if (snapshot.exists()) {
                        List<DataSnapshot> childSnapshots = new ArrayList<>();
                        for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                            childSnapshots.add(childSnapshot);
                        }
                        //Reverse
                        Collections.reverse(childSnapshots);

                        String val = childSnapshots.get(0).child("value").getValue(String.class);
                        String desc = childSnapshots.get(0).child("description").getValue(String.class);
                        editor.putString("moisture_status", desc);
                        editor.commit();

                        editor.putString("moisture", val);
                        editor.commit();

                    } else {
                        binding.latestMoistureTextView.setText("Latest Moisture Level: N/A");
                    }
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                binding.latestMoistureTextView.setText("Latest Moisture Level: N/A");
                Log.d(LOG_TAG, "Failed to read value.", error.toException());
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentControlsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        sharedPreferences = context.getSharedPreferences("PREFS_DATA", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        globalObject = new GlobalObject(context.getApplicationContext());
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setPreferenceBasedValues();

        getParentFragmentManager().beginTransaction().replace(R.id.control_container, new JoystickFragment())
                .addToBackStack(null)
                .commit();

        binding.tablayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        Log.d(LOG_TAG, "Tab selected: " + tab.getPosition());
                        Log.d(LOG_TAG, "ControlsFragment is_automatic_mode: " + sharedPreferences.getString("is_automatic_mode", "false"));
                        if (sharedPreferences.getString("is_automatic_mode", "false").equals("true")) {
                            AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
                            alertDialog.setTitle("Warning");
                            alertDialog.setMessage("It looks like you're in automatic mode. Do you want to exit?");
                            alertDialog.setIcon(R.drawable.information);
                            alertDialog.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.dismiss();
                                    editor.putString("is_automatic_mode", "false");
                                    editor.commit();
                                    getParentFragmentManager().beginTransaction().replace(R.id.control_container, new JoystickFragment())
                                            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                                            .commit();

                                    binding.latestMoistureTextView.setText("Latest Moisture Level: N/A");
                                    binding.moistCheckBtn.setEnabled(true);

                                    saveActivityLog("Manual Mode Stopped");
                                }
                            });
                            alertDialog.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    TabLayout.Tab tab = binding.tablayout.getTabAt(1);
                                    tab.select();

                                    dialog.dismiss();
                                }
                            });
                            if (alertDialogShow != null) {
                                alertDialogShow.dismiss();
                            }
                            alertDialogShow = alertDialog.create();
                            alertDialogShow.show();
                            Log.d(LOG_TAG, "Alert dialog show");
                        } else {
                            getParentFragmentManager().beginTransaction().replace(R.id.control_container, new JoystickFragment())
                                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                                    .commit();
                            binding.moistCheckBtn.setEnabled(true);
                        }
                        break;
                    case 1:
                        Log.d(LOG_TAG, "Tab selected: " + tab.getPosition());
                        Log.d(LOG_TAG, "ControlsFragment manual_start_timestamp: " + sharedPreferences.getString("is_automatic_mode", "false"));
                        if (!sharedPreferences.getString("manual_start_timestamp", "").isEmpty()) {
                            AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
                            alertDialog.setTitle("Warning");
                            alertDialog.setMessage("It looks like you're in manual mode. Do you want to exit?");
                            alertDialog.setIcon(R.drawable.information);
                            alertDialog.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    editor.putString("manual_start_timestamp", "");
                                    editor.commit();
                                    Log.d(LOG_TAG, "Tab selected: " + tab.getPosition());
                                    getParentFragmentManager().beginTransaction().replace(R.id.control_container, new AutomaticFragment())
                                            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                                            .commit();
                                    binding.latestMoistureTextView.setText("Latest Moisture Level: N/A");
                                    binding.moistCheckBtn.setEnabled(false);
                                    dialog.dismiss();
                                }
                            });
                            alertDialog.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    TabLayout.Tab tab = binding.tablayout.getTabAt(0);
                                    tab.select();

                                    dialog.dismiss();
                                }
                            });
                            if (alertDialogShow != null) {
                                alertDialogShow.dismiss();
                            }
                            alertDialogShow = alertDialog.create();
                            alertDialogShow.show();
                            Log.d(LOG_TAG, "Alert dialog show");
                        } else {
                            Log.d(LOG_TAG, "Tab selected: " + tab.getPosition());
                            getParentFragmentManager().beginTransaction().replace(R.id.control_container, new AutomaticFragment())
                                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                                    .commit();
                            binding.latestMoistureTextView.setText("Latest Moisture Level: N/A");
                            binding.moistCheckBtn.setEnabled(false);
                        }
                        break;
                    case 2:

                        getParentFragmentManager().beginTransaction().replace(R.id.control_container, new DrawingFragment())
                                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                                .commit();
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        binding.moistCheckBtn.setOnClickListener(v -> {
            Log.d(LOG_TAG, "Moisture check button clicked");
            if (sharedPreferences.getString("connection_mode", null).equals("BT")) {
                if (globalObject.isBluetoothAvailable() && sharedPreferences.getString("bt_status", "Disconnected").equals("Connected")) {
                    Log.d(LOG_TAG, "Data sent through btmode!!!!!");
                    ConnectedFragment connectedFragment = (ConnectedFragment) getParentFragmentManager().findFragmentByTag("connectedAcitivity");
                    Log.d(ControlsFragment.class.getSimpleName(), "connectedFragment != null " + String.valueOf(connectedFragment != null));
                    if (connectedFragment != null) {
                        connectedFragment.send("O");
                    }
                } else {
                    Toast.makeText(getContext().getApplicationContext(), "Bluetooth is not available", Toast.LENGTH_SHORT).show();
                }
            } else {
                if (globalObject.isInternetAvailable()) {
                    globalObject.deviceRef.setValue("");
                    globalObject.deviceRef.setValue("O");
                } else {
                    Toast.makeText(getActivity(), "WiFi is not available", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        Log.d(LOG_TAG, "ControlsFragment DestroyView");
    }

    private void setPreferenceBasedValues() {
        String connectionMode = sharedPreferences.getString("connection_mode", "N/A");
            if (connectionMode.equals("BT")) {
                connectionMode = "Bluetooth";
            }

        binding.modeTxtview.setText(connectionMode);
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
//        AlertDialog dialog_create = alertDialog.create();
//        dialog_create.show();
        if (alertDialogShow != null) {
            alertDialogShow.dismiss();
        }
        alertDialogShow = alertDialog.create();
        alertDialogShow.show();
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(LOG_TAG, "ControlsFragment Destroy");
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);

//        getParentFragmentManager().beginTransaction().remove(new AutomaticFragment()).commit();
//        getParentFragmentManager().beginTransaction().detach(new JoystickFragment()).commit();
    }
}