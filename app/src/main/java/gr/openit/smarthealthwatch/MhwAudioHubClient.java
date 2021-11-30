package gr.openit.smarthealthwatch;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.error.AuthFailureError;
import com.android.volley.error.VolleyError;
import com.android.volley.request.SimpleMultiPartRequest;
import com.android.volley.request.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

/**
 * Implementation of client interface for the audio hub server.
 * @author Yannis Mastorakis <ymastorak@gmail.com>
 */
public class MhwAudioHubClient implements Response.ErrorListener, Response.Listener<String> {

    public enum AudioService { ASR, SPR };

    public interface MhwAudioHubClientListener {
        void onResponse(String response, boolean error);
    }

    private static final String TAG = "audio_tag";
    private static final String TIMESTAMP_FORMAT = "yyyy-MM-dd hh:mm:ss.SSS z";
    public static final String PARAMS_FORM_NAME = "parameters";
    public static final String PING_MSG_FIELD = "msg";
    public static final String USER_ID_FIELD = "user_id";
    public static final String SERVICE_FIELD = "service";
    public static final String INPUT_ID_FIELD = "input_id";
    public static final String SYNC_FIELD = "sync";
    public static final String SYNC_TIMEOUT_FIELD = "sync_timeout";
    public static final String REC_START_TIMESTAMP_FIELD = "rec_start_timestamp";

    private RequestQueue requestQueue;
    private String host;
    private int port;
    private String restPath;
    private String username;
    private String password;
    private MhwAudioHubClientListener listener;

    /**
     * Create a new client object.
     *
     * @param listener the listener of request responses
     * @param host domain or ip of audio hub server
     * @param port port of audio hub server
     * @param restPath path of REST interface of audio hub server
     * @param username the username of authentication credentials
     * @param password the password of authentication credentials
     */
    MhwAudioHubClient(Context context,
                      String host, int port,
                      String restPath,
                      String username,
                      String password,
                      MhwAudioHubClientListener listener) {
        this.host = host;
        this.port = port;
        this.restPath = restPath;
        this.username = username;
        this.password = password;
        this.listener = listener;

        // Initialize the hostname verifier to accept our custom hostname. This is not needed
        // if the server has a valid certificate!
        initHostnameVerifier();

        requestQueue = Volley.newRequestQueue(context);
        requestQueue.start();
    }

    /**
     * Sends a request for job execution to the audio hub server. The server will store the uploaded
     * audio file to its database. Then it will create a new job for the requested service and
     * send back the job entity in json format. This request is identical to executing first
     * {@link #uploadInputFileRequest(String, String, long)} and then
     * {@link #postJobRequest(String, AudioService, String, boolean)}.
     *
     * @param userId the id of the user that executes the job.
     * @param service the processing service.
     * @param filepath the audio file to upload.
     * @param recStartTimestamp the absolute start time of audio recording in milliseconds.
     * @param sync if true, the server will try to execute the job immediately, and the
     *             response json will contain the final job result. If the server cannot start
     *             processing the new job within time set by <code>syncTimeoutMillis</code>, then the
     *             job fails by timing-out.  If set to false, then the server schedules the job for
     *             future execution and returns job json response immediately. The user can check
     *             the job result status by polling at the server, using the {@link #getJobRequest(String)}
     *             method. In asynchronous mode there is no timeout.
     * @param syncTimeoutMillis time-out duration of synced job in milliseconds.
     *
     * @returns The response will be:
     *          <p>201 - Created on success, and the job entity in json format.
     *          <p>403 - Forbidden if authentication credentials are not correct.
     *          <p>400 - Bad Request if request parameters are missing or invalid. Response entity
     *                   will contain the explanation.
     *          <p>404 - Not found if requested service does not exist.
     *          <p>415 - Unsupported media type if audio file type is not supported. Supported
     *                   audio files are files supported by ffmpeg 4.1.4-1.
     *          <p>503 - Service Unavailable if the server is busy and the job times out (only for
     *                   synced jobs). The result will contain the job json entity.
     *          <p>500 - Internal Server Error if anything goes wrong in the server.
     *
     * @see #uploadInputFileRequest(String, String, long)
     * @see #postJobRequest(String, AudioService, String, boolean)
     * @see #getJobRequest(String)
     */
    public void executeJobRequest(String userId,
                                  AudioService service,
                                  String filepath,
                                  long recStartTimestamp,
                                  boolean sync,
                                  long syncTimeoutMillis) {

        String url = "https://"+host+":"+port+"/"+restPath+"/executor";
        SimpleMultiPartRequest executeRequest = new SimpleMultiPartRequest(Request.Method.POST, url,
                this, this);
        executeRequest.setHeaders(getBasicAuthenticationHeader(username, password));
        executeRequest.addFile("file", filepath);

        String params = null;
        try {
            params = new JSONObject()
                    .put(USER_ID_FIELD, userId)
                    .put(SERVICE_FIELD, serviceToString(service))
                    .put(REC_START_TIMESTAMP_FIELD, timestampToString(recStartTimestamp))
                    .put(SYNC_FIELD, sync)
                    .put(SYNC_TIMEOUT_FIELD, syncTimeoutMillis)
                    .toString();
        } catch (JSONException e) {
            Log.e(TAG, "executeJobRequest: "+e.toString());
            return;
        }
        executeRequest.addStringParam(PARAMS_FORM_NAME, params);
        requestQueue.add(executeRequest);
    }

