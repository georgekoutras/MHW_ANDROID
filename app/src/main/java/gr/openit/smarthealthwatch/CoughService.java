package gr.openit.smarthealthwatch;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.app.Service;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.error.AuthFailureError;
import com.android.volley.error.VolleyError;
import com.android.volley.request.StringRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import gr.openit.smarthealthwatch.MhwAudioHubClient;
import gr.openit.smarthealthwatch.R;
import gr.openit.smarthealthwatch.ui.MoodmetricServiceReceiver;
import gr.openit.smarthealthwatch.util.HttpsTrustManager;
import gr.openit.smarthealthwatch.util.SharedPrefManager;
import gr.openit.smarthealthwatch.util.URLs;
import gr.openit.smarthealthwatch.util.VolleySingleton;


public class CoughService extends Service implements MhwAudioHubClient.MhwAudioHubClientListener {
    private Timer timer;
    private TimerTask timerTask;
    private final Integer current_interval = 300; // interval every five minutes
    private Boolean userStoppedService = false;
    private boolean recording = false;
    private MediaRecorder recorder;
    private ProgressDialog progressBar;
    private String filepath;
    private String previousfilepath;

    private long recStartTimestamp;

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
        String channelName = "Background Cough Service";
        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(true)
                .setContentTitle("Cough Service is running in background")
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
            stopTimerTask();
            startTimer();
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        stopTimerTask();

