package gr.openit.smarthealthwatch;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
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
import org.w3c.dom.Text;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import gr.openit.smarthealthwatch.util.Helpers;
import gr.openit.smarthealthwatch.util.HttpsTrustManager;
import gr.openit.smarthealthwatch.util.URLs;
import gr.openit.smarthealthwatch.util.User;
import gr.openit.smarthealthwatch.util.VolleySingleton;

import static com.basgeekball.awesomevalidation.ValidationStyle.BASIC;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link FragmentForgotPassword#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FragmentForgotPassword extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    TextView backToLogin,mailSent;
    View root;
    Button forgotPassword;
    RelativeLayout reset_form;
    ProgressDialog pd;
    public FragmentForgotPassword() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment FragmentForgotPassword.
     */
    // TODO: Rename and change types and number of parameters
    public static FragmentForgotPassword newInstance(String param1, String param2) {
        FragmentForgotPassword fragment = new FragmentForgotPassword();
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        root = inflater.inflate(R.layout.fragment_forgot_password, container, false);
        backToLogin = root.findViewById(R.id.goLogin);
        EditText email = root.findViewById(R.id.login_email);
        mailSent = root.findViewById(R.id.mailSent);
        reset_form = root.findViewById(R.id.reset_password_form);
        forgotPassword = root.findViewById(R.id.btn_forgot_password);

        forgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AwesomeValidation mAwesomeValidation = new AwesomeValidation(BASIC);
                mAwesomeValidation.addValidation((Activity)getContext(), R.id.login_email, Patterns.EMAIL_ADDRESS, R.string.err_mail);
                if(mAwesomeValidation.validate()) {
                    Helpers.hideKeyboard((MainActivity) getContext());
                    resetPassword(email.getText().toString());
                }
            }

        });

        backToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fm = getFragmentManager();
                fm.popBackStackImmediate();
            }

        });
        return root;
    }

    private void resetPassword(String email){
        HttpsTrustManager.allowAllSSL();
        pd = new ProgressDialog(getContext());
        pd.setMessage("Παρακαλώ περιμένετε..");
        pd.show();

        StringRequest stringRequest = new StringRequest(Request.Method.POST, URLs.URL_RESET_PASSWORD,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        //progressBar.setVisibility(View.GONE);
                        //Toast.makeText(getApplicationContext(), "response "+response, Toast.LENGTH_SHORT).show();
                        pd.hide();
                        reset_form.setVisibility(View.GONE);
                        mailSent.setText(getString(R.string.email_reset_instructions,email));
                        mailSent.setVisibility(View.VISIBLE);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        pd.hide();
                        Toast.makeText(getContext(), "Παρουσιάστηκε πρόβλημα. Παρακαλώ ελέγξτε τη σύνδεσή σας στο διαδύκτιο.", Toast.LENGTH_LONG).show();
                    }
                }


        ) {
            @Override
            public byte[] getBody() throws AuthFailureError {
                String reset_email = "\""+email+"\"";
                return reset_email.getBytes();
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

        VolleySingleton.getInstance(getContext()).addToRequestQueue(stringRequest);

    }


}