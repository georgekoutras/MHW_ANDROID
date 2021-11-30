package gr.openit.smarthealthwatch.util;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Looper;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.error.AuthFailureError;
import com.android.volley.error.VolleyError;
import com.android.volley.request.StringRequest;
import com.garmin.health.Device;
import com.garmin.health.DeviceManager;
import com.garmin.health.customlog.LoggingResult;
import com.garmin.health.customlog.LoggingSyncListener;
import com.garmin.health.database.dtos.HeartRateLog;
import com.garmin.health.database.dtos.PulseOxLog;
import com.garmin.health.database.dtos.StressLog;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;

import gr.openit.smarthealthwatch.MainActivity;
import gr.openit.smarthealthwatch.ui.sync.LoggingFragment;

public class Alarm extends BroadcastReceiver
{
    final public static String ONE_TIME = "onetime";
    private String deviceAddress;
    private FragmentActivity fa;

    @Override
    public void onReceive(Context context, Intent intent)
    {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "gh:wakelock");
        wl.acquire();

        // Put here YOUR code.
        //Toast.makeText(context, "Alarm !!!!!!!!!!", Toast.LENGTH_LONG).show(); // For example
        //You can do the processing here.
        Bundle extras = intent.getExtras();
        StringBuilder msgStr = new StringBuilder();

        if(extras != null && extras.getBoolean(ONE_TIME, Boolean.FALSE)){
            //Make sure this intent has been sent by the one-time timer button.
            msgStr.append("One time Timer : ");
        }
        String device_address = extras.getString("device_address");
        int current_interval = extras.getInt("current_interval");

        DeviceManager deviceManager = DeviceManager.getDeviceManager();
        Device device = deviceManager.getDevice(device_address);

        Format formatter = new SimpleDateFormat("hh:mm:ss a");
        msgStr.append(formatter.format(new Date()));

        if(device != null){
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            if((prefs.contains("syncing") && !prefs.getBoolean("syncing",false)) || !prefs.contains("syncing")) {
                getDeviceData(context, device, current_interval);
            }

        }

        Intent i = new Intent(context, Alarm.class);
        AlarmManager am =( AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+(1000*current_interval), pi); // 1000 * Second * Minute

