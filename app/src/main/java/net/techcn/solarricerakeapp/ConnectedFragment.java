package net.techcn.solarricerakeapp;

import static net.techcn.solarricerakeapp.App.CHANNEL_1_ID;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.os.Handler;
import android.os.IBinder;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.images.WebImage;
import com.google.android.material.snackbar.Snackbar;

import net.techcn.solarricerakeapp.BluetoothTools.SerialListener;
import net.techcn.solarricerakeapp.BluetoothTools.SerialService;
import net.techcn.solarricerakeapp.BluetoothTools.SerialSocket;
import net.techcn.solarricerakeapp.BluetoothTools.TextUtil;
import net.techcn.solarricerakeapp.databinding.FragmentAutomaticBinding;
import net.techcn.solarricerakeapp.databinding.FragmentConnectedBinding;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashMap;

public class ConnectedFragment extends Fragment implements ServiceConnection, SerialListener {

    public static final String LOG_TAG = ConnectedFragment.class.getSimpleName();

    private enum Connected {False, Pending, True}

    private String deviceAddress;
    private SerialService service;

    private TextView receiveText;
    private TextView sendText;
    private TextUtil.HexWatcher hexWatcher;

    public Connected connected = Connected.False;
    private boolean initialStart = true;
    private boolean hexEnabled = false;
    private boolean pendingNewline = false;
    private String newline = TextUtil.newline_crlf;
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;
    FragmentConnectedBinding binding;

    Context context;
    GlobalObject globalObject;

    /*
     * Lifecycle
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        deviceAddress = getArguments().getString("device");
        Log.d(LOG_TAG, "device: " + deviceAddress);

    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
        sharedPreferences = context.getSharedPreferences("PREFS_DATA", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
        globalObject = new GlobalObject(context.getApplicationContext());
    }

    @Override
    public void onDestroy() {
        Log.d(LOG_TAG, "onDestroy");
        if (connected != Connected.False)
            disconnect();
        getActivity().stopService(new Intent(getActivity(), SerialService.class));
        super.onDestroy();
    }

    @Override
    public void onStart() {
        Log.d(LOG_TAG, "onStart");
        super.onStart();
        if (service != null)
            service.attach(this);
        else
            getActivity().startService(new Intent(getActivity(), SerialService.class)); // prevents service destroy on unbind from recreated activity caused by orientation change
    }

    @Override
    public void onStop() {
        Log.d(LOG_TAG, "onStop");
//        if(service != null && !getActivity().isChangingConfigurations())
//            service.detach();
        super.onStop();
    }

    @SuppressWarnings("deprecation")
    // onAttach(context) was added with API 23. onAttach(activity) works for all API versions
    @Override
    public void onAttach(@NonNull Activity activity) {
        super.onAttach(activity);
        Log.d(LOG_TAG, "onAttach");
        getActivity().bindService(new Intent(getActivity(), SerialService.class), this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDetach() {
//        Log.d("TerminalFragment", "onDetach");
//        try { getActivity().unbindService(this); } catch(Exception ignored) {}
        super.onDetach();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(LOG_TAG, "onResume");
        if (service != null)
            service.attach(this);
        if (initialStart && service != null) {
            initialStart = false;
            getActivity().runOnUiThread(this::connect);
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        Log.d(LOG_TAG, "onServiceConnected");
        service = ((SerialService.SerialBinder) binder).getService();
        service.attach(this);
        if (initialStart && isResumed()) {
            initialStart = false;
            getActivity().runOnUiThread(this::connect);
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        service = null;
        Log.d(LOG_TAG, "onServiceDisconnected");
    }

    /*
     * UI
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(LOG_TAG, "onCreateView");
        // Inflate the layout for this fragment
        editor = sharedPreferences.edit();
        binding = FragmentConnectedBinding.inflate(inflater, container, false);

        hexWatcher = new TextUtil.HexWatcher(sendText);
        hexWatcher.enable(hexEnabled);

        return binding.getRoot();
    }

    /*
     * Serial + UI
     */
    private void connect() {
        try {
            binding.connectionTextview.setText("Connecting...");
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
            status("connecting...");
            connected = Connected.Pending;
            SerialSocket socket = new SerialSocket(getActivity().getApplicationContext(), device);
            service.connect(socket);
        } catch (Exception e) {
            onSerialConnectError(e);
        }
    }

    public void disconnect() {
        connected = Connected.False;
        if (sharedPreferences.getString("bt_status", "Disconnected").equals("Connected")) {
            editor.putString("device_status", "OFF");
            editor.commit();
        }
        editor.putString("bt_status", "Disconnected");
        editor.commit();
        service.disconnect();
    }

    public void send(String str) {
        Log.d("ConnectedFragment", "send: " + str);
        if(connected != Connected.True) {
//            Toast.makeText(getActivity(), "not connected", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            String msg;
            byte[] data;
            if(hexEnabled) {
                StringBuilder sb = new StringBuilder();
                TextUtil.toHexString(sb, TextUtil.fromHexString(str));
                TextUtil.toHexString(sb, newline.getBytes());
                msg = sb.toString();
                data = TextUtil.fromHexString(msg);
            } else {
                msg = str;
                data = (str + newline).getBytes();
            }
            SpannableStringBuilder spn = new SpannableStringBuilder(msg + '\n');
            spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorSendText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//            receiveText.append(spn);
            service.write(data);
            Log.d("ConnectedFragment", "send: " + msg);
        } catch (Exception e) {
            onSerialIoError(e);
        }
    }

