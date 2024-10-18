package com.example.pookies;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Button;
import android.location.Location;
import android.widget.Toast;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import androidx.core.app.ActivityCompat;

public class GPS  extends AppCompatActivity {

    private TextView latitude, longitude;
    private Button location;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_REQUEST_CODE = 100;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        latitude = findViewById(R.id.Latitude);
        longitude = findViewById(R.id.Longitude);
        location = findViewById(R.id.location);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        location.setOnClickListener(v -> getLocation());
    }

    private void getLocation() {
        // Check if permission is granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            // Request permissions if not granted
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
            return;
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            latitude.setText("Latitude: " + location.getLatitude());
                            longitude.setText("Longitude: " + location.getLongitude());
                        } else {
                            Toast.makeText(GPS.this, "Unable to find location", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocation();
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}

