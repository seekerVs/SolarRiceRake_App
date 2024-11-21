package net.techcn.solarricerakeapp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import net.techcn.solarricerakeapp.databinding.FragmentSettingsBinding;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

public class SettingsFragment extends Fragment {

    FragmentSettingsBinding binding;
    public static final String LOG_TAG = SettingsFragment.class.getSimpleName();
    FirebaseAuth auth;
    GoogleSignInClient googleSignInClient;
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;

    GlobalObject globalObject;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        sharedPreferences = context.getSharedPreferences("PREFS_DATA", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();

//        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        globalObject = new GlobalObject(context.getApplicationContext());
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Initialize UI components and set click listeners
        // Access SharedPreferences through dependency injection or Activity context
        sharedPreferences = requireContext().getSharedPreferences("PREFS_DATA", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();

//        String user_account = sharedPreferences.getString("current_email",null);
//        myRef = database.getReference().child(user_account);

        auth = FirebaseAuth.getInstance();

        String currentUser = sharedPreferences.getString("current_email", null);
        Log.d(LOG_TAG, "currentUser::::::::::::::::::::  " + currentUser );

        GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.client_id))
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(requireActivity(), options);

        String status = sharedPreferences.getString("is_notif_enabled",null);
        if (status.equals("false")) {
            binding.notificationSwitch.setChecked(false);
        } else if (status.equals("true")) {
            binding.notificationSwitch.setChecked(true);
        } else {
            binding.notificationSwitch.setChecked(false);
        }

