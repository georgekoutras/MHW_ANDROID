package gr.openit.smarthealthwatch;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.ImageView;
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
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import gr.openit.smarthealthwatch.util.Helpers;
import gr.openit.smarthealthwatch.util.SharedPrefManager;
import gr.openit.smarthealthwatch.util.URLs;
import gr.openit.smarthealthwatch.util.VolleySingleton;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link FragmentPulseoxMeasurements#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FragmentPulseoxMeasurements extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private FragmentMeasurements fm;
    private Context mContext;
    TextView title;
    ProgressDialog pd;
    String displayDate;
    SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
    final String startTime = "00:00:00";
    final String endTime = "23:59:59";
    TextView date_field;
    ImageView prevDate, nextDate;
    Calendar cal;
    RelativeLayout pulseoxBody;

    ArrayList<MeasurementRow> dataModels;
    ListView listView;
    private static ListViewAdapter adapter;
    View root;
    DateFormat df1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    DateFormat onlyTime = new SimpleDateFormat("HH:mm");

    public FragmentPulseoxMeasurements(Context mContext, FragmentMeasurements fm, String displayDate) {
        this.mContext = mContext;
        this.fm = fm;
        this.displayDate = displayDate;
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment FragmentPulseoxMeasurements.
     */
    // TODO: Rename and change types and number of parameters
    public static FragmentPulseoxMeasurements newInstance(String param1, String param2) {
        FragmentPulseoxMeasurements fragment = new FragmentPulseoxMeasurements(null,null,null);
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
        getMeasurements();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_pulseox_measurements, container, false);
        pulseoxBody = root.findViewById(R.id.pulseox_measurement_body);
        cal = Calendar.getInstance();

        title = root.findViewById(R.id.pulseox_body);
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
                getMeasurements();

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
                    getMeasurements();

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
                    getMeasurements();
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }

        });

        return root;
    }

    public boolean onBackPressed() {
        this.fm.toolbarTitleDefault();
        return true;
    }

    public boolean refresh(){
        getMeasurements();
        return true;
    }

    public void getMeasurements(){
        pd = new ProgressDialog(mContext);
        pd.setMessage("???????????????? ????????????????????..");
        pd.show();
        DateFormat startDateFormat = new SimpleDateFormat("yyyy-MM-dd'T"+startTime+"'"); // Quoted "Z" to indicate UTC, no timezone offset
        DateFormat endDateFormat = new SimpleDateFormat("yyyy-MM-dd'T"+endTime+"'"); // Quoted "Z" to indicate UTC, no timezone offset
        try {
            String startForQuery = startDateFormat.format(dateFormat.parse(displayDate));
            String endForQuery = endDateFormat.format(dateFormat.parse(displayDate));

            String primaryUserInfoUrl = URLs.URL_GET_MEASUREMENT.replace("{id}",""+ SharedPrefManager.getInstance(mContext).getUser().getId());
            primaryUserInfoUrl += "?"+URLs.START_TIME+startForQuery+"&"+URLs.END_TIME+endForQuery+"&"+URLs.MEASUREMENT_TYPE+"??????????????";
            StringRequest stringRequest = new StringRequest(Request.Method.GET, primaryUserInfoUrl,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            pd.hide();
                            try {
                                JSONArray jsonArray = new JSONArray(response);
                                if(jsonArray.length() > 0){
                                    title.setText(getString(R.string.pulseox_measurements_header));
                                    showPulseOxData(jsonArray);
                                    pulseoxBody.setVisibility(View.VISIBLE);
                                }else{
                                    title.setText(getString(R.string.no_pulseox_measurements_header,displayDate));
                                    pulseoxBody.setVisibility(View.GONE);
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
                            //Toast.makeText(mContext, "?????????????????????????? ????????????! ???????????????? ?????????????? ?????? ?????????????? ?????? ?????? ??????????????????.", Toast.LENGTH_LONG).show();
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

    public void showPulseOxData(JSONArray data){
        listView=(ListView)root.findViewById(R.id.pulseox_measurements_list);
        dataModels= new ArrayList<>();
        data = Helpers.sortJsonArray(data);
        for (int i=0; i<data.length(); i++) {
            try {
                JSONObject measurement = data.getJSONObject(i);
                String timeStamp = measurement.getString("timeStamp");
                Date result1 = df1.parse(timeStamp);

                String time = onlyTime.format(result1);
                String value = measurement.getString("value");
                dataModels.add(new MeasurementRow("-1",time,value));

            } catch (JSONException | ParseException e) {
                e.printStackTrace();
            }
        }
        adapter= new ListViewAdapter(dataModels,mContext,null,1);
        listView.setAdapter(adapter);

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