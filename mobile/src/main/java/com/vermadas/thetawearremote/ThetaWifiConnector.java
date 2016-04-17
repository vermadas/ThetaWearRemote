package com.vermadas.thetawearremote;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.util.Log;

import com.vermadas.thetawearremotelibrary.Messages;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by adam on 4/17/16.
 */
public class ThetaWifiConnector
{
    public ThetaWifiConnector(Context context, MessageHandler messageHandler, Runnable connectCallback)
    {
        _context = context;
        _messageHandler = messageHandler;
        _connectCallback = connectCallback;
        _intentFilter = new IntentFilter();
        _intentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
    }

    public void connect()
    {
        WifiConnectTask connectTask = new WifiConnectTask();
        connectTask.execute();
    }

    public void disconnect()
    {
        WifiDisconnectTask disconnectTask = new WifiDisconnectTask();
        disconnectTask.execute();
    }

    private class WifiConnectTask extends AsyncTask<Void, Void, Void>
    {
        @Override
        protected Void doInBackground(Void... params)
        {
            WifiManager wifiManager = (WifiManager) _context.getSystemService(Context.WIFI_SERVICE);
            if (wifiManager.isWifiEnabled())
            {
                ConnectivityManager connManager = (ConnectivityManager) _context.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo wifiNetworkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

                WifiInfo currentWifiInfo = wifiManager.getConnectionInfo();
                if (wifiNetworkInfo != null && wifiNetworkInfo.isConnected() &&
                        currentWifiInfo != null && currentWifiInfo.getSSID().startsWith(_thetaSsidStart))
                {
                    _messageHandler.writeMessageToOnScreenLog("Theta already connected to WiFi, starting session...");
                    _wifiConnected = true;
                    _connectCallback.run();
                    return null;
                }
                else if (currentWifiInfo != null)
                {
                    _wifiNetworkId = currentWifiInfo.getNetworkId();
                }
            }
            else
            {
                wifiManager.setWifiEnabled(true);
            }
            List<WifiConfiguration> wifiNetworks = wifiManager.getConfiguredNetworks();
            for (WifiConfiguration config : wifiNetworks)
            {
                if (config.SSID.startsWith(_thetaSsidStart) &&
                        wifiManager.disconnect() &&
                        wifiManager.enableNetwork(config.networkId, true) &&
                        wifiManager.reconnect() )
                {
                    _messageHandler.writeMessageToOnScreenLog("Connecting to Theta WiFi...");
                    _context.registerReceiver(_wifiConnectedReceiver, _intentFilter);
                    Timer connectTimeoutTimer = new Timer();
                    connectTimeoutTimer.schedule(new TimerTask()
                    {
                        @Override
                        public void run()
                        {
                            if (!_wifiConnected)
                            {
                                Log.e(TAG, "Connection to camera WiFi timed out");
                                _messageHandler.writeMessageToOnScreenLog("Connection to camera WiFi timed out");
                                _messageHandler.sendMessageToWatch(Messages.CAMERA_ERROR);
                                _messageHandler.sendMessageToWatch(Messages.CAMERA_DISCONNECTED);
                                _context.unregisterReceiver(_wifiConnectedReceiver);
                            }
                        }

                    },_connectTimeoutMs);
                    return null;
                }

            }
            Log.e(TAG, "Theta WiFi configuration not found or could not be connected");
            _messageHandler.writeMessageToOnScreenLog("Theta WiFi configuration not found or could not be connected");
            _messageHandler.sendMessageToWatch(Messages.CAMERA_ERROR);
            _messageHandler.sendMessageToWatch(Messages.CAMERA_DISCONNECTED);
            return null;
        }
    }

    private class WifiDisconnectTask extends AsyncTask<Void, Void, Void>
    {
        @Override
        protected Void doInBackground(Void... params)
        {
            WifiManager wifiManager = (WifiManager) _context.getSystemService(Context.WIFI_SERVICE);

            if (wifiManager.isWifiEnabled() &&
                    wifiManager.getConnectionInfo() != null &&
                    wifiManager.getConnectionInfo().getSSID().startsWith(_thetaSsidStart))
            {
                wifiManager.disconnect();

                if (_wifiNetworkId != -1)
                {
                    wifiManager.enableNetwork(_wifiNetworkId, true);
                    wifiManager.reconnect();
                    _wifiNetworkId = -1;
                }
            }

            return null;
        }
    }
    private final BroadcastReceiver _wifiConnectedReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            SupplicantState state = intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE);

            if (SupplicantState.isValidState(state) && state == SupplicantState.COMPLETED)
            {
                WifiManager wifiManager = (WifiManager) _context.getSystemService(Context.WIFI_SERVICE);
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();

                if (wifiInfo != null && wifiInfo.getSSID().startsWith(_thetaSsidStart))
                {
                    Timer delaySessionStart = new Timer();
                    delaySessionStart.schedule(new TimerTask()
                    {
                        @Override
                        public void run()
                        {
                            _messageHandler.writeMessageToOnScreenLog("Connection established, starting session...");
                            _connectCallback.run();
                            _wifiConnected = true;
                        }
                    }, _delaySessionStartMs);

                    _context.unregisterReceiver(this);
                }
            }
        }
    };

    private static final int _connectTimeoutMs = 12000;
    private static final int _delaySessionStartMs = 4000;
    private static final String TAG = "ThetaWearRemote";
    private static final String _thetaSsidStart = "\"THETAXS";

    private final Context _context;
    private final MessageHandler _messageHandler;
    private final Runnable _connectCallback;
    private final IntentFilter _intentFilter;

    private boolean _wifiConnected;
    private int _wifiNetworkId = -1;
}
