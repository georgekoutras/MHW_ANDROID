package gr.openit.smarthealthwatch.ui;

import android.content.*;
import android.os.Build;
import android.util.Log;

import gr.openit.smarthealthwatch.CoughService;

public class MoodmetricServiceReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("Broadcast Listened", "Service tried to stop");
        if(intent != null){
            if(intent.getAction().equals("restart_service_ring")){
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(new Intent(context, BluetoothLeService.class));
                } else {
                    context.startService(new Intent(context, BluetoothLeService.class));
                }
            }else if(intent.getAction().equals("restart_service_cough")){
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(new Intent(context, CoughService.class));
                } else {
                    context.startService(new Intent(context, CoughService.class));
                }
            }
        }

    }
}
