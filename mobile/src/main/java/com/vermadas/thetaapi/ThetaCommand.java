package com.vermadas.thetaapi;

/**
 * Created by adam on 3/17/16.
 */
public enum ThetaCommand
{
    START_SESSION ("camera.startSession"),
    UPDATE_SESSION ("camera.updateSession"),
    CLOSE_SESSION ("camera.closeSession"),
    TAKE_PICTURE ("camera.takePicture"),
    START_CAPTURE ("camera._startCapture"),
    STOP_CAPTURE ("camera._stopCapture"),
    GET_OPTIONS ("camera.getOptions"),
    SET_OPTIONS ("camera.setOptions");

    private final String name;

    public String getName() { return name; }

    ThetaCommand(String name)
    {
        this.name = name;
    }
}
