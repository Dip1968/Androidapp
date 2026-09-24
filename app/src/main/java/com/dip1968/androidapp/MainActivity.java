package com.dip1968.androidapp;

import android.Manifest;
import android.app.Activity;
import android.content.*;
import android.content.pm.PackageManager;
import android.location.*;
import android.net.wifi.p2p.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import android.media.ToneGenerator;
import android.media.AudioManager;

import java.io.*;
import java.net.*;
import java.util.*;

public class MainActivity extends Activity {

    private static final int REQ = 100;
    private static final int PORT = 8988;
    private static final float RADIUS = 150f;

    private RadioGroup roleGroup;
    private TextView status;
    private Button startBtn, stopBtn, locationBtn;

    private boolean milkman = false;
    private boolean running = false;
    private boolean alerted = false;

    private WifiP2pManager wifi;
    private WifiP2pManager.Channel channel;
    private BroadcastReceiver receiver;

    private LocationManager locationManager;
    private Location lastLocation;

    private double societyLat = 0;
    private double societyLon = 0;
    private boolean societySet = false;

    private ServerSocket server;
    private Socket socket;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private final String[] perms = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        buildUI();
        loadLocation();
        initWifi();

        requestPermissionsIfNeeded();
    }

    private void buildUI() {

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(30, 40, 30, 30);

        TextView title = new TextView(this);
        title.setText("🥛 દૂધવાળો Alert V2");
        title.setTextSize(26);
        title.setGravity(Gravity.CENTER);
        box.addView(title);

        roleGroup = new RadioGroup(this);

        RadioButton home = new RadioButton(this);
        home.setText("🏠 ઘર");

        RadioButton milk = new RadioButton(this);
        milk.setText("🥛 દૂધવાળો");

        roleGroup.addView(home);
        roleGroup.addView(milk);
        home.setChecked(true);

        box.addView(roleGroup);

        locationBtn = new Button(this);
        locationBtn.setText("📍 Society Location Set કરો");
        box.addView(locationBtn);

        startBtn = new Button(this);
        startBtn.setText("▶️ Start");
        box.addView(startBtn);

        stopBtn = new Button(this);
        stopBtn.setText("⏹ Stop");
        box.addView(stopBtn);

        status = new TextView(this);
        status.setText("\n⚪ Ready");
        status.setTextSize(18);
        box.addView(status);

        setContentView(box);

        roleGroup.setOnCheckedChangeListener((g, id) -> {
            milkman = (id == milk.getId());
            status.setText("\nRole: " +
                    (milkman ? "🥛 દૂધવાળો" : "🏠 ઘર"));
        });

        locationBtn.setOnClickListener(v -> setSocietyLocation());

        startBtn.setOnClickListener(v -> startSystem());

        stopBtn.setOnClickListener(v -> stopSystem());
    }

    // ---------------------------------------------------------

    private void requestPermissionsIfNeeded() {

        ArrayList<String> list = new ArrayList<>();

        for (String p : perms) {
            if (Build.VERSION.SDK_INT >= 23 &&
                    checkSelfPermission(p) != PackageManager.PERMISSION_GRANTED) {
                list.add(p);
            }
        }

        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(
                        Manifest.permission.NEARBY_WIFI_DEVICES)
                        != PackageManager.PERMISSION_GRANTED) {

            list.add(Manifest.permission.NEARBY_WIFI_DEVICES);
        }

        if (!list.isEmpty()) {
            requestPermissions(
                    list.toArray(new String[0]), REQ);
        }
    }

    // ---------------------------------------------------------

    private void initWifi() {

        wifi = (WifiP2pManager)
                getSystemService(WIFI_P2P_SERVICE);

        if (wifi == null) {
            status.setText("❌ Wi-Fi Direct supported નથી");
            return;
        }

        channel = wifi.initialize(
                this,
                getMainLooper(),
                null
        );

        receiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context c, Intent i) {

                String action = i.getAction();

                if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION
                        .equals(action)) {

                    discoverPeers();
                }

                if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION
                        .equals(action)) {

                    connectionChanged();
                }
            }
        };
    }

    // ---------------------------------------------------------

    private void startSystem() {

        if (running) return;

        running = true;

        if (milkman) {

            status.setText(
                    "🥛 દૂધવાળો\n\n📍 GPS ચાલુ..."
            );

            startGPS();
            discoverPeers();

        } else {

            if (!societySet) {

                status.setText(
                        "⚠️ પહેલા Society Location Set કરો"
                );

                running = false;
                return;
            }

            status.setText(
                    "🏠 ઘર\n\n📡 Wi-Fi Direct waiting..."
            );

            createGroup();
        }
    }

    // ---------------------------------------------------------

    private void stopSystem() {

        running = false;
        alerted = false;

        try {
            if (locationManager != null)
                locationManager.removeUpdates(locationListener);
        } catch (Exception ignored) {}

        try {
            if (socket != null)
                socket.close();
        } catch (Exception ignored) {}

        try {
            if (server != null)
                server.close();
        } catch (Exception ignored) {}

        if (wifi != null && channel != null) {

            try {
                wifi.removeGroup(channel, null);
            } catch (Exception ignored) {}
        }

        status.setText("⚪ Stopped");
    }

    // ---------------------------------------------------------

    private void setSocietyLocation() {

        startGPS();

        if (lastLocation == null) {

            Toast.makeText(
                    this,
                    "GPS Location મળી નથી",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        societyLat = lastLocation.getLatitude();
        societyLon = lastLocation.getLongitude();
        societySet = true;

        getPreferences(0)
                .edit()
                .putBoolean("set", true)
                .putString("lat", "" + societyLat)
                .putString("lon", "" + societyLon)
                .apply();

        status.setText(
                "📍 Society Location Set!\n\n" +
                "Radius: " + (int) RADIUS + " meter"
        );
    }

    // ---------------------------------------------------------

    private void loadLocation() {

        SharedPreferences p = getPreferences(0);

        societySet = p.getBoolean("set", false);

        try {
            societyLat = Double.parseDouble(
                    p.getString("lat", "0")
            );

            societyLon = Double.parseDouble(
                    p.getString("lon", "0")
            );
        } catch (Exception ignored) {}
    }

    // ---------------------------------------------------------

    private void startGPS() {

        locationManager =
                (LocationManager)
                        getSystemService(LOCATION_SERVICE);

        try {

            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    3000,
                    5,
                    locationListener
            );

            lastLocation =
                    locationManager.getLastKnownLocation(
                            LocationManager.GPS_PROVIDER
                    );

        } catch (SecurityException e) {

            status.setText("⚠️ GPS Permission આપો");
        }
    }

    // ---------------------------------------------------------

    private final LocationListener locationListener =
            new LocationListener() {

        @Override
        public void onLocationChanged(Location l) {

            lastLocation = l;

            if (!milkman || !running || !societySet)
                return;

            float[] d = new float[1];

            Location.distanceBetween(
                    l.getLatitude(),
                    l.getLongitude(),
                    societyLat,
                    societyLon,
                    d
            );

            if (d[0] <= RADIUS) {

                sendMessage("MILKMAN_ENTERED");

                status.setText(
                        "🟢 Societyમાં આવી ગયા!\n\n" +
                        "Distance: " + (int)d[0] + "m"
                );

            } else {

                sendMessage("MILKMAN_OUTSIDE");

                status.setText(
                        "🔴 Societyની બહાર\n\n" +
                        "Distance: " + (int)d[0] + "m"
                );
            }
        }
    };

    // ---------------------------------------------------------

    private void discoverPeers() {

        if (wifi == null || channel == null) return;

        try {

            wifi.discoverPeers(
                    channel,
                    new WifiP2pManager.ActionListener() {

                        public void onSuccess() {
                            status.setText(
                                    "📡 Wi-Fi Direct શોધી રહ્યું છે..."
                            );
                        }

                        public void onFailure(int reason) {
                            status.setText(
                                    "❌ Wi-Fi discovery failed: "
                                            + reason
                            );
                        }
                    });

        } catch (SecurityException e) {

            status.setText(
                    "⚠️ Nearby Wi-Fi Permission આપો"
            );
        }
    }

    // ---------------------------------------------------------

    private void createGroup() {

        try {

            wifi.createGroup(
                    channel,
                    new WifiP2pManager.ActionListener() {

                        public void onSuccess() {

                            status.setText(
                                    "🏠 Home Ready\n\n" +
                                    "📡 Milkmanની રાહ જોઈ રહ્યા છીએ..."
                            );

                            startServer();
                        }

                        public void onFailure(int reason) {

                            status.setText(
                                    "❌ Group create failed: "
                                            + reason
                            );
                        }
                    });

        } catch (SecurityException e) {

            status.setText(
                    "⚠️ Nearby Wi-Fi Permission આપો"
            );
        }
    }

    // ---------------------------------------------------------

    private void connectionChanged() {

        if (wifi == null || channel == null) return;

        try {

            wifi.requestConnectionInfo(
                    channel,
                    info -> {

                        if (!info.groupFormed)
                            return;

                        if (info.isGroupOwner) {

                            status.setText(
                                    "🏠 Connected!\n\n" +
                                    "🥛 Milkman ready"
                            );

                            startServer();

                        } else if (info.groupOwnerAddress != null) {

                            connectToHome(
                                    info.groupOwnerAddress
                                            .getHostAddress()
                            );
                        }
                    });

        } catch (SecurityException ignored) {}
    }

    // ---------------------------------------------------------

    private void connectToHome(String ip) {

        new Thread(() -> {

            try {

                socket = new Socket(ip, PORT);

                PrintWriter out =
                        new PrintWriter(
                                socket.getOutputStream(),
                                true
                        );

                if (lastLocation != null &&
                        societySet) {

                    float[] d = new float[1];

                    Location.distanceBetween(
                            lastLocation.getLatitude(),
                            lastLocation.getLongitude(),
                            societyLat,
                            societyLon,
                            d
                    );

                    out.println(
                            d[0] <= RADIUS
                                    ? "MILKMAN_ENTERED"
                                    : "MILKMAN_OUTSIDE"
                    );
                }

            } catch (Exception e) {

                handler.post(() ->
                        status.setText(
                                "❌ Home connection failed"
                        )
                );
            }

        }).start();
    }

    // ---------------------------------------------------------

    private void startServer() {

        new Thread(() -> {

            try {

                server = new ServerSocket(PORT);

                socket = server.accept();

                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        socket.getInputStream()
                                )
                        );

                String msg;

                while (running &&
                        (msg = reader.readLine()) != null) {

                    final String message = msg;

                    handler.post(() ->
                            handleMessage(message)
                    );
                }

            } catch (Exception ignored) {}
        }).start();
    }

    // ---------------------------------------------------------

    private void sendMessage(String message) {

        if (socket == null ||
                socket.isClosed())
            return;

        new Thread(() -> {

            try {

                PrintWriter out =
                        new PrintWriter(
                                socket.getOutputStream(),
                                true
                        );

                out.println(message);

            } catch (Exception ignored) {}
        }).start();
    }

    // ---------------------------------------------------------

    private void handleMessage(String message) {

        if ("MILKMAN_ENTERED".equals(message)) {

            if (!alerted) {

                alerted = true;

                showMilkmanAlert();
            }

        } else if ("MILKMAN_OUTSIDE".equals(message)) {

            alerted = false;

            status.setText(
                    "🔴 દૂધવાળો Societyની બહાર છે"
            );
        }
    }

    // ---------------------------------------------------------

    private void showMilkmanAlert() {

        status.setText(
                "🟢 દૂધવાળો Societyમાં આવ્યો!\n\n" +
                "🔊 દૂધવાળો આવ્યો છે"
        );

        Toast.makeText(
                this,
                "🥛 દૂધવાળો આવ્યો છે!",
                Toast.LENGTH_LONG
        ).show();

        try {

            ToneGenerator tone =
                    new ToneGenerator(
                            AudioManager.STREAM_ALARM,
                            100
                    );

            tone.startTone(
                    ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD,
                    1000
            );

        } catch (Exception ignored) {}

        Vibrator v =
                (Vibrator)
                        getSystemService(VIBRATOR_SERVICE);

        if (v != null) {

            if (Build.VERSION.SDK_INT >= 26) {

                v.vibrate(
                        VibrationEffect.createWaveform(
                                new long[]{0, 300, 200, 300},
                                -1
                        )
                );

            } else {

                v.vibrate(800);
            }
        }
    }

    // ---------------------------------------------------------

    @Override
    protected void onResume() {

        super.onResume();

        if (receiver != null) {

            IntentFilter f = new IntentFilter();

            f.addAction(
                    WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION
            );

            f.addAction(
                    WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION
            );

            try {

                if (Build.VERSION.SDK_INT >= 33) {

                    registerReceiver(
                            receiver,
                            f,
                            Context.RECEIVER_NOT_EXPORTED
                    );

                } else {

                    registerReceiver(receiver, f);
                }

            } catch (Exception ignored) {}
        }
    }

    // ---------------------------------------------------------

    @Override
    protected void onPause() {

        super.onPause();

        try {

            if (receiver != null)
                unregisterReceiver(receiver);

        } catch (Exception ignored) {}
    }

    // ---------------------------------------------------------

    @Override
    protected void onDestroy() {

        stopSystem();

        super.onDestroy();
    }
}
