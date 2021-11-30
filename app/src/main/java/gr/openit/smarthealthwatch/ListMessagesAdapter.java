package gr.openit.smarthealthwatch;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.garmin.health.settings.Alert;

import java.util.ArrayList;

public class ListMessagesAdapter extends ArrayAdapter<MessageRow> {

    private ArrayList<MessageRow> dataSet;
    Context mContext;
    RelativeLayout message_row;
    UserHome uh;
    FragmentMessages fm;
    // View lookup cache
    private static class ViewHolder {
        TextView sender;
        TextView id;
        TextView date;
        TextView data;
    }

    public ListMessagesAdapter(ArrayList<MessageRow> data, Context context, UserHome uh, FragmentMessages fm) {
        super(context, R.layout.list_row_item_message, data);
        this.dataSet = data;
        this.mContext= context;
        this.uh = uh;
        this.fm = fm;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        MessageRow dataModel = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        ViewHolder viewHolder; // view lookup cache stored in tag.
        final View result;
        if (convertView == null) {

            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.list_row_item_message, parent, false);
            viewHolder.sender = (TextView) convertView.findViewById(R.id.message_title);
            viewHolder.id = (TextView) convertView.findViewById(R.id.message_id);
            viewHolder.date = (TextView) convertView.findViewById(R.id.message_time);
            viewHolder.data = (TextView) convertView.findViewById(R.id.message_data);
            convertView.setTag(viewHolder);

        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }


        if(!dataModel.getRead()){
            convertView.setBackground(mContext.getResources().getDrawable(R.drawable.border_shape_unread));
        }else{
            convertView.setBackground(mContext.getResources().getDrawable(R.drawable.border_shape));
        }
        viewHolder.sender.setText(mContext.getResources().getString(R.string.message_from, dataModel.getSender()));
        viewHolder.id.setText(dataModel.getId());
        viewHolder.date.setText(dataModel.getDate());
        viewHolder.data.setText(dataModel.getData().toString());
        message_row = convertView.findViewById(R.id.message_layout);
        message_row.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("StringFormatMatches")
            @Override
            public void onClick(View v) {
                //Toast.makeText(mContext,"Pathsa to "+viewHolder.id.getText(),Toast.LENGTH_LONG).show();
                showMessageFragmentTransition(viewHolder.data.getText().toString(), dataModel.isMessage());
            }
        });
        return convertView;
    }

    private void showMessageFragmentTransition(String data, Boolean isMessage){
        Fragment messageFragment = new FragmentShowMessage(mContext, data, fm,isMessage,uh);
        FragmentManager fm = ((MainActivity)mContext).getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
        transaction.replace(R.id.main_container,messageFragment,"toBePoped"); // give your fragment container id in first parameter
        transaction.addToBackStack(null);
        transaction.commit();
        this.uh.active = messageFragment;
        this.uh.setHasOptionsMenu(false);
        this.uh.toolbarTitleBack();
    }
    @Override
    public boolean isEnabled(int position) {
        return false;
    }

}