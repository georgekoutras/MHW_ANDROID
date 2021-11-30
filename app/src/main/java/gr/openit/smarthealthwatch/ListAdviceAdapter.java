package gr.openit.smarthealthwatch;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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

public class ListAdviceAdapter extends ArrayAdapter<AdviceRow> {

    private ArrayList<AdviceRow> dataSet;
    Context mContext;
    RelativeLayout advice_row;
    UserHome uh;
    FragmentAdvices fa;
    // View lookup cache
    private static class ViewHolder {
        TextView title;
        TextView id;
    }

    public ListAdviceAdapter(ArrayList<AdviceRow> data, Context context, UserHome uh, FragmentAdvices fa) {
        super(context, R.layout.list_row_item_advice, data);
        this.dataSet = data;
        this.mContext=context;
        this.uh = uh;
        this.fa = fa;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        AdviceRow dataModel = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        ViewHolder viewHolder; // view lookup cache stored in tag.
        final View result;
        if (convertView == null) {

            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.list_row_item_advice, parent, false);
            viewHolder.title = (TextView) convertView.findViewById(R.id.advice_title);
            viewHolder.id = (TextView) convertView.findViewById(R.id.advice_id);

            convertView.setTag(viewHolder);

        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.title.setText(dataModel.getTitle());
        viewHolder.id.setText(dataModel.getId());
        advice_row = convertView.findViewById(R.id.advice_layout);
        advice_row.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("StringFormatMatches")
            @Override
            public void onClick(View v) {
                //Toast.makeText(mContext,"Pathsa to "+viewHolder.id.getText(),Toast.LENGTH_LONG).show();
                showAdviceFragmentTransition(viewHolder.id.getText().toString());
            }
        });
        return convertView;
    }

    private void showAdviceFragmentTransition(String id){
        Fragment adviceFragment = new FragmentShowAdvice(mContext, id, fa,uh);
        FragmentManager fm = ((MainActivity)mContext).getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
        transaction.replace(R.id.main_container,adviceFragment,"toBePoped"); // give your fragment container id in first parameter
        transaction.addToBackStack(null);
        transaction.commit();
        this.uh.active = adviceFragment;
        this.uh.setHasOptionsMenu(false);
        this.uh.toolbarTitleBack();
    }
    @Override
    public boolean isEnabled(int position) {
        return false;
    }

}