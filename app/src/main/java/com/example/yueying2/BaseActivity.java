package com.example.yueying2;

import android.animation.ObjectAnimator;
import android.content.ComponentName;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media.MediaBrowserServiceCompat;

import com.example.yueying2.service.MusicService;
import com.example.yueying2.service.MusicService;
import com.example.yueying2.viewmodel.MusicViewModel;

import java.lang.ref.SoftReference;

/**
 * @author : Hyena王紫涓
 * @since : 2022-06-01
 * 作用 基础Activity
 */
public abstract class BaseActivity<M extends MusicViewModel> extends AppCompatActivity implements View.OnApplyWindowInsetsListener {

    private static final String TAG = "BaseActivity";

    private MediaBrowserCompat mMediaBrowser;

    private MediaControllerCompat.Callback mControllerCallback;
    private MediaBrowserCompat.SubscriptionCallback mSubscriptionCallback;
    //上述变量的抽象方法
    protected abstract MediaControllerCompat.Callback getControllerCallback();
    protected abstract MediaBrowserCompat.SubscriptionCallback getSubscriptionCallback();
    //动画
    private Animation mLoadingAnimation;
    private ObjectAnimator mRecordAnimator;
    //设备参数
    protected int mRefreshRateMax,mPhoneWidth,mPhoneHeight;
    protected boolean isPad, backToDesktop, isLifePauseAnimator, isFirstResume;

    @Override
    protected void onCreate(@Nullable Bundle saveInstanceState){
        super.onCreate(saveInstanceState);
        HighHzAdaptation();
        initMediaBrowser();
        initRotateAnimation();
    }

