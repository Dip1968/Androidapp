package com.dip1968.androidapp;

import android.Manifest;
import android.app.Activity;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.location.*;
import android.os.*;
import android.view.*;
import android.widget.*;

import java.util.*;

public class MainActivity extends Activity {

    private static final int REQ = 100;

    /*
     * EXISTING FUNCTIONALITY
     * DO NOT CHANGE
     */
    public static final String API_URL =
            "https://script.google.com/macros/s/AKfycbyV1Wyg-3yX4Lrtm1XT9L3LqsyNGCR1uII3GxxrN6IT_Eg5JuIXiFcNbqNTNJ05YBqY/exec";

    public static final String TOKEN =
            "DUDHWALO_2026_SECRET";

    // ----------------------------------------------------
    // Existing functionality variables
    // ----------------------------------------------------

    private double societyLat;
    private double societyLon;

    private boolean societySet = false;

    private boolean providerRole = false;

    private TextView status;
    private TextView locationValue;
    private TextView radiusValue;
    private TextView serviceValue;
    private TextView serviceStatus;

    private Button startButton;
    private Button stopButton;

    // ----------------------------------------------------
    // Theme
    // ----------------------------------------------------

    private final int PRIMARY = Color.rgb(101, 185, 242);
    private final int SECONDARY = Color.rgb(184, 227, 255);
    private final int BACKGROUND = Color.rgb(244, 250, 255);
    private final int CARD = Color.WHITE;
    private final int TEXT = Color.rgb(23, 50, 77);
    private final int SECONDARY_TEXT = Color.rgb(107, 114, 128);
    private final int SUCCESS = Color.rgb(56, 161, 105);
    private final int WARNING = Color.rgb(255, 180, 84);

    // ----------------------------------------------------
    // Activity
    // ----------------------------------------------------

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        load();

        showWelcome();

