package gr.openit.smarthealthwatch;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager.widget.ViewPager;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link OnBoardingFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class OnBoardingFragment extends Fragment {

    private ViewPager onboarding_pager;
    int previous_pos=0;
    int curr_pos_btn = 0;
    Button mSkipBtn, mFinishBtn,mPrevBtn,mNextBtn;
    private OnBoard_Adapter mAdapter;

    private Button btn_get_started;
    private ImageView bottom_logo;
    private FrameLayout eu_erdf;
    private int dotsCount;
    private ImageView[] dots;
    ArrayList<OnBoardItem> onBoardItems=new ArrayList<>();

    public OnBoardingFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment OnBoardingFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static OnBoardingFragment newInstance(String param1, String param2) {
        OnBoardingFragment fragment = new OnBoardingFragment();
        Bundle args = new Bundle();
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

        setRetainInstance(true);
        View root = (View) inflater.inflate(R.layout.activity_on_boarding, container, false);
        View rootView = inflater.inflate(R.layout.onboard_item1, container, false);
        onboarding_pager = (ViewPager) root.findViewById(R.id.pager_introduction);
        mAdapter = new OnBoard_Adapter(getContext(),onBoardItems);
        onboarding_pager.setAdapter(mAdapter);
        onboarding_pager.setCurrentItem(0);


        btn_get_started = (Button) root.findViewById(R.id.btn_get_started);
        //pager_indicator = (LinearLayout) findViewById(R.id.viewPagerCountDots);

        //loadData();
        mNextBtn = (Button) root.findViewById(R.id.intro_btn_next);
        mPrevBtn = (Button) root.findViewById(R.id.intro_btn_prev);

        bottom_logo = root.findViewById(R.id.logo_bottom);
        eu_erdf = root.findViewById(R.id.eu_erdf);

        // TODO: Rename and change types of parameters

        onboarding_pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                curr_pos_btn = position;

                int pos=position+1;

                if(position==0)
                    show_animation();
                else
                    hide_animation();

                previous_pos=pos;

                mPrevBtn.setVisibility(position == 0 ? View.GONE : View.VISIBLE);

            }


            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        mNextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //mNextBtn.setVisibility(curr_pos_btn == 6 ? View.GONE : View.VISIBLE);
                if(curr_pos_btn == 6){
                    //Toast.makeText(OnBoardingActivity.this,"Redirect to wherever you want",Toast.LENGTH_LONG).show();
                    Fragment registerFragment = new RegisterFragment();
                    FragmentManager fm = getFragmentManager();
                    FragmentTransaction transaction = fm.beginTransaction();
                    transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
                    transaction.replace(R.id.main_root_ob,registerFragment); // give your fragment container id in first parameter
                    //transaction.addToBackStack(null);  // if written, this transaction will be added to backstack
                    transaction.commit();
                }else{
                    onboarding_pager.setCurrentItem(curr_pos_btn=curr_pos_btn+1, true);
                    mPrevBtn.setVisibility(curr_pos_btn == 0 ? View.GONE : View.VISIBLE);
                }
            }

        });

        mPrevBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onboarding_pager.setCurrentItem(curr_pos_btn=curr_pos_btn-1, true);
                //mNextBtn.setVisibility(curr_pos_btn == 6 ? View.GONE : View.VISIBLE);
                mPrevBtn.setVisibility(curr_pos_btn == 0 ? View.GONE : View.VISIBLE);
            }

        });
        btn_get_started.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Fragment registerFragment = new RegisterFragment();
                FragmentManager fm = getFragmentManager();
                FragmentTransaction transaction = fm.beginTransaction();
                transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
                transaction.replace(R.id.main_root_ob,registerFragment); // give your fragment container id in first parameter
                //transaction.addToBackStack(null);  // if written, this transaction will be added to backstack
                transaction.commit();
                //Toast.makeText(OnBoardingActivity.this,"Redirect to wherever you want",Toast.LENGTH_LONG).show();
            }
        });

        setUiPageViewController();

        // Inflate the layout for this fragment
        return root;
    }

    public void show_animation()
    {
        if(bottom_logo.getVisibility() == View.VISIBLE)
            return;

        final float scale = getContext().getResources().getDisplayMetrics().density;
        int pixels = (int) (64 * scale + 0.5f);
        eu_erdf.getLayoutParams().height = pixels;
        bottom_logo.setVisibility(View.VISIBLE);
    }

    // Button Topdown animation

    public void hide_animation()
    {
        if(bottom_logo.getVisibility() == View.GONE)
            return;

        bottom_logo.setVisibility(View.GONE);
        eu_erdf.getLayoutParams().height = 0;
    }

    // setup the
    private void setUiPageViewController() {

        dotsCount = mAdapter.getCount();
        dots = new ImageView[dotsCount];

        for (int i = 0; i < dotsCount; i++) {
            dots[i] = new ImageView(getContext());
            dots[i].setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.non_selected_item_dot));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );

            params.setMargins(6, 0, 6, 0);
        }

        dots[0].setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.selected_item_dot));
    }
}