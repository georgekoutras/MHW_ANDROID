package gr.openit.smarthealthwatch;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.fragment.app.Fragment;

import android.transition.TransitionManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.error.AuthFailureError;
import com.android.volley.error.VolleyError;
import com.android.volley.request.StringRequest;
import com.aurelhubert.ahbottomnavigation.AHBottomNavigation;
import com.aurelhubert.ahbottomnavigation.AHBottomNavigationAdapter;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.unity3d.player.UnityPlayer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gr.openit.smarthealthwatch.util.CircularFrameLayout;
import gr.openit.smarthealthwatch.util.HttpsTrustManager;
import gr.openit.smarthealthwatch.util.SharedPrefManager;
import gr.openit.smarthealthwatch.util.URLs;
import gr.openit.smarthealthwatch.util.VolleySingleton;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link UserHome#newInstance} factory method to
 * create an instance of this fragment.
 */
public class UserHome extends Fragment implements View.OnTouchListener  {

    // TODO: Rename parameter arguments, choose names that match
    Toolbar appToolbar;
    private Context mContext;
    private View root;
    final Fragment fragment1;
    final Fragment fragment2;
    final Fragment fragment3;
    final Fragment fragment4;
    final Fragment fragment5;

    FragmentManager fm;
    Fragment active,active_bottom_nav;
    AHBottomNavigation navigation;