    /**
     * Uploads an audio file to the audio hub server. The user can then create a new job for this
     * file using the {@link #postJobRequest(String, AudioService, String, boolean)} method.
     *
     * @param userId the id of the user that uploads the audio file.
     * @param filepath the audio file to upload.
     * @param recStartTimestamp the absolute start time of audio recording in milliseconds.
     *
     * @returns The response will be:
     *          <p>200 - OK on success, and the input file entity in json format.
     *          <p>403 - Forbidden if authentication credentials are not correct.
     *          <p>400 - Bad Request if request parameters are missing or invalid. Response entity
     *                   will contain the explanation.
     *          <p>415 - Unsupported media type if audio file type is not supported. Supported
     *                   audio files are files supported by ffmpeg 4.1.4-1.
     *          <p>500 - Internal Server Error if anything goes wrong in the server.
     * @see #postJobRequest(String, AudioService, String, boolean)
     */
    public void uploadInputFileRequest(String userId, String filepath, long recStartTimestamp) {

        String url = "https://"+host+":"+port+"/"+restPath+"/inputs";
        SimpleMultiPartRequest uploadRequest = new SimpleMultiPartRequest(Request.Method.POST, url,
                this, this);
        uploadRequest.setHeaders(getBasicAuthenticationHeader(username, password));
        uploadRequest.addFile("file", filepath);

        String params = null;
        try {
            params = new JSONObject()
                    .put(USER_ID_FIELD, userId)
                    .put(REC_START_TIMESTAMP_FIELD, timestampToString(recStartTimestamp))
                    .toString();
        } catch (JSONException e) {
            Log.e(TAG, "uploadInputFileRequest: "+e.toString());
            return;
        }
        uploadRequest.addStringParam(PARAMS_FORM_NAME, params);
        requestQueue.add(uploadRequest);
    }

    /**
     * Sends a new job request to the audio hub server, for an already uploaded audio file.
     *
     * @param userId the id of the user that creates the job.
     * @param service the processing service.
     * @param inputId the id of the audio file to process.
     * @param sync if true, the server will try to execute the job immediately, and the
     *             response json will contain the final job result. If the server cannot start
     *             processing the new job within time set by <code>syncTimeoutMillis</code>, then the
     *             job fails by timing-out.  If set to false, then the server schedules the job for
     *             future execution and returns job json response immediately. The user can check
     *             the job result status by polling at the server, using the {@link #getJobRequest(String)}
     *             method. In asynchronous mode there is no timeout.
     *
     * @returns The response will be:
     *          <p>201 - Created on success, and the job entity in json format.
     *          <p>403 - Forbidden if authentication credentials are not correct.
     *          <p>400 - Bad Request if request parameters are missing or invalid. Response entity
     *                   will contain the explanation.
     *          <p>404 - Not found if requested service does not exist.
     *          <p>401 - Unauthorized if the user that tries to create the job is different than the
     *          user that uploaded the audio file.
     *          <p>503 - Service Unavailable if the server is busy and the job times out (only for
     *                   synced jobs). The result will contain the job json entity.
     *          <p>500 - Internal Server Error if anything goes wrong in the server.
     * @see #uploadInputFileRequest(String, String, long)
     * @see #getJobRequest(String)
     */
    public void postJobRequest(String userId, AudioService service, String inputId, boolean sync) {

        String params = null;
        try {
            params = new JSONObject()
                    .put(USER_ID_FIELD, userId)
                    .put(SERVICE_FIELD, serviceToString(service))
                    .put(INPUT_ID_FIELD, inputId)
                    .put(SYNC_FIELD, sync)
                    .toString();
        } catch (JSONException e) {
            Log.e(TAG, "postJobRequest: "+e.toString());
            return;
        }

        String url = "https://"+host+":"+port+"/"+restPath+"/jobs";
        StringJsonRequest postJobRequest = new StringJsonRequest(Request.Method.POST, url, this, this);
        postJobRequest.setBody(params);
        postJobRequest.setHeaders(getBasicAuthenticationHeader(username, password));
        requestQueue.add(postJobRequest);
    }

    /**
     * Get a job entity.
     *
     * @param jobId id of the job entity.
     *
     * @returns The response will be:
     *          <p>200 - OK on success, and the job entity in json format.
     *          <p>403 - Forbidden if authentication credentials are not correct.
     *          <p>405 - Not found if job entity does not exist.
     *          <p>500 - Internal Server Error if anything goes wrong in the server.
     */
    public void getJobRequest(String jobId) {

        String url = "https://"+host+":"+port+"/"+restPath+"/jobs/"+jobId;
        StringJsonRequest getJobRequest = new StringJsonRequest(Request.Method.GET, url, this, this);
        getJobRequest.setShouldCache(false);
        getJobRequest.setHeaders(getBasicAuthenticationHeader(username, password));
        requestQueue.add(getJobRequest);
    }