    public void receive(ArrayDeque<byte[]> datas) {
        SpannableStringBuilder spn = new SpannableStringBuilder();
        for (byte[] data : datas) {
            if (hexEnabled) {
                spn.append(TextUtil.toHexString(data)).append('\n');
            } else {
                String msg = new String(data);
                if (newline.equals(TextUtil.newline_crlf) && msg.length() > 0) {

                    msg = msg.replace(TextUtil.newline_crlf, TextUtil.newline_lf);

                    if (pendingNewline && msg.charAt(0) == '\n') {
                        if(spn.length() >= 2) {
                            spn.delete(spn.length() - 2, spn.length());
                        } else {
                            Editable edt = receiveText.getEditableText();
                            if (edt != null && edt.length() >= 2)
                                edt.delete(edt.length() - 2, edt.length());
                        }
                    }
                    pendingNewline = msg.charAt(msg.length() - 1) == '\r';
                }
                spn.append(TextUtil.toCaretString(msg, newline.length() != 0));
            }
        }
//        receiveText.append(spn);
        Log.d(LOG_TAG, "RECEIVED MESSAGE!!!!!!!!!!!!!!:     " + spn);
        processReceivedData(spn.toString());

    }

    private void status(String str) {
//        SpannableStringBuilder spn = new SpannableStringBuilder(str + '\n');
//        spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorStatusText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//        Toast.makeText(getActivity(), "Status: " + str, Toast.LENGTH_SHORT).show();
//        receiveText.append(spn);
        Log.d("ConnectedFragment", "status: " + str);
    }

    /*
     * starting with Android 14, notifications are not shown in notification bar by default when App is in background
     */

    private void showNotificationSettings() {
        Intent intent = new Intent();
        intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
        intent.putExtra("android.provider.extra.APP_PACKAGE", getActivity().getPackageName());
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(Arrays.equals(permissions, new String[]{Manifest.permission.POST_NOTIFICATIONS}) &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !service.areNotificationsEnabled())
            showNotificationSettings();
    }

    /*
     * SerialListener
     */
    @Override
    public void onSerialConnect() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);

        // Check Bluetooth device name before connecting
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        String currentDevice = sharedPreferences.getString("current_device", "");
//        Toast.makeText(getActivity(), "currentDevice: " + currentDevice, Toast.LENGTH_SHORT).show();
//        if (!device.getName().equals(currentDevice)) {
//            status("Incorrect Device\nDisconnected");
//            binding.connectionTextview.setText("Incorrect Device\nDisconnected");
//            disconnect();
//        } else {
//            status("connected");
//            connected = Connected.True;
//            if (sharedPreferences.getString("connection_mode", "").equals("BT")) {
//                editor.putString("device_status", "ON");
//                editor.commit();
//            }
//            editor.putString("bt_status", "Connected");
//            editor.commit();
//            binding.connectionTextview.setText("Connected");
//        }
        status("connected");
        connected = Connected.True;
        if (sharedPreferences.getString("connection_mode", "").equals("BT")) {
            editor.putString("device_status", "ON");
            editor.commit();
        }
        editor.putString("bt_status", "Connected");
        editor.commit();
        binding.connectionTextview.setText("Connected");

        hideThisFragment();
    }

    @Override
    public void onSerialConnectError(Exception e) {
        binding.connectionTextview.setText("ERROR");
        Toast.makeText(getActivity().getApplicationContext(), "ERROR: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        status("connection failed: " + e.getMessage());
        disconnect();
        hideThisFragment();
    }

    @Override
    public void onSerialRead(byte[] data) {
        ArrayDeque<byte[]> datas = new ArrayDeque<>();
        datas.add(data);
        receive(datas);
    }

    public void onSerialRead(ArrayDeque<byte[]> datas) {
        receive(datas);
    }

    @Override
    public void onSerialIoError(Exception e) {
        status("connection lost: " + e.getMessage());
        disconnect();
    }

    private void hideThisFragment() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                FragmentManager fragmentManager = getParentFragmentManager();
                fragmentManager.beginTransaction().hide(ConnectedFragment.this).commit();
            }
        }, 1500);
    }

    private void processReceivedData(String data) {
        if (sharedPreferences.getString("connection_mode", "").equals("BT")) {
            if (data != null && !data.isEmpty()) {
                Log.d("ConnectedFragment", "processReceivedData: " + data);
                if (data.contains("\n")) {
                    data = data.replace("\n", "");
                }
                if (data.contains(",")) {
                    String[] strList = data.split(",");
                    String key = strList[0];
                    String value1 = strList[1];
                    String value2 = "";

                    Log.d(LOG_TAG, "strList.length > 3: " + strList.length);
                    if (strList.length >= 3) {
                        value2 = strList[2];
                    }
                    Log.d(LOG_TAG, "key: " + key);
                    Log.d(LOG_TAG, " value1: " + value1);
                    Log.d(LOG_TAG, " value2: " + value2);

                    Log.d(LOG_TAG, "key: " + key + " value: " + value1);

//                    editor.putSts
                    switch (key) {
                        case "moisture":
                            editor.putString("moisture_status", value2);
                            editor.commit();

                            editor.putString(key, value1);
                            editor.commit();
                            break;
                        case "is_automatic_mode":
                            editor.putString(key, value1);
                            editor.commit();
                            break;
                        case "is_no_obstacle":
                            editor.putString(key, value1);
                            editor.commit();
                            break;
                        case "automatic_status":
                            editor.putString(key, value1);
                            editor.commit();
                            break;
                    }
                }
                if (data.equals("automatic stopped!")) {
                    Log.d(LOG_TAG, "if-satement = automatic stopped!");
                }
            }
        }
    }

}