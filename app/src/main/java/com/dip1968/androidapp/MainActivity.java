package com.dip1968.androidapp;

import android.Manifest;
import android.app.Activity;
import android.content.*;
import android.content.pm.PackageManager;
import android.location.*;
import android.os.*;
import android.view.*;
import android.widget.*;

import java.util.*;

public class MainActivity extends Activity {

    private static final int REQ = 100;

    /*
     * IMPORTANT:
     * અહીં તમારું Google Apps Script URL નાખવાનું છે.
     */
    public static final String API_URL =
            "https://script.google.com/macros/s/AKfycbyV1Wyg-3yX4Lrtm1XT9L3LqsyNGCR1uII3GxxrN6IT_Eg5JuIXiFcNbqNTNJ05YBqY/exec";

    public static final String TOKEN =
            "DUDHWALO_2026_SECRET";

    private RadioGroup roles;
    private TextView status;

    private double societyLat;
    private double societyLon;

    private boolean societySet = false;

    @Override
    protected void onCreate(Bundle b) {

        super.onCreate(b);

        buildUI();
        load();

        requestPermissionsIfNeeded();
    }

    private void buildUI() {

        LinearLayout box =
                new LinearLayout(this);

        box.setOrientation(
                LinearLayout.VERTICAL
        );

        box.setPadding(
                25, 35, 25, 25
        );

        TextView title =
                new TextView(this);

        title.setText(
                "🥛 દૂધવાળો Alert V3"
        );

        title.setTextSize(26);
        title.setGravity(Gravity.CENTER);

        box.addView(title);

        roles = new RadioGroup(this);

        RadioButton home =
                new RadioButton(this);

        home.setText("🏠 ઘર");

        RadioButton milk =
                new RadioButton(this);

        milk.setText("🥛 દૂધવાળો");

        roles.addView(home);
        roles.addView(milk);

        home.setChecked(true);

        box.addView(roles);

        Button loc =
                new Button(this);

        loc.setText(
                "📍 Society Location Set કરો"
        );

        box.addView(loc);

        Button start =
                new Button(this);

        start.setText("▶️ START");

        box.addView(start);

        Button stop =
                new Button(this);

        stop.setText("⏹ STOP");

        box.addView(stop);

        status =
                new TextView(this);

        status.setText(
                "\n⚪ Ready"
        );

        status.setTextSize(18);

        box.addView(status);

        setContentView(box);

        loc.setOnClickListener(
                v -> setLocation()
        );

        start.setOnClickListener(
                v -> startApp()
        );

        stop.setOnClickListener(
                v -> stopApp()
        );
    }

    private void requestPermissionsIfNeeded() {

        ArrayList<String> p =
                new ArrayList<>();

        if (Build.VERSION.SDK_INT >= 23) {

            if (checkSelfPermission(
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {

                p.add(
                        Manifest.permission.ACCESS_FINE_LOCATION
                );
            }

            if (checkSelfPermission(
                    Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {

                p.add(
                        Manifest.permission.ACCESS_COARSE_LOCATION
                );
            }
        }

        if (Build.VERSION.SDK_INT >= 33) {

            if (checkSelfPermission(
                    Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                p.add(
                        Manifest.permission.POST_NOTIFICATIONS
                );
            }
        }

        if (!p.isEmpty()) {

            requestPermissions(
                    p.toArray(new String[0]),
                    REQ
            );
        }
    }

    private void setLocation() {

        if (checkSelfPermission(
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissionsIfNeeded();

            Toast.makeText(
                    this,
                    "Location Permission આપો",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        LocationManager lm =
                (LocationManager)
                        getSystemService(
                                LOCATION_SERVICE
                        );

        try {

            Location l =
                    lm.getLastKnownLocation(
                            LocationManager.GPS_PROVIDER
                    );

            if (l == null) {

                l = lm.getLastKnownLocation(
                        LocationManager.NETWORK_PROVIDER
                );
            }

            if (l == null) {

                status.setText(
                        "⚠️ Location મળી નથી.\n" +
                        "GPS ચાલુ કરો અને ફરી try કરો."
                );

                return;
            }

            societyLat =
                    l.getLatitude();

            societyLon =
                    l.getLongitude();

            societySet = true;

            getSharedPreferences(
                    "app", 0
            ).edit()
                    .putBoolean("set", true)
                    .putLong(
                            "lat",
                            Double.doubleToLongBits(
                                    societyLat
                            )
                    )
                    .putLong(
                            "lon",
                            Double.doubleToLongBits(
                                    societyLon
                            )
                    )
                    .apply();

            status.setText(
                    "📍 Society Location Set!\n\n" +
                    "🎯 Radius: 150 meter"
            );

        } catch (SecurityException e) {

            status.setText(
                    "⚠️ Location permission required"
            );
        }
    }

    private void startApp() {

        int id =
                roles.getCheckedRadioButtonId();

        RadioButton selected =
                findViewById(id);

        boolean milkman =
                selected != null &&
                selected.getText()
                        .toString()
                        .contains("દૂધવાળો");

        if (!societySet) {

            status.setText(
                    "⚠️ પહેલા Society Location Set કરો"
            );

            return;
        }

        Intent i =
                new Intent(
                        this,
                        LocationService.class
                );

        i.putExtra(
                "role",
                milkman ? "MILKMAN" : "HOME"
        );

        if (Build.VERSION.SDK_INT >= 26) {

            startForegroundService(i);

        } else {

            startService(i);
        }

        status.setText(
                milkman
                        ? "🥛 GPS Monitoring ચાલુ..."
                        : "🏠 દૂધવાળાની રાહ જોઈ રહ્યા છીએ..."
        );
    }

    private void stopApp() {

        stopService(
                new Intent(
                        this,
                        LocationService.class
                )
        );

        status.setText(
                "⚪ STOPPED"
        );
    }

    private void load() {

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
    }
}
