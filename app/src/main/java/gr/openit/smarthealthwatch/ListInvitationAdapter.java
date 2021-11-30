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

public class ListInvitationAdapter extends ArrayAdapter<InvitationRow> {

    private ArrayList<InvitationRow> dataSet;
    Context mContext;
    RelativeLayout invitation_row;
    UserHome uh;
    FragmentInvitations fi;
    // View lookup cache
    private static class ViewHolder {
        TextView name;
        TextView id;
    }

    public ListInvitationAdapter(ArrayList<InvitationRow> data, Context context, UserHome uh, FragmentInvitations fi) {
        super(context, R.layout.list_row_item_invitation, data);
        this.dataSet = data;
        this.mContext=context;
        this.uh = uh;
        this.fi = fi;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        InvitationRow dataModel = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        ListInvitationAdapter.ViewHolder viewHolder; // view lookup cache stored in tag.
        final View result;
        if (convertView == null) {

            viewHolder = new ListInvitationAdapter.ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.list_row_item_invitation, parent, false);
            viewHolder.name = (TextView) convertView.findViewById(R.id.invitation_sender);
            viewHolder.id = (TextView) convertView.findViewById(R.id.invitation_id);

            convertView.setTag(viewHolder);

        } else {
            viewHolder = (ListInvitationAdapter.ViewHolder) convertView.getTag();
        }

        if(!dataModel.getSeen() && dataModel.isIncoming()){
            convertView.setBackground(mContext.getResources().getDrawable(R.drawable.border_shape_unread));
        }else{
            convertView.setBackground(mContext.getResources().getDrawable(R.drawable.border_shape));
        }

        viewHolder.name.setText(dataModel.getSender() + " - " + dataModel.getRelation());
        viewHolder.id.setText(dataModel.getId());
        invitation_row = convertView.findViewById(R.id.invitation_layout);
        invitation_row.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("StringFormatMatches")
            @Override
            public void onClick(View v) {
                //Toast.makeText(mContext,"Pathsa to "+viewHolder.id.getText(),Toast.LENGTH_LONG).show();
                showInvitationFragmentTransition(viewHolder.id.getText().toString(), dataModel.isIncoming());
            }
        });
        return convertView;
    }

    private void showInvitationFragmentTransition(String id, Boolean isIncoming){
        Fragment invitationFragment = new FragmentShowInvitation(mContext, id, fi,uh, isIncoming);
        FragmentManager fm = ((MainActivity)mContext).getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
        transaction.replace(R.id.main_container,invitationFragment); // give your fragment container id in first parameter
        transaction.addToBackStack(null);
        transaction.commit();
        this.uh.active = invitationFragment;
        this.uh.setHasOptionsMenu(false);
        this.uh.toolbarTitleBack();
        this.uh.hideMenu();
    }
    @Override
    public boolean isEnabled(int position) {
        return false;
    }
}
