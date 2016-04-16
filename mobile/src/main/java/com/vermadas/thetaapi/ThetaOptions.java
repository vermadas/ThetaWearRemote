package com.vermadas.thetaapi;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;

/**
 * Created by adam on 3/24/16.
 */
public class ThetaOptions implements Serializable
{
    public static String REQUIRED_FIRMWARE = "1.42";
    private static SimpleDateFormat thetaDateFormat = new SimpleDateFormat("yyyy:MM:dd HH:mm:ssz");

    private Double aperture;
    private Integer _captureInterval;
    private CaptureMode captureMode;
    private Integer _captureNumber;
    private GregorianCalendar dateTimeZone;
    private Double exposureCompensation; // -2.0,-1.7,-1.3,-1.0,-0.7,-0.3,0.0,0.3,0.7,1.0,1.3,1.7,2.0
    private Integer exposureDelay;
    private ExposureProgram exposureProgram;
    private FileFormat fileFormat;
    private Filter _filter;
    private GpsInfo gpsInfo;
    private HdmiReso _HDMIreso;
    private Integer iso; // 100, 125, 160, 200, 250, 320, 400, 500, 640, 800, 1000, 1250, 1600, 0 == AUTO
    private int _latestEnabledExposureDelayTime;
    private Integer offDelay;
    private int remainingPictures;
    private long remainingSpace;
    private int _remainingVideos;
    private Double shutterSpeed;
    private Integer _shutterVolume;
    private Integer sleepDelay;
    private long totalSpace;
    private WhiteBalance whiteBalance;
    private Integer _wlanChannel;

    public Double getAperture() { return aperture; }
    public void setAperture(Double aperture) { this.aperture = aperture; }

    public Integer getCaptureInterval() { return _captureInterval; }
    public void setCaptureInterval(Integer _captureInterval) { this._captureInterval = _captureInterval; }

    public CaptureMode getCaptureMode() { return captureMode; }
    public void setCaptureMode(CaptureMode captureMode) { this.captureMode = captureMode; }

    public Integer getCaptureNumber() { return _captureNumber; }
    public void setCaptureNumber(Integer _captureNumber) { this._captureNumber = _captureNumber; }

    public GregorianCalendar getDateTimeZone() { return dateTimeZone; }
    public void setDateTimeZone(GregorianCalendar dateTimeZone) { this.dateTimeZone = dateTimeZone; }

    public Double getExposureCompensation() { return exposureCompensation; }
    public void setExposureCompensation(Double exposureCompensation) { this.exposureCompensation = exposureCompensation; }

    public Integer getExposureDelay() { return exposureDelay; }
    public void setExposureDelay(Integer exposureDelay) { this.exposureDelay = exposureDelay; }

    public ExposureProgram getExposureProgram() { return exposureProgram; }
    public void setExposureProgram(ExposureProgram exposureProgram) { this.exposureProgram = exposureProgram; }

    public FileFormat getFileFormat() { return fileFormat; }
    public void setFileFormat(FileFormat fileFormat) { this.fileFormat = fileFormat; }

    public Filter getFilter() { return _filter; }
    public void setFilter(Filter _filter) { this._filter = _filter; }

    public GpsInfo getGpsInfo() { return gpsInfo; }
    public void setGpsInfo(GpsInfo gpsInfo) { this.gpsInfo = gpsInfo; }

    public HdmiReso getHdmiReso() { return _HDMIreso; }
    public void setHdmiReso(HdmiReso _HDMIreso) { this._HDMIreso = _HDMIreso; }

    public Integer getIso() { return iso; }
    public void setIso(Integer iso) { this.iso = iso; }

    public int getLatestEnabledExposureDelayTime() { return _latestEnabledExposureDelayTime; }

    public Integer getOffDelay() { return offDelay; }
    public void setOffDelay(Integer offDelay) { this.offDelay = offDelay; }

    public int getRemainingPictures() { return remainingPictures; }

    public long getRemainingSpace() { return remainingSpace; }

    public int getRemainingVideos() { return _remainingVideos; }

    public Double getShutterSpeed() { return shutterSpeed; }
    public void setShutterSpeed(Double shutterSpeed) { this.shutterSpeed = shutterSpeed; }

    public Integer getShutterVolume() { return _shutterVolume; }
    public void setShutterVolume(Integer _shutterVolume) { this._shutterVolume = _shutterVolume; }

