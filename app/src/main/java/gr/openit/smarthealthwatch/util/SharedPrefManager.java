package gr.openit.smarthealthwatch.util;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.garmin.health.DeviceManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import gr.openit.smarthealthwatch.MainActivity;

public class SharedPrefManager {

    //the constants
    private static final String SHARED_PREF_NAME = "garminLoggedInUser";
    private static final String SHARED_FIRST_OPEN = "garminFirstOpen";
    private static final String KEY_FIRSTNAME = "keyfirstname";
    private static final String KEY_LASTNAME = "KEYLASTNAME";
    private static final String KEY_EMAIL = "keyemail";
    private static final String KEY_ID = "keyid";
    private static final String KEY_MONITOR_TYPES = "keymonitortypes";
    private static final String KEY_YEAR = "keyyear";
    private static final String KEY_PHONE = "keyphone";
    private static final String KEY_ACCESS_TOKEN = "keyaccesstoken";
    private static final String KEY_FIRST_OPEN = "firstOpen";
    private static final String KEY_THRESHOLDS = "keythresholds";

    public static final String AUDIO_SERVICE_IP = "139.91.68.13";
    public static final Integer AUDIO_SERVICE_PORT = 38443;
    public static final String AUDIO_REST_PATH = "mhw-audio-service-hub-1/rest";
    public static final String AUDIO_USER_NAME = "mhw-audio-admin";
    public static final String AUDIO_PASSWORD = "-5GdYug?vn8VVr_-TryTBAuHcz$9=wM67u_%_-=Y!bn-qM7Q4NA2Eaa7dnBah_4X";
    public static final String AUDIO_CLIENT_USER_NAME = "audio-client-user";
    public static final String GLOBAL_INTERVAL = "deviceglobalinterval";
    public static final String GARMIN_DEVICE_ADDRESS = "garmindeviceaddress";
    private static SharedPrefManager mInstance;
    private static Context mCtx;

    private SharedPrefManager(Context context) {
        mCtx = context;
    }

    public static synchronized SharedPrefManager getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new SharedPrefManager(context);
        }
        return mInstance;
    }

    //method to let the user login
    //this method will store the user data in shared preferences
    public void userLogin(User user){
        //Log.i("storing ",user.getEmail());

        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(KEY_ID, user.getId());
        editor.putString(KEY_FIRSTNAME, user.getFirstName());
        editor.putString(KEY_LASTNAME, user.getLastName());
        editor.putString(KEY_EMAIL, user.getEmail());
        editor.putStringSet(KEY_MONITOR_TYPES,user.getMonitorTypes());
        editor.apply();
    }

    //this method will checker whether user is already logged in or not
    public boolean isLoggedIn() {
        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString(KEY_EMAIL, null) != null;
    }

    public boolean isFirstOpen(){
        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(SHARED_FIRST_OPEN, Context.MODE_PRIVATE);
        return sharedPreferences.getBoolean(KEY_FIRST_OPEN, false);
    }

    public void confirmFirstOpen(){
        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(SHARED_FIRST_OPEN, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_FIRST_OPEN, true);
        editor.apply();
    }

    //this method will give the logged in user
    public User getUser() {
        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        return new User(
                sharedPreferences.getInt(KEY_ID, -1),
                sharedPreferences.getString(KEY_FIRSTNAME, null),
                sharedPreferences.getString(KEY_LASTNAME, null),
                sharedPreferences.getString(KEY_EMAIL, null),
                sharedPreferences.getStringSet(KEY_MONITOR_TYPES,null),
                sharedPreferences.getInt(KEY_YEAR,-1),
                sharedPreferences.getString(KEY_PHONE,null)
        );
    }

    public void setKeyAccessToken(String access_token){
        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_ACCESS_TOKEN, access_token);
        editor.apply();
    }

    public String getKeyAccessToken(){
        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);

        return sharedPreferences.getString(KEY_ACCESS_TOKEN, null);
    }

    public void setThresholds(Set<JSONObject> thresholds){
        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_THRESHOLDS, thresholds.toString());
        editor.apply();
    }

    public String getThresholds(){
        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);

        return sharedPreferences.getString(KEY_THRESHOLDS, null);
    }


    //this method will logout the user
    public void logout() {
        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);

        if(sharedPreferences.getString(GARMIN_DEVICE_ADDRESS,null) != null) {
            //DeviceManager.getDeviceManager().forget(sharedPreferences.getString(GARMIN_DEVICE_ADDRESS, null));
            //SharedPrefManager.getInstance(mCtx).setGarminDeviceAddress(null);
        }
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
        //mCtx.startActivity(new Intent(mCtx, LoginActivity.class));
    }

    public void setGlobalInterval(Integer interval){
        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(GLOBAL_INTERVAL, interval);
        editor.apply();
    }

    public int getGlobalInterval(){
        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);

        return sharedPreferences.getInt(GLOBAL_INTERVAL,30);
    }

    public void setGarminDeviceAddress(String address){
        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(GARMIN_DEVICE_ADDRESS, address);
        editor.apply();
    }

    public String getGarminDeviceAddress(){
        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);

        return sharedPreferences.getString(GARMIN_DEVICE_ADDRESS,null);
    }
}
