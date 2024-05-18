package com.example.yueying2.service.manager;

import static android.support.v4.media.session.PlaybackStateCompat.STATE_PLAYING;

import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.widget.Toast;

import com.example.yueying2.R;

import java.io.IOException;
import java.util.Locale;

/**
 * @author : Bilibili喜闻人籁
 * @since : 2021/11/6
 * 作用:
 */
public class MediaPlayerManager {
    private static final String TAG = "MediaPlayManager";
    //
    private Application mApplication;
    private MediaSessionCompat mMediaSession;
    private com.example.yueying2.service.manager.MyAudioManager mMyAudioManager;
    private MediaPlayer mMediaPlayer;
    private boolean isFirstPlay = true;
    private int mCurrentPosition = -1, mCurrentAudioLevel = 2;
    //播放模式Flag
    private int DYQL_PLAYBACK_MODE = 0;
    protected static final int DYQL_PLAYBACK_MODE_ORDER = 1;
    protected static final int DYQL_PLAYBACK_MODE_REPEAT = 3;
    protected static final int DYQL_PLAYBACK_MODE_RANDOM = 2;
    public static final String DYQL_CUSTOM_ACTION_PLAYBACK_MODE_CHANGE = "playback_mode_change_dyql";
    //MusicService 回调更新通知栏
    private NotificationListener mNotificationListener;
    public interface NotificationListener{
        void onUpdateNotification();
    }

    public MediaPlayerManager(Application application,
                              MediaSessionCompat mediaSession,
                              NotificationListener notificationListener,
                              AudioManager.OnAudioFocusChangeListener focusChangeListener){
        mApplication = application;
        mMediaSession = mediaSession;
        mNotificationListener = notificationListener;
        //初始化MediaPlayer
        mMediaPlayer = new MediaPlayer();
        //唤醒锁定模式，关闭屏幕时，CPU不休眠
        mMediaPlayer.setWakeMode(application, PowerManager.PARTIAL_WAKE_LOCK);
        //初始化MyAudioManager
        mMyAudioManager = new com.example.yueying2.service.manager.MyAudioManager(application,focusChangeListener,
                                                        mMediaPlayer.getAudioSessionId());

        mMediaPlayer.setAudioAttributes(mMyAudioManager.getPlaybackAttributes());
        mMediaPlayer.setOnErrorListener(new onErrorListener());
        mMediaPlayer.setOnPreparedListener(new onPreparedListener());
        mMediaPlayer.setOnCompletionListener(new onCompleteListener());
    }

    public void onDestroy(){
        if (mApplication != null) { mApplication = null; }
        if (mMediaSession != null) { mMediaSession = null; }
        if (mNotificationListener != null) { mNotificationListener = null; }
        if (mMyAudioManager != null) {
            mMyAudioManager.onDestroy();
            mMyAudioManager = null;
        }
        releaseMediaPlayer();
    }

