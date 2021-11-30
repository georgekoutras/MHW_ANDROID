package gr.openit.smarthealthwatch;

import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link FragmentRegisterConfirmation#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FragmentRegisterConfirmation extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private Context mContext;

    public FragmentRegisterConfirmation(Context mContext) {
        // Required empty public constructor
        this.mContext = mContext;
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment FragmentRegisterConfirmation.
     */
    // TODO: Rename and change types and number of parameters
    public static FragmentRegisterConfirmation newInstance(String param1, String param2) {
        FragmentRegisterConfirmation fragment = new FragmentRegisterConfirmation(null);
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View root ;
        root = LayoutInflater.from(mContext).inflate(R.layout.fragment_register_confirmation, container, false);
        Button login_now = root.findViewById(R.id.btn_login_now);

        login_now.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginTransition();
            }

        });
        return root;
    }

    private void loginTransition(){
        Fragment loginFragment = new LoginFragment(mContext);
        FragmentManager fm = ((MainActivity)mContext).getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
        transaction.replace(R.id.main_root,loginFragment); // give your fragment container id in first parameter
        fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        transaction.commit();
    }
}