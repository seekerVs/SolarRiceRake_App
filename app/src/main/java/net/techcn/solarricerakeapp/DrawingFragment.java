package net.techcn.solarricerakeapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import net.techcn.solarricerakeapp.databinding.FragmentDrawingBinding;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

public class DrawingFragment extends Fragment {
    public static final String LOG_TAG = DrawingFragment.class.getSimpleName();
    BluetoothAdapter adapter;
    GlobalObject globalObject;
    FragmentDrawingBinding binding;
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;
    ConnectedFragment connectedFragment;
    BluetoothDevice MiDevice;
    FragmentManager fragmentManager;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        Log.d("DrawingFragment", "onCreateView");

        binding = FragmentDrawingBinding.inflate(inflater, container, false);

        PathDrawView pathView = binding.canvas;
        pathView.addSharedPreferences(sharedPreferences);

        fragmentManager = getParentFragmentManager();
        connectedFragment = (ConnectedFragment) fragmentManager.findFragmentByTag("connectedAcitivity");
        return binding.getRoot();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        Log.d("DrawingFragment", "onAttach");
        sharedPreferences = context.getSharedPreferences("PREFS_DATA", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();



//        PreferenceManager.setDefaultValues(this, R.xml.preferences, true);
//        String option = shPref.getString("PREF_LIST", "Medium");
//        if (option == "Low") {
//
//        }
    }


//    public void runBT() throws IOException {
//
//        //pairs bluetooth
//        adapter = BluetoothAdapter.getDefaultAdapter();
//
//        if (!adapter.isEnabled()) {
//            Intent enable = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//            if (ActivityCompat.checkSelfPermission(requireContext(), android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
//                ActivityCompat.requestPermissions(requireActivity(),
//                        new String[]{android.Manifest.permission.BLUETOOTH_CONNECT}, 1);
//                return;
//            }
//            startActivityForResult(enable, 0);
//        }
//
//        Set<BluetoothDevice> pairedDevices = adapter.getBondedDevices();
//        if(pairedDevices.size() > 0) {
//            String currentDevice = sharedPreferences.getString("current_device", "");
//            for(BluetoothDevice device : pairedDevices) {
//                if(device.getName().equals(currentDevice)) {
//                    MiDevice = device;
//                    Toast.makeText(requireContext(), "Device Paired",
//                            Toast.LENGTH_SHORT).show();
//                    break;
//                }
//            }
//        }
//
//
//        //opens connection
//        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //Standard SerialPortService ID
//        // UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); //Standard SerialPortService ID
//
//        if(MiDevice.createRfcommSocketToServiceRecord(uuid) == null){
//            Toast.makeText(requireContext(), "Device is not paired to the car.",
//                    Toast.LENGTH_SHORT).show();
//        }else{
//            socket = MiDevice.createRfcommSocketToServiceRecord(uuid);
//        }
//
//
//        socket.connect();
//        out = socket.getOutputStream();
//        in = socket.getInputStream();
//        Toast.makeText(getApplicationContext(), "Connection Established",
//                Toast.LENGTH_SHORT).show();
//
//
//        //gets data
//        final Handler handler = new Handler();
//        final byte delimiter = 10;
//
//        stop = false;
//        position = 0;
//        read = new byte[1024*4];
//        BlueToothThread = new Thread(new Runnable() {
//
//            public void run() {
//
//                while(!Thread.currentThread().isInterrupted() && !stop) {
//
//                    try {
//
//                        int bytesAvailable = in.available();
//                        if(bytesAvailable > 0) {
//                            byte[] packetBytes = new byte[bytesAvailable];
//                            in.read(packetBytes);
//                            for(int i=0;i<bytesAvailable;i++) {
//                                byte b = packetBytes[i];
//                                if(b == delimiter) {
//                                    byte[] encodedBytes = new byte[position];
//                                    System.arraycopy(read, 0, encodedBytes, 0, encodedBytes.length);
//                                    final String result = new String(encodedBytes, "US-ASCII");
//                                    position = 0;
//
//                                    handler.post(new Runnable() {
//                                        public void run() {
//                                            //                                          data.setText(result);
//                                            if(result.contains("Obstacle ")){
//                                                System.out.println("OBSTACLE DETETECTED!");
//                                                try {
//                                                    int i = Integer.parseInt(result.split("Obstacle ")[1].replaceAll("\\r|\\n", ""));
//                                                    pathView.onObstacleDetected(i);
//                                                }catch(Exception e){
//                                                    pathView.onObstacleDetected(0);
//                                                    Toast.makeText(getApplicationContext(), "Strange obstacle index",
//                                                            Toast.LENGTH_SHORT).show();
//                                                }
//                                            }else{
//                                                System.out.println("ELSE: "+result);
//                                            }
//                                        }
//                                    });
//
//                                } else {
//                                    read[position++] = b;
//                                }
//                            }
//                        }
//                    }
//                    catch (IOException ex) {
//                        stop = true;
//                    }
//                }
//            }
//        });
//
//        BlueToothThread.start();
//
//    }

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
}