    /**
     * Send a simple GET request to the audio hub server with a query message.
     * Used only for debug/testing purposes.
     *
     * @param msg the message to send to the server
     *
     * @returns The response will be:
     *          <p>200 - OK on success, and message doubled in the entity.
     *          <p>403 - Forbidden if authentication credentials are not correct.
     *          <p>500 - Internal Server Error if anything goes wrong in the server.
     */
    public void pingRequest(String msg) {

        String url = "https://"+host+":"+port+"/"+restPath;
        Request pingRequest = new StringRequest(Request.Method.GET, url, this, this);
        pingRequest.setShouldCache(false);
        pingRequest.setHeaders(getBasicAuthenticationHeader(username, password));
        HashMap<String, String> params = new HashMap<>();
        params.put(PING_MSG_FIELD, msg);
        pingRequest.setParams(params);
        requestQueue.add(pingRequest);
    }

    @Override
    public void onErrorResponse(VolleyError error) {
        String response = error.getClass().getSimpleName();
        if (error.networkResponse != null)
            response += " "+error.networkResponse.statusCode;
        else
            response += " "+error.getMessage();
        listener.onResponse(response, true);
    }

    @Override
    public void onResponse(String response) {
        listener.onResponse(response, false);
    }

    /**
     * Init SSL parameters to accept our self-signed certificate :)
     * This is not needed in final production where a valid server certificate will be used.
     * Custom certificate is at res/raw/mhwaudiohubcert.crt.
     *
     * @param context application context
     */
    public static void initSSLCertificate(Context context) {
        try {
            HttpsURLConnection.setDefaultSSLSocketFactory(getSocketFactory(context));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Initialize the hostname verifier to accept our custom hostname. This is not needed
     * if the server has a valid certificate!
     */
    private static void initHostnameVerifier() {
        try {
            HttpsURLConnection.setDefaultHostnameVerifier(getHostnameVerifier());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String serviceToString(AudioService service) {
        switch(service) {
            case ASR:
                return "asr";
            case SPR:
                return "spr";
            default:
                return null;
        }
    }

    private static String timestampToString(long timestamp) {
        DateFormat formatter = new SimpleDateFormat(TIMESTAMP_FORMAT);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        return formatter.format(calendar.getTime());
    }

    private Map<String, String> getBasicAuthenticationHeader(String username, String password) {
        String credentials = username+":"+password;
        String auth = "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
        Map<String, String> authHeaders = new HashMap<String, String>();
        authHeaders.put("Authorization", auth);
        return authHeaders;
    }

    // Code taken from https://developer.android.com/training/articles/security-ssl.html
    private static SSLSocketFactory getSocketFactory(Context context)
            throws CertificateException, IOException, KeyStoreException, NoSuchAlgorithmException, KeyManagementException {

        // Load CAs from an InputStream
        // (could be from a resource or ByteArrayInputStream or ...)
        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        InputStream caInput = new BufferedInputStream(context.getResources().openRawResource(R.raw.mhwaudiohubcert));
        Certificate ca;
        try {
            ca = cf.generateCertificate(caInput);
        } finally {
            caInput.close();
        }

        // Create a KeyStore containing our trusted CAs
        String keyStoreType = KeyStore.getDefaultType();
        KeyStore keyStore = KeyStore.getInstance(keyStoreType);
        keyStore.load(null, null);
        keyStore.setCertificateEntry("ca", ca);

        // Create a TrustManager that trusts the CAs in our KeyStore
        String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
        tmf.init(keyStore);

        // Create an SSLContext that uses our TrustManager
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, tmf.getTrustManagers(), null);

        // Tell the URLConnection to use a SocketFactory from our SSLContext
        return sslContext.getSocketFactory();
    }

    // Code taken from https://developer.android.com/training/articles/security-ssl.html
    private static HostnameVerifier getHostnameVerifier() {
        return new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                if (hostname.equals(hostname))
                    return true;
                HostnameVerifier hv = HttpsURLConnection.getDefaultHostnameVerifier();
                return hv.verify(hostname, session);
            }
        };
    }

    private class StringJsonRequest extends StringRequest {

        String body;

        public StringJsonRequest(int method, String url,
                                 Response.Listener<String> listener, Response.ErrorListener errorListener) {
            super(method, url, listener, errorListener);
            body = null;
        }

        public void setBody(String body) {
            this.body = body;
        }

        @Override
        public String getBodyContentType() {
            return "application/json; charset=utf-8";
        }

        @Override
        public byte[] getBody() {
            try {
                return body != null ? body.getBytes("utf-8") : null;
            } catch (UnsupportedEncodingException e) {
                return null; // should not reach here...
            }
        }
    }
}