        if(!this.userStoppedService){
            Intent broadcastIntent = new Intent();
            broadcastIntent.setAction("restart_service_cough");
            broadcastIntent.setClass(this, MoodmetricServiceReceiver.class);
            this.sendBroadcast(broadcastIntent);
        }else{
            stopTimerTask();
            stopRecording();
        }
        super.onDestroy();

    }

    public void startTimer() {
        startRecording();

        timer = new Timer();
        timerTask = new TimerTask() {
            public void run() {
               // Log.i("CoughService", "running");
                stopRecording();
               // Log.i("CoughService", "running");

            }
        };
        timer.schedule(timerTask,
                current_interval*1000,
                current_interval*1000);

    }

    public void stopTimerTask() {
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

    private void startRecording() {

        if(!recording) {
            if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getApplicationContext(), "No audio record permissions!", Toast.LENGTH_LONG).show();
                return;
            }

            Log.d("recording", "startRecordingCough()");
            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setAudioSamplingRate(16000);
            recorder.setAudioChannels(1);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            if(filepath != null && !filepath.equals("")){
                previousfilepath = filepath;
            }
            filepath = getApplicationContext().getExternalCacheDir().getAbsolutePath() + "/" + System.currentTimeMillis() + ".3gp";
            recorder.setOutputFile(filepath);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recorder.setAudioEncodingBitRate(128000);

            try {
                recorder.prepare();
            } catch (IOException e) {
                Log.e("recording", "prepare() failed");
            }

            recording = true;
            recStartTimestamp = System.currentTimeMillis();
            recorder.start();
        }
    }

    private void stopRecording() {

        if(recording) {
            try {
                Log.d("recording", "stopRecording()");
                recorder.stop();
                recorder.release();
                recorder = null;
                recording = false;

/*            File f = new File(filepath);
            int file_size = Integer.parseInt(String.valueOf(f.length()/1024));
            Log.i("fileSize",file_size+"");*/
               // Log.i("fileSize", filepath);
                HttpsTrustManager.allowAllSSL();
                sendExecuteJobRequest(filepath, recStartTimestamp);

            } catch (Exception e) {
                Log.e("stopRecordingException", "recording Exception "+e.getMessage());
                recording = false;
            }
        }

    }

    private void sendExecuteJobRequest(String filepath, long recStartTimestamp) {

        Log.d("recording", "sendExecuteJobRequest: \""+filepath+"\"");

        String host = SharedPrefManager.AUDIO_SERVICE_IP;
        int port = SharedPrefManager.AUDIO_SERVICE_PORT;
        String restPath = SharedPrefManager.AUDIO_REST_PATH;
        String username = SharedPrefManager.AUDIO_USER_NAME;
        String password = SharedPrefManager.AUDIO_PASSWORD;

        MhwAudioHubClient client = new MhwAudioHubClient(getApplicationContext(), host, port,
                restPath, username, password, this);

        //String userId = sharedPreferences.getString("user_id", "");
        String userId = SharedPrefManager.AUDIO_CLIENT_USER_NAME;

        client.executeJobRequest(userId, MhwAudioHubClient.AudioService.SPR, filepath, recStartTimestamp,
                true, 10000);
        /*if (voiceCmdRadioButton.isChecked()) {
            // Voice Command service is executed in real-time so we prefer a synced job execution
            // with a relatively small timeout.
            client.executeJobRequest(userId, MhwAudioHubClient.AudioService.ASR, filepath, recStartTimestamp,
                    true, 5000);
        } else {
            // Speech Pathology (cough detection) service is processed "in background" so we don't
            // care for a synced execution here. The job result is not shown on the end user but it
            // is sent to the central database by the audio server. Thus, in the final deployment
            // this should be called with sync=false. Only for this test app we call with sync=true
            // to include the job result immediately on the response and show it to the user.
            client.executeJobRequest(userId, MhwAudioHubClient.AudioService.SPR, filepath, recStartTimestamp,
                    true, 10000);
        }*/
        if(!this.userStoppedService) {
            startRecording();
        }
    }


    @Override
    public void onResponse(String response, boolean error) {

        Log.d("recordingResponse", "MainActivity.onResponse: error: "+error+", response: \""+response+"\"");
        if (!error) {
            try {
                JSONObject jobJsonObj = new JSONObject(response);
                //Log.i("coughResponse",jobJsonObj.toString());
                parseSprResult(jobJsonObj);

            } catch (JSONException e) {
                Log.i("Invalid JSON: ", "\""+response+"\": "+e.toString());
            }
        } else {
            Log.i("Error storing Cough", response);
        }
        deleteLastFile(previousfilepath);

    }

    private void deleteLastFile(String filepath){
        if(filepath != null && !filepath.equals("")) {
            File fdelete = new File(filepath);
            if (fdelete.exists()) {
                //Log.i("deleteFile", "fileExists");

                if (fdelete.delete()) {
                    Log.i("deleteFile", "file Deleted :" + filepath);
                } else {
                    Log.i("deleteFile", "file NOT Deleted :" + filepath);
                }
            }
        }
    }

    private void parseSprResult(JSONObject resultJson) throws JSONException {
        JSONObject result = resultJson.getJSONObject("result");
        JSONArray coughs = result.getJSONArray("coughs");
        if(coughs.length() > 0){
            Integer totalCough = 0;
            for(int i=0; i<coughs.length(); i++ ) {
                JSONObject coughDetails = coughs.getJSONObject(i);
                totalCough += coughDetails.getInt("coughs_n");
            }
            storeCough(totalCough);
        }
    }

    public void storeCough(Integer value){
        final JSONObject body = new JSONObject();
        TimeZone tz = TimeZone.getDefault();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS"); // Quoted "Z" to indicate UTC, no timezone offset
        df.setTimeZone(tz);
        String nowAsISO = df.format(new Date());

        try {
            body.put("name", "Βήχας");
            body.put("value",value);
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
                        //Log.i("storeCough","done");
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if(error.networkResponse.statusCode == 401 || error.networkResponse.statusCode == 403) {
                            stopRecording();
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
                headers.put("Authorization", "Bearer " + SharedPrefManager.getInstance(getApplicationContext()).getKeyAccessToken());
                return headers;
            }
        };
        stringRequest.setShouldCache(false);

        VolleySingleton.getInstance(getApplicationContext()).addToRequestQueue(stringRequest);
    }
}