        requestPermissionsIfNeeded();
    }

    // ====================================================
    // WELCOME
    // ====================================================

    private void showWelcome() {

        LinearLayout root = baseLayout();

        Space top = new Space(this);
        root.addView(top, new LinearLayout.LayoutParams(
                1, dp(55)
        ));

        TextView logo = text(
                "🥛  📰  🚐\n🥬  💧  🍱",
                30,
                TEXT,
                Gravity.CENTER
        );

        root.addView(logo, matchWrap());

        TextView title = text(
                "નૈઋત",
                38,
                TEXT,
                Gravity.CENTER
        );

        title.setTypeface(null, android.graphics.Typeface.BOLD);

        LinearLayout.LayoutParams titleParams =
                matchWrap();

        titleParams.topMargin = dp(20);

        root.addView(title, titleParams);

        TextView tagline = text(
                "તમારી આસપાસની સેવાઓ,\nહવે તમારી સાથે.",
                19,
                SECONDARY_TEXT,
                Gravity.CENTER
        );

        LinearLayout.LayoutParams tagParams =
                matchWrap();

        tagParams.topMargin = dp(8);

        root.addView(tagline, tagParams);

        TextView english = text(
                "Services around you,\nnow connected with you.",
                14,
                SECONDARY_TEXT,
                Gravity.CENTER
        );

        LinearLayout.LayoutParams englishParams =
                matchWrap();

        englishParams.topMargin = dp(8);

        root.addView(english, englishParams);

        Space middle = new Space(this);

        LinearLayout.LayoutParams middleParams =
                new LinearLayout.LayoutParams(
                        1, 0, 1
                );

        root.addView(middle, middleParams);

        TextView locationIcon = text(
                "📍",
                64,
                PRIMARY,
                Gravity.CENTER
        );

        root.addView(locationIcon, matchWrap());

        Button start = primaryButton(
                "શરૂ કરીએ  →"
        );

        LinearLayout.LayoutParams startParams =
                matchWrap();

        startParams.setMargins(
                dp(25), dp(25), dp(25), dp(12)
        );

        root.addView(start, startParams);

        Button language = secondaryButton(
                "ગુજરાતી  •  हिंदी  •  English"
        );

        root.addView(
                language,
                matchWrap()
        );

        start.setOnClickListener(
                v -> showRoleSelection()
        );

        language.setOnClickListener(
                v -> showRoleSelection()
        );

        setContentView(root);
    }

    // ====================================================
    // ROLE SELECTION
    // ====================================================

    private void showRoleSelection() {

        LinearLayout root = baseLayout();

        addHeader(
                root,
                "નૈઋત",
                "તમે નૈઋતનો ઉપયોગ કેવી રીતે કરશો?"
        );

        LinearLayout homeCard =
                roleCard(
                        "🏠",
                        "ઘર",
                        "તમારી નજીક સેવા આવે ત્યારે જાણો"
                );

        LinearLayout providerCard =
                roleCard(
                        "🥛",
                        "સેવા આપનાર",
                        "તમારી સેવા નજીકના ઘરો સુધી પહોંચાડો"
                );

        LinearLayout.LayoutParams p =
                matchWrap();

        p.setMargins(
                dp(20), dp(25), dp(20), dp(10)
        );

        root.addView(homeCard, p);

        LinearLayout.LayoutParams p2 =
                matchWrap();

        p2.setMargins(
                dp(20), dp(10), dp(20), dp(10)
        );

        root.addView(providerCard, p2);

        TextView info = text(
                "તમે પછીથી પણ તમારી ભૂમિકા બદલી શકો છો.",
                14,
                SECONDARY_TEXT,
                Gravity.CENTER
        );

        LinearLayout.LayoutParams infoParams =
                matchWrap();

        infoParams.topMargin = dp(20);

        root.addView(info, infoParams);

        homeCard.setOnClickListener(
                v -> {
                    providerRole = false;
                    showHomeDashboard();
                }
        );

        providerCard.setOnClickListener(
                v -> {
                    providerRole = true;
                    showProviderDashboard();
                }
        );

        setContentView(root);
    }

    // ====================================================
    // HOME DASHBOARD
    // ====================================================

    private void showHomeDashboard() {

        providerRole = false;

        LinearLayout root = baseLayout();

        addHeader(
                root,
                "નમસ્તે 👋",
                "તમારા આસપાસની સેવાઓ"
        );

        // Location Card
        LinearLayout locationCard =
                card();

        TextView locationTitle =
                text(
                        "📍  તમારું ઘર",
                        18,
                        TEXT,
                        Gravity.LEFT
                );

        locationTitle.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );

        locationCard.addView(
                locationTitle,
                matchWrap()
        );

        locationValue =
                text(
                        societySet
                                ? "Location set છે"
                                : "Location હજુ set નથી",
                        15,
                        SECONDARY_TEXT,
                        Gravity.LEFT
                );

        LinearLayout.LayoutParams lv =
                matchWrap();

        lv.topMargin = dp(8);

        locationCard.addView(
                locationValue,
                lv
        );

        Button locationButton =
                secondaryButton(
                        "📍  Location Set કરો"
                );

        LinearLayout.LayoutParams lb =
                matchWrap();

        lb.topMargin = dp(12);

        locationCard.addView(
                locationButton,
                lb
        );

        LinearLayout.LayoutParams cp =
                matchWrap();

        cp.setMargins(
                dp(20), dp(15), dp(20), dp(8)
        );

        root.addView(locationCard, cp);

        locationButton.setOnClickListener(
                v -> setLocation()
        );

        // Radius
        LinearLayout radiusCard =
                card();

        TextView radiusTitle =
                text(
                        "🎯  Notification Radius",
                        18,
                        TEXT,
                        Gravity.LEFT
                );

        radiusTitle.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );

        radiusCard.addView(
                radiusTitle,
                matchWrap()
        );

        radiusValue =
                text(
                        "500 meter",
                        16,
                        SECONDARY_TEXT,
                        Gravity.LEFT
                );

        LinearLayout.LayoutParams rv =
                matchWrap();

        rv.topMargin = dp(8);

        radiusCard.addView(
                radiusValue,
                rv
        );

        TextView radiusHint =
                text(
                        "Provider આ વિસ્તારમાં આવે ત્યારે તમને alert મળશે.",
                        13,
                        SECONDARY_TEXT,
                        Gravity.LEFT
                );

        LinearLayout.LayoutParams rh =
                matchWrap();

        rh.topMargin = dp(4);

        radiusCard.addView(
                radiusHint,
                rh
        );

        LinearLayout.LayoutParams rp =
                matchWrap();

        rp.setMargins(
                dp(20), dp(8), dp(20), dp(8)
        );

        root.addView(radiusCard, rp);

        // Services
        LinearLayout serviceCard =
                card();

        TextView serviceTitle =
                text(
                        "🔔  મારી સેવાઓ",
                        18,
                        TEXT,
                        Gravity.LEFT
                );

        serviceTitle.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );

        serviceCard.addView(
                serviceTitle,
                matchWrap()
        );

        serviceValue =
                text(
                        "🥛 દૂધ",
                        16,
                        SECONDARY_TEXT,
                        Gravity.LEFT
                );

        LinearLayout.LayoutParams sv =
                matchWrap();

        sv.topMargin = dp(8);

        serviceCard.addView(
                serviceValue,
                sv
        );

        LinearLayout.LayoutParams sp =
                matchWrap();

        sp.setMargins(
                dp(20), dp(8), dp(20), dp(8)
        );

        root.addView(serviceCard, sp);

        // Status
        LinearLayout monitoringCard =
                card();

        TextView monitoringTitle =
                text(
                        "📡  Monitoring",
                        18,
                        TEXT,
                        Gravity.LEFT
                );

        monitoringTitle.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );

        monitoringCard.addView(
                monitoringTitle,
                matchWrap()
        );

        serviceStatus =
                text(
                        "⚪ તૈયાર",
                        16,
                        SECONDARY_TEXT,
                        Gravity.LEFT
                );

        LinearLayout.LayoutParams ss =
                matchWrap();

        ss.topMargin = dp(8);

        monitoringCard.addView(
                serviceStatus,
                ss
        );

        startButton =
                primaryButton(
                        "▶  Monitoring શરૂ કરો"
                );

        LinearLayout.LayoutParams stb =
                matchWrap();

        stb.topMargin = dp(15);

        monitoringCard.addView(
                startButton,
                stb
        );

        stopButton =
                dangerButton(
                        "■  Monitoring બંધ કરો"
                );

        LinearLayout.LayoutParams spb =
                matchWrap();

        spb.topMargin = dp(8);

        monitoringCard.addView(
                stopButton,
                spb
        );

        LinearLayout.LayoutParams mp =
                matchWrap();

        mp.setMargins(
                dp(20), dp(8), dp(20), dp(20)
        );

        root.addView(monitoringCard, mp);

        startButton.setOnClickListener(
                v -> startApp()
        );

        stopButton.setOnClickListener(
                v -> stopApp()
        );

        // Bottom navigation
        addBottomNavigation(
                root,
                false
        );

        setContentView(root);
    }

    // ====================================================
    // PROVIDER DASHBOARD
    // ====================================================

    private void showProviderDashboard() {

        providerRole = true;

        LinearLayout root = baseLayout();

        addHeader(
                root,
                "સેવા આપનાર 👋",
                "તમારી સેવા નજીકના ઘરો સુધી પહોંચાડો"
        );

        // Service card
        LinearLayout serviceCard =
                card();

        TextView serviceTitle =
                text(
                        "🥛  મારી સેવા",
                        18,
                        TEXT,
                        Gravity.LEFT
                );

        serviceTitle.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );

        serviceCard.addView(
                serviceTitle,
                matchWrap()
        );

        TextView serviceName =
                text(
                        "દૂધ સેવા",
                        16,
                        SECONDARY_TEXT,
                        Gravity.LEFT
                );

        LinearLayout.LayoutParams sn =
                matchWrap();

        sn.topMargin = dp(8);

        serviceCard.addView(
                serviceName,
                sn
        );

        LinearLayout.LayoutParams scp =
                matchWrap();

        scp.setMargins(
                dp(20), dp(20), dp(20), dp(8)
        );

        root.addView(
                serviceCard,
                scp
        );

        // Location
        LinearLayout locationCard =
                card();

        TextView locationTitle =
                text(
                        "📍  Location Sharing",
                        18,
                        TEXT,
                        Gravity.LEFT
                );

        locationTitle.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );

        locationCard.addView(
                locationTitle,
                matchWrap()
        );

        locationValue =
                text(
                        societySet
                                ? "Current location ready"
                                : "Location set કરો",
                        15,
                        SECONDARY_TEXT,
                        Gravity.LEFT
                );

        LinearLayout.LayoutParams lvp =
                matchWrap();

        lvp.topMargin = dp(8);

        locationCard.addView(
                locationValue,
                lvp
        );

        Button locButton =
                secondaryButton(
                        "📍  Current Location"
                );

        LinearLayout.LayoutParams locp =
                matchWrap();

        locp.topMargin = dp(12);

        locationCard.addView(
                locButton,
                locp
        );

        LinearLayout.LayoutParams lcp =
                matchWrap();

        lcp.setMargins(
                dp(20), dp(8), dp(20), dp(8)
        );

        root.addView(
                locationCard,
                lcp
        );

        locButton.setOnClickListener(
                v -> setLocation()
        );

        // Active service
        LinearLayout activeCard =
                card();

        TextView activeTitle =
                text(
                        "📡  સેવા સ્થિતિ",
                        18,
                        TEXT,
                        Gravity.LEFT
                );

        activeTitle.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );

        activeCard.addView(
                activeTitle,
                matchWrap()
        );

        serviceStatus =
                text(
                        "⚪ OFFLINE",
                        20,
                        SECONDARY_TEXT,
                        Gravity.LEFT
                );

        LinearLayout.LayoutParams statusParams =
                matchWrap();

        statusParams.topMargin = dp(10);

        activeCard.addView(
                serviceStatus,
                statusParams
        );

        startButton =
                primaryButton(
                        "▶  સેવા શરૂ કરો"
                );

        LinearLayout.LayoutParams sbp =
                matchWrap();

        sbp.topMargin = dp(18);

        activeCard.addView(
                startButton,
                sbp
        );

        stopButton =
                dangerButton(
                        "■  સેવા બંધ કરો"
                );

        LinearLayout.LayoutParams stp =
                matchWrap();

        stp.topMargin = dp(8);

        activeCard.addView(
                stopButton,
                stp
        );

        LinearLayout.LayoutParams acp =
                matchWrap();

        acp.setMargins(
                dp(20), dp(8), dp(20), dp(20)
        );

        root.addView(
                activeCard,
                acp
        );

        startButton.setOnClickListener(
                v -> startApp()
        );

        stopButton.setOnClickListener(
                v -> stopApp()
        );

        addBottomNavigation(
                root,
                true
        );

        setContentView(root);
    }

    // ====================================================
    // EXISTING LOCATION FUNCTIONALITY
    // ====================================================

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

                updateStatus(
                        "⚠️ Location મળી નથી.\nGPS ચાલુ કરો અને ફરી try કરો."
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
                    .putBoolean(
                            "set",
                            true
                    )
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

            if (locationValue != null) {
                locationValue.setText(
                        "📍 Location set successfully"
                );
            }

            updateStatus(
                    "📍 Location Set!\n\n" +
                    "🎯 Radius: 500 meter"
            );

            Toast.makeText(
                    this,
                    "Location Set થઈ ગયું",
                    Toast.LENGTH_SHORT
            ).show();

        } catch (SecurityException e) {

            updateStatus(
                    "⚠️ Location permission required"
            );
        }
    }

    // ====================================================
    // EXISTING START FUNCTIONALITY
    // ====================================================

    private void startApp() {

        if (!societySet) {

            updateStatus(
                    "⚠️ પહેલા Location Set કરો"
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
                providerRole
                        ? "MILKMAN"
                        : "HOME"
        );

        if (Build.VERSION.SDK_INT >= 26) {

            startForegroundService(i);

        } else {

            startService(i);
        }

        if (serviceStatus != null) {

            serviceStatus.setText(
                    providerRole
                            ? "🟢 ONLINE\nGPS Location Sharing ચાલુ છે"
                            : "🟢 MONITORING\nદૂધવાળાની રાહ જોઈ રહ્યા છીએ..."
            );

            serviceStatus.setTextColor(
                    SUCCESS
            );
        }

        if (status != null) {

            status.setText(
                    providerRole
                            ? "🥛 GPS Monitoring ચાલુ..."
                            : "🏠 Monitoring ચાલુ..."
            );
        }
    }

    // ====================================================
    // EXISTING STOP FUNCTIONALITY
    // ====================================================

    private void stopApp() {

        stopService(
                new Intent(
                        this,
                        LocationService.class
                )
        );

        if (serviceStatus != null) {

            serviceStatus.setText(
                    providerRole
                            ? "⚪ OFFLINE"
                            : "⚪ Monitoring બંધ છે"
            );

            serviceStatus.setTextColor(
                    SECONDARY_TEXT
            );
        }

        if (status != null) {

            status.setText(
                    "⚪ STOPPED"
            );
        }
    }

    // ====================================================
    // EXISTING PERMISSIONS
    // ====================================================

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
                    p.toArray(
                            new String[0]
                    ),
                    REQ
            );
        }
    }

    // ====================================================
    // EXISTING LOAD
    // ====================================================

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
                        p.getLong(
                                "lat",
                                0
                        )
                );

        societyLon =
                Double.longBitsToDouble(
                        p.getLong(
                                "lon",
                                0
                        )
                );
    }

    // ====================================================
    // UI HELPERS
    // ====================================================

    private LinearLayout baseLayout() {

        LinearLayout root =
                new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL
        );

        root.setPadding(
                dp(18),
                dp(18),
                dp(18),
                dp(10)
        );

        root.setBackgroundColor(
                BACKGROUND
        );

        ScrollView scroll =
                new ScrollView(this);

        scroll.setFillViewport(true);

        scroll.addView(root);

        // Return root itself.
        // Main content remains scrollable through
        // the outer wrapper created below.

        return createScrollableRoot(root);
    }

    private LinearLayout createScrollableRoot(
            LinearLayout content
    ) {

        LinearLayout wrapper =
                new LinearLayout(this);

        wrapper.setOrientation(
                LinearLayout.VERTICAL
        );

        wrapper.setBackgroundColor(
                BACKGROUND
        );

        ScrollView scroll =
                new ScrollView(this);

        scroll.setFillViewport(true);

        scroll.addView(content);

        wrapper.addView(
                scroll,
                new LinearLayout.LayoutParams(
                        -1,
                        0,
                        1
                )
        );

        return wrapper;
    }

    private void addHeader(
            LinearLayout root,
            String title,
            String subtitle
    ) {

        TextView t =
                text(
                        title,
                        30,
                        TEXT,
                        Gravity.LEFT
                );

        t.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );

        root.addView(
                t,
                matchWrap()
        );

        TextView s =
                text(
                        subtitle,
                        15,
                        SECONDARY_TEXT,
                        Gravity.LEFT
                );

        LinearLayout.LayoutParams sp =
                matchWrap();

        sp.topMargin = dp(4);

        root.addView(
                s,
                sp
        );
    }

    private LinearLayout roleCard(
            String icon,
            String title,
            String description
    ) {

        LinearLayout card =
                card();

        TextView iconView =
                text(
                        icon,
                        40,
                        TEXT,
                        Gravity.CENTER
                );

        LinearLayout.LayoutParams ip =
                matchWrap();

        card.addView(
                iconView,
                ip
        );

        TextView t =
                text(
                        title,
                        22,
                        TEXT,
                        Gravity.CENTER
                );

        t.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );

        LinearLayout.LayoutParams tp =
                matchWrap();

        tp.topMargin = dp(8);

        card.addView(
                t,
                tp
        );

        TextView d =
                text(
                        description,
                        14,
                        SECONDARY_TEXT,
                        Gravity.CENTER
                );

        LinearLayout.LayoutParams dp =
                matchWrap();

        dp.topMargin = dp(5);

        card.addView(
                d,
                dp
        );

        return card;
    }

    private LinearLayout card() {

        LinearLayout c =
                new LinearLayout(this);

        c.setOrientation(
                LinearLayout.VERTICAL
        );

        c.setPadding(
                dp(18),
                dp(18),
                dp(18),
                dp(18)
        );

        GradientDrawable bg =
                new GradientDrawable();

        bg.setColor(CARD);

        bg.setCornerRadius(
                dp(20)
        );

        c.setBackground(bg);

        c.setElevation(
                dp(2)
        );

        return c;
    }

    private Button primaryButton(
            String title
    ) {

        Button b =
                new Button(this);

        b.setText(title);

        b.setTextSize(16);

        b.setTextColor(
                Color.WHITE
        );

        b.setAllCaps(false);

        b.setMinHeight(
                dp(52)
        );

        GradientDrawable bg =
                new GradientDrawable();

        bg.setColor(
                PRIMARY
        );

        bg.setCornerRadius(
                dp(16)
        );

        b.setBackground(bg);

        return b;
    }

    private Button secondaryButton(
            String title
    ) {

        Button b =
                new Button(this);

        b.setText(title);

        b.setTextSize(15);

        b.setTextColor(
                TEXT
        );

        b.setAllCaps(false);

        b.setMinHeight(
                dp(50)
        );

        GradientDrawable bg =
                new GradientDrawable();

        bg.setColor(
                SECONDARY
        );

        bg.setCornerRadius(
                dp(15)
        );

        b.setBackground(bg);

        return b;
    }

    private Button dangerButton(
            String title
    ) {

        Button b =
                new Button(this);

        b.setText(title);

        b.setTextSize(15);

        b.setTextColor(
                TEXT
        );

        b.setAllCaps(false);

        b.setMinHeight(
                dp(50)
        );

        GradientDrawable bg =
                new GradientDrawable();

        bg.setColor(
                Color.rgb(
                        235,
                        242,
                        247
                )
        );

        bg.setCornerRadius(
                dp(15)
        );

        b.setBackground(bg);

        return b;
    }

    private TextView text(
            String value,
            float size,
            int color,
            int gravity
    ) {

        TextView t =
                new TextView(this);

        t.setText(value);

        t.setTextSize(size);

        t.setTextColor(color);

        t.setGravity(gravity);

        t.setPadding(
                dp(2),
                dp(2),
                dp(2),
                dp(2)
        );

        return t;
    }

    private LinearLayout.LayoutParams matchWrap() {

        return new LinearLayout.LayoutParams(
                -1,
                -2
        );
    }

    private int dp(int value) {

        return (int) (
                value *
                        getResources()
                                .getDisplayMetrics()
                                .density
        );
    }

    private void updateStatus(
            String message
    ) {

        if (status != null) {
            status.setText(message);
        }

        if (serviceStatus != null) {
            serviceStatus.setText(message);
        }
    }

    // ====================================================
    // BOTTOM NAVIGATION
    // ====================================================

    private void addBottomNavigation(
            LinearLayout root,
            boolean provider
    ) {

        LinearLayout nav =
                new LinearLayout(this);

        nav.setOrientation(
                LinearLayout.HORIZONTAL
        );

        nav.setGravity(
                Gravity.CENTER
        );

        nav.setPadding(
                0,
                dp(8),
                0,
                dp(8)
        );

        GradientDrawable bg =
                new GradientDrawable();

        bg.setColor(
                Color.WHITE
        );

        bg.setCornerRadius(
                dp(18)
        );

        nav.setBackground(bg);

        String[] items =
                provider
                        ? new String[]{
                        "⌂\nHome",
                        "◉\nActivity",
                        "🔔\nAlerts",
                        "●\nProfile"
                }
                        : new String[]{
                        "⌂\nHome",
                        "📍\nNearby",
                        "🔔\nAlerts",
                        "●\nProfile"
                };

        for (String item : items) {

            TextView v =
                    text(
                            item,
                            12,
                            TEXT,
                            Gravity.CENTER
                    );

            LinearLayout.LayoutParams np =
                    new LinearLayout.LayoutParams(
                            0,
                            dp(58),
                            1
                    );

            nav.addView(
                    v,
                    np
            );
        }

        LinearLayout.LayoutParams navParams =
                matchWrap();

        navParams.setMargins(
                dp(5),
                dp(5),
                dp(5),
                dp(5)
        );

        root.addView(
                nav,
                navParams
        );
    }
}
