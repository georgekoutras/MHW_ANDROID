package gr.openit.smarthealthwatch;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.error.AuthFailureError;
import com.android.volley.error.VolleyError;
import com.android.volley.request.StringRequest;
import com.unity3d.player.UnityPlayer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.annotation.Nullable;

import gr.openit.smarthealthwatch.util.CircularFrameLayout;
import gr.openit.smarthealthwatch.util.SharedPrefManager;
import gr.openit.smarthealthwatch.util.URLs;
import gr.openit.smarthealthwatch.util.User;
import gr.openit.smarthealthwatch.util.VolleySingleton;

/**
 * A simple {@link Fragment} subclass.
 * Use the  factory method to
 * create an instance of this fragment.
 */
public class HelperFragment extends Fragment implements TextToSpeech.OnInitListener,MhwAudioHubClient.MhwAudioHubClientListener {

    private TextToSpeech tts;

    private EditText speakText;

    private Spinner animationList;

    private File tempDir;
    private File wavFile;

    TextView message;
    Context mContext;
    UserHome uh;
    private boolean recording = false;
    private MediaRecorder recorder;
    private AlertDialog.Builder builder;
    private ProgressDialog progressBar;
    private String filepath;
    private long recStartTimestamp;
    ImageView recStopButton;
    Button measurements_btn, messages_btn;
    ProgressDialog pd;
    final String startTime = "00:00:00";
    final String endTime = "23:59:59";
    SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
    Calendar cal;
    String displayDate;
    JSONObject stressThres = new JSONObject();

    public HelperFragment(Context mContext ,UserHome uh) {
        this.mContext = mContext;
        this.uh = uh;
        // Required empty public constructor
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onResume(){
        super.onResume();

        uh.hideMenu();
        uh.hideToolbar();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        cal = Calendar.getInstance();
        TimeZone tz = cal.getTimeZone();

        DateFormat toShow = new SimpleDateFormat("dd/MM/yyyy");
        toShow.setTimeZone(tz);
        displayDate = toShow.format(new Date());

        View v = inflater.inflate(R.layout.fragment_helper, container, false);
        ImageView helper_exit = v.findViewById(R.id.helper_exit);
        message = v.findViewById(R.id.helper_welcome_message);
        String firstName = SharedPrefManager.getInstance(mContext).getUser().getFirstName();
        builder = new AlertDialog.Builder(mContext);

        message.setText(getString(R.string.helper_message, firstName));
        recStopButton = v.findViewById(R.id.helper_rec);
        recStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onRecStopClick(recording);
            }
        });

        measurements_btn = v.findViewById(R.id.btn_measurements);
        measurements_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getMeasurements(null,"measurements");
            }
        });

        messages_btn = v.findViewById(R.id.btn_messages);
        messages_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getMessages();
            }
        });
        tts = new TextToSpeech(getActivity(), this);
        tts.setLanguage(Locale.US);
        tts.setSpeechRate(0.3f);
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String s) {
                Log.d("debug", "setOnUtteranceProgressListener onStart");
            }
            @Override
            public void onDone(String s) {
                Log.d("debug", "setOnUtteranceProgressListener onDone");

                try (FileInputStream fileInputStreamReader = new FileInputStream(wavFile)) {
                    byte[] bytes = new byte[(int)wavFile.length()];
                    fileInputStreamReader.read(bytes);
                    String encodedFile = Base64.encodeToString(bytes, Base64.DEFAULT);
                    UnityPlayer.UnitySendMessage("Model", "Speak", encodedFile);
                    encodedFile = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(String s) {
                Log.d("debug", "setOnUtteranceProgressListener onError" + s);
            }
        });

        helper_exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getFragmentManager().popBackStackImmediate();
                uh.showMenu();
                uh.showToolbar();
                uh.goToMainMenu(null);
            }

        });

        JSONArray tmpThres = null;
        try {
            tmpThres = new JSONArray(SharedPrefManager.getInstance(mContext).getThresholds());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if(tmpThres != null) {

            for (int i = 0; i < tmpThres.length(); i++) {
                String type = null;
                JSONObject s = null;
                try {
                    s = tmpThres.getJSONObject(i);
                    type = s.getString("name");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (type.equals("??????????")) {
                    stressThres = s;
                    break;
                }
            }
        }
        return v;
    }

    private void onRecStopClick(boolean recording) {
        if (recording)
            stopRecording();
        else
            startRecording();
    }

    private void startRecording() {

        if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED)
        {
            Toast.makeText(mContext, "No audio record permissions!", Toast.LENGTH_LONG).show();
            return;
        }

        Log.d("recording", "startRecording()");
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setAudioSamplingRate(16000);
        recorder.setAudioChannels(1);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        filepath = getActivity().getExternalCacheDir().getAbsolutePath()+"/"+System.currentTimeMillis()+".3gp";
        recorder.setOutputFile(filepath);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        recorder.setAudioEncodingBitRate(128000);

        try {
            recorder.prepare();
        } catch (IOException e) {
            Log.e("recording", "prepare() failed");
        }

        recording = true;
        recStopButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_baseline_stop_24));
        recStartTimestamp = System.currentTimeMillis();
        recorder.start();

    }

    private void stopRecording() {

        try {
            Log.d("recording", "stopRecording()");
            recorder.stop();
            recorder.release();
            recorder = null;
            recording = false;
            recStopButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_baseline_mic_24));

            progressBar = new ProgressDialog(mContext);
            progressBar.setCancelable(false);
            progressBar.setTitle("???????????????? ????????????????????..");
            progressBar.show();
