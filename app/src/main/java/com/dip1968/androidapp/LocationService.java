package com.dip1968.androidapp;

import android.app.*;
import android.content.*;
import android.location.*;
import android.os.*;

import java.io.*;
import java.net.*;
import java.util.*;

public class LocationService
        extends Service {

    private static final int FG_ID = 100;

    private static final int ALERT_ID = 200;

    private static final long CHECK =
            5000;

    private static final float RADIUS =
            150f;

    private LocationManager locationManager;

    private Handler handler;

    private Runnable homeTask;

    private Location lastLocation;

    private double societyLat;

    private double societyLon;

    private boolean societySet;

    private boolean inside = false;

    private String homeId;

    @Override
    public void onCreate() {

        super.onCreate();

        handler =
                new Handler(
                        Looper.getMainLooper()
                );

        loadSettings();

        createChannels();
    }

    private void loadSettings() {

        SharedPreferences p =
                getSharedPreferences(
                        "app", 0
                );

        societySet =
                p.getBoolean(
                        "set",
                        false
                );

        societyLat =
                Double.longBitsToDouble(
                        p.getLong("lat", 0)
                );

        societyLon =
                Double.longBitsToDouble(
                        p.getLong("lon", 0)
                );

        homeId =
                p.getString(
                        "homeId",
                        "HOME1"
                );
    }

    @Override
    public int onStartCommand(
            Intent intent,
            int flags,
            int startId) {

        String role =
                intent.getStringExtra(
                        "role"
                );

        if (role == null)
            role = "HOME";

        startForeground(
                FG_ID,
                foregroundNotification(role)
        );

        if (role.equals("MILKMAN")) {

            startMilkman();

        } else {

            startHome();
        }

        return START_STICKY;
    }

    private Notification foregroundNotification(
            String role) {

        String text =
                role.equals("MILKMAN")
                        ? "🥛 GPS Monitoring ચાલુ"
                        : "🏠 દૂધવાળાની રાહ જોઈ રહ્યા છીએ";

        return new Notification.Builder(
                this,
                "service"
        )
                .setContentTitle(
                        "દૂધવાળો Alert"
                )
                .setContentText(text)
                .setSmallIcon(
                        android.R.drawable.ic_dialog_info
                )
                .setOngoing(true)
                .build();
    }

    private void createChannels() {

        if (Build.VERSION.SDK_INT >= 26) {

            NotificationManager nm =
                    (NotificationManager)
                            getSystemService(
                                    NOTIFICATION_SERVICE
                            );

            nm.createNotificationChannel(
                    new NotificationChannel(
                            "service",
                            "Location Service",
                            NotificationManager
                                    .IMPORTANCE_LOW
                    )
            );

            nm.createNotificationChannel(
                    new NotificationChannel(
                            "alert",
                            "Milkman Alert",
                            NotificationManager
                                    .IMPORTANCE_HIGH
                    )
            );
        }
    }

    private void startMilkman() {

        if (!societySet) {

            stopSelf();

            return;
        }

        locationManager =
                (LocationManager)
                        getSystemService(
                                LOCATION_SERVICE
                        );

        try {

            locationManager
                    .requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            3000,
                            5,
                            listener
                    );

            locationManager
                    .requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            5000,
                            10,
                            listener
                    );

        } catch (SecurityException ignored) {
        }
    }

    private final LocationListener listener =
            new LocationListener() {

        @Override
        public void onLocationChanged(
                Location location) {

            lastLocation = location;

            checkLocation(location);
        }
    };

    private void checkLocation(
            Location location) {

        if (!societySet)
            return;

        float[] distance =
                new float[1];

        Location.distanceBetween(
                location.getLatitude(),
                location.getLongitude(),
                societyLat,
                societyLon,
                distance
        );

        boolean nowInside =
                distance[0] <= RADIUS;

        if (nowInside != inside) {

            inside = nowInside;

            sendStatus(
                    nowInside
                            ? "ENTERED"
                            : "OUTSIDE"
            );
        }
    }

    private void sendStatus(
            String state) {

        final Location l =
                lastLocation;

        new Thread(() -> {

            try {

                String url =
                        MainActivity.API_URL +
                        "?action=update" +
                        "&homeId=" +
                        encode(homeId) +
                        "&token=" +
                        encode(
                                MainActivity.TOKEN
                        ) +
                        "&status=" +
                        encode(state) +
                        "&lat=" +
                        (l == null
                                ? ""
                                : l.getLatitude()) +
                        "&lon=" +
                        (l == null
                                ? ""
                                : l.getLongitude()) +
                        "&ts=" +
                        System.currentTimeMillis();

                request(url);

            } catch (Exception ignored) {
            }

        }).start();
    }

    private void startHome() {

        if (homeTask != null)
            return;

        homeTask =
                new Runnable() {

            @Override
            public void run() {

                checkHome();

                handler.postDelayed(
                        this,
                        CHECK
                );
            }
        };

        handler.post(homeTask);
    }

    private void checkHome() {

        new Thread(() -> {

            try {

                String url =
                        MainActivity.API_URL +
                        "?action=status" +
                        "&homeId=" +
                        encode(homeId) +
                        "&token=" +
                        encode(
                                MainActivity.TOKEN
                        );

                String response =
                        request(url);

                String state =
                        value(
                                response,
                                "status"
                        );

                String event =
                        value(
                                response,
                                "event"
                        );

                if ("ENTERED".equals(state)) {

                    SharedPreferences p =
                            getSharedPreferences(
                                    "app",
                                    0
                            );

                    String old =
                            p.getString(
                                    "lastEvent",
                                    ""
                            );

                    if (!event.equals(old)) {

                        p.edit()
                                .putString(
                                        "lastEvent",
                                        event
                                )
                                .apply();

                        handler.post(
                                this::showAlert
                        );
                    }
                }

            } catch (Exception ignored) {
            }

        }).start();
    }

    private void showAlert() {

        Notification notification =
                new Notification.Builder(
                        this,
                        "alert"
                )
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
                        .setPriority(
                                Notification.PRIORITY_MAX
                        )
                        .setAutoCancel(true)
                        .setVibrate(
                                new long[]{
                                        0,
                                        400,
                                        200,
                                        700
                                }
                        )
                        .build();

        NotificationManager nm =
                (NotificationManager)
                        getSystemService(
                                NOTIFICATION_SERVICE
                        );

        nm.notify(
                ALERT_ID,
                notification
        );
    }

    private String request(
            String url)
            throws Exception {

        HttpURLConnection c =
                (HttpURLConnection)
                        new URL(url)
                                .openConnection();

        c.setRequestMethod("GET");

        c.setConnectTimeout(7000);

        c.setReadTimeout(7000);

        InputStream in =
                c.getInputStream();

        ByteArrayOutputStream out =
                new ByteArrayOutputStream();

        byte[] buffer =
                new byte[1024];

        int n;

        while ((n =
                in.read(buffer)) != -1) {

            out.write(
                    buffer,
                    0,
                    n
            );
        }

        in.close();

        c.disconnect();

        return out.toString(
                "UTF-8"
        );
    }

    private String encode(
            String s)
            throws Exception {

        return URLEncoder.encode(
                s,
                "UTF-8"
        );
    }

    private String value(
            String json,
            String key) {

        String find =
                "\"" + key + "\":\"";

        int start =
                json.indexOf(find);

        if (start < 0)
            return "";

        start += find.length();

        int end =
                json.indexOf(
                        "\"",
                        start
                );

        if (end < 0)
            return "";

        return json.substring(
                start,
                end
        );
    }

    @Override
    public void onDestroy() {

        if (locationManager != null) {

            try {

                locationManager
                        .removeUpdates(
                                listener
                        );

            } catch (Exception ignored) {
            }
        }

        if (handler != null &&
                homeTask != null) {

            handler.removeCallbacks(
                    homeTask
            );
        }

        super.onDestroy();
    }

    @Override
    public IBinder onBind(
            Intent intent) {

        return null;
    }
        }
