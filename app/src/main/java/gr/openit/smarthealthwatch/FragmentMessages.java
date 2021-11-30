package gr.openit.smarthealthwatch;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gr.openit.smarthealthwatch.util.SharedPrefManager;
import gr.openit.smarthealthwatch.util.URLs;
import gr.openit.smarthealthwatch.util.VolleySingleton;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link FragmentMessages#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FragmentMessages extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    final Context mContext;
    private static ListMessagesAdapter adapter;
    UserHome uh;
    View root;
    ArrayList<MessageRow> dataModels;
    ProgressDialog pd;
    TextView no_messages;
    ListView listView;
    DateFormat df1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    DateFormat df2 = new SimpleDateFormat("dd/MM/yy HH:mm");
    public FragmentMessages(Context mContext, UserHome userHome) {
        // Required empty public constructor
        this.mContext = mContext;
        this.uh = userHome;
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment FragmentMessages.
     */
    // TODO: Rename and change types and number of parameters
    public static FragmentMessages newInstance(String param1, String param2) {
        FragmentMessages fragment = new FragmentMessages(null,null);
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
        getMessages();
        this.uh.setHasOptionsMenu(true);
        this.uh.active = this;
    }

    public boolean refresh(){
        getMessages();
        this.uh.getUnreadMessages();
        return true;
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_messages, container, false);
        no_messages = root.findViewById(R.id.no_messages_text);
        return root;
    }

    public void getMessages(){
        pd = new ProgressDialog(mContext);
        pd.setMessage("Παρακαλώ περιμένετε..");
        pd.show();

        String primaryUserInfoUrl = URLs.URL_GET_MESSAGES.replace("{id}",""+ SharedPrefManager.getInstance(mContext).getUser().getId());
        StringRequest stringRequest = new StringRequest(Request.Method.GET, primaryUserInfoUrl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONArray jsonArray = new JSONArray(response);
                            getAlerts(jsonArray);

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

    public void getAlerts(JSONArray messagesArray){
        String primaryUserInfoUrl = URLs.URL_GET_ALERTS.replace("{id}",""+ SharedPrefManager.getInstance(mContext).getUser().getId());
        StringRequest stringRequest = new StringRequest(Request.Method.GET, primaryUserInfoUrl+"?excludeSeen=true",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        pd.hide();
                        try {
                            JSONArray jsonArray = new JSONArray(response);
                            if(jsonArray.length() == 0 && messagesArray.length() == 0){
                                no_messages.setVisibility(View.VISIBLE);
                            }else{
                                no_messages.setVisibility(View.GONE);
                                showMessages(jsonArray, messagesArray);
                            }
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

    public void showMessages(JSONArray dataAlerts, JSONArray dataMessages){
        listView=(ListView)root.findViewById(R.id.messages_list);
        dataModels= new ArrayList<>();
        if(dataAlerts.length() > 0){

            dataAlerts = sortJsonArray(dataAlerts);
            for (int i=0; i<dataAlerts.length(); i++) {
                try {
                    JSONObject message = dataAlerts.getJSONObject(i);

                    String sender = getString(R.string.smart_device);
                    String id = String.valueOf(message.getInt("id"));
                    String time = message.getString("timeStamp");
                    Boolean read = message.getBoolean("seen");
                    Date result1 = null;
                    String date = null;
                    try {
                        result1 = df1.parse(time);
                        date = df2.format(result1);

                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    dataModels.add(new MessageRow(id,sender,date,read, message, false));

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        if(dataMessages.length() > 0) {
            dataMessages = sortJsonArray(dataMessages);
            for (int i = 0; i < dataMessages.length(); i++) {
                try {
                    JSONObject message = dataMessages.getJSONObject(i);

                    String sender = message.getString("sender");
                    String id = String.valueOf(message.getInt("id"));
                    String time = message.getString("timeStamp");
                    Boolean read = message.getBoolean("read");
                    Date result1 = null;
                    String date = null;
                    try {
                        result1 = df1.parse(time);
                        date = df2.format(result1);

                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    dataModels.add(new MessageRow(id, sender, date, read, message, true));

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        //dataModels.add(new AdviceRow("1","test"));
        //dataModels.add(new AdviceRow("2","test2test2test2test2test2test2test2test2test2test2test2test2test2test2test2test2test2test2"));
        //dataModels.add(new AdviceRow("3","test3"));

        adapter= new ListMessagesAdapter(dataModels,mContext, uh, this);
        listView.setAdapter(adapter);

    }

    public static JSONArray sortJsonArray(JSONArray array) {
        List<JSONObject> jsons = new ArrayList<JSONObject>();
        try {
            for (int i = 0; i < array.length(); i++) {
                jsons.add(array.getJSONObject(i));
            }
            Collections.sort(jsons, new Comparator<JSONObject>() {
                @Override
                public int compare(JSONObject lhs, JSONObject rhs) {
                    String lid = null;
                    String rid = null;
                    try {
                        lid = lhs.getString("timeStamp");
                        rid = rhs.getString("timeStamp");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    return rid.compareTo(lid);

                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return new JSONArray(jsons);
    }

    public void toolbarTitleDefault(){
        this.uh.toolbarTitleDefault();
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