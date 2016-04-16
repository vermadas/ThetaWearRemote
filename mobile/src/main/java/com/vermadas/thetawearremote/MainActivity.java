package com.vermadas.thetawearremote;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.vermadas.thetaapi.ThetaOptions;
import com.vermadas.thetaapi.ThetaState;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.vermadas.thetawearremotelibrary.Messages;

public class MainActivity extends AppCompatActivity implements
        MessageApi.MessageListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, MessageHandler {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        screenLogTextView = (TextView) findViewById(R.id.logText);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onDestroy() {

        sendMessageToWatch(Messages.CAMERA_DISCONNECTED);
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent)
    {
        switch (messageEvent.getPath())
        {
            case Messages.TOGGLE_CONNECTION_STATE:

                if (isConnected)
                {
                    _cameraController.close();
                    isConnected = false;
                }
                else
                {
                    try
                    {
                        _cameraController = new ThetaCameraController(this, this);
                        isConnected = true;
                    }
                    catch (CameraException e)
                    {
                        writeMessageToOnScreenLog("Error connecting: " + e.getMessage());
                    }
                }
                break;

            case Messages.TAKE_PICTURE:

                writeMessageToOnScreenLog("Taking photo");
                _cameraController.takePhoto();

                break;

            case Messages.TOGGLE_RECORDING_STATE:

                if (isCapturing)
                {
                    writeMessageToOnScreenLog("Attempting to stop video capture");
                    _cameraController.stopVideoCapture();
                }
                else
                {
                    writeMessageToOnScreenLog("Attempting to start video capture");
                    _cameraController.startVideoCapture();
                }
                isCapturing = !isCapturing;
                break;

            case Messages.TOGGLE_SHUTTER_VOLUME:

                if (isMuted)
                {
                    writeMessageToOnScreenLog("Attempting to turn shutter volume on");
                    _cameraController.setVolume(VOLUME_LEVEL);
                }
                else
                {
                    writeMessageToOnScreenLog("Attempting to mute shutter volume");
                    _cameraController.setVolume(0);
                }
                isMuted = !isMuted;
                break;

            case Messages.TOGGLE_TIMER:

                if (isTimerOn)
                {
                    writeMessageToOnScreenLog("Attempting to turn off photo timer");
                    _cameraController.setTimer(0);
                }
                else
                {
                    writeMessageToOnScreenLog("Attempting to turn on photo timer");
                    _cameraController.setTimer(TIMER_SECONDS);
                }
                isTimerOn = !isTimerOn;
                break;

            case Messages.TOGGLE_MODE:

                if (isPhotoMode)
                {
                    writeMessageToOnScreenLog("Attempting to turn on video mode");
                    _cameraController.setMode(ThetaOptions.CaptureMode.VIDEO);
                }
                else
                {
                    writeMessageToOnScreenLog("Attempting to turn on photo mode");
                    _cameraController.setMode(ThetaOptions.CaptureMode.IMAGE);
                }
                isPhotoMode = !isPhotoMode;
                break;
        }
    }

    @Override
    public void onConnected(Bundle bundle)
    {
        Wearable.MessageApi.addListener(mGoogleApiClient, this);
    }

    @Override
    public void onConnectionSuspended(int i)
    {
        Wearable.MessageApi.removeListener(mGoogleApiClient, this);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(TAG, "Failed to connect to Google Api Client with error code "
                + connectionResult.getErrorCode());
    }

    @Override
    public void writeMessageToOnScreenLog(final String message)
    {
        if (!DEBUG) return;

        runOnUiThread(new Runnable() {
            @Override
            public void run()
            {
                SimpleDateFormat timestampFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                String timestamp = timestampFormat.format(new Date());

                screenLogTextView.append(timestamp + " " + message + "\n");
            }
        });

    }

    @Override
    public void sendMessageToWatch(final String message)
    {
        switch (message) {
            // These flags should be set correctly but let's make sure to stay in sync
            case Messages.CAMERA_CONNECTED:
                isConnected = true;
                break;
            case Messages.CAMERA_DISCONNECTED:
                isConnected = false;
                _cameraController = null;
                break;
            case Messages.CONFIRMATION_CAPTURE_OFF:
                isCapturing = false;
                break;
            case Messages.CONFIRMATION_CAPTURE_ON:
                isCapturing = true;
                break;
            case Messages.CAMERA_TIMER_OFF:
                isTimerOn = false;
                break;
            case Messages.CAMERA_TIMER_ON:
                isTimerOn = true;
                break;
            case Messages.CAMERA_VOLUME_MUTED:
                isMuted = true;
                break;
            case Messages.CAMERA_VOLUME_ON:
                isMuted = false;
                break;
            case Messages.CAMERA_MODE_PHOTO:
                isPhotoMode = true;
                break;
            case Messages.CAMERA_MODE_VIDEO:
                isPhotoMode = false;
                break;
        }

        Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).setResultCallback(
                new ResultCallback<NodeApi.GetConnectedNodesResult>() {
                    @Override
                    public void onResult(NodeApi.GetConnectedNodesResult getConnectedNodesResult) {
                        for (final Node node : getConnectedNodesResult.getNodes()) {
                            Wearable.MessageApi.sendMessage(mGoogleApiClient, node.getId(),
                                    message, new byte[0]).setResultCallback(
                                    getSendMessageResultCallback());
                        }
                    }
                });
    }

    private ResultCallback<MessageApi.SendMessageResult> getSendMessageResultCallback() {
        return new ResultCallback<MessageApi.SendMessageResult>() {
            @Override
            public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                if (!sendMessageResult.getStatus().isSuccess()) {
                    Log.e(TAG, "Failed to connect to Google Api Client with status "
                            + sendMessageResult.getStatus());
                }
            }
        };
    }

    private GoogleApiClient mGoogleApiClient;
    private CameraControlInterface<ThetaState> _cameraController;
    private static final String TAG = "ThetaWearRemote";
    private static final int VOLUME_LEVEL = 75;
    protected static final int TIMER_SECONDS = 3;

    private TextView screenLogTextView;
    private boolean isConnected;
    private boolean isCapturing;
    private boolean isTimerOn;
    private boolean isPhotoMode;
    private boolean isMuted;

    private static final boolean DEBUG = true;
}
