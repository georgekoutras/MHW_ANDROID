package gr.openit.smarthealthwatch;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import java.util.ArrayList;

public class ListContactAdapter  extends ArrayAdapter<ContactRow>  {
    private ArrayList<ContactRow> dataSet;
    Context mContext;
    RelativeLayout contact_row;
    UserHome uh;
    FragmentContacts fc;
    // View lookup cache
    private static class ViewHolder {
        TextView name;
        TextView id;
    }

    public ListContactAdapter(ArrayList<ContactRow> data, Context context, UserHome uh, FragmentContacts fc) {
        super(context, R.layout.list_row_item_contact, data);
        this.dataSet = data;
        this.mContext=context;
        this.uh = uh;
        this.fc = fc;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        ContactRow dataModel = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        ListContactAdapter.ViewHolder viewHolder; // view lookup cache stored in tag.
        final View result;
        if (convertView == null) {

            viewHolder = new ListContactAdapter.ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.list_row_item_contact, parent, false);
            viewHolder.name = (TextView) convertView.findViewById(R.id.contact_name);
            viewHolder.id = (TextView) convertView.findViewById(R.id.contact_id);

            convertView.setTag(viewHolder);

        } else {
            viewHolder = (ListContactAdapter.ViewHolder) convertView.getTag();
        }

        viewHolder.name.setText(dataModel.getName());
        viewHolder.id.setText(dataModel.getId());
        contact_row = convertView.findViewById(R.id.contact_layout);
        contact_row.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("StringFormatMatches")
            @Override
            public void onClick(View v) {
                //Toast.makeText(mContext,"Pathsa to "+viewHolder.id.getText(),Toast.LENGTH_LONG).show();
                showContactFragmentTransition(viewHolder.id.getText().toString());
            }
        });
        return convertView;
    }

    private void showContactFragmentTransition(String id){
        Fragment contactFragment = new FragmentShowContact(mContext, id, fc, uh);
        FragmentManager fm = ((MainActivity)mContext).getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
        transaction.replace(R.id.main_container,contactFragment,"toBePoped"); // give your fragment container id in first parameter
        transaction.addToBackStack(null);
        transaction.commit();
        this.uh.active = contactFragment;
        this.uh.setHasOptionsMenu(false);
        this.uh.toolbarTitleBack();
        this.uh.hideMenu();
        this.uh.hideUnity();
    }
    @Override
    public boolean isEnabled(int position) {
        return false;
    }
}
