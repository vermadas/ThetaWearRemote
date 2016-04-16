package com.vermadas.thetawearremote;

import android.app.Activity;
import android.os.Bundle;
import android.support.wearable.view.ProgressSpinner;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.vermadas.thetawearremotelibrary.Messages;

import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity implements GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks,
        CapabilityApi.CapabilityListener,
        MessageApi.MessageListener
{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
 //       setContentView(R.layout.activity_main);
        setContentView(R.layout.rect_activity_main);
//        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);

//        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
//            @Override
//            public void onLayoutInflated(WatchViewStub stub) {
//              thetaRemoteView = (TextView) stub.findViewById(R.id.text);
//                initUiVars();
//            }
//        });

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addOnConnectionFailedListener(this)
                .addConnectionCallbacks(this)
                .build();

        initUiVars();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
        }
//        startMobileApp();
    }

    @Override
    protected void onPause() {
        if (mGoogleApiClient.isConnected()) {
            Wearable.CapabilityApi.removeCapabilityListener(mGoogleApiClient, this,
                    THETA_CONNECTION_CAPABILITY_NAME);
            mGoogleApiClient.disconnect();
        }
        super.onPause();
    }

    @Override
    public void onCapabilityChanged(CapabilityInfo capabilityInfo) {
        updateThetaConnectionCapability(capabilityInfo);
    }

    @Override
    public void onConnected(Bundle bundle)
    {
        setupThetaConnectionNode();
        Wearable.MessageApi.addListener(mGoogleApiClient, this);
    }

    @Override
    public void onConnectionSuspended(int i)
    {
        mThetaConnectionNode = null;
        Wearable.MessageApi.removeListener(mGoogleApiClient, this);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(TAG, "Failed to connect to Google Api Client");
        mThetaConnectionNode = null;
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent)
    {
        if (initRegister != null && !initRegister.isComplete())
        {
            if (initRegister.register(messageEvent.getPath()))
            {
                setBusy(false);
            }
        }
        switch (messageEvent.getPath())
        {
            case Messages.CAMERA_ERROR:
                Toast.makeText(this, "Error - see mobile app", Toast.LENGTH_SHORT).show();
                break;
            case Messages.CAMERA_CONNECTED:
                isConnected = true;
                // Before we can set is busy, all options must be set
                break;
            case Messages.CAMERA_DISCONNECTED:
                isConnected = false;
                setBusy(false);
                break;
            case Messages.CAMERA_IDLE:
                setBusy(false);
                break;
            case Messages.CAMERA_MODE_PHOTO:
                isPhotoMode = true;
                break;
            case Messages.CAMERA_MODE_VIDEO:
                isPhotoMode = false;
                break;
            case Messages.CAMERA_VOLUME_MUTED:
                isMuted = true;
                break;
            case Messages.CAMERA_VOLUME_ON:
                isMuted = false;
                break;
            case Messages.CAMERA_TIMER_ON:
                isTimerOn = true;
                break;
            case Messages.CAMERA_TIMER_OFF:
                isTimerOn = false;
                break;
        }

        refreshButtonStates();
    }
    public void toggleConnect(View view)
    {
        if (isConnected)
        {
            isConnected = false;
            initRegister.reset();
        }
        else
        {
            setBusy(true);
            initRegister = new InitializationRegister();
            Timer connectTimeout = new Timer();
            connectTimeout.schedule(new TimerTask()
            {
                @Override
                public void run()
                {
                    if (!initRegister.isComplete())
                    {
                        setBusy(false);
                        refreshButtonStates();
                        initRegister.reset();
                        if (isConnected)
                        {
                            sendMessageToMobile(Messages.TOGGLE_CONNECTION_STATE);
                            isConnected = false;
                        }
                    }
                }
            }, 20000);
        }
        refreshButtonStates();
        sendMessageToMobile(Messages.TOGGLE_CONNECTION_STATE);
    }

    public void takePhoto(View view)
    {
        setBusy(true);
        refreshButtonStates();
        sendMessageToMobile(Messages.TAKE_PICTURE);
    }

    public void toggleTimer(View view)
    {
        isTimerOn = !isTimerOn;
        startBusyTimer();
        sendMessageToMobile(Messages.TOGGLE_TIMER);
    }

    public void toggleVideo(View view)
    {
        if (isCapturing)
        {
            setBusy(true);
        }
        else
        {
            startBusyTimer();
        }
        isCapturing = !isCapturing;
        sendMessageToMobile(Messages.TOGGLE_RECORDING_STATE);
    }

    public void toggleMode(View view)
    {
        isPhotoMode = !isPhotoMode;
        startBusyTimer();
        sendMessageToMobile(Messages.TOGGLE_MODE);
    }

    public void toggleShutterVolume(View view)
    {
        isMuted = !isMuted;
        startBusyTimer();
        sendMessageToMobile(Messages.TOGGLE_SHUTTER_VOLUME);
    }

    private void refreshButtonStates()
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run() {
                connectButton.setImageResource(isConnected ? R.drawable.disconnect : R.drawable.connect);
                captureButton.setImageResource(isCapturing ? R.drawable.capture_end : R.drawable.capture_start);
                modeButton.setImageResource(isPhotoMode ? R.drawable.mode_picture : R.drawable.mode_video);
                timerButton.setImageResource(isTimerOn ? R.drawable.timer_on : R.drawable.timer_off);
                volumeButton.setImageResource(isMuted ? R.drawable.volume_mute : R.drawable.volume_on);

                connectButton.setEnabled(!isBusy && !isCapturing);
                takePhotoButton.setEnabled(isConnected && !isBusy && !isCapturing);
                captureButton.setEnabled(isConnected && !isBusy);
                modeButton.setEnabled(isConnected && !isBusy && !isCapturing);
                timerButton.setEnabled(isConnected && !isBusy && !isCapturing);
                volumeButton.setEnabled(isConnected && !isBusy && !isCapturing);
            }
        });
    }

    private void initUiVars()
    {
        connectButton = (ImageButton) findViewById(R.id.connectButton);
        takePhotoButton = (ImageButton) findViewById(R.id.takePhotoButton);
        captureButton = (ImageButton) findViewById(R.id.captureButton);
        modeButton = (ImageButton) findViewById(R.id.modeButton);
        timerButton = (ImageButton) findViewById(R.id.timerButton);
        volumeButton = (ImageButton) findViewById(R.id.volumeButton);
        progressSpinner = (ProgressSpinner) findViewById(R.id.progressSpinner);
        progressSpinner.hide();
        refreshButtonStates();
    }

    private void startBusyTimer()
    {
        isBusy = true;
        refreshButtonStates();
        Timer busyTimer = new Timer();
        busyTimer.schedule(new TimerTask()
        {
            @Override
            public void run()
            {
                isBusy = false;
                refreshButtonStates();
            }

        },1000);
    }

    private void setBusy(boolean isBusy)
    {
        this.isBusy = isBusy;
        if (isBusy)
        {
            progressSpinner.showWithAnimation();
        }
        else
        {
            progressSpinner.hide();
        }
    }
    private void startMobileApp() {
        Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).setResultCallback(
                new ResultCallback<NodeApi.GetConnectedNodesResult>() {
                    @Override
                    public void onResult(NodeApi.GetConnectedNodesResult getConnectedNodesResult) {
                        for (final Node node : getConnectedNodesResult.getNodes()) {
                            Wearable.MessageApi.sendMessage(mGoogleApiClient, node.getId(),
                                    START_ACTIVITY_PATH, new byte[0]).setResultCallback(
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

    private void sendMessageToMobile(final String path) {
        if (mThetaConnectionNode != null) {
            Wearable.MessageApi.sendMessage(mGoogleApiClient, mThetaConnectionNode.getId(),
                    path, new byte[0])
                    .setResultCallback(getSendMessageResultCallback(mThetaConnectionNode));
        } else {
            Toast.makeText(this, "No device was found", Toast.LENGTH_SHORT).show();
        }

    }

    private ResultCallback<MessageApi.SendMessageResult> getSendMessageResultCallback(
            final Node node) {
        return new ResultCallback<MessageApi.SendMessageResult>() {
            @Override
            public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                if (!sendMessageResult.getStatus().isSuccess()) {
                    Log.e(TAG, "Failed to send message with status "
                            + sendMessageResult.getStatus());
                } else {
                    Log.d(TAG, "Sent confirmation message to node " + node.getDisplayName());
                }
            }
        };
    }

    private void setupThetaConnectionNode() {
        Wearable.CapabilityApi.addCapabilityListener(
                mGoogleApiClient, this, THETA_CONNECTION_CAPABILITY_NAME);

        Wearable.CapabilityApi.getCapability(
                mGoogleApiClient, THETA_CONNECTION_CAPABILITY_NAME,
                CapabilityApi.FILTER_REACHABLE).setResultCallback(
                new ResultCallback<CapabilityApi.GetCapabilityResult>() {
                    @Override
                    public void onResult(CapabilityApi.GetCapabilityResult result) {
                        if (!result.getStatus().isSuccess()) {
                            Log.e(TAG, "setupThetaConnectionNode() Failed to get capabilities, "
                                    + "status: " + result.getStatus().getStatusMessage());
                            return;
                        }
                        updateThetaConnectionCapability(result.getCapability());
                    }
                });
    }

    private void updateThetaConnectionCapability(CapabilityInfo capabilityInfo) {
        Set<Node> connectedNodes = capabilityInfo.getNodes();
        if (connectedNodes.isEmpty()) {
            mThetaConnectionNode = null;
        } else {
            mThetaConnectionNode = pickBestNode(connectedNodes);
        }
    }

    /**
     * We pick a node that is capable of handling the confirmation. If there is more than one,
     * then we would prefer the one that is directly connected to this device. In general,
     * depending on the situation and requirements, the "best" node might be picked based on other
     * criteria.
     */
    private Node pickBestNode(Set<Node> connectedNodes) {
        Node best = null;
        if (connectedNodes != null) {
            for (Node node : connectedNodes) {
                if (node.isNearby()) {
                    return node;
                }
                best = node;
            }
        }
        return best;
    }
    private ImageButton connectButton;
    private ImageButton takePhotoButton;
    private ImageButton captureButton;
    private ImageButton modeButton;
    private ImageButton timerButton;
    private ImageButton volumeButton;

    private ProgressSpinner progressSpinner;

    private GoogleApiClient mGoogleApiClient;
//    private TextView thetaRemoteView;
    private Node mThetaConnectionNode;

    private InitializationRegister initRegister;

    private boolean isConnected;
    private boolean isBusy;
    private boolean isCapturing;
    private boolean isPhotoMode; // vs. video
    private boolean isMuted;
    private boolean isTimerOn;

    private static final String TAG = "ThetaWearRemote";
    private static final String START_ACTIVITY_PATH = "/start-activity";
    private static final String THETA_CONNECTION_CAPABILITY_NAME = "theta_connection";

    class InitializationRegister
    {
        private boolean modeSet;
        private boolean timerSet;
        private boolean volumeSet;

        protected boolean register(String message)
        {
            if (message.equals(Messages.CAMERA_MODE_PHOTO) || message.equals(Messages.CAMERA_MODE_VIDEO))
            {
                modeSet = true;
            }
            else if (message.equals(Messages.CAMERA_TIMER_OFF) || message.equals(Messages.CAMERA_TIMER_ON))
            {
                timerSet = true;
            }
            else if (message.equals(Messages.CAMERA_VOLUME_MUTED) || message.equals(Messages.CAMERA_VOLUME_ON))
            {
                volumeSet = true;
            }

            return isComplete();
        }
        protected boolean isComplete()
        {
            return modeSet && timerSet && volumeSet;
        }
        protected void reset()
        {
            modeSet = false;
            timerSet = false;
            volumeSet = false;
        }
    }
}