        binding.contextMenuImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPopupMenu(v);
            }
        });

        binding.DevicesLayout.setOnClickListener(v -> {
            getFragmentManager().beginTransaction().replace(R.id.framelayout, new DevicesFragment())
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .addToBackStack(null)
                    .commit();
        });

        binding.AboutLayout.setOnClickListener(v -> {
            getFragmentManager().beginTransaction().replace(R.id.framelayout, new AboutFragment())
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .addToBackStack(null)
                    .commit();
        });

        binding.LogoutLayout.setOnClickListener(v -> {
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
            alertDialog.setTitle("Logout");
            alertDialog.setMessage("Are you sure you want to logout?");
            alertDialog.setIcon(R.drawable.information);
            alertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();
                    out_google_auth();
                    openMainActivity();
                }
            });
            alertDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });

            AlertDialog dialog_create = alertDialog.create();
            dialog_create.show();

        });

        binding.notificationSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    editor.putString("is_notif_enabled", "true");
                    editor.commit();
                    saveActivityLog("Notification enabled");
                    // Start the foreground service
                    startBackgroundService();
                } else {
                    editor.putString("is_notif_enabled", "false");
                    editor.commit();

                    saveActivityLog("Notification disabled");
                    // Stop the foreground service
                    stopBackgroundService();
                }
                Log.d(LOG_TAG, "isChecked: " + isChecked);
            }
        });

        checkDevice();
    }

    private void checkDevice() {
        String currentDevice = sharedPreferences.getString("current_device", "");
        if (currentDevice.isBlank() && globalObject.isInternetAvailable()) {
            globalObject.releasedDeviceRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshots) {
                    Log.d(LOG_TAG, "snapshots.getChildrenCount(): " + snapshots.getChildrenCount());
                    String currentEmail = sharedPreferences.getString("current_email", "");
                    boolean isDeviceSet = false;
                    String key = "";
                    String value = "";
                    for (DataSnapshot snapshot : snapshots.getChildren()) {
                        key = snapshot.getKey();
                        value = snapshot.getValue(String.class);

                        if (value.equals(currentEmail)) {
                            isDeviceSet = true;
                            break;
                        }
                        Log.d(LOG_TAG, "key: " + key + ", value: " + value);
                    }

                    if (isDeviceSet) {
                        binding.deviceNameTextview.setText(key);
                    } else {
                        binding.deviceNameTextview.setText("N/A");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        } else {
            binding.deviceNameTextview.setText(currentDevice);
        }
    }

    private void showPopupMenu(View view) {
        PopupMenu popupMenu = new PopupMenu(getContext(), view);
        MenuInflater inflater = popupMenu.getMenuInflater();
        inflater.inflate(R.menu.menu_item, popupMenu.getMenu());

        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.change_device) {
                Toast.makeText(getActivity(), "Change device", Toast.LENGTH_SHORT).show();
                scanCode();
                return true;
            } else if (itemId == R.id.remove_device) {
                Toast.makeText(getActivity(), "Remove device", Toast.LENGTH_SHORT).show();
                removeDevice();
                return true;
            } else {
                return false;
            }
        });

        popupMenu.show();
    }

    private void scanCode() {
        ScanOptions options = new ScanOptions();
        options.setPrompt("Volume up to flash on");
        options.setBeepEnabled(true);
        options.setOrientationLocked(true);
        options.setCaptureActivity(CaptureAct.class);
        barLaucher.launch(options);
    }

    ActivityResultLauncher<ScanOptions> barLaucher = registerForActivityResult(new ScanContract(), result->
    {
        String deviceName = result.getContents();
        if(deviceName != null && !deviceName.isBlank()) {
            if (globalObject.isInternetAvailable()) {
                globalObject.releasedDeviceRef.child(deviceName).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Log.d(LOG_TAG, "snapshot.child(deviceName).exists(): " + snapshot.child(deviceName).exists());
                        if (snapshot.exists()) {
                            String value = snapshot.getValue(String.class);
                            if (value != null && !value.isBlank() ) {
                                negative_dialog("QRCode Scan Failed", "The device with model name \"" + deviceName + "\" is already used by other user.");
                            } else {
                                String currentEmail = sharedPreferences.getString("current_email", "");
                                positive_dialog("QRCode Scan Successful", "The device with model name \"" + deviceName + "\" has been set as your new device. Please restart the SolaRice device.");
                                globalObject.releasedDeviceRef.child(deviceName).setValue(currentEmail);
                                binding.deviceNameTextview.setText(deviceName);
                                editor.putString("current_device", deviceName);
                                editor.commit();
                            }

                        } else {
                            negative_dialog("QRCode Scan Failed", "The device with model name \"" + deviceName + "\" does not exist.");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            } else {
                negative_dialog("QRCode Scan Failed", "Please check your internet connection. Stable internet is required to continue!");
            }
        } else {
            negative_dialog("QR Code Scan Failed", "Please try again.");
        }
    });

    private void removeDevice() {
        if (globalObject.isInternetAvailable()) {
            String currentDevice = sharedPreferences.getString("current_device", "");
            binding.deviceNameTextview.setText("N/A");
            globalObject.releasedDeviceRef.child(currentDevice).setValue("");

            editor.putString("current_device", "");
            editor.commit();
            positive_dialog("Device Removed", "Your device has been removed successfully.");
        } else {
            negative_dialog("Device Removal Failed", "Please check your internet connection. \nStable internet is required to continue!");
        }
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
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd:HH-mm:ss·SSS").withZone(ZoneId.systemDefault());
        return formatter.format(instant);
    }

    public void out_google_auth() {
        if (auth.getCurrentUser() != null) {
            auth.signOut();
            googleSignInClient.signOut();
            Log.d(LOG_TAG, "Google authentication account detected: Signing out while initializing");
            Toast.makeText(requireContext(), "Signed out", Toast.LENGTH_SHORT).show();
            sharedPreferences.edit().clear().apply();
        }
    }

    public void openMainActivity() {
        Intent intent = new Intent(getActivity(), MainActivity.class);
        startActivity(intent);
        // Pop fragments after starting the new activity
        requireActivity().getSupportFragmentManager().popBackStackImmediate();
    }

    private void startBackgroundService() {
        Context context = getContext();
        Intent intent = new Intent(getActivity(), ForegroundService.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    private void stopBackgroundService() {
        Context context = getContext();
        Intent intent = new Intent(getActivity(), ForegroundService.class);
        context.stopService(intent);
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


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}