    //断开连接
    private void disConnect(){
        if (MediaControllerCompat.getMediaController(BaseActivity.this) != null) {
            MediaControllerCompat.getMediaController(BaseActivity.this).unregisterCallback(mControllerCallback);
        }
        if (mMediaBrowser.isConnected()) {
            mMediaBrowser.unsubscribe(mMediaBrowser.getRoot());
            mMediaBrowser.disconnect();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart: ");
        if (!mMediaBrowser.isConnected()) { mMediaBrowser.connect(); }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: ");
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: ");
        releaseMediaBrowser();
        if (mLoadingAnimation != null){
            mLoadingAnimation.reset();
            mLoadingAnimation.cancel();
            mLoadingAnimation = null;
        }
        if (mRecordAnimator != null) {
            mRecordAnimator.pause();
            mRecordAnimator.cancel();
            mRecordAnimator = null;
        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    @Override
    public WindowInsets onApplyWindowInsets(View view, WindowInsets windowInsets) {
        Log.d(TAG, "onApplyWindowInsets:" + windowInsets.getSystemWindowInsetTop());
        if (windowInsets.getSystemWindowInsetBottom() < 288){
            int paddingBottom = windowInsets.getSystemWindowInsetBottom();
            view.setPadding(windowInsets.getSystemWindowInsetLeft(), windowInsets.getSystemWindowInsetTop(), windowInsets.getSystemWindowInsetRight(), paddingBottom);
        }
        return windowInsets;
    }

    private void initMediaBrowser(){
        mControllerCallback = getControllerCallback();
        mSubscriptionCallback = getSubscriptionCallback();

        mMediaBrowser = new MediaBrowserCompat(this,
                new ComponentName(this, MusicService.class),
                new MyMediaBrowserConnectionCallback(), null);
    }

    private class MyMediaBrowserConnectionCallback extends MediaBrowserCompat.ConnectionCallback{
        @Override
        public void onConnected() {
            super.onConnected();
            Log.d(TAG, "onConnected: 连接成功");

            // 获得MediaSession的Token口令
            MediaSessionCompat.Token token = mMediaBrowser.getSessionToken();

            // 初始化MediaControllerCompat
            MediaControllerCompat mediaController = null;
            try {
                mediaController = new MediaControllerCompat(BaseActivity.this, token);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            if (mediaController == null) { return; }
            // 保存controller
            MediaControllerCompat.setMediaController(BaseActivity.this, mediaController);
            //注册controller回调以保持数据同步
            mediaController.registerCallback(mControllerCallback);

            //订阅的回调
            String mediaId = mMediaBrowser.getRoot();
            mMediaBrowser.unsubscribe(mediaId);
            //向服务订阅音乐列表集合信息！
            mMediaBrowser.subscribe(mediaId,mSubscriptionCallback);
        }

        @Override
        public void onConnectionSuspended() {
            super.onConnectionSuspended();
            //服务崩溃了。禁用传输控制，直到它自动重新连接
            Log.d(TAG, "onConnectionSuspended: 连接中断");
        }

        @Override
        public void onConnectionFailed() {
            super.onConnectionFailed();
            //服务端拒绝连接
            Log.d(TAG, "onConnectionFailed: 连接失败");
        }
    }

    private void releaseMediaBrowser(){
        disConnect();
        if (mControllerCallback != null) { mControllerCallback = null; }
        if (mSubscriptionCallback != null) { mSubscriptionCallback = null; }
        if (mMediaBrowser != null) {
//            mMediaBrowser.unsubscribe(mMediaBrowser.getRoot());
            mMediaBrowser = null; }
    }

    private void initRotateAnimation(){
        //加载效果 旋转动画 初始化
        mLoadingAnimation = new SoftReference<>(
                AnimationUtils.loadAnimation(this, R.anim.circle_rotate)).get();
        LinearInterpolator interpolator = new SoftReference<>(new LinearInterpolator()).get();
        mLoadingAnimation.setInterpolator(interpolator);
    }

    protected ObjectAnimator initAnimation(View view){
        //唱片转动动画 初始化
        mRecordAnimator = ObjectAnimator.ofFloat(
                view, "rotation", 0.0f, 360.0f);
        mRecordAnimator.setDuration(30000);//设定转一圈的时间
        mRecordAnimator.setRepeatCount(Animation.INFINITE);//设定无限循环
        mRecordAnimator.setRepeatMode(ObjectAnimator.RESTART);// 循环模式
        mRecordAnimator.setInterpolator(new LinearInterpolator());//匀速

        return mRecordAnimator;
    }

    protected void playbackStateChanged(PlaybackStateCompat playbackState,
                                        @NonNull View loadingView){
        if (mRecordAnimator == null || playbackState == null) {
            Toast.makeText(this,"未初始化唱片旋转动画",Toast.LENGTH_SHORT).show();
            return;
        }
        int state = playbackState.getState();
        Bundle bundle = playbackState.getExtras();
        if (state == PlaybackStateCompat.STATE_PLAYING) {
            Log.w(TAG, "playbackStateChanged: ");
            loadingView.clearAnimation();
            loadingView.setVisibility(View.GONE);
            if (bundle == null || !mRecordAnimator.isStarted()) mRecordAnimator.start();//动画开始
            else if (bundle.getBoolean("Continue_Playing_Tips")) mRecordAnimator.resume();
        }else if (state == PlaybackStateCompat.STATE_BUFFERING){
            loadingView.startAnimation(mLoadingAnimation);
            loadingView.setVisibility(View.VISIBLE);
        }else if (state == PlaybackStateCompat.STATE_PAUSED){
            mRecordAnimator.pause();
        }
    }

    /**
     * 设置最大刷新率
     * 1.通过Activity 的Window对象获取到{@link Display.Mode[]} 所有的刷新率模式数组
     * 2.通过遍历判断出刷新率最大那一组，并获取此组引用{@link Display.Mode}
     * 3.国际惯例首先判空，再获取{@link WindowManager.LayoutParams}引用，其成员变量preferredDisplayModeId是{@link Display.Mode}的ModeID
     * 4.window.setAttributes(layoutParams);最后设置下，收工*/
    protected void HighHzAdaptation(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 获取系统window支持的模式,得到刷新率组合
            Window window = getWindow();
            //获取屏幕宽高
            DisplayMetrics dm = new DisplayMetrics();
            window.getWindowManager().getDefaultDisplay().getMetrics(dm);
            mPhoneWidth = dm.widthPixels;
            mPhoneHeight = dm.heightPixels;
            isPad = (float) Math.max(mPhoneWidth,mPhoneHeight) /
                    Math.min(mPhoneWidth,mPhoneHeight) < 1.5;
            //获取屏幕刷新率组合
            Display.Mode[] modes = window.getWindowManager().getDefaultDisplay().getSupportedModes();
            //对获取的模式，基于刷新率的大小进行排序，从小到大排序
            float RefreshRateMax = 0f;
            Display.Mode RefreshRateMaxMode = null;
            for (Display.Mode mode : modes){
                float RefreshRateTemp = mode.getRefreshRate();
                if (RefreshRateTemp > RefreshRateMax) {
                    RefreshRateMax = RefreshRateTemp;
                    RefreshRateMaxMode = mode;
                }
            }
            if (RefreshRateMaxMode != null) {
                WindowManager.LayoutParams layoutParams = window.getAttributes();
                layoutParams.preferredDisplayModeId = RefreshRateMaxMode.getModeId();
                window.setAttributes(layoutParams);
                //Log.d(TAG, "设置最大刷新率为 "+RefreshRateMaxMode.getRefreshRate()+"Hz");
                mRefreshRateMax = (int)RefreshRateMax;
            }
        }
    }

    protected int dpToPx(int dp){
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp,
                getResources().getDisplayMetrics());
    }

    protected Animation getLoadingAnimation() { return mLoadingAnimation; }
}
