package gr.openit.smarthealthwatch;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.error.AuthFailureError;
import com.android.volley.error.VolleyError;
import com.android.volley.request.StringRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

import gr.openit.smarthealthwatch.util.SharedPrefManager;
import gr.openit.smarthealthwatch.util.URLs;
import gr.openit.smarthealthwatch.util.VolleySingleton;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link FragmentShowAdvice#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FragmentShowAdvice extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    Context mContext;
    String advice_id;
    FragmentAdvices fa;
    ProgressDialog pd;
    JSONObject adviceData;
    View root;
    UserHome uh;

    public FragmentShowAdvice(Context mContext, String id, FragmentAdvices fa,UserHome uh) {
        // Required empty public constructor
        this.mContext = mContext;
        this.advice_id = id;
        this.fa = fa;
        this.uh = uh;
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment FragmentShowAdvice.
     */
    // TODO: Rename and change types and number of parameters
    public static FragmentShowAdvice newInstance(String param1, String param2) {
        FragmentShowAdvice fragment = new FragmentShowAdvice(null,null,null,null);
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
        this.uh.setHasOptionsMenu(false);
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        root = inflater.inflate(R.layout.fragment_show_advice, container, false);
        getAdviceData();
        return root;
    }

    public boolean onBackPressed() {
        this.fa.toolbarTitleDefault();
        return true;
    }

    private void getAdviceData(){
        pd = new ProgressDialog(mContext);
        pd.setMessage(getString(R.string.please_wait));
        pd.show();

        String primaryUserInfoUrl = URLs.URL_GET_ADVICE_DETAILS.replace("{id}", this.advice_id);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, primaryUserInfoUrl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        pd.hide();
                        pd.cancel();

                        try {
                            adviceData = new JSONObject(response);
                            showAdvice(adviceData);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        pd.hide();
                        pd.cancel();

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

    private void showAdvice(JSONObject data){
        TextView title = root.findViewById(R.id.advice_header);
        TextView body = root.findViewById(R.id.advice_body);

        try {
            title.setText(data.getString("title"));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                body.setText(Html.fromHtml(data.getString("text"), Html.FROM_HTML_MODE_COMPACT));
            } else {
                body.setText(Html.fromHtml(data.getString("text")));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
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