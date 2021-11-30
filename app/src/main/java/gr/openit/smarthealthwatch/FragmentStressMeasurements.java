package gr.openit.smarthealthwatch;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.error.AuthFailureError;
import com.android.volley.error.VolleyError;
import com.android.volley.request.StringRequest;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.*;
import com.github.mikephil.charting.model.GradientColor;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import gr.openit.smarthealthwatch.util.SharedPrefManager;
import gr.openit.smarthealthwatch.util.URLs;
import gr.openit.smarthealthwatch.util.VolleySingleton;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link FragmentStressMeasurements#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FragmentStressMeasurements extends Fragment {

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
    TextView date_field, stress_data, stress_level;
    ImageView prevDate, nextDate;
    Calendar cal;
    RelativeLayout stressBody;
    private ValueFormatter xLabelFormatter;
    private DecimalFormat df;
    Integer stress_total=0, stress_count = 0;
    ArrayList<MeasurementRow> dataModels;
    ListView listView;

    private LineChart chart;

    private static ListViewAdapter adapter;
    View root;
    ArrayList<Entry> values = new ArrayList<>();
    List<Integer> listOfColors = new ArrayList<>();
    List<GradientColor> gC = new ArrayList<>();
    Map<Long,String> dataMap = new HashMap<>();
    ToggleButton dayNightBtn;
    JSONObject stressThres = new JSONObject();

    public FragmentStressMeasurements(Context mContext, FragmentMeasurements fm, String displayDate) {
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
     * @return A new instance of fragment FragmentStressMeasurements.
     */
    // TODO: Rename and change types and number of parameters
    public static FragmentStressMeasurements newInstance(String param1, String param2) {
        FragmentStressMeasurements fragment = new FragmentStressMeasurements(null,null,null);
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
                if (type.equals("Στρες")) {
                    stressThres = s;
                    break;
                }
            }
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        getMeasurements(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_stress_measurements, container, false);

        stressBody = root.findViewById(R.id.stress_measurement_body);
        cal = Calendar.getInstance();

        stress_data = root.findViewById(R.id.total_stress_data);
        stress_level = root.findViewById(R.id.total_stress_level);

        title = root.findViewById(R.id.stress_body);
        prevDate = root.findViewById(R.id.prev_date);
        nextDate = root.findViewById(R.id.next_date);

        date_field = root.findViewById(R.id.measurements_date_field);
        dayNightBtn = root.findViewById(R.id.btnDaynight);

        chart = root.findViewById(R.id.chart1);
        xLabelFormatter = new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return formatTiempoHHMM(value);
            }
        };
        df = new DecimalFormat("00");

