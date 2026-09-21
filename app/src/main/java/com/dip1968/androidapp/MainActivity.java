package com.dip1968.androidapp;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothLeAdvertiser;
import android.bluetooth.BluetoothLeScanner;
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
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends Activity {

    // Same UUID must be used by both phones.
    private static final UUID MILKMAN_SERVICE_UUID =
            UUID.fromString("7d2a0001-6a4f-4c2d-9e01-1234567890ab");

    private static final ParcelUuid MILKMAN_PARCEL_UUID =
            new ParcelUuid(MILKMAN_SERVICE_UUID);

    private static final int REQUEST_BLE_PERMISSIONS = 1001;

    private enum Role {
        NONE,
        MILKMAN,
        HOME
    }

    private Role currentRole = Role.NONE;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeAdvertiser bluetoothLeAdvertiser;
    private BluetoothLeScanner bluetoothLeScanner;

    private AdvertiseCallback advertiseCallback;
    private ScanCallback scanCallback;

    private boolean isAdvertising = false;
    private boolean isScanning = false;

    private TextView titleText;
    private TextView statusText;
    private TextView roleText;

    private Button milkmanButton;
    private Button homeButton;
    private Button startButton;
    private Button stopButton;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private long lastAlertTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupBluetooth();
        createUserInterface();
    }

    private void setupBluetooth() {

        if (!getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_BLUETOOTH_LE)) {

            bluetoothAdapter = null;
            return;
        }

        android.bluetooth.BluetoothManager bluetoothManager =
                (android.bluetooth.BluetoothManager)
                        getSystemService(Context.BLUETOOTH_SERVICE);

        if (bluetoothManager != null) {
            bluetoothAdapter = bluetoothManager.getAdapter();
        }
    }

    private void createUserInterface() {

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(40, 50, 40, 40);
        root.setBackgroundColor(Color.WHITE);

        titleText = new TextView(this);
        titleText.setText("🥛 દૂધવાળો Alert");
        titleText.setTextSize(28);
        titleText.setTextColor(Color.BLACK);
        titleText.setGravity(Gravity.CENTER);
        titleText.setPadding(0, 0, 0, 35);

        root.addView(titleText,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                ));

        roleText = new TextView(this);
        roleText.setText("પહેલા તમારો Role પસંદ કરો");
        roleText.setTextSize(20);
        roleText.setTextColor(Color.DKGRAY);
        roleText.setGravity(Gravity.CENTER);
        roleText.setPadding(0, 0, 0, 25);

        root.addView(roleText,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                ));

        milkmanButton = new Button(this);
        milkmanButton.setText("🥛 હું દૂધવાળો છું");
        milkmanButton.setTextSize(18);

        root.addView(milkmanButton,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                ));

        homeButton = new Button(this);
        homeButton.setText("🏠 હું ઘર છું");
        homeButton.setTextSize(18);

        LinearLayout.LayoutParams homeParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        homeParams.topMargin = 15;

        root.addView(homeButton, homeParams);

        startButton = new Button(this);
        startButton.setText("▶️ START");
        startButton.setTextSize(18);
        startButton.setEnabled(false);

        LinearLayout.LayoutParams startParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        startParams.topMargin = 30;

        root.addView(startButton, startParams);

        stopButton = new Button(this);
        stopButton.setText("⏹️ STOP");
        stopButton.setTextSize(18);
        stopButton.setEnabled(false);

        LinearLayout.LayoutParams stopParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        stopParams.topMargin = 10;

        root.addView(stopButton, stopParams);

        statusText = new TextView(this);
        statusText.setText("Status: તૈયાર");
        statusText.setTextSize(20);
        statusText.setTextColor(Color.DKGRAY);
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(0, 40, 0, 0);

        root.addView(statusText,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                ));

        setContentView(root);

        milkmanButton.setOnClickListener(v -> {

            currentRole = Role.MILKMAN;

            roleText.setText("તમારો Role: 🥛 દૂધવાળો");
            statusText.setText("Status: START દબાવો");
            startButton.setEnabled(true);

            Toast.makeText(
                    MainActivity.this,
                    "દૂધવાળો Role પસંદ થયો",
                    Toast.LENGTH_SHORT
            ).show();
        });

        homeButton.setOnClickListener(v -> {

            currentRole = Role.HOME;

            roleText.setText("તમારો Role: 🏠 ઘર");
            statusText.setText("Status: START દબાવો");
            startButton.setEnabled(true);

            Toast.makeText(
                    MainActivity.this,
                    "ઘર Role પસંદ થયો",
                    Toast.LENGTH_SHORT
            ).show();
        });

        startButton.setOnClickListener(v -> {

            if (currentRole == Role.NONE) {
                Toast.makeText(
                        MainActivity.this,
                        "પહેલા Role પસંદ કરો",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            if (!hasRequiredPermissions()) {
                requestRequiredPermissions();
                return;
            }

            startSelectedRole();
        });

        stopButton.setOnClickListener(v -> stopEverything());
    }

    private void startSelectedRole() {

        if (bluetoothAdapter == null) {

            statusText.setText(
                    "❌ આ ફોનમાં Bluetooth ઉપલબ્ધ નથી"
            );

            return;
        }

        if (!bluetoothAdapter.isEnabled()) {

            statusText.setText(
                    "⚠️ Bluetooth OFF છે. Bluetooth ON કરો."
            );

            try {
                Intent enableBluetoothIntent =
                        new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);

                startActivity(enableBluetoothIntent);

            } catch (Exception e) {

                Toast.makeText(
                        this,
                        "Bluetooth ON કરો",
                        Toast.LENGTH_SHORT
                ).show();
            }

            return;
        }

        if (!getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_BLUETOOTH_LE)) {

            statusText.setText(
                    "❌ આ ફોન BLE support કરતો નથી"
            );

            return;
        }

        if (currentRole == Role.MILKMAN) {

            startAdvertising();

        } else if (currentRole == Role.HOME) {

            startScanning();
        }
    }

    // ============================================================
    // MILKMAN - BLE ADVERTISING
    // ============================================================

    private void startAdvertising() {

        if (isAdvertising) {
            statusText.setText(
                    "🟢 દૂધવાળો signal ચાલુ છે"
            );
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

            if (checkSelfPermission(
                    Manifest.permission.BLUETOOTH_ADVERTISE)
                    != PackageManager.PERMISSION_GRANTED) {

                requestRequiredPermissions();
                return;
            }
        }

        bluetoothLeAdvertiser =
                bluetoothAdapter.getBluetoothLeAdvertiser();

        if (bluetoothLeAdvertiser == null) {

            statusText.setText(
                    "❌ આ ફોન BLE advertising support કરતો નથી"
            );

            Toast.makeText(
                    this,
                    "આ ફોન BLE advertising support કરતો નથી",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        AdvertiseSettings settings =
                new AdvertiseSettings.Builder()
                        .setAdvertiseMode(
                                AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                        .setTxPowerLevel(
                                AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                        .setConnectable(false)
                        .setTimeout(0)
                        .build();

        AdvertiseData advertiseData =
                new AdvertiseData.Builder()
                        .setIncludeDeviceName(false)
                        .addServiceUuid(MILKMAN_PARCEL_UUID)
                        .build();

        advertiseCallback = new AdvertiseCallback() {

            @Override
            public void onStartSuccess(
                    AdvertiseSettings settingsInEffect) {

                runOnUiThread(() -> {

                    isAdvertising = true;

                    statusText.setText(
                            "🟢 દૂધવાળો signal ચાલુ છે\n\n" +
                            "હવે ઘરનો ફોન તમને શોધી શકે છે."
                    );

                    startButton.setEnabled(false);
                    stopButton.setEnabled(true);

                    Toast.makeText(
                            MainActivity.this,
                            "🥛 દૂધવાળો signal ચાલુ!",
                            Toast.LENGTH_SHORT
                    ).show();
                });
            }

            @Override
            public void onStartFailure(int errorCode) {

                runOnUiThread(() -> {

                    isAdvertising = false;

                    statusText.setText(
                            "❌ BLE Advertising શરૂ થયું નથી\n" +
                            "Error code: " + errorCode
                    );

                    stopButton.setEnabled(false);
                    startButton.setEnabled(true);
                });
            }
        };

        try {

            bluetoothLeAdvertiser.startAdvertising(
                    settings,
                    advertiseData,
                    advertiseCallback
            );

            statusText.setText(
                    "⏳ દૂધવાળો signal શરૂ થઈ રહ્યો છે..."
            );

        } catch (SecurityException e) {

            statusText.setText(
                    "❌ Bluetooth permission જરૂરી છે"
            );

            requestRequiredPermissions();

        } catch (Exception e) {

            statusText.setText(
                    "❌ Advertising error: " + e.getMessage()
            );
        }
    }

    // ============================================================
    // HOME - BLE SCANNING
    // ============================================================

    private void startScanning() {

        if (isScanning) {

            statusText.setText(
                    "🔍 દૂધવાળાને શોધી રહ્યા છીએ..."
            );

            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

            if (checkSelfPermission(
                    Manifest.permission.BLUETOOTH_SCAN)
                    != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(
                            Manifest.permission.BLUETOOTH_CONNECT)
                            != PackageManager.PERMISSION_GRANTED) {

                requestRequiredPermissions();
                return;
            }

        } else {

            if (checkSelfPermission(
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {

                requestRequiredPermissions();
                return;
            }
        }

        bluetoothLeScanner =
                bluetoothAdapter.getBluetoothLeScanner();

        if (bluetoothLeScanner == null) {

            statusText.setText(
                    "❌ BLE scanner ઉપલબ્ધ નથી"
            );

            return;
        }

        ScanFilter filter =
                new ScanFilter.Builder()
                        .setServiceUuid(MILKMAN_PARCEL_UUID)
                        .build();

        List<ScanFilter> filters =
                new ArrayList<>();

        filters.add(filter);

        ScanSettings scanSettings =
                new ScanSettings.Builder()
                        .setScanMode(
                                ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .build();

        scanCallback = new ScanCallback() {

            @Override
            public void onScanResult(
                    int callbackType,
                    ScanResult result) {

                runOnUiThread(() ->
                        milkmanDetected(result)
                );
            }

            @Override
            public void onBatchScanResults(
                    List<ScanResult> results) {

                if (results == null) {
                    return;
                }

                for (ScanResult result : results) {

                    if (result != null) {

                        runOnUiThread(() ->
                                milkmanDetected(result)
                        );

                        break;
                    }
                }
            }

            @Override
            public void onScanFailed(int errorCode) {

                runOnUiThread(() -> {

                    isScanning = false;

                    statusText.setText(
                            "❌ BLE Scan failed\n" +
                            "Error code: " + errorCode
                    );

                    startButton.setEnabled(true);
                    stopButton.setEnabled(false);
                });
            }
        };

        try {

            bluetoothLeScanner.startScan(
                    filters,
                    scanSettings,
                    scanCallback
            );

            isScanning = true;

            statusText.setText(
                    "🔍 દૂધવાળાને શોધી રહ્યા છીએ...\n\n" +
                    "દૂધવાળો ફોન નજીક આવે ત્યારે alert મળશે."
            );

            startButton.setEnabled(false);
            stopButton.setEnabled(true);

        } catch (SecurityException e) {

            statusText.setText(
                    "❌ Bluetooth permission જરૂરી છે"
            );

            requestRequiredPermissions();

        } catch (Exception e) {

            statusText.setText(
                    "❌ Scanning error: " + e.getMessage()
            );
        }
    }

    private void milkmanDetected(ScanResult result) {

        long now = System.currentTimeMillis();

        // Avoid continuous beep/Toast for every BLE packet.
        if (now - lastAlertTime < 5000) {
            return;
        }

        lastAlertTime = now;

        statusText.setText(
                "🟢 દૂધવાળો નજીક છે!\n\n" +
                "🔊 દૂધવાળો આવ્યો છે"
        );

        playAlert();

        Toast.makeText(
                this,
                "🥛 દૂધવાળો આવી ગયો!",
                Toast.LENGTH_LONG
        ).show();
    }

    // ============================================================
    // ALERT
    // ============================================================

    private void playAlert() {

        try {

            android.media.ToneGenerator toneGenerator =
                    new android.media.ToneGenerator(
                            android.media.AudioManager.STREAM_NOTIFICATION,
                            100
                    );

            toneGenerator.startTone(
                    android.media.ToneGenerator.TONE_PROP_BEEP,
                    500
            );

            handler.postDelayed(
                    toneGenerator::release,
                    700
            );

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

    // ============================================================
    // PERMISSIONS
    // ============================================================

    private boolean hasRequiredPermissions() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

            boolean scan =
                    checkSelfPermission(
                            Manifest.permission.BLUETOOTH_SCAN)
                            == PackageManager.PERMISSION_GRANTED;

            boolean connect =
                    checkSelfPermission(
                            Manifest.permission.BLUETOOTH_CONNECT)
                            == PackageManager.PERMISSION_GRANTED;

            boolean advertise =
                    checkSelfPermission(
                            Manifest.permission.BLUETOOTH_ADVERTISE)
                            == PackageManager.PERMISSION_GRANTED;

            return scan && connect && advertise;

        } else {

            return checkSelfPermission(
                    Manifest.permission.ACCESS_F
