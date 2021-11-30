package gr.openit.smarthealthwatch.ui;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.Service;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.JobIntentService;
import androidx.core.app.NotificationCompat;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.error.AuthFailureError;
import com.android.volley.error.VolleyError;
import com.android.volley.request.StringRequest;
import com.garmin.health.Device;
import com.garmin.health.database.dtos.StressLog;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import gr.openit.smarthealthwatch.MainActivity;
import gr.openit.smarthealthwatch.util.HttpsTrustManager;
import gr.openit.smarthealthwatch.util.SharedPrefManager;
import gr.openit.smarthealthwatch.util.URLs;
import gr.openit.smarthealthwatch.util.VolleySingleton;


public class BluetoothLeService extends Service  {

    private final static String TAG = BluetoothLeService.class.getSimpleName();
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private Boolean userStoppedService = false;
    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;
    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;
    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";
    private static final UUID mmServiceUUID =
            UUID.fromString("dd499b70-e4cd-4988-a923-a7aab7283f8e");
    private static final UUID streamingCharacteristicUUID =
            UUID.fromString("a0956420-9bd2-11e4-bd06-0800200c9a66");
    // Defined by the BLE standard
    private static final UUID clientCharacteristicConfigurationUUID =
            UUID.fromString("00002902-0000-1000-8000-00805F9B34FB");
    private BluetoothGatt bleGatt;
    BluetoothDevice mDevice;

    public int counter = 0;
    private Timer timer;
    private TimerTask timerTask;
    Context mContext;
    private String currentValue = null;
    List<String> valuesList = new ArrayList<>();
    boolean deviceDisconnected = false;
    ProgressDialog pd;
    private int current_interval;

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
        String NOTIFICATION_CHANNEL_ID = "example.permanence";
        String channelName = "Background Service";
        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(true)
                .setContentInfo("Moodmetric Ring is paired")
                .setContentTitle("App is running in background")
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        startForeground(2, notification);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        if(intent != null){
            if(intent.hasExtra("device_address")) {
                this.mBluetoothDeviceAddress = intent.getStringExtra("device_address");
            }
            if(intent.hasExtra("stop_service")) {
                this.userStoppedService = intent.getBooleanExtra("stop_service",false);
            }
            if(intent.hasExtra("interval")) {
                this.current_interval = intent.getIntExtra("interval",10);
                stoptimertask();
                startTimer();
            }
        }
        final BluetoothManager bluetoothManager =
                (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) {
            Log.i("bleService","bluetooth service not available");

        }else {
            // Get a reference to the bluetooth adapter of the device
            if(!this.userStoppedService && intent !=null && intent.hasExtra("device_address")) {
                mBluetoothAdapter = bluetoothManager.getAdapter();
                if (mBluetoothDeviceAddress != null && !mBluetoothDeviceAddress.equals("")) {
                    mDevice = mBluetoothAdapter.getRemoteDevice(mBluetoothDeviceAddress);
                    if (mDevice != null) {
                        bleGatt = mDevice.connectGatt(getApplicationContext(), false, mGattCallback);
                        bleGatt.connect();
                    }
                }
            }
        }
        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        stoptimertask();

