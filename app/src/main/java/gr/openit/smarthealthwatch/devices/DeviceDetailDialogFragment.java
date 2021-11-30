package gr.openit.smarthealthwatch.devices;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import gr.openit.smarthealthwatch.MainActivity;
import gr.openit.smarthealthwatch.R;
import gr.openit.smarthealthwatch.ui.settings.widget.CustomAutoValueListView;
import gr.openit.smarthealthwatch.ui.sync.LoggingFragment;
import gr.openit.smarthealthwatch.util.Alarm;

import com.garmin.health.ConnectionState;
import com.garmin.health.Device;
import com.garmin.health.DeviceManager;
import com.garmin.health.NotificationManager;
import com.garmin.health.customlog.DataSource;
import com.garmin.health.customlog.LoggingStatus.State;
import com.garmin.health.customlog.LoggingSyncListener;
import com.garmin.health.settings.ConnectIqItem;
import com.garmin.health.settings.SupportStatus;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.JdkFutureAdapters;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Copyright (c) 2017 Garmin International. All Rights Reserved.
 * <p></p>
 * This software is the confidential and proprietary information of
 * Garmin International.
 * You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement
 * you entered into with Garmin International.
 * <p></p>
 * Garmin International MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF THE
 * SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE, OR NON-INFRINGEMENT. Garmin International SHALL NOT BE LIABLE FOR ANY DAMAGES
 * SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR DISTRIBUTING
 * THIS SOFTWARE OR ITS DERIVATIVES.
 * <p></p>
 * Created by jacksoncol on 6/19/18.
 */
public class DeviceDetailDialogFragment extends DialogFragment
{
    private Alarm alarm;
    public final static String DEVICE_ADDRESS_EXTRA = "device.address";
    private int CURRENT_INTERVAL = 10;
    private Button mSettingsButton = null;

