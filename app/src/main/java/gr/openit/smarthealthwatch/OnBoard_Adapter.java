package gr.openit.smarthealthwatch;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;

import androidx.viewpager.widget.PagerAdapter;

import java.util.ArrayList;


/**
 * Created by Jaison
 */


class OnBoard_Adapter extends PagerAdapter {

    private Context mContext;
    ArrayList<OnBoardItem> onBoardItems=new ArrayList<>();
    private int ONBOARD_PAGE_COUNT = 7;

    public OnBoard_Adapter(Context mContext, ArrayList<OnBoardItem> items) {
        this.mContext = mContext;
        this.onBoardItems = items;
    }

    @Override
    public int getCount() {
        return ONBOARD_PAGE_COUNT;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {

        View itemView ;
        //itemView = LayoutInflater.from(mContext).inflate(R.layout.onboard_item, container, false);

        if(position==0){
            itemView = LayoutInflater.from(mContext).inflate(R.layout.onboard_item1, container, false);
        }else if(position==1){
            itemView = LayoutInflater.from(mContext).inflate(R.layout.onboard_item2, container, false);;
        }else if(position==2){
            itemView = LayoutInflater.from(mContext).inflate(R.layout.onboard_item3, container, false);;
        }else if(position==3){
            itemView = LayoutInflater.from(mContext).inflate(R.layout.onboard_item4, container, false);;
        }else if(position==4){
            itemView = LayoutInflater.from(mContext).inflate(R.layout.onboard_item5, container, false);;
        }else if(position==5){
            itemView = LayoutInflater.from(mContext).inflate(R.layout.onboard_item6, container, false);;
        }else {
            itemView = LayoutInflater.from(mContext).inflate(R.layout.onboard_item7, container, false);;
        }

/*        OnBoardItem item=onBoardItems.get(position);

        ImageView imageView = (ImageView) itemView.findViewById(R.id.iv_onboard);
        imageView.setImageResource(item.getImageID());

        TextView tv_title=(TextView)itemView.findViewById(R.id.tv_header);
        tv_title.setText(item.getTitle());

        TextView tv_content=(TextView)itemView.findViewById(R.id.tv_desc);
        tv_content.setText(item.getDescription())*/;

        container.addView(itemView);

        return itemView;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((ScrollView) object);
    }

}