    private String defaultToolbarTitle;
    CircularFrameLayout unityPanel;
    Button dragger;
    UnityPlayer mUnityPlayer;
    AppBarLayout appBar;
    ProgressDialog pd;
    Boolean afterLogin;
    public UserHome(Context mContext, Boolean afterLogin) {
        // Required empty public constructor
        this.mContext = mContext;
        this.afterLogin = afterLogin;
        fm = ((MainActivity)mContext).getSupportFragmentManager();
        fragment1 = new FragmentMeasurements(mContext,this);
        fragment2 = new FragmentIndicators(mContext);
        fragment3 = new FragmentAdvices(mContext,this);
        fragment4 = new FragmentMessages(mContext,this);
        fragment5 = new FragmentContacts(mContext,this);
        this.active = fragment1;
        active_bottom_nav = active;
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment user_home.
     */
    // TODO: Rename and change types and number of parameters
    public static UserHome newInstance(String param1, String param2) {
        UserHome fragment = new UserHome(null,false);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
       // setRetainInstance(true);
        HttpsTrustManager.allowAllSSL();

        fm.beginTransaction().add(R.id.main_container,fragment2, "2").hide(fragment2).commit();
        fm.beginTransaction().add(R.id.main_container,fragment3, "3").hide(fragment3).commit();
        fm.beginTransaction().add(R.id.main_container,fragment4, "4").hide(fragment4).commit();
        fm.beginTransaction().add(R.id.main_container,fragment5, "5").hide(fragment5).commit();
        fm.beginTransaction().add(R.id.main_container,fragment1, "1").commit();

    }

    @Override
    public void onResume(){
        super.onResume();
        showMenu();
        showUnity();
    }

    @SuppressLint("ResourceAsColor")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        super.onCreateView(inflater, container, savedInstanceState);

        setHasOptionsMenu(true);
        //Log.i("testtest",fm.getFragments().toString());


        mUnityPlayer = ((MainActivity)mContext).getUnityPlayer();
        String firstName = SharedPrefManager.getInstance(mContext).getUser().getFirstName();
        root =  inflater.inflate(R.layout.fragment_user_home, container, false);

        appBar = root.findViewById(R.id.appBarLayout);
        appToolbar = (Toolbar) root.findViewById(R.id.toolbar);
        defaultToolbarTitle = "Γεια σας "+firstName;
        TextView toolbarTitle = (TextView) root.findViewById(R.id.toolbarTitle);
        toolbarTitle.setText(defaultToolbarTitle);
        //appToolbar.setTitle(defaultToolbarTitle);
        ((MainActivity)mContext).setSupportActionBar(appToolbar);
        ((MainActivity)mContext).getSupportActionBar().setDisplayShowTitleEnabled(false);
        navigation = (AHBottomNavigation) root.findViewById(R.id.navigation);
        navigation.setTitleState(AHBottomNavigation.TitleState.ALWAYS_SHOW);
        //navigation.setColored(true);
        //navigation.setDefaultBackgroundColor(R.color.basic_color);

        AHBottomNavigationAdapter navigationAdapter = new AHBottomNavigationAdapter((MainActivity)mContext, R.menu.bottom_nav);
        navigationAdapter.setupWithBottomNavigation(navigation, null);
        navigation.setAccentColor(Color.parseColor("#22817b"));
        navigation.setInactiveColor(Color.parseColor("#606060"));
        //navigation.setNotification("2",navigation.getItemsCount()-2);

        //changed BottomNavigationView to AHBottomNavigationView for badges
        navigation.setOnTabSelectedListener(new AHBottomNavigation.OnTabSelectedListener() {
            @Override
            public boolean onTabSelected(int position, boolean wasSelected) {
                //fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                /*for(int entry = 0; entry<fm.getBackStackEntryCount(); entry++){
                    fm.popBackStack();

                }*/

                //fm.popBackStack();
                //active = active_bottom_nav;
                toolbarTitleDefault();
                setHasOptionsMenu(true);
                //Log.i("bottomMenu",""+active);
                //Log.i("bottomMenu",""+active.getTag());
                if(active.getTag() != null && active.getTag().equals("toBePoped")){
                    fm.popBackStack();
                }
                active = active_bottom_nav;

                switch (position) {
                    case 0:
                        fm.beginTransaction().hide(active).commit();
                        fm.beginTransaction().setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out).show(fragment1).commit();
                        active = fragment1;
                        active_bottom_nav = active;
                        return true;

/*                   case R.id.deiktes:
                        fm.beginTransaction().hide(active).show(fragment2).commit();
                        active = fragment2;*/
                    case 1:
                        fm.beginTransaction().hide(active).commit();
                        fm.beginTransaction().setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out).show(fragment3).commit();
                        active = fragment3;
                        active_bottom_nav = active;
                        return true;

                    case 2:
                        fm.beginTransaction().hide(active).commit();
                        fm.beginTransaction().setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out).show(fragment4).commit();
                        active = fragment4;
                        //((FragmentMessages)fragment4).getMessages();
                        active_bottom_nav = active;
                        return true;

                    case 3:
                        fm.beginTransaction().hide(active).commit();
                        fm.beginTransaction().setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out).show(fragment5).commit();
                        active = fragment5;
                        active_bottom_nav = active;
                        return true;

                }
                return false;
            }
        });

        // This code refers to BottomNavigationView from android.material. It works if nav changes from AHBottomNavigationView
        /*navigation.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                active = active_bottom_nav;
                toolbarTitleDefault();
                setHasOptionsMenu(true);

                switch (item.getItemId()) {
                    case R.id.metriseis:
                        fm.beginTransaction().hide(active).show(fragment1).commit();
                        active = fragment1;
                        active_bottom_nav = active;
                        return true;

*//*                    case R.id.deiktes:
                        fm.beginTransaction().hide(active).show(fragment2).commit();
                        active = fragment2;
                        return true;*//*
                    case R.id.simvoules:
                        fm.beginTransaction().hide(active).show(fragment3).commit();
                        active = fragment3;
                        active_bottom_nav = active;
                        return true;
                    case R.id.minimata:
                        fm.beginTransaction().hide(active).show(fragment4).commit();
                        active = fragment4;
                        active_bottom_nav = active;
                        return true;
                    case R.id.epafes:
                        fm.beginTransaction().hide(active).show(fragment5).commit();
                        active = fragment5;
                        active_bottom_nav = active;
                        return true;
                }
                return false;
            }
        });*/

        unityPanel = root.findViewById(R.id.unity);
        //fixed The specified child already has a parent error
        if(mUnityPlayer.getParent() != null) {
            ((ViewGroup)mUnityPlayer.getParent()).removeView(mUnityPlayer); // <- fix
        }
        unityPanel.addView(mUnityPlayer.getView(),
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        dragger = root.findViewById(R.id.dragger);
        goToMainMenu(null);

        mUnityPlayer.requestFocus();


        return root;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // TODO Add your menu entries here
        inflater.inflate(R.menu.settings_menu,menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    public void hideUnity(){ unityPanel.setVisibility(View.GONE);}
    public void showUnity(){ unityPanel.setVisibility(View.VISIBLE);}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //handle menu item clicks
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            userSettings();
        }else if (id == R.id.refresh) {
            List<Fragment> fragmentList = getFragmentManager().getFragments();

            boolean handled = false;
            for(Fragment f : fragmentList) {
                if(f instanceof FragmentHrMeasurements) {
                    handled = ((FragmentHrMeasurements)f).refresh();
                    if(handled) {
                        break;
                    }
                }
                if(f instanceof FragmentPulseoxMeasurements) {
                    handled = ((FragmentPulseoxMeasurements)f).refresh();
                    if(handled) {
                        break;
                    }
                }
                if(f instanceof FragmentGluceMeasurements) {
                    handled = ((FragmentGluceMeasurements)f).refresh();
                    if(handled) {
                        break;
                    }
                }
                if(f instanceof FragmentPresureMeasurements) {
                    handled = ((FragmentPresureMeasurements)f).refresh();
                    if(handled) {
                        break;
                    }
                }
                if(f instanceof FragmentStressMeasurements) {
                    handled = ((FragmentStressMeasurements)f).refresh();
                    if(handled) {
                        break;
                    }
                }
                if(f instanceof FragmentMeasurements && active instanceof FragmentMeasurements) {
                    handled = ((FragmentMeasurements)f).refresh();

                    if(handled) {
                        break;
                    }
                }
                if(f instanceof FragmentAdvices && active instanceof FragmentAdvices) {
                    handled = ((FragmentAdvices)f).refresh();

                    if(handled) {
                        break;
                    }
                }
                if(f instanceof FragmentMessages && active instanceof FragmentMessages) {
                    handled = ((FragmentMessages)f).refresh();

                    if(handled) {
                        break;
                    }
                }
                if(f instanceof FragmentContacts && active instanceof FragmentContacts) {
                    handled = ((FragmentContacts)f).refresh();

                    if(handled) {
                        break;
                    }
                }
                if(f instanceof FragmentInvitations && active instanceof FragmentInvitations) {
                    handled = ((FragmentInvitations)f).refresh();

                    if(handled) {
                        break;
                    }
                }
                if(f instanceof FragmentCoughMeasurements) {
                    handled = ((FragmentCoughMeasurements)f).refresh();
                    if(handled) {
                        break;
                    }
                }
            }
        }

        return super.onOptionsItemSelected(item);
    }

    public void hideMenu(){
        navigation.setVisibility(View.GONE);
        setHasOptionsMenu(false);
    }

    public void showMenu(){
        navigation.setVisibility(View.VISIBLE);
        setHasOptionsMenu(true);

    }

    public void hideToolbar(){
        appBar.setVisibility(View.GONE);
    }

    public void showToolbar(){
        appBar.setVisibility(View.VISIBLE);
    }

    public void toolbarTitleBack(){
        TextView toolbarTitle = (TextView) root.findViewById(R.id.toolbarTitle);
        toolbarTitle.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_baseline_chevron_left_24,0,0,0);
        toolbarTitle.setText(R.string.back);
        toolbarTitle.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
               ((MainActivity)mContext).onBackPressed();
            }
        });

    }

    public void toolbarTitleDefault(){
        appToolbar.setLogo(android.R.color.transparent);
        TextView toolbarTitle = (TextView) root.findViewById(R.id.toolbarTitle);
        String firstName = SharedPrefManager.getInstance(mContext).getUser().getFirstName();
        defaultToolbarTitle = "Γεια σας "+firstName;
        toolbarTitle.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        toolbarTitle.setText(defaultToolbarTitle);
        toolbarTitle.setOnClickListener(null);
    }
    private void userSettings(){
        FragmentSettings myFragment = (FragmentSettings)getFragmentManager().findFragmentByTag("settings");
        if (myFragment == null || !myFragment.isVisible()) {
            Fragment settingsFragment = new FragmentSettings(mContext, this);
            FragmentManager fm = getFragmentManager();
            FragmentTransaction transaction = fm.beginTransaction();
            transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
            transaction.addToBackStack("toBePoped");
            transaction.replace(R.id.main_container,settingsFragment,"settings"); // give your fragment container id in first parameter
            transaction.commit();
            toolbarTitleBack();
            hideMenu();
            hideUnity();
        }

    }


    @SuppressLint("ClickableViewAccessibility")
    public void goToMainMenu(View view) {
        //make unity a small circle and smaller size and on top of everything else
        unityPanel.setCircle(true);
        //TODO set proper elevation value from configuration
        unityPanel.setElevation(2.0f);

        //Load constraints for the unity view to make it a circle and position it
        ConstraintSet set = new ConstraintSet();
        ConstraintLayout layout = root.findViewById(R.id.container);
        set.load(mContext, R.layout.unity_circle_constraints);
        TransitionManager.beginDelayedTransition(layout);
        set.applyTo(layout);
        //change fragment
        loadMainMenu();

        //make unity panel draggable
        dragger.setOnTouchListener(this);
    }

    @SuppressLint("ClickableViewAccessibility")
    public void goToTester(View view) {
        //restore unity size as full screen
        unityPanel.setCircle(false);
        unityPanel.setElevation(0.0f);


        //load initial constraints
        ConstraintSet set = new ConstraintSet();
        ConstraintLayout layout = root.findViewById(R.id.container);
        set.load(mContext, R.layout.unity_square_constraints);
        TransitionManager.beginDelayedTransition(layout);
        set.applyTo(layout);


        //load main menu fragment
        loadTester();

        //make unity non-draggable again
        dragger.setOnTouchListener(null);
    }

    private void loadTester() {

        hideMenu();
        hideToolbar();
        HelperFragment tf = new HelperFragment(mContext,this);
        FragmentManager fm = getFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
        transaction.addToBackStack(null);
        transaction.replace(R.id.main_container,tf,null); // give your fragment container id in first parameter
        transaction.commit();
    }

    private void loadMainMenu() {
        /*MainMenuFragment mf = new MainMenuFragment();
        getSupportFragmentManager().beginTransaction().replace(R.id.content, mf).commit();*/
    }

    private float dX;
    private float dY;
    private int lastAction;
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View view, MotionEvent event) {
        View view1 = (View)view.getParent();
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                dX = view1.getX() - event.getRawX();
                dY = view1.getY() - event.getRawY();
                lastAction = MotionEvent.ACTION_DOWN;
                break;

            case MotionEvent.ACTION_MOVE:
                view1.setY(event.getRawY() + dY);
                view1.setX(event.getRawX() + dX);
                Log.d("XXXXX YYYYY", dX + " " + dY);
                lastAction = MotionEvent.ACTION_MOVE;
                break;

            case MotionEvent.ACTION_UP:
                if (lastAction == MotionEvent.ACTION_DOWN) {
                    goToTester(null);
                }

                break;
            default:
                return false;
        }
        return true;
    }

    public void getUnreadMessages(){
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
                            Integer notification_count = 0;
                            for (int i = 0; i < jsonArray.length(); i++) {
                                if(!jsonArray.getJSONObject(i).getBoolean("read")){
                                    notification_count++;
                                }

                            }
                            getUnreadAlerts(notification_count);
                            /*if(notification_count > 0){
                                navigation.setNotification(String.valueOf(notification_count),navigation.getItemsCount()-2);
                                navigation.setNotificationBackgroundColor(Color.parseColor("#E59400"));
                            }else{
                                navigation.setNotification("",navigation.getItemsCount()-2);
                            }*/
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
        //Log.i("failed",SharedPrefManager.getInstance(mContext).getKeyAccessToken());
        VolleySingleton.getInstance(mContext).addToRequestQueue(stringRequest);
    }

    public void getUnreadAlerts(Integer notificationCnt){

        String primaryUserInfoUrl = URLs.URL_GET_ALERTS.replace("{id}",""+ SharedPrefManager.getInstance(mContext).getUser().getId());
        StringRequest stringRequest = new StringRequest(Request.Method.GET, primaryUserInfoUrl+"?excludeSeen=true",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONArray jsonArray = new JSONArray(response);

                            if(notificationCnt == 0 && jsonArray.length() == 0){
                                navigation.setNotification("",navigation.getItemsCount()-2);
                            }else{
                                navigation.setNotification(String.valueOf(notificationCnt+jsonArray.length()),navigation.getItemsCount()-2);
                                navigation.setNotificationBackgroundColor(Color.parseColor("#E59400"));
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        getUnseenInvitations();
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

    public void getUnseenInvitations() {

        String primaryUserInfoUrl = URLs.URL_GET_INVITATIONS.replace("{id}", "" + SharedPrefManager.getInstance(mContext).getUser().getId());
        StringRequest stringRequest = new StringRequest(Request.Method.GET, primaryUserInfoUrl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        pd.hide();

                        try {
                            JSONArray jsonArray = new JSONArray(response);
                            if (jsonArray.length() > 0) {
                                int new_invitations = 0;
                                for (int i = 0; i < jsonArray.length(); i++) {
                                    JSONObject x = jsonArray.getJSONObject(i);
                                    if (!x.getBoolean("seen") && x.getString("email").equals(SharedPrefManager.getInstance(mContext).getUser().getEmail())) {
                                        new_invitations++;
                                    }
                                }
                                if (new_invitations == 0) {
                                    navigation.setNotification("", navigation.getItemsCount() - 1);
                                } else {
                                    navigation.setNotification(String.valueOf(new_invitations), navigation.getItemsCount() - 1);
                                    navigation.setNotificationBackgroundColor(Color.parseColor("#E59400"));
                                }
                            } else {
                                navigation.setNotification("", navigation.getItemsCount() - 1);
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