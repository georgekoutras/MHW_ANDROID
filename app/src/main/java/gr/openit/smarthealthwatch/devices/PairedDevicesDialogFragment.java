package gr.openit.smarthealthwatch.devices;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import gr.openit.smarthealthwatch.GarminCustomService;
import gr.openit.smarthealthwatch.R;
import gr.openit.smarthealthwatch.UserHome;
import gr.openit.smarthealthwatch.adapters.PairedDeviceListAdapter;
import gr.openit.smarthealthwatch.pairing.ScanningDialogFragment;
import gr.openit.smarthealthwatch.util.ConfirmationDialog;
import gr.openit.smarthealthwatch.util.SharedPrefManager;

import com.garmin.health.*;
import com.garmin.health.bluetooth.FailureCode;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows a list of paired Garmin devices. It also provides a way to perform bluetooth scanning and to add a new device.
 *
 * @author morajkar
 */
public class PairedDevicesDialogFragment extends DialogFragment implements DeviceConnectionStateListener
{
    public static final String DEVICE_ARG = "DEVICE_ARG";
    public final static String DEVICE_ADDRESS_EXTRA = "device.address";

    private PairedDeviceListAdapter mListAdapter;
    UserHome uh;
    Context mContext;
    public PairedDevicesDialogFragment(Context mContext, UserHome uh) {
        // Required empty public constructor
        super();
        this.uh = uh;
        this.mContext = mContext;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState)
    {
        setRetainInstance(true);
        View view = inflater.inflate(R.layout.fragment_paired_devices, container, false);

        DeviceManager deviceManager = DeviceManager.getDeviceManager();
        if(deviceManager != null) {
            List<Device> devices = new ArrayList<>(deviceManager.getPairedDevices());


            ListView mPairedDevicesList = view.findViewById(R.id.paired_devices_listview);
            mListAdapter = new PairedDeviceListAdapter(getContext(), devices, new DeviceItemClickListener());
            mPairedDevicesList.setAdapter(mListAdapter);

            //mPairedDevicesList.setOnItemLongClickListener((adapterView, view1, i, l) ->
            mPairedDevicesList.setOnItemClickListener((adapterView, v, i, l) ->
            {
                Device device = (Device) adapterView.getItemAtPosition(i);
                if (device.connectionState() != ConnectionState.CONNECTED) {
                    Toast.makeText(getContext(), "H συσκευή δεν είναι συνδεδεμένη", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (device.setupState() != SetupState.COMPLETE) {
                    Toast.makeText(getContext(), "Η σύνδεση της συσκευής δεν ολοκληρώθηκε", Toast.LENGTH_SHORT).show();
                    return;
                }

                return;
            });

            FloatingActionButton mAddDeviceButton = view.findViewById(R.id.add_device_button);
            mAddDeviceButton.setOnClickListener(mAddButtonListener);

            // if there are no paired devices, show an alert to begin the device scan
        /*if(devices.isEmpty())
        {
            ConfirmationDialog scanningConfirm = new ConfirmationDialog(getContext(), getString(R.string.scan_devices_title), getString(R.string.scan_devices_msg), getString(R.string.alert_dialog_ok),
                            getString(R.string.alert_dialog_cancel), new ScanningBeginClickListener());
            scanningConfirm.show();
        }*/
        }
        return view;
    }

    @Override
    public void onResume()
    {
        super.onResume();
        this.uh.hideMenu();
        this.uh.hideUnity();
        //this.uh.toolbarTitleBack();
        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();

        DeviceManager.getDeviceManager().addConnectionStateListener(this);

        if (actionBar != null)
        {
            actionBar.setTitle(R.string.connected_devices_title);
        }
    }

    @Override
    public void onStop()
    {
        super.onStop();
    }

    private View.OnClickListener mAddButtonListener = view ->
    {
        //Transitions to a new fragment to begin the Bluetooth scanning for the Garmin devices nearby
        ScanningDialogFragment scanningDialogFragment = new ScanningDialogFragment(uh);

        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        transaction.addToBackStack(null);
        transaction.replace(R.id.main_container, scanningDialogFragment, scanningDialogFragment.getTag()).addToBackStack(getTag()).commit();
    };

    @Override
    public void onDeviceConnected(@NonNull Device device)
    {
        mListAdapter.notifyDataSetChanged();
    }

    @Override
    public void onDeviceDisconnected(@NonNull Device device)
    {
        mListAdapter.notifyDataSetChanged();
    }

    @Override
    public void onDeviceConnectionFailed(@NonNull Device device, @NonNull FailureCode failure)
    {
        Context context = getContext();

        if(context != null)
        {
            Toast.makeText(getContext(), String.format("Απέτυχε η σύνδεση με την συσκευή. [%s]", device.address()), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Listens to scanning begin click
     */
    private class ScanningBeginClickListener implements DialogInterface.OnClickListener
    {
        @Override
        public void onClick(DialogInterface dialogInterface, int i)
        {
            if(i == DialogInterface.BUTTON_POSITIVE)
            {
                ScanningDialogFragment scanningDialogFragment = new ScanningDialogFragment(uh);

                FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                FragmentTransaction transaction = fragmentManager.beginTransaction();
                transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                transaction.addToBackStack(null);
                transaction.replace(R.id.main_container, scanningDialogFragment, scanningDialogFragment.getTag()).addToBackStack(getTag()).commit();
            }
        }
    }

    /**
     * Listener to handle the various click actions on device list item.
     */
    private class DeviceItemClickListener implements PairedDeviceListAdapter.OnItemClickListener
    {
        @Override
        public void onForgetDeviceClick(final Device device)
        {
            Log.d(getTag(), "onForgetDeviceClick(device = " + device.friendlyName() + ")");
            ConfirmationDialog dialog = new ConfirmationDialog(getContext(), null, getString(R.string.connected_forget_device_message), getString(R.string.button_yes), getString(R.string.button_no),
                    (dialog1, which) ->
                    {
                        if (which == DialogInterface.BUTTON_POSITIVE)
                        {
                            GarminCustomService gcService = new GarminCustomService();
                            Intent gattServiceIntent = new Intent(mContext, gcService.getClass());
                            gattServiceIntent.putExtra("stop_service",true);
                            getActivity().startService(gattServiceIntent);
                            DeviceManager.getDeviceManager().forget(device.address());
                            mListAdapter.removeDevice(device.address());
                            SharedPrefManager.getInstance(getContext()).setGarminDeviceAddress(null);
                            getActivity().stopService(gattServiceIntent);
                        }
                    });
            dialog.show();
        }
    }
}
