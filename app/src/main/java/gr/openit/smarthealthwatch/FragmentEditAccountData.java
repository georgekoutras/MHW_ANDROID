package gr.openit.smarthealthwatch;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.error.AuthFailureError;
import com.android.volley.error.VolleyError;
import com.android.volley.request.StringRequest;
import com.basgeekball.awesomevalidation.AwesomeValidation;
import com.google.common.collect.Range;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import gr.openit.smarthealthwatch.util.SharedPrefManager;
import gr.openit.smarthealthwatch.util.URLs;
import gr.openit.smarthealthwatch.util.User;
import gr.openit.smarthealthwatch.util.VolleySingleton;

import static com.basgeekball.awesomevalidation.ValidationStyle.BASIC;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link FragmentEditAccountData#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FragmentEditAccountData extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final Boolean firstOpen = false;
    EditText first_name, last_name, year, phone;
    CheckBox hr, pressure, pulseox, gluce, stress, cough;
    private Context mContext;
    private String primaryUserInfo;
    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    ArrayList<String> monitorTypes;
    ProgressDialog pd;
    UserHome uh;
    private CoughService coughService;
    Intent gattServiceIntent;

    public FragmentEditAccountData(Context mContext, String pui, UserHome uh) {
        // Required empty public constructor
        this.mContext = mContext;
        this.primaryUserInfo = pui;
        this.uh = uh;
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment FragmentEditAccountData.
     */
    // TODO: Rename and change types and number of parameters
    public static FragmentEditAccountData newInstance(String param1, String param2) {
        FragmentEditAccountData fragment = new FragmentEditAccountData(null,null,null);
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    @Override
    public void onResume() {
        super.onResume();
        this.uh.hideMenu();
        this.uh.hideUnity();
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View root ;
        root = LayoutInflater.from(mContext).inflate(R.layout.fragment_edit_account_data, container, false);
        Button changeData = (Button)root.findViewById(R.id.btn_save_account_data);


        if(primaryUserInfo != null) {
            fillInForm(root);
        }

        changeData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String regExEmpty = "^(?=\\s*\\S).*$";
                String regExPhone = "^\\d{10}$";

                year = root.findViewById(R.id.birth_date_field);
                phone = root.findViewById(R.id.telephone_field);

                AwesomeValidation mAwesomeValidation = new AwesomeValidation(BASIC);
                mAwesomeValidation.addValidation((Activity) mContext, R.id.first_name_field, regExEmpty, R.string.err_empty);
                mAwesomeValidation.addValidation((Activity) mContext, R.id.last_name_field, regExEmpty, R.string.err_empty);
                if(!TextUtils.isEmpty(year.getText().toString().trim())) {
                    mAwesomeValidation.addValidation((Activity) mContext, R.id.birth_date_field, Range.closed(1900, Calendar.getInstance().get(Calendar.YEAR)), R.string.err_year);
                }
                if(!TextUtils.isEmpty(phone.getText().toString().trim())) {
                    mAwesomeValidation.addValidation((Activity) mContext, R.id.telephone_field, regExPhone, R.string.err_phone);
                }

                if(mAwesomeValidation.validate()) {
                    //Toast.makeText(mContext,"Register now!",Toast.LENGTH_SHORT).show();
                    first_name = root.findViewById(R.id.first_name_field);
                    last_name = root.findViewById(R.id.last_name_field);

                    hr = root.findViewById(R.id.heartrate_checkbox);
                    pressure = root.findViewById(R.id.presure_checkbox);
                    pulseox = root.findViewById(R.id.pulseox_checkbox);
                    gluce = root.findViewById(R.id.glucometer_checkbox);
                    cough = root.findViewById(R.id.cough_checkbox);
                    stress = root.findViewById(R.id.stress_checkbox);

                    monitorTypes = new ArrayList<String>();
                    if(hr.isChecked())
                        monitorTypes.add("Παλμοί"); // you can save this as checked somewhere
                    if(pressure.isChecked())
                        monitorTypes.add("Πίεση"); // you can save this as checked somewhere
                    if(pulseox.isChecked())
                        monitorTypes.add("Οξυγόνο"); // you can save this as checked somewhere
                    if(gluce.isChecked())
                        monitorTypes.add("Σάκχαρο"); // you can save this as checked somewhere
                    if(cough.isChecked())
                        monitorTypes.add("Βήχας"); // you can save this as checked somewhere
                    if(stress.isChecked())
                        monitorTypes.add("Στρες"); // you can save this as checked somewhere

                    pd = new ProgressDialog(mContext);
                    pd.setMessage("Παρακαλώ περιμένετε..");
                    pd.show();
                    updateUserInfo();
                }
            }
        });

        return root;
    }

    public void fillInForm(View root){
        EditText firstName = root.findViewById(R.id.first_name_field);
        EditText lastName = root.findViewById(R.id.last_name_field);
        EditText year = root.findViewById(R.id.birth_date_field);
        EditText telephone = root.findViewById(R.id.telephone_field);
        hr = root.findViewById(R.id.heartrate_checkbox);
        pressure = root.findViewById(R.id.presure_checkbox);
        pulseox = root.findViewById(R.id.pulseox_checkbox);
        gluce = root.findViewById(R.id.glucometer_checkbox);
        cough = root.findViewById(R.id.cough_checkbox);
        stress = root.findViewById(R.id.stress_checkbox);
        try {

            JSONObject obj = new JSONObject(primaryUserInfo);
            JSONArray monitorTypes = obj.getJSONArray("monitorTypes");
            List<String> list = new ArrayList<>();
            for(int i=0; i <monitorTypes.length(); i++) {
                list.add(monitorTypes.getString(i));
            }
            firstName.setText(obj.getString("firstName"));
            lastName.setText(obj.getString("lastName"));
            if(obj.has("birthYear")) {
                year.setText(obj.getString("birthYear"));
            }
            if(obj.has("phone")) {
                telephone.setText(obj.getString("phone"));
            }
            if(list.contains("Παλμοί")){
                hr.setChecked(true);
            }
            if(list.contains("Οξυγόνο")){
                pulseox.setChecked(true);
            }
            if(list.contains("Στρες")){
                stress.setChecked(true);
            }
            if(list.contains("Βήχας")){
                cough.setChecked(true);
            }
            if(list.contains("Πίεση")){
                pressure.setChecked(true);
            }
            if(list.contains("Σάκχαρο")){
                gluce.setChecked(true);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void updateUserInfo(){
        final JSONObject body = new JSONObject();
        JSONArray jsArray = new JSONArray(monitorTypes);
        try {
            body.put("firstName", first_name.getText().toString());
            body.put("lastName", last_name.getText().toString());
            if(!TextUtils.isEmpty(year.getText().toString().trim())) {
                body.put("birthYear", Integer.parseInt(year.getText().toString()));
            }
            if(!TextUtils.isEmpty(phone.getText().toString().trim())) {
                body.put("phone", phone.getText().toString().trim());
            }
            body.put("monitorTypes",jsArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        StringRequest stringRequest = new StringRequest(Request.Method.PUT, URLs.URL_UPDATE_PRIMARY_USER_INFO.replace("{id}",""+ SharedPrefManager.getInstance(mContext).getUser().getId()),
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        //progressBar.setVisibility(View.GONE);
                        //Toast.makeText(getApplicationContext(), "response "+response, Toast.LENGTH_SHORT).show();
                        pd.hide();
                        Toast.makeText(mContext,"Τα στοιχεία σας ανανεώθηκαν", Toast.LENGTH_LONG).show();
                        Integer year_assigned;
                        User user;
                        if(!TextUtils.isEmpty(year.getText().toString().trim())) {
                            //SharedPrefManager.getInstance(mContext).getUser().setYear(Integer.parseInt(year.getText().toString()));
                            if(!TextUtils.isEmpty(phone.getText().toString().trim())) {
                                user = new User(
                                        SharedPrefManager.getInstance(mContext).getUser().getId(),
                                        first_name.getText().toString(),
                                        last_name.getText().toString(),
                                        SharedPrefManager.getInstance(mContext).getUser().getEmail(),
                                        new HashSet<>(monitorTypes),
                                        Integer.parseInt(year.getText().toString()),
                                        phone.getText().toString().trim()
                                );
                            }else{
                                user = new User(
                                        SharedPrefManager.getInstance(mContext).getUser().getId(),
                                        first_name.getText().toString(),
                                        last_name.getText().toString(),
                                        SharedPrefManager.getInstance(mContext).getUser().getEmail(),
                                        new HashSet<>(monitorTypes),
                                        Integer.parseInt(year.getText().toString())
                                );
                            }
                        }else {
                            if(!TextUtils.isEmpty(phone.getText().toString().trim())) {
                                user = new User(
                                        SharedPrefManager.getInstance(mContext).getUser().getId(),
                                        first_name.getText().toString(),
                                        last_name.getText().toString(),
                                        SharedPrefManager.getInstance(mContext).getUser().getEmail(),
                                        new HashSet<>(monitorTypes),
                                        phone.getText().toString().trim()
                                );
                            }else{
                                user = new User(
                                        SharedPrefManager.getInstance(mContext).getUser().getId(),
                                        first_name.getText().toString(),
                                        last_name.getText().toString(),
                                        SharedPrefManager.getInstance(mContext).getUser().getEmail(),
                                        new HashSet<>(monitorTypes)
                                );
                            }
                        }
                        if(monitorTypes.contains("Βήχας")){
                            coughService = new CoughService();
                            gattServiceIntent = new Intent(mContext, coughService.getClass());
                            getActivity().startService(gattServiceIntent);
                        }else{
                            coughService = new CoughService();
                            gattServiceIntent = new Intent(mContext, coughService.getClass());
                            gattServiceIntent.putExtra("stop_service",true);
                            if (isMyServiceRunning(coughService.getClass())) {
                                getActivity().startService(gattServiceIntent);
                                getActivity().stopService(gattServiceIntent);

                            }
                        }
                        SharedPrefManager.getInstance(mContext).userLogin(user);
                        getFragmentManager().popBackStackImmediate();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        pd.hide();
                        Toast.makeText(mContext, "Παρουσιάστηκε σφάμλα! Παρακαλώ ελένξτε την σύνδεση σας στο διαδίκτυο.", Toast.LENGTH_LONG).show();
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
                headers.put("Authorization", "Bearer " + SharedPrefManager.getInstance(mContext).getKeyAccessToken());
                return headers;
            }
        };
        stringRequest.setShouldCache(false);

        VolleySingleton.getInstance(mContext).addToRequestQueue(stringRequest);
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getActivity().getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                Log.i ("isMyServiceRunning?", true+"");
                return true;
            }
        }
        Log.i ("isMyServiceRunning?", false+"");
        return false;
    }
}