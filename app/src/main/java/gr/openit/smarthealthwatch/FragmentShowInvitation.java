package gr.openit.smarthealthwatch;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RelativeLayout;
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
import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gr.openit.smarthealthwatch.util.SharedPrefManager;
import gr.openit.smarthealthwatch.util.URLs;
import gr.openit.smarthealthwatch.util.VolleySingleton;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link FragmentShowInvitation#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FragmentShowInvitation extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    Context mContext;
    String invitation_id;
    FragmentInvitations fi;
    ProgressDialog pd;
    JSONObject invitationData;
    View root;
    RelativeLayout hr;
    RelativeLayout pressure;
    RelativeLayout pulseox ;
    RelativeLayout gluce;
    RelativeLayout cough ;
    RelativeLayout stress;
    Button accept,reject,delete;
    TextView name;
    UserHome uh;
    TextView title_in,title_out;
    final Boolean isIncoming;

    public FragmentShowInvitation(Context mContext, String id, FragmentInvitations fi, UserHome uh, Boolean isIncoming) {
        // Required empty public constructor
        this.mContext = mContext;
        this.invitation_id = id;
        this.fi = fi;
        this.uh = uh;
        this.isIncoming = isIncoming;
    }


    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment FragmentShowInvitation.
     */
    // TODO: Rename and change types and number of parameters
    public static FragmentShowInvitation newInstance(String param1, String param2) {
        FragmentShowInvitation fragment = new FragmentShowInvitation(null,null,null,null,null);
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
        this.uh.hideUnity();
    }

    public boolean onBackPressed() {
        this.fi.toolbarTitleDefault();
        return true;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        root = inflater.inflate(R.layout.fragment_show_invitation, container, false);
        getInvitationData();
        title_in = root.findViewById(R.id.tv_subtitle);
        accept = root.findViewById(R.id.btn_accept_invitation);
        accept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(mContext, R.style.LogoutDialog)
                        .setMessage(getString(R.string.accept_contact_ask,name.getText().toString()))
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(R.string.button_yes_gr, new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int whichButton) {
                                acceptRejectInvitation(true);
                            }})
                        .setNegativeButton(R.string.button_no_gr, null).show();
            }
        });
        reject = root.findViewById(R.id.btn_reject_invitation);
        reject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(mContext, R.style.LogoutDialog)
                        .setMessage(getString(R.string.reject_contact_ask,name.getText().toString()))
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(R.string.button_yes_gr, new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int whichButton) {
                                acceptRejectInvitation(false);
                            }})
                        .setNegativeButton(R.string.button_no_gr, null).show();
            }
        });

        delete = root.findViewById(R.id.btn_delete_invitation);
        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(mContext, R.style.LogoutDialog)
                        .setMessage(getString(R.string.delete_invitation_ask,name.getText().toString()))
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(R.string.button_yes_gr, new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int whichButton) {
                                deleteInvitation();
                            }})
                        .setNegativeButton(R.string.button_no_gr, null).show();
            }
        });

        return root;
    }

    private void deleteInvitation(){
        pd = new ProgressDialog(mContext);
        pd.setMessage(getString(R.string.please_wait));
        pd.show();
        String primaryUserInfoUrl = URLs.URL_DELETE_INVITATION.replace("{id}",""+ SharedPrefManager.getInstance(mContext).getUser().getId());
        primaryUserInfoUrl = primaryUserInfoUrl.replace("{invitation_id}",String.valueOf(invitation_id));

        StringRequest stringRequest = new StringRequest(Request.Method.DELETE, primaryUserInfoUrl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        pd.hide();
                        pd.cancel();

                        Toast.makeText(mContext,getString(R.string.invitation_delete_success),Toast.LENGTH_SHORT).show();
                        getFragmentManager().popBackStackImmediate();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        pd.hide();
                        pd.cancel();

                        Toast.makeText(mContext, getString(R.string.network_error), Toast.LENGTH_LONG).show();
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

    private void acceptRejectInvitation(Boolean accept){
        String primaryUserInfoUrl = URLs.URL_ACCEPT_REJECT_INVITATION.replace("{id}", String.valueOf(SharedPrefManager.getInstance(mContext).getUser().getId()));
        primaryUserInfoUrl = primaryUserInfoUrl.replace("{invitation_id}",this.invitation_id);
        StringRequest stringRequest = new StringRequest(Request.Method.POST, primaryUserInfoUrl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        //progressBar.setVisibility(View.GONE);
                        pd.hide();
                        pd.cancel();

                        if(accept) {
                            Toast.makeText(mContext, "Προστέθηκε στις επαφές σας.", Toast.LENGTH_LONG).show();
                        }else{
                            Toast.makeText(mContext, "Η πρόσκληση διαγράφηκε.", Toast.LENGTH_LONG).show();
                        }

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
                return accept.toString().getBytes();
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

    private void getInvitationData(){
        pd = new ProgressDialog(mContext);
        pd.setMessage(getString(R.string.please_wait));
        pd.show();

        String primaryUserInfoUrl = URLs.URL_GET_INVITATION_DETAILS.replace("{id}", String.valueOf(SharedPrefManager.getInstance(mContext).getUser().getId()));
        primaryUserInfoUrl = primaryUserInfoUrl.replace("{invitation_id}",this.invitation_id);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, primaryUserInfoUrl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        pd.hide();
                        pd.cancel();

                        try {
                            invitationData = new JSONObject(response);
                            showInvitation(invitationData);
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

    private void showInvitation(JSONObject data){
        name = root.findViewById(R.id.name_field);
        TextView relation = root.findViewById(R.id.relation_field);
        //TextView email = root.findViewById(R.id.email_field);
        try {

            if(!isIncoming) {
                reject.setVisibility(View.GONE);
                accept.setVisibility(View.GONE);
                delete.setVisibility(View.VISIBLE);
                title_in.setText(getString(R.string.show_invitation_subtitle_out));
                name.setText(data.getString("fullName"));
            }else{
                title_in.setText(getString(R.string.show_invitation_subtitle));
                name.setText(data.getString("sender"));
            }
            relation.setText(data.getString("relation"));
            //email.setText(data.getString("email"));
            hr = root.findViewById(R.id.hr_mes);
            pressure = root.findViewById(R.id.pressure_mes);
            pulseox = root.findViewById(R.id.pulseox_mes);
            gluce = root.findViewById(R.id.gluce_mes);
            cough = root.findViewById(R.id.cough_mes);
            stress = root.findViewById(R.id.stress_mes);
            JSONArray monitorTypes = data.getJSONArray("monitorTypes");
            List<String> list = new ArrayList<>();
            for(int i=0; i <monitorTypes.length(); i++) {
                list.add(monitorTypes.getString(i));
            }
            if(list.contains("HR")){
                hr.setVisibility(View.VISIBLE);
            }
            if(list.contains("O2")){
                pulseox.setVisibility(View.VISIBLE);
            }
            if(list.contains("STR")){
                stress.setVisibility(View.VISIBLE);
            }
            if(list.contains("CGH")){
                cough.setVisibility(View.VISIBLE);
            }
            if(list.contains("BP")){
                pressure.setVisibility(View.VISIBLE);
            }
            if(list.contains("GLU")){
                gluce.setVisibility(View.VISIBLE);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            if (!data.getBoolean("seen") && isIncoming) {
                markAsRead(String.valueOf(data.getInt("id")));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void markAsRead(String id){
        String primaryUserInfoUrl = URLs.URL_MARKASREAD_INVITATION.replace("{id}", "" + SharedPrefManager.getInstance(mContext).getUser().getId());
        primaryUserInfoUrl = primaryUserInfoUrl.replace("{invitation_id}", id);

        StringRequest stringRequest = new StringRequest(Request.Method.PUT, primaryUserInfoUrl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(mContext, getString(R.string.network_error), Toast.LENGTH_LONG).show();
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
                headers.put("Accept", "application/json");
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