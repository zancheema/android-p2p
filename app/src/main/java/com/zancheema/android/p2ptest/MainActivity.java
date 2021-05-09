package com.zancheema.android.p2ptest;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.net.InetAddress;

import static android.net.wifi.p2p.WifiP2pManager.BUSY;
import static android.net.wifi.p2p.WifiP2pManager.ERROR;
import static android.net.wifi.p2p.WifiP2pManager.P2P_UNSUPPORTED;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private static final int RC_ALL_PERMISSIONS = 0;
    private static final int RC_PICK_IMAGE = 101;
    private static final int RC_GET_IMAGE = 102;
    private String[] permissions = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    WifiP2pManager manager;
    Channel channel;
    BroadcastReceiver receiver;
    private IntentFilter intentFilter;

    String host;

    private Button buttonDiscover;
    private Button buttonReceive;
    private Button buttonSend;
    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!allPermissionsGranted()) {
            requestPermissions();
        } else {
            Log.d(TAG, "onCreate: all permissions granted");
            startProcessing();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == RC_ALL_PERMISSIONS) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startProcessing();
                registerReceiver(receiver, intentFilter);
            } else {
                Log.d(TAG, "onRequestPermissionsResult: Permissions not granted");
                finish();
            }
        } else if (requestCode == RC_PICK_IMAGE && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, RC_GET_IMAGE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.d(TAG, "onActivityResult: called");
        Log.d(TAG, "onActivityResult: RC_GET_IMAGE: " + (RC_GET_IMAGE == requestCode));
        Log.d(TAG, "onActivityResult: RESULT_OK: " + (RESULT_OK == resultCode));
        if (requestCode == RC_GET_IMAGE && resultCode == RESULT_OK) {
            Log.d(TAG, "onActivityResult: path: " + data.getDataString());
            new FileClientAsyncTask(this, host, 8888, data.getDataString()).execute();
        }
    }

    @SuppressLint("MissingPermission")
    private void startProcessing() {
        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);
        receiver = new WiFiDirectBroadcastReceiver(manager, channel, this);

        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        buttonDiscover = findViewById(R.id.button_discover);
        buttonReceive = findViewById(R.id.button_receive);
        buttonSend = findViewById(R.id.button_send);
        tvStatus = findViewById(R.id.tv_status);

        buttonDiscover.setOnClickListener(v -> manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Discover Peers: Success");
            }

            @Override
            public void onFailure(int reasonCode) {
                Log.d(TAG, "Discover Peers: Failure");
            }
        }));

        buttonReceive.setOnClickListener(v -> new FileServerAsyncTask(this, tvStatus).execute());

        buttonSend.setOnClickListener(v -> pickImage());
    }

    private void pickImage() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        ) {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, RC_GET_IMAGE);
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    RC_PICK_IMAGE
            );
        }
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, permissions, RC_ALL_PERMISSIONS);
    }

    private boolean allPermissionsGranted() {
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume: called");
        super.onResume();
        if (receiver != null) {
            registerReceiver(receiver, intentFilter);
        }
    }

    /* unregister the broadcast receiver */
    @Override
    protected void onPause() {
        Log.d(TAG, "onPause: called");
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy: called");
        if (receiver != null) {
            unregisterReceiver(receiver);
        }
        super.onDestroy();
    }

    private boolean connected = false;

    WifiP2pManager.ConnectionInfoListener connectionInfoListener = info -> {
        InetAddress address = info.groupOwnerAddress;
        if (address != null) {
            host = address.getHostAddress();

            if (info.groupFormed && info.isGroupOwner) {
                tvStatus.setText("Host");
            } else if (info.groupFormed) {
                tvStatus.setText("Client");
            }
        }
    };

    @SuppressLint("MissingPermission")
    WifiP2pManager.PeerListListener myPeerListListener = peers -> {
        if (connected) return;
        Log.d(TAG, "No. of peers: " + peers.getDeviceList().size());

        for (WifiP2pDevice device : peers.getDeviceList()) {
            WifiP2pConfig config = new WifiP2pConfig();
            config.deviceAddress = device.deviceAddress;
            connected = true;
            manager.connect(channel, config, new WifiP2pManager.ActionListener() {

                @Override
                public void onSuccess() {
                    Toast.makeText(MainActivity.this, "Connected to " + device.deviceName, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Connected to " + device.deviceName);
                    connected = true;
                }

                @Override
                public void onFailure(int reason) {
                    String cause;
                    switch (reason) {
                        case P2P_UNSUPPORTED:
                            cause = "Unsupported";
                            break;
                        case ERROR:
                            cause = "Error";
                            break;
                        case BUSY:
                            cause = "Busy";
                            break;
                        default:
                            cause = "Unknown";
                            break;
                    }
                    Toast.makeText(
                            MainActivity.this,
                            "Failed connecting to device: " + cause,
                            Toast.LENGTH_SHORT
                    ).show();
                }
            });
        }
    };
}