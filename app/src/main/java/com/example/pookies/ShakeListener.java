package com.example.pookies;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class ShakeListener implements SensorEventListener {
    private static final float SHAKE_THRESHOLD = 15.0f; // Adjust this threshold as needed
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private boolean isShaking = false;
    private ShakeListenerCallback callback;

    public interface ShakeListenerCallback {
        void onShake();
    }

    public ShakeListener(Context context, ShakeListenerCallback callback) {
        this.callback = callback;
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    public void start() {
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void stop() {
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            // Calculate the shake magnitude
            float magnitude = (float) Math.sqrt(x * x + y * y + z * z);
            if (magnitude > SHAKE_THRESHOLD && !isShaking) {
                isShaking = true;
                callback.onShake(); // Notify that a shake was detected
            } else if (magnitude < SHAKE_THRESHOLD) {
                isShaking = false;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not used
    }
}
