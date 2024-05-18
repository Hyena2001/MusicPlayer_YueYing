package com.example.yueying2.service.manager;

import android.app.Application;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.audiofx.LoudnessEnhancer;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.view.WindowManager;

import java.util.Optional;
import java.util.function.Function;

/**
 * @author : Bilibili喜闻人籁
 * @since : 2021/11/3
 * 作用: 系统声音服务 所需 代码和方法  管理帮助类
 *      1.音频焦点管理、 顺便管理下Wifi锁
 *      2.调节系统音量、
 *      3.播放声音增强、
 */
public class MyAudioManager {

    private static final String TAG = "MyAudioManager";
    //音量Key
    public static final String DYQL_CUSTOM_ACTION_MAX_VOLUME = "max_volume_dyql";
    public static final String DYQL_CUSTOM_ACTION_CURRENT_VOLUME = "current_volume_dyql";

    private Application mApplication;

    private AudioManager mAudioManager;
    private WindowManager mWindowManager;
    private WifiManager.WifiLock mWifiLock;
    //音频焦点管理
    private AudioManager.OnAudioFocusChangeListener mFocusChangeListener;
    private AudioAttributes mPlaybackAttributes;
    private AudioFocusRequest mFocusRequest;
    //人声增强
    private LoudnessEnhancer mLoudnessEnhancer;
    private long mCurrentVoiceMb;

    public MyAudioManager(Application application,
                          AudioManager.OnAudioFocusChangeListener focusChangeListener,
                          int audioSessionId){
        mApplication = application;

        //初始化管理者
        mAudioManager = (AudioManager) application.getSystemService(Context.AUDIO_SERVICE);
        mWindowManager = (WindowManager) application.getSystemService(Context.WINDOW_SERVICE);
        mWifiLock = ((WifiManager) application.getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL, "dyqlLock");

        //音频焦点管理初始化
        mFocusChangeListener = focusChangeListener;
        mPlaybackAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build();
        //获取长时间音频播放焦点
        mFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(mPlaybackAttributes)
                // 可让您的应用异步处理焦点请求。设置此标记后，
                // 在焦点锁定时发出的请求会返回 AUDIOFOCUS_REQUEST_DELAYED。
                // 当锁定音频焦点的情况不再存在时（例如当通话结束时），
                // 系统会批准待处理的焦点请求，并调用 onAudioFocusChange() 来通知您的应用。
                .setAcceptsDelayedFocusGain(true)
                //播放通知铃声时自动降低音量，true则回调音频焦点更改回调，可在回调里暂停音乐
                .setWillPauseWhenDucked(false)
                .setOnAudioFocusChangeListener(mFocusChangeListener)
                .build();

        //人声增强器初始化
        mLoudnessEnhancer = new LoudnessEnhancer(audioSessionId);
        mLoudnessEnhancer.setTargetGain(1000);//调节此值 可按值增强声音 | 人声增强 mLoudnessEnhancer
        mLoudnessEnhancer.setEnabled(true);
    }

    public void onDestroy(){
        releaseAudioFocus();
        if (mApplication != null) mApplication = null;

        if (mAudioManager != null) mAudioManager = null;
        if (mWindowManager != null) mWindowManager = null;
        if (mWifiLock != null) mWifiLock = null;

        if (mLoudnessEnhancer != null) {
            mLoudnessEnhancer.release();
            mLoudnessEnhancer = null;
        }

        if (mFocusChangeListener != null) mFocusChangeListener = null;
        if (mPlaybackAttributes != null) mPlaybackAttributes = null;
        if (mFocusRequest != null) mFocusRequest = null;
    }

    public AudioAttributes getPlaybackAttributes() {
        return mPlaybackAttributes;
    }

    public void lowerTheVolume(){
        int volume = getVolume();
        setVolume( volume > 4 ? volume - 2 : 2);
    }

    public int getMaxVolume(){
        if (mAudioManager != null) { return mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC); }
        return 0;
    }
    public boolean setVolume(int volume){
        boolean canSetVolume = mAudioManager != null && !mAudioManager.isVolumeFixed();

        if (canSetVolume) {
            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC,volume, AudioManager.FLAG_PLAY_SOUND);
            //Log.d(TAG, "setVolume: "+percent);

        }else Log.e(TAG, "setVolume: 音量设置无效 参数 "+volume);

        return canSetVolume;
    }
    public int getVolume(){
        if (mAudioManager != null) {
            Object object = getVolume(mAudioManager);
            if (object instanceof Integer) { return Integer.parseInt(object.toString()); }
        }
        return 0;
    }
    private Object getVolume(AudioManager manager){
        return Optional.of(manager).map((Function<AudioManager, Object>)
                manager1 -> manager1.getStreamVolume(AudioManager.STREAM_MUSIC)).orElse("0");
    }

    //获取与设置用户信息-人声增强幅度
    public long getCurrentVoiceMb() { return mCurrentVoiceMb; }
    public void setCurrentVoiceMb(long currentVoiceMb, boolean isSave) {
        if (mCurrentVoiceMb == currentVoiceMb) return;

        if(mLoudnessEnhancer != null) {
            mLoudnessEnhancer.setEnabled(currentVoiceMb > 0);
            mLoudnessEnhancer.setTargetGain((int) currentVoiceMb);
        }

        if (!isSave) return;

        this.mCurrentVoiceMb = currentVoiceMb;
        if (mCurrentVoiceMb < 0) mCurrentVoiceMb = 0;
        if (mCurrentVoiceMb > 2600) mCurrentVoiceMb = 2600;

        /*settings = getSharedPreferences("UserLastMusicPlay",0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putLong("UserVoiceMb",mCurrentVoiceMb);
        editor.apply();*/
    }

    public int registerAudioFocus(){
        //启动wifi锁,在暂停或者停止时释放WiFi锁
        mWifiLock.acquire();
        //获得播放焦点
        return mAudioManager.requestAudioFocus(mFocusRequest);
    }

    public void releaseAudioFocus(){
        //停止播放音乐后释放焦点
        mAudioManager.abandonAudioFocusRequest(mFocusRequest);
        //释放wifi锁
        if (mWifiLock.isHeld()) mWifiLock.release();
    }
}
