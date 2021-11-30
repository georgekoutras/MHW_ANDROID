package gr.openit.smarthealthwatch;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.error.AuthFailureError;
import com.android.volley.error.VolleyError;
import com.android.volley.request.StringRequest;
import com.basgeekball.awesomevalidation.AwesomeValidation;
import com.garmin.health.DeviceManager;
import com.garmin.health.GarminHealthInitializationException;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;

import gr.openit.smarthealthwatch.devices.PairedDevicesDialogFragment;
import gr.openit.smarthealthwatch.ui.HealthSDKManager;
import gr.openit.smarthealthwatch.util.Alarm;
import gr.openit.smarthealthwatch.util.Helpers;
import gr.openit.smarthealthwatch.util.HttpsTrustManager;
import gr.openit.smarthealthwatch.util.SharedPrefManager;
import gr.openit.smarthealthwatch.util.URLs;
import gr.openit.smarthealthwatch.util.User;
import gr.openit.smarthealthwatch.util.VolleySingleton;

import static com.basgeekball.awesomevalidation.ValidationStyle.BASIC;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

public class LoginFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    private static final String TAG = "MainActivity";

    private Context mContext;
    EditText email, password;
    ProgressDialog pd;
    TextView forgot_password;
    private Alarm alarm;


    public LoginFragment(Context mContext) {
        // Required empty public constructor
        this.mContext = mContext;
    }

    // TODO: Rename and change types and number of parameters
    public static LoginFragment newInstance(Context mContext) {
        LoginFragment fragment = new LoginFragment(mContext);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View itemView ;
        itemView = LayoutInflater.from(mContext).inflate(R.layout.fragment_login, container, false);

        Button doLogin = (Button) itemView.findViewById(R.id.btn_login);
        TextView goRegister = (TextView) itemView.findViewById(R.id.goRegister);
        forgot_password = itemView.findViewById(R.id.forgot_password);
        forgot_password.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                forgotPasswordTransition();
            }

        });

        doLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String regExPass = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{6,}$";
                String regExEmpty = "^(?=\\s*\\S).*$";

                AwesomeValidation mAwesomeValidation = new AwesomeValidation(BASIC);
                mAwesomeValidation.addValidation((Activity) mContext, R.id.login_email, Patterns.EMAIL_ADDRESS, R.string.err_mail);
                mAwesomeValidation.addValidation((Activity) mContext, R.id.login_password, regExEmpty, R.string.err_empty);

                if(mAwesomeValidation.validate()) {
                    //Toast.makeText(mContext,"Call login request",Toast.LENGTH_SHORT).show();
                    email = itemView.findViewById(R.id.login_email);
                    password = itemView.findViewById(R.id.login_password);
                    pd = new ProgressDialog(mContext);
                    pd.setMessage("Γίνεται σύνδεση. Παρακαλώ περιμένετε..");
                    pd.show();
                    userLogin(email.getText().toString(), password.getText().toString());
                }
            }

        });

        goRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerTransition();
            }

        });
        // Inflate the layout for this fragment
        FragmentManager fm = getFragmentManager();
        if(fm.getBackStackEntryCount()>0){
            itemView.findViewById(R.id.register_link).setVisibility(View.VISIBLE);
        }
        return itemView;
    }

    private void initializeService(){
        try
        {
            Futures.addCallback(HealthSDKManager.initializeHealthSDK(mContext), new FutureCallback<Boolean>()
            {
                @Override
                public void onSuccess(@Nullable Boolean result)
                {
                    //findViewById(R.id.loadingPanel).setVisibility(View.GONE);
                    getActivity().runOnUiThread(() -> {
                        //Toast.makeText(mContext, "SDK Initialized Successfully", Toast.LENGTH_LONG).show();
                        pd.hide();
                        alarm = new Alarm();
                        alarm.setFragmentActivity(getActivity());
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
                    getActivity().runOnUiThread(() ->
                    {
                        Toast.makeText(mContext, R.string.initialization_failed, Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Garmin Health initialization failed.", t);

                        getActivity().finishAndRemoveTask();
                    });
                }
            }, Executors.newSingleThreadExecutor());
        }
        catch (GarminHealthInitializationException e)
        {
            Toast.makeText(mContext, R.string.initialization_failed, Toast.LENGTH_LONG).show();
            Log.e(TAG, "Garmin Health initialization failed.", e);
            getActivity().finishAndRemoveTask();
        }

    }

    private void checkAlarmUp(){
        boolean alarmUp = (PendingIntent.getBroadcast(mContext, 0,
                new Intent(mContext,Alarm.class),
                PendingIntent.FLAG_NO_CREATE) != null);


        if (alarmUp)
        {
            Toast.makeText(mContext, "Alarm is active!", Toast.LENGTH_SHORT).show();
            //alarm.cancelAlarm(this);
        }else{

            Toast.makeText(mContext, "Alarm is Inactive!", Toast.LENGTH_SHORT).show();
            if(alarm != null && SharedPrefManager.getInstance(mContext).getGarminDeviceAddress() !=null){
                DeviceManager deviceManager = DeviceManager.getDeviceManager();
                try {
                    alarm.setAlarm(mContext,SharedPrefManager.getInstance(mContext).getGarminDeviceAddress(),SharedPrefManager.getInstance(mContext).getGlobalInterval());
                }catch (Exception e){
                    Log.i("alarmError",""+e.getMessage());
                }

            }
        }
    }

    private void userLogin(String email, String password) {
        ///first getting the values
        //final String email = "giakoumakis@openit.gr";
        //final String password = "\"web7689.\"";
        //if everything is fine
        HttpsTrustManager.allowAllSSL();

        StringRequest stringRequest = new StringRequest(Request.Method.POST, URLs.URL_LOGIN+email,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        //progressBar.setVisibility(View.GONE);
                        //Toast.makeText(getApplicationContext(), "response "+response, Toast.LENGTH_SHORT).show();

                        try {
                            JSONObject obj = new JSONObject(response);
                            JSONArray monitorTypes = obj.getJSONArray("monitorTypes");
                            Set<String> list = new HashSet<String>();
                            for(int i=0; i <monitorTypes.length(); i++) {
                                list.add(monitorTypes.getString(i));
                            }
                            User user = new User(
                                    Integer.parseInt(obj.getString("id")),
                                    obj.getString("firstName"),
                                    obj.getString("lastName"),
                                    obj.getString("email"),
                                    list
                            );

                            //storing the user in shared preferences
                            //Toast.makeText(mContext,"User logged successfully",Toast.LENGTH_SHORT).show();
                            getToken(email,password, user);

                        }catch (JSONException e){

                        }

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        pd.hide();
                        Toast.makeText(mContext, "Παρουσιάστηκε πρόβλημα. Παρακαλώ ελέγξτε τα στοιχεία εισόδου σας.", Toast.LENGTH_LONG).show();
                    }
                }


        ) {
            @Override
            public byte[] getBody() throws AuthFailureError {
                String mod_password = "\""+password+"\"";
                return mod_password.getBytes();
            }

            @Override
            public String getBodyContentType() {
                return "application/json";
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Accept","application/json");
                return headers;
            }
        };

        stringRequest.setShouldCache(false);

        VolleySingleton.getInstance(mContext).addToRequestQueue(stringRequest);
    }

    private void getToken(String email, String password, User user) {

        final JSONObject body = new JSONObject();
        try {
            body.put("email", email);
            body.put("password", password);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        //if everything is fine
        HttpsTrustManager.allowAllSSL();

        StringRequest stringRequest = new StringRequest(Request.Method.POST, URLs.URL_ACCESS_TOKEN,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        //progressBar.setVisibility(View.GONE);
                        //Toast.makeText(getApplicationContext(), "response "+response, Toast.LENGTH_SHORT).show();
                        Toast.makeText(mContext,"Επιτυχής είσοδος.",Toast.LENGTH_SHORT).show();
                        if(response.charAt(0) == '\"' && response.charAt(response.length()-1) == '\"'){
                            response = response.substring( 1, response.length() - 1 );
                        }

                        SharedPrefManager.getInstance(mContext).setKeyAccessToken(response);
                        SharedPrefManager.getInstance(mContext).userLogin(user);
                        initializeService();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        pd.hide();
                        Toast.makeText(mContext, "Παρουσιάστηκε πρόβλημα. Παρακαλώ ελέγξτε τα στοιχεία εισόδου σας και την σύνδεση σας στο  διαδίκτυο.", Toast.LENGTH_LONG).show();
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
                headers.put("Accept","application/json");
                return headers;
            }
        };
        stringRequest.setShouldCache(false);

        VolleySingleton.getInstance(mContext).addToRequestQueue(stringRequest);
    }

    private void registerTransition(){
        Fragment registerFragment = new RegisterFragment();
        FragmentManager fm = ((MainActivity)mContext).getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
        transaction.replace(R.id.main_root,registerFragment); // give your fragment container id in first parameter
        transaction.addToBackStack(null);  // if written, this transaction will be added to backstack
        transaction.commit();
    }

    private void forgotPasswordTransition(){
        Fragment forgotPasswordFragment = new FragmentForgotPassword();
        FragmentManager fm = ((MainActivity)mContext).getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
        transaction.replace(R.id.main_root,forgotPasswordFragment); // give your fragment container id in first parameter
        transaction.addToBackStack(null);  // if written, this transaction will be added to backstack
        transaction.commit();
    }

    private void userHomeTransition(){
        Helpers.hideKeyboard((MainActivity)mContext);
        Fragment homeFragment = new UserHome(mContext,true);
        FragmentManager fm = ((MainActivity)mContext).getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
        transaction.replace(R.id.main_root,homeFragment); // give your fragment container id in first parameter
        transaction.commit();
    }

}