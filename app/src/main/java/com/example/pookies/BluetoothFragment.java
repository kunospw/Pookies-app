package com.example.pookies;
import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

public class BluetoothFragment extends Fragment {

    private BluetoothAdapter bluetoothAdapter;
    private ArrayList<BluetoothDevice> discoveredDevices = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private ListView listView;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_PERMISSION = 2;

    // UUID for Serial Port Profile (SPP)
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bluetooth, container, false);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        listView = view.findViewById(R.id.device_list);
        Button scanButton = view.findViewById(R.id.scan_button);

        // Set up adapter for list view
        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, new ArrayList<>());
        listView.setAdapter(adapter);

        // Check Bluetooth permission
        checkBluetoothPermission();

        // Set up the button to start scanning
        scanButton.setOnClickListener(v -> startScanning());

        // Register broadcast receiver for device discovery
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        getActivity().registerReceiver(receiver, filter);

        // Handle list item click
        listView.setOnItemClickListener((parent, view1, position, id) -> {
            BluetoothDevice device = discoveredDevices.get(position);
            pairAndConnect(device);
        });

        return view;
    }

    private void checkBluetoothPermission() {
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, REQUEST_PERMISSION);
        }
    }

    @SuppressLint("MissingPermission")
    private void startScanning() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Toast.makeText(getContext(), "Bluetooth is not enabled", Toast.LENGTH_SHORT).show();
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            return;
        }

        adapter.clear();
        discoveredDevices.clear();

        // Start discovery
        if (bluetoothAdapter.startDiscovery()) {
            Toast.makeText(getContext(), "Scanning for devices...", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "Discovery failed to start", Toast.LENGTH_SHORT).show();
        }
    }

    // Broadcast receiver for discovered devices
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null && !discoveredDevices.contains(device)) {
                    discoveredDevices.add(device);
                    adapter.add(device.getName() + "\n" + device.getAddress());
                    adapter.notifyDataSetChanged();
                }
            }
        }
    };

    @SuppressLint("MissingPermission")
    private void pairAndConnect(BluetoothDevice device) {
        // Check if the device is already paired
        if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
            device.createBond(); // Initiate pairing
            Toast.makeText(getContext(), "Pairing with " + device.getName(), Toast.LENGTH_SHORT).show();
        } else {
            connectToDevice(device);
        }
    }

    @SuppressLint("MissingPermission")
    private void connectToDevice(BluetoothDevice device) {
        new Thread(() -> {
            BluetoothSocket socket = null;
            try {
                // Attempt to create a BluetoothSocket for the device
                socket = device.createRfcommSocketToServiceRecord(MY_UUID);
                socket.connect(); // Attempt to connect
                Toast.makeText(getContext(), "Connected to " + device.getName(), Toast.LENGTH_SHORT).show();

                // Handle your connection logic here (e.g., data transfer)

                // Remember to close the socket after use
                socket.close();

            } catch (IOException e) {
                // Handle the error appropriately
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException closeException) {
                        closeException.printStackTrace();
                    }
                }
                e.printStackTrace();
                Toast.makeText(getContext(), "Connection failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }).start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().unregisterReceiver(receiver);
    }
}