package com.vermadas.thetawearremote;

import com.vermadas.thetaapi.ThetaOptions;

/**
 * Created by adam on 11/19/15.
 */
public interface CameraControlInterface<CameraState>
{
    void close();

    void takePhoto();

    void startVideoCapture();

    void stopVideoCapture();

    void setMode(ThetaOptions.CaptureMode mode);

    void setTimer(int timer);

    void setVolume(int volume);

    CameraState getState();
}
