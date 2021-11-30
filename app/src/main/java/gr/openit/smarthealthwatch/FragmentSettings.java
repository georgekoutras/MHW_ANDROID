package gr.openit.smarthealthwatch;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.basgeekball.awesomevalidation.AwesomeValidation;
import com.garmin.health.DeviceManager;
import com.garmin.health.GarminHealth;

import java.util.Set;

import gr.openit.smarthealthwatch.devices.PairedDevicesDialogFragment;
import gr.openit.smarthealthwatch.devices.PairedRingDialogFragment;
import gr.openit.smarthealthwatch.ui.BluetoothLeService;
import gr.openit.smarthealthwatch.util.SharedPrefManager;

import static com.basgeekball.awesomevalidation.ValidationStyle.BASIC;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link FragmentSettings#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FragmentSettings extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private Context mContext;
    private UserHome uh;
    private String primaryUserInfo = null;
    ProgressDialog pd;
    String regExEmpty = "^(?=\\s*\\S).*$";

    public FragmentSettings(Context mContext, UserHome u) {
        // Required empty public constructor
        this.mContext = mContext;
        this.uh = u;
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment FragmentSettings.
     */
    // TODO: Rename and change types and number of parameters
    public static FragmentSettings newInstance(String param1, String param2) {
        FragmentSettings fragment = new FragmentSettings(null,null);
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        View root =  inflater.inflate(R.layout.fragment_settings, container, false);
        RelativeLayout logout = root.findViewById(R.id.logout_line);
        RelativeLayout account = root.findViewById(R.id.account_line);
        RelativeLayout devices = root.findViewById(R.id.bluetooth_line);
        RelativeLayout interval = root.findViewById(R.id.interval_line);

        interval.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                final AlertDialog dialogBuilder = new AlertDialog.Builder(mContext).create();
                View dialogView = inflater.inflate(R.layout.dialog_edit_interval, null);
                final EditText editText = (EditText) dialogView.findViewById(R.id.edt_comment);
                Button button1 = (Button) dialogView.findViewById(R.id.buttonSubmit);
                Button button2 = (Button) dialogView.findViewById(R.id.buttonCancel);

                editText.setText(String.valueOf(SharedPrefManager.getInstance(mContext).getGlobalInterval()));

                button2.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialogBuilder.dismiss();
                    }
                });
                button1.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // DO SOMETHINGS
                        boolean valid;
                        AwesomeValidation mAwesomeValidation = new AwesomeValidation(BASIC);
                        mAwesomeValidation.addValidation((Activity) mContext, R.id.edt_comment, regExEmpty, R.string.err_empty);
                        if(!editText.getText().toString().equals("")){
                            if(Integer.parseInt(editText.getText().toString()) < 30 || Integer.parseInt(editText.getText().toString()) > 3600 ) {
                                String errorString = "Αποδεκτές τιμές απο 30 - 3600 δευτερόλεπτα.";
                                editText.setError(errorString);
                                valid = false;
                            }else{
                                valid = true;
                                editText.setError(null);
                            }
                        }else{
                            valid = false;
                            String errorString = "Αποδεκτές τιμές απο 30 - 3600 δευτερόλεπτα.";
                            editText.setError(errorString);
                        }
                        if(mAwesomeValidation.validate() && valid) {
                            SharedPrefManager.getInstance(mContext).setGlobalInterval(Integer.parseInt(editText.getText().toString()));
                            Intent gattServiceIntent;
                            BluetoothLeService bleService;
                            bleService = new BluetoothLeService();
                            gattServiceIntent = new Intent(mContext, bleService.getClass());
                            gattServiceIntent.putExtra("interval", SharedPrefManager.getInstance(mContext).getGlobalInterval());
                            if (isMyServiceRunning(bleService.getClass())) {
                                getActivity().startService(gattServiceIntent);
                            }
                            GarminCustomService gcService;
                            gcService = new GarminCustomService();
                            gattServiceIntent = new Intent(mContext, gcService.getClass());
                            gattServiceIntent.putExtra("interval", SharedPrefManager.getInstance(mContext).getGlobalInterval());
                            Set<String> monitorTypes = SharedPrefManager.getInstance(mContext).getUser().getMonitorTypes();
                            if(monitorTypes.contains("HR")) {
                                gattServiceIntent.putExtra("hr_enabled", true);
                            }else{
                                gattServiceIntent.putExtra("hr_enabled", false);
                            }
                            if(monitorTypes.contains("O2")) {
                                gattServiceIntent.putExtra("pulseox_enabled", true);
                            }else{
                                gattServiceIntent.putExtra("pulseox_enabled", false);
                            }
                            if (isMyServiceRunning(gcService.getClass())) {
                                getActivity().startService(gattServiceIntent);
                            }
                            Toast.makeText(mContext, getString(R.string.interval_set_success), Toast.LENGTH_SHORT).show();
                            dialogBuilder.dismiss();
                        }
                    }
                });

                dialogBuilder.setView(dialogView);
                dialogBuilder.show();
            }
        });

        devices.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                String[] colors = {getString(R.string.smart_watch),getString(R.string.smart_ring)};

                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                builder.setTitle(getString(R.string.connect_device_ask));
                builder.setItems(colors, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Set<String> monitorTypes = SharedPrefManager.getInstance(mContext).getUser().getMonitorTypes();
                        switch (which) {
                            case 0:
                                if(monitorTypes != null && (monitorTypes.contains("O2") || monitorTypes.contains("HR"))) {
                                    connectedDevicesTransition();
                                }else{
                                    new AlertDialog.Builder(mContext, R.style.LogoutDialog)
                                            .setMessage(R.string.prompt_enable_hr_pulse)
                                            .setIcon(android.R.drawable.ic_dialog_alert)
                                            .setPositiveButton(R.string.button_ok, null).show();
                                }
                                break;
                            case 1:

                                if(monitorTypes != null && monitorTypes.contains("STR")) {
                                    connectRingTransition();
                                }else{
                                    new AlertDialog.Builder(mContext, R.style.LogoutDialog)
                                            .setMessage(R.string.prompt_enable_stress)
                                            .setIcon(android.R.drawable.ic_dialog_alert)
                                            .setPositiveButton(R.string.button_ok, null).show();
                                }
                                break;
                        }
                    }
                });
                builder.show();
                //connectedDevicesTransition();
            }
        });

        account.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                accountData();
            }
        });
        logout.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                new AlertDialog.Builder(mContext, R.style.LogoutDialog)
                        .setMessage(R.string.logout_ask)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(R.string.button_yes_gr, new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int whichButton) {
                                Intent gattServiceIntent;
                                BluetoothLeService bleService;
                                bleService = new BluetoothLeService();
                                gattServiceIntent = new Intent(mContext, bleService.getClass());
                                gattServiceIntent.putExtra("stop_service",true);
                                if (isMyServiceRunning(bleService.getClass())) {
                                    getActivity().startService(gattServiceIntent);
                                    getActivity().stopService(gattServiceIntent);

                                }
                                CoughService coughService;
                                coughService = new CoughService();
                                gattServiceIntent = new Intent(mContext, coughService.getClass());
                                gattServiceIntent.putExtra("stop_service",true);
                                if (isMyServiceRunning(coughService.getClass())) {
                                    getActivity().startService(gattServiceIntent);
                                    getActivity().stopService(gattServiceIntent);

                                }
                                GarminCustomService gcService;
                                gcService = new GarminCustomService();
                                gattServiceIntent = new Intent(mContext, gcService.getClass());
                                gattServiceIntent.putExtra("stop_service",true);
                                if (isMyServiceRunning(gcService.getClass())) {
                                    getActivity().startService(gattServiceIntent);
                                    getActivity().stopService(gattServiceIntent);

                                }
                                SharedPrefManager.getInstance(mContext).logout();
                                userLogin();
                            }})
                        .setNegativeButton(R.string.button_no_gr, null).show();
            }
        });
        return root;
    }

    public boolean onBackPressed() {
        this.uh.showMenu();
        if(this.uh.active == this.uh.active_bottom_nav) {
            this.uh.toolbarTitleDefault();
        }
        return true;
    }

    private void userLogin() {
        Fragment loginFragment = new LoginFragment(mContext);
        FragmentManager fm = getFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
        transaction.hide(this).replace(R.id.main_root,loginFragment); // give your fragment container id in first parameter
        transaction.commit();
    }

    private void accountData(){
        Fragment accountFragment = new FragmentShowAccountData(mContext,uh);
        FragmentManager fm = getFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
        transaction.replace(R.id.main_container,accountFragment); // give your fragment container id in first parameter
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private void connectedDevicesTransition()
    {
        if(GarminHealth.isInitialized()) {
            DeviceManager.getDeviceManager().addPairedStateListener(new MainActivity.SetupListener(mContext));

            PairedDevicesDialogFragment pairedDevicesDialogFragment = new PairedDevicesDialogFragment(mContext,uh);

            FragmentManager fragmentManager = getFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.addToBackStack(null);

            transaction.replace(R.id.main_container, pairedDevicesDialogFragment, pairedDevicesDialogFragment.getTag()).commit();
        }else{
            Log.i("GarminHealth","not initialized");
        }
    }

    private void connectRingTransition(){
        Fragment ringFragment = new PairedRingDialogFragment(mContext,uh);
        FragmentManager fm = getFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        transaction.replace(R.id.main_container,ringFragment); // give your fragment container id in first parameter
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getActivity().getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                Log.i ("isMyServiceRunning?", true+"");
                return true;
            }
        }
        Log.i ("isMyServiceRunning?", false+"");
        return false;
    }
}