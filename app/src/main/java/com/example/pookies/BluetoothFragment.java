package com.example.pookies;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.Manifest;
import android.bluetooth.BluetoothDevice;
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
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import java.util.ArrayList;
import java.util.Set;

public class BluetoothFragment extends Fragment {

    private static final int REQUEST_PERMISSION = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private BluetoothAdapter bluetoothAdapter;
    private ArrayAdapter<String> deviceArrayAdapter;
    private TextView statusTextView;
    private ArrayList<BluetoothDevice> pairedDevicesList;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bluetooth, container, false);

        statusTextView = view.findViewById(R.id.statusTextView);
        Button pairButton = view.findViewById(R.id.pairButton);
        Button connectButton = view.findViewById(R.id.connectButton);
        ListView deviceListView = view.findViewById(R.id.deviceListView);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        pairedDevicesList = new ArrayList<>();
        deviceArrayAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, new ArrayList<>());
        deviceListView.setAdapter(deviceArrayAdapter);

        // Check if Bluetooth is supported
        if (bluetoothAdapter == null) {
            statusTextView.setText("Bluetooth is not supported on this device.");
            return view;
        }

        // Register for broadcasts on BluetoothDevice action
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        getContext().registerReceiver(broadcastReceiver, filter);

        pairButton.setOnClickListener(v -> pairDevices());
        connectButton.setOnClickListener(v -> connectToDevice());

        return view;
    }

    @SuppressLint("MissingPermission")
    private void pairDevices() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            statusTextView.setText("Bluetooth is not enabled.");
            return;
        }

        try {
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            deviceArrayAdapter.clear();
            pairedDevicesList.clear();

            if (pairedDevices.size() > 0) {
                for (BluetoothDevice device : pairedDevices) {
                    deviceArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                    pairedDevicesList.add(device);
                }
                statusTextView.setText("Paired devices loaded.");
            } else {
                statusTextView.setText("No paired devices found.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            statusTextView.setText("Error pairing devices: " + e.getMessage());
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableBluetooth(); // Call method to enable Bluetooth or proceed with pairing
            } else {
                statusTextView.setText("Permission denied. Unable to pair devices.");
            }
        }
    }



    @SuppressLint("MissingPermission")
    private void connectToDevice() {
        // Logic to connect to the selected device
        // Example: Connect to the first paired device
        if (!pairedDevicesList.isEmpty()) {
            BluetoothDevice device = pairedDevicesList.get(0); // Change this to get selected device from ListView
            statusTextView.setText("Connecting to " + device.getName() + "...");
            // Add your connection logic here
        } else {
            statusTextView.setText("No devices to connect.");
        }
    }

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);

                if (state == BluetoothDevice.BOND_BONDED) {
                    statusTextView.setText("Paired with " + device.getName());
                } else if (state == BluetoothDevice.BOND_NONE) {
                    statusTextView.setText("Pairing failed or unpaired.");
                }
            }
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        getContext().unregisterReceiver(broadcastReceiver);
    }

    private void checkBluetoothPermissions() {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            // Request both permissions
            ActivityCompat.requestPermissions(getActivity(), new String[]{
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, REQUEST_PERMISSION);
        } else {
            enableBluetooth();
        }
    }

    private void enableBluetooth() {
        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            pairDevices(); // Call your pairing method here
        }
    }


}