/*
        Paint paint = chart.getRenderer().getPaintRender();
        int height = 100;
        LinearGradient linGrad = new LinearGradient(0, 0, 0, height,
                ContextCompat.getColor((MainActivity)mContext, R.color.stressNormal),
                ContextCompat.getColor((MainActivity)mContext, R.color.stressHigh),
                Shader.TileMode.CLAMP);
        paint.setShader(linGrad);
*/

        // no description text
        chart.getDescription().setEnabled(false);

        // enable touch gestures
        chart.setTouchEnabled(true);

        chart.setDragDecelerationFrictionCoef(0.9f);

        // enable scaling and dragging
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setScaleYEnabled(false);
        chart.setDrawGridBackground(false);
        chart.setHighlightPerDragEnabled(false);
        //chart.setPinchZoom(true);
        chart.setHighlightPerTapEnabled(false);
        chart.setDoubleTapToZoomEnabled(false);

        // set an alternative background color
        chart.setBackgroundColor(Color.WHITE);
        chart.setViewPortOffsets(50f, 0f, 0f, 50f);
        chart.setVisibleXRangeMaximum(5);

        Legend l = chart.getLegend();
        l.setEnabled(false);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextSize(10f);
        xAxis.setTextColor(Color.BLACK);
        xAxis.setDrawAxisLine(false);
        xAxis.setDrawGridLines(true);
        xAxis.setCenterAxisLabels(true);
        xAxis.setGranularity(10f); // ten seconds show
        xAxis.setValueFormatter(xLabelFormatter);
        xAxis.mAxisRange = 8f;

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
        leftAxis.setTextColor(Color.BLACK);
        leftAxis.setDrawGridLines(false);
        leftAxis.setGranularityEnabled(true);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(100f);
        leftAxis.setYOffset(-9f);
        //leftAxis.setTextColor(Color.rgb(255, 192, 56));

        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setEnabled(false);

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
                dayNightBtn.setChecked(false);
                getMeasurements(false);

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
                    dayNightBtn.setChecked(false);

                    showHideNextDate();
                    getMeasurements(false);

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
                    dayNightBtn.setChecked(false);
                    showHideNextDate();
                    getMeasurements(false);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }

        });

        dayNightBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked)
                {
                    setData(false);
                }
                else
                {
                    setData(true);
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
        getMeasurements(true);
        return true;
    }

    public void getMeasurements(Boolean onRefresh){
        pd = new ProgressDialog(mContext);
        pd.setMessage("Παρακαλώ περιμένετε..");
        pd.show();
        DateFormat startDateFormat = new SimpleDateFormat("yyyy-MM-dd'T"+startTime+"'"); // Quoted "Z" to indicate UTC, no timezone offset
        DateFormat endDateFormat = new SimpleDateFormat("yyyy-MM-dd'T"+endTime+"'"); // Quoted "Z" to indicate UTC, no timezone offset
        try {
            String startForQuery = startDateFormat.format(dateFormat.parse(displayDate));
            String endForQuery = endDateFormat.format(dateFormat.parse(displayDate));

            String primaryUserInfoUrl = URLs.URL_GET_MEASUREMENT.replace("{id}",""+ SharedPrefManager.getInstance(mContext).getUser().getId());
            primaryUserInfoUrl += "?"+URLs.START_TIME+startForQuery+"&"+URLs.END_TIME+endForQuery+"&"+URLs.MEASUREMENT_TYPE+"Στρες";
            StringRequest stringRequest = new StringRequest(Request.Method.GET, primaryUserInfoUrl,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            try {
                                JSONArray jsonArray = new JSONArray(response);
                                if(jsonArray.length() > 0){
                                    title.setText(getString(R.string.stress_measurements_header));
                                    parseData(jsonArray,onRefresh);
                                    stressBody.setVisibility(View.VISIBLE);
                                }else{
                                    title.setText(getString(R.string.no_stress_measurements_header,displayDate));
                                    stressBody.setVisibility(View.GONE);
                                }
                                pd.hide();

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

    private void parseData(JSONArray response, Boolean onRefresh){
        dataMap.clear();
        stress_count = response.length();
        stress_total = 0;
        for(int j=0; j< response.length(); j++){
            try {

                JSONObject s = response.getJSONObject(j);
                String []test = s.getString("timeStamp").split("T"); //split timestamp to get Time of day
                Date dd = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(s.getString("timeStamp")); // format timestamp to Date
                Date dd2 =  new SimpleDateFormat("yyyy-MM-dd").parse(test[0]); // Format timestamp to Date with time 00:00

                long now = dd.getTime();
                long now2 = dd2.getTime();
                long now1 = TimeUnit.MILLISECONDS.toSeconds(now) - TimeUnit.MILLISECONDS.toSeconds(now2); // get the time Difference in millis from 00:00 - value time
                stress_total += Integer.parseInt(s.getString("value"));
                dataMap.put(now1,s.getString("value")); // add all values to Hashmap
            } catch (JSONException | ParseException e) {
                e.printStackTrace();
            }
        }

        stress_data.setText(getString(R.string.stress_day_data,stress_total/stress_count));
        try {
            if(stress_total/stress_count > Float.parseFloat(stressThres.getString("higher"))){
                stress_level.setText(getString(R.string.stress_high));
            }else if(stress_total/stress_count < Float.parseFloat(stressThres.getString("lower"))){
                stress_level.setText(getString(R.string.stress_low));
            }else{
                stress_level.setText(getString(R.string.stress_normal));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        setData(!dayNightBtn.isChecked());
    }
    private void setData(Boolean forDay) {

        values.clear();
        listOfColors.clear();
        Integer startTime, endTime;

        if(forDay){
            startTime = 0;
            endTime = 43200;
        }else{
            startTime = 43200;
            endTime = 86400;
        }

        /*// increment by 1  minute
            for(int i=1; i<=721; i+=60){
                float y = (float) (Math.random() * range);
                tmp = new Entry(i,y);
                addDiffValue(tmp);
        }*/
        Entry tmp;

        for(int i=startTime; i<=endTime; i+=1){
            if(dataMap.containsKey((long)i)){
                tmp = new Entry(i,Float.parseFloat(dataMap.get((long)i)));
            }else{
                tmp = new Entry(i,0);
            }
            addDiffValue(tmp);

        }
        // create a dataset and give it a type
        LineDataSet set1 = new LineDataSet(values, "Stress Levels");
        set1.setAxisDependency(YAxis.AxisDependency.LEFT);
        set1.setMode(LineDataSet.Mode.LINEAR);
        set1.setCubicIntensity(0.2f);
        set1.setLineWidth(1.5f);
        set1.setDrawFilled(false);
        set1.setDrawCircles(false);
        set1.setDrawValues(false);
        set1.setFillAlpha(65);
        set1.setHighLightColor(Color.rgb(244, 117, 117));
        set1.setDrawCircleHole(false);
        set1.setColors(listOfColors);
        //set1.setGradientColors(gC);

        LineData data = new LineData(set1);
        data.setValueTextSize(9f);

        if(!chart.isEmpty()){
            chart.clearValues();
            chart.clear();
        }
        chart.setData(data);

    }

    private void addDiffValue(Entry newEntry){

        if(values.size() > 0) {
            Entry last = values.get(values.size() - 1);
            float limit = 0f;
            if(stressThres.length() > 0) {
                try {
                    limit =  Float.parseFloat(stressThres.getString("higher"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }else {
                limit = 50f;
            }
            Entry temp;

            if (last.getY() < limit && newEntry.getY() > limit) {
                float gradient = (newEntry.getY() - last.getY()) / (newEntry.getX() - last.getX());
                float x_border = last.getX() + ((limit - last.getY()) / gradient);
                temp = new Entry(x_border, limit);
                values.add(temp);

                gC.add(new GradientColor(R.color.colorPrimaryDark,R.color.colorPrimaryDark));
                gC.add(new GradientColor(R.color.colorPrimaryDark,R.color.stressHigh));

                listOfColors.add(Color.rgb(34, 129, 123));
                listOfColors.add(Color.rgb(204,0,0));
            }
            // Vorher größer, jetzt kleiner
            else if (last.getY() > limit && newEntry.getY() < limit) {
                float gradient = (newEntry.getY() - last.getY()) / (newEntry.getX() - last.getX());
                float x_border = last.getX() + ((limit - last.getY()) / gradient);
                temp = new Entry(x_border, limit);

                values.add(temp);
                gC.add(new GradientColor(R.color.stressHigh,R.color.colorPrimaryDark));
                gC.add(new GradientColor(R.color.colorPrimaryDark,R.color.colorPrimaryDark));

                listOfColors.add(Color.rgb(204,0,0));
                listOfColors.add(Color.rgb(34, 129, 123));
            } else if (last.getY() > limit) {
                gC.add(new GradientColor(R.color.stressHigh,R.color.stressHigh));

                listOfColors.add(Color.rgb(204,0,0));
            } else {
                gC.add(new GradientColor(R.color.colorPrimaryDark,R.color.colorPrimaryDark));

                listOfColors.add(Color.rgb(34, 129, 123));
            }
        }
        values.add(newEntry);
    }

    private String formatTiempoHHMM(float value) {
        int minutes = (int)(value / 60f);
        int seconds = ((int)value - minutes * 60);
        int hours = (int)(minutes / 60f);
        minutes = minutes - hours * 60;
        return df.format(hours) + ":" + df.format(minutes) + ":" + df.format(seconds);
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