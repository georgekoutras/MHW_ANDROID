package gr.openit.smarthealthwatch;

import android.Manifest.permission;
import android.annotation.SuppressLint;

import androidx.annotation.RequiresApi;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import androidx.viewpager.widget.ViewPager;

import gr.openit.smarthealthwatch.devices.PairedDevicesDialogFragment;
import gr.openit.smarthealthwatch.devices.PairedRingDialogFragment;
import gr.openit.smarthealthwatch.ui.BaseGarminHealthActivity;
import gr.openit.smarthealthwatch.ui.BluetoothLeService;
import gr.openit.smarthealthwatch.ui.HealthSDKManager;
import gr.openit.smarthealthwatch.ui.MoodmetricServiceReceiver;
import gr.openit.smarthealthwatch.ui.UnityPlayerActivity;
import gr.openit.smarthealthwatch.util.Alarm;
import gr.openit.smarthealthwatch.util.HttpsTrustManager;
import gr.openit.smarthealthwatch.util.SharedPrefManager;
import gr.openit.smarthealthwatch.util.URLs;
import gr.openit.smarthealthwatch.util.VolleySingleton;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.error.AuthFailureError;
import com.android.volley.error.VolleyError;
import com.android.volley.request.StringRequest;
import com.garmin.health.Device;
import com.garmin.health.DeviceManager;
import com.garmin.health.DevicePairedStateListener;
import com.garmin.health.GarminHealth;
import com.garmin.health.GarminHealthInitializationException;
import com.garmin.health.customlog.DataSource;
import com.garmin.health.customlog.LoggingStatus;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.unity3d.player.UnityPlayer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;

/**
 * Copyright (c) 2017 Garmin International. All Rights Reserved.
 * <p></p>
 * This software is the confidential and proprietary information of
 * Garmin International.
 * You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement
 * you entered into with Garmin International.
 * <p></p>
 * Garmin International MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF THE
 * SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE, OR NON-INFRINGEMENT. Garmin International SHALL NOT BE LIABLE FOR ANY DAMAGES
 * SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR DISTRIBUTING
 * THIS SOFTWARE OR ITS DERIVATIVES.
 * <p></p>
 * Created by jacksoncol on 6/22/17.
 */

public class MainActivity extends UnityPlayerActivity
{
    private static final String TAG = "MainActivity";

    private static String BROADCAST_PERMISSION_SUFFIX =
            ".permission.RECEIVE_BROADCASTS";
    private static final int REQUEST_COARSE_LOCATION = 1;
    ProgressDialog pd;
    Boolean auth = true;
    private Alarm alarm;

    private static GarminHealthServiceReceiver mReceiver = new GarminHealthServiceReceiver();

    final BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.
            getDefaultAdapter();


    private static final String[] permissions = new String[] {
            Build.VERSION.SDK_INT >= 29 ? permission.ACCESS_FINE_LOCATION : permission.ACCESS_COARSE_LOCATION, // DYNAMIC LOCATION PERMISSION
            permission.WRITE_EXTERNAL_STORAGE,
            permission.READ_PHONE_STATE,
            permission.READ_CONTACTS,
            permission.ANSWER_PHONE_CALLS,
            permission.CALL_PHONE,
            permission.MEDIA_CONTENT_CONTROL,
            permission.RECORD_AUDIO};

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        HttpsTrustManager.allowAllSSL();
        MhwAudioHubClient.initSSLCertificate(getApplicationContext());

        if(savedInstanceState != null)
        {
            return;
        }
        if(!verifyPermissions())
        {
            requestPermissions(permissions, REQUEST_COARSE_LOCATION);
            requestPermissions(new String[] {getApplicationContext().getPackageName()+BROADCAST_PERMISSION_SUFFIX}, 1);
        }

        initializeApp();

        // Check that we have location permissions, required for bluetooth pairing.
        /*if (!isNetworkAvailable()) {
            enableWifi();
        }else {
            if (mBluetoothAdapter == null) {
                Toast.makeText(this, "BT: device not supported", Toast.LENGTH_LONG).show();
                this.finish();
            } else {
                if (!mBluetoothAdapter.isEnabled()) {
                    enableBT();
                }
            }
        }*/


