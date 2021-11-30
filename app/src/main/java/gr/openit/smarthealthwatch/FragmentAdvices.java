package gr.openit.smarthealthwatch;

import android.app.ProgressDialog;
import android.content.Context;
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
import com.android.volley.error.NetworkError;
import com.android.volley.error.NoConnectionError;
import com.android.volley.error.ParseError;
import com.android.volley.error.ServerError;
import com.android.volley.error.TimeoutError;
import com.android.volley.error.VolleyError;
import com.android.volley.request.StringRequest;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import gr.openit.smarthealthwatch.util.HttpsTrustManager;
import gr.openit.smarthealthwatch.util.SharedPrefManager;
import gr.openit.smarthealthwatch.util.URLs;
import gr.openit.smarthealthwatch.util.VolleySingleton;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link FragmentAdvices#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FragmentAdvices extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    final Context mContext;
    View root;
    ArrayList<AdviceRow> dataModels;
    ProgressDialog pd;
    ListView listView;
    TextView no_advices;
    private static ListAdviceAdapter adapter;
    UserHome uh;
    public FragmentAdvices(Context mContext, UserHome userHome) {
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
     * @return A new instance of fragment FragmentAdvices.
     */
    // TODO: Rename and change types and number of parameters
    public static FragmentAdvices newInstance(String param1, String param2) {
        FragmentAdvices fragment = new FragmentAdvices(null,null);
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
        getAdvices();
        this.uh.setHasOptionsMenu(true);
        this.uh.active = this;

    }

    public boolean refresh(){
        getAdvices();
        this.uh.getUnreadMessages();
        return true;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_advices, container, false);
        no_advices = root.findViewById(R.id.no_advices_text);
        return root;
    }

    public void getAdvices(){
        pd = new ProgressDialog(mContext);
        pd.setMessage("Παρακαλώ περιμένετε..");
        pd.show();
        //showAdvices(new JSONArray());

        String primaryUserInfoUrl = URLs.URL_GET_ADVICES.replace("{id}",""+ SharedPrefManager.getInstance(mContext).getUser().getId());
        StringRequest stringRequest = new StringRequest(Request.Method.GET, primaryUserInfoUrl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        pd.hide();
                        try {
                            JSONArray jsonArray = new JSONArray(response);
                            if(jsonArray.length() > 0){
                                no_advices.setVisibility(View.GONE);
                                showAdvices(jsonArray);

                            }else{
                                no_advices.setVisibility(View.VISIBLE);
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
        /*                if (error instanceof TimeoutError || error instanceof NoConnectionError) {
                            //This indicates that the reuest has either time out or there is no connection
                            Toast.makeText(mContext, "Παρουσιάστηκε σφάμλα! Παρακαλώ ελέγξτε την σύνδεση σας στο διαδίκτυο.", Toast.LENGTH_SHORT).show();
                        } else if (error instanceof AuthFailureError) {
                            // Error indicating that there was an Authentication Failure while performing the request
                            // tbd if redirects to login fragment
                            Toast.makeText(mContext, "Παρουσιάστηκε σφάμλα! Παρακαλώ ελένξτε την σύνδεση σας στο διαδίκτυο.", Toast.LENGTH_SHORT).show();
                        } else if (error instanceof ServerError) {
                            //Indicates that the server responded with a error response
                            Toast.makeText(mContext, "Παρουσιάστηκε σφάμλα! Παρακαλώ επικοινωνήστε με τον διαχειριστή της εφαρμογής.\nError: ServerError", Toast.LENGTH_SHORT).show();
                        } else if (error instanceof NetworkError) {
                            //Indicates that there was network error while performing the request
                            Toast.makeText(mContext, "Παρουσιάστηκε σφάμλα! Παρακαλώ προσπαθήστε ξανά.\nError: ΝetworkΕrror", Toast.LENGTH_SHORT).show();
                        } else if (error instanceof ParseError) {
                            // Indicates that the server response could not be parsed
                            Toast.makeText(mContext, "Παρουσιάστηκε σφάμλα! Παρακαλώ προσπαθήστε ξανά.\nError: ParseError", Toast.LENGTH_SHORT).show();
                        }*/
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

    public void showAdvices(JSONArray data){
        listView=(ListView)root.findViewById(R.id.advices_measurements_list);
        dataModels= new ArrayList<>();
        for (int i=0; i<data.length(); i++) {
            try {
                JSONObject advice = data.getJSONObject(i);

                String value = advice.getString("title");
                String id = String.valueOf(advice.getInt("id"));
                dataModels.add(new AdviceRow(id,value));

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        //dataModels.add(new AdviceRow("1","test"));
        //dataModels.add(new AdviceRow("2","test2test2test2test2test2test2test2test2test2test2test2test2test2test2test2test2test2test2"));
        //dataModels.add(new AdviceRow("3","test3"));

        adapter= new ListAdviceAdapter(dataModels,mContext, uh, this);
        listView.setAdapter(adapter);

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