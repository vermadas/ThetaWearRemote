package com.vermadas.thetawearremote;

/**
 * Created by adam on 3/17/16.
 */
public interface MessageHandler
{
    void writeMessageToOnScreenLog(String message);

    void sendMessageToWatch(String message);
}
