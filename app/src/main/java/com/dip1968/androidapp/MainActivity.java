package com.dip1968.androidapp;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.Build;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final int REQUEST_PERMISSIONS = 1001;

    private static final int PORT = 8988;

    // Society radius: 150 meters
    private static final float SOCIETY_RADIUS_METERS = 150f;

    private TextView statusText;
    private TextView gpsText;
    private TextView wifiText;

    private RadioGroup roleGroup;
    private RadioButton milkmanRadio;
    private RadioButton homeRadio;

    private Button societyButton;
    private Button startButton;
    private Button stopButton;

    private boolean isMilkman = false;
    private boolean running = false;

    private WifiP2pManager wifiManager;
    private WifiP2pManager.Channel wifiChannel;

    private BroadcastReceiver wifiReceiver;
    private IntentFilter wifiIntentFilter;

    private final List<WifiP2pDevice> peers = new ArrayList<>();

    private LocationManager locationManager;
    private Location lastLocation;

    private double societyLatitude = 0.0;
    private double societyLongitude = 0.0;
    private boolean societyLocationSet = false;

    private SharedPreferences preferences;

    private ServerSocket serverSocket;
    private Socket connectedSocket;

    private Thread serverThread;
    private Thread socketThread;

    private Handler handler = new Handler(Looper.getMainLooper());

    private boolean alreadyAlerted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        preferences = getSharedPreferences("dudh_alert", MODE_PRIVATE);

        loadSocietyLocation();

        createUI();

        initializeWifiDirect();

        initializeLocation();

        requestRequiredPermissions();
    }

    // ---------------------------------------------------------
    // UI
    // ---------------------------------------------------------

    private void createUI() {

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(30, 30, 30, 30);

        TextView title = new TextView(this);
        title.setText("🥛 દૂધવાળો Alert - V2");
        title.setTextSize(26);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 10, 0, 30);

        root.addView(title);

        TextView roleTitle = new TextView(this);
        roleTitle.setText("તમારો ફોન કયો છે?");
        roleTitle.setTextSize(18);

        root.addView(roleTitle);

        roleGroup = new RadioGroup(this);
        roleGroup.setOrientation(RadioGroup.VERTICAL);

        homeRadio = new RadioButton(this);
        homeRadio.setText("🏠 ઘર");
        homeRadio.setTextSize(18);

        milkmanRadio = new RadioButton(this);
        milkmanRadio.setText("🥛 દૂધવાળો");
        milkmanRadio.setTextSize(18);

        roleGroup.addView(homeRadio);
        roleGroup.addView(milkmanRadio);

        homeRadio.setChecked(true);

        root.addView(roleGroup);

        societyButton = new Button(this);
        societyButton.setText("📍 Society Location Set કરો");

        root.addView(societyButton);

        startButton = new Button(this);
        startButton.setText("▶️ Start");

        root.addView(startButton);

        stopButton = new Button(this);
        stopButton.setText("⏹ Stop");

        root.addView(stopButton);

        gpsText = new TextView(this);
        gpsText.setText("📍 GPS: Waiting...");
        gpsText.setTextSize(16);
        gpsText.setPadding(0, 20, 0, 10);

        root.addView(gpsText);

        wifiText = new TextView(this);
        wifiText.setText("📡 Wi-Fi Direct: Waiting...");
        wifiText.setTextSize(16);
        wifiText.setPadding(0, 10, 0, 10);

        root.addView(wifiText);

        statusText = new TextView(this);
        statusText.setText("Ready");
        statusText.setTextSize(20);
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(10, 30, 10, 30);

        root.addView(statusText);

        setContentView(root);

        roleGroup.setOnCheckedChangeListener((group, checkedId) -> {

            if (checkedId == milkmanRadio.getId()) {
                isMilkman = true;

                societyButton.setVisibility(Button.GONE);

                statusText.setText(
                        "🥛 દૂધવાળો mode\n\n" +
                        "GPS + Wi-Fi Direct માટે Ready"
                );

            } else {
                isMilkman = false;

                societyButton.setVisibility(Button.VISIBLE);

                statusText.setText(
                        "🏠 ઘર mode\n\n" +
                        "પહેલા Society Location Set કરો"
                );
            }
        });

        societyButton.setOnClickListener(v -> setSocietyLocation());

        startButton.setOnClickListener(v -> startSystem());

        stopButton.setOnClickListener(v -> stopSystem());
    }

    // ---------------------------------------------------------
    // Permissions
    // ---------------------------------------------------------

    private void requestRequiredPermissions() {

        List<String> permissions = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

            permissions.add(Manifest.permission.BLUETOOTH_CONNECT);

        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES);

        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }

        List<String> missing = new ArrayList<>();

        for (String permission : permissions) {

            if (checkSelfPermission(permission)
                    != PackageManager.PERMISSION_GRANTED) {

                missing.add(permission);
            }
        }

        if (!missing.isEmpty()) {

            requestPermissions(
                    missing.toArray(new String[0]),
                    REQUEST_PERMISSIONS
            );
        }
    }

    // ---------------------------------------------------------
    // Wi-Fi Direct
    // ---------------------------------------------------------

    private void initializeWifiDirect() {

        wifiManager =
                (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);

        if (wifiManager == null) {

            wifiText.setText("❌ Wi-Fi Direct supported નથી");

            return;
        }

        wifiChannel =
                wifiManager.initialize(
                        this,
                        getMainLooper(),
                        () -> runOnUiThread(() ->
                                wifiText.setText(
                                        "📡 Wi-Fi Direct channel lost"
                                )
                        )
                );

        wifiIntentFilter = new IntentFilter();

        wifiIntentFilter.addAction(
                WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION
        );

        wifiIntentFilter.addAction(
                WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION
        );

        wifiIntentFilter.addAction(
                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION
        );

        wifiIntentFilter.addAction(
                WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION
        );

        wifiReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {

                String action = intent.getAction();

                if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION
                        .equals(action)) {

                    int state = intent.getIntExtra(
                            WifiP2pManager.EXTRA_WIFI_STATE,
                            -1
                    );

                    if (state ==
                            WifiP2pManager.WIFI_P2P_STATE_ENABLED) {

                        wifiText.setText(
                                "📡 Wi-Fi Direct ON"
                        );

                    } else {

                        wifiText.setText(
                                "❌ Wi-Fi Direct OFF કરો"
                        );
                    }

                } else if (
                        WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION
                                .equals(action)) {

                    requestPeers();

                } else if (
                        WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION
                                .equals(action)) {

                    requestConnectionInfo();
                }
            }
        };
    }

    private void requestPeers() {

        if (wifiManager == null || wifiChannel == null) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            if (checkSelfPermission(
                    Manifest.permission.NEARBY_WIFI_DEVICES)
                    != PackageManager.PERMISSION_GRANTED) {

                return;
            }
        }

        wifiManager.requestPeers(
                wifiChannel,
                peerListListener
        );
    }

    private final WifiP2pManager.PeerListListener peerListListener =
            new WifiP2pManager.PeerListListener() {

                @Override
                public void onPeersAvailable(
                        WifiP2pDeviceList peerList) {

                    peers.clear();

                    peers.addAll(
                            peerList.getDeviceList()
                    );

                    runOnUiThread(() -> {

                        wifiText.setText(
                                "📡 Wi-Fi Direct peers: "
                                        + peers.size()
                        );

                        if (isMilkman &&
                                running &&
                                !peers.isEmpty()) {

                            connectToHome();
                        }
                    });
                }
            };

    // ---------------------------------------------------------
    // Home connection
    // ---------------------------------------------------------

    private void connectToHome() {

        if (wifiManager == null ||
                wifiChannel == null ||
                peers.isEmpty()) {

            return;
        }

        WifiP2pDevice target = peers.get(0);

        WifiP2pConfig config = new WifiP2pConfig();

        config.deviceAddress = target.deviceAddress;

        wifiText.setText(
                "📡 Home phone સાથે connect થઈ રહ્યું છે..."
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            if (checkSelfPermission(
                    Manifest.permission.NEARBY_WIFI_DEVICES)
                    != PackageManager.PERMISSION_GRANTED) {

                return;
            }
        }

        wifiManager.connect(
                wifiChannel,
                config,
                new WifiP2pManager.ActionListener() {

                    @Override
                    public void onSuccess() {

                        wifiText.setText(
                                "📡 Wi-Fi Direct connection request sent"
                        );
                    }

                    @Override
                    public void onFailure(int reason) {

                        wifiText.setText(
                                "❌ Wi-Fi Direct connect failed: "
                                        + reason
                        );
                    }
                }
        );
    }

    // ---------------------------------------------------------
    // Connection info
    // ---------------------------------------------------------

    private void requestConnectionInfo() {

        if (wifiManager == null ||
                wifiChannel == null) {

            return;
        }

        wifiManager.requestConnectionInfo(
                wifiChannel,
                info -> {

                    if (info == null ||
                            !info.groupFormed) {

                        return;
                    }

                    runOnUiThread(() -> {

                        wifiText.setText(
                                "🟢 Wi-Fi Direct connected"
                        );
                    });

                    if (info.isGroupOwner) {

                        startServer();

                    } else {

                        InetAddress ownerAddress =
                                info.groupOwnerAddress;

                        if (ownerAddress != null) {

                            connectToServer(
                                    ownerAddress.getHostAddress()
                            );
                        }
                    }
                }
        );
    }

    // ---------------------------------------------------------
    // Server - Home
    // ---------------------------------------------------------

    private void startServer() {

        if (serverThread != null &&
                serverThread.isAlive()) {

            return;
        }

        serverThread = new Thread(() -> {

            try {

                if (serverSocket != null &&
                        !serverSocket.isClosed()) {

                    serverSocket.close();
                }

                serverSocket =
                        new ServerSocket(PORT);

                runOnUiThread(() ->
                        wifiText.setText(
                                "📡 Home server ready"
                        )
                );

                while (running) {

                    Socket socket =
                            serverSocket.accept();

                    connectedSocket = socket;

                    handleIncomingSocket(socket);
                }

            } catch (Exception e) {

                runOnUiThread(() ->
                        wifiText.setText(
                                "❌ Server stopped"
                        )
                );
            }
        });

        serverThread.start();
    }

    private void handleIncomingSocket(Socket socket) {

        try {

            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    socket.getInputStream()
                            )
                    );

            String message;

            while (running &&
                    (message = reader.readLine()) != null) {

                final String received = message;

                runOnUiThread(() ->
                        handleMessage(received)
                );
            }

        } catch (Exception ignored) {

        }
    }

    // ---------------------------------------------------------
    // Client - Milkman
    // ---------------------------------------------------------

    private void connectToServer(String host) {

        if (socketThread != null &&
                socketThread.isAlive()) {

            return;
        }

        socketThread = new Thread(() -> {

            try {

                Thread.sleep(1000);

                Socket socket =
                        new Socket(
                                host,
                                PORT
                        );

                connectedSocket = socket;

                runOnUiThread(() ->
                        wifiText.setText(
                                "🟢 Home સાથે connected"
                        )
                );

                PrintWriter writer =
                        new PrintWriter(
                                new OutputStreamWriter(
                                        socket.getOutputStream()
                                ),
                                true
                        );

                sendGpsStatus(writer);

                while (running) {

                    Thread.sleep(5000);

                    sendGpsStatus(writer);
                }

            } catch (Exception e) {

                runOnUiThread(() ->
                        wifiText.setText(
                                "📡 Home connection waiting..."
                        )
                );
            }
        });

        socketThread.start();
    }

    // ---------------------------------------------------------
    // Send GPS status
    // ---------------------------------------------------------

    private void sendGpsStatus(PrintWriter writer) {

        if (lastLocation == null) {

            return;
        }

        if (!societyLocationSet) {

            runOnUiThread(() ->
                    gpsText.setText(
                            "📍 Society location set નથી"
                    )
            );

            return;
        }

        float[] distance = new float[1];

        Location.distanceBetween(
                lastLocation.getLatitude(),
                lastLocation.getLongitude(),

                societyLatitude,
                societyLongitude,

                distance
        );

        float meters = distance[0];

        boolean inside =
                meters <= SOCIETY_RADIUS_METERS;

        String message;

        if (inside) {

            message = "MILKMAN_ENTERED";

        } else {

            message = "MILKMAN_OUTSIDE";
        }

        writer.println(message);

        final float finalMeters = meters;

        runOnUiThread(() ->
                gpsText.setText(
                        String.format(
                                Locale.US,
                                "📍 Distance: %.0f m\n%s",
                                finalMeters,
                                inside
                                        ? "🟢 Societyમાં"
                                        : "🔴 Society બહાર"
                        )
                )
        );
    }

    // ---------------------------------------------------------
    // Incoming message
    // ---------------------------------------------------------

    private void handleMessage(String message) {

        if ("MILKMAN_ENTERED".equals(message)) {

            if (!alreadyAlerted) {

                alreadyAlerted = true;

                showMilkmanAlert();
            }

        } else if ("MILKMAN_OUTSIDE".equals(message)) {

            alreadyAlerted = false;

            statusText.setText(
                    "🔴 દૂધવાળો Societyની બહાર છે"
            );
        }
    }

    // -----------------------
