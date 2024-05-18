package com.example.yueying2.custom.lyrics;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.v4.media.session.MediaControllerCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.yueying2.R;
import com.example.yueying2.util.PictureUtil;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * @author : Bilibili喜闻人籁
 * @since : 2021/9/27
 * 作用:
 */
public class MyLrcView extends RelativeLayout implements View.OnClickListener {

    private static final String TAG = "MyLrcView";

    private ImageView iv_touch_play, iv_line, iv_translate,
             iv_text_size, iv_color;//TouchView
    private LrcView mLrcView; //自定义歌词控件
    private TextView tv_touch_time; //TouchView
    private DateFormat mDateFormat; //(long) ms 转化为 00:00 格式
    private MediaControllerCompat mMediaController; //服务引用
    private boolean isTranslate = false;

    public MyLrcView(Context context) {
        super(context);
        Log.d(TAG, "MyLrcView: 1");
        initView((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE));
    }

    public MyLrcView(Context context, AttributeSet attrs) {
        super(context, attrs);
        Log.d(TAG, "MyLrcView: 2");
        initView((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE));
    }

    public MyLrcView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        Log.d(TAG, "MyLrcView: 3");
    }

    public MyLrcView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Log.e(TAG, "onDetachedFromWindow: ");
        release();
    }

    private void release() {
        if (mMediaController != null) mMediaController = null;
        if (mDateFormat != null) mDateFormat = null;
        if (mLrcView != null) {
            mLrcView.release();
            mLrcView = null;
        }
        if (iv_translate != null) iv_translate = null;
        if (iv_touch_play != null) iv_touch_play = null;
        if (iv_text_size != null) iv_text_size = null;
        if (iv_color != null) iv_color = null;
        if (tv_touch_time != null) tv_touch_time = null;
        if (iv_line != null) iv_line = null;
    }

    private void initView(LayoutInflater layoutInflater){
        View view = layoutInflater.inflate(R.layout.custom_lrc_view, this, true);
        mLrcView = view.findViewById(R.id.my_lrc_view);
        mLrcView.setSuperLayout(this);
        Log.d(TAG, "initView: "+(mLrcView == null));

        mDateFormat = new SimpleDateFormat("mm:ss", Locale.CHINA);

        iv_touch_play = view.findViewById(R.id.my_lrc_view_iv_touch_play);
        iv_touch_play.setOnClickListener(this);
        iv_translate = view.findViewById(R.id.my_lrc_view_iv_translate);
        iv_translate.setOnClickListener(this);
        iv_text_size = view.findViewById(R.id.my_lrc_view_iv_size);
        iv_text_size.setOnClickListener(this);
        iv_color = view.findViewById(R.id.my_lrc_view_iv_color);
        iv_color.setOnClickListener(this);
        iv_color.setImageDrawable(PictureUtil.createColorDrawable(
                getContext().getApplicationContext(),mLrcView.getHighLineColor(),48,48));

        tv_touch_time = view.findViewById(R.id.my_lrc_view_tv_touch_time);

        iv_line = view.findViewById(R.id.my_lrc_view_iv_line);


    }

    public void setMediaController(MediaControllerCompat mediaController) {
        this.mMediaController = mediaController;
        if (mLrcView != null) mLrcView.setMediaControllerCompat(mMediaController);
        else Log.e(TAG, "setMediaController: lrcView 为空！");
    }

    public MyLrcView setLrc(List<LrcBean> lrcBeans){
        Log.d(TAG, "setLrc: "+(mLrcView == null));
        //Log.d(TAG, "setLrc: "+(lrcBeans == null));
        int size = lrcBeans == null ? 0 : lrcBeans.size();
        String translate = size == 0 ? null : lrcBeans.get(size >> 1).getTranslateLrc();
        if(translate == null && lrcBeans != null){
            translate = lrcBeans.get((size >> 1) + 1).getTranslateLrc();
            if(translate == null) translate = lrcBeans.get(size >> 2).getTranslateLrc();
        }//第一次获取翻译有可能获取不到，最多再获取两次翻译歌词
        isTranslate = size > 3 && translate != null && !TextUtils.isEmpty(translate);
        mLrcView.setShowTranslate(isTranslate);
        iv_translate.setAlpha(isTranslate ? 1f : 0.6f);
        //Log.d(TAG, "setLrc: "+size+", "+translate+", "+isTranslate);
        if (mLrcView != null) mLrcView.setLrc(lrcBeans);
        return this;
    }
    public void setLrcTips(boolean tips){
        if (mLrcView != null) mLrcView.setTips(tips);
    }
    public void UpdateMusicProcess(){
        if (mLrcView != null) mLrcView.UpdateMusicProcess();
    }
    public void showLrcInfoView(){
        Log.d(TAG, "showLrcInfoView: "+(iv_touch_play == null));
        if (iv_line != null) iv_line.setVisibility(VISIBLE);
        if (iv_touch_play != null) iv_touch_play.setVisibility(VISIBLE);
        if (tv_touch_time != null) tv_touch_time.setVisibility(VISIBLE);
    }
    public void hideLrcInfoView(){
        if (iv_line != null) iv_line.setVisibility(INVISIBLE);
        if (iv_touch_play != null) iv_touch_play.setVisibility(INVISIBLE);
        if (tv_touch_time != null) tv_touch_time.setVisibility(INVISIBLE);
    }
    /** 设置右侧 的 （StartTime）TextView*/
    public void setText(long startTime){
        if (mDateFormat != null && tv_touch_time != null) {
            if (tv_touch_time.getVisibility() == VISIBLE)
                tv_touch_time.setText(mDateFormat.format(startTime));
        }
    }

    /** 00:00 转化为整型（int）ms[毫秒]*/
    private int getTime(String musicProcess){
        int second = Integer.parseInt(musicProcess.substring(musicProcess.indexOf(":")+1));
        int minute = Integer.parseInt(musicProcess.substring(musicProcess.indexOf(":")-2,musicProcess.indexOf(":")));
        return minute * 60000 + second * 1000;
    }

    public void init(){
        Log.d(TAG, "init: "+(mLrcView == null));
        if (mLrcView != null) mLrcView.init();
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.my_lrc_view_iv_touch_play) {
            try {
                Log.d(TAG, "onClick: ");
                assert tv_touch_time != null;
                long process = getTime(tv_touch_time.getText().toString());
                process = process > 0 ? process + 500 : 0;

                if (mMediaController == null) {
                    Log.e(TAG, "mMediaController 为空 ");
                    Toast.makeText(getContext(), "无法向服务发送更改播放进度请求，请请重新进入此界面 ", Toast.LENGTH_SHORT).show();
                    return;
                }

                mMediaController.getTransportControls().seekTo(process);
            } catch (Exception e) {
                Toast.makeText(getContext(), "获取歌曲播放进度失败，请重新进入此界面", Toast.LENGTH_SHORT).show();
            }
        }else if(v.getId() == R.id.my_lrc_view_iv_translate){
            if (!isTranslate || mLrcView == null) return;
            mLrcView.changeShowTranslate();
            iv_translate.setAlpha(mLrcView.isShowTranslate() ? 1f : 0.6f);
        }else if(v.getId() == R.id.my_lrc_view_iv_size){
            if (mLrcView == null) return;
            mLrcView.setTextSize();
        }else if(v.getId() == R.id.my_lrc_view_iv_color){
            if (mLrcView == null) return;
            iv_color.setImageDrawable(PictureUtil.createColorDrawable(
                    getContext(),mLrcView.setTextColor(),48,48));

        }
    }
}