    public void releaseMediaPlayer(){
        if (mMediaPlayer != null) {
            StopMediaPlayer();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }
    //MediaPlayer 相关方法
    public void StopMediaPlayer(){
        //Log.d(TAG, "StopMediaPlayer: ");
        if (isFirstPlay() && mCurrentPosition <= 0) return;

        if (mMediaPlayer != null) {
            if (isPlaying())  {
                mMediaPlayer.pause();
                mMediaPlayer.seekTo(0);
                mMediaPlayer.stop();
            }
            //释放wifi锁 , 释放音频焦点
            if (mMyAudioManager != null) { mMyAudioManager.releaseAudioFocus(); }
            //重置MediaPlayer
            mMediaPlayer.reset();
            //适用于上次播放有进度，但是第一次播放了这首歌曲，所以播放进度保留
            if (!isFirstPlay()) resetCurrentPosition();

        }else System.out.println("MediaPlayer is null!");
    }
    public void setDataRes(String path){
        StopMediaPlayer();
        try {
            mMediaPlayer.setDataSource(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void PlayFromUri(){
        if (mMediaPlayer == null || isPlaying() ) return;
        if (!mMediaSession.isActive()) { mMediaSession.setActive(true); }
        if (mMyAudioManager != null) { setVolume(mCurrentAudioLevel); }

        if (mCurrentPosition <= 0 || isFirstPlay) {
            mMediaSession.setPlaybackState(
                    newPlaybackState(PlaybackStateCompat.STATE_BUFFERING,null));
            mMediaPlayer.prepareAsync();
        }else { //暂停后继续播放
            mMediaPlayer.seekTo(mCurrentPosition);

            Bundle bundle = new Bundle();
            bundle.putBoolean("Continue_Playing_Tips",true);
            checkFocusPlay(bundle);

            Log.d(TAG, "PlayMediaPlayer: 暂停后播放");
        }

    }
    public void OnPause(boolean notReleaseAudio){
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            //记录播放队列位置
            mCurrentPosition = mMediaPlayer.getCurrentPosition();
            mMediaPlayer.pause();
        }
        //停止播放音乐释放焦点
        if (!notReleaseAudio) { mMyAudioManager.releaseAudioFocus(); }

        mMediaSession.setPlaybackState(newPlaybackState(PlaybackStateCompat.STATE_PAUSED,null));
        mNotificationListener.onUpdateNotification();
    }

    public void seekTo(long pos) {
        if (mMediaPlayer == null && mMediaSession == null) return;

        MediaControllerCompat mMediaController = mMediaSession.getController();
        if (mMediaController.getPlaybackState().getState()
                == STATE_PLAYING) mMediaPlayer.pause();
        mCurrentPosition = pos == 0 ? 1 : (int) pos;
        if (isFirstPlay()) {
            String path = mMediaController.getMetadata()
                    .getString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI);
            setDataRes(path);
        }
        PlayFromUri();
    }
    //***************************音量更改与获取****************************/
    public boolean checkAudioChange(){ return mCurrentAudioLevel != mMyAudioManager.getVolume();}
    public void lowerTheVolume(){ mMyAudioManager.lowerTheVolume(); }
    public void setVolume(int volume) {
        if (mMyAudioManager.setVolume(volume)){
            //设置播放流音量，防止静音时播放后，再调整音量无效的问题
            float percent = Float.parseFloat(String.format(Locale.CHINA, "%.2f",
                    (float)volume / (float)mMyAudioManager.getMaxVolume()));
            Log.d(TAG, "setVolume: "+percent);
            if (mMediaPlayer != null) mMediaPlayer.setVolume(percent,percent);

            mCurrentAudioLevel = volume;
        }
    }
    public int getVolume(){
        //Log.d(TAG, "getVolume: "+mMyAudioManager.getVolume());
        return mMyAudioManager.getVolume();}
    public int getMaxVolume(){
        //Log.d(TAG, "getMaxVolume: "+mMyAudioManager.getMaxVolume());
        return mMyAudioManager.getMaxVolume();}
    //***************************获取与更改播放进度****************************/
    public void resetCurrentPosition() { mCurrentPosition = 0; }
    public int getCurrentPosition() {
        return isPlaying() ? mMediaPlayer.getCurrentPosition() : mCurrentPosition;
    }
    public void initCurrentPosition(int position){ mCurrentPosition = position;}
    //***************************一些播放状态****************************/
    public boolean isPlaying() { return mMediaPlayer != null && mMediaPlayer.isPlaying(); }
    public boolean isFirstPlay() { return isFirstPlay; }

    public void setLooping(){
        mMediaPlayer.setLooping(getPlaybackMode() == getDyqlPlaybackModeRepeat());
    }
    //***************************MediaPlayer回调区****************************/
    private class onCompleteListener implements MediaPlayer.OnCompletionListener {
        @Override
        public void onCompletion(MediaPlayer mp) {
            Log.d(TAG, "onCompletion: ");
            //播放下一曲
            mMediaSession.getController().getTransportControls().skipToNext();
        }
    }

    private class onErrorListener implements  MediaPlayer.OnErrorListener{
        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            //当播放错误时，MediaPlayer执行此回调并停止播放，故isPlayerPrepared 状态应为true，已准备好播放
            /*if (!isPlayerPrepared) isPlayerPrepared = true;
            isError_Flag = true;//必须要设置不同Path才能继续播放*/
            if (what != -38 && extra != 0) mApplication.sendBroadcast(new Intent("error"));
            Log.d(TAG, "onErrorListener: what:"+what+" , extra = "+extra);
            switch (what) {
                case 1:
                    if (extra == 1)
                        Log.e(TAG,"播放错误，歌曲地址为空,请播放其他歌曲");
                    else if (extra == 2)
                        Log.e(TAG,"该音乐文件已损坏,请播放其他歌曲");
                    else if (extra == 28)
                        Log.e(TAG,"该音乐媒体对象为空，请重新打开App");
                    else if (extra == -2147483648)
                        Log.e(TAG,"音乐文件解码失败,请删除该文件,尝试播放网络版本");
                    else Log.e(TAG,"播放错误,请播放其他歌曲");
                    break;
                case 2:
                    if (extra == 2) Log.d(TAG, "onError: 音乐文件解码失败,请播放其他歌曲");
                    else Log.d(TAG, "onError: 播放错误,请播放其他歌曲");
                    Log.e(TAG, "Error Prepare 只能使用log或者通知");
                    break;
            }
            //onError返回值返回false会触发onCompletionListener，y
            //所以返回false，一般意味着会退出当前歌曲播放。
            //如果不想退出当前歌曲播放则应该返回true
            return true;
        }
    }

    private class onPreparedListener implements MediaPlayer.OnPreparedListener{

        @Override
        public void onPrepared(MediaPlayer mp) {
            Log.e(TAG, "onPrepared: "+mCurrentPosition);
            if (mCurrentPosition > 0) mMediaPlayer.seekTo(mCurrentPosition);
            else if (mCurrentPosition == -1) resetCurrentPosition();
            //获得音频焦点
            checkFocusPlay(null);
            if (isFirstPlay) { isFirstPlay = false; }
        }
    }
    private void checkFocusPlay(Bundle bundle){
        int audioFocusState = mMyAudioManager.registerAudioFocus();

        if(audioFocusState == AudioManager.AUDIOFOCUS_REQUEST_GRANTED){
            mMediaPlayer.start();
            //更新状态, 使该通知处于栈顶, 赋予播放进度初始值, 从而在通知中展示播放进度
            mMediaSession.setPlaybackState(newPlaybackState(STATE_PLAYING,bundle));
            mNotificationListener.onUpdateNotification();
        }else if (audioFocusState == AudioManager.AUDIOFOCUS_REQUEST_FAILED){//请求焦点失败
            Toast.makeText(mApplication,"请求播放失败",Toast.LENGTH_SHORT).show();
        }else Log.e(TAG, "checkFocusPlay: 音频焦点延迟获得！");
    }
    //***************************获取新播放状态PlaybackStateCompat****************************/
    public PlaybackStateCompat newPlaybackState(@PlaybackStateCompat.State int newState, Bundle bundle){
        return new PlaybackStateCompat.Builder()
                .setExtras(bundle)
                //设置需使用的Action
                .setActions(
                        PlaybackStateCompat.ACTION_PLAY |
                                PlaybackStateCompat.ACTION_PAUSE |
                                PlaybackStateCompat.ACTION_SEEK_TO |
                                PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                                //关闭Notification
                                PlaybackStateCompat.ACTION_STOP |
                                //歌词action，翻译为字幕
                                PlaybackStateCompat.ACTION_SET_CAPTIONING_ENABLED |
                                //歌曲收藏action，翻译为星级评级
                                PlaybackStateCompat.ACTION_SET_RATING |
                                //播放模式切换action，翻译为设置重复播放
                                PlaybackStateCompat.ACTION_SET_SHUFFLE_MODE)
                .addCustomAction(DYQL_CUSTOM_ACTION_PLAYBACK_MODE_CHANGE,
                        String.valueOf(getPlaybackMode()), R.drawable.iv_playback_mode_order)
                .setState(newState,mCurrentPosition,1.0f).build();
    }
    //***************************播放模式更改与获取****************************/
    public void setPlayBackMode(int mode){
        DYQL_PLAYBACK_MODE = (mode < 1 || mode > 3) ? 1 : mode;
        Log.e(TAG, "setPlayBackMode: "+mode);
    }

    public int getPlaybackMode(){
        Log.e(TAG, "getPlaybackMode: "+DYQL_PLAYBACK_MODE);return DYQL_PLAYBACK_MODE; }

    public static int getDyqlPlaybackModeOrder() { return DYQL_PLAYBACK_MODE_ORDER; }

    public static int getDyqlPlaybackModeRandom() { return DYQL_PLAYBACK_MODE_RANDOM; }

    public static int getDyqlPlaybackModeRepeat() { return DYQL_PLAYBACK_MODE_REPEAT; }

    public int playbackModeChange(){
        if (DYQL_PLAYBACK_MODE != DYQL_PLAYBACK_MODE_REPEAT) { DYQL_PLAYBACK_MODE++; }
        else DYQL_PLAYBACK_MODE = DYQL_PLAYBACK_MODE_ORDER;

        SharedPreferences settings = mApplication.getSharedPreferences("UserLastMusicPlay", 0);
        SharedPreferences.Editor editor = settings.edit();

        editor.putInt("MusicPlaybackMode",DYQL_PLAYBACK_MODE);
        editor.apply();

        return DYQL_PLAYBACK_MODE;
    }

}