        if(!this.userStoppedService){
            Intent broadcastIntent = new Intent();
            broadcastIntent.setAction("restart_service_ring");
            broadcastIntent.setClass(this, MoodmetricServiceReceiver.class);
            this.sendBroadcast(broadcastIntent);
        }else{
            disconnectGatt();
            stoptimertask();
        }
        super.onDestroy();

    }


    public void startTimer() {
        timer = new Timer();
        timerTask = new TimerTask() {
            public void run() {
                if(deviceDisconnected){
                    Log.i("Last value", "device disconnected");

                    mDevice = mBluetoothAdapter.getRemoteDevice(mBluetoothDeviceAddress);
                    if(mDevice != null) {
                        bleGatt = mDevice.connectGatt(getApplicationContext(), false, mGattCallback);
                        if(bleGatt.connect()){
                            deviceDisconnected = false;
                        }
                    }
                }else {
                    if (valuesList.size() > 0) {
                        currentValue = valuesList.get(valuesList.size() - 1);
                        storeStress(currentValue);
                        currentValue = null;
                        valuesList.clear();
                    } else {
                        Log.i("Last value", "doesn't exist Address: "+mBluetoothDeviceAddress);

                    }
                }
            }
        };
        timer.schedule(timerTask,
                current_interval*1000,
                current_interval*1000);
    }

    public void stoptimertask() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt.discoverServices();
            }else if(newState == BluetoothProfile.STATE_DISCONNECTED){
                deviceDisconnected = true;
                bleGatt.close();
            }
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, int status) {
            enableStreamingNotification(gatt);

        }

        private void enableStreamingNotification(BluetoothGatt gatt) {
            //log1("services discovered, enabling notifications");
            // Get the MM service

            BluetoothGattService mmService = gatt.getService(mmServiceUUID);
            // Get the streaming characteristic
            BluetoothGattCharacteristic streamingCharacteristic =
                    mmService.getCharacteristic(streamingCharacteristicUUID);
            // Enable notifications on the streaming characteristic
            gatt.setCharacteristicNotification(streamingCharacteristic, true);
            // For some reason the above does not tell the ring that it should
            // start sending notifications, so we have to do it explicitly
            BluetoothGattDescriptor cccDescriptor =
                    streamingCharacteristic.getDescriptor(clientCharacteristicConfigurationUUID);
            cccDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            gatt.writeDescriptor(cccDescriptor);
        }

        @Override
        public void onCharacteristicChanged(
                BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {

            byte[] payload = characteristic.getValue();
            // Decode payload
            int status = payload[0] & 0xff;
            int mm = payload[1] & 0xff;
            // Instant EDA value is in payload bytes 2 and 3 in big-endian format
            int instant = ((payload[2] & 0xff) << 8) | (payload[3] & 0xff);
            // Acceleration in x, y and z directions
            int ax = payload[4] & 0xff;
            int ay = payload[5] & 0xff;
            int az = payload[6] & 0xff;
            // Acceleration magnitude
            double a = Math.sqrt(ax*ax + ay*ay + az*az);
            //log1("st:%02x\tmm:%d\teda:%d\ta:%.1f", status, mm, instant, a);
            String s = String.format("%d",mm);
            //currentValue = s;
            valuesList.add(s);
            //Log.i("ringData","value: "+s+" status: "+status+" "+" instant: "+instant);
        }
    };

    public void disconnectGatt() {
        if(bleGatt != null) {
            bleGatt.close();
            bleGatt.disconnect();
            bleGatt=null;
        }
    }

    public void storeStress(String value){
        final JSONObject body = new JSONObject();
        TimeZone tz = TimeZone.getDefault();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS"); // Quoted "Z" to indicate UTC, no timezone offset
        df.setTimeZone(tz);
        String nowAsISO = df.format(new Date());

        try {
            body.put("name", "STR");
            body.put("value", Integer.parseInt(value));
            body.put("timeStamp", nowAsISO);

        } catch (JSONException e) {
            e.printStackTrace();
        }


        HttpsTrustManager.allowAllSSL();
        StringRequest stringRequest = new StringRequest(Request.Method.POST, (URLs.URL_ADD_MEASUREMENT).replace("{id}", "" + SharedPrefManager.getInstance(getApplicationContext()).getUser().getId()),
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        //progressBar.setVisibility(View.GONE);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if(error.networkResponse.statusCode == 401 || error.networkResponse.statusCode == 403) {
                            disconnectGatt();
                            stoptimertask();
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
                headers.put("Authorization", "Bearer " + SharedPrefManager.getInstance(getApplicationContext()).getKeyAccessToken());
                return headers;
            }
        };
        stringRequest.setShouldCache(false);

        VolleySingleton.getInstance(getApplicationContext()).addToRequestQueue(stringRequest);

    }
}
