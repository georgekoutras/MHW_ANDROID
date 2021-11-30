package gr.openit.smarthealthwatch;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;

import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;

import android.widget.TextView;
import android.widget.Toast;


import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager.widget.PagerAdapter;

import com.android.volley.Request;
import com.android.volley.Response;

import com.android.volley.error.AuthFailureError;
import com.android.volley.error.VolleyError;
import com.android.volley.request.StringRequest;
import com.basgeekball.awesomevalidation.AwesomeValidation;

import gr.openit.smarthealthwatch.util.HttpsTrustManager;
import gr.openit.smarthealthwatch.util.URLs;
import gr.openit.smarthealthwatch.util.VolleySingleton;

import com.google.common.collect.Range;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static com.basgeekball.awesomevalidation.ValidationStyle.BASIC;

public class RegistrationAdapter extends PagerAdapter {
    private Context mContext;
    private int ONBOARD_PAGE_COUNT = 2;
    EditText username, password, first_name, last_name, year;
    CheckBox hr, pressure, pulseox, gluce, stress, cough;
    ArrayList<String> monitorTypes;
    private static final String TAG = "MainActivity";
    ProgressDialog pd;

    public RegistrationAdapter(Context mContext) {
        this.mContext = mContext;
    }

    @Override
    public int getCount() {
        return ONBOARD_PAGE_COUNT;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public Object instantiateItem(final ViewGroup container, int position) {
        //Log.i("instantiatePos register",""+position);

        final View itemView ;
        //itemView = LayoutInflater.from(mContext).inflate(R.layout.onboard_item, container, false);
        final NonSwipableViewPager p1= container.findViewById(R.id.pager_registration);

        if(position==0){
            itemView = LayoutInflater.from(mContext).inflate(R.layout.register_board1, container, false);
            Button next_step = (Button) itemView.findViewById(R.id.next_step);
            TextView login = (TextView) itemView.findViewById(R.id.login);

            next_step.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String regExPass = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{6,}$";
                    String regExPass1 = "^(?=.*\\d)(?=.*[a-z])(?!.*\\s).{6,}$";
                    AwesomeValidation mAwesomeValidation = new AwesomeValidation(BASIC);
                    mAwesomeValidation.addValidation((Activity) mContext, R.id.et_email, Patterns.EMAIL_ADDRESS, R.string.err_mail);
                    mAwesomeValidation.addValidation((Activity) mContext, R.id.et_password, regExPass1, R.string.err_pass);

                    //Toast.makeText(mContext,""+mAwesomeValidation.validate(),Toast.LENGTH_SHORT).show();
                    if(mAwesomeValidation.validate()) {
                        p1.setCurrentItem(1, true);
                        username = itemView.findViewById(R.id.et_email);
                        password = itemView.findViewById(R.id.et_password);
                        //Toast.makeText(mContext,"Register now! "+username.getText()+" "+password.getText(),Toast.LENGTH_SHORT).show();

                    }
                }

            });
            login.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Fragment loginFragment = new LoginFragment(mContext);
                    FragmentManager fm = ((MainActivity)mContext).getSupportFragmentManager();
                    FragmentTransaction transaction = fm.beginTransaction();
                    transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
                    transaction.replace(R.id.main_root,loginFragment); // give your fragment container id in first parameter
                    transaction.addToBackStack(null);  // if written, this transaction will be added to backstack
                    transaction.commit();
                }

            });

        }else{
            itemView = LayoutInflater.from(mContext).inflate(R.layout.register_board2, container, false);
            Button reg_back = (Button) itemView.findViewById(R.id.btn_reg_back);
            Button register_btn = (Button) itemView.findViewById(R.id.btn_register);
            register_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    String regExEmpty = "^(?=\\s*\\S).*$";
                    String regExMinMax = "^.{5,50}$";
                    AwesomeValidation mAwesomeValidation = new AwesomeValidation(BASIC);
/*                    mAwesomeValidation.addValidation((Activity) mContext, R.id.first_name_field, regExEmpty, R.string.err_empty);
                    mAwesomeValidation.addValidation((Activity) mContext, R.id.last_name_field, regExEmpty, R.string.err_empty);*/
                    mAwesomeValidation.addValidation((Activity) mContext, R.id.first_name_field, regExMinMax, R.string.minimum_chars);
                    mAwesomeValidation.addValidation((Activity) mContext, R.id.last_name_field, regExMinMax, R.string.minimum_chars);
                    mAwesomeValidation.addValidation((Activity) mContext, R.id.birth_date_field , Range.closed(1900, Calendar.getInstance().get(Calendar.YEAR)), R.string.err_year);


                    if(mAwesomeValidation.validate()) {
                        //Toast.makeText(mContext,"Register now!",Toast.LENGTH_SHORT).show();
                        first_name = itemView.findViewById(R.id.first_name_field);
                        last_name = itemView.findViewById(R.id.last_name_field);
                        year = itemView.findViewById(R.id.birth_date_field);

                        hr = itemView.findViewById(R.id.heartrate_checkbox);
                        pressure = itemView.findViewById(R.id.presure_checkbox);
                        pulseox = itemView.findViewById(R.id.pulseox_checkbox);
                        gluce = itemView.findViewById(R.id.glucometer_checkbox);
                        cough = itemView.findViewById(R.id.cough_checkbox);
                        stress = itemView.findViewById(R.id.stress_checkbox);

                        monitorTypes = new ArrayList<String>();
                        if(hr.isChecked())
                            monitorTypes.add("HR"); // you can save this as checked somewhere
                        if(pressure.isChecked())
                            monitorTypes.add("BP"); // you can save this as checked somewhere
                        if(pulseox.isChecked())
                            monitorTypes.add("O2"); // you can save this as checked somewhere
                        if(gluce.isChecked())
                            monitorTypes.add("GLU"); // you can save this as checked somewhere
                        if(cough.isChecked())
                            monitorTypes.add("CGH"); // you can save this as checked somewhere
                        if(stress.isChecked())
                            monitorTypes.add("STR"); // you can save this as checked somewhere

                        register();
                    }

                }
            });

            reg_back.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    p1.setCurrentItem(0, true);
                }

            });

        }

        container.addView(itemView);

        return itemView;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((LinearLayout) object);
    }

    private void register(){
        pd = new ProgressDialog(mContext);
        pd.setMessage(mContext.getString(R.string.please_wait));
        pd.show();

        final JSONObject body = new JSONObject();
        JSONArray jsArray = new JSONArray(monitorTypes);
        try {
            body.put("email", username.getText().toString().trim());
            body.put("password", password.getText().toString());
            body.put("firstName", first_name.getText().toString().trim());
            body.put("lastName", last_name.getText().toString().trim());
            if(!TextUtils.isEmpty(year.getText().toString().trim())) {
                body.put("birthYear", Integer.parseInt(year.getText().toString().trim()));
            }
            body.put("monitorTypes",jsArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }

       /* Log.i("response33",URLs.URL_REGISTER);
        Log.i("response33",body.toString());*/

        HttpsTrustManager.allowAllSSL();
        StringRequest stringRequest = new StringRequest(Request.Method.POST, URLs.URL_REGISTER,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        pd.hide();
                        pd.cancel();

                        showRegisterConfirmation();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        pd.hide();
                        pd.cancel();

                        if(error.networkResponse.statusCode == 400) {
                            Toast.makeText(mContext, new String(error.networkResponse.data, StandardCharsets.UTF_8), Toast.LENGTH_LONG).show();
                        }else if(error.networkResponse.statusCode == 422){
                            try {
                                final JSONObject body = new JSONObject(new String(error.networkResponse.data, StandardCharsets.UTF_8));
                                JSONObject errors = new JSONObject(body.get("errors").toString());

                                Iterator<String> keys = errors.keys();
                                String msg = "";
                                while(keys.hasNext()) {
                                    String key = keys.next();
                                    JSONArray curr_msg = new JSONArray(errors.get(key).toString());
                                    msg += key+": "+curr_msg.getString(0)+ '\n';

                                }
                                //Log.i("response331",msg);

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                        }
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
                headers.put("Accept","application/json");
                return headers;
            }
        };
        stringRequest.setShouldCache(false);

        VolleySingleton.getInstance(mContext).addToRequestQueue(stringRequest);
    }

    private void showRegisterConfirmation(){
        Fragment confirmationFragment = new FragmentRegisterConfirmation(mContext);
        FragmentManager fm = ((MainActivity)mContext).getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
        transaction.replace(R.id.main_root,confirmationFragment); // give your fragment container id in first parameter
        transaction.commit();
    }
}