        wl.release();
    }

    public void setAlarm(Context context, String dAddress, int current_interval)
    {
        this.setDeviceAddress(dAddress);

        AlarmManager am =(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(context, Alarm.class);
        i.putExtra("device_address", dAddress);
        i.putExtra("current_interval",current_interval);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
        //Toast.makeText(context, "Setting alarm with Interval: "+i.getExtras().getInt("current_interval"), Toast.LENGTH_LONG).show();
        setPreference(context,"syncing",false);

        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+(1000*current_interval), pi); // 1000 * Second * Minute*/
    }

    public void cancelAlarm(Context context)
    {
        //Toast.makeText(context, "Stopping Alarm", Toast.LENGTH_LONG).show();

        Intent intent = new Intent(context, Alarm.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent,  PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(sender);
        sender.cancel();

    }

    private void setDeviceAddress(String  address){
        this.deviceAddress = address;
    }

    public void setFragmentActivity(FragmentActivity main){
        this.fa = main;
    }

    private void getDeviceData(Context context, Device device, int current_interval){
        if(device != null) {
            (device).downloadLoggedData(new LoggingSyncListener() {
                @Override
                public void onSyncProgress(Device device, int progress) {
                    Log.i("syncProgress",""+progress);
                }

                @Override
                public void onSyncStarted(Device device) {
                    //Toast.makeText(context, "Logging Sync Started", Toast.LENGTH_SHORT).show();
                    setPreference(context,"syncing",true);
                }

                @Override
                public void onSyncComplete(Device device) {
                    //Toast.makeText(context, "Logging Sync Complete", Toast.LENGTH_SHORT).show();
                    setPreference(context,"syncing",false);
                    checkAvailableData(device.address(),   context, current_interval);
                }

                @Override
                public void onSyncFailed(Device device, Exception e) {
                    /*Toast.makeText(context, String.format("Logging Sync Failed... [%s]",
                            e == null ? null : e.getMessage()), Toast.LENGTH_SHORT).show();*/
                    Log.i("garminSyncFailed",String.format("Logging Sync Failed... [%s]",
                            e == null ? null : e.getMessage()));
                    setPreference(context,"syncing",false);

                }
            });
        }else{
            //Toast.makeText(context, "Device is null", Toast.LENGTH_SHORT).show();
            Log.i("garminSyncDevice","device is null");
        }
    }

    private void setPreference(Context context, String pref, Boolean val){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putBoolean(pref, val).apply();
    }

    private void checkAvailableData(String address, Context context, int current_interval)
    {
        long mEndTime = Calendar.getInstance().getTimeInMillis()/1000;
        long mStartTime = mEndTime - current_interval - 5;

        if(mEndTime - mStartTime <= 0 || mEndTime - mStartTime > 86400)
        {
            Toast.makeText(context, "Mέγιστος ρυθμός ανανέωσης: 24 ώρες", Toast.LENGTH_SHORT).show();
        }

        DeviceManager.getDeviceManager().hasLoggedData(address, mStartTime, mEndTime, (hasData) ->
        {
            if(hasData == null)
            {
                if(Looper.myLooper() == null)
                {
                    Looper.prepare();
                }

                //Toast.makeText(context, "An error occured during communication.", Toast.LENGTH_SHORT).show();
            }
            else if(hasData)
            {
                Log.i("garminData","has data");

                DeviceManager.getDeviceManager().getLoggedDataForDevice(address, mStartTime, mEndTime, (loggingResult) -> { storeLoggedData(loggingResult,context);
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

    private void storeLoggedData(LoggingResult loggingResult, Context context){
        /*Toast.makeText(context, "HEART_X -> "+loggingResult.getHeartRateList().get(loggingResult.getHeartRateList().size()-1).getHeartRate(), Toast.LENGTH_SHORT).show()*/
        String access_token=SharedPrefManager.getInstance(context).getKeyAccessToken();

        if ( access_token == null) {
            //Toast.makeText(context, "Μη εξουσιοδοτημένος χρήστης", Toast.LENGTH_SHORT).show();
            cancelAlarm(context);
        }else{
            int user_id = (SharedPrefManager.getInstance(context).getUser().getId());

            storeHeartRate(loggingResult.getHeartRateList(),user_id, context, access_token);
            storePulseOx(loggingResult.getPulseOxList(),user_id, context, access_token);
        }

    }

    public void storeHeartRate(List<HeartRateLog> heartRateList, int userId, Context context, String access_token){
        final JSONObject body = new JSONObject();
        TimeZone tz = TimeZone.getDefault();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
        df.setTimeZone(tz);
        String nowAsISO = df.format(new Date());

        if(heartRateList.size() > 0) {
            int val = heartRateList.get(heartRateList.size()-1).getHeartRate();
            Log.i("lastHRValue",""+val);
            if(val>0) {
                try {
                    body.put("name", "Παλμοί");
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

    public void storeStress(List<StressLog> stressList, int userId , Context context, String access_token){
        final JSONObject body = new JSONObject();
        TimeZone tz = TimeZone.getDefault();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
        df.setTimeZone(tz);
        String nowAsISO = df.format(new Date());

        if(stressList.size() > 0) {
            int val = stressList.get(stressList.size()-1).getStressScore();
            if(val>0) {
                try {
                    body.put("name", "Στρες");
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

        if(pulseOxList.size() > 0) {
            int val = pulseOxList.get(pulseOxList.size()-1).getPulseOx();
            Log.i("lastPULSEOXValue",""+val);

            if(val>0) {
                try {
                    body.put("name", "Οξυγόνο");
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
}