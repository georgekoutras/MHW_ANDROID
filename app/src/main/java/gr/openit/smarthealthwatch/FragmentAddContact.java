package gr.openit.smarthealthwatch;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.error.AuthFailureError;
import com.android.volley.error.VolleyError;
import com.android.volley.request.StringRequest;
import com.basgeekball.awesomevalidation.AwesomeValidation;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import gr.openit.smarthealthwatch.util.Helpers;
import gr.openit.smarthealthwatch.util.SharedPrefManager;
import gr.openit.smarthealthwatch.util.URLs;
import gr.openit.smarthealthwatch.util.VolleySingleton;

import static com.basgeekball.awesomevalidation.ValidationStyle.BASIC;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link FragmentAddContact#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FragmentAddContact extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    Context mContext;
    View root;
    Spinner occupation;
    Button add;
    String regExEmpty = "^(?=\\s*\\S).*$";
    ProgressDialog pd;
    CheckBox hr, pressure, pulseox, gluce, stress, cough;
    TextView mes_title;
    EditText full_name, email;
    ArrayList<String> monitorTypes;
    UserHome uh;
    ScrollView sv;
    public FragmentAddContact(Context mContext,UserHome uh) {
        // Required empty public constructor
        this.mContext = mContext;
        this.uh = uh;
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment FragmentAddContact.
     */
    // TODO: Rename and change types and number of parameters
    public static FragmentAddContact newInstance(String param1, String param2) {
        FragmentAddContact fragment = new FragmentAddContact(null,null);
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
    public void onResume(){
        super.onResume();
        this.uh.hideMenu();
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        root = inflater.inflate(R.layout.fragment_add_contact, container, false);
        sv = root.findViewById(R.id.edit_account_scrollview);
        occupation = root.findViewById(R.id.choose_occupation);
        add = root.findViewById(R.id.btn_send_invitation);
        ArrayAdapter<String> arrayAdapter2 = new ArrayAdapter<String>(mContext, android.R.layout.simple_spinner_dropdown_item, new ArrayList<String>(Helpers.OCCUPATION_LIST));
        occupation.setAdapter(arrayAdapter2);
        hr = root.findViewById(R.id.heartrate_checkbox);
        pressure = root.findViewById(R.id.presure_checkbox);
        pulseox = root.findViewById(R.id.pulseox_checkbox);
        gluce = root.findViewById(R.id.glucometer_checkbox);
        cough = root.findViewById(R.id.cough_checkbox);
        stress = root.findViewById(R.id.stress_checkbox);

        mes_title = root.findViewById(R.id.reg2_desc2);
        full_name = root.findViewById(R.id.contact_fullname_value);
        email = root.findViewById(R.id.contact_email_value);

        root.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                Helpers.hideKeyboard((MainActivity)getContext());

                return false;
            }

        });

        sv.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                Helpers.hideKeyboard((MainActivity)getContext());

                return false;
            }

        });

        occupation.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                Helpers.hideKeyboard((MainActivity)getContext());

                return false;
            }

        });
        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean valid1 = true, valid2 = true;
                AwesomeValidation mAwesomeValidation = new AwesomeValidation(BASIC);
                mAwesomeValidation.addValidation((Activity) mContext, R.id.contact_fullname_value, regExEmpty, R.string.err_empty);
                mAwesomeValidation.addValidation((Activity) mContext, R.id.contact_email_value, Patterns.EMAIL_ADDRESS, R.string.err_mail);
                if(!hr.isChecked() && !pressure.isChecked() && !pulseox.isChecked() && !gluce.isChecked() && !cough.isChecked() && !stress.isChecked()){
                    valid1 = false;
                    mes_title.setError(getString(R.string.select_one_measurement_error));
                    mes_title.requestFocus();
                }else{
                    mes_title.setError(null);
                }

                if(mAwesomeValidation.validate() && valid1) {
                    sendInvitation();
                }
            }

        });

        return root;
    }

    private void sendInvitation(){
        pd = new ProgressDialog(mContext);
        pd.setMessage(getString(R.string.please_wait));
        pd.show();
        final JSONObject body = new JSONObject();

        int userId = (SharedPrefManager.getInstance(mContext).getUser().getId());
        monitorTypes = new ArrayList<String>();
        if(hr.isChecked())
            monitorTypes.add("HR"); // you can save this as checked somewhere
        if(pressure.isChecked())
            monitorTypes.add("BP"); // you can save this as checked somewhere
        if(pulseox.isChecked())
            monitorTypes.add("O2"); // you can save this as checked somewhere
        if(gluce.isChecked())
            monitorTypes.add("GLU"); // you can save this as checked somewhere
        if(cough.isChecked())
            monitorTypes.add("CGH"); // you can save this as checked somewhere
        if(stress.isChecked())
            monitorTypes.add("STR"); // you can save this as checked somewhere

        JSONArray jsArray = new JSONArray(monitorTypes);

        try {
            body.put("fullName", full_name.getText().toString().trim());
            body.put("email", email.getText().toString().trim());
            body.put("relation",occupation.getSelectedItem().toString().trim());
            body.put("monitorTypes",jsArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }


        StringRequest stringRequest = new StringRequest(Request.Method.POST, (URLs.URL_SEND_INVITATION).replace("{id}", "" + userId),
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        //progressBar.setVisibility(View.GONE);
                        pd.hide();
                        pd.cancel();

                        Toast.makeText(mContext,getString(R.string.contact_delete_success), Toast.LENGTH_LONG).show();
                        getFragmentManager().popBackStackImmediate();

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        pd.hide();
                        pd.cancel();

                        Toast.makeText(mContext, getString(R.string.network_error), Toast.LENGTH_LONG).show();
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
                headers.put("Authorization", "Bearer " + SharedPrefManager.getInstance(mContext).getKeyAccessToken());
                return headers;
            }
        };
        stringRequest.setShouldCache(false);

        VolleySingleton.getInstance(mContext).addToRequestQueue(stringRequest);
    }
}