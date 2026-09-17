package com.example.proximityalert;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.*;
import android.bluetooth.le.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.*;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class MainActivity extends Activity {
    static final String SERVICE_UUID = "0000fd6f-0000-1000-8000-00805f9b34fb";
    static final String MILK_ID = "MILKMAN_POC_001";
    BluetoothAdapter adapter;
    BluetoothLeAdvertiser advertiser;
    BluetoothLeScanner scanner;
    ScanCallback scanCallback;
    TextView status, nearby;
    boolean advertising=false, scanning=false;
    long lastAlert=0;
    String role="HOME";

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        buildUi();
        requestPermissionsIfNeeded();
    }

    void buildUi() {
        LinearLayout l=new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL); l.setPadding(32,40,32,32);
        TextView title=new TextView(this); title.setText("📡 Proximity Alert POC"); title.setTextSize(26); title.setTextColor(Color.BLACK);
        l.addView(title);
        TextView info=new TextView(this); info.setText("\nબે ફોન નજીક આવે ત્યારે test કરો.\nPhone A = દૂધવાળો\nPhone B = ઘર"); info.setTextSize(18); l.addView(info);
        Spinner sp=new Spinner(this);
        String[] roles={"ઘર (Receiver)","દૂધવાળો (Sender)"};
        sp.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,roles));
        sp.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
            public void onNothingSelected(android.widget.AdapterView<?> p){}
            public void onItemSelected(android.widget.AdapterView<?> p, View v,int pos,long id){ role=pos==0?"HOME":"MILK"; stopAll(); }
        });
        l.addView(sp);
        Button start=new Button(this); start.setText("START POC");
        start.setOnClickListener(v->{ if(role.equals("MILK")) startAdvertising(); else startScanning(); });
        l.addView(start);
        Button stop=new Button(this); stop.setText("STOP");
        stop.setOnClickListener(v->stopAll()); l.addView(stop);
        status=new TextView(this); status.setText("\nStatus: Ready"); status.setTextSize(18); l.addView(status);
        nearby=new TextView(this); nearby.setText("\nNearby: —"); nearby.setTextSize(22); l.addView(nearby);
        setContentView(l);
    }

    void requestPermissionsIfNeeded() {
        if(Build.VERSION.SDK_INT>=31) {
            ArrayList<String> p=new ArrayList<>();
            for(String x:new String[]{Manifest.permission.BLUETOOTH_SCAN,Manifest.permission.BLUETOOTH_ADVERTISE,Manifest.permission.BLUETOOTH_CONNECT})
                if(checkSelfPermission(x)!=PackageManager.PERMISSION_GRANTED) p.add(x);
            if(!p.isEmpty()) requestPermissions(p.toArray(new String[0]),42);
        }
        BluetoothManager bm=(BluetoothManager)getSystemService(BLUETOOTH_SERVICE);
        adapter=bm.getAdapter();
        if(adapter==null) status.setText("Bluetooth not supported");
    }

    void startAdvertising() {
        if(!adapter.isEnabled()){ status.setText("Bluetooth ON કરો."); return; }
        advertiser=adapter.getBluetoothLeAdvertiser();
        if(advertiser==null){ status.setText("BLE advertising supported નથી."); return; }
        AdvertiseSettings s=new AdvertiseSettings.Builder().setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY).setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH).setConnectable(false).build();
        ParcelUuid uuid=ParcelUuid.fromString(SERVICE_UUID);
        AdvertiseData d=new AdvertiseData.Builder().setIncludeDeviceName(false).addServiceUuid(uuid).addServiceData(uuid,MILK_ID.getBytes(StandardCharsets.UTF_8)).build();
        advertiser.startAdvertising(s,d,new AdvertiseCallback(){
            public void onStartSuccess(AdvertiseSettings settings){ advertising=true; status.setText("🟢 દૂધવાળો mode ON — phone સાથે રાખો"); }
            public void onStartFailure(int error){ status.setText("Advertising failed: "+error); }
        });
    }

    void startScanning() {
        if(!adapter.isEnabled()){ status.setText("Bluetooth ON કરો."); return; }
        scanner=adapter.getBluetoothLeScanner();
        if(scanner==null){ status.setText("BLE scanner unavailable"); return; }
        scanCallback=new ScanCallback(){
            public void onScanResult(int type, ScanResult r){
                ScanRecord rec=r.getScanRecord();
                if(rec==null) return;
                byte[] data=rec.getServiceData(ParcelUuid.fromString(SERVICE_UUID));
                if(data!=null && new String(data,StandardCharsets.UTF_8).equals(MILK_ID)) {
                    int rssi=r.getRssi();
                    nearby.setText("🟢 દૂધવાળો નજીક છે!\nRSSI: "+rssi+" dBm");
                    if(System.currentTimeMillis()-lastAlert>15000) { lastAlert=System.currentTimeMillis(); alert(); }
                }
            }
        };
        scanner.startScan(scanCallback); scanning=true; status.setText("🟢 Home receiver scanning...");
    }

    void alert() {
        android.media.ToneGenerator tg=new android.media.ToneGenerator(android.media.AudioManager.STREAM_NOTIFICATION,100);
        tg.startTone(android.media.ToneGenerator.TONE_PROP_BEEP2,700);
        Toast.makeText(this,"🔊 દૂધવાળો આવ્યો છે!",Toast.LENGTH_LONG).show();
    }

    void stopAll() {
        try { if(advertiser!=null && advertising) advertiser.stopAdvertising(new AdvertiseCallback(){}); } catch(Exception e){}
        try { if(scanner!=null && scanning) scanner.stopScan(scanCallback); } catch(Exception e){}
        advertising=false; scanning=false; if(status!=null) status.setText("Status: Stopped");
    }

    @Override protected void onDestroy(){ stopAll(); super.onDestroy(); }
}