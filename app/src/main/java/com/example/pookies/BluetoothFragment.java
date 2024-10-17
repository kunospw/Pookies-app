package com.example.pookies;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class BluetoothFragment extends Fragment {
    private BluetoothAdapter bluetoothAdapter;
    private ArrayAdapter<String> devicesArrayAdapter;
    private final ArrayList<BluetoothDevice> discoveredDevices = new ArrayList<>();
    private final Map<String, String> deviceAddressToNameMap = new HashMap<>();

    private TextView statusText;
    private Button enableBtBtn;
    private Button discoverBtn;
    private ListView devicesList;
    private ProgressBar progressBar;

    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private static final int DISCOVERY_DURATION = 30000; // 30 seconds
    private boolean isScanning = false;

    private final ActivityResultLauncher<String[]> multiplePermissionsLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), permissions -> {
                boolean allGranted = true;
                for (Boolean isGranted : permissions.values()) {
                    allGranted = allGranted && isGranted;
                }
                if (allGranted) {
                    initializeBluetooth();
                } else {
                    Toast.makeText(getContext(), "Permissions are required for Bluetooth functionality", Toast.LENGTH_LONG).show();
                }
            });

    private final ActivityResultLauncher<Intent> enableBluetoothLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK) {
                    updateUIState(true);
                } else {
                    Toast.makeText(getContext(), "Bluetooth must be enabled to use this feature", Toast.LENGTH_SHORT).show();
                }
            });

    private final BroadcastReceiver discoveryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                updateBluetoothState(state);
            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                isScanning = true;
                Toast.makeText(context, "Discovery started...", Toast.LENGTH_SHORT).show();
                Log.d("BluetoothFragment", "Discovery started");
            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null && ActivityCompat.checkSelfPermission(requireContext(),
                        Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {

                    // Get the device's RSSI (signal strength)
                    int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);

                    String deviceName = device.getName();
                    String deviceAddress = device.getAddress();
                    String displayName = (deviceName != null && !deviceName.isEmpty()) ?
                            deviceName : deviceAddress;

                    // Add RSSI information to the display name
                    displayName += " (Signal: " + rssi + " dBm)";

                    if (!discoveredDevices.contains(device)) {
                        discoveredDevices.add(device);
                        deviceAddressToNameMap.put(deviceAddress, displayName);
                        devicesArrayAdapter.add(displayName);
                        devicesArrayAdapter.notifyDataSetChanged();

                        // Log device found
                        Log.d("BluetoothFragment", "Found device: " + displayName);
                    }
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                isScanning = false;
                progressBar.setVisibility(View.GONE);
                discoverBtn.setEnabled(true);
                Log.d("BluetoothFragment", "Discovery finished. Devices found: " + discoveredDevices.size());

                if (discoveredDevices.isEmpty()) {
                    // Try scanning for already paired devices
                    scanPairedDevices();
                }
            }
        }
    };

    private void updateBluetoothState(int state) {
        switch (state) {
            case BluetoothAdapter.STATE_OFF:
                Toast.makeText(getContext(), "Bluetooth is turned off", Toast.LENGTH_SHORT).show();
                updateUIState(false);
                break;
            case BluetoothAdapter.STATE_TURNING_OFF:
                Toast.makeText(getContext(), "Bluetooth is turning off...", Toast.LENGTH_SHORT).show();
                break;
            case BluetoothAdapter.STATE_ON:
                Toast.makeText(getContext(), "Bluetooth is turned on", Toast.LENGTH_SHORT).show();
                updateUIState(true);
                break;
            case BluetoothAdapter.STATE_TURNING_ON:
                Toast.makeText(getContext(), "Bluetooth is turning on...", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void scanPairedDevices() {
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                String deviceAddress = device.getAddress();
                String displayName = (deviceName != null && !deviceName.isEmpty()) ?
                        deviceName : deviceAddress;
                displayName += " (Paired)";

                if (!discoveredDevices.contains(device)) {
                    discoveredDevices.add(device);
                    deviceAddressToNameMap.put(deviceAddress, displayName);
                    devicesArrayAdapter.add(displayName);
                }
            }
            devicesArrayAdapter.notifyDataSetChanged();
        } else {
            Toast.makeText(getContext(),
                    "No devices found. Please ensure other devices have Bluetooth enabled and are discoverable.",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bluetooth, container, false);
        initializeViews(view);
        checkPermissions();
        return view;
    }

    private void initializeViews(View view) {
        statusText = view.findViewById(R.id.status_text);
        enableBtBtn = view.findViewById(R.id.enable_bt_btn);
        discoverBtn = view.findViewById(R.id.discover_btn);
        devicesList = view.findViewById(R.id.devices_list);
        progressBar = view.findViewById(R.id.progress_bar);

        devicesArrayAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1);
        devicesList.setAdapter(devicesArrayAdapter);

        enableBtBtn.setOnClickListener(v -> enableBluetooth());
        discoverBtn.setOnClickListener(v -> startDiscovery());
        devicesList.setOnItemClickListener((parent, v, position, id) -> {
            BluetoothDevice device = discoveredDevices.get(position);
            if (device != null) {
                duplicateChatHistory(device);
            }
        });
        devicesArrayAdapter.add("Test Device");
        devicesArrayAdapter.notifyDataSetChanged();
    }

    private void checkPermissions() {
        String[] permissions = {
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        };

        ArrayList<String> permissionsNeeded = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(requireContext(), permission)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(permission);
            }
        }

        if (!permissionsNeeded.isEmpty()) {
            multiplePermissionsLauncher.launch(permissionsNeeded.toArray(new String[0]));
        } else {
            initializeBluetooth();
        }
    }

    private void initializeBluetooth() {
        BluetoothManager bluetoothManager = (BluetoothManager) requireContext()
                .getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        if (bluetoothAdapter == null) {
            Toast.makeText(getContext(), "Bluetooth is not supported on this device", Toast.LENGTH_LONG).show();
            return;
        }

        mDatabase = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        requireContext().registerReceiver(discoveryReceiver, filter);

        updateUIState(bluetoothAdapter.isEnabled());
    }

    private void updateUIState(boolean bluetoothEnabled) {
        enableBtBtn.setEnabled(!bluetoothEnabled);
        discoverBtn.setEnabled(bluetoothEnabled);
        statusText.setText("Bluetooth Status: " + (bluetoothEnabled ? "Ready" : "Not Ready"));
    }

    private void enableBluetooth() {
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableBluetoothLauncher.launch(enableBtIntent);
        }
    }

    private void startDiscovery() {
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getContext(), "Bluetooth scan permission not granted", Toast.LENGTH_SHORT).show();
            Log.e("BluetoothFragment", "Bluetooth scan permission not granted");
            return;
        }
        try {
            // Clear previous results
            devicesArrayAdapter.clear();
            discoveredDevices.clear();
            deviceAddressToNameMap.clear();

            // First, scan for paired devices
            scanPairedDevices();

            // Cancel ongoing discovery if any
            if (bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
            }

            // Disable the discover button while scanning
            discoverBtn.setEnabled(false);
            progressBar.setVisibility(View.VISIBLE);

            // Make device discoverable
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);

            // Start new discovery
            boolean started = bluetoothAdapter.startDiscovery();
            if (!started) {
                progressBar.setVisibility(View.GONE);
                discoverBtn.setEnabled(true);
                Toast.makeText(getContext(), "Failed to start device discovery", Toast.LENGTH_SHORT).show();
                Log.e("BluetoothFragment", "Failed to start device discovery");
            } else {
                Log.d("BluetoothFragment", "Device discovery started");
                // Set a timeout for discovery
                new Handler().postDelayed(() -> {
                    if (isScanning) {
                        if (ActivityCompat.checkSelfPermission(requireContext(),
                                Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                            bluetoothAdapter.cancelDiscovery();
                            Log.d("BluetoothFragment", "Discovery cancelled after timeout");
                        }
                    }
                }, DISCOVERY_DURATION);
            }
        } catch (SecurityException e) {
            progressBar.setVisibility(View.GONE);
            discoverBtn.setEnabled(true);
            Toast.makeText(getContext(), "Security exception: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e("BluetoothFragment", "Security exception: " + e.getMessage(), e);
        }
    }

    private void duplicateChatHistory(BluetoothDevice device) {
        if (device == null) {
            Log.e("BluetoothFragment", "Attempted to duplicate chat history with null device");
            Toast.makeText(getContext(), "Error: Invalid device selected", Toast.LENGTH_SHORT).show();
            return;
        }
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getContext(), "Bluetooth connect permission not granted", Toast.LENGTH_SHORT).show();
            Log.e("BluetoothFragment", "Bluetooth connect permission not granted");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        String currentUserId = mAuth.getCurrentUser().getUid();
        Log.d("BluetoothFragment", "Attempting to duplicate chat history for device: " + device.getName());

        // First, get the current user's chat history
        mDatabase.child("users").child(currentUserId).child("messages")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (!dataSnapshot.exists()) {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(getContext(), "No chat history to duplicate", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // For demonstration, we're using the device address as a simple way to get target user
                        String targetUserId = device.getAddress().replace(":", "");

                        // Get the target user's existing messages first
                        mDatabase.child("users").child(targetUserId).child("messages")
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot targetSnapshot) {
                                        Map<String, Object> updates = new HashMap<>();

                                        // Add existing messages if any
                                        if (targetSnapshot.exists()) {
                                            for (DataSnapshot messageSnapshot : targetSnapshot.getChildren()) {
                                                updates.put(messageSnapshot.getKey(), messageSnapshot.getValue());
                                            }
                                        }

                                        // Add new messages
                                        for (DataSnapshot messageSnapshot : dataSnapshot.getChildren()) {
                                            String newKey = mDatabase.child("users").child(targetUserId)
                                                    .child("messages").push().getKey();
                                            updates.put(newKey, messageSnapshot.getValue());
                                        }

                                        // Update the target user's messages
                                        mDatabase.child("users").child(targetUserId).child("messages")
                                                .updateChildren(updates)
                                                .addOnSuccessListener(aVoid -> {
                                                    progressBar.setVisibility(View.GONE);
                                                    Toast.makeText(getContext(),
                                                            "Chat history duplicated successfully",
                                                            Toast.LENGTH_SHORT).show();
                                                })
                                                .addOnFailureListener(e -> {
                                                    progressBar.setVisibility(View.GONE);
                                                    Toast.makeText(getContext(),
                                                            "Failed to duplicate chat history: " + e.getMessage(),
                                                            Toast.LENGTH_SHORT).show();
                                                });
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        progressBar.setVisibility(View.GONE);
                                        Toast.makeText(getContext(),
                                                "Failed to access target user data: " + error.getMessage(),
                                                Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(getContext(),
                                "Failed to access chat history: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            if (ActivityCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                if (bluetoothAdapter != null && bluetoothAdapter.isDiscovering()) {
                    bluetoothAdapter.cancelDiscovery();
                }
            }
            requireContext().unregisterReceiver(discoveryReceiver);
        } catch (SecurityException | IllegalArgumentException e) {
            // Handle exceptions
        }
    }
}