    private  View view = null;
/*    private long mEndTime = Calendar.getInstance().getTimeInMillis()/1000;
    private long mStartTime = mEndTime - (3600 * 6);*/

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState)
    {
        setRetainInstance(true);
        alarm = new Alarm();
        alarm.setFragmentActivity(getActivity());
        DeviceManager deviceManager = DeviceManager.getDeviceManager();
        Device device = deviceManager.getDevice(getArguments().getString(DEVICE_ADDRESS_EXTRA));
        if(device.connectionState() != ConnectionState.CONNECTED)
        {
            return null;
        }

        view = inflater.inflate(R.layout.fragment_device_details, container, false);
        //hideNotifications(view);
        //hideConnectIq(view);
        mSettingsButton = view.findViewById(R.id.settings_button);

        checkAlarmUp();

        if(device.dataLoggingSupportStatus() != SupportStatus.ENABLED)
        {
            Log.e("STATUS",""+device.dataLoggingSupportStatus());
            hideDataLogging(view);
        }
        else
        {
            setDataLogging(view, device);
        }

        return view;
    }

    private void checkAlarmUp(){
        boolean alarmUp = (PendingIntent.getBroadcast(getContext(), 0,
                new Intent(getContext(),Alarm.class),
                PendingIntent.FLAG_NO_CREATE) != null);
        Button stopLoggingButton = null;
        stopLoggingButton = view.findViewById(R.id.stop_alarm_button);

        if (alarmUp)
        {
            mSettingsButton.setEnabled(false);
            stopLoggingButton.setEnabled(true);
            Toast.makeText(getContext(), "Alarm is active!", Toast.LENGTH_SHORT).show();
        }else{
            mSettingsButton.setEnabled(true);
            stopLoggingButton.setEnabled(false);
            // if alarm is inactive and syncing preference is true, do it false.

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
            if(prefs.contains("syncing") && prefs.getBoolean("syncing",false)) {
                prefs.edit().putBoolean("syncing", false).apply();
            }
            Toast.makeText(getContext(), "Alarm is Inactive!", Toast.LENGTH_SHORT).show();
        }
    }

    private void hideDataLogging(View view)
    {
        TextView loggingTitle = view.findViewById(R.id.interval_picker_top_anchor);
        TextView startTitle = view.findViewById(R.id.start_title);
        TextView endTitle = view.findViewById(R.id.end_title);

        loggingTitle.setVisibility(View.GONE);
        startTitle.setVisibility(View.GONE);
        endTitle.setVisibility(View.GONE);
        mSettingsButton.setVisibility(View.GONE);
    }

    private void setDataLogging(View view, Device device)
    {

        checkAvailableData(device.address());

        Button settingsButton = view.findViewById(R.id.settings_button);
        Button stopLoggingButton = view.findViewById(R.id.stop_alarm_button);

        stopLoggingButton.setOnClickListener((v)->{
            alarm.cancelAlarm(this.getContext());
            stopLoggingButton.setEnabled(false);
            settingsButton.setEnabled(true);
        });
        settingsButton.setOnClickListener((v) ->
        {
            settingsButton.setEnabled(false);

            new LoggingGetSettingsTask().execute(device);
        });
    }


    @Override
    public void onResume()
    {
        super.onResume();
        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();

        if(actionBar != null)
        {
            actionBar.setTitle(R.string.device_detail_title);
        }
    }

    private void checkAvailableData(String address)
    {
        if(getActivity() == null)
        {
            return;
        }

        getActivity().runOnUiThread(() ->
                DeviceManager.getDeviceManager().hasLoggedData(address, b ->
                        {
                            Activity activity = getActivity();

                            if(activity != null && b != null)
                            {
                                //mQueryButton.setEnabled(b);
                            }
                        }));
    }

    private class LoggingGetSettingsTask extends AsyncTask<Device, Integer, Map<DataSource, Float>>
    {
        Device mDevice;

        protected Map<DataSource, Float> doInBackground(Device... devices)
        {
            Device device = devices[0];
            mDevice = device;

            Map<DataSource, Float> ret = new HashMap<>();

            Set<DataSource> sources = device.supportedLoggingTypes();

            if(sources == null)
            {
                return ret;
            }

            for(DataSource source : sources)
            {
                try
                {
                    Futures.getChecked(DeviceManager.getDeviceManager().getLoggingState(device.address(), source, loggingStatus ->
                    {
                        if(source.name().equals("HEART_RATE") || source.name().equals("STRESS") || source.name().equals("PULSE_OX")) {
                            if (loggingStatus != null && loggingStatus.getState() == State.LOGGING_ON) {
                                ret.put(source, Float.valueOf(loggingStatus.getInterval()));
                            } else {
                                ret.put(source, 0.0f);
                            }
                        }
                    }), Exception.class);
                }
                catch(Exception ignored) {}
            }

            return ret;
        }

        protected void onProgressUpdate(Integer... progress) {}

        protected void onPostExecute(Map<DataSource, Float> options)
        {
            AlertDialog.Builder dialog = new AlertDialog.Builder(getContext(), R.style.LogoutDialog);
            dialog.setCancelable(false);
            View root = getActivity().getLayoutInflater().inflate(R.layout.interval_options_dialog, null);
            EditText interval_input = root.findViewById(R.id.interval);

            if(options.isEmpty())
            {
                Toast.makeText(getContext(), "No Custom Types Returned Settings", Toast.LENGTH_SHORT).show();
                mSettingsButton.setEnabled(true);
                return;
            }

            //dataSources.setOptions(options);
            dialog.setTitle("Διάστημα καταγραφής");
            Set<DataSource> sources = mDevice.supportedLoggingTypes();
            for(DataSource source : sources) {
                //custom default interval set to 10
                if (source.name().equals("HEART_RATE") || source.name().equals("STRESS") || source.name().equals("PULSE_OX")) {
                    if(Math.round(options.get(source)) < 10 || Math.round(options.get(source)) > 3600){
                        interval_input.setText("10");
                        CURRENT_INTERVAL = 10;
                    }else {
                        interval_input.setText(String.valueOf(Math.round(options.get(source))));
                        CURRENT_INTERVAL = Math.round(options.get(source));

                    }
                    break;
                }
            }
            dialog.setView(root);
            dialog.setNegativeButton("Aκυρο", (dialog1, which) -> { dialog1.dismiss(); mSettingsButton.setEnabled(true); });
            dialog.setPositiveButton("Eναρξη", (dialog12, which) ->
            {

                new LoggingSetSettingsTask().execute(root, mDevice, dialog12);
            });
            dialog.create().show();
        }
    }

    private class LoggingSetSettingsTask extends AsyncTask<Object, Integer, Boolean>
    {
        Dialog mDialog;

        protected Boolean doInBackground(Object... settings)
        {
            //Map<DataSource, Float> options = (Map<DataSource, Float>) settings[0];
            View root = (View) settings[0];
            Device device = (Device) settings[1];
            mDialog = (Dialog) settings[2];

            DeviceManager manager = DeviceManager.getDeviceManager();

            AtomicBoolean error = new AtomicBoolean(false);

            Set<DataSource> sources = device.supportedLoggingTypes();

            if(sources == null)
            {
                return false;
            }
            int interval;
            EditText x = root.findViewById(R.id.interval);
            String interval_string = x.getText().toString();
            if(interval_string.matches("")){
                interval = 10;
                CURRENT_INTERVAL = 10;

            }else{
                interval = Integer.parseInt(x.getText().toString());
                CURRENT_INTERVAL = Integer.parseInt(x.getText().toString());

            }

            for(DataSource source : sources) {
                //custom default interval set to 10
                if (source.name().equals("HEART_RATE") || source.name().equals("STRESS") || source.name().equals("PULSE_OX")) {

                    if (DataSource.NO_INTERVAL_SOURCES.contains(source)) {
                        interval = source.defaultInterval();
                    }

                    try {
                        if (interval < 10 || interval > 3600) {
                            interval = 10;
                            CURRENT_INTERVAL = 10;

                            error.set(true);
                        }else{
                            CURRENT_INTERVAL = interval;

                        }

                        Futures.getChecked(manager.setLoggingStateWithInterval(device.address(), source, true, interval, loggingStatus ->
                        {
                            if (loggingStatus != null && loggingStatus.getState() != State.LOGGING_ON && loggingStatus.getState() != State.LOGGING_OFF) {
                                error.set(true);
                            }
                        }), Exception.class);
                    } catch (Exception e) {
                        error.set(true);
                    }
                }
            }
            return error.get();
        }

        protected void onProgressUpdate(Integer... progress) {}

        protected void onPostExecute(Boolean result)
        {
            if(result)
            {
                Toast.makeText(getContext(), "Some Error Occurred Sending Settings", Toast.LENGTH_SHORT).show();
            }
            else
            {
                Toast.makeText(getContext(), "Settings Sent Successfully", Toast.LENGTH_SHORT).show();
                if(alarm != null){
                    Context context = getContext();
                    Button stopLoggingButton =view.findViewById(R.id.stop_alarm_button);

                    stopLoggingButton.setEnabled(true);
                    mSettingsButton.setEnabled(false);
                    Toast.makeText(getContext(), "Starting alarm with Interval "+CURRENT_INTERVAL, Toast.LENGTH_SHORT).show();
                    DeviceManager deviceManager = DeviceManager.getDeviceManager();
                    Device device = deviceManager.getDevice(getArguments().getString(DEVICE_ADDRESS_EXTRA));
                    alarm.setAlarm(context,device.address(),CURRENT_INTERVAL);

                }
            }
            mDialog.dismiss();
        }
    }
}
