package com.vermadas.thetawearremote;

import android.os.AsyncTask;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by adam on 11/19/15.
 */
public class ThetaHttpConnector extends AsyncTask<JSONObject, Integer, ThetaHttpConnector.ConnectorPayload>
{
    public ThetaHttpConnector(ThetaHttpCallbackHandler callbackHandler, String relativeUrl, String callbackMessage)
    {
        _callbackHandler = callbackHandler;
        _relativeUrl = relativeUrl;
        _callbackMessage = callbackMessage;
    }

    @Override
    protected ConnectorPayload doInBackground(JSONObject... request)
    {
        try
        {
            URL cameraUrl = new URL(_cameraIp + _relativeUrl);
            _urlConnection = (HttpURLConnection) cameraUrl.openConnection();
            _urlConnection.setRequestProperty("Content-Type", "application/json;charset=utf-8");
            _urlConnection.setRequestProperty("Accept", "application/json");
            _urlConnection.setDoInput(true);
            _urlConnection.setRequestMethod("POST");
            _urlConnection.setDoOutput(true);

            OutputStream out = new BufferedOutputStream(_urlConnection.getOutputStream());
            String requestString = request[0].toString();
            out.write(requestString.getBytes());
            out.flush();
            out.close();

            int responseCode = _urlConnection.getResponseCode();
            boolean error = false;
            InputStream in;
            if (responseCode >= 300 && responseCode <= 500)
            {
                error = true;
                in = new BufferedInputStream(_urlConnection.getErrorStream());
            }
            else
            {
                in = new BufferedInputStream(_urlConnection.getInputStream());
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));

            StringBuilder result = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null)
            {
                result.append(line);
            }

            if (error)
            {
                return new ConnectorPayload(null, new CameraException(result.toString()));
            }
            return new ConnectorPayload(new JSONObject(result.toString()), null);
        }
        catch (IOException|JSONException e)
        {
            return new ConnectorPayload(null, e);
        }
        finally
        {
            _urlConnection.disconnect();
        }
    }

    @Override
    protected void onPostExecute(ConnectorPayload result)
    {
        _callbackHandler.handleResult(result.getJson(), result.getConnectorException(), _callbackMessage);
    }

    private String _relativeUrl;
    private static final String _cameraIp = "http://192.168.1.1:80";
    private static HttpURLConnection _urlConnection;
    private final ThetaHttpCallbackHandler _callbackHandler;
    private final String _callbackMessage;

    class ConnectorPayload
    {
        ConnectorPayload(JSONObject json, Exception connectorException)
        {
            this.json = json;
            this.connectorException = connectorException;
        }
        private final JSONObject json;
        private final Exception connectorException;

        public JSONObject getJson() { return json; }
        public Exception getConnectorException() { return connectorException; }

    }
}
