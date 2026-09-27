package com.dip1968.androidapp;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LocationService extends Service {

    // ---------------------------------------------------------
    // CONFIGURATION
    // ---------------------------------------------------------

    private static final int FG_ID = 100;
    private static final int ALERT_ID = 200;

    // Home checks server every 5 seconds
    private static final long HOME_CHECK_INTERVAL = 5000L;

    // Default society radius
    private static final float DEFAULT_RADIUS = 500f;

    // GPS accuracy required for reliable detection
    private static final float MAX_ACCEPTABLE_ACCURACY = 100f;

    // Exit only after crossing radius + this buffer.
    // Example:
    // radius = 500m
    // enter <= 500m
    // exit  >= 600m
    private static final float EXIT_BUFFER = 100f;

    // Need two consecutive good inside readings
    // before declaring ENTERED.
    private static final int REQUIRED_INSIDE_READINGS = 2;

    // ---------------------------------------------------------
    // ANDROID
    // ---------------------------------------------------------

    private LocationManager locationManager;
    private Handler handler;

    private Runnable homeTask;

    private final ExecutorService networkExecutor =
            Executors.newSingleThreadExecutor();

    // ---------------------------------------------------------
    // LOCATION / SOCIETY
    // ---------------------------------------------------------

    private double societyLat;
    private double societyLon;

    private boolean societySet;

    private float radius = DEFAULT_RADIUS;

    private Location lastLocation;

    // ---------------------------------------------------------
    // STATE
    // ---------------------------------------------------------

    private boolean inside = false;

    private int consecutiveInsideReadings = 0;

    private String lastSentState = "";

    // ---------------------------------------------------------
    // HOME
    // ---------------------------------------------------------

    private String homeId = "HOME1";

    // ---------------------------------------------------------
    // SERVICE ROLE
    // ---------------------------------------------------------

    private String currentRole = "HOME";

    // ---------------------------------------------------------
    // LIFECYCLE
    // ---------------------------------------------------------

    @Override
    public void onCreate() {
        super.onCreate();

        handler = new Handler(Looper.getMainLooper());

        loadSettings();

        createNotificationChannels();
    }

    @Override
    public int onStartCommand(
            Intent intent,
            int flags,
            int startId
    ) {

        if (intent != null) {

            String role =
                    intent.getStringExtra("role");

            if (role != null) {
                currentRole = role;
            }
        }

        if (currentRole == null) {
            currentRole = "HOME";
        }

        // Foreground service notification
        startForeground(
                FG_ID,
                createForegroundNotification(currentRole)
        );

        if ("MILKMAN".equals(currentRole)) {

            startMilkman();

        } else {

            startHome();
        }

        return START_STICKY;
    }

    // ---------------------------------------------------------
    // LOAD SETTINGS
    // ---------------------------------------------------------

    private void loadSettings() {

        SharedPreferences p =
                getSharedPreferences("app", MODE_PRIVATE);

        societySet =
                p.getBoolean("set", false);

        societyLat =
                Double.longBitsToDouble(
                        p.getLong("lat", 0L)
                );

        societyLon =
                Double.longBitsToDouble(
                        p.getLong("lon", 0L)
                );

        homeId =
                p.getString(
                        "homeId",
                        "HOME1"
                );

        // Try to read radius saved by MainActivity.
        // Supports both float and String storage.

        try {

            radius =
                    p.getFloat(
                            "radius",
                            DEFAULT_RADIUS
                    );

        } catch (Exception ignored) {

            try {

                String radiusString =
                        p.getString(
                                "radius",
                                String.valueOf(DEFAULT_RADIUS)
                        );

                radius =
                        Float.parseFloat(radiusString);

            } catch (Exception ignoredAgain) {

                radius = DEFAULT_RADIUS;
            }
        }

        // Safety
        if (radius < 50f) {
            radius = DEFAULT_RADIUS;
        }

        if (radius > 5000f) {
            radius = 5000f;
        }
    }

    // ---------------------------------------------------------
    // NOTIFICATION CHANNELS
    // ---------------------------------------------------------

    private void createNotificationChannels() {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationManager manager =
                (NotificationManager)
                        getSystemService(
                                Context.NOTIFICATION_SERVICE
                        );

        if (manager == null) {
            return;
        }

        // Foreground service channel
        NotificationChannel serviceChannel =
                new NotificationChannel(
                        "service",
                        "Location Service",
                        NotificationManager.IMPORTANCE_LOW
                );

        serviceChannel.setDescription(
                "નૈઋત location monitoring"
        );

        manager.createNotificationChannel(
                serviceChannel
        );

        // Alert channel
        NotificationChannel alertChannel =
                new NotificationChannel(
                        "alert",
                        "Nearby Service Alerts",
                        NotificationManager.IMPORTANCE_HIGH
                );

        alertChannel.setDescription(
                "નજીકની service માટે alerts"
        );

        alertChannel.enableVibration(true);

        manager.createNotificationChannel(
                alertChannel
        );
    }

    // ---------------------------------------------------------
    // FOREGROUND NOTIFICATION
    // ---------------------------------------------------------

    private Notification createForegroundNotification(
            String role
    ) {

        String text;

        if ("MILKMAN".equals(role)) {

            text =
                    "🥛 GPS monitoring ચાલુ છે";

        } else {

            text =
                    "🏠 નજીકની service તપાસી રહ્યા છીએ";
        }

        Notification.Builder builder;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            builder =
                    new Notification.Builder(
                            this,
                            "service"
                    );

        } else {

            builder =
                    new Notification.Builder(this);
        }

        return builder
                .setContentTitle(
                        "નૈઋત"
                )
                .setContentText(text)
                .setSmallIcon(
                        android.R.drawable.ic_dialog_info
                )
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .build();
    }

    // =========================================================
    // MILKMAN / SERVICE PROVIDER
    // =========================================================

    private void startMilkman() {

        if (!societySet) {

            stopSelf();

            return;
        }

        locationManager =
                (LocationManager)
                        getSystemService(
                                Context.LOCATION_SERVICE
                        );

        if (locationManager == null) {
            return;
        }

        if (
                Build.VERSION.SDK_INT >=
                        Build.VERSION_CODES.M
        ) {

            if (
                    checkSelfPermission(
                            Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                    &&
                    checkSelfPermission(
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
            ) {

                stopSelf();

                return;
            }
        }

        try {

            // GPS
            if (
                    locationManager.isProviderEnabled(
                            LocationManager.GPS_PROVIDER
                    )
            ) {

                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        3000L,
                        5f,
                        listener,
                        Looper.getMainLooper()
                );
            }

            // Network location
            if (
                    locationManager.isProviderEnabled(
                            LocationManager.NETWORK_PROVIDER
                    )
            ) {

                locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        5000L,
                        10f,
                        listener,
                        Looper.getMainLooper()
                );
            }

            // Get last known GPS immediately.
            Location gpsLast =
                    locationManager.getLastKnownLocation(
                            LocationManager.GPS_PROVIDER
                    );

            if (gpsLast != null) {

                processLocation(gpsLast);
            }

            // Get last known network location.
            Location networkLast =
                    locationManager.getLastKnownLocation(
                            LocationManager.NETWORK_PROVIDER
                    );

            if (networkLast != null) {

                processLocation(networkLast);
            }

        } catch (SecurityException e) {

            stopSelf();
        }
    }

    // =========================================================
    // LOCATION LISTENER
    // =========================================================

    private final LocationListener listener =
            new LocationListener() {

                @Override
                public void onLocationChanged(
                        Location location
                ) {

                    processLocation(location);
                }

                @Override
                public void onProviderDisabled(
                        String provider
                ) {
                    // Nothing required.
                }

                @Override
                public void onProviderEnabled(
                        String provider
                ) {
                    // Nothing required.
                }
            };

    // =========================================================
    // PROCESS LOCATION
    // =========================================================

    private void processLocation(
            Location location
    ) {

        if (location == null) {
            return;
        }

        // Ignore obviously inaccurate readings.
        if (
                location.hasAccuracy()
                &&
                location.getAccuracy()
                        > MAX_ACCEPTABLE_ACCURACY
        ) {

            return;
        }

        lastLocation = location;

        checkSocietyDistance(location);
    }

    // =========================================================
    // SOCIETY DISTANCE / STATE MACHINE
    // =========================================================

    private void checkSocietyDistance(
            Location location
    ) {

        if (!societySet) {
            return;
        }

        float[] distance =
                new float[1];

        Location.distanceBetween(
                location.getLatitude(),
                location.getLongitude(),
                societyLat,
                societyLon,
                distance
        );

        float distanceMeters =
                distance[0];

        // -----------------------------------------------------
        // ENTER ZONE
        // -----------------------------------------------------

        boolean withinEntryRadius =
                distanceMeters <= radius;

        // -----------------------------------------------------
        // OUTSIDE ZONE
        // Exit has extra buffer to prevent GPS bouncing.
        // -----------------------------------------------------

        boolean beyondExitRadius =
                distanceMeters >
                        (radius + EXIT_BUFFER);

        if (!inside) {

            if (withinEntryRadius) {

                consecutiveInsideReadings++;

            } else {

                consecutiveInsideReadings = 0;
            }

            // Require two consecutive good readings
            if (
                    consecutiveInsideReadings
                            >= REQUIRED_INSIDE_READINGS
            ) {

                inside = true;

                consecutiveInsideReadings = 0;

                sendState(
                        "ENTERED",
                        location
                );
            }

        } else {

            // Already inside.
            // Do not repeatedly send ENTERED.

            if (beyondExitRadius) {

                inside = false;

                consecutiveInsideReadings = 0;

                sendState(
                        "OUTSIDE",
                        location
                );
            }
        }
    }

    // =========================================================
    // SEND STATUS TO GOOGLE APPS SCRIPT
    // =========================================================

    private void sendState(
            String state,
            Location location
    ) {

        if (state == null) {
            return;
        }

        // Prevent duplicate state transmissions.
        if (state.equals(lastSentState)) {

            return;
        }

        lastSentState = state;

        final double lat =
                location != null
                        ? location.getLatitude()
                        : 0;

        final double lon =
                location != null
                        ? location.getLongitude()
                        : 0;

        networkExecutor.execute(() -> {

            boolean success = false;

            try {

                String url =
                        MainActivity.API_URL
                                + "?action=update"
                                + "&homeId="
                                + encode(homeId)
                                + "&token="
                                + encode(
                                MainActivity.TOKEN
                        )
                                + "&status="
                                + encode(state)
                                + "&lat="
                                + lat
                                + "&lon="
                                + lon
                                + "&ts="
                                + System.currentTimeMillis();

                String response =
                        request(url);

                if (response != null) {

                    JSONObject json =
                            new JSONObject(response);

                    success =
                            json.optBoolean(
                                    "ok",
                                    false
                            );
                }

            } catch (Exception ignored) {
            }

            // If server failed, allow the same state
            // to be sent again on next valid location.
            if (!success) {

                lastSentState = "";
            }
        });
    }

    // =========================================================
    // HOME MODE
    // =========================================================

    private void startHome() {

        if (homeTask != null) {
            return;
        }

        homeTask =
                new Runnable() {

                    @Override
                    public void run() {

                        checkHomeStatus();

                        handler.postDelayed(
                                this,
                                HOME_CHECK_INTERVAL
                        );
                    }
                };

        handler.post(homeTask);
    }

    // =========================================================
    // CHECK HOME STATUS
    // =========================================================

    private void checkHomeStatus() {

        networkExecutor.execute(() -> {

            try {

                String url =
                        MainActivity.API_URL
                                + "?action=status"
                                + "&homeId="
                                + encode(homeId)
                                + "&token="
                                + encode(
                                MainActivity.TOKEN
                        );

                String response =
                        request(url);

                if (
                        response == null
                        ||
                        response.trim().isEmpty()
                ) {

                    return;
                }

                JSONObject json =
                        new JSONObject(response);

                String status =
                        json.optString(
                                "status",
                                "OUTSIDE"
                        );

                String event =
                        json.optString(
                                "event",
                                "0"
                        );

                if (
                        "ENTERED".equals(status)
                        &&
                        event != null
                        &&
                        !event.isEmpty()
                ) {

                    SharedPreferences preferences =
                            getSharedPreferences(
                                    "app",
                                    MODE_PRIVATE
                            );

                    String lastEvent =
                            preferences.getString(
                                    "lastEvent",
                                    ""
                            );

                    // New arrival event
                    if (!event.equals(lastEvent)) {

                        preferences.edit()
                                .putString(
                                        "lastEvent",
                                        event
                                )
                                .apply();

                        handler.post(
                                this::showServiceAlert
                        );
                    }
                }

            } catch (Exception ignored) {
            }
        });
    }

    // =========================================================
    // HOME ALERT
    // =========================================================

    private void showServiceAlert() {

        Notification.Builder builder;

        if (
                Build.VERSION.SDK_INT
                        >= Build.VERSION_CODES.O
        ) {

            builder =
                    new Notification.Builder(
                            this,
                            "alert"
                    );

        } else {

            builder =
                    new Notification.Builder(this);
        }

        Notification notification =
                builder
                        .setContentTitle(
                                "🥛 દૂધવાળો Alert"
                        )
                        .setContentText(
                                "દૂધવાળો Societyમાં આવ્યો છે!"
                        )
                        .setSmallIcon(
                                android.R.drawable
                                        .ic_dialog_alert
                        )
                        .setAutoCancel(true)
                        .setPriority(
                                Notification.PRIORITY_MAX
                        )
                        .setVibrate(
                                new long[]{
                                        0,
                                        400,
                                        200,
                                        700
                                }
                        )
                        .build();

        NotificationManager manager =
                (NotificationManager)
                        getSystemService(
                                Context.NOTIFICATION_SERVICE
                        );

        if (manager != null) {

            manager.notify(
                    ALERT_ID,
                    notification
            );
        }
    }

    // =========================================================
    // HTTP REQUEST
    // =========================================================

    private String request(
            String urlString
    ) throws Exception {

        HttpURLConnection connection =
                null;

        InputStream inputStream =
                null;

        try {

            URL url =
                    new URL(urlString);

            connection =
                    (HttpURLConnection)
                            url.openConnection();

            connection.setRequestMethod("GET");

            connection.setConnectTimeout(10000);

            connection.setReadTimeout(10000);

            connection.setInstanceFollowRedirects(
                    true
            );

            connection.setUseCaches(false);

            int responseCode =
                    connection.getResponseCode();

            if (
                    responseCode < 200
                    ||
                    responseCode >= 400
            ) {

                return null;
            }

            inputStream =
                    connection.getInputStream();

            ByteArrayOutputStream output =
                    new ByteArrayOutputStream();

            byte[] buffer =
                    new byte[2048];

            int count;

            while (
                    (count =
                            inputStream.read(buffer))
                            != -1
            ) {

                output.write(
                        buffer,
                        0,
                        count
                );
            }

            return output.toString("UTF-8");

        } finally {

            if (inputStream != null) {

                try {
                    inputStream.close();
                } catch (Exception ignored) {
                }
            }

            if (connection != null) {

                connection.disconnect();
            }
        }
    }

    // =========================================================
    // URL ENCODING
    // =========================================================

    private String encode(
            String value
    ) throws Exception {

        if (value == null) {
            value = "";
        }

        return URLEncoder.encode(
                value,
                "UTF-8"
        );
    }

    // =========================================================
    // SERVICE DESTROY
    // =========================================================

    @Override
    public void onDestroy() {

        if (locationManager != null) {

            try {

                locationManager.removeUpdates(
                        listener
                );

            } catch (Exception ignored) {
            }
        }

        if (
                handler != null
                &&
                homeTask != null
        ) {

            handler.removeCallbacks(
                    homeTask
            );
        }

        homeTask = null;

        networkExecutor.shutdownNow();

        super.onDestroy();
    }

    // =========================================================
    // BIND
    // =========================================================

    @Override
    public IBinder onBind(
            Intent intent
    ) {

        return null;
    }
}
