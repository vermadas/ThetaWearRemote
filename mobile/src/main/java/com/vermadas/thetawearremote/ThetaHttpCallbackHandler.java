package com.vermadas.thetawearremote;

import org.json.JSONObject;

/**
 * Created by adam on 3/17/16.
 */
public interface ThetaHttpCallbackHandler
{
    void handleResult(JSONObject response, Exception failureException, String callbackMessage);
}
