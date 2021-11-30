package gr.openit.smarthealthwatch;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.error.AuthFailureError;
import com.android.volley.error.VolleyError;
import com.android.volley.request.JsonObjectRequest;
import com.android.volley.request.StringRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import gr.openit.smarthealthwatch.util.SharedPrefManager;
import gr.openit.smarthealthwatch.util.URLs;
import gr.openit.smarthealthwatch.util.User;
import gr.openit.smarthealthwatch.util.VolleySingleton;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link FragmentShowMessage#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FragmentShowMessage extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    Context mContext;
    String message_data;
    FragmentMessages fm;
    View root;
    DateFormat df1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    DateFormat df2 = new SimpleDateFormat("dd/MM/yy HH:mm");
    Button delete;
    ProgressDialog pd;
    JSONObject message;
    Integer message_id;
    Boolean isMessage;
    UserHome uh;
    public FragmentShowMessage(Context mContext, String data, FragmentMessages fm, Boolean isMessage, UserHome uh) {
        // Required empty public constructor
        this.mContext = mContext;
        this.message_data = data;
        this.fm = fm;
        this.isMessage = isMessage;
        this.uh = uh;
    }
    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment FragmentShowMessage.
     */
    // TODO: Rename and change types and number of parameters
    public static FragmentShowMessage newInstance(String param1, String param2) {
        FragmentShowMessage fragment = new FragmentShowMessage(null,null,null,null,null);
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    public boolean onBackPressed() {
        this.fm.toolbarTitleDefault();
        return true;
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
        root = inflater.inflate(R.layout.fragment_show_message, container, false);
        try {
            message = new JSONObject(message_data);
            message_id = message.getInt("id");
            showMessage();
            delete = (Button) root.findViewById(R.id.btn_delete_message);

            if(isMessage) {

                delete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        new AlertDialog.Builder(mContext, R.style.LogoutDialog)
                                .setMessage(R.string.delete_message_ask)
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .setPositiveButton(R.string.button_yes_gr, new DialogInterface.OnClickListener() {

                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        deleteMessage();
                                    }
                                })
                                .setNegativeButton(R.string.button_no_gr, null).show();
                    }
                });
            }else{
                delete.setVisibility(View.GONE);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return root;
    }

    private void deleteMessage(){
        pd = new ProgressDialog(mContext);
        pd.setMessage(getString(R.string.please_wait));
        pd.show();
        String primaryUserInfoUrl = URLs.URL_DELETE_MESSAGE.replace("{id}",""+ SharedPrefManager.getInstance(mContext).getUser().getId());
        primaryUserInfoUrl = primaryUserInfoUrl.replace("{message_id}",String.valueOf(message_id));

        StringRequest stringRequest = new StringRequest(Request.Method.DELETE, primaryUserInfoUrl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        pd.hide();
                        pd.cancel();

                        Toast.makeText(mContext,getString(R.string.message_delete_success),Toast.LENGTH_SHORT).show();
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

    private void showMessage(){
        TextView title = root.findViewById(R.id.message_header);
        TextView body = root.findViewById(R.id.message_body);
        TextView date = root.findViewById(R.id.message_date);

        try {

            if(!isMessage) {
                title.setText(getResources().getString(R.string.message_from, getResources().getString(R.string.smart_device)));
                String type = message.getString("type");
                String preview = "";
                if(type.equals("BP")){
                    preview += "Πίεση";
                }else if(type.equals("O2")){
                    preview += "Οξυγόνο";
                }else if(type.equals("HR")){
                    preview += "Παλμοί";
                }else if(type.equals("GLU")){
                    preview += "Σάκχαρο";
                }else if(type.equals("STR")){
                    preview += "Στρες";
                }else if(type.equals("CGH")){
                    preview += "Βήχας";
                }
                preview += ": ";
                if(message.has("value")){
                    preview += message.getString("value");
                }
                if(message.has("extraValue")){
                    preview += ", "+ message.getString("extraValue");
                }
                preview += "\n" + message.getString("note");
                body.setText(preview);
                String time = message.getString("timeStamp");
                Date result1 = null;
                String date1 = null;
                try {
                    result1 = df1.parse(time);
                    date1 = df2.format(result1);
                    date.setText(date1);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                if (!message.getBoolean("seen")) {
                    markAsRead(String.valueOf(message.getInt("id")),"alert");
                }
            }else{
                title.setText(getResources().getString(R.string.message_from, message.getString("sender")));
                body.setText(message.getString("text"));
                String time = message.getString("timeStamp");
                Date result1 = null;
                String date1 = null;
                try {
                    result1 = df1.parse(time);
                    date1 = df2.format(result1);
                    date.setText(date1);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                if (!message.getBoolean("read")) {
                    markAsRead(String.valueOf(message.getInt("id")),"message");
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void markAsRead(String id, String type){

        if(type.equals("message")) {
            String primaryUserInfoUrl = URLs.URL_MARKASREAD_MESSAGE.replace("{id}", "" + SharedPrefManager.getInstance(mContext).getUser().getId());
            primaryUserInfoUrl = primaryUserInfoUrl.replace("{message_id}", id);

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
        }else{
            String primaryUserInfoUrl = URLs.URL_MARKASREAD_ALERT.replace("{id}", "" + SharedPrefManager.getInstance(mContext).getUser().getId());
            primaryUserInfoUrl = primaryUserInfoUrl.replace("{alert_id}", id);

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
    }

}