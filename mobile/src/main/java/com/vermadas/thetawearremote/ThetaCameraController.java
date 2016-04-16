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
import android.util.Log;

import com.vermadas.thetaapi.ThetaCommand;
import com.vermadas.thetaapi.ThetaOptions;
import com.vermadas.thetaapi.ThetaState;
import com.vermadas.thetawearremotelibrary.Messages;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by adam on 11/19/15.
 */
public class ThetaCameraController implements CameraControlInterface<ThetaState>, ThetaHttpCallbackHandler
{
    public ThetaCameraController(MessageHandler screenLogMessageHandler, Context context) throws CameraException
    {
        this.messageHandler = screenLogMessageHandler;
        this._context = context;
        this.wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        this._intentFilter = new IntentFilter();
        _intentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);

        connectTheta();
        updateCheckService = Executors.newScheduledThreadPool(1);
    }

    @Override
    public void close()
    {
        closeSession();
        resetAfterCameraDisconnect();
    }

    @Override
    public void takePhoto()
    {
        try
        {
            if (_cameraMode == ThetaOptions.CaptureMode.VIDEO)
            {
                _takePhotoAfterModeSwitch = true;
                setMode(ThetaOptions.CaptureMode.IMAGE);
            }
            JSONObject request = new JSONObject();
            request.put("name", ThetaCommand.TAKE_PICTURE.getName());
            JSONObject params = new JSONObject();
            params.put("sessionId", _sessionId);
            request.put("parameters", params);
            new ThetaHttpConnector(this, "/osc/commands/execute", Messages.CONFIRMATION_PHOTO).execute(request);
            startUpdateCheckService();

        }
        catch (JSONException e)
        {
            Log.e(TAG, "JSON error creating request", e);
        }
    }

    @Override
    public void startVideoCapture()
    {
        try
        {
            JSONObject request = new JSONObject();
            request.put("name", ThetaCommand.START_CAPTURE.getName());
            JSONObject params = new JSONObject();
            params.put("sessionId", _sessionId);
            request.put("parameters", params);
            new ThetaHttpConnector(this, "/osc/commands/execute", Messages.CONFIRMATION_CAPTURE_ON).execute(request);
        }
        catch (JSONException e)
        {
            Log.e(TAG, "JSON error creating request", e);
        }
    }

    @Override
    public void stopVideoCapture()
    {
        try
        {
            JSONObject request = new JSONObject();
            request.put("name", ThetaCommand.STOP_CAPTURE.getName());
            JSONObject params = new JSONObject();
            params.put("sessionId", _sessionId);
            request.put("parameters", params);
            new ThetaHttpConnector(this, "/osc/commands/execute", Messages.CONFIRMATION_CAPTURE_OFF).execute(request);
            startUpdateCheckService();
        }
        catch (JSONException e)
        {
            Log.e(TAG, "JSON error creating request", e);
        }
    }

    @Override
    public void setMode(ThetaOptions.CaptureMode mode)
    {
        try
        {
            JSONObject request = new JSONObject();
            request.put("name", ThetaCommand.SET_OPTIONS.getName());
            JSONObject params = new JSONObject();
            params.put("sessionId", _sessionId);
            ThetaOptions options = new ThetaOptions();
            options.setCaptureMode(mode);
            params.put("options", options.getJson());
            request.put("parameters", params);
            String message = mode == ThetaOptions.CaptureMode.IMAGE ? Messages.CAMERA_MODE_PHOTO : Messages.CAMERA_MODE_VIDEO;
            new ThetaHttpConnector(this, "/osc/commands/execute", message).execute(request);
        }
        catch (JSONException e)
        {
            Log.e(TAG, "JSON error creating request", e);
        }
    }

    @Override
    public void setTimer(int timer)
    {
        try
        {
            JSONObject request = new JSONObject();
            request.put("name", ThetaCommand.SET_OPTIONS.getName());
            JSONObject params = new JSONObject();
            params.put("sessionId", _sessionId);
            ThetaOptions options = new ThetaOptions();
            options.setExposureDelay(timer);
            params.put("options", options.getJson());
            request.put("parameters", params);
            String message = timer == 0 ? Messages.CAMERA_TIMER_OFF : Messages.CAMERA_TIMER_ON;
            new ThetaHttpConnector(this, "/osc/commands/execute", message).execute(request);
        }
        catch (JSONException e)
        {
            Log.e(TAG, "JSON error creating request", e);
        }
    }

    @Override
    public void setVolume(int volume)
    {
        try
        {
            JSONObject request = new JSONObject();
            request.put("name", ThetaCommand.SET_OPTIONS.getName());
            JSONObject params = new JSONObject();
            params.put("sessionId", _sessionId);
            ThetaOptions options = new ThetaOptions();
            options.setShutterVolume(volume);
            params.put("options", options.getJson());
            request.put("parameters", params);
            String message = volume == 0 ? Messages.CAMERA_VOLUME_MUTED : Messages.CAMERA_VOLUME_ON;
            new ThetaHttpConnector(this, "/osc/commands/execute", message).execute(request);
        }
        catch (JSONException e)
        {
            Log.e(TAG, "JSON error creating request", e);
        }
    }

    @Override
    public synchronized ThetaState getState()
    {
        return _state;
    }


    @Override
    public void handleResult(JSONObject response, Exception failureException, String callbackMessage)
    {
        if (failureException != null)
        {
            Log.e(TAG, "Error processing request", failureException);
            messageHandler.writeMessageToOnScreenLog(
                    "Error processing request: " + failureException.getMessage());

            if (failureException instanceof IOException)
            {
                messageHandler.writeMessageToOnScreenLog("Camera disconnected");
                messageHandler.sendMessageToWatch(Messages.CAMERA_DISCONNECTED);
                resetAfterCameraDisconnect();
            }
        }
        if (response == null) return;

        try
        {
            switch (callbackMessage)
            {
                case Messages.CONFIRMATION_CHECK_FOR_UPDATES:

                    if (!response.has("stateFingerprint")) return;

                    if (_stateFingerprint != null && _stateFingerprint.equals(response.getString("stateFingerprint")))
                    {
                        return;
                    }
                    else
                    {
                        _stateFingerprint = null;
                        _updateState.run();
                    }
                    break;

                case Messages.CONFIRMATION_STATE:

                    messageHandler.writeMessageToOnScreenLog("Back from getting state");
                    if (!response.has("fingerprint") || !response.has("state")) return;

                    _state = new ThetaState(response.getJSONObject("state"));
                    _stateFingerprint = response.getString("fingerprint");

                    messageHandler.writeMessageToOnScreenLog("state is " + _state.getCaptureStatus().name());
                    if (_firstStateCheck)
                    {
                        _firstStateCheck = false;
                    }
                    else if (_cameraBusy && _state.getCaptureStatus() == ThetaState.CaptureStatus.IDLE)
                    {
                        _cameraBusy = false;
                        updateCheckService.shutdown();
                        messageHandler.sendMessageToWatch(Messages.CAMERA_IDLE);
                    }

                    break;

                case Messages.CAMERA_CONNECTED:

                    if (!response.has("results") || !response.getJSONObject("results").has("sessionId")) return;

                    _sessionId = response.getJSONObject("results").getString("sessionId");
                    messageHandler.sendMessageToWatch(Messages.CAMERA_CONNECTED);
                    messageHandler.writeMessageToOnScreenLog("Session created.");
                    getOptions();
                    _updateState.run();

                    break;

                case Messages.CONFIRMATION_GET_OPTIONS:

                    if (!response.has("results") || !response.getJSONObject("results").has("options")) return;

                    JSONObject options = response.getJSONObject("results").getJSONObject("options");

                    if (!options.isNull("captureMode"))
                    {
                        ThetaOptions.CaptureMode captureMode = ThetaOptions.CaptureMode.fromString(options.getString("captureMode"));
                        if (captureMode == ThetaOptions.CaptureMode.IMAGE)
                        {
                            messageHandler.sendMessageToWatch(Messages.CAMERA_MODE_PHOTO);
                        }
                        else
                        {
                            messageHandler.sendMessageToWatch(Messages.CAMERA_MODE_VIDEO);
                        }
                    }

                    if (!options.isNull("exposureDelay"))
                    {
                        int exposureDelay = options.getInt("exposureDelay");
                        if (exposureDelay == 0)
                        {
                            messageHandler.sendMessageToWatch(Messages.CAMERA_TIMER_OFF);
                        }
                        else
                        {
                            messageHandler.sendMessageToWatch(Messages.CAMERA_TIMER_ON);
                        }
                    }

                    if (!options.isNull("_shutterVolume"))
                    {
                        int shutterVolume = options.getInt("_shutterVolume");
                        if (shutterVolume == 0)
                        {
                            messageHandler.sendMessageToWatch(Messages.CAMERA_VOLUME_MUTED);
                        }
                        else
                        {
                            messageHandler.sendMessageToWatch(Messages.CAMERA_VOLUME_ON);
                        }
                    }
                    break;

                case Messages.CONFIRMATION_PHOTO:

                    messageHandler.writeMessageToOnScreenLog(
                            "Picture processing in progress");
                    break;

                case Messages.CONFIRMATION_CAPTURE_ON:

                    messageHandler.sendMessageToWatch(Messages.CONFIRMATION_CAPTURE_ON);
                    messageHandler.writeMessageToOnScreenLog(
                            "Capture in progress");
                    break;

                case Messages.CONFIRMATION_CAPTURE_OFF:

                    messageHandler.sendMessageToWatch(Messages.CONFIRMATION_CAPTURE_OFF);
                    messageHandler.writeMessageToOnScreenLog(
                            "Capture stopped");
                    break;

                case Messages.CAMERA_MODE_PHOTO:

                    if (_takePhotoAfterModeSwitch)
                    {
                        _takePhotoAfterModeSwitch = false;
                        takePhoto();
                    }
                    _cameraMode = ThetaOptions.CaptureMode.IMAGE;
                    messageHandler.sendMessageToWatch(Messages.CAMERA_MODE_PHOTO);
                    messageHandler.writeMessageToOnScreenLog(
                            "Set Options to Photo Mode");
                    break;

                case Messages.CAMERA_MODE_VIDEO:

                    _cameraMode = ThetaOptions.CaptureMode.VIDEO;
                    messageHandler.sendMessageToWatch(Messages.CAMERA_MODE_VIDEO);
                    messageHandler.writeMessageToOnScreenLog(
                            "Set Options to Video Mode");
                    break;

                case Messages.CAMERA_VOLUME_MUTED:

                    messageHandler.sendMessageToWatch(Messages.CAMERA_VOLUME_MUTED);
                    messageHandler.writeMessageToOnScreenLog(
                            "Set Options to Mute Volume");
                    break;

                case Messages.CAMERA_VOLUME_ON:

                    messageHandler.sendMessageToWatch(Messages.CAMERA_VOLUME_ON);
                    messageHandler.writeMessageToOnScreenLog(
                            "Set Options to Turn Volume On");
                    break;

                case Messages.CAMERA_TIMER_OFF:

                    messageHandler.sendMessageToWatch(Messages.CAMERA_TIMER_OFF);
                    messageHandler.writeMessageToOnScreenLog(
                            "Set Options to Turn Off Photo Timer");
                    break;

                case Messages.CAMERA_TIMER_ON:

                    messageHandler.sendMessageToWatch(Messages.CAMERA_TIMER_ON);
                    messageHandler.writeMessageToOnScreenLog(
                            "Set Options to Turn On Photo Timer");
                    break;

                case Messages.CAMERA_DISCONNECTED:

                    messageHandler.sendMessageToWatch(Messages.CAMERA_DISCONNECTED);
                    messageHandler.writeMessageToOnScreenLog(
                            "Session closed");
                    break;

            }
        }
        catch (JSONException e)
        {
            Log.e(TAG, "JSON error reading response", e);
        }

    }

    private void connectTheta() throws CameraException
    {
        if (wifiManager.isWifiEnabled())
        {
            ConnectivityManager connManager = (ConnectivityManager) _context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo wifiNetworkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

            WifiInfo currentWifiInfo = wifiManager.getConnectionInfo();
            if (wifiNetworkInfo != null && wifiNetworkInfo.isConnected() &&
                    currentWifiInfo != null && currentWifiInfo.getSSID().startsWith("\"THETAXS"))
            {
                messageHandler.writeMessageToOnScreenLog("Theta already connected to WiFi, starting session...");
                wifiConnected = true;
                startSession();
                return;
            }
            else if (currentWifiInfo != null)
            {
                wifiNetworkId = currentWifiInfo.getNetworkId();
            }
        }
        else
        {
            wifiManager.setWifiEnabled(true);
        }
        List<WifiConfiguration> wifiNetworks = wifiManager.getConfiguredNetworks();
        for (WifiConfiguration config : wifiNetworks)
        {
            if (config.SSID.startsWith("\"THETAXS"))
            {
                if ( wifiManager.disconnect() &&
                    wifiManager.enableNetwork(config.networkId, true) &&
                    wifiManager.reconnect() )
                {
                    messageHandler.writeMessageToOnScreenLog("Connecting to Theta WiFi...");
                    _context.registerReceiver(wifiConnectedReceiver, _intentFilter);
                    Timer connectTimeoutTimer = new Timer();
                    connectTimeoutTimer.schedule(new TimerTask()
                    {
                        @Override
                        public void run()
                        {
                            if (!wifiConnected)
                            {
                                Log.e(TAG, "Connection to camera WiFi timed out");
                                messageHandler.writeMessageToOnScreenLog("Connection to camera WiFi timed out");
                                messageHandler.sendMessageToWatch(Messages.CAMERA_ERROR);
                                messageHandler.sendMessageToWatch(Messages.CAMERA_DISCONNECTED);
                                _context.unregisterReceiver(wifiConnectedReceiver);
                            }
                        }

                    },_connectTimeoutMs);
                    return;
                }
            }
        }

        throw new CameraException("Unable to establish WiFi connection with Theta");
    }

    private void disconnectThetaWifi()
    {
        if (wifiManager.isWifiEnabled() &&
                wifiManager.getConnectionInfo() != null &&
                wifiManager.getConnectionInfo().getSSID().startsWith("\"THETAXS"))
        {
            wifiManager.disconnect();

            if (wifiNetworkId != -1)
            {
                wifiManager.enableNetwork(wifiNetworkId, true);
                wifiManager.reconnect();
                wifiNetworkId = -1;
            }
        }
    }
    private void startSession()
    {
        try
        {
            JSONObject request = new JSONObject();
            request.put("name", ThetaCommand.START_SESSION.getName());
            request.put("parameters", new JSONObject());

            new ThetaHttpConnector(this, "/osc/commands/execute", Messages.CAMERA_CONNECTED).execute(request);
        }
        catch (JSONException e)
        {
            Log.e(TAG, "JSON error creating request", e);
        }
    }

    private void closeSession()
    {
        try
        {
            JSONObject request = new JSONObject();
            request.put("name", ThetaCommand.CLOSE_SESSION.getName());
            JSONObject params = new JSONObject();
            params.put("sessionId", _sessionId);
            request.put("parameters", params);
            new ThetaHttpConnector(this, "/osc/commands/execute", Messages.CAMERA_DISCONNECTED).execute(request);

        } catch (JSONException e)
        {
            // no throw on close
        }
    }

    private void getOptions()
    {
        try
        {
            JSONObject request = new JSONObject();
            request.put("name", ThetaCommand.GET_OPTIONS.getName());
            JSONObject params = new JSONObject();
            params.put("sessionId", _sessionId);
            JSONArray optionNames = new JSONArray();
            optionNames.put("captureMode");
            optionNames.put("exposureDelay");
            optionNames.put("_shutterVolume");
            params.put("optionNames", optionNames);
            request.put("parameters", params);
            new ThetaHttpConnector(this, "/osc/commands/execute", Messages.CONFIRMATION_GET_OPTIONS).execute(request);
        }
        catch (JSONException e)
        {
            Log.e(TAG, "JSON error creating request", e);
        }
    }

    private void startUpdateCheckService()
    {
        _cameraBusy = true;
        if (updateCheckService.isShutdown())
        {
            updateCheckService = Executors.newScheduledThreadPool(1);
        }
        updateCheckService.scheduleAtFixedRate(_updateState, 3500, _checkForUpdatesIntervalMs, TimeUnit.MILLISECONDS);
    }

    private void resetAfterCameraDisconnect()
    {
        updateCheckService.shutdown();
        disconnectThetaWifi();
        _sessionId = null;
        _stateFingerprint = null;
        _state = null;
        _takePhotoAfterModeSwitch = false;
        _cameraBusy = false;
        _firstStateCheck = true;
        wifiConnected = false;
    }

    private final Runnable _updateState = new Runnable()
    {
        public void run()
        {
            try
            {
                if (_stateFingerprint != null)
                {
                    JSONObject request = new JSONObject();
                    request.put("stateFingerprint", _stateFingerprint);
                    new ThetaHttpConnector(getThis(), "/osc/checkForUpdates", Messages.CONFIRMATION_CHECK_FOR_UPDATES).execute(request);

                }
                else
                {
                    new ThetaHttpConnector(getThis(), "/osc/state", Messages.CONFIRMATION_STATE).execute(new JSONObject());
                }
            }
            catch (JSONException e)
            {
                // empty
            }
        }
    };

    private final BroadcastReceiver wifiConnectedReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            SupplicantState state = intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE);

            if (SupplicantState.isValidState(state) && state == SupplicantState.COMPLETED)
            {
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();

                if (wifiInfo != null && wifiInfo.getSSID().startsWith("\"THETAXS"))
                {
                    Timer delaySessionStart = new Timer();
                    delaySessionStart.schedule(new TimerTask()
                    {
                        @Override
                        public void run()
                        {
                            messageHandler.writeMessageToOnScreenLog("Connection established, starting session...");
                            startSession();
                            wifiConnected = true;
                        }
                    }, _delaySessionStartMs);

                    _context.unregisterReceiver(this);
                }
            }
        }
    };

    private ThetaCameraController getThis()
    {
        return this;
    }

    private boolean wifiConnected;
    private boolean _cameraBusy;
    private boolean _takePhotoAfterModeSwitch;
    private String _sessionId;
    private String _stateFingerprint;
    private boolean _firstStateCheck = true;
    private ThetaState _state;
    private int wifiNetworkId = -1;
    private static final String TAG = "ThetaWearRemote";
    private static final int _checkForUpdatesIntervalMs = 1500;
    private static final int _connectTimeoutMs = 12000;
    private static final int _delaySessionStartMs = 4000;
    private ScheduledExecutorService updateCheckService;
    private ThetaOptions.CaptureMode _cameraMode;
    private IntentFilter _intentFilter;
    private final MessageHandler messageHandler;
    private final Context _context;
    private final WifiManager wifiManager;
}