package gr.openit.smarthealthwatch;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.error.AuthFailureError;
import com.android.volley.error.VolleyError;
import com.android.volley.request.StringRequest;
import com.basgeekball.awesomevalidation.AwesomeValidation;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import gr.openit.smarthealthwatch.util.Helpers;
import gr.openit.smarthealthwatch.util.SharedPrefManager;
import gr.openit.smarthealthwatch.util.URLs;
import gr.openit.smarthealthwatch.util.VolleySingleton;

import static com.basgeekball.awesomevalidation.ValidationStyle.BASIC;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link FragmentAddPressure#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FragmentAddPressure extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private Context mContext;
    TextView title;
    ProgressDialog pd;
    String displayDate;
    View root;
    TextView date_field;
    Calendar cal;
    Spinner hour,minutes,type;
    Button add;
    String regExEmpty = "^(?=\\s*\\S).*$";
    EditText pressure_value_high, pressure_value_low;
    UserHome uh;
    public FragmentAddPressure(Context mContext, String displayDate, UserHome uh) {
        this.mContext = mContext;
        this.displayDate = displayDate;
        // Required empty public constructor
        this.uh = uh;
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment FragmentAddPressure.
     */
    // TODO: Rename and change types and number of parameters
    public static FragmentAddPressure newInstance(String param1, String param2) {
        FragmentAddPressure fragment = new FragmentAddPressure(null,null,null);
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
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_add_pressure, container, false);

        cal = Calendar.getInstance();
        add = root.findViewById(R.id.btn_add_measurement);

        date_field = root.findViewById(R.id.measurements_date_field);
        date_field.setText(displayDate);
        date_field.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_baseline_calendar_today_24, 0);
        date_field.setCompoundDrawablePadding(5);

        hour = root.findViewById(R.id.choose_hour);
        minutes = root.findViewById(R.id.choose_minutes);
        pressure_value_high = root.findViewById(R.id.mes_pressure_high);
        pressure_value_low = root.findViewById(R.id.mes_pressure_low);

        pressure_value_low.setFilters(new InputFilter[]{new DecimalDigitsInputFilter(3, 1)});
        pressure_value_high.setFilters(new InputFilter[]{new DecimalDigitsInputFilter(3, 1)});

        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(mContext, android.R.layout.simple_spinner_dropdown_item, new ArrayList<String>(Helpers.HOURS));
        hour.setAdapter(arrayAdapter);

        ArrayAdapter<String> arrayAdapter1 = new ArrayAdapter<String>(mContext, android.R.layout.simple_spinner_dropdown_item, new ArrayList<String>(Helpers.MINUTES));
        minutes.setAdapter(arrayAdapter1);

        SimpleDateFormat timeF = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String []time = timeF.format(Calendar.getInstance().getTime()).split(":");

        int hourPosition = arrayAdapter.getPosition(time[0]);
        hour.setSelection(hourPosition);

        if(Integer.parseInt(time[1]) % 5 == 0 ) {
            int minutePosition = arrayAdapter1.getPosition(time[1]);
            minutes.setSelection(minutePosition);
        }else{
            if(Integer.parseInt(time[1].substring(time[1].length() - 1)) > 5){
                int minutePosition = arrayAdapter1.getPosition(time[1].substring(0, time[1].length() - 1) + "5");
                minutes.setSelection(minutePosition);
            }else {
                int minutePosition = arrayAdapter1.getPosition(time[1].substring(0, time[1].length() - 1) + "0");
                minutes.setSelection(minutePosition);
            }
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
                date_field.setText(displayDate);
                date_field.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_baseline_calendar_today_24, 0);
                date_field.setCompoundDrawablePadding(5);
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

        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean valid1 = true, valid2 = true, valid3 = true;
                AwesomeValidation mAwesomeValidation = new AwesomeValidation(BASIC);
                mAwesomeValidation.addValidation((Activity) mContext, R.id.mes_pressure_high, regExEmpty, R.string.err_empty);
                mAwesomeValidation.addValidation((Activity) mContext, R.id.mes_pressure_low, regExEmpty, R.string.err_empty);

                View selectedView1 = hour.getSelectedView();
                View selectedView2 = minutes.getSelectedView();
                if (selectedView1 != null && selectedView1 instanceof TextView) {
                    TextView selectedTextView = (TextView) selectedView1;
                    if (hour.getSelectedItem().toString().trim().equals("Ώρα")) {
                        String errorString = "Επιλέξτε Ώρα";
                        selectedTextView.setError(errorString);
                        valid1 = false;
                    }
                    else {
                        selectedTextView.setError(null);
                        valid1 = true;
                    }
                }

                if (selectedView2 != null && selectedView2 instanceof TextView) {
                    TextView selectedTextView = (TextView) selectedView2;
                    if (minutes.getSelectedItem().toString().trim().equals("Λεπτά")) {
                        String errorString = "Επιλέξτε Ώρα";
                        selectedTextView.setError(errorString);
                        valid2 = false;
                    }
                    else {
                        selectedTextView.setError(null);
                        valid2 = true;
                    }
                }

                if(!pressure_value_high.getText().toString().equals("") && !pressure_value_low.getText().toString().equals("")){
                    double pres_high, pres_low;
                    if(Double.parseDouble(pressure_value_high.getText().toString()) < 40){
                        pres_high = Double.parseDouble(pressure_value_high.getText().toString()) * 10;
                    }else{
                        pres_high = Double.parseDouble(pressure_value_high.getText().toString());
                    }
                    if(Double.parseDouble(pressure_value_low.getText().toString()) < 40){
                        pres_low = Double.parseDouble(pressure_value_low.getText().toString()) * 10;
                    }else{
                        pres_low = Double.parseDouble(pressure_value_low.getText().toString());
                    }
                    if(pres_high < pres_low) {
                        String errorString = "Η υψηλή πίεση δεν μπορεί να είναι μικρότερη απο τη χαμηλή.";
                        pressure_value_high.setError(errorString);
                        errorString = "Η χαμηλή πίεση δεν μπορεί να είναι μεγαλύτερη απο την υψηλή.";
                        pressure_value_low.setError(errorString);
                        valid3 = false;
                    }else{
                        valid3 = true;
                        pressure_value_high.setError(null);
                        pressure_value_low.setError(null);
                    }
                }else{
                    valid3 = true;
                }
                if(mAwesomeValidation.validate() && valid1 && valid2 && valid3) {
                    pd = new ProgressDialog(mContext);
                    pd.setMessage(getString(R.string.please_wait));
                    pd.show();
                    Helpers.hideKeyboard((MainActivity)getContext());
                    storePressure(displayDate,hour.getSelectedItem().toString().trim(),minutes.getSelectedItem().toString().trim(),pressure_value_high.getText().toString(),pressure_value_low.getText().toString());
                }
            }

        });

        return root;
    }


    public void storePressure(String date, String hour, String minutes, String value_high,String value_low) {
        final JSONObject body = new JSONObject();
        TimeZone tz = TimeZone.getDefault();

        int userId = (SharedPrefManager.getInstance(mContext).getUser().getId());
        try {
            Date date1=new SimpleDateFormat("dd/MM/yyyy").parse(date);

            DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS"); // Quoted "Z" to indicate UTC, no timezone offset
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date1);
            calendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH), Integer.parseInt(hour), Integer.parseInt(minutes), 0);
            String nowAsISO = df.format(new Date(calendar.getTimeInMillis()));
            //if everything is fine
            double pres_high, pres_low;
            if(Double.parseDouble(pressure_value_high.getText().toString()) < 40){
                pres_high = Double.parseDouble(pressure_value_high.getText().toString()) * 10;
            }else{
                pres_high = Double.parseDouble(pressure_value_high.getText().toString());
            }
            if(Double.parseDouble(pressure_value_low.getText().toString()) < 40){
                pres_low = Double.parseDouble(pressure_value_low.getText().toString()) * 10;
            }else{
                pres_low = Double.parseDouble(pressure_value_low.getText().toString());
            }
            try {
                body.put("name", "BP");
                body.put("value", pres_high);
                body.put("extraValue", pres_low);
                body.put("timeStamp", nowAsISO);
                body.put("note",type);
            } catch (JSONException e) {
                e.printStackTrace();
            }

        } catch (ParseException e) {
            e.printStackTrace();
        }


        StringRequest stringRequest = new StringRequest(Request.Method.POST, (URLs.URL_ADD_MEASUREMENT).replace("{id}", "" + userId),
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        //progressBar.setVisibility(View.GONE);
                        pd.hide();
                        pd.cancel();

                        Toast.makeText(mContext,getString(R.string.measurement_add_success), Toast.LENGTH_LONG).show();
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
                return body.toString().getBytes();
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
}

class DecimalDigitsInputFilter implements InputFilter {
    private Pattern mPattern;
    DecimalDigitsInputFilter(int digitsBeforeZero, int digitsAfterZero) {
        mPattern = Pattern.compile("[0-9]{0," + (digitsBeforeZero - 1) + "}+((\\.[0-9]{0," + (digitsAfterZero - 1) + "})?)||(\\.)?");
    }
    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        Matcher matcher = mPattern.matcher(dest);
        if (!matcher.matches())
            return "";
        return null;
    }

}