        // Init SSL system to accept our self-signed certificate :)
        // This is not needed in final production where a valid server certificate will be used.
        // Custom certificate is at res/raw/mhwaudiohubcert.crt.

    }

    public UnityPlayer getUnityPlayer(){
        return mUnityPlayer;
    }

    private boolean checkAuthorization(){
        String primaryUserInfoUrl = URLs.URL_GET_PRIMARY_USER_INFO.replace("{id}",""+SharedPrefManager.getInstance(getApplicationContext()).getUser().getId());
        HttpsTrustManager.allowAllSSL();

        StringRequest stringRequest = new StringRequest(Request.Method.GET, primaryUserInfoUrl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        initializeService();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        //here we use it without checking statusCode. this way we logout the user even if no internet connection is available.
                        //if we want to check internet connection separately, then comment the first to lines, and uncomment the if block.
                        SharedPrefManager.getInstance(getApplicationContext()).logout();
                        userLogin();
                        /*if(error.networkResponse.statusCode == 401) {
                            SharedPrefManager.getInstance(getApplicationContext()).logout();
                            userLogin();
                        }*/
                    }
                }


        ) {
            @Override
            public String getBodyContentType() {
                return "application/json";
            }
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Accept","application/json");
                headers.put("Authorization", "Bearer " + SharedPrefManager.getInstance(getApplicationContext()).getKeyAccessToken());

                return headers;
            }
        };
        stringRequest.setShouldCache(false);

        VolleySingleton.getInstance(getApplicationContext()).addToRequestQueue(stringRequest);

        return auth;
    }
    private void initializeApp(){
        if (!SharedPrefManager.getInstance(getApplicationContext()).isFirstOpen()) {
            //Toast.makeText(getApplicationContext(), "First Open", Toast.LENGTH_SHORT).show();
            SharedPrefManager.getInstance(getApplicationContext()).confirmFirstOpen();
            Fragment ob = new OnBoardingFragment();
            FragmentManager fm = getSupportFragmentManager();
            FragmentTransaction transaction = fm.beginTransaction();
            transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
            transaction.replace(R.id.main_root,ob); // give your fragment container id in first parameter
            //transaction.addToBackStack(null);  // if written, this transaction will be added to backstack
            transaction.commit();
            //userLogin();
        }else{
            if (!SharedPrefManager.getInstance(getApplicationContext()).isLoggedIn()) {
                userLogin();
            }else{
                checkAuthorization();
            }
        }
    }
    @Override
    public void onBackPressed() {

        List<Fragment> fragmentList = getSupportFragmentManager().getFragments();
        boolean handled = false;
        for(Fragment f : fragmentList) {
            if(f instanceof FragmentSettings) {
                handled = ((FragmentSettings)f).onBackPressed();
                if(handled) {
                    break;
                }
            }
            if(f instanceof FragmentHrMeasurements) {
                handled = ((FragmentHrMeasurements)f).onBackPressed();
                if(handled) {
                    break;
                }
            }
            if(f instanceof FragmentPulseoxMeasurements) {
                handled = ((FragmentPulseoxMeasurements)f).onBackPressed();
                if(handled) {
                    break;
                }
            }
            if(f instanceof FragmentGluceMeasurements) {
                handled = ((FragmentGluceMeasurements)f).onBackPressed();
                if(handled) {
                    break;
                }
            }
            if(f instanceof FragmentPresureMeasurements) {
                handled = ((FragmentPresureMeasurements)f).onBackPressed();
                if(handled) {
                    break;
                }
            }
            if(f instanceof FragmentStressMeasurements) {
                handled = ((FragmentStressMeasurements)f).onBackPressed();
                if(handled) {
                    break;
                }
            }
            if(f instanceof FragmentShowAdvice) {
                handled = ((FragmentShowAdvice)f).onBackPressed();
                if(handled) {
                    break;
                }
            }
            if(f instanceof FragmentShowMessage) {
                handled = ((FragmentShowMessage)f).onBackPressed();
                if(handled) {
                    break;
                }
            }
            if(f instanceof FragmentShowContact) {
                handled = ((FragmentShowContact)f).onBackPressed();
                if(handled) {
                    break;
                }
            }
            if(f instanceof FragmentInvitations) {
                handled = ((FragmentInvitations)f).onBackPressed();
                if(handled) {
                    break;
                }
            }
            if(f instanceof FragmentShowInvitation) {
                handled = ((FragmentShowInvitation)f).onBackPressed();
                if(handled) {
                    break;
                }
            }
            if(f instanceof PairedRingDialogFragment) {
                handled = ((PairedRingDialogFragment)f).onBackPressed();
                if(handled) {
                    break;
                }
            }
            if(f instanceof FragmentCoughMeasurements) {
                handled = ((FragmentCoughMeasurements)f).onBackPressed();
                if(handled) {
                    break;
                }
            }
        }
        super.onBackPressed();

    }

    public void enableBT(){
        new AlertDialog.Builder(this, R.style.LogoutDialog)
                .setMessage(R.string.enable_bluetooth_ask)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(R.string.button_yes_gr, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        mBluetoothAdapter.enable();
                    }})
                .setNegativeButton(R.string.button_no_gr, null).show();
    }

    public void enableWifi(){
        new AlertDialog.Builder(this, R.style.LogoutDialog)
                .setMessage(R.string.enable_wifi_ask)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(R.string.button_yes_gr, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                    }})
                .setNegativeButton(R.string.button_no_gr,  new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        finish();
                    }}).show();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network nw = connectivityManager.getActiveNetwork();
            if (nw == null) return false;
            NetworkCapabilities actNw = connectivityManager.getNetworkCapabilities(nw);
            return actNw != null && (actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) || actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
        } else {
            NetworkInfo nwInfo = connectivityManager.getActiveNetworkInfo();
            return nwInfo != null && nwInfo.isConnected();
        }
    }
    private void userHomeTransition(){
        Fragment homeFragment = new UserHome(this,false);
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
        transaction.replace(R.id.main_root,homeFragment); // give your fragment container id in first parameter
        transaction.commit();
    }

    private void initializeService(){

        try
        {
            Futures.addCallback(HealthSDKManager.initializeHealthSDK(this), new FutureCallback<Boolean>()
            {
                @Override
                public void onSuccess(@Nullable Boolean result)
                {
                    //findViewById(R.id.loadingPanel).setVisibility(View.GONE);
                    runOnUiThread(() -> {
                        Toast.makeText(getApplicationContext(), "SDK Initialized Successfully", Toast.LENGTH_LONG).show();
                        alarm = new Alarm();
                        checkAlarmUp();
                        userHomeTransition();

                        //connectedDevicesTransition();
                    });

                }

                @Override
                public void onFailure(@NonNull Throwable t)
                {
                    //pd.hide();
                    //Log.e(TAG, "Garmin Health initialization failed.", t);
                    runOnUiThread(() ->
                    {

                        Toast.makeText(getApplicationContext(), R.string.initialization_failed, Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Garmin Health initialization failed.", t);

                        finishAndRemoveTask();
                    });
                }
            }, Executors.newSingleThreadExecutor());
        }
        catch (GarminHealthInitializationException e)
        {

            Toast.makeText(getApplicationContext(), R.string.initialization_failed, Toast.LENGTH_LONG).show();
            Log.e(TAG, "Garmin Health initialization failed.", e);
            finishAndRemoveTask();
        }

    }

    private void checkAlarmUp(){
        boolean alarmUp = (PendingIntent.getBroadcast(this, 0,
                new Intent(this,Alarm.class),
                PendingIntent.FLAG_NO_CREATE) != null);


        if (alarmUp)
        {
            Toast.makeText(this, "Alarm is active!", Toast.LENGTH_SHORT).show();
            //alarm.cancelAlarm(this);
        }else{

            Toast.makeText(this, "Alarm is Inactive!", Toast.LENGTH_SHORT).show();
            if(alarm != null && SharedPrefManager.getInstance(this).getGarminDeviceAddress() !=null){
                try {
                    alarm.setAlarm(this,SharedPrefManager.getInstance(this).getGarminDeviceAddress(),SharedPrefManager.getInstance(this).getGlobalInterval());
                }catch (Exception e){
                    Log.i("alarmError",""+e.getMessage());
                }

            }
        }
    }

    public void userLogin() {
        Fragment loginFragment = new LoginFragment(this);
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
        transaction.replace(R.id.main_root,loginFragment); // give your fragment container id in first parameter
        transaction.commit();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        switch(requestCode)
        {
            case REQUEST_COARSE_LOCATION:

                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    // Verify that the location services are available.
                    if(!verifyLocationServices())
                    {
                        Toast.makeText(getApplicationContext(), R.string.loc_service_unavailable, Toast.LENGTH_LONG).show();

                        finishAndRemoveTask();
                    }
                }

                break;
        }
    }

    /**
     * Checks if the location permissions are enabled or not.
     *
     * @return true if permissions are available.
     */
    private boolean verifyPermissions()
    {
        boolean buildCondition = Build.VERSION.SDK_INT < Build.VERSION_CODES.M;
        boolean permissionsCondition = true;

        for(String permission : permissions)
        {
            permissionsCondition &= (checkCallingOrSelfPermission(permission) == PackageManager.PERMISSION_GRANTED);
        }

        permissionsCondition &= (checkCallingOrSelfPermission(getApplicationContext().getPackageName()+BROADCAST_PERMISSION_SUFFIX) == PackageManager.PERMISSION_GRANTED);

        return buildCondition || permissionsCondition;

    }

    /**
     * Checks if the location services are enabled or not.
     *
     * @return true if services are available.
     */
    private boolean verifyLocationServices()
    {
        LocationManager locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);

        return locationManager != null && (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
    }


    public static class SetupListener implements DevicePairedStateListener
    {
        private final Context mAppContext;

        SetupListener(Context appContext)
        {
            this.mAppContext = appContext.getApplicationContext();
        }

        @Override
        public void onDeviceSetupComplete(@NonNull Device device)
        {
            Toast.makeText(mAppContext, String.format("Ολοκληρώθηκε η σύνδεση με %s", device.model()), Toast.LENGTH_LONG).show();
            DeviceManager manager = DeviceManager.getDeviceManager();
            Set<DataSource> sources = device.supportedLoggingTypes();

            Log.i("pairingSucceed",device.supportedLoggingTypes()+"");
            if(sources != null)
            {
                for(DataSource source : sources) {
                    //custom default interval set to 10
                    if (source.name().equals("HEART_RATE") || source.name().equals("PULSE_OX")) {

                        try {
                            Futures.getChecked(manager.setLoggingStateWithInterval(device.address(), source, true, SharedPrefManager.getInstance(mAppContext).getGlobalInterval(), loggingStatus ->
                            {
                                if (loggingStatus != null && loggingStatus.getState() != LoggingStatus.State.LOGGING_ON && loggingStatus.getState() != LoggingStatus.State.LOGGING_OFF) {
                                    Log.i("settingDeviceInterval","error");
                                }
                                Log.i("device logging data","set");
                            }), Exception.class);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            boolean alarmUp = (PendingIntent.getBroadcast(mAppContext, 0,
                    new Intent(mAppContext,Alarm.class),
                    PendingIntent.FLAG_NO_CREATE) != null);


            if (!alarmUp)
            {
                Log.i("pairingSuccedd","alarm not up");
                Alarm alarm;
                alarm = new Alarm();
                if(alarm != null && SharedPrefManager.getInstance(mAppContext).getGarminDeviceAddress() !=null){
                    try {
                        Log.i("pairingSuccedd","alarm set");

                        alarm.setAlarm(mAppContext,SharedPrefManager.getInstance(mAppContext).getGarminDeviceAddress(),SharedPrefManager.getInstance(mAppContext).getGlobalInterval());
                    }catch (Exception e){
                        Log.i("alarmError",""+e.getMessage());
                    }

                }
            }
        }

        @Override
        public void onDevicePaired(@NonNull Device device) {
            //Log.i("deviceStatus","connected");
        }

        @Override
        public void onDeviceUnpaired(@NonNull String macAddress) {}
    }

    @Override
    protected void onDestroy() {
        //stopService(mServiceIntent);

/*        Log.i("onDestroy","in");
        if(isMyServiceRunning(BluetoothLeService.class)) {
            Intent broadcastIntent = new Intent();
            broadcastIntent.setAction("restartservice");
            broadcastIntent.setClass(this, MoodmetricServiceReceiver.class);
            this.sendBroadcast(broadcastIntent);
            Log.i("onDestroy","ble");

        }*/
        super.onDestroy();

    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                Log.i ("isMyServiceRunning?", true+"");
                return true;
            }
        }
        Log.i ("isMyServiceRunning?", false+"");
        return false;
    }
    @Override
    protected void onStart() {
        if (GarminHealth.isInitialized()) {
            GarminHealth.restart();
        }
        Log.i("garminActivity","started");

        registerReceiver(mReceiver, GarminHealthServiceReceiver.INTENT_FILTER);

        super.onStart();
    }

    @Override
    protected void onStop() {
        if (GarminHealth.isInitialized()) {
            //GarminHealth.stop();
            GarminHealth.restart();

        }
        Log.i("garminActivity","stopped");
        unregisterReceiver(mReceiver);

        super.onStop();
    }

}
