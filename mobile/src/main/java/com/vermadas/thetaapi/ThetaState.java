package com.vermadas.thetaapi;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

/**
 * Created by adam on 11/19/15.
 */
public class ThetaState implements Serializable
{
    private String _sessionId;
    private BatteryLevel _batteryLevel;
    private boolean _storageChanged;
    private CaptureStatus _captureStatus;
    private int _recordedTime;
    private int _recordableTime;
    private String _latestFileUri;
    private ChargingStatus _batteryState;
    private JSONArray _cameraError;

    public String getSessionId() { return _sessionId; }
    public BatteryLevel getBatteryLevel() { return _batteryLevel; }
    public boolean getStorageChanged() { return _storageChanged; }
    public CaptureStatus getCaptureStatus() { return _captureStatus; }
    public int getRecordedTime() { return _recordedTime; }
    public int getRecordableTime() { return _recordableTime; }
    public String getLatestFileUri() { return _latestFileUri; }
    public ChargingStatus getBatteryState() { return _batteryState; }
    public JSONArray getCameraError() { return _cameraError; }

    public ThetaState(JSONObject stateJsonObject) throws JSONException
    {
        _sessionId = stateJsonObject.getString("sessionId");
        _batteryLevel = BatteryLevel.fromDouble(stateJsonObject.getDouble("batteryLevel"));
        _storageChanged = stateJsonObject.getBoolean("storageChanged");
        _captureStatus = CaptureStatus.fromString(stateJsonObject.getString("_captureStatus"));
        _recordedTime = stateJsonObject.getInt("_recordedTime");
        _recordableTime = stateJsonObject.getInt("_recordableTime");
        _latestFileUri = stateJsonObject.getString("_latestFileUri");
        _batteryState = ChargingStatus.fromString(stateJsonObject.getString("_batteryState"));
        if (stateJsonObject.has("_cameraError"))
        {
            _cameraError = stateJsonObject.getJSONArray("_cameraError");
        }
    }

    public enum BatteryLevel
    {
        ZERO,
        ONE_THIRD,
        TWO_THIRDS,
        FULL;

        public static BatteryLevel fromDouble(double value)
        {
            if (value == 0)
            {
                return ZERO;
            }
            else if (value == 0.33)
            {
                return ONE_THIRD;
            }
            else if (value == 0.66)
            {
                return TWO_THIRDS;
            }
            else // value == 1.00
            {
                return FULL;
            }
        }
    }

    public enum CaptureStatus
    {
        IDLE,
        SHOOTING;

        public static CaptureStatus fromString(String value)
        {
            switch (value)
            {
                case "shooting":
                    return SHOOTING;
                default: // idle
                    return IDLE;
            }
        }
    }

    public enum ChargingStatus
    {
        CHARGING,
        CHARGED,
        DISCONNECT;

        public static ChargingStatus fromString(String value)
        {
            switch (value)
            {
                case "charging":
                    return CHARGING;
                case "charged":
                    return CHARGED;
                default: // "disconnect"
                    return DISCONNECT;
            }
        }
    }
}
