package gr.openit.smarthealthwatch;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;

import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

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
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import gr.openit.smarthealthwatch.util.SharedPrefManager;
import gr.openit.smarthealthwatch.util.URLs;
import gr.openit.smarthealthwatch.util.VolleySingleton;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link FragmentMeasurements#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FragmentMeasurements extends Fragment {
    ProgressDialog pd;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    final Context mContext;
    LinearLayout hr,pulseox, pressure,gluce,stress,cough;
    ImageView prevDate, nextDate;
    private View root;
    String displayDate;
    final String startTime = "00:00:00";
    final String endTime = "23:59:59";
    Calendar cal;
    SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
    TextView title, data_dash;
    LinearLayout title_data;
    private Integer minusDays = 0;
    TextView date_field;
    private UserHome uh;
    Boolean hrEnabled = true, stressEnabled = true, pulseEnabled = true, cougheEnabled = true, gluceEnabled = true, pressureEnabled = true;
    DateFormat df1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    DateFormat onlyTime = new SimpleDateFormat("HH:mm");

    JSONObject hrThres = new JSONObject(), pulseThres  = new JSONObject(), stressThres = new JSONObject(),
            gluceThres = new JSONObject(), pressureThres = new JSONObject(), coughThres = new JSONObject();

    public FragmentMeasurements(Context mContext,UserHome uh) {
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
     * @return A new instance of fragment FragmentMeasurements.
     */
    // TODO: Rename and change types and number of parameters
    public static FragmentMeasurements newInstance(String param1, String param2) {
        FragmentMeasurements fragment = new FragmentMeasurements(null,null);
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
    public void onResume() {
        super.onResume();
        enableDisableMeasurements();
        getThresholds();

        //getUserMeasurements();
        this.uh.getUnreadMessages();
        this.uh.active = this;
    }
    
    public boolean refresh(){
        getUserMeasurements();
        this.uh.getUnreadMessages();
        return true;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        cal = Calendar.getInstance();
        TimeZone tz = cal.getTimeZone();

        //DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"); // Quoted "Z" to indicate UTC, no timezone offset


        DateFormat toShow = new SimpleDateFormat("dd/MM/yyyy");
        toShow.setTimeZone(tz);
        displayDate = toShow.format(new Date());

        root = LayoutInflater.from(mContext).inflate(R.layout.fragment_measurements, container, false);
        hr = root.findViewById(R.id.hr_measurement);
        pulseox = root.findViewById(R.id.pulseox_measurement);
        pressure = root.findViewById(R.id.pressure_measurement);
        gluce = root.findViewById(R.id.gluce_measurement);
        stress = root.findViewById(R.id.stress_measurement);
        cough = root.findViewById(R.id.cough_measurement);

        prevDate = root.findViewById(R.id.prev_date);
        nextDate = root.findViewById(R.id.next_date);

        date_field = root.findViewById(R.id.measurements_date_field);
        try {
            showHideNextDate();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        DatePickerDialog.OnDateSetListener date = new DatePickerDialog.OnDateSetListener() {

            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear,
                                  int dayOfMonth) {
                // TODO Auto-generated method stub
                cal.set(Calendar.YEAR, year);
                cal.set(Calendar.MONTH, monthOfYear);
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                String myFormat = "dd/MM/yyyy"; //In which you need put here
                SimpleDateFormat sdf = new SimpleDateFormat(myFormat);
                displayDate = sdf.format(cal.getTime());
                getUserMeasurements();
                try {
                    showHideNextDate();
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        };

        date_field.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
                    Date d = null;
                    d = dateFormat.parse(displayDate);
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(d);
                    DatePickerDialog datePickerDialog=new DatePickerDialog(getActivity(), date, calendar
                            .get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH));

                    //following line to restrict future date selection
                    datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
                    datePickerDialog.show();
                } catch (ParseException e) {
                    e.printStackTrace();
                }

            }

        });

        prevDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
                try {
                    Date date = dateFormat.parse(displayDate);
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(date);
                    calendar.add(Calendar.DATE, -1);
                    String yesterdayAsString = dateFormat.format(calendar.getTime());
                    displayDate = yesterdayAsString;

                    showHideNextDate();
                    getUserMeasurements();
                } catch (ParseException e) {
                    e.printStackTrace();
                }

            }

        });

        nextDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Date date = dateFormat.parse(displayDate);
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(date);

                    calendar.add(Calendar.DATE, +1);
                    String yesterdayAsString = dateFormat.format(calendar.getTime());
                    displayDate = yesterdayAsString;

                    showHideNextDate();

                    getUserMeasurements();
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }

        });

        hr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hrTransition();
            }

        });
        pulseox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { pulseOxTransition(); }

        });
        pressure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { presureTransition(); }

        });
        gluce.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { gluceTransition(); }

        });
        stress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { stressTransition(); }


        });
        cough.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                coughTransition();
            }

        });
        return root;
    }

    public void getThresholds(){
        ProgressDialog pd1 = new ProgressDialog(mContext);
        pd1.setMessage(getString(R.string.please_wait));
        pd1.show();
        try {
            String primaryUserInfoUrl = URLs.URL_GET_THRESHOLDS.replace("{id}",""+ SharedPrefManager.getInstance(mContext).getUser().getId());
            StringRequest stringRequest = new StringRequest(Request.Method.GET, primaryUserInfoUrl,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            pd1.hide();
                            try {
                                JSONArray jsonArray = new JSONArray(response);
                                if(jsonArray.length() > 0){
                                    Set<JSONObject> list = new HashSet<>();
                                    for(int i=0; i <jsonArray.length(); i++) {
                                        JSONObject jsonobject = jsonArray.getJSONObject(i);
                                        list.add(jsonobject);
                                    }
                                    SharedPrefManager.getInstance(mContext).setThresholds(list);
                                    getUserMeasurements();
                                }else{
                                    Log.i("thresholds", "empty");
                                }

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            pd1.hide();
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
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    private void showHideNextDate() throws ParseException {
        if(isToday(dateFormat.parse(displayDate))){
            nextDate.setVisibility(View.GONE);
            date_field.setText(getString(R.string.today,displayDate));
            date_field.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_baseline_calendar_today_24, 0);
            date_field.setCompoundDrawablePadding(5);

        }else{
            nextDate.setVisibility(View.VISIBLE);
            date_field.setText(displayDate);
            date_field.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_baseline_calendar_today_24, 0);
            date_field.setCompoundDrawablePadding(5);

        }
    }

    private boolean isToday(Date date) {
        Calendar calendar = Calendar.getInstance();
        Calendar toCompare = Calendar.getInstance();
        toCompare.setTimeInMillis(date.getTime());

        return calendar.get(Calendar.YEAR) == toCompare.get(Calendar.YEAR)
                && calendar.get(Calendar.MONTH) == toCompare.get(Calendar.MONTH)
                && calendar.get(Calendar.DAY_OF_MONTH) == toCompare.get(Calendar.DAY_OF_MONTH);
    }

    private void enableDisableMeasurements(){
        Set<String> monitorTypes = SharedPrefManager.getInstance(mContext).getUser().getMonitorTypes();
        if(monitorTypes != null) {
            if (!monitorTypes.contains("HR")) {
                hr.setClickable(false);
                hr.setFocusable(false);
                hr.setBackground(ContextCompat.getDrawable(mContext, R.drawable.border_shape_greyd));
                hr.findViewById(R.id.hr_more).setVisibility(View.GONE);
                TextView hr_text = (TextView) hr.findViewById(R.id.hr_body);
                hr_text.setText(R.string.not_active);
                hrEnabled = false;

            } else {
                hr.setClickable(true);
                hr.setFocusable(true);
                hr.setBackground(ContextCompat.getDrawable(mContext, R.drawable.border_shape));
                hr.findViewById(R.id.hr_more).setVisibility(View.VISIBLE);
                hrEnabled = true;

            }

            if (!monitorTypes.contains("O2")) {
                pulseox.setClickable(false);
                pulseox.setFocusable(false);
                pulseox.setBackground(ContextCompat.getDrawable(mContext, R.drawable.border_shape_greyd));
                pulseox.findViewById(R.id.pulseox_more).setVisibility(View.GONE);
                TextView pulseox_text = (TextView) pulseox.findViewById(R.id.pulseox_body);
                pulseox_text.setText(R.string.not_active);
                pulseEnabled = false;

            } else {
                pulseox.setClickable(true);
                pulseox.setFocusable(true);
                pulseox.setBackground(ContextCompat.getDrawable(mContext, R.drawable.border_shape));
                pulseox.findViewById(R.id.pulseox_more).setVisibility(View.VISIBLE);
                pulseEnabled = true;
            }

            if (!monitorTypes.contains("BP")) {
                pressure.setClickable(false);
                pressure.setFocusable(false);
                pressure.setBackground(ContextCompat.getDrawable(mContext, R.drawable.border_shape_greyd));
                pressure.findViewById(R.id.pressure_more).setVisibility(View.GONE);
                TextView pressure_text = (TextView) pressure.findViewById(R.id.pressure_body);
                pressure_text.setText(R.string.not_active);
                pressureEnabled = false;

            } else {
                pressure.setClickable(true);
                pressure.setFocusable(true);
                pressure.setBackground(ContextCompat.getDrawable(mContext, R.drawable.border_shape));
                pressure.findViewById(R.id.pressure_more).setVisibility(View.VISIBLE);
                pressureEnabled = true;

            }

            if (!monitorTypes.contains("STR")) {
                stress.setClickable(false);
                stress.setFocusable(false);
                stress.setBackground(ContextCompat.getDrawable(mContext, R.drawable.border_shape_greyd));
                stress.findViewById(R.id.stress_more).setVisibility(View.GONE);
                TextView stress_text = (TextView) stress.findViewById(R.id.stress_body);
                stress_text.setText(R.string.not_active);
                stressEnabled = false;

            } else {
                stress.setClickable(true);
                stress.setFocusable(true);
                stress.setBackground(ContextCompat.getDrawable(mContext, R.drawable.border_shape));
                stress.findViewById(R.id.stress_more).setVisibility(View.VISIBLE);
                stressEnabled = true;

            }

            if (!monitorTypes.contains("GLU")) {
                gluce.setClickable(false);
                gluce.setFocusable(false);
                gluce.setBackground(ContextCompat.getDrawable(mContext, R.drawable.border_shape_greyd));
                gluce.findViewById(R.id.gluce_more).setVisibility(View.GONE);
                TextView gluce_text = (TextView) gluce.findViewById(R.id.gluce_body);
                gluce_text.setText(R.string.not_active);
                gluceEnabled = false;

            } else {
                gluce.setClickable(true);
                gluce.setFocusable(true);
                gluce.setBackground(ContextCompat.getDrawable(mContext, R.drawable.border_shape));
                gluce.findViewById(R.id.gluce_more).setVisibility(View.VISIBLE);
                gluceEnabled = true;

            }

            if (!monitorTypes.contains("CGH")) {
                cough.setClickable(false);
                cough.setFocusable(false);
                cough.setBackground(ContextCompat.getDrawable(mContext, R.drawable.border_shape_greyd));
                cough.findViewById(R.id.cough_more).setVisibility(View.GONE);
                TextView cough_text = (TextView) cough.findViewById(R.id.cough_body);
                cough_text.setText(R.string.not_active);
                cougheEnabled = false;

            } else {
                cough.setClickable(true);
                cough.setFocusable(true);
                cough.setBackground(ContextCompat.getDrawable(mContext, R.drawable.border_shape));
                cough.findViewById(R.id.cough_more).setVisibility(View.VISIBLE);
                cougheEnabled = true;
                CoughService coughService;
                Intent gattServiceIntent;
                coughService = new CoughService();
                gattServiceIntent = new Intent(mContext, coughService.getClass());
                getActivity().startService(gattServiceIntent);
            }
        }
    }
    private void stressTransition(){
        Fragment stressFragment = new FragmentStressMeasurements(mContext,this,displayDate,uh);
        FragmentManager fm = ((MainActivity)mContext).getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
        transaction.replace(R.id.main_container,stressFragment,"toBePoped"); // give your fragment container id in first parameter
        transaction.addToBackStack("toBePoped");  // if written, this transaction will be added to backstack
        transaction.commit();
        this.uh.active = stressFragment;
        toolbarTitleBack();
    }

    private void coughTransition(){
        Fragment coughFragment = new FragmentCoughMeasurements(mContext,this,displayDate,uh);
        FragmentManager fm = ((MainActivity)mContext).getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
        transaction.replace(R.id.main_container,coughFragment,"toBePoped"); // give your fragment container id in first parameter
        transaction.addToBackStack("toBePoped");  // if written, this transaction will be added to backstack
        transaction.commit();
        this.uh.active = coughFragment;
        toolbarTitleBack();
    }

    private void hrTransition(){
        Fragment hrFragment = new FragmentHrMeasurements(mContext,this,displayDate,uh);
        FragmentManager fm = ((MainActivity)mContext).getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
        transaction.replace(R.id.main_container,hrFragment,"toBePoped"); // give your fragment container id in first parameter
        transaction.addToBackStack("toBePoped");  // if written, this transaction will be added to backstack
        transaction.commit();
        this.uh.active = hrFragment;
        toolbarTitleBack();
    }

    private void pulseOxTransition(){
        Fragment pulseoxFragment = new FragmentPulseoxMeasurements(mContext,this,displayDate,uh);
        FragmentManager fm = ((MainActivity)mContext).getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
        transaction.replace(R.id.main_container,pulseoxFragment,"toBePoped"); // give your fragment container id in first parameter
        transaction.addToBackStack("toBePoped");  // if written, this transaction will be added to backstack
        transaction.commit();
        this.uh.active = pulseoxFragment;
        toolbarTitleBack();
    }

    private void gluceTransition(){
        Fragment gluceFragment = new FragmentGluceMeasurements(mContext,this,displayDate,uh);
        FragmentManager fm = ((MainActivity)mContext).getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
        transaction.replace(R.id.main_container,gluceFragment,"toBePoped"); // give your fragment container id in first parameter
        transaction.addToBackStack("toBePoped");  // if written, this transaction will be added to backstack
        transaction.commit();
        this.uh.active = gluceFragment;
        toolbarTitleBack();
    }

    private void presureTransition(){
        Fragment presureFragment = new FragmentPresureMeasurements(mContext,this,displayDate,uh);
        FragmentManager fm = ((MainActivity)mContext).getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
        transaction.replace(R.id.main_container,presureFragment,"toBePoped"); // give your fragment container id in first parameter
        transaction.addToBackStack("toBePoped");  // if written, this transaction will be added to backstack
        transaction.commit();
        this.uh.active = presureFragment;
        toolbarTitleBack();
    }

    public void toolbarTitleBack(){
        TextView toolbarTitle = (TextView) ((MainActivity)mContext).findViewById(R.id.toolbarTitle);
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
        Toolbar appToolbar = (Toolbar) ((MainActivity)mContext).findViewById(R.id.toolbar);;
        appToolbar.setLogo(android.R.color.transparent);
        TextView toolbarTitle = (TextView) ((MainActivity)mContext).findViewById(R.id.toolbarTitle);
        String firstName = SharedPrefManager.getInstance(mContext).getUser().getFirstName();
        String defaultToolbarTitle = "Γεια σας "+firstName;
        toolbarTitle.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        toolbarTitle.setText(defaultToolbarTitle);
        toolbarTitle.setOnClickListener(null);
        this.uh.active = this.uh.active_bottom_nav;

    }

    private void getUserMeasurements(){

        pd = new ProgressDialog(mContext);
        pd.setMessage(getString(R.string.please_wait));
        pd.show();
        DateFormat startDateFormat = new SimpleDateFormat("yyyy-MM-dd'T"+startTime+"'"); // Quoted "Z" to indicate UTC, no timezone offset
        DateFormat endDateFormat = new SimpleDateFormat("yyyy-MM-dd'T"+endTime+"'"); // Quoted "Z" to indicate UTC, no timezone offset
        try {
            String startForQuery = startDateFormat.format(dateFormat.parse(displayDate));
            String endForQuery = endDateFormat.format(dateFormat.parse(displayDate));

            String primaryUserInfoUrl = URLs.URL_GET_MEASUREMENT.replace("{id}",""+ SharedPrefManager.getInstance(mContext).getUser().getId());
            primaryUserInfoUrl += "?"+URLs.START_TIME+startForQuery+"&"+URLs.END_TIME+endForQuery;
            StringRequest stringRequest = new StringRequest(Request.Method.GET, primaryUserInfoUrl,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            pd.hide();
                            pd.cancel();

                            try {
                                JSONArray jsonArray = new JSONArray(response);
                                showMeasurementsData(jsonArray);
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
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }


    public void showMeasurementsData(JSONArray data){

        //Log.i("testview",data.toString());
        JSONArray hrData = new JSONArray(), pulseData = new JSONArray(), stressData = new JSONArray(),
                gluceData = new JSONArray(), pressureData = new JSONArray(), coughData = new JSONArray();

        if(data.length() > 0){
            try {
                for (int i = 0; i < data.length(); i++) {
                        JSONObject a = data.getJSONObject(i);
                        String type = a.getString("name");
                        if(type.equals("HR")){
                            hrData.put(a);
                        }else if(type.equals("O2")){
                            pulseData.put(a);
                        }else if(type.equals("CGH")){
                            coughData.put(a);
                        }else if(type.equals("BP")){
                            pressureData.put(a);
                        }else if(type.equals("STR")){
                            stressData.put(a);
                        }else if(type.equals("GLU")){
                            gluceData.put(a);
                        }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
        JSONArray tmpThres = null;
        try {
            tmpThres = new JSONArray(SharedPrefManager.getInstance(mContext).getThresholds());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if(tmpThres != null) {

            for (int i = 0; i < tmpThres.length(); i++) {
                String type = null;
                JSONObject s = null;
                try {
                    s = tmpThres.getJSONObject(i);
                    type = s.getString("name");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (type.equals("HR")) {
                    hrThres = s;
                } else if (type.equals("O2")) {
                    pulseThres = s;
                } else if (type.equals("CGH")) {
                    coughThres = s;
                } else if (type.equals("BP")) {
                    pressureThres = s;
                } else if (type.equals("STR")) {
                    stressThres = s;
                } else if (type.equals("GLU")) {
                    gluceThres = s;
                }
            }
        }
        if(hrEnabled) {
            title = root.findViewById(R.id.hr_body);
            title_data = root.findViewById(R.id.hr_body_data);
            if (hrData.length() > 0) {
                hrData = sortJsonArray(hrData);
                Integer sum=0,avg=0,high=0,low=0;

                for(int i=0; i<hrData.length(); i++){
                    try {
                        JSONObject measurement = hrData.getJSONObject(i);
                        String value = measurement.getString("value");
                        sum += Integer.parseInt(value);

                        if(Integer.parseInt(value) > high) {
                            high = Integer.parseInt(value);
                        }

                        if(low == 0) {
                            low = Integer.parseInt(value);
                        }else if(Integer.parseInt(value) < low) {
                            low = Integer.parseInt(value);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                avg = sum / hrData.length();

                title.setVisibility(View.GONE);
                title_data.setVisibility(View.VISIBLE);
                data_dash = root.findViewById(R.id.hr_highest_value);
                data_dash.setText(String.valueOf(high));
                try {
                    if(hrThres.length() > 0 && Integer.parseInt(hrThres.getString("emergencyHigher")) < high) {
                        data_dash.setTextColor(Color.RED);
                    }else if(hrThres.length() > 0 && Integer.parseInt(hrThres.getString("emergencyLower")) > high) {
                        data_dash.setTextColor(Color.RED);
                    }else{
                        data_dash.setTextColor(Color.BLACK);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                data_dash = root.findViewById(R.id.hr_lowest_value);
                data_dash.setText(String.valueOf(low));
                try {
                    if(hrThres.length() > 0 && Integer.parseInt(hrThres.getString("emergencyHigher")) < low) {
                        data_dash.setTextColor(Color.RED);
                    }else if(hrThres.length() > 0 && Integer.parseInt(hrThres.getString("emergencyLower")) > low) {
                        data_dash.setTextColor(Color.RED);
                    }else{
                        data_dash.setTextColor(Color.BLACK);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                data_dash = root.findViewById(R.id.hr_average_value);
                data_dash.setText(String.valueOf(avg));
                try {
                    if(hrThres.length() > 0 && Integer.parseInt(hrThres.getString("emergencyHigher")) < avg) {
                        data_dash.setTextColor(Color.RED);
                    }else if(hrThres.length() > 0 && Integer.parseInt(hrThres.getString("emergencyLower")) > avg) {
                        data_dash.setTextColor(Color.RED);
                    }else{
                        data_dash.setTextColor(Color.BLACK);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }else {
                title.setVisibility(View.VISIBLE);
                title.setText(getString(R.string.no_measurements));
                title_data.setVisibility(View.GONE);
            }
        }else{
            if (hrData.length() > 0) {
                hr.setClickable(true);
                hr.setFocusable(true);
                hr.findViewById(R.id.hr_activate).setVisibility(View.VISIBLE);
            }else{
                hr.setClickable(false);
                hr.setFocusable(false);
                hr.findViewById(R.id.hr_activate).setVisibility(View.GONE);
            }
        }

        if(pulseEnabled) {
            title = root.findViewById(R.id.pulseox_body);
            title_data = root.findViewById(R.id.pulseox_body_data);
            if (pulseData.length() > 0) {
                pulseData = sortJsonArray(pulseData);
                data_dash = root.findViewById(R.id.pulseox_list_time_first);
                data_dash.setText("");
                data_dash = root.findViewById(R.id.pulseox_list_data_first);
                data_dash.setText("");
                data_dash = root.findViewById(R.id.pulseox_list_time_second);
                data_dash.setText("");
                data_dash = root.findViewById(R.id.pulseox_list_data_second);
                data_dash.setText("");

                for(int i=0; i<pulseData.length(); i++){
                    try {
                        if(i == 0) {
                            data_dash = root.findViewById(R.id.pulseox_list_time_first);
                            String timeStamp = pulseData.getJSONObject(i).getString("timeStamp");
                            Date result1 = df1.parse(timeStamp);
                            String time = onlyTime.format(result1);
                            data_dash.setText(time);
                            data_dash = root.findViewById(R.id.pulseox_list_data_first);
                            data_dash.setText(pulseData.getJSONObject(i).getString("value") +" %");
                            try {
                                if(pulseThres.length() > 0 && Integer.parseInt(pulseThres.getString("emergencyHigher")) < Integer.parseInt(pulseData.getJSONObject(i).getString("value"))) {
                                    data_dash.setTextColor(Color.RED);
                                }else if(pulseThres.length() > 0 && Integer.parseInt(pulseThres.getString("emergencyLower")) > Integer.parseInt(pulseData.getJSONObject(i).getString("value"))) {
                                    data_dash.setTextColor(Color.RED);
                                }else{
                                    data_dash.setTextColor(Color.BLACK);
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }else if (i == 1){
                            data_dash = root.findViewById(R.id.pulseox_list_time_second);
                            String timeStamp = pulseData.getJSONObject(i).getString("timeStamp");
                            Date result1 = df1.parse(timeStamp);
                            String time = onlyTime.format(result1);
                            data_dash.setText(time);
                            data_dash = root.findViewById(R.id.pulseox_list_data_second);
                            data_dash.setText(pulseData.getJSONObject(i).getString("value") +" %");
                            try {
                                if(pulseThres.length() > 0 && Integer.parseInt(pulseThres.getString("emergencyHigher")) < Integer.parseInt(pulseData.getJSONObject(i).getString("value"))) {
                                    data_dash.setTextColor(Color.RED);
                                }else if(pulseThres.length() > 0 && Integer.parseInt(pulseThres.getString("emergencyLower")) > Integer.parseInt(pulseData.getJSONObject(i).getString("value"))) {
                                    data_dash.setTextColor(Color.RED);
                                }else{
                                    data_dash.setTextColor(Color.BLACK);
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }else{
                            break;
                        }
                    } catch (JSONException | ParseException e) {
                        e.printStackTrace();
                    }
                }
                title.setVisibility(View.GONE);
                title_data.setVisibility(View.VISIBLE);
            } else {
                title.setVisibility(View.VISIBLE);
                title.setText(getString(R.string.no_measurements));
                title_data.setVisibility(View.GONE);
            }
        }else{
            if (pulseData.length() > 0) {
                pulseox.setClickable(true);
                pulseox.setFocusable(true);
                pulseox.findViewById(R.id.pulseox_activate).setVisibility(View.VISIBLE);
            }else{
                pulseox.setClickable(false);
                pulseox.setFocusable(false);
                pulseox.findViewById(R.id.pulseox_activate).setVisibility(View.GONE);
            }
        }

        if(cougheEnabled) {
            title = root.findViewById(R.id.cough_body);
            if (coughData.length() > 0) {
                Integer totalCough = 0;
                for(int i=0; i<coughData.length(); i++) {
                    try {
                        Integer val= coughData.getJSONObject(i).getInt("value");
                        totalCough += val;
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                title.setText(getString(R.string.total_cough,totalCough));
            } else {
                title.setVisibility(View.VISIBLE);
                title.setText(getString(R.string.no_measurements));
            }
        }else{
            if (coughData.length() > 0) {
                cough.setClickable(true);
                cough.setFocusable(true);
                cough.findViewById(R.id.cough_activate).setVisibility(View.VISIBLE);
            }else{
                cough.setClickable(false);
                cough.setFocusable(false);
                cough.findViewById(R.id.cough_activate).setVisibility(View.GONE);
            }
        }

        if(pressureEnabled) {
            title = root.findViewById(R.id.pressure_body);
            title_data = root.findViewById(R.id.pressure_body_data);
            if (pressureData.length() > 0) {
                pressureData = sortJsonArray(pressureData);
                data_dash = root.findViewById(R.id.pressure_list_time_first);
                data_dash.setText("");
                data_dash = root.findViewById(R.id.pressure_list_data_first);
                data_dash.setText("");
                data_dash = root.findViewById(R.id.pressure_list_time_second);
                data_dash.setText("");
                data_dash = root.findViewById(R.id.pressure_list_data_second);
                data_dash.setText("");
                for(int i=0; i<pressureData.length(); i++){
                    try {
                        if(i == 0) {
                            data_dash = root.findViewById(R.id.pressure_list_time_first);
                            String timeStamp = pressureData.getJSONObject(i).getString("timeStamp");
                            Date result1 = df1.parse(timeStamp);
                            String time = onlyTime.format(result1);
                            data_dash.setText(time);
                            data_dash = root.findViewById(R.id.pressure_list_data_first);
                            String text2 = pressureData.getJSONObject(i).getString("value") +" / "+pressureData.getJSONObject(i).getString("extraValue");

                            Spannable spannable = new SpannableString(text2);

                            try {
                                if(pressureThres.length() > 0 && Float.parseFloat(pressureThres.getString("emergencyHigher")) < Float.parseFloat(pressureData.getJSONObject(i).getString("value"))) {
                                    spannable.setSpan(new ForegroundColorSpan(Color.RED), 0, pressureData.getJSONObject(i).getString("value").length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                }else if(pressureThres.length() > 0 && Float.parseFloat(pressureThres.getString("emergencyLower")) > Float.parseFloat(pressureData.getJSONObject(i).getString("value"))) {
                                    spannable.setSpan(new ForegroundColorSpan(Color.RED), 0, pressureData.getJSONObject(i).getString("value").length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                }else{
                                    spannable.setSpan(new ForegroundColorSpan(Color.BLACK), 0, pressureData.getJSONObject(i).getString("value").length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                }

                                if(pressureThres.length() > 0 && Float.parseFloat(pressureThres.getString("emergencyHigher")) < Float.parseFloat(pressureData.getJSONObject(i).getString("extraValue"))) {
                                    spannable.setSpan(new ForegroundColorSpan(Color.RED), pressureData.getJSONObject(i).getString("value").length() + 3, text2.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                }else if(pressureThres.length() > 0 && Float.parseFloat(pressureThres.getString("emergencyLower")) > Float.parseFloat(pressureData.getJSONObject(i).getString("extraValue"))) {
                                    spannable.setSpan(new ForegroundColorSpan(Color.RED), pressureData.getJSONObject(i).getString("value").length() + 3, text2.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                }else{
                                    spannable.setSpan(new ForegroundColorSpan(Color.BLACK), pressureData.getJSONObject(i).getString("value").length() + 3, text2.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            data_dash.setText(spannable, TextView.BufferType.SPANNABLE);
                            //data_dash.setText(pressureData.getJSONObject(i).getString("value") +" / "+pressureData.getJSONObject(i).getString("extraValue"));
                        }else if (i == 1){
                            data_dash = root.findViewById(R.id.pressure_list_time_second);
                            String timeStamp = pressureData.getJSONObject(i).getString("timeStamp");
                            Date result1 = df1.parse(timeStamp);
                            String time = onlyTime.format(result1);
                            data_dash.setText(time);
                            data_dash = root.findViewById(R.id.pressure_list_data_second);

                            String text2 = pressureData.getJSONObject(i).getString("value") +" / "+pressureData.getJSONObject(i).getString("extraValue");

                            Spannable spannable = new SpannableString(text2);

                            try {
                                if(pressureThres.length() > 0 && Float.parseFloat(pressureThres.getString("emergencyHigher")) < Float.parseFloat(pressureData.getJSONObject(i).getString("value"))) {
                                    spannable.setSpan(new ForegroundColorSpan(Color.RED), 0, pressureData.getJSONObject(i).getString("value").length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                }else if(pressureThres.length() > 0 && Float.parseFloat(pressureThres.getString("emergencyLower")) > Float.parseFloat(pressureData.getJSONObject(i).getString("value"))) {
                                    spannable.setSpan(new ForegroundColorSpan(Color.RED), 0, pressureData.getJSONObject(i).getString("value").length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                }else{
                                    spannable.setSpan(new ForegroundColorSpan(Color.BLACK), 0, pressureData.getJSONObject(i).getString("value").length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                }

                                if(pressureThres.length() > 0 && Float.parseFloat(pressureThres.getString("emergencyHigher")) < Float.parseFloat(pressureData.getJSONObject(i).getString("extraValue"))) {
                                    spannable.setSpan(new ForegroundColorSpan(Color.RED), pressureData.getJSONObject(i).getString("value").length() + 3, text2.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                }else if(pressureThres.length() > 0 && Float.parseFloat(pressureThres.getString("emergencyLower")) > Float.parseFloat(pressureData.getJSONObject(i).getString("extraValue"))) {
                                    spannable.setSpan(new ForegroundColorSpan(Color.RED), pressureData.getJSONObject(i).getString("value").length() + 3, text2.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                }else{
                                    spannable.setSpan(new ForegroundColorSpan(Color.BLACK), pressureData.getJSONObject(i).getString("value").length() + 3, text2.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            data_dash.setText(spannable, TextView.BufferType.SPANNABLE);
                        }else{
                            break;
                        }
                    } catch (JSONException | ParseException e) {
                        e.printStackTrace();
                    }
                }
                title.setVisibility(View.GONE);
                title_data.setVisibility(View.VISIBLE);
            } else {
                title.setVisibility(View.VISIBLE);
                title.setText(getString(R.string.no_measurements));
                title_data.setVisibility(View.GONE);
            }
        }else {
            if (pressureData.length() > 0) {
                pressure.setClickable(true);
                pressure.setFocusable(true);
                pressure.findViewById(R.id.pressure_activate).setVisibility(View.VISIBLE);
            } else {
                pressure.setClickable(false);
                pressure.setFocusable(false);
                pressure.findViewById(R.id.pressure_activate).setVisibility(View.GONE);
            }
        }

        if(stressEnabled) {
            title = root.findViewById(R.id.stress_body);
            title_data = root.findViewById(R.id.stress_body_data);
            data_dash = root.findViewById(R.id.stress_list_data1);
            String stressType = "";
            if (stressData.length() > 0) {
                stressData = sortJsonArray(stressData);
                Integer sum =0;
                Integer avg = 0;
                try {
                    for(int i=0; i< stressData.length(); i++){
                            sum += Integer.parseInt(stressData.getJSONObject(i).getString("value"));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                avg = sum / stressData.length();

                try {
                    if(stressThres.length() > 0 && Integer.parseInt(stressThres.getString("emergencyHigher")) < avg) {
                        data_dash.setTextColor(Color.RED);
                        stressType = getString(R.string.stress_high);
                    }else if(stressThres.length() > 0 && Integer.parseInt(stressThres.getString("emergencyLower")) > avg) {
                        data_dash.setTextColor(Color.RED);
                        stressType = getString(R.string.stress_low);
                    }else{
                        Log.i("stressThres",stressThres.length()+"");
                        data_dash.setTextColor(Color.BLACK);
                        stressType = getString(R.string.stress_normal);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                data_dash.setText(avg + " / 100 - "+ stressType);
                title.setVisibility(View.GONE);
                title_data.setVisibility(View.VISIBLE);
            } else {
                title.setVisibility(View.VISIBLE);
                title.setText(getString(R.string.no_measurements));
                title_data.setVisibility(View.GONE);
            }
        }else {
            if (stressData.length() > 0) {
                stress.setClickable(true);
                stress.setFocusable(true);
                stress.findViewById(R.id.stress_activate).setVisibility(View.VISIBLE);
            } else {
                stress.setClickable(false);
                stress.setFocusable(false);
                stress.findViewById(R.id.stress_activate).setVisibility(View.GONE);
            }
        }

        if(gluceEnabled) {
            title = root.findViewById(R.id.gluce_body);
            title_data = root.findViewById(R.id.gluce_body_data);
            if (gluceData.length() > 0) {
                gluceData = sortJsonArray(gluceData);
                data_dash = root.findViewById(R.id.gluce_list_time_first);
                data_dash.setText("");
                data_dash = root.findViewById(R.id.gluce_list_data_first);
                data_dash.setText("");
                data_dash = root.findViewById(R.id.gluce_list_time_second);
                data_dash.setText("");
                data_dash = root.findViewById(R.id.gluce_list_data_second);
                data_dash.setText("");
                for(int i=0; i<gluceData.length(); i++){
                    try {
                        if(i == 0) {
                            data_dash = root.findViewById(R.id.gluce_list_time_first);
                            String timeStamp = gluceData.getJSONObject(i).getString("timeStamp");
                            Date result1 = df1.parse(timeStamp);
                            String time = onlyTime.format(result1);
                            data_dash.setText(time);
                            data_dash = root.findViewById(R.id.gluce_list_data_first);
                            try {
                                if(gluceThres.length() > 0 && Integer.parseInt(gluceThres.getString("emergencyHigher")) < Integer.parseInt(gluceData.getJSONObject(i).getString("value"))) {
                                    data_dash.setTextColor(Color.RED);
                                }else if(gluceThres.length() > 0 && Integer.parseInt(gluceThres.getString("emergencyLower")) > Integer.parseInt(gluceData.getJSONObject(i).getString("value"))) {
                                    data_dash.setTextColor(Color.RED);
                                }else{
                                    data_dash.setTextColor(Color.BLACK);
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            data_dash.setText(gluceData.getJSONObject(i).getString("value"));
                        }else if (i == 1){
                            data_dash = root.findViewById(R.id.gluce_list_time_second);
                            String timeStamp = gluceData.getJSONObject(i).getString("timeStamp");
                            Date result1 = df1.parse(timeStamp);
                            String time = onlyTime.format(result1);
                            data_dash.setText(time);
                            data_dash = root.findViewById(R.id.gluce_list_data_second);
                            try {
                                if(gluceThres.length() > 0 && Integer.parseInt(gluceThres.getString("emergencyHigher")) < Integer.parseInt(gluceData.getJSONObject(i).getString("value"))) {
                                    data_dash.setTextColor(Color.RED);
                                }else if(gluceThres.length() > 0 && Integer.parseInt(gluceThres.getString("emergencyLower")) > Integer.parseInt(gluceData.getJSONObject(i).getString("value"))) {
                                    data_dash.setTextColor(Color.RED);
                                }else{
                                    data_dash.setTextColor(Color.BLACK);
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            data_dash.setText(gluceData.getJSONObject(i).getString("value"));
                        }else{
                            break;
                        }
                    } catch (JSONException | ParseException e) {
                        e.printStackTrace();
                    }
                }
                title.setVisibility(View.GONE);
                title_data.setVisibility(View.VISIBLE);
            } else {
                title.setVisibility(View.VISIBLE);
                title.setText(getString(R.string.no_measurements));
                title_data.setVisibility(View.GONE);
            }
        }else {
            if (gluceData.length() > 0) {
                gluce.setClickable(true);
                gluce.setFocusable(true);
                gluce.findViewById(R.id.gluce_activate).setVisibility(View.VISIBLE);
            } else {
                gluce.setClickable(false);
                gluce.setFocusable(false);
                gluce.findViewById(R.id.gluce_activate).setVisibility(View.GONE);
            }
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

    public void userLogin() {
        Fragment loginFragment = new LoginFragment(mContext);
        FragmentManager fm = getFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
        transaction.replace(R.id.main_root,loginFragment); // give your fragment container id in first parameter
        transaction.commit();
    }
}