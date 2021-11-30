package gr.openit.smarthealthwatch;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ProgressBar;
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

import java.text.ParseException;
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
 * Use the {@link FragmentInvitations#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FragmentInvitations extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    Context mContext;
    FragmentContacts fc;
    UserHome uh;
    View root;
    TextView no_in_invitations,no_out_invitations;
    ProgressDialog pd;
    ListView incominglistView, outgoingListView;
    ArrayList<InvitationRow> incomingDataModels, outgoingDataModels;
    private static ListInvitationAdapter incomingAdapter, outgoingAdapter;

    public FragmentInvitations(Context mContext, FragmentContacts fc, UserHome uh) {
        // Required empty public constructor
        this.mContext = mContext;
        this.fc = fc;
        this.uh = uh;
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment FragmentShowInvitations.
     */
    // TODO: Rename and change types and number of parameters
    public static FragmentInvitations newInstance(String param1, String param2) {
        FragmentInvitations fragment = new FragmentInvitations(null,null,null);
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
        getInvitations();
        this.uh.getUnreadMessages();
        this.uh.setHasOptionsMenu(true);
        this.uh.active = this;
        this.uh.showMenu();
        this.uh.showUnity();
    }

    public boolean refresh(){
        getInvitations();
        this.uh.getUnreadMessages();
        return true;
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        root = inflater.inflate(R.layout.fragment_show_invitations, container, false);
        no_in_invitations = root.findViewById(R.id.no_in_invitations_text);
        no_out_invitations = root.findViewById(R.id.no_out_invitations_text);

        return root;
    }

    public void getInvitations(){
        pd = new ProgressDialog(mContext);
        pd.setMessage(getString(R.string.please_wait));
        pd.show();

        String primaryUserInfoUrl = URLs.URL_GET_INVITATIONS.replace("{id}",""+ SharedPrefManager.getInstance(mContext).getUser().getId());
        StringRequest stringRequest = new StringRequest(Request.Method.GET, primaryUserInfoUrl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        pd.hide();
                        pd.cancel();

                        try {
                            JSONArray jsonArray = new JSONArray(response);
                            if(jsonArray.length() == 0){
                                no_in_invitations.setVisibility(View.VISIBLE);
                                no_out_invitations.setVisibility(View.VISIBLE);

                            }else{
                                no_in_invitations.setVisibility(View.GONE);
                                no_out_invitations.setVisibility(View.GONE);
                                showInvitations(jsonArray);
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

    public void showInvitations(JSONArray data){
        incominglistView=(ListView)root.findViewById(R.id.incoming_invitations_list);
        outgoingListView=(ListView)root.findViewById(R.id.outgoing_invitations_list);

        incomingDataModels= new ArrayList<>();
        outgoingDataModels= new ArrayList<>();

        if(data.length() > 0){

            data = sortJsonArray(data);
            for (int i=0; i<data.length(); i++) {
                try {
                    JSONObject invitation = data.getJSONObject(i);

                    String id = String.valueOf(invitation.getInt("id"));
                    Boolean seen = invitation.getBoolean("seen");
                    String relation = invitation.getString("relation");

                    if(invitation.getString("email").equals(SharedPrefManager.getInstance(mContext).getUser().getEmail())) {
                        String sender = invitation.getString("sender");
                        incomingDataModels.add(new InvitationRow(id, sender, relation, seen,true));
                    }else{
                        String sender = invitation.getString("fullName");
                        outgoingDataModels.add(new InvitationRow(id, sender, relation, seen,false));
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        if(incomingDataModels.size() > 0) {
            incomingAdapter = new ListInvitationAdapter(incomingDataModels, mContext, uh, this);
            incominglistView.setAdapter(incomingAdapter);
        }else{
            no_in_invitations.setVisibility(View.VISIBLE);
        }
        if(outgoingDataModels.size() > 0) {
            outgoingAdapter = new ListInvitationAdapter(outgoingDataModels, mContext, uh, this);
            outgoingListView.setAdapter(outgoingAdapter);
        }else{
            no_out_invitations.setVisibility(View.VISIBLE);
        }
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
        this.uh.setHasOptionsMenu(true);
        this.uh.showMenu();
    }

    public boolean onBackPressed(){
        this.fc.toolbarTitleDefault();
        return true;
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