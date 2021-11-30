package gr.openit.smarthealthwatch;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.error.AuthFailureError;
import com.android.volley.error.VolleyError;
import com.android.volley.request.JsonObjectRequest;
import com.android.volley.request.StringRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import gr.openit.smarthealthwatch.util.SharedPrefManager;
import gr.openit.smarthealthwatch.util.URLs;
import gr.openit.smarthealthwatch.util.VolleySingleton;

public class ListViewAdapter extends ArrayAdapter<MeasurementRow> {

    private ArrayList<MeasurementRow> dataSet;
    Context mContext;
    ProgressDialog pd;
    Fragment f;
    Integer row_type;
    // View lookup cache
    private static class ViewHolder {
        TextView time;
        TextView data;
        TextView data_extra;
        TextView id;
    }

    public ListViewAdapter(ArrayList<MeasurementRow> data, Context context, @Nullable Fragment f, Integer type) {
        super(context, R.layout.list_row_item, data);
        this.dataSet = data;
        this.mContext=context;
        this.f = f;
        this.row_type = type;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        MeasurementRow dataModel = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        ViewHolder viewHolder; // view lookup cache stored in tag.
        JSONArray tmpThres = null;
        try {
            tmpThres = new JSONArray(SharedPrefManager.getInstance(mContext).getThresholds());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        final View result;
        if(row_type == 1) {

            if (convertView == null) {

                viewHolder = new ViewHolder();
                LayoutInflater inflater = LayoutInflater.from(getContext());
                convertView = inflater.inflate(R.layout.list_row_item, parent, false);
                viewHolder.time = (TextView) convertView.findViewById(R.id.time);
                viewHolder.data = (TextView) convertView.findViewById(R.id.data);
                viewHolder.id = (TextView) convertView.findViewById(R.id.mes_id);

                convertView.setTag(viewHolder);

            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            List<Fragment> fragmentList = ((MainActivity) mContext).getSupportFragmentManager().getFragments();
            String type = null;
            JSONObject s = null;
            for (Fragment f : fragmentList) {
                if (f instanceof FragmentGluceMeasurements && f.isVisible()) {
                    if(tmpThres != null) {
                        for (int i = 0; i < tmpThres.length(); i++) {
                            try {
                                s = tmpThres.getJSONObject(i);
                                type = s.getString("name");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            if (type.equals("GLU")) {
                                break;
                            }
                        }
                    }

                }else if(f instanceof FragmentPulseoxMeasurements && f.isVisible()){

                    if(tmpThres != null) {
                        for (int i = 0; i < tmpThres.length(); i++) {
                            try {
                                s = tmpThres.getJSONObject(i);
                                type = s.getString("name");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            if (type.equals("O2")) {
                                break;
                            }
                        }
                    }

                }else if(f instanceof  FragmentHrMeasurements && f.isVisible()){
                    if(tmpThres != null) {
                        for (int i = 0; i < tmpThres.length(); i++) {
                            try {
                                s = tmpThres.getJSONObject(i);
                                type = s.getString("name");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            if (type.equals("HR")) {
                                break;
                            }
                        }
                    }
                }else if(f instanceof  FragmentStressMeasurements && f.isVisible()){
                    if(tmpThres != null) {
                        for (int i = 0; i < tmpThres.length(); i++) {
                            try {
                                s = tmpThres.getJSONObject(i);
                                type = s.getString("name");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            if (type.equals("CGH")) {
                                break;
                            }
                        }
                    }
                }
            }
            try {
                if(s != null && Integer.parseInt(s.getString("emergencyHigher")) < Integer.parseInt(dataModel.getData())) {
                    viewHolder.data.setTextColor(Color.RED);
                }else if(s != null  && Integer.parseInt(s.getString("emergencyLower")) > Integer.parseInt(dataModel.getData())) {
                    viewHolder.data.setTextColor(Color.RED);
                }else{
                    viewHolder.data.setTextColor(Color.BLACK);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            viewHolder.time.setText(dataModel.getTime());
            if(dataModel.getExtraData() != null) {
                viewHolder.data.setText(dataModel.getData() + " , "+ dataModel.getExtraData());
            }else{
                viewHolder.data.setText(dataModel.getData());
            }
            viewHolder.id.setText(dataModel.getId());


            boolean handled = false;
            for (Fragment f : fragmentList) {
                if (f instanceof FragmentGluceMeasurements && f.isVisible()) {
                    ImageView deleteBtn = convertView.findViewById(R.id.delete_mes);
                    deleteBtn.setVisibility(View.VISIBLE);
                    TextView id = convertView.findViewById(R.id.mes_id);
                    TextView time = convertView.findViewById(R.id.time);
                    TextView data = convertView.findViewById(R.id.data);

                    deleteBtn.setOnClickListener(new View.OnClickListener() {
                        @SuppressLint("StringFormatMatches")
                        @Override
                        public void onClick(View v) {
                            new AlertDialog.Builder(mContext, R.style.LogoutDialog)
                                    .setMessage(mContext.getString(R.string.delete_confirm, time.getText(), data.getText()))
                                    .setIcon(android.R.drawable.ic_dialog_alert)
                                    .setPositiveButton(R.string.button_yes_gr, new DialogInterface.OnClickListener() {

                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            deleteMeasurement(id.getText().toString());
                                        }
                                    })
                                    .setNegativeButton(R.string.button_no_gr, null).show();
                            //notifyDataSetChanged();
                        }
                    });
                    break;
                }
            }
            //Handle buttons and add onClickListeners
        }else{

            if (convertView == null) {

                viewHolder = new ViewHolder();
                LayoutInflater inflater = LayoutInflater.from(getContext());
                convertView = inflater.inflate(R.layout.list_row_item_pressure, parent, false);
                viewHolder.time = (TextView) convertView.findViewById(R.id.time);
                viewHolder.data = (TextView) convertView.findViewById(R.id.data_high);
                viewHolder.data_extra = (TextView) convertView.findViewById(R.id.data_low);
                viewHolder.id = (TextView) convertView.findViewById(R.id.mes_id);

                convertView.setTag(viewHolder);

            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            String type = null;
            JSONObject s = null;
            if(tmpThres != null) {
                for (int i = 0; i < tmpThres.length(); i++) {
                    try {
                        s = tmpThres.getJSONObject(i);
                        type = s.getString("name");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    if (type.equals("BP")) {
                        break;
                    }
                }
            }
            try {
                if(s!=null && Float.parseFloat(s.getString("emergencyHigher")) < Float.parseFloat(dataModel.getData())) {
                    viewHolder.data.setTextColor(Color.RED);
                }else if(s!=null && Float.parseFloat(s.getString("emergencyLower")) > Float.parseFloat(dataModel.getData())) {
                    viewHolder.data.setTextColor(Color.RED);
                }else{
                    viewHolder.data.setTextColor(Color.BLACK);
                }

                if(s!=null && Float.parseFloat(s.getString("emergencyHigher")) < Float.parseFloat(dataModel.getExtraData())) {
                    viewHolder.data_extra.setTextColor(Color.RED);
                }else if(s!=null && Float.parseFloat(s.getString("emergencyLower")) > Float.parseFloat(dataModel.getExtraData())) {
                    viewHolder.data_extra.setTextColor(Color.RED);
                }else{
                    viewHolder.data_extra.setTextColor(Color.BLACK);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            viewHolder.time.setText(dataModel.getTime());
            viewHolder.data.setText(dataModel.getData());
            viewHolder.data_extra.setText(dataModel.getExtraData());

            viewHolder.id.setText(dataModel.getId());

            List<Fragment> fragmentList = ((MainActivity) mContext).getSupportFragmentManager().getFragments();

            boolean handled = false;
            for (Fragment f : fragmentList) {
                if (f instanceof FragmentPresureMeasurements && f.isVisible()) {
                    ImageView deleteBtn = convertView.findViewById(R.id.delete_mes);
                    deleteBtn.setVisibility(View.VISIBLE);
                    TextView id = convertView.findViewById(R.id.mes_id);
                    TextView time = convertView.findViewById(R.id.time);
                    TextView data = convertView.findViewById(R.id.data_high);
                    TextView data_extra = convertView.findViewById(R.id.data_low);
                    deleteBtn.setOnClickListener(new View.OnClickListener() {
                        @SuppressLint("StringFormatMatches")
                        @Override
                        public void onClick(View v) {
                            new AlertDialog.Builder(mContext, R.style.LogoutDialog)
                                    .setMessage(mContext.getString(R.string.delete_confirm, time.getText(), data.getText()+" "+data_extra.getText()))
                                    .setIcon(android.R.drawable.ic_dialog_alert)
                                    .setPositiveButton(R.string.button_yes_gr, new DialogInterface.OnClickListener() {

                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            deleteMeasurement(id.getText().toString());
                                        }
                                    })
                                    .setNegativeButton(R.string.button_no_gr, null).show();
                            //notifyDataSetChanged();
                        }
                    });
                    break;
                }
            }
            //Handle buttons and add onClickListeners
        }
        return convertView;
    }

    private void deleteMeasurement(String id){
        pd = new ProgressDialog(mContext);
        pd.setMessage(mContext.getString(R.string.please_wait));
        pd.show();

        String primaryUserInfoUrl = URLs.URL_DELETE_MEASUREMENT.replace("{id}",""+ SharedPrefManager.getInstance(mContext).getUser().getId()).replace("{mes_id}",id);

        StringRequest request = new  StringRequest(Request.Method.DELETE, primaryUserInfoUrl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        pd.hide();
                        pd.cancel();

                        Toast.makeText(mContext,mContext.getString(R.string.measurement_delete_success),Toast.LENGTH_SHORT).show();
                        if (f instanceof FragmentGluceMeasurements && f.isVisible()) {
                            ((FragmentGluceMeasurements) f).refresh();
                        }else if (f instanceof FragmentPresureMeasurements && f.isVisible()) {
                            ((FragmentPresureMeasurements) f).refresh();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        pd.hide();
                        pd.cancel();

                        Toast.makeText(mContext, mContext.getString(R.string.network_error), Toast.LENGTH_LONG).show();

                    }
                }){

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Accept","application/json");
                headers.put("Authorization", "Bearer " + SharedPrefManager.getInstance(mContext).getKeyAccessToken());

                return headers;
            }
        };
        request.setShouldCache(false);

        VolleySingleton.getInstance(mContext).addToRequestQueue(request);
    }

    @Override
    public boolean isEnabled(int position) {
        return false;
    }

}