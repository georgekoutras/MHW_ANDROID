package gr.openit.smarthealthwatch;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.error.AuthFailureError;
import com.android.volley.error.VolleyError;
import com.android.volley.request.StringRequest;
import com.garmin.health.ConnectionState;
import com.garmin.health.Device;
import com.garmin.health.DeviceManager;
import com.garmin.health.GarminHealth;
import com.garmin.health.GarminHealthInitializationException;
import com.garmin.health.customlog.LoggingResult;
import com.garmin.health.customlog.LoggingSyncListener;
import com.garmin.health.database.dtos.HeartRateLog;
import com.garmin.health.database.dtos.PulseOxLog;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;

import gr.openit.smarthealthwatch.ui.HealthSDKManager;
import gr.openit.smarthealthwatch.ui.MoodmetricServiceReceiver;
import gr.openit.smarthealthwatch.util.SharedPrefManager;
import gr.openit.smarthealthwatch.util.URLs;
import gr.openit.smarthealthwatch.util.VolleySingleton;

public class GarminCustomService extends Service {
    private Timer timer;
    private TimerTask timerTask;
    private Integer current_interval = 10; // interval every five minutes
    private Boolean userStoppedService = false;
    private String mBluetoothDeviceAddress;
    private Boolean hr_enabled=false, pulseox_enabled=false;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O)
            startMyOwnForeground();
        else
            startForeground(1, new Notification());
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private void startMyOwnForeground()
    {
        String NOTIFICATION_CHANNEL_ID = "example.permanence.garmin";
        String channelName = "Background Garmin Service";
        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(true)
                .setContentTitle("Garmin Custom Service is running in background")
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        startForeground(2, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        if(intent != null){

            if(intent.hasExtra("stop_service")) {
                this.userStoppedService = intent.getBooleanExtra("stop_service",false);
            }
            if(intent.hasExtra("interval")) {
                this.current_interval = intent.getIntExtra("interval",10);
            }
            if(intent.hasExtra("device_address")) {
                this.mBluetoothDeviceAddress = intent.getStringExtra("device_address");
            }
            if(intent.hasExtra("hr_enabled")) {
                this.hr_enabled = intent.getBooleanExtra("hr_enabled",false);
            }
            if(intent.hasExtra("pulseox_enabled")) {
                this.pulseox_enabled = intent.getBooleanExtra("pulseox_enabled",false);
            }
            stopTimerTask();
            startTimer();

        }
        if (!GarminHealth.isInitialized()) {
            initializeService();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        stopTimerTask();

        if(!this.userStoppedService){
            Intent broadcastIntent = new Intent();
            broadcastIntent.setAction("restart_service_garmin");
            broadcastIntent.setClass(this, MoodmetricServiceReceiver.class);
            this.sendBroadcast(broadcastIntent);
        }else{
            stopTimerTask();
            DeviceManager.getDeviceManager().forget(mBluetoothDeviceAddress);
            this.userStoppedService = false;
        }
        super.onDestroy();
    }

    public void stopTimerTask() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    public void startTimer() {

        timer = new Timer();
        timerTask = new TimerTask() {
            public void run() {
                Log.i("garminService","running");
                if (!GarminHealth.isInitialized()) {
                    initializeService();
                    Log.i("healthSDK","not initialized, restarting");
                }else{
                    if(mBluetoothDeviceAddress!= null) {
                        Log.i("healthSDK", "initialized");
                        DeviceManager deviceManager = DeviceManager.getDeviceManager();
                        Device device = deviceManager.getDevice(mBluetoothDeviceAddress);
                        getDeviceData(device);
                    }else{
                        stopTimerTask();
                    }
                }
            }
        };
        timer.schedule(timerTask,
                current_interval*1000,
                current_interval*1000);

    }

    private void getDeviceData(Device device){
        boolean isLogging = device.isDownloadingLoggedData();

        if(!isLogging) {
            if (device != null && device.connectionState().equals(ConnectionState.CONNECTED)) {
                device.downloadLoggedData(new LoggingSyncListener() {
                    @Override
                    public void onSyncProgress(Device device, int progress) {
                        Log.i("syncProgress", "" + progress);
                    }

                    @Override
                    public void onSyncStarted(Device device) {
                        //Toast.makeText(context, "Logging Sync Started", Toast.LENGTH_SHORT).show();
                        //setPreference(getApplicationContext(),"syncing",true);
                        Log.i("garminSync","started");

                    }

                    @Override
                    public void onSyncComplete(Device device) {
                        //Toast.makeText(context, "Logging Sync Complete", Toast.LENGTH_SHORT).show();
                        //setPreference(getApplicationContext(),"syncing",false);
                        Log.i("garminSync", "completed");

                        checkAvailableData(device.address(), current_interval);
                    }

                    @Override
                    public void onSyncFailed(Device device, Exception e) {
                    /*Toast.makeText(context, String.format("Logging Sync Failed... [%s]",
                            e == null ? null : e.getMessage()), Toast.LENGTH_SHORT).show();*/
                        /*Log.i("garminSyncFailed", String.format("Logging Sync Failed... [%s]",
                                e == null ? null : e.getMessage()));*/
                        //setPreference(getApplicationContext(),"syncing",false);

                    }
                });
            } else {
                //Toast.makeText(context, "Device is null", Toast.LENGTH_SHORT).show();
                Log.i("garminSyncDevice", "device is null");
            }
        }
    }

    private void checkAvailableData(String address, int current_interval)
    {
        long mEndTime = Calendar.getInstance().getTimeInMillis()/1000;
        long mStartTime = mEndTime - current_interval - 3;

        if(mEndTime - mStartTime <= 0 || mEndTime - mStartTime > 86400)
        {
            //Toast.makeText(context, "Mέγιστος ρυθμός ανανέωσης: 24 ώρες", Toast.LENGTH_SHORT).show();
        }

        DeviceManager.getDeviceManager().hasLoggedData(address, mStartTime, mEndTime, (hasData) ->
        {
            if(hasData == null)
            {
                if(Looper.myLooper() == null)
                {
                    Looper.prepare();
                }

            }
            else if(hasData)
            {
                Log.i("garminData","has data");

                DeviceManager.getDeviceManager().getLoggedDataForDevice(address, mStartTime, mEndTime, (loggingResult) -> { storeLoggedData(loggingResult);
                });
            }
            else
            {
                if(Looper.myLooper() == null)
                {
                    Looper.prepare();
                }
                Log.i("garminData","no data");
            }
        });
    }

    private void storeLoggedData(LoggingResult loggingResult){
        String access_token= SharedPrefManager.getInstance(getApplicationContext()).getKeyAccessToken();

        if ( access_token == null) {
            stopTimerTask();
        }else{
            int user_id = (SharedPrefManager.getInstance(getApplicationContext()).getUser().getId());
            Log.i("garminStore",this.hr_enabled+"");
            Log.i("garminStore",this.pulseox_enabled+"");
            if(this.hr_enabled) {
                storeHeartRate(loggingResult.getHeartRateList(), user_id, getApplicationContext(), access_token);
            }
            if(this.pulseox_enabled) {
                storePulseOx(loggingResult.getPulseOxList(), user_id, getApplicationContext(), access_token);
            }
        }

    }

    public void storeHeartRate(List<HeartRateLog> heartRateList, int userId, Context context, String access_token){
        final JSONObject body = new JSONObject();
        TimeZone tz = TimeZone.getDefault();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
        df.setTimeZone(tz);
        String nowAsISO = df.format(new Date());
        //Log.i("garminDataHR",heartRateList.size()+"");

        if(heartRateList.size() > 0) {
            int val = heartRateList.get(heartRateList.size()-1).getHeartRate();
            //Log.i("lastHRValue",""+val);
            if(val>0) {
                try {
                    body.put("name", "HR");
                    body.put("value", val);
                    body.put("timeStamp", nowAsISO);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                //if everything is fine
                StringRequest stringRequest = new StringRequest(Request.Method.POST, (URLs.URL_ADD_MEASUREMENT).replace("{id}", "" + userId),
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                //progressBar.setVisibility(View.GONE);
                                //Toast.makeText(context, "add measurement response: " + response, Toast.LENGTH_SHORT).show();
                            }
                        },
                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                //Toast.makeText(context, "Error: Cannot add measurement. Please check your credentials and provided url.", Toast.LENGTH_SHORT).show();
                                if(error.networkResponse.statusCode == 401 || error.networkResponse.statusCode == 403) {
                                    stopTimerTask();
                                }
                            }
                        }


                ) {
                    @Override
                    public byte[] getBody() throws AuthFailureError {
                        return body.toString().getBytes();
                    }

                    @Override
                    public String getBodyContentType() {
                        return "application/json";
                    }

                    @Override
                    public Map<String, String> getHeaders() throws AuthFailureError {
                        Map<String, String> headers = new HashMap<>();
                        headers.put("Accept", "application/json");
                        headers.put("Authorization", "Bearer " + access_token);
                        return headers;
                    }
                };
                stringRequest.setShouldCache(false);

                VolleySingleton.getInstance(context).addToRequestQueue(stringRequest);
            }
        }
    }

    public void storePulseOx(List<PulseOxLog> pulseOxList, int userId , Context context, String access_token){
        final JSONObject body = new JSONObject();
        TimeZone tz = TimeZone.getDefault();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
        df.setTimeZone(tz);
        String nowAsISO = df.format(new Date());
        Log.i("garminDataOX",pulseOxList.size()+"");

        if(pulseOxList.size() > 0) {
            int val = pulseOxList.get(pulseOxList.size()-1).getPulseOx();
            Log.i("lastPULSEOXValue",""+val);

            if(val>0) {
                try {
                    body.put("name", "O2");
                    body.put("value", val);
                    body.put("timeStamp", nowAsISO);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                //if everything is fine
                StringRequest stringRequest = new StringRequest(Request.Method.POST, (URLs.URL_ADD_MEASUREMENT).replace("{id}", "" + userId),
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                //progressBar.setVisibility(View.GONE);
                                //Toast.makeText(context, "add measurement response: " + response, Toast.LENGTH_SHORT).show();
                            }
                        },
                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                //Toast.makeText(context, "Error: Cannot add measurement. Please check your credentials and provided url.", Toast.LENGTH_SHORT).show();
                                if(error.networkResponse.statusCode == 401 || error.networkResponse.statusCode == 403) {
                                    stopTimerTask();
                                }
                            }
                        }


                ) {
                    @Override
                    public byte[] getBody() throws AuthFailureError {
                        return body.toString().getBytes();
                    }

                    @Override
                    public String getBodyContentType() {
                        return "application/json";
                    }

                    @Override
                    public Map<String, String> getHeaders() throws AuthFailureError {
                        Map<String, String> headers = new HashMap<>();
                        headers.put("Accept", "application/json");
                        headers.put("Authorization", "Bearer " + access_token);
                        return headers;
                    }
                };
                stringRequest.setShouldCache(false);

                VolleySingleton.getInstance(context).addToRequestQueue(stringRequest);
            }
        }
    }

    private void initializeService(){

        try
        {
            Futures.addCallback(HealthSDKManager.initializeHealthSDK(this), new FutureCallback<Boolean>()
            {
                @Override
                public void onSuccess(@Nullable Boolean result)
                {
                    Log.i("HealthSDK","initialized successfully");
                }

                @Override
                public void onFailure(@NonNull Throwable t)
                {
                    Log.i("HealthSDK","initialization failed");
                    //initializeService();

                }
            }, Executors.newSingleThreadExecutor());
        }
        catch (GarminHealthInitializationException e)
        {
            Log.i("HealthSDK","initialization failed" + e);
        }

    }
}
