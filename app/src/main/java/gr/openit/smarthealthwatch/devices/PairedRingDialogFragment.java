package gr.openit.smarthealthwatch.devices;

import android.animation.ObjectAnimator;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.garmin.health.DeviceManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import gr.openit.smarthealthwatch.R;
import gr.openit.smarthealthwatch.UserHome;
import gr.openit.smarthealthwatch.ui.BluetoothLeService;
import gr.openit.smarthealthwatch.ui.MoodmetricServiceReceiver;
import gr.openit.smarthealthwatch.util.SharedPrefManager;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PairedRingDialogFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PairedRingDialogFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    Context mContext;
    View root;

    private static final UUID mmServiceUUID =
            UUID.fromString("dd499b70-e4cd-4988-a923-a7aab7283f8e");

    private BluetoothAdapter bluetoothAdapter;
    private boolean scanning;
    private TextView logView;
    private ScrollView scrollView;
    FragmentManager fm;
    UserHome uh;
    ProgressDialog pd;
    ArrayList<String> listItems=new ArrayList<String>();
    ArrayList<BluetoothDevice> deviceList=new ArrayList<BluetoothDevice>();

    ArrayAdapter<String> adapter;
    ListView ring_list;
    private BluetoothLeService mBluetoothLeService;
    Intent gattServiceIntent;
    private BluetoothLeService bleService;
    BluetoothManager bluetoothManager;
    TextView mScanText;
    private ProgressBar mScanProgressBar;
    private ObjectAnimator mScanProgressBarAnimator;
    Switch mScanSwitch;
    public PairedRingDialogFragment(Context mContext, UserHome uh) {
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
     * @return A new instance of fragment PairedRingDialogFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static PairedRingDialogFragment newInstance(String param1, String param2) {
        PairedRingDialogFragment fragment = new PairedRingDialogFragment(null,null);
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
    public void onResume()
    {
        super.onResume();
        this.uh.hideMenu();
        this.uh.hideUnity();

        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();

        if (actionBar != null)
        {
            actionBar.setTitle(R.string.connected_devices_title);
        }
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        fm = getFragmentManager();
        root = inflater.inflate(R.layout.fragment_paired_ring_dialog, container, false);
        ring_list = root.findViewById(R.id.ring_list);
        adapter=new ArrayAdapter<String>(mContext,
                android.R.layout.simple_list_item_1,
                listItems);
        ring_list.setAdapter(adapter);
        mScanSwitch = root.findViewById(R.id.scan_switch);
        mScanText = root.findViewById(R.id.scan_text);
        mScanProgressBar = root.findViewById(R.id.scanning_icon);
        mScanProgressBarAnimator = ObjectAnimator.ofInt(mScanProgressBar, "progress", 0, 100);
        mScanProgressBarAnimator.setDuration(2000);

        mScanSwitch.setOnCheckedChangeListener((compoundButton, checked) ->
        {
            if(checked)
            {
                startScanning();
            }
            else
            {

                stopScanning();
            }
        });

        ring_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position,
                                    long id) {
                String  itemValue    = (String) parent.getItemAtPosition(position);
                if(itemValue.contains("CONNECTED")){
                    new AlertDialog.Builder(mContext, R.style.LogoutDialog)
                            .setMessage(R.string.disconnect_ble_device)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setPositiveButton(R.string.button_yes_gr, new DialogInterface.OnClickListener() {

                                public void onClick(DialogInterface dialog, int whichButton) {
                                    bleService = new BluetoothLeService();
                                    gattServiceIntent = new Intent(mContext, bleService.getClass());
                                    gattServiceIntent.putExtra("stop_service",true);
                                    getActivity().startService(gattServiceIntent);
                                    deviceList.remove(position);
                                    listItems.remove(position);
                                    adapter.notifyDataSetChanged();
                                    Toast.makeText(mContext,"Η συσκευή αποσυνδέθηκε.",Toast.LENGTH_LONG).show();
                                    getActivity().stopService(gattServiceIntent);

                                    mScanSwitch.setChecked(true);
                                    startScanning();
                                }})
                            .setNegativeButton(R.string.button_no_gr, null).show();
                }else{
                    bleService = new BluetoothLeService();
                    gattServiceIntent = new Intent(mContext, bleService.getClass());
                    gattServiceIntent.putExtra("device_address",deviceList.get(position).getAddress());
                    gattServiceIntent.putExtra("interval",SharedPrefManager.getInstance(mContext).getGlobalInterval());

                    getActivity().startService(gattServiceIntent);
                    ((String) parent.getItemAtPosition(position)).concat("- CONNECTED");
                    fm.popBackStackImmediate();
                    Toast.makeText(mContext,"Η συσκευή συνδέθηκε.",Toast.LENGTH_LONG).show();
                }


                //Toast.makeText(mContext,parent.getItemAtPosition(position).toString(),Toast.LENGTH_SHORT).show();
                //bleGatt = deviceList.get(position).connectGatt(mContext, false, mGattCallback);
                //bleGatt.connect();
            }
        });

        bluetoothManager =
                (BluetoothManager)getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) {
            //log("bluetooth service not available");
            //return;
            fm.popBackStackImmediate();
        }
        // Get a reference to the bluetooth adapter of the device
        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            //log("bluetooth not supported");
            //return;
            fm.popBackStackImmediate();

        }
        // Check that bluetooth is enabled. If not, ask the user to enable it.
        if (!bluetoothAdapter.isEnabled()) {
            new AlertDialog.Builder(mContext, R.style.LogoutDialog)
                    .setMessage(R.string.enable_bluetooth_ask)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(R.string.button_yes_gr, new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int whichButton) {
                            bluetoothAdapter.enable();
                        }})
                    .setNegativeButton(R.string.button_no_gr, null).show();
        }
        // If bluetooth was enabled start scanning for rings.
        return root;
    }

    private final BluetoothAdapter.LeScanCallback scanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    //log("found ring %s, rssi: %d", device.getAddress(), rssi);
                    //stopScanning();
                    getActivity().runOnUiThread(
                            new Runnable() {
                                @Override
                                public void run() {
                                    log("MoodMetric " + device.getAddress(), device);
                                    //log("found ring %s, rssi: %d", device.getAddress(), rssi);
                                    mScanSwitch.setChecked(false);
                                    stopScanning();
                                    //log("connecting with %s", device.getAddress());
                                    // Establish a connection with the ring.
                                    //bleGatt = device.connectGatt(mContext, false, gattCallback);
                                    //bleGatt.connect();
                                }
                            });
                }
            };

    private void startScanning() {
        if (scanning) return;
        mScanText.setText(getString(R.string.scan_on));
        mScanProgressBar.setVisibility(View.VISIBLE);
        mScanProgressBarAnimator.start();
        scanning = true;
        //log("scanning for rings");
        // Start scanning for devices that support the Moodmetric service
        if(bluetoothManager.getConnectedDevices(BluetoothProfile.GATT).size() == 0){
        }else{
            List<BluetoothDevice> connectedDevices = bluetoothManager.getConnectedDevices(BluetoothProfile.GATT);
            for(int i=0; i<=connectedDevices.size()-1; i++ ){
                if(!(connectedDevices.get(i).getAddress()).equals(SharedPrefManager.getInstance(mContext).getGarminDeviceAddress()))
                {
                    log("MoodMetric " + connectedDevices.get(i).getAddress() + " - CONNECTED", connectedDevices.get(i));
                }
            }
        }
        bluetoothAdapter.startLeScan(new UUID[] { mmServiceUUID }, scanCallback);
/*        timer = new Timer();
        timerTask = new TimerTask() {
            public void run() {
                stopScanning();
            }
        };
        timer.schedule(timerTask, 5000); //*/
        Runnable progressRunnable = new Runnable() {

            @Override
            public void run() {
                mScanSwitch.setChecked(false);
                stopScanning();
            }
        };

        Handler pdCanceller = new Handler();
        pdCanceller.postDelayed(progressRunnable, 5000);

    }

    private void stopScanning() {
        if (!scanning) return;
        mScanText.setText(getString(R.string.scan_off));
        mScanProgressBar.setVisibility(View.GONE);

        scanning = false;
        //log("scanning stopped");
        bluetoothAdapter.stopLeScan(scanCallback);
    }

    private void log(final String fmt, BluetoothDevice device) {
/*        logView.append(String.format(fmt+"\n",args));
        scrollView.fullScroll(View.FOCUS_DOWN);*/
        listItems.add(fmt);
        deviceList.add(device);
        adapter.notifyDataSetChanged();
    }

    private void log1(final String fmt, final Object... args) {
        // Updates to the UI should be done from the UI thread
        logView.append(String.format(fmt+"\n",args));
        Log.i("ringData",String.format(fmt+"\n",args));
   }

    public boolean onBackPressed(){
        //fm.popBackStackImmediate();
        if (scanning){
            mScanSwitch.setChecked(false);
            stopScanning();
        }

        return true;
    }
}