package gr.openit.smarthealthwatch;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

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
import com.garmin.health.database.dtos.HeartRateLog;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import gr.openit.smarthealthwatch.util.Helpers;
import gr.openit.smarthealthwatch.util.SharedPrefManager;
import gr.openit.smarthealthwatch.util.URLs;
import gr.openit.smarthealthwatch.util.VolleySingleton;

import static com.basgeekball.awesomevalidation.ValidationStyle.BASIC;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link FragmentAddGluce#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FragmentAddGluce extends Fragment {

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
    View root;
    TextView date_field;
    Calendar cal;
    Spinner hour,minutes,type;
    Button add;
    String regExEmpty = "^(?=\\s*\\S).*$";
    EditText gluce_value;
    String type_value, time_value;
    UserHome uh;
    public FragmentAddGluce(Context mContext,String displayDate, UserHome uh) {
        this.mContext = mContext;
        this.uh = uh;
        this.displayDate = displayDate;
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment FragmentAddGluce.
     */
    // TODO: Rename and change types and number of parameters
    public static FragmentAddGluce newInstance(String param1, String param2) {
        FragmentAddGluce fragment = new FragmentAddGluce(null,null,null);
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
        root = inflater.inflate(R.layout.fragment_add_gluce, container, false);
        cal = Calendar.getInstance();
        add = root.findViewById(R.id.btn_add_measurement);

        date_field = root.findViewById(R.id.measurements_date_field);
        date_field.setText(displayDate);
        date_field.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_baseline_calendar_today_24, 0);
        date_field.setCompoundDrawablePadding(5);

        hour = root.findViewById(R.id.choose_hour);
        minutes = root.findViewById(R.id.choose_minutes);
        type = root.findViewById(R.id.choose_gluce_type);
        gluce_value = root.findViewById(R.id.mes_gluce_value);

        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(mContext, android.R.layout.simple_spinner_dropdown_item, new ArrayList<String>(Helpers.HOURS));
        hour.setAdapter(arrayAdapter);

        ArrayAdapter<String> arrayAdapter1 = new ArrayAdapter<String>(mContext, android.R.layout.simple_spinner_dropdown_item, new ArrayList<String>(Helpers.MINUTES));
        minutes.setAdapter(arrayAdapter1);

        ArrayAdapter<String> arrayAdapter2 = new ArrayAdapter<String>(mContext, android.R.layout.simple_spinner_dropdown_item, new ArrayList<String>(Helpers.GLUCE_TYPES));
        type.setAdapter(arrayAdapter2);

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
                boolean valid1 = true, valid2 = true;
                AwesomeValidation mAwesomeValidation = new AwesomeValidation(BASIC);
                mAwesomeValidation.addValidation((Activity) mContext, R.id.mes_gluce_value, regExEmpty, R.string.err_empty);
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

                if(mAwesomeValidation.validate() && valid1 && valid2) {
                    pd = new ProgressDialog(mContext);
                    pd.setMessage("Παρακαλώ περιμένετε..");
                    pd.show();
                    storeGluce(displayDate,hour.getSelectedItem().toString().trim(),minutes.getSelectedItem().toString().trim(),gluce_value.getText().toString(),type.getSelectedItem().toString().trim());
                }
            }

        });

        return root;
    }

    public void storeGluce(String date, String hour, String minutes, String value, String type) {
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
            try {
                body.put("name", "Σάκχαρο");
                body.put("value", Integer.parseInt(value));
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
                        Toast.makeText(mContext,"Η μέτρηση προστέθηκε επιτυχώς", Toast.LENGTH_LONG).show();
                        getFragmentManager().popBackStackImmediate();

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        pd.hide();
                        Toast.makeText(mContext, "Παρουσιάστηκε σφάμλα! Παρακαλώ ελένξτε την σύνδεση σας στο διαδίκτυο.", Toast.LENGTH_LONG).show();
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