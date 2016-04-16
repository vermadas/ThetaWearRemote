package com.vermadas.thetawearremote;

/**
 * Created by adam on 11/19/15.
 */
public class CameraException extends Exception
{
    public CameraException( Exception e )
    {
        super(e);
    }

    public CameraException( String message )
    {
        super(message);
    }
}
