package com.onlybeyond.MediaPlayer.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.onlybeyond.MediaPlayer.R;

/**
 * Created by only on 15/12/2.
 */
public class LoadingView extends RelativeLayout {
    private Context mContext;
    private TextView tvLoading;

    public LoadingView(Context context) {
        this(context, null);
    }

    public LoadingView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
         mContext=context;
        init();
    }

    public LoadingView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    public void init(){
        LayoutInflater.from(mContext).inflate(R.layout.widget_loading_view, this);
        tvLoading = (TextView) findViewById(R.id.tv_loading);
    }
    public void setLoadingText(String text){
        tvLoading.setText(text);
    }



}