/*            File f = new File(filepath);
            int file_size = Integer.parseInt(String.valueOf(f.length()/1024));
            Log.i("fileSize",file_size+"");*/
            sendExecuteJobRequest(filepath, recStartTimestamp);

        }catch (Exception e){
            Log.e("stopRecordingException", "recording Exception");
            recStopButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_baseline_mic_24));
            recording = false;
        }


    }

    private void sendExecuteJobRequest(String filepath, long recStartTimestamp) {

        Log.d("recording", "sendExecuteJobRequest: \""+filepath+"\"");

        String host = SharedPrefManager.AUDIO_SERVICE_IP;
        int port = SharedPrefManager.AUDIO_SERVICE_PORT;
        String restPath = SharedPrefManager.AUDIO_REST_PATH;
        String username = SharedPrefManager.AUDIO_USER_NAME;
        String password = SharedPrefManager.AUDIO_PASSWORD;

        MhwAudioHubClient client = new MhwAudioHubClient(mContext, host, port,
                restPath, username, password, this);

        //String userId = sharedPreferences.getString("user_id", "");
        String userId = SharedPrefManager.AUDIO_CLIENT_USER_NAME;

        client.executeJobRequest(userId, MhwAudioHubClient.AudioService.ASR, filepath, recStartTimestamp,
                true, 5000);

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
    }

    @Override
    public void onResponse(String response, boolean error) {

        Log.d("recordingResponse", "MainActivity.onResponse: error: "+error+", response: \""+response+"\"");
        if (!error) {
            try {
                JSONObject jobJsonObj = new JSONObject(response);

                String jobStatus = jobJsonObj.getString("status");
                if (jobStatus.equals("FAILED")) {
                    showResult2("Server Error", jobJsonObj.getString("status_info"));
                    return;
                }

                String jobService = jobJsonObj.getString("service");
                JSONObject resultJson = jobJsonObj.getJSONObject("result");
                if (jobService.equals("asr")) {
                    //showResult("Voice Command", parseAsrResult(resultJson));
                    showResult(resultJson);
                } else {
                    parseSprResult(jobJsonObj);                }
                //showResult("Cough Detection", parseSprResult(resultJson));
            } catch (JSONException e) {
                showResult2("Invalid JSON: ", "\""+response+"\": "+e.toString());
            }
        } else {
            showResult2("Error2", response);
        }
    }

    private void showResult(JSONObject response){
        progressBar.hide();
        progressBar = null;
        String action = null;
        String msg = null;
        try {
            action = response.getString("action");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if(action.equals("unknown")){
            msg = getString(R.string.helper_no_match);
            message.setText(msg);
            commandToSpeech(msg);
        }else{
            switch (action){
                case "stress":
                    getMeasurements("??????????",action);

                    break;
                case "pressure":
                    getMeasurements("??????????",action);

                    break;
                case "bloodsugar":
                    getMeasurements("??????????????",action);

                    break;
                case "heartrate":
                    getMeasurements("????????????",action);

                    break;
                case "cough":
                    getMeasurements("??????????",action);


                    break;
                case "oxygen":
                    getMeasurements("??????????????",action);

                    break;
                case "measurements":
                    getMeasurements(null,action);
                    break;
                case "messages":
                case "notifications":
                    getMessages();
                    break;
                case "emergency":
                    //tbd for calling an emergency number
                    break;
                default:
                    break;
            }

        }
    }

    private void buildMsg(String type, JSONArray measurementResults, JSONArray alertResults){
        String msg = null;
        measurementResults = sortJsonArray(measurementResults);
        switch (type){
            case "stress":
                if(measurementResults.length() > 0){
                    Integer stressTotal = 0;
                    for(int i=0; i< measurementResults.length(); i++){
                        JSONObject s = null;
                        try {
                            s = measurementResults.getJSONObject(i);
                            stressTotal += Integer.parseInt(s.getString("value"));

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }

                    try {
                        if(stressTotal/measurementResults.length() > Float.parseFloat(stressThres.getString("higher"))){
                            msg = getString(R.string.helper_message_stress,stressTotal/measurementResults.length(),"??????????");
                        }else if(stressTotal/measurementResults.length() < Float.parseFloat(stressThres.getString("lower"))){
                            msg = getString(R.string.helper_message_stress,stressTotal/measurementResults.length(),"????????????");
                        }else{
                            msg = getString(R.string.helper_message_stress,stressTotal/measurementResults.length(),"??????????????????????");
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    message.setText(msg);
                    commandToSpeech(msg);
                }else{
                    msg = getString(R.string.helper_message_no_mes,"??????????");
                    message.setText(msg);
                    commandToSpeech(msg);
                }

                break;
            case "pressure":
                if(measurementResults.length() > 0){
                    Integer limit = 0;
                    String values = "";
                    for (int i=0; i<measurementResults.length(); i++) {
                        try {
                            JSONObject measurement = measurementResults.getJSONObject(i);

                            values += measurement.getString("value") + " ?????????? ???? ";
                            values += measurement.getString("extraValue") + " ???????????? ";

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        if(limit == 1)
                            break;
                        limit++;
                    }
                    msg = getString(R.string.helper_message_mes,"??????????",values);

                    message.setText(msg);
                    commandToSpeech(msg);
                }else{
                    msg = getString(R.string.helper_message_no_mes,"??????????");
                    message.setText(msg);
                    commandToSpeech(msg);
                }

                break;
            case "bloodsugar":
                if(measurementResults.length() > 0){
                    Integer limit = 0;
                    String values = "";
                    for (int i=0; i<measurementResults.length(); i++) {
                        try {
                            JSONObject measurement = measurementResults.getJSONObject(i);
                            values += measurement.getString("value")+" ";
                            values += measurement.getString("note")+" ";

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        if(limit == 1)
                            break;
                        limit++;
                    }
                    msg = getString(R.string.helper_message_mes,"??????????????", values);

                    message.setText(msg);
                    commandToSpeech(msg);
                }else{
                    msg = getString(R.string.helper_message_no_mes,"??????????????");
                    message.setText(msg);
                    commandToSpeech(msg);
                }

                break;
            case "heartrate":
                if(measurementResults.length() > 0){
                    Integer limit = 0;
                    String values = "";
                    for (int i=0; i<measurementResults.length(); i++) {
                        try {
                            JSONObject measurement = measurementResults.getJSONObject(i);
                            if(limit == 0)
                                values += measurement.getString("value")+ ", ";
                            else
                                values += measurement.getString("value");

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        if(limit == 1)
                            break;
                        limit++;
                    }
                    msg = getString(R.string.helper_message_mes,"??????????????",values);
                    message.setText(msg);
                    commandToSpeech(msg);
                }else{
                    msg = getString(R.string.helper_message_no_mes,"????????????");
                    message.setText(msg);
                    commandToSpeech(msg);
                }

                break;
            case "cough":
                if(measurementResults.length() > 0){
                    Integer coughCounter = 0;
                    for (int i=0; i<measurementResults.length(); i++) {
                        try {
                            JSONObject measurement = measurementResults.getJSONObject(i);
                            coughCounter += measurement.getInt("value");


                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    msg = getString(R.string.helper_message_mes_cough,"????????",coughCounter);
                    message.setText(msg);
                    commandToSpeech(msg);
                }else{
                    msg = getString(R.string.helper_message_no_mes,"????????");
                    message.setText(msg);
                    commandToSpeech(msg);
                }

                break;
            case "oxygen":
                if(measurementResults.length() > 0){
                    Integer limit = 0;
                    String values = "";
                    for (int i=0; i<measurementResults.length(); i++) {
                        try {
                            JSONObject measurement = measurementResults.getJSONObject(i);

                            values += measurement.getString("value")+"% ";

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        if(limit == 1)
                            break;
                        limit++;
                    }
                    msg = getString(R.string.helper_message_mes,"??????????????",values);
                    message.setText(msg);
                    commandToSpeech(msg);
                }else{
                    msg = getString(R.string.helper_message_no_mes,"??????????????");
                    message.setText(msg);
                    commandToSpeech(msg);
                }

                break;
            case "measurements":
                if(measurementResults.length() > 0){
                    Integer limit = 0;
                    String values = "";

                    String cough = "", stress="",hr="",gluce="",pressure="",ox="";
                    Integer stressTotal=0;
                    JSONArray hrArray = new JSONArray(), oxArray = new JSONArray(), pressureArray = new JSONArray(),
                            gluceArray = new JSONArray(), stressArray = new JSONArray(), coughArray = new JSONArray();
                    Integer hrCnt=0, oxCnt = 0, pressureCnt = 0, gluceCnt = 0, stressCnt = 0, coughCnt = 0;
                    for (int i=0; i<measurementResults.length(); i++) {
                        try {
                            JSONObject measurement = measurementResults.getJSONObject(i);

                            String name = measurement.getString("name");

                            if(name.equals("????????????")){
                                if(hrCnt < 2){
                                    hr += measurement.getString("value")+ ", ";
                                    hrCnt++;
                                }
                                hrArray.put(measurement);
                            }else if(name.equals("??????????????")){
                                if(oxCnt < 2){
                                    ox += measurement.getString("value")+"%, ";
                                    oxCnt++;
                                }
                                oxArray.put(measurement);
                            }else if(name.equals("??????????")){
                                if(pressureCnt < 2){
                                    pressure += measurement.getString("value") + " ?????????? ???? ";
                                    pressure += measurement.getString("extraValue") + " ????????????, ";
                                    pressureCnt++;
                                }
                                pressureArray.put(measurement);
                            }else if(name.equals("??????????????")){
                                if(gluceCnt < 2){
                                    gluce += measurement.getString("value")+" ";
                                    gluce += measurement.getString("note")+", ";
                                    gluceCnt++;
                                }
                                gluceArray.put(measurement);
                            }else if(name.equals("??????????")){
                                stressTotal += Integer.parseInt(measurement.getString("value"));
                                stressCnt++;
                                stressArray.put(measurement);
                            }else if(name.equals("??????????")){
                                if(coughArray.length() < 3)
                                    coughArray.put(measurement);
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    if(hrCnt > 0)
                        values += "\n????????????: "+hr;
                    if(oxCnt > 0)
                        values += "\n??????????????: "+ox;
                    if(pressureCnt > 0)
                        values += "\n??????????: "+pressure;
                    if(gluceCnt > 0)
                        values += "\n??????????????: "+gluce;
                    if(stressCnt> 0){
                        try {
                            values += "\n??????????: ";
                            if(stressTotal/stressCnt > Float.parseFloat(stressThres.getString("higher"))){
                                values += stressTotal/stressCnt + " ??????????";
                            }else if(stressTotal/stressCnt < Float.parseFloat(stressThres.getString("lower"))){
                                values += stressTotal/stressCnt + " ????????????";
                            }else{
                                values += stressTotal/stressCnt + " ??????????????????????";
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    if(coughCnt > 0){
                        values += "\n??????????: " + cough;
                    }
                    msg = getString(R.string.helper_message_mes_general,values);
                    message.setText(msg);
                    commandToSpeech(msg);
                }else{
                    msg = getString(R.string.helper_message_no_mes_total);
                    message.setText(msg);
                    commandToSpeech(msg);
                }
                break;
            case "messages":
                JSONArray lastUnread = new JSONArray();
                Integer unreadCnt = 0;

                if(measurementResults.length() > 0) {
                    try {
                        for (int i=0; i<measurementResults.length(); i++) {

                            JSONObject message = measurementResults.getJSONObject(i);
                            if(!message.getBoolean("read")) {
                                lastUnread.put(message);
                                unreadCnt++;
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                if(alertResults.length() > 0) {
                    try {
                        for (int i = 0; i < alertResults.length(); i++) {

                            JSONObject message = alertResults.getJSONObject(i);

                            if (!message.getBoolean("seen")) {
                                lastUnread.put(message);
                                unreadCnt++;
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                lastUnread = sortJsonArray(lastUnread);

                if(unreadCnt > 0 && lastUnread.length() > 0) {

                    JSONObject s = null;
                    try {
                        s = lastUnread.getJSONObject(0);
                        if(s.has("sender")) {
                            msg = getString(R.string.helper_message_notifications, String.valueOf(unreadCnt), s.getString("sender"), s.getString("text"));
                        }else{
                            msg = getString(R.string.helper_message_notifications, String.valueOf(unreadCnt),getString(R.string.smart_device),  getString(R.string.smart_device_message,s.getString("type"),String.valueOf(s.getInt("value"))));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    message.setText(msg);
                    commandToSpeech(msg);
                }else{
                    msg = getString(R.string.helper_message_no_notifications);
                }
                message.setText(msg);
                commandToSpeech(msg);
                break;
            case "notifications":
                break;
            case "emergency":
                //tbd for calling an emergency number
                break;
            default:
                break;
        }
    }
    private void showResult2(String title, String result) {

        progressBar.hide();
        progressBar = null;
        builder.setMessage(result);
        AlertDialog alert = builder.create();
        alert.setTitle(title);
        alert.show();
        new File(filepath).delete();
        filepath = null;
    }

    private String parseAsrResult(JSONObject resultJson) throws JSONException {
        String action = resultJson.getString("action");
        String command = resultJson.getString("command");
        //commandToSpeech(command);
        return "Action: "+action+"\nCommand: \""+command+"\"";
    }

    private String parseSprResult(JSONObject resultJson) throws JSONException {
        JSONArray coughs = resultJson.getJSONArray("coughs");
        int totalCoughs = 0;
        for (int i=0; i < coughs.length(); i++) {
            totalCoughs += coughs.getJSONObject(i).getInt("coughs_n");
        }
        return "Detected "+totalCoughs+" coughs!";
    }

    private void commandToSpeech(String command) {
        String text = command;
        Log.i("unityCommand",""+command);
        if (command != null){
            try {
                if (tempDir == null) {
                    tempDir = getActivity().getCacheDir();
                }
                wavFile = File.createTempFile("speech", "wav", tempDir);
                Bundle params = new Bundle();
                params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "");
                tts.synthesizeToFile(text, params, wavFile, "id");

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void getMessages(){
        pd = new ProgressDialog(mContext);
        pd.setMessage("???????????????? ????????????????????..");
        pd.show();

        String primaryUserInfoUrl = URLs.URL_GET_MESSAGES.replace("{id}",""+ SharedPrefManager.getInstance(mContext).getUser().getId());
        StringRequest stringRequest = new StringRequest(Request.Method.GET, primaryUserInfoUrl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            getAlerts(new JSONArray(response));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        pd.hide();
                        /*SharedPrefManager.getInstance(mContext).logout();
                        userLogin();*/
                        Toast.makeText(mContext, "?????????????????????????? ????????????! ???????????????? ?????????????? ?????? ?????????????? ?????? ?????? ??????????????????.", Toast.LENGTH_LONG).show();
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
                headers.put("Authorization", "Bearer " + SharedPrefManager.getInstance(mContext).getKeyAccessToken());

                return headers;
            }
        };
        stringRequest.setShouldCache(false);

        VolleySingleton.getInstance(mContext).addToRequestQueue(stringRequest);
    }

    public void getAlerts(JSONArray messagesArray){
        String primaryUserInfoUrl = URLs.URL_GET_ALERTS.replace("{id}",""+ SharedPrefManager.getInstance(mContext).getUser().getId());
        StringRequest stringRequest = new StringRequest(Request.Method.GET, primaryUserInfoUrl+"?excludeSeen=true",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        pd.hide();
                        try {
                            JSONArray jsonArray = new JSONArray(response);
                            try {
                                buildMsg("messages", messagesArray,new JSONArray(response));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        pd.hide();
                        /*SharedPrefManager.getInstance(mContext).logout();
                        userLogin();*/
                        Toast.makeText(mContext, "?????????????????????????? ????????????! ???????????????? ?????????????? ?????? ?????????????? ?????? ?????? ??????????????????.", Toast.LENGTH_LONG).show();
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
                headers.put("Authorization", "Bearer " + SharedPrefManager.getInstance(mContext).getKeyAccessToken());

                return headers;
            }
        };
        stringRequest.setShouldCache(false);

        VolleySingleton.getInstance(mContext).addToRequestQueue(stringRequest);
    }

    private void getMeasurements(String type,String action){
        pd = new ProgressDialog(mContext);
        pd.setMessage("???????????????? ????????????????????..");
        pd.show();
        DateFormat startDateFormat = new SimpleDateFormat("yyyy-MM-dd'T"+startTime+"'"); // Quoted "Z" to indicate UTC, no timezone offset
        DateFormat endDateFormat = new SimpleDateFormat("yyyy-MM-dd'T"+endTime+"'"); // Quoted "Z" to indicate UTC, no timezone offset
        try {
            String startForQuery = startDateFormat.format(dateFormat.parse(displayDate));
            String endForQuery = endDateFormat.format(dateFormat.parse(displayDate));

            String primaryUserInfoUrl = URLs.URL_GET_MEASUREMENT.replace("{id}",""+ SharedPrefManager.getInstance(mContext).getUser().getId());
            if(type !=null)
                primaryUserInfoUrl += "?"+URLs.START_TIME+startForQuery+"&"+URLs.END_TIME+endForQuery+"&"+URLs.MEASUREMENT_TYPE+type;
            else
                primaryUserInfoUrl += "?"+URLs.START_TIME+startForQuery+"&"+URLs.END_TIME+endForQuery;

            StringRequest stringRequest = new StringRequest(Request.Method.GET, primaryUserInfoUrl,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            pd.hide();
                            try {
                                buildMsg(action,new JSONArray(response),null);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            pd.hide();
                            /*SharedPrefManager.getInstance(mContext).logout();
                            userLogin();*/
                            Toast.makeText(mContext, "?????????????????????????? ????????????! ???????????????? ?????????????? ?????? ?????????????? ?????? ?????? ??????????????????.", Toast.LENGTH_LONG).show();
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
                    headers.put("Authorization", "Bearer " + SharedPrefManager.getInstance(mContext).getKeyAccessToken());

                    return headers;
                }
            };
            stringRequest.setShouldCache(false);

            VolleySingleton.getInstance(mContext).addToRequestQueue(stringRequest);
        } catch (ParseException e) {
            e.printStackTrace();
        }

    }
    @Override
    public void onInit(int status) {

    }

    public static JSONArray sortJsonArray(JSONArray array) {
        List<JSONObject> jsons = new ArrayList<JSONObject>();
        try {
            for (int i = 0; i < array.length(); i++) {
                jsons.add(array.getJSONObject(i));
            }
            Collections.sort(jsons, new Comparator<JSONObject>() {
                @Override
                public int compare(JSONObject lhs, JSONObject rhs) {
                    String lid = null;
                    String rid = null;
                    try {
                        lid = lhs.getString("timeStamp");
                        rid = rhs.getString("timeStamp");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    return rid.compareTo(lid);

                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return new JSONArray(jsons);
    }

    public void userLogin() {
        Fragment loginFragment = new LoginFragment(mContext);
        FragmentManager fm = getFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
        transaction.replace(R.id.main_root,loginFragment); // give your fragment container id in first parameter
        transaction.commit();
    }
}