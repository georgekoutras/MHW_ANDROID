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
import android.widget.Button;
import android.widget.ListView;
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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import gr.openit.smarthealthwatch.util.SharedPrefManager;
import gr.openit.smarthealthwatch.util.URLs;
import gr.openit.smarthealthwatch.util.User;
import gr.openit.smarthealthwatch.util.VolleySingleton;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link FragmentContacts#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FragmentContacts extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    final Context mContext;
    final UserHome uh;
    View root;
    ProgressDialog pd;
    TextView no_contacts, invitations_title;
    ListView listView;
    private static ListContactAdapter adapter;
    ArrayList<ContactRow> dataModels;
    RelativeLayout invitations_border;
    Button add_contact;
    public FragmentContacts(Context mContext,UserHome uh) {
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
     * @return A new instance of fragment FragmentContacts.
     */
    // TODO: Rename and change types and number of parameters
    public static FragmentContacts newInstance(String param1, String param2) {
        FragmentContacts fragment = new FragmentContacts(null,null);
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

    public boolean refresh(){
        getInvitations();
        this.uh.getUnreadMessages();
        return true;
    }


    @Override
    public void onResume() {
        super.onResume();

        getInvitations();
        this.uh.setHasOptionsMenu(true);
        this.uh.active = this;
        this.uh.showMenu();
        this.uh.toolbarTitleDefault();
        this.uh.showUnity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        root = inflater.inflate(R.layout.fragment_contacts, container, false);
        no_contacts = root.findViewById(R.id.no_contacts_text);
        add_contact = root.findViewById(R.id.btn_add_contact);
        invitations_title = root.findViewById(R.id.invitations_title);
        invitations_border = root.findViewById(R.id.contact_invitations);
        invitations_border.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                invitationsTransition();
            }

        });
        add_contact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addContactTransition();
            }

        });
        return root;
    }

    private void addContactTransition(){
        Fragment addContactFragment = new FragmentAddContact(mContext,this.uh);
        FragmentManager fm = ((MainActivity)mContext).getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
        transaction.replace(R.id.main_container,addContactFragment); // give your fragment container id in first parameter
        transaction.addToBackStack(null);  // if written, this transaction will be added to backstack
        transaction.commit();
        this.uh.active = addContactFragment;
        this.uh.setHasOptionsMenu(false);
        this.uh.hideMenu();
        this.uh.toolbarTitleBack();
        this.uh.hideUnity();
    }

    private void invitationsTransition(){
        Fragment contactsFragment = new FragmentInvitations(mContext,this, uh);
        FragmentManager fm = ((MainActivity)mContext).getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
        transaction.replace(R.id.main_container,contactsFragment,"toBePoped"); // give your fragment container id in first parameter
        transaction.addToBackStack("toBePoped");  // if written, this transaction will be added to backstack
        transaction.commit();
        this.uh.active = contactsFragment;
        this.uh.toolbarTitleBack();
    }

    private void getInvitations(){
        pd = new ProgressDialog(mContext);
        pd.setMessage("Παρακαλώ περιμένετε..");
        pd.show();

        String primaryUserInfoUrl = URLs.URL_GET_INVITATIONS.replace("{id}",""+ SharedPrefManager.getInstance(mContext).getUser().getId());
        StringRequest stringRequest = new StringRequest(Request.Method.GET, primaryUserInfoUrl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONArray jsonArray = new JSONArray(response);
                            if(jsonArray.length()> 0) {
                                int new_invitations = 0;
                                for (int i = 0; i < jsonArray.length(); i++) {
                                    JSONObject x = jsonArray.getJSONObject(i);
                                    if (!x.getBoolean("seen") && x.getString("email").equals(SharedPrefManager.getInstance(mContext).getUser().getEmail())) {
                                        new_invitations++;
                                    }
                                }
                                if (new_invitations == 0) {
                                    invitations_title.setText(getString(R.string.all_contact_invitaions));
                                    invitations_border.setBackground(mContext.getResources().getDrawable(R.drawable.border_shape));
                                } else {
                                    invitations_title.setText(getString(R.string.new_contact_invitations, new_invitations));
                                    invitations_border.setBackground(mContext.getResources().getDrawable(R.drawable.border_shape_unread));
                                }
                            }else{
                                invitations_title.setText(getString(R.string.all_contact_invitaions));
                                invitations_border.setBackground(mContext.getResources().getDrawable(R.drawable.border_shape));

                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        getContacts();
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

    private void getContacts(){
/*
        pd = new ProgressDialog(mContext);
        pd.setMessage("Παρακαλώ περιμένετε..");
        pd.show();
*/

        String primaryUserInfoUrl = URLs.URL_GET_CONTACTS.replace("{id}",""+ SharedPrefManager.getInstance(mContext).getUser().getId());
        StringRequest stringRequest = new StringRequest(Request.Method.GET, primaryUserInfoUrl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        pd.hide();
                        try {
                            JSONArray jsonArray = new JSONArray(response);
                            showContacts(jsonArray);
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

    private void showContacts(JSONArray data){
        if(data.length() > 0){
            no_contacts.setVisibility(View.GONE);
            listView=(ListView)root.findViewById(R.id.contacts_list);
            dataModels= new ArrayList<>();
            for (int i=0; i<data.length(); i++) {
                try {
                    JSONObject contact = data.getJSONObject(i);

                    String name = contact.getString("fullName");
                    String id = String.valueOf(contact.getInt("id"));
                    dataModels.add(new ContactRow(id,name));

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            adapter= new ListContactAdapter(dataModels,mContext, uh, this);
            listView.setAdapter(adapter);
        }else{
            no_contacts.setVisibility(View.VISIBLE);
        }
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