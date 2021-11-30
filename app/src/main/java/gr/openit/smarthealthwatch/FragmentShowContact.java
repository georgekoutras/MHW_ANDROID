package gr.openit.smarthealthwatch;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
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
 * Use the {@link FragmentShowContact#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FragmentShowContact extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    Context mContext;
    String contact_id;
    FragmentContacts fc;
    View root;
    ProgressDialog pd;
    JSONObject contactData;
    CheckBox hr;
    CheckBox pressure;
    CheckBox pulseox ;
    CheckBox gluce;
    CheckBox cough ;
    CheckBox stress;
    UserHome uh;

    public FragmentShowContact(Context mContext, String id, FragmentContacts fc, UserHome uh) {
        // Required empty public constructor
        this.mContext = mContext;
        this.contact_id = id;
        this.fc = fc;
        this.uh = uh;
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment FragmentShowContact.
     */
    // TODO: Rename and change types and number of parameters
    public static FragmentShowContact newInstance(String param1, String param2) {
        FragmentShowContact fragment = new FragmentShowContact(null,null,null,null);
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getContactData();
        this.uh.hideMenu();
        this.uh.hideUnity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        /// Inflate the layout for this fragment
        root = inflater.inflate(R.layout.fragment_show_contact, container, false);
        Button delete_contact = root.findViewById(R.id.btn_delete_contact);
        Button update_contact = root.findViewById(R.id.btn_update_contact);

        update_contact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateContactInfo();
            }
        });
        delete_contact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(mContext, R.style.LogoutDialog)
                        .setMessage(getString(R.string.delete_contact_ask))
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(R.string.button_yes_gr, new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int whichButton) {
                                deleteContact();
                            }})
                        .setNegativeButton(R.string.button_no_gr, null).show();            }
        });
        return root;
    }

    public boolean onBackPressed() {
        this.fc.toolbarTitleDefault();
        return true;
    }

    private void deleteContact(){
        pd = new ProgressDialog(mContext);
        pd.setMessage("Παρακαλώ περιμένετε..");
        pd.show();
        String primaryUserInfoUrl = URLs.URL_DELETE_CONTACT.replace("{id}",""+ SharedPrefManager.getInstance(mContext).getUser().getId());
        primaryUserInfoUrl = primaryUserInfoUrl.replace("{contact_id}",String.valueOf(contact_id));

        StringRequest stringRequest = new StringRequest(Request.Method.DELETE, primaryUserInfoUrl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        pd.hide();
                        Toast.makeText(mContext,"Η επαφή διαγράφηκε επιτυχώς",Toast.LENGTH_SHORT).show();
                        getFragmentManager().popBackStackImmediate();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        pd.hide();
                        Toast.makeText(mContext, "Παρουσιάστηκε σφάμλα! Παρακαλώ ελένξτε την σύνδεση σας στο διαδίκτυο.\nError "+error.networkResponse.statusCode, Toast.LENGTH_LONG).show();
                        //Toast.makeText(mContext, ""+error.networkResponse.statusCode, Toast.LENGTH_LONG).show();
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

    private void getContactData(){
        pd = new ProgressDialog(mContext);
        pd.setMessage("Παρακαλώ περιμένετε..");
        pd.show();

        String primaryUserInfoUrl = URLs.URL_GET_CONTACT_DETAILS.replace("{id}", String.valueOf(SharedPrefManager.getInstance(mContext).getUser().getId()));
        primaryUserInfoUrl = primaryUserInfoUrl.replace("{contact_id}",this.contact_id);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, primaryUserInfoUrl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        pd.hide();
                        try {
                            contactData = new JSONObject(response);
                            showContact(contactData);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        pd.hide();
                        SharedPrefManager.getInstance(mContext).logout();
                        userLogin();
                        //Toast.makeText(mContext, "Παρουσιάστηκε σφάμλα! Παρακαλώ ελένξτε την σύνδεση σας στο διαδίκτυο.", Toast.LENGTH_LONG).show();
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

    private void showContact(JSONObject data){
        TextView fullname = root.findViewById(R.id.contact_name_field);
        TextView occupation = root.findViewById(R.id.contact_occupation_field);
        TextView phone = root.findViewById(R.id.contact_phone_field);
        TextView email = root.findViewById(R.id.contact_email_field);
        hr = root.findViewById(R.id.heartrate_checkbox);
        pressure = root.findViewById(R.id.presure_checkbox);
        pulseox = root.findViewById(R.id.pulseox_checkbox);
        gluce = root.findViewById(R.id.glucometer_checkbox);
        cough = root.findViewById(R.id.cough_checkbox);
        stress = root.findViewById(R.id.stress_checkbox);
        try {
            fullname.setText(data.getString("fullName"));
            occupation.setText(data.getString("occupation"));
            if(data.has("email")){
                email.setText(data.getString("email"));
            }
            if(data.has("phone")){
                phone.setText(data.getString("phone"));
            }
            JSONArray monitorTypes = data.getJSONArray("monitorTypes");
            List<String> list = new ArrayList<>();
            for(int i=0; i <monitorTypes.length(); i++) {
                list.add(monitorTypes.getString(i));
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

    public void updateContactInfo(){
        pd = new ProgressDialog(mContext);
        pd.setMessage("Παρακαλώ περιμένετε..");
        pd.show();
        final JSONObject body = new JSONObject();
        ArrayList<String> monitorTypes;
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

        JSONArray jsArray = new JSONArray(monitorTypes);
        try {
            body.put("monitorTypes",jsArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String primaryUserInfoUrl = URLs.URL_UPDATE_CONTACT.replace("{id}", String.valueOf(SharedPrefManager.getInstance(mContext).getUser().getId()));
        primaryUserInfoUrl = primaryUserInfoUrl.replace("{contact_id}",this.contact_id);
        StringRequest stringRequest = new StringRequest(Request.Method.PUT, primaryUserInfoUrl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        //progressBar.setVisibility(View.GONE);
                        //Toast.makeText(getApplicationContext(), "response "+response, Toast.LENGTH_SHORT).show();
                        pd.hide();
                        Toast.makeText(mContext,"Τα στοιχεία της επαφής ανανεώθηκαν", Toast.LENGTH_LONG).show();
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

    public void userLogin() {
        Fragment loginFragment = new LoginFragment(mContext);
        FragmentManager fm = getFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
        transaction.replace(R.id.main_root,loginFragment); // give your fragment container id in first parameter
        transaction.commit();
    }
}