    public Integer getSleepDelay() { return sleepDelay; }
    public void setSleepDelay(Integer sleepDelay) { this.sleepDelay = sleepDelay; }

    public long getTotalSpace() { return totalSpace; }

    public WhiteBalance getWhiteBalance() { return whiteBalance; }
    public void setWhiteBalance(WhiteBalance whiteBalance) { this.whiteBalance = whiteBalance; }

    public Integer getWlanChannel() { return _wlanChannel; }
    public void setWlanChannel(Integer _wlanChannel) { this._wlanChannel = _wlanChannel; }

    public ThetaOptions() { }

    public ThetaOptions(JSONObject options)
    {
        try
        {
            aperture = !options.isNull("aperture") ? options.getDouble("aperture") : null;
            _captureInterval = !options.isNull("_captureInterval") ? options.getInt("_captureInterval") : null;
            _captureNumber = !options.isNull("_captureNumber") ? options.getInt("_captureNumber") : null;
            captureMode = !options.isNull("captureMode") ? CaptureMode.fromString(options.getString("captureInterval")) : null;
            if (!options.isNull("dateTimeZone"))
            {
                dateTimeZone = new GregorianCalendar();
                try
                {
                    dateTimeZone.setTime(thetaDateFormat.parse(options.getString("dateTimeZone")));
                }
                catch (ParseException e)
                {
                    dateTimeZone = null;
                }
            }
            exposureCompensation = !options.isNull("exposureCompensation") ? options.getDouble("exposureCompensation") : null;
            exposureDelay = !options.isNull("exposureDelay") ? options.getInt("exposureDelay") : null;
            exposureProgram = !options.isNull("exposureProgram") ? ExposureProgram.fromValue(options.getInt("exposureProgram")) : null;
            fileFormat = !options.isNull("fileFormat") ? FileFormat.fromJson(options.getJSONObject("fileFormat")) : null;
            _filter = !options.isNull("_filter") ? Filter.fromString(options.getString("_filter")) : null;
            gpsInfo = !options.isNull("gpsInfo") ? new GpsInfo(options.getJSONObject("gpsInfo")) : null;
            _HDMIreso = !options.isNull("_HDMIreso") ? HdmiReso.fromString("_HDMIreso") : null;
            iso = !options.isNull("iso") ? options.getInt("iso") : null;
            _latestEnabledExposureDelayTime = !options.isNull("_latestEnabledExposureDelayTime") ?
                    options.getInt("_latestEnabledExposureDelayTime") : 1;
            offDelay = !options.isNull("offDelay") ? options.getInt("offDelay") : null;
            remainingPictures = !options.isNull("remainingPictures") ? options.getInt("remainingPictures") : 0;
            remainingSpace = !options.isNull("remainingSpace") ? options.getLong("remainingSpace") : 0L;
            _remainingVideos = !options.isNull("_remainingVideos") ? options.getInt("_remainingVideos") : 0;
            _shutterVolume = !options.isNull("_shutterVolume") ? options.getInt("_shutterVolume") : 0;
            shutterSpeed = !options.isNull("shutterSpeed") ? options.getDouble("shutterSpeed") : null;
            sleepDelay = !options.isNull("sleepDelay") ? options.getInt("sleepDelay") : null;
            totalSpace = !options.isNull("totalSpace") ? options.getLong("totalSpace") : 0L;
            whiteBalance = !options.isNull("whiteBalance") ? WhiteBalance.fromString(options.getString("whiteBalance")) : null;
            _wlanChannel = !options.isNull("_wlanChannel") ? options.getInt("_wlanChannel") : null;
        }
        catch (JSONException e)
        {
            // Do nothing
        }
    }

    public JSONObject getJson()
    {
        JSONObject options = new JSONObject();
        try {
            if (aperture != null)
            {
                options.put("aperture", aperture);
            }
            if (_captureInterval != null)
            {
                options.put("_captureInterval", _captureInterval);
            }
            if (_captureNumber != null)
            {
                options.put("_captureNumber", _captureNumber);
            }
            if (captureMode != null)
            {
                options.put("captureMode", captureMode.getValue());
            }
            if (dateTimeZone != null)
            {
                options.put("dateTimeZone", thetaDateFormat.format(dateTimeZone));
            }
            if (exposureProgram != null)
            {
                options.put("exposureProgram", exposureProgram.getValue());
            }
            if (exposureDelay != null)
            {
                options.put("exposureDelay", exposureDelay);
            }
            if (fileFormat != null)
            {
                options.put("fileFormat", fileFormat.getJson());
            }
            if (_filter != null)
            {
                options.put("_filter", _filter.getValue());
            }
            if (gpsInfo != null)
            {
                options.put("gpsInfo", gpsInfo.getJson());
            }
            if (_HDMIreso != null)
            {
                options.put("_HDMIreso", _HDMIreso.getValue());
            }
            if (iso != null)
            {
                options.put("iso", iso);
            }
            if (offDelay != null)
            {
                options.put("offDelay", offDelay);
            }
            if (shutterSpeed != null)
            {
                options.put("shutterSpeed", shutterSpeed);
            }
            if (sleepDelay != null)
            {
                options.put("sleepDelay", sleepDelay);
            }
            if (_shutterVolume != null)
            {
                options.put("_shutterVolume", _shutterVolume);
            }
            if (whiteBalance != null)
            {
                options.put("whiteBalance", whiteBalance.getValue());
            }
            if (_wlanChannel != null)
            {
                options.put("_wlanChannel", _wlanChannel);
            }

            return options;
        }
        catch (JSONException e)
        {
            return null;
        }
    }

    public enum CaptureMode
    {
        IMAGE("image"),
        VIDEO("_video"),
        LIVE_STREAMING("_liveStreaming"); // not supported in API

        private final String value;

        public String getValue() { return value; }

        CaptureMode(String value)
        {
            this.value = value;
        }

        public static CaptureMode fromString(String value)
        {
            switch(value)
            {
                case "image":
                    return IMAGE;
                case "_video":
                    return VIDEO;
                case "_liveStreaming":
                    return LIVE_STREAMING;
                default:
                    return IMAGE;
            }
        }
    }

    public enum ExposureProgram
    {
        MANUAL (1),
        NORMAL (2),
        SHUTTER_PRIORITY (4),
        ISO_PRIORITY (9);

        private int value;

        public int getValue() { return value; }

        ExposureProgram(int value)
        {
            this.value = value;
        }

        public static ExposureProgram fromValue(int value)
        {
            switch (value)
            {
                case 1:
                    return MANUAL;
                case 2:
                    return NORMAL;
                case 4:
                    return SHUTTER_PRIORITY;
                case 9:
                    return ISO_PRIORITY;
                default:
                    return NORMAL;
            }
        }
    }

    public enum FileFormat
    {
        JPEG_5376X2688("jpeg",5376,2688),
        JPEG_2048X1024("jpeg",2048,1024),
        MP4_1920X1080("mp4",1920,1080),
        MP4_1280X720("mp4",1280,720),
        UNKNOWN(null,0,0);

        private String type;
        private int width;
        private int height;

        FileFormat(String type, int width, int height)
        {
            this.type = type;
            this.width = width;
            this.height = height;
        }

        public JSONObject getJson()
        {
            try
            {
                JSONObject json = new JSONObject();
                json.put("type", this.type);
                json.put("width", this.width);
                json.put("height", this.height);
                return json;
            }
            catch (JSONException e)
            {
                return null;
            }
        }

        public static FileFormat fromJson(JSONObject fileFormat)
        {
            try
            {
                String type = fileFormat.getString("type");
                int width = fileFormat.getInt("width");
                int height = fileFormat.getInt("height");

                if (type.equals("jpeg") && width == 5376 && height == 2688)
                {
                    return JPEG_5376X2688;
                }
                else if (type.equals("jpeg") && width == 2048 && height == 1024)
                {
                    return JPEG_2048X1024;
                }
                else if (type.equals("mp4") && width == 1920 && height == 1080)
                {
                    return MP4_1920X1080;
                }
                else if (type.equals("mp4") && width == 1280 && height == 720)
                {
                    return MP4_1280X720;
                }
                return UNKNOWN;
            }
            catch (JSONException e)
            {
                return UNKNOWN;
            }
        }
    }

    public enum Filter
    {
        OFF ("off"),
        DR_COMP ("DR comp"),
        NOISE_REDUCTION ("Noise Reduction"),
        HDR ("hdr");

        private final String value;

        public String getValue() { return value; }

        Filter(String value)
        {
            this.value = value;
        }

        public static Filter fromString(String value)
        {
            switch(value)
            {
                case "off":
                    return OFF;
                case "DR comp":
                    return DR_COMP;
                case "Noise Reduction":
                    return NOISE_REDUCTION;
                case "hdr":
                    return HDR;
                default:
                    return OFF;
            }
        }
    }

    public class GpsInfo
    {
        private double lat;
        private double lng;
        private double _altitude;
        private GregorianCalendar _dateTimeZone;
        private String _datum;

        public double getLatitude() { return lat; }
        public void setLatitude(double lat) { this.lat = lat; }

        public double getLongitude() { return lng; }
        public void setLongitude(double lng) { this.lng = lng; }

        public double getAltitude() { return _altitude; }
        public void setAltitude(double _altitude) { this._altitude = _altitude; }

        public GregorianCalendar getDateTimeZone() { return _dateTimeZone; }
        public void set_dateTimeZone(GregorianCalendar _dateTimeZone) { this._dateTimeZone = _dateTimeZone; }

        public String getDatum() { return _datum; }

        public boolean isEnabled() { return (_datum != null && _datum.equals("WGS84")); }

        public GpsInfo(JSONObject gpsInfo)
        {
            try
            {
                lat = gpsInfo.has("lat") ? gpsInfo.getDouble("lat") : 65535;
                lng = gpsInfo.has("lng") ? gpsInfo.getDouble("lng") : 65535;
                _altitude = gpsInfo.has("_altitude") ? gpsInfo.getDouble("_altitude") : 0;
                if (!gpsInfo.isNull("_dateTimeZone"))
                {
                    _dateTimeZone = new GregorianCalendar();
                    try
                    {
                        _dateTimeZone.setTime(thetaDateFormat.parse(gpsInfo.getString("_dateTimeZone")));
                    }
                    catch (ParseException e)
                    {
                        _dateTimeZone = null;
                    }
                }
                _datum = !gpsInfo.isNull("_datum") ? gpsInfo.getString("_datum") : null;
            }
            catch (JSONException e)
            {
                // Do nothing
            }
        }

        public JSONObject getJson()
        {
            try
            {
                JSONObject gpsInfo = new JSONObject();
                gpsInfo.put("lat", lat);
                gpsInfo.put("lng", lng);
                gpsInfo.put("_altitude", _altitude);
                if (_dateTimeZone != null)
                {
                    gpsInfo.put("_dateTimeZone", thetaDateFormat.format(_dateTimeZone));
                }
                gpsInfo.put("_datum", _datum != null ? _datum : JSONObject.NULL);
                return gpsInfo;
            }
            catch(JSONException e)
            {
                return null;
            }

        }
    }

    public enum HdmiReso
    {
        AUTO("Auto"),
        L_1920x1080("L"),
        M_1280x720("M"),
        S_720x480("S");

        private final String value;

        public String getValue() { return value; }

        HdmiReso(String value)
        {
            this.value = value;
        }

        public static HdmiReso fromString(String value)
        {
            switch(value)
            {
                case "Auto":
                    return AUTO;
                case "L":
                    return L_1920x1080;
                case "M":
                    return M_1280x720;
                case "S":
                    return S_720x480;
                default:
                    return AUTO;
            }
        }
    }

    public enum WhiteBalance
    {
        AUTO("auto"),
        OUTDOOR("daylight"),
        SHADE("shade"),
        CLOUDY("cloudy-daylight"),
        INCANDESCENT("incandescent"),
        INCANDESCENT_WARM_WHITE("_warmWhiteFluorescent"),
        FLUORESCENT_DAYLIGHT("_dayLightFluorescent"),
        FLUORESCENT_NATURAL_WHITE("_dayWhiteFluorescent"),
        FLUORESCENT_WHITE("fluorescent"),
        FLUORESCENT_BULB("_bulbFluorescent");

        private final String value;

        public String getValue() { return value; }

        WhiteBalance(String value)
        {
            this.value = value;
        }

        public static WhiteBalance fromString(String value)
        {
            switch(value)
            {
                case "auto":
                    return AUTO;
                case "daylight":
                    return OUTDOOR;
                case "shade":
                    return SHADE;
                case "cloudy-daylight":
                    return CLOUDY;
                case "incandescent":
                    return INCANDESCENT;
                case "_warmWhiteFluorescent":
                    return INCANDESCENT_WARM_WHITE;
                case "_dayLightFluorescent":
                    return FLUORESCENT_DAYLIGHT;
                case "_dayWhiteFluorescent":
                    return FLUORESCENT_NATURAL_WHITE;
                case "fluorescent":
                    return FLUORESCENT_WHITE;
                case "_bulbFluorescent":
                    return FLUORESCENT_BULB;
                default:
                    return AUTO;
            }
        }
    }
}
