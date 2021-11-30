package gr.openit.smarthealthwatch;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import gr.openit.smarthealthwatch.util.SharedPrefManager;
import gr.openit.smarthealthwatch.util.URLs;
import gr.openit.smarthealthwatch.util.User;
import gr.openit.smarthealthwatch.util.VolleySingleton;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link FragmentShowAccountData#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FragmentShowAccountData extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private Context mContext;
    private String primaryUserInfo = null;
    private View root ;
    ProgressDialog pd;
    UserHome uh;
    public FragmentShowAccountData(Context mContext, UserHome uh) {
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
     * @return A new instance of fragment FragmentShowAccountData.
     */
    // TODO: Rename and change types and number of parameters
    public static FragmentShowAccountData newInstance(String param1, String param2) {
        FragmentShowAccountData fragment = new FragmentShowAccountData(null,null);
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
        pd = new ProgressDialog(mContext);
        pd.setMessage(getString(R.string.please_wait));
        pd.show();
        getPrimaryUserInfo();
        this.uh.hideMenu();
        this.uh.hideUnity();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        root = LayoutInflater.from(mContext).inflate(R.layout.fragment_show_account_data, container, false);
        Button changeData = (Button)root.findViewById(R.id.btn_change_data);

        if(primaryUserInfo != null) {
            showUserData();
        }

        changeData.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                editAccountData();
            }
        });

        return root;
    }

    public void showUserData(){
        TextView firstName = root.findViewById(R.id.first_name_field);
        TextView lastName = root.findViewById(R.id.last_name_field);
        TextView year = root.findViewById(R.id.birth_date_field);
        TextView telephone = root.findViewById(R.id.telephone_field);
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

            showSelectedIndicators(list);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void editAccountData(){
        Fragment accountFragment = new FragmentEditAccountData(mContext,primaryUserInfo,uh);
        FragmentManager fm = getFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
        transaction.replace(R.id.main_container,accountFragment); // give your fragment container id in first parameter
        transaction.addToBackStack(null);
        transaction.commit();
    }

    public void showSelectedIndicators(List<String> list){
        if(list.contains("HR")){
            ((ImageView)root.findViewById(R.id.hearrate_valid)).setImageResource(R.drawable.ic_baseline_check_24);
            ((ImageView)root.findViewById(R.id.hearrate_valid)).setColorFilter(ContextCompat.getColor(mContext, android.R.color.holo_green_dark), android.graphics.PorterDuff.Mode.MULTIPLY);
        }else{
            ((ImageView)root.findViewById(R.id.hearrate_valid)).setImageResource(R.drawable.ic_baseline_clear_24);
            ((ImageView)root.findViewById(R.id.hearrate_valid)).setColorFilter(ContextCompat.getColor(mContext, android.R.color.holo_red_dark), android.graphics.PorterDuff.Mode.MULTIPLY);
        }
        if(list.contains("O2")){
            ((ImageView)root.findViewById(R.id.pulseox_valid)).setImageResource(R.drawable.ic_baseline_check_24);
            ((ImageView)root.findViewById(R.id.pulseox_valid)).setColorFilter(ContextCompat.getColor(mContext, android.R.color.holo_green_dark), android.graphics.PorterDuff.Mode.MULTIPLY);
        }else{
            ((ImageView)root.findViewById(R.id.pulseox_valid)).setImageResource(R.drawable.ic_baseline_clear_24);
            ((ImageView)root.findViewById(R.id.pulseox_valid)).setColorFilter(ContextCompat.getColor(mContext, android.R.color.holo_red_dark), android.graphics.PorterDuff.Mode.MULTIPLY);
        }
        if(list.contains("STR")){
            ((ImageView)root.findViewById(R.id.stress_valid)).setImageResource(R.drawable.ic_baseline_check_24);
            ((ImageView)root.findViewById(R.id.stress_valid)).setColorFilter(ContextCompat.getColor(mContext, android.R.color.holo_green_dark), android.graphics.PorterDuff.Mode.MULTIPLY);
        }else{
            ((ImageView)root.findViewById(R.id.stress_valid)).setImageResource(R.drawable.ic_baseline_clear_24);
            ((ImageView)root.findViewById(R.id.stress_valid)).setColorFilter(ContextCompat.getColor(mContext, android.R.color.holo_red_dark), android.graphics.PorterDuff.Mode.MULTIPLY);
        }
        if(list.contains("CGH")){
            ((ImageView)root.findViewById(R.id.cough_valid)).setImageResource(R.drawable.ic_baseline_check_24);
            ((ImageView)root.findViewById(R.id.cough_valid)).setColorFilter(ContextCompat.getColor(mContext, android.R.color.holo_green_dark), android.graphics.PorterDuff.Mode.MULTIPLY);
        }else{
            ((ImageView)root.findViewById(R.id.cough_valid)).setImageResource(R.drawable.ic_baseline_clear_24);
            ((ImageView)root.findViewById(R.id.cough_valid)).setColorFilter(ContextCompat.getColor(mContext, android.R.color.holo_red_dark), android.graphics.PorterDuff.Mode.MULTIPLY);
        }
        if(list.contains("BP")){
            ((ImageView)root.findViewById(R.id.presure_valid)).setImageResource(R.drawable.ic_baseline_check_24);
            ((ImageView)root.findViewById(R.id.presure_valid)).setColorFilter(ContextCompat.getColor(mContext, android.R.color.holo_green_dark), android.graphics.PorterDuff.Mode.MULTIPLY);
        }else{
            ((ImageView)root.findViewById(R.id.presure_valid)).setImageResource(R.drawable.ic_baseline_clear_24);
            ((ImageView)root.findViewById(R.id.presure_valid)).setColorFilter(ContextCompat.getColor(mContext, android.R.color.holo_red_dark), android.graphics.PorterDuff.Mode.MULTIPLY);
        }
        if(list.contains("GLU")){
            ((ImageView)root.findViewById(R.id.glucometer_valid)).setImageResource(R.drawable.ic_baseline_check_24);
            ((ImageView)root.findViewById(R.id.glucometer_valid)).setColorFilter(ContextCompat.getColor(mContext, android.R.color.holo_green_dark), android.graphics.PorterDuff.Mode.MULTIPLY);
        }else{
            ((ImageView)root.findViewById(R.id.glucometer_valid)).setImageResource(R.drawable.ic_baseline_clear_24);
            ((ImageView)root.findViewById(R.id.glucometer_valid)).setColorFilter(ContextCompat.getColor(mContext, android.R.color.holo_red_dark), android.graphics.PorterDuff.Mode.MULTIPLY);
        }
    }

    private void getPrimaryUserInfo(){

        String primaryUserInfoUrl = URLs.URL_GET_PRIMARY_USER_INFO.replace("{id}",""+SharedPrefManager.getInstance(mContext).getUser().getId());

        StringRequest stringRequest = new StringRequest(Request.Method.GET, primaryUserInfoUrl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        primaryUserInfo = response;
                        pd.hide();
                        pd.cancel();

                        showUserData();
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

    public void userLogin() {
        Fragment loginFragment = new LoginFragment(mContext);
        FragmentManager fm = getFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
        transaction.replace(R.id.main_root,loginFragment); // give your fragment container id in first parameter
        transaction.commit();
    }
}