package com.example.pookies;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
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
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 2;

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

        // Check Bluetooth permissions
        requestBluetoothPermissions();

        // Set up the button to start scanning
        scanButton.setOnClickListener(v -> {
            if (permissionsGranted()) {
                // If Bluetooth is enabled, start scanning immediately; otherwise, wait for state change
                if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
                    startScanning();
                } else {
                    // Request to turn on Bluetooth and wait for the state change broadcast
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                }
            } else {
                requestBluetoothPermissions();
            }
        });

        // Register broadcast receiver for device discovery and Bluetooth state change
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        getActivity().registerReceiver(receiver, filter);

        // Handle list item click
        listView.setOnItemClickListener((parent, view1, position, id) -> {
            BluetoothDevice device = discoveredDevices.get(position);
            pairAndConnect(device);
        });

        return view;
    }

    // Step 1: Request Bluetooth Permissions
    private void requestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // For Android 12 and above, check for Bluetooth and location permissions
            if (!permissionsGranted()) {
                ActivityCompat.requestPermissions(getActivity(), new String[]{
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.ACCESS_FINE_LOCATION
                }, REQUEST_BLUETOOTH_PERMISSIONS);
            }
        } else {
            // For Android versions below 12
            if (!legacyPermissionsGranted()) {
                ActivityCompat.requestPermissions(getActivity(), new String[]{
                        Manifest.permission.BLUETOOTH,
                        Manifest.permission.BLUETOOTH_ADMIN,
                        Manifest.permission.ACCESS_FINE_LOCATION
                }, REQUEST_BLUETOOTH_PERMISSIONS);
            }
        }
    }

    // Step 2: Check Permissions (Android 12+)
    private boolean permissionsGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
        return legacyPermissionsGranted(); // For older versions
    }

    // Step 2: Check Permissions (Legacy)
    private boolean legacyPermissionsGranted() {
        return ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    // Step 3: Handle Permission Result
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            // Check if all permissions were granted
            if (grantResults.length > 0 && allPermissionsGranted(grantResults)) {
                Toast.makeText(getContext(), "Bluetooth permissions granted", Toast.LENGTH_SHORT).show();
                // Proceed to scan for devices or other Bluetooth operations
                startScanning();
            } else {
                Toast.makeText(getContext(), "Bluetooth permissions are required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean allPermissionsGranted(int[] grantResults) {
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void startScanning() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Toast.makeText(getContext(), "Bluetooth is not enabled", Toast.LENGTH_SHORT).show();
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

    // Broadcast receiver for discovered devices and Bluetooth state change
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null && !discoveredDevices.contains(device)) {
                    discoveredDevices.add(device);
                    adapter.add((device.getName() != null ? device.getName() : "Unknown Device") + "\n" + device.getAddress());
                    adapter.notifyDataSetChanged();
                }
            } else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                if (state == BluetoothAdapter.STATE_ON) {
                    // Bluetooth is now ON, start discovery
                    startScanning();
                }
            }
        }
    };

    private void pairAndConnect(BluetoothDevice device) {
        if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
            device.createBond(); // Initiate pairing
            Toast.makeText(getContext(), "Pairing with " + device.getName(), Toast.LENGTH_SHORT).show();
        } else {
            connectToDevice(device);
        }
    }

    private void connectToDevice(BluetoothDevice device) {
        new Thread(() -> {
            BluetoothSocket socket = null;
            try {
                // Always cancel discovery before trying to connect
                bluetoothAdapter.cancelDiscovery();

                // Create the Bluetooth socket
                socket = device.createRfcommSocketToServiceRecord(MY_UUID);

                // Attempt to connect to the device
                socket.connect();

                // Connection successful, update the UI on the main thread
                getActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Connected to " + device.getName(), Toast.LENGTH_SHORT).show()
                );

                // You can now handle the data transfer or communication here...

                // Close the socket when done
                socket.close();
            } catch (IOException e) {
                // Handle connection failure

                // Ensure the socket is closed on failure
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException closeException) {
                        closeException.printStackTrace();
                    }
                }

                // Show connection failed message on the main thread
                getActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Connection failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );

                e.printStackTrace(); // Log the full stack trace for debugging
            }
        }).start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().unregisterReceiver(receiver);
    }
}
