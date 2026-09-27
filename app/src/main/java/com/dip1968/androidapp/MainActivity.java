package com.dip1968.androidapp;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends Activity {

    // ============================================================
    // EXISTING FUNCTIONALITY
    // ============================================================

    private static final int REQ = 100;

    public static final String API_URL =
            "https://script.google.com/macros/s/AKfycbyV1Wyg-3yX4Lrtm1XT9L3qsyNGCR1uII3GxxrN6IT_Eg5JuIXiFcNbqNTNJ05YBqY/exec";

    public static final String TOKEN =
            "DUDHWALO_2026_SECRET";

    private double societyLat;
    private double societyLon;
    private boolean societySet = false;

    private boolean providerRole = false;

    // Current screen references
    private TextView locationValue;
    private TextView serviceStatus;
    private TextView globalStatus;

    // ============================================================
    // NAIṚT THEME
    // ============================================================

    private final int PRIMARY = Color.rgb(101, 185, 242);
    private final int SECONDARY = Color.rgb(184, 227, 255);
    private final int BACKGROUND = Color.rgb(244, 250, 255);
    private final int CARD = Color.WHITE;
    private final int TEXT = Color.rgb(23, 50, 77);
    private final int SECONDARY_TEXT = Color.rgb(107, 114, 128);
    private final int SUCCESS = Color.rgb(56, 161, 105);
    private final int WARNING = Color.rgb(255, 180, 84);

    // ============================================================
    // ACTIVITY
    // ============================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        load();

        showWelcome();

        requestPermissionsIfNeeded();
    }

    // ============================================================
    // WELCOME SCREEN
    // ============================================================

    private void showWelcome() {

        LinearLayout content = createContent();

        addSpace(content, 35);

        TextView icons = makeText(
                "🥛  📰  🚐\n🥬  💧  🍱",
                30,
                TEXT,
                Gravity.CENTER
        );

        content.addView(icons, match());

        addSpace(content, 18);

        TextView title = makeText(
                "નૈઋત",
                40,
                TEXT,
                Gravity.CENTER
        );

        title.setTypeface(null, Typeface.BOLD);

        content.addView(title, match());

        addSpace(content, 8);

        TextView subtitle = makeText(
                "તમારી આસપાસની સેવાઓ,\nહવે તમારી સાથે.",
                19,
                SECONDARY_TEXT,
                Gravity.CENTER
        );

        content.addView(subtitle, match());

        addSpace(content, 8);

        TextView english = makeText(
                "Services around you,\nnow connected with you.",
                14,
                SECONDARY_TEXT,
                Gravity.CENTER
        );

        content.addView(english, match());

        Space flexible = new Space(this);

        content.addView(
                flexible,
                new LinearLayout.LayoutParams(
                        1,
                        0,
                        1
                )
        );

        TextView locationIcon = makeText(
                "📍",
                65,
                PRIMARY,
                Gravity.CENTER
        );

        content.addView(locationIcon, match());

        addSpace(content, 20);

        Button start = primaryButton("શરૂ કરીએ  →");

        LinearLayout.LayoutParams startParams = match();
        startParams.setMargins(
                dp(25),
                0,
                dp(25),
                dp(12)
        );

        content.addView(start, startParams);

        Button language = secondaryButton(
                "ગુજરાતી  •  हिंदी  •  English"
        );

        LinearLayout.LayoutParams languageParams = match();
        languageParams.setMargins(
                dp(25),
                0,
                dp(25),
                dp(20)
        );

        content.addView(language, languageParams);

        start.setOnClickListener(v -> showRoleSelection());

        language.setOnClickListener(v -> showRoleSelection());

        setScreen(content);
    }

    // ============================================================
    // ROLE SELECTION
    // ============================================================

    private void showRoleSelection() {

        LinearLayout content = createContent();

        addHeader(
                content,
                "નૈઋત",
                "તમે નૈઋતનો ઉપયોગ કેવી રીતે કરશો?"
        );

        addSpace(content, 20);

        LinearLayout homeCard = roleCard(
                "🏠",
                "ઘર",
                "તમારી નજીક સેવા આવે ત્યારે જાણો"
        );

        LinearLayout.LayoutParams homeParams = match();
        homeParams.setMargins(
                dp(18),
                0,
                dp(18),
                dp(12)
        );

        content.addView(homeCard, homeParams);

        LinearLayout providerCard = roleCard(
                "🥛",
                "સેવા આપનાર",
                "તમારી સેવા નજીકના ઘરો સુધી પહોંચાડો"
        );

        LinearLayout.LayoutParams providerParams = match();
        providerParams.setMargins(
                dp(18),
                0,
                dp(18),
                dp(12)
        );

        content.addView(providerCard, providerParams);

        addSpace(content, 10);

        TextView info = makeText(
                "તમે પછીથી પણ તમારી ભૂમિકા બદલી શકો છો.",
                14,
                SECONDARY_TEXT,
                Gravity.CENTER
        );

        content.addView(info, match());

        homeCard.setOnClickListener(v -> {
            providerRole = false;
            showHomeDashboard();
        });

        providerCard.setOnClickListener(v -> {
            providerRole = true;
            showProviderDashboard();
        });

        setScreen(content);
    }

    // ============================================================
    // HOME DASHBOARD
    // ============================================================

    private void showHomeDashboard() {

        providerRole = false;

        LinearLayout content = createContent();

        addHeader(
                content,
                "નમસ્તે 👋",
                "તમારા આસપાસની સેવાઓ"
        );

        // --------------------------------------------------------
        // LOCATION CARD
        // --------------------------------------------------------

        LinearLayout locationCard = card();

        TextView locationTitle = makeText(
                "📍  તમારું ઘર",
                18,
                TEXT,
                Gravity.LEFT
        );

        bold(locationTitle);

        locationCard.addView(locationTitle, match());

        locationValue = makeText(
                societySet
                        ? "📍 Location set છે"
                        : "Location હજુ set નથી",
                15,
                SECONDARY_TEXT,
                Gravity.LEFT
        );

        addTopMargin(locationCard, locationValue, 8);

        Button locationButton = secondaryButton(
                "📍  Location Set કરો"
        );

        addTopMargin(locationCard, locationButton, 12);

        LinearLayout.LayoutParams locationParams = match();
        locationParams.setMargins(
                dp(18),
                dp(15),
                dp(18),
                dp(8)
        );

        content.addView(locationCard, locationParams);

        locationButton.setOnClickListener(v -> setLocation());

        // --------------------------------------------------------
        // RADIUS CARD
        // --------------------------------------------------------

        LinearLayout radiusCard = card();

        TextView radiusTitle = makeText(
                "🎯  Notification Radius",
                18,
                TEXT,
                Gravity.LEFT
        );

        bold(radiusTitle);

        radiusCard.addView(radiusTitle, match());

        TextView radiusValue = makeText(
                "500 meter",
                16,
                SECONDARY_TEXT,
                Gravity.LEFT
        );

        addTopMargin(radiusCard, radiusValue, 8);

        TextView radiusHint = makeText(
                "Provider આ વિસ્તારમાં આવે ત્યારે તમને alert મળશે.",
                13,
                SECONDARY_TEXT,
                Gravity.LEFT
        );

        addTopMargin(radiusCard, radiusHint, 4);

        LinearLayout.LayoutParams radiusParams = match();
        radiusParams.setMargins(
                dp(18),
                dp(8),
                dp(18),
                dp(8)
        );

        content.addView(radiusCard, radiusParams);

        // --------------------------------------------------------
        // SERVICES CARD
        // --------------------------------------------------------

        LinearLayout servicesCard = card();

        TextView servicesTitle = makeText(
                "🔔  મારી સેવાઓ",
                18,
                TEXT,
                Gravity.LEFT
        );

        bold(servicesTitle);

        servicesCard.addView(servicesTitle, match());

        TextView services = makeText(
                "🥛  દૂધ",
                16,
                SECONDARY_TEXT,
                Gravity.LEFT
        );

        addTopMargin(servicesCard, services, 8);

        LinearLayout.LayoutParams servicesParams = match();
        servicesParams.setMargins(
                dp(18),
                dp(8),
                dp(18),
                dp(8)
        );

        content.addView(servicesCard, servicesParams);

        // --------------------------------------------------------
        // MONITORING CARD
        // --------------------------------------------------------

        LinearLayout monitoringCard = card();

        TextView monitoringTitle = makeText(
                "📡  Monitoring",
                18,
                TEXT,
                Gravity.LEFT
        );

        bold(monitoringTitle);

        monitoringCard.addView(
                monitoringTitle,
                match()
        );

        serviceStatus = makeText(
                "⚪ તૈયાર",
                16,
                SECONDARY_TEXT,
                Gravity.LEFT
        );

        addTopMargin(
                monitoringCard,
                serviceStatus,
                8
        );

        Button start = primaryButton(
                "▶  Monitoring શરૂ કરો"
        );

        addTopMargin(
                monitoringCard,
                start,
                15
        );

        Button stop = dangerButton(
                "■  Monitoring બંધ કરો"
        );

        addTopMargin(
                monitoringCard,
                stop,
                8
        );

        LinearLayout.LayoutParams monitoringParams = match();

        monitoringParams.setMargins(
                dp(18),
                dp(8),
                dp(18),
                dp(20)
        );

        content.addView(
                monitoringCard,
                monitoringParams
        );

        start.setOnClickListener(v -> startApp());

        stop.setOnClickListener(v -> stopApp());

        // --------------------------------------------------------
        // STATUS
        // --------------------------------------------------------

        globalStatus = makeText(
                "",
                13,
                SECONDARY_TEXT,
                Gravity.CENTER
        );

        content.addView(globalStatus, match());

        addBottomNavigation(
                content,
                false
        );

        setScreen(content);
    }

    // ============================================================
    // PROVIDER DASHBOARD
    // ============================================================

    private void showProviderDashboard() {

        providerRole = true;

        LinearLayout content = createContent();

        addHeader(
                content,
                "સેવા આપનાર 👋",
                "તમારી સેવા નજીકના ઘરો સુધી પહોંચાડો"
        );

        // --------------------------------------------------------
        // SERVICE CARD
        // --------------------------------------------------------

        LinearLayout serviceCard = card();

        TextView serviceTitle = makeText(
                "🥛  મારી સેવા",
                18,
                TEXT,
                Gravity.LEFT
        );

        bold(serviceTitle);

        serviceCard.addView(
                serviceTitle,
                match()
        );

        TextView serviceName = makeText(
                "દૂધ સેવા",
                16,
                SECONDARY_TEXT,
                Gravity.LEFT
        );

        addTopMargin(
                serviceCard,
                serviceName,
                8
        );

        LinearLayout.LayoutParams serviceParams = match();

        serviceParams.setMargins(
                dp(18),
                dp(20),
                dp(18),
                dp(8)
        );

        content.addView(
                serviceCard,
                serviceParams
        );

        // --------------------------------------------------------
        // LOCATION SHARING CARD
        // --------------------------------------------------------

        LinearLayout locationCard = card();

        TextView locationTitle = makeText(
                "📍  Location Sharing",
                18,
                TEXT,
                Gravity.LEFT
        );

        bold(locationTitle);

        locationCard.addView(
                locationTitle,
                match()
        );

        locationValue = makeText(
                societySet
                        ? "📍 Current location ready"
                        : "Location set કરો",
                15,
                SECONDARY_TEXT,
                Gravity.LEFT
        );

        addTopMargin(
                locationCard,
                locationValue,
                8
        );

        Button currentLocation = secondaryButton(
                "📍  Current Location"
        );

        addTopMargin(
                locationCard,
                currentLocation,
                12
        );

        LinearLayout.LayoutParams locationParams = match();

        locationParams.setMargins(
                dp(18),
                dp(8),
                dp(18),
                dp(8)
        );

        content.addView(
                locationCard,
                locationParams
        );

        currentLocation.setOnClickListener(
                v -> setLocation()
        );

        // --------------------------------------------------------
        // ACTIVE SERVICE CARD
        // --------------------------------------------------------

        LinearLayout activeCard = card();

        TextView activeTitle = makeText(
                "📡  સેવા સ્થિતિ",
                18,
                TEXT,
                Gravity.LEFT
        );

        bold(activeTitle);

        activeCard.addView(
                activeTitle,
                match()
        );

        serviceStatus = makeText(
                "⚪ OFFLINE",
                20,
                SECONDARY_TEXT,
                Gravity.LEFT
        );

        addTopMargin(
                activeCard,
                serviceStatus,
                10
        );

        Button start = primaryButton(
                "▶  સેવા શરૂ કરો"
        );

        addTopMargin(
                activeCard,
                start,
                18
        );

        Button stop = dangerButton(
                "■  સેવા બંધ કરો"
        );

        addTopMargin(
                activeCard,
                stop,
                8
        );

        LinearLayout.LayoutParams activeParams = match();

        activeParams.setMargins(
                dp(18),
                dp(8),
                dp(18),
                dp(20)
        );

        content.addView(
                activeCard,
                activeParams
        );

        start.setOnClickListener(
                v -> startApp()
        );

        stop.setOnClickListener(
                v -> stopApp()
        );

        // --------------------------------------------------------
        // STATUS
        // --------------------------------------------------------

        globalStatus = makeText(
                "",
                13,
                SECONDARY_TEXT,
                Gravity.CENTER
        );

        content.addView(
                globalStatus,
                match()
        );

        addBottomNavigation(
                content,
                true
        );

        setScreen(content);
    }

    // ============================================================
    // EXISTING LOCATION FUNCTIONALITY
    // ============================================================

    private void setLocation() {

        if (Build.VERSION.SDK_INT >= 23 &&
                checkSelfPermission(
                        Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED) {

            requestPermissionsIfNeeded();

            Toast.makeText(
                    this,
                    "Location Permission આપો",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        LocationManager locationManager =
                (LocationManager)
                        getSystemService(LOCATION_SERVICE);

        try {

            Location location =
                    locationManager.getLastKnownLocation(
                            LocationManager.GPS_PROVIDER
                    );

            if (location == null) {

                location =
                        locationManager.getLastKnownLocation(
                                LocationManager.NETWORK_PROVIDER
                        );
            }

            if (location == null) {

                updateStatus(
                        "⚠️ Location મળી નથી.\n" +
                        "GPS ચાલુ કરો અને ફરી try કરો."
                );

                return;
            }

            societyLat = location.getLatitude();
            societyLon = location.getLongitude();

            societySet = true;

            getSharedPreferences(
                    "app",
                    MODE_PRIVATE
            )
                    .edit()
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

    // ============================================================
    // EXISTING START FUNCTIONALITY
    // ============================================================

    private void startApp() {

        if (!societySet) {

            updateStatus(
                    "⚠️ પહેલા Location Set કરો"
            );

            return;
        }

        Intent intent =
                new Intent(
                        this,
                        LocationService.class
                );

        intent.putExtra(
                "role",
                providerRole
                        ? "MILKMAN"
                        : "HOME"
        );

        try {

            if (Build.VERSION.SDK_INT >= 26) {

                startForegroundService(intent);

            } else {

                startService(intent);
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

            updateGlobalStatus(
                    providerRole
                            ? "🥛 GPS Monitoring ચાલુ..."
                            : "🏠 Monitoring ચાલુ..."
            );

        } catch (Exception e) {

            updateStatus(
                    "⚠️ Service start થઈ શક્યું નથી.\n" +
                    e.getClass().getSimpleName()
            );
        }
    }

    // ============================================================
    // EXISTING STOP FUNCTIONALITY
    // ============================================================

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

        updateGlobalStatus(
                "⚪ STOPPED"
        );
    }

    // ============================================================
    // EXISTING PERMISSIONS
    // ============================================================

    private void requestPermissionsIfNeeded() {

        ArrayList<String> permissions =
                new ArrayList<>();

        if (Build.VERSION.SDK_INT >= 23) {

            if (checkSelfPermission(
                    Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED) {

                permissions.add(
                        Manifest.permission.ACCESS_FINE_LOCATION
                );
            }

            if (checkSelfPermission(
                    Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED) {

                permissions.add(
                        Manifest.permission.ACCESS_COARSE_LOCATION
                );
            }
        }

        if (Build.VERSION.SDK_INT >= 33) {

            if (checkSelfPermission(
                    Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {

                permissions.add(
                        Manifest.permission.POST_NOTIFICATIONS
                );
            }
        }

        if (!permissions.isEmpty()) {

            requestPermissions(
                    permissions.toArray(
                            new String[0]
                    ),
                    REQ
            );
        }
    }

    // ============================================================
    // LOAD SAVED LOCATION
    // ============================================================

    private void load() {

        SharedPreferences preferences =
                getSharedPreferences(
                        "app",
                        MODE_PRIVATE
                );

        societySet =
                preferences.getBoolean(
                        "set",
                        false
                );

        societyLat =
                Double.longBitsToDouble(
                        preferences.getLong(
                                "lat",
                                0
                        )
                );

        societyLon =
                Double.longBitsToDouble(
                        preferences.getLong(
                                "lon",
                                0
                        )
                );
    }

    // ============================================================
    // SCREEN CONTAINER
    // ============================================================

    private LinearLayout createContent() {

        LinearLayout content =
                new LinearLayout(this);

        content.setOrientation(
                LinearLayout.VERTICAL
        );

        content.setPadding(
                dp(18),
                dp(20),
                dp(18),
                dp(20)
        );

        content.setBackgroundColor(
                BACKGROUND
        );

        return content;
    }

    private void setScreen(
            LinearLayout content
    ) {

        ScrollView scroll =
                new ScrollView(this);

        scroll.setFillViewport(true);

        scroll.setBackgroundColor(
                BACKGROUND
        );

        scroll.addView(
                content,
                new ScrollView.LayoutParams(
                        -1,
                        -2
                )
        );

        setContentView(scroll);
    }

    // ============================================================
    // HEADER
    // ============================================================

    private void addHeader(
            LinearLayout parent,
            String title,
            String subtitle
    ) {

        TextView titleView =
                makeText(
                        title,
                        30,
                        TEXT,
                        Gravity.LEFT
                );

        bold(titleView);

        parent.addView(
                titleView,
                match()
        );

        TextView subtitleView =
                makeText(
                        subtitle,
                        15,
                        SECONDARY_TEXT,
                        Gravity.LEFT
                );

        addTopMargin(
                parent,
                subtitleView,
                5
        );
    }

    // ============================================================
    // ROLE CARD
    // ============================================================

    private LinearLayout roleCard(
            String icon,
            String title,
            String description
    ) {

        LinearLayout card =
                card();

        TextView iconView =
                makeText(
                        icon,
                        42,
                        TEXT,
                        Gravity.CENTER
                );

        card.addView(
                iconView,
                match()
        );

        TextView titleView =
                makeText(
                        title,
                        22,
                        TEXT,
                        Gravity.CENTER
                );

        bold(titleView);

        addTopMargin(
                card,
                titleView,
                8
        );

        TextView descriptionView =
                makeText(
                        description,
                        14,
                        SECONDARY_TEXT,
                        Gravity.CENTER
                );

        addTopMargin(
                card,
                descriptionView,
                5
        );

        return card;
    }

    // ============================================================
    // CARD
    // ============================================================

    private LinearLayout card() {

        LinearLayout layout =
                new LinearLayout(this);

        layout.setOrientation(
                LinearLayout.VERTICAL
        );

        layout.setPadding(
                dp(18),
                dp(18),
                dp(18),
                dp(18)
        );

        GradientDrawable background =
                new GradientDrawable();

        background.setColor(CARD);

        background.setCornerRadius(
                dp(20)
        );

        layout.setBackground(
                background
        );

        layout.setElevation(
                dp(2)
        );

        return layout;
    }

    // ============================================================
    // PRIMARY BUTTON
    // ============================================================

    private Button primaryButton(
            String title
    ) {

        Button button =
                new Button(this);

        button.setText(title);

        button.setTextSize(16);

        button.setTextColor(
                Color.WHITE
        );

        button.setAllCaps(false);

        button.setMinHeight(
                dp(52)
        );

        GradientDrawable background =
                new GradientDrawable();

        background.setColor(
                PRIMARY
        );

        background.setCornerRadius(
                dp(16)
        );

        button.setBackground(
                background
        );

        return button;
    }

    // ============================================================
    // SECONDARY BUTTON
    // ============================================================

    private Button secondaryButton(
            String title
    ) {

        Button button =
                new Button(this);

        button.setText(title);

        button.setTextSize(15);

        button.setTextColor(
                TEXT
        );

        button.setAllCaps(false);

        button.setMinHeight(
                dp(50)
        );

        GradientDrawable background =
                new GradientDrawable();

        background.setColor(
                SECONDARY
        );

        background.setCornerRadius(
                dp(15)
        );

        button.setBackground(
                background
        );

        return button;
    }

    // ============================================================
    // DANGER / STOP BUTTON
    // ============================================================

    private Button dangerButton(
            String title
    ) {

        Button button =
                new Button(this);

        button.setText(title);

        button.setTextSize(15);

        button.setTextColor(
                TEXT
        );

        button.setAllCaps(false);

        button.setMinHeight(
                dp(50)
        );

        GradientDrawable background =
                new GradientDrawable();

        background.setColor(
                Color.rgb(
                        235,
                        242,
                        247
                )
        );

        background.setCornerRadius(
                dp(15)
        );

        button.setBackground(
                background
        );

        return button;
    }

    // ============================================================
    // TEXT
    // ============================================================

    private TextView makeText(
            String value,
            float size,
            int color,
            int gravity
    ) {

        TextView text =
                new TextView(this);

        text.setText(value);

        text.setTextSize(size);

        text.setTextColor(color);

        text.setGravity(gravity);

        text.setPadding(
                dp(2),
                dp(2),
                dp(2),
                dp(2)
        );

        return text;
    }

    // ============================================================
    // BOTTOM NAVIGATION
    // ============================================================

    private void addBottomNavigation(
            LinearLayout parent,
            boolean provider
    ) {

        addSpace(
                parent,
                12
        );

        LinearLayout nav =
                new LinearLayout(this);

        nav.setOrientation(
                LinearLayout.HORIZONTAL
        );

        nav.setGravity(
                Gravity.CENTER
        );

        nav.setPadding(
                dp(5),
                dp(6),
                dp(5),
                dp(6)
        );

        GradientDrawable background =
                new GradientDrawable();

        background.setColor(
                Color.WHITE
        );

        background.setCornerRadius(
                dp(18)
        );

        nav.setBackground(
                background
        );

        String[] items;

        if (provider) {

            items = new String[]{
                    "⌂\nHome",
                    "◉\nActivity",
                    "🔔\nAlerts",
                    "●\nProfile"
            };

        } else {

            items = new String[]{
                    "⌂\nHome",
                    "📍\nNearby",
                    "🔔\nAlerts",
                    "●\nProfile"
            };
        }

        for (String item : items) {

            TextView itemView =
                    makeText(
                            item,
                            12,
                            TEXT,
                            Gravity.CENTER
                    );

            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(
                            0,
                            dp(58),
                            1
                    );

            nav.addView(
                    itemView,
                    params
            );
        }

        LinearLayout.LayoutParams navParams =
                match();

        navParams.setMargins(
                dp(2),
                dp(4),
                dp(2),
                dp(4)
        );

        parent.addView(
                nav,
                navParams
        );
    }

    // ============================================================
    // UI HELPERS
    // ============================================================

    private LinearLayout.LayoutParams match() {

        return new LinearLayout.LayoutParams(
                -1,
                -2
        );
    }

    private void addTopMargin(
            LinearLayout parent,
            View view,
            int margin
    ) {

        LinearLayout.LayoutParams params =
                match();

        params.topMargin =
                dp(margin);

        parent.addView(
                view,
                params
        );
    }

    private void addSpace(
            LinearLayout parent,
            int height
    ) {

        Space space =
                new Space(this);

        parent.addView(
                space,
                new LinearLayout.LayoutParams(
                        1,
                        dp(height)
                )
        );
    }

    private void bold(
            TextView view
    ) {

        view.setTypeface(
                null,
                Typeface.BOLD
        );
    }

    private int dp(
            int value
    ) {

        return (int) (
                value *
                        getResources()
                                .getDisplayMetrics()
                                .density
        );
    }

    // ============================================================
    // STATUS HELPERS
    // ============================================================

    private void updateStatus(
            String message
    ) {

        if (serviceStatus != null) {

            serviceStatus.setText(
                    message
            );
        }

        if (globalStatus != null) {

            globalStatus.setText(
                    message
            );
        }
    }

    private void updateGlobalStatus(
            String message
    ) {

        if (globalStatus != null) {

            globalStatus.setText(
                    message
            );
        }
    }
}
