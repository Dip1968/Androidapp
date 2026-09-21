package com.dip1968.androidapp;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Collections;
import java.util.UUID;

public class MainActivity extends Activity {

    private static final int PERMISSION_REQUEST = 100;

    private static final UUID MILKMAN_UUID =
            UUID.fromString("7d2a0001-6a4f-4c2d-9e01-1234567890ab");

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeAdvertiser advertiser;
    private BluetoothLeScanner scanner;

    private AdvertiseCallback advertiseCallback;
    private ScanCallback scanCallback;

    private TextView roleText;
    private TextView statusText;
    private Button startButton;
    private Button stopButton;

    private boolean roleSelected = false;
    private boolean milkman = false;
    private boolean advertising = false;
    private boolean scanning = false;

    private long lastAlert = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        BluetoothManager manager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

        if (manager != null) {
            bluetoothAdapter = manager.getAdapter();
        }

        createUI();
    }

    private void createUI() {

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(30, 40, 30, 30);

        TextView title = new TextView(this);
        title.setText("🥛 દૂધવાળો Alert");
        title.setTextSize(28);
        title.setGravity(Gravity.CENTER);
        root.addView(title, params());

        roleText = new TextView(this);
        roleText.setText("તમારો Role પસંદ કરો");
        roleText.setTextSize(20);
        roleText.setGravity(Gravity.CENTER);
        roleText.setPadding(0, 25, 0, 15);
        root.addView(roleText, params());

        Button milkmanButton = new Button(this);
        milkmanButton.setText("🥛 હું દૂધવાળો છું");
        root.addView(milkmanButton, params());

        Button homeButton = new Button(this);
        homeButton.setText("🏠 હું ઘર છું");
        root.addView(homeButton, params());

        startButton = new Button(this);
        startButton.setText("▶ START");
        startButton.setEnabled(false);
        root.addView(startButton, params());

        stopButton = new Button(this);
        stopButton.setText("⏹ STOP");
        stopButton.setEnabled(false);
        root.addView(stopButton, params());

        statusText = new TextView(this);
        statusText.setText("Status: તૈયાર");
        statusText.setTextSize(18);
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(0, 25, 0, 0);
        root.addView(statusText, params());

        setContentView(root);

        milkmanButton.setOnClickListener(v -> {

            stopServices();

            milkman = true;
            roleSelected = true;

            roleText.setText("Role: 🥛 દૂધવાળો");
            statusText.setText("START દબાવો");

            startButton.setEnabled(true);
            stopButton.setEnabled(false);
        });

        homeButton.setOnClickListener(v -> {

            stopServices();

            milkman = false;
            roleSelected = true;

            roleText.setText("Role: 🏠 ઘર");
            statusText.setText("START દબાવો");

            startButton.setEnabled(true);
            stopButton.setEnabled(false);
        });

        startButton.setOnClickListener(v -> {

            if (!roleSelected) {
                return;
            }

            if (!hasPermissions()) {
                requestBlePermissions();
                return;
            }

            startRole();
        });

        stopButton.setOnClickListener(v -> {

            stopServices();

            statusText.setText(
                    "⏹ Service બંધ છે\n\nSTART દબાવીને ફરી શરૂ કરો."
            );

            startButton.setEnabled(roleSelected);
            stopButton.setEnabled(false);
        });
    }

    private LinearLayout.LayoutParams params() {

        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
    }

    private void startRole() {

        if (bluetoothAdapter == null) {

            statusText.setText("❌ Bluetooth ઉપલબ્ધ નથી");
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {

            statusText.setText(
                    "⚠ Bluetooth OFF છે.\nBluetooth ON કરો."
            );

            try {
                startActivity(
                        new Intent(
                                BluetoothAdapter.ACTION_REQUEST_ENABLE
                        )
                );
            } catch (Exception ignored) {
            }

            return;
        }

        if (milkman) {
            startAdvertising();
        } else {
            startScanning();
        }
    }

    private void startAdvertising() {

        if (advertising) {
            return;
        }

        advertiser =
                bluetoothAdapter.getBluetoothLeAdvertiser();

        if (advertiser == null) {

            statusText.setText(
                    "❌ BLE advertising support નથી"
            );
            return;
        }

        AdvertiseSettings settings =
                new AdvertiseSettings.Builder()
                        .setAdvertiseMode(
                                AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY
                        )
                        .setTxPowerLevel(
                                AdvertiseSettings.ADVERTISE_TX_POWER_HIGH
                        )
                        .setConnectable(false)
                        .setTimeout(0)
                        .build();

        AdvertiseData data =
                new AdvertiseData.Builder()
                        .setIncludeDeviceName(false)
                        .addServiceUuid(
                                new ParcelUuid(MILKMAN_UUID)
                        )
                        .build();

        advertiseCallback = new AdvertiseCallback() {

            @Override
            public void onStartSuccess(
                    AdvertiseSettings settingsInEffect) {

                advertising = true;

                runOnUiThread(() -> {

                    statusText.setText(
                            "🟢 દૂધવાળો signal ચાલુ છે\n\n" +
                            "ઘરનો ફોન તમને શોધી શકે છે."
                    );

                    startButton.setEnabled(false);
                    stopButton.setEnabled(true);
                });
            }

            @Override
            public void onStartFailure(int errorCode) {

                advertising = false;

                runOnUiThread(() -> {

                    statusText.setText(
                            "❌ Advertising failed\nError: "
                                    + errorCode
                    );

                    startButton.setEnabled(true);
                    stopButton.setEnabled(false);
                });
            }
        };

        try {

            advertiser.startAdvertising(
                    settings,
                    data,
                    advertiseCallback
            );

            statusText.setText(
                    "⏳ Signal શરૂ થઈ રહ્યો છે..."
            );

        } catch (SecurityException e) {

            requestBlePermissions();

        } catch (Exception e) {

            statusText.setText(
                    "❌ Advertising error"
            );
        }
    }

    private void startScanning() {

        if (scanning) {
            return;
        }

        scanner =
                bluetoothAdapter.getBluetoothLeScanner();

        if (scanner == null) {

            statusText.setText(
                    "❌ BLE scanner ઉપલબ્ધ નથી"
            );
            return;
        }

        ScanFilter filter =
                new ScanFilter.Builder()
                        .setServiceUuid(
                                new ParcelUuid(MILKMAN_UUID)
                        )
                        .build();

        ScanSettings settings =
                new ScanSettings.Builder()
                        .setScanMode(
                                ScanSettings.SCAN_MODE_LOW_LATENCY
                        )
                        .build();

        scanCallback = new ScanCallback() {

            @Override
            public void onScanResult(
                    int callbackType,
                    ScanResult result) {

                runOnUiThread(() -> milkmanFound());
            }

            @Override
            public void onScanFailed(int errorCode) {

                scanning = false;

                runOnUiThread(() -> {

                    statusText.setText(
                            "❌ Scan failed\nError: "
                                    + errorCode
                    );

                    startButton.setEnabled(true);
                    stopButton.setEnabled(false);
                });
            }
        };

        try {

            scanner.startScan(
                    Collections.singletonList(filter),
                    settings,
                    scanCallback
            );

            scanning = true;

            statusText.setText(
                    "🔍 દૂધવાળાને શોધી રહ્યા છીએ...\n\n" +
                    "દૂધવાળો નજીક આવે ત્યારે alert મળશે."
            );

            startButton.setEnabled(false);
            stopButton.setEnabled(true);

        } catch (SecurityException e) {

            requestBlePermissions();

        } catch (Exception e) {

            statusText.setText(
                    "❌ Scanning error"
            );
        }
    }

    private void milkmanFound() {

        long now = System.currentTimeMillis();

        if (now - lastAlert < 5000) {
            return;
        }

        lastAlert = now;

        statusText.setText(
                "🟢 દૂધવાળો નજીક છે!\n\n" +
                "🔊 દૂધવાળો આવ્યો છે"
        );

        Toast.makeText(
                this,
                "🥛 દૂધવાળો આવી ગયો!",
                Toast.LENGTH_LONG
        ).show();

        alert();
    }

    private void alert() {

        try {

            android.media.ToneGenerator tone =
                    new android.media.ToneGenerator(
                            android.media.AudioManager.STREAM_NOTIFICATION,
                            100
                    );

            tone.startTone(
                    android.media.ToneGenerator.TONE_PROP_BEEP,
                    500
            );

            tone.release();

        } catch (Exception ignored) {
        }

        try {

            Vibrator vibrator =
                    (Vibrator) getSystemService(
                            Context.VIBRATOR_SERVICE
                    );

            if (vibrator != null &&
                    vibrator.hasVibrator()) {

                if (Build.VERSION.SDK_INT >=
                        Build.VERSION_CODES.O) {

                    vibrator.vibrate(
                            VibrationEffect.createOneShot(
                                    500,
                                    VibrationEffect.DEFAULT_AMPLITUDE
                            )
                    );

                } else {

                    vibrator.vibrate(500);
                }
            }

        } catch (Exception ignored) {
        }
    }

    private boolean hasPermissions() {

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.S) {

            return checkSelfPermission(
                    Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
                    &&
                    checkSelfPermission(
                            Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED
                    &&
                    checkSelfPermission(
                            Manifest.permission.BLUETOOTH_ADVERTISE
                    ) == PackageManager.PERMISSION_GRANTED;

        } else {

            return checkSelfPermission(
                    Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestBlePermissions() {

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.S) {

            requestPermissions(
                    new String[]{
                            Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.BLUETOOTH_ADVERTISE
                    },
                    PERMISSION_REQUEST
            );

        } else {

            requestPermissions(
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION
                    },
                    PERMISSION_REQUEST
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode != PERMISSION_REQUEST) {
            return;
        }

        boolean granted = true;

        for (int result : grantResults) {

            if (result != PackageManager.PERMISSION_GRANTED) {
                granted = false;
                break;
            }
        }

        if (granted) {

            Toast.makeText(
                    this,
                    "Bluetooth permission મળી ગઈ",
                    Toast.LENGTH_SHORT
            ).show();

            if (roleSelected) {
                startRole();
            }

        } else {

            statusText.setText(
                    "❌ Bluetooth permission જરૂરી છે"
            );
        }
    }

    private void stopServices() {

        stopAdvertising();
        stopScanning();
    }

    private void stopAdvertising() {

        if (advertiser != null &&
                advertiseCallback != null) {

            try {

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                        checkSelfPermission(
                                Manifest.permission.BLUETOOTH_ADVERTISE
                        ) == PackageManager.PERMISSION_GRANTED) {

                    advertiser.stopAdvertising(
                            advertiseCallback
                    );
                }

            } catch (Exception ignored) {
            }
        }

        advertising = false;
        advertiseCallback = null;
    }

    private void stopScanning() {

        if (scanner != null &&
                scanCallback != null) {

            try {

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                        checkSelfPermission(
                                Manifest.permission.BLUETOOTH_SCAN
                        ) == PackageManager.PERMISSION_GRANTED) {

                    scanner.stopScan(scanCallback);
                }

            } catch (Exception ignored) {
            }
        }

        scanning = false;
        scanCallback = null;
    }

    @Override
    protected void onDestroy() {

        stopServices();

        super.onDestroy();
    }
}
