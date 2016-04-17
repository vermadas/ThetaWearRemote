package com.vermadas.thetawearremote;

import android.content.Context;
import android.util.Log;

import com.vermadas.thetaapi.ThetaCommand;
import com.vermadas.thetaapi.ThetaOptions;
import com.vermadas.thetaapi.ThetaState;
import com.vermadas.thetawearremotelibrary.Messages;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

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
        _messageHandler = screenLogMessageHandler;
        _context = context;
        Runnable wifiConnectCallback = new Runnable()
        {
            @Override
            public void run() { startSession(); }
        };
        _wifiConnector = new ThetaWifiConnector(context,screenLogMessageHandler,wifiConnectCallback);
        _wifiConnector.connect();
        _updateCheckService = Executors.newScheduledThreadPool(1);
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
            _messageHandler.writeMessageToOnScreenLog(
                    "Error processing request: " + failureException.getMessage());

            if (failureException instanceof IOException)
            {
                _messageHandler.writeMessageToOnScreenLog("Camera disconnected");
                _messageHandler.sendMessageToWatch(Messages.CAMERA_DISCONNECTED);
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

                    _messageHandler.writeMessageToOnScreenLog("Back from getting state");
                    if (!response.has("fingerprint") || !response.has("state")) return;

                    _state = new ThetaState(response.getJSONObject("state"));
                    _stateFingerprint = response.getString("fingerprint");

                    _messageHandler.writeMessageToOnScreenLog("state is " + _state.getCaptureStatus().name());
                    if (_firstStateCheck)
                    {
                        _firstStateCheck = false;
                    }
                    else if (_cameraBusy && _state.getCaptureStatus() == ThetaState.CaptureStatus.IDLE)
                    {
                        _cameraBusy = false;
                        _updateCheckService.shutdown();
                        _messageHandler.sendMessageToWatch(Messages.CAMERA_IDLE);
                    }

                    break;

                case Messages.CAMERA_CONNECTED:

                    if (!response.has("results") || !response.getJSONObject("results").has("sessionId")) return;

                    _sessionId = response.getJSONObject("results").getString("sessionId");
                    _messageHandler.sendMessageToWatch(Messages.CAMERA_CONNECTED);
                    _messageHandler.writeMessageToOnScreenLog("Session created.");
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
                            _messageHandler.sendMessageToWatch(Messages.CAMERA_MODE_PHOTO);
                        }
                        else
                        {
                            _messageHandler.sendMessageToWatch(Messages.CAMERA_MODE_VIDEO);
                        }
                    }

                    if (!options.isNull("exposureDelay"))
                    {
                        int exposureDelay = options.getInt("exposureDelay");
                        if (exposureDelay == 0)
                        {
                            _messageHandler.sendMessageToWatch(Messages.CAMERA_TIMER_OFF);
                        }
                        else
                        {
                            _messageHandler.sendMessageToWatch(Messages.CAMERA_TIMER_ON);
                        }
                    }

                    if (!options.isNull("_shutterVolume"))
                    {
                        int shutterVolume = options.getInt("_shutterVolume");
                        if (shutterVolume == 0)
                        {
                            _messageHandler.sendMessageToWatch(Messages.CAMERA_VOLUME_MUTED);
                        }
                        else
                        {
                            _messageHandler.sendMessageToWatch(Messages.CAMERA_VOLUME_ON);
                        }
                    }
                    break;

                case Messages.CONFIRMATION_PHOTO:

                    _messageHandler.writeMessageToOnScreenLog(
                            "Picture processing in progress");
                    break;

                case Messages.CONFIRMATION_CAPTURE_ON:

                    _messageHandler.sendMessageToWatch(Messages.CONFIRMATION_CAPTURE_ON);
                    _messageHandler.writeMessageToOnScreenLog(
                            "Capture in progress");
                    break;

                case Messages.CONFIRMATION_CAPTURE_OFF:

                    _messageHandler.sendMessageToWatch(Messages.CONFIRMATION_CAPTURE_OFF);
                    _messageHandler.writeMessageToOnScreenLog(
                            "Capture stopped");
                    break;

                case Messages.CAMERA_MODE_PHOTO:

                    if (_takePhotoAfterModeSwitch)
                    {
                        _takePhotoAfterModeSwitch = false;
                        takePhoto();
                    }
                    _cameraMode = ThetaOptions.CaptureMode.IMAGE;
                    _messageHandler.sendMessageToWatch(Messages.CAMERA_MODE_PHOTO);
                    _messageHandler.writeMessageToOnScreenLog(
                            "Set Options to Photo Mode");
                    break;

                case Messages.CAMERA_MODE_VIDEO:

                    _cameraMode = ThetaOptions.CaptureMode.VIDEO;
                    _messageHandler.sendMessageToWatch(Messages.CAMERA_MODE_VIDEO);
                    _messageHandler.writeMessageToOnScreenLog(
                            "Set Options to Video Mode");
                    break;

                case Messages.CAMERA_VOLUME_MUTED:

                    _messageHandler.sendMessageToWatch(Messages.CAMERA_VOLUME_MUTED);
                    _messageHandler.writeMessageToOnScreenLog(
                            "Set Options to Mute Volume");
                    break;

                case Messages.CAMERA_VOLUME_ON:

                    _messageHandler.sendMessageToWatch(Messages.CAMERA_VOLUME_ON);
                    _messageHandler.writeMessageToOnScreenLog(
                            "Set Options to Turn Volume On");
                    break;

                case Messages.CAMERA_TIMER_OFF:

                    _messageHandler.sendMessageToWatch(Messages.CAMERA_TIMER_OFF);
                    _messageHandler.writeMessageToOnScreenLog(
                            "Set Options to Turn Off Photo Timer");
                    break;

                case Messages.CAMERA_TIMER_ON:

                    _messageHandler.sendMessageToWatch(Messages.CAMERA_TIMER_ON);
                    _messageHandler.writeMessageToOnScreenLog(
                            "Set Options to Turn On Photo Timer");
                    break;

                case Messages.CAMERA_DISCONNECTED:

                    _messageHandler.sendMessageToWatch(Messages.CAMERA_DISCONNECTED);
                    _messageHandler.writeMessageToOnScreenLog(
                            "Session closed");
                    break;

            }
        }
        catch (JSONException e)
        {
            Log.e(TAG, "JSON error reading response", e);
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
        if (_updateCheckService.isShutdown())
        {
            _updateCheckService = Executors.newScheduledThreadPool(1);
        }
        _updateCheckService.scheduleAtFixedRate(_updateState, 3500, _checkForUpdatesIntervalMs, TimeUnit.MILLISECONDS);
    }

    private void resetAfterCameraDisconnect()
    {
        _updateCheckService.shutdown();
        _wifiConnector.disconnect();
        _sessionId = null;
        _stateFingerprint = null;
        _state = null;
        _takePhotoAfterModeSwitch = false;
        _cameraBusy = false;
        _firstStateCheck = true;
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

    private ThetaCameraController getThis()
    {
        return this;
    }

    private static final String TAG = "ThetaWearRemote";
    private static final int _checkForUpdatesIntervalMs = 1500;

    private final MessageHandler _messageHandler;
    private final Context _context;
    private final ThetaWifiConnector _wifiConnector;

    private ScheduledExecutorService _updateCheckService;
    private ThetaOptions.CaptureMode _cameraMode;
    private boolean _cameraBusy;
    private boolean _takePhotoAfterModeSwitch;
    private String _sessionId;
    private String _stateFingerprint;
    private boolean _firstStateCheck = true;
    private ThetaState _state;
}