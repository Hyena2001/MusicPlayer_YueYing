package com.example.yueying2.viewmodel;

import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import androidx.databinding.Bindable;

import com.example.yueying2.BR;
import com.example.yueying2.R;
import com.example.yueying2.service.manager.MediaPlayerManager;
import com.example.yueying2.service.manager.MyAudioManager;

import java.lang.ref.SoftReference;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimerTask;

/**
 * @author : Bilibili喜闻人籁
 * @since : 2021/10/31
 * 作用: 
 */
public class SongLrcViewModel extends MusicViewModel{
    private static final String TAG = "SongLrcViewModel";

    private Application mApplication;
    private String musicTitle, musicArtist, timeStart, timeEnd;
    private SimpleDateFormat mDateFormat;
    private int progress, secondProgress, max, albumViewSize;
    private int progressV, maxV;//音量进度条
    private int playbackResId, playbackModeResId;
    private boolean isSeekBarChanging, isShowLyric;

    private SoftReference<LayerDrawable> mBackgroundDrawable, albumDrawable;
    private SoftReference<TransitionDrawable> mGradualChangeBg;

    public SongLrcViewModel(Application application) {
        super(application);
        mApplication = application;
        mDateFormat = new SimpleDateFormat(application.getResources()
                .getString(R.string.label_minute_second), Locale.CHINA);

       this.playbackResId = R.drawable.ic_lrc_play;
       this.playbackModeResId = R.drawable.iv_playback_mode_order;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        Log.e(TAG, "onCleared: ");
        if (mApplication != null) mApplication = null;
        if (mDateFormat != null) mDateFormat = null;
        if (musicTitle != null) musicTitle = null;
        if (musicArtist != null) musicArtist = null;
        if (timeStart != null) timeStart = null;
        if (timeEnd != null) timeEnd = null;
        if (mBackgroundDrawable != null) {
            mBackgroundDrawable.clear();
            mBackgroundDrawable = null;
        }
        if (albumDrawable != null) {
            albumDrawable.clear();
            albumDrawable = null;
        }
        if (mGradualChangeBg != null) {
            mGradualChangeBg.clear();
            mGradualChangeBg = null;
        }
        System.gc();
    }

    @Bindable
    public boolean isShowLyric() {
        return isShowLyric;
    }
    public void setShowLyric(boolean showLyric) {
        this.isShowLyric = showLyric;
        notifyPropertyChanged(BR.showLyric);
    }

    @Bindable
    public int getPlaybackResId() {
        return playbackResId;
    }
    public void setPlaybackState(@PlaybackStateCompat.State int playState) {
        boolean state = playState == PlaybackStateCompat.STATE_PLAYING ||
                playState == PlaybackStateCompat.STATE_PAUSED ||
                playState == PlaybackStateCompat.STATE_STOPPED;
        if (!state) { return; }

        this.playbackResId = playState == PlaybackStateCompat.STATE_PLAYING ?
                R.drawable.ic_lrc_pause : R.drawable.ic_lrc_play;

        notifyPropertyChanged(BR.playbackResId);
    }
    public void playbackButton(){
        if (mMediaControllerCompat == null) return;

        // 因为这是一个播放/暂停按钮，所以需要测试当前状态，并相应地选择动作
        int pbState = mMediaControllerCompat.getPlaybackState().getState();
        Log.d(TAG, "initView: 点击了播放暂停按钮, 播放状态代码: "+pbState);
        if (pbState == PlaybackStateCompat.STATE_PLAYING) {
            mMediaControllerCompat.getTransportControls().pause();
            this.playbackResId = R.drawable.ic_lrc_play;
            notifyPropertyChanged(BR.playbackResId);
        } else if (pbState == PlaybackStateCompat.STATE_PAUSED) {
            mMediaControllerCompat.getTransportControls().play();
            this.playbackResId = R.drawable.ic_lrc_pause;
            notifyPropertyChanged(BR.playbackResId);
        }else {//PlaybackStateCompat.STATE_STOPPED or other
            //Toast.makeText(this, "进入APP首次播放", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "playbackButton: 首次播放");
            String path = mMediaControllerCompat.getMetadata()
                                        .getString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI);
            if (path == null || TextUtils.isEmpty(path)) return;
            mMediaControllerCompat.getTransportControls().playFromUri(Uri.parse(path),null);
            this.playbackResId = R.drawable.ic_lrc_pause;
            notifyPropertyChanged(BR.playbackResId);
        }
    }

    @Bindable
    public int getPlaybackModeResId() {
        return playbackModeResId == 0 ? R.drawable.iv_playback_mode_order : playbackModeResId;
    }
    public void setPlaybackModeRes(int modeNow) {
        if (modeNow < 1 || modeNow > 3) { Log.e(TAG, "播放模式错误代码 "+modeNow); return; }

        if (modeNow == 1) {
            this.playbackModeResId = R.drawable.iv_playback_mode_order;
        }else if (modeNow == 2){
            this.playbackModeResId = R.drawable.iv_playback_mode_random;
        }else this.playbackModeResId = R.drawable.iv_playback_mode_repeat;

        notifyPropertyChanged(BR.playbackModeResId);
    }
    public void playbackModeButton(){
        //发送指令给Service，使其切换播放模式
        mMediaControllerCompat.getTransportControls()
                .setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_NONE);
    }

    public void SkipToNextPlayback(){
        mMediaControllerCompat.getTransportControls().skipToNext();
    }
    public void SkipToPreviousPlayback(){
        mMediaControllerCompat.getTransportControls().skipToPrevious();
    }

    public void SyncMusicInformation(){

        MediaMetadataCompat lastMetadata = mMediaControllerCompat.getMetadata();
        //更新专辑唱片
        Bitmap bitmap = lastMetadata.getBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART);
        setAlbumDrawable(getRecordBig(bitmap,mApplication,albumViewSize));
        //歌名-歌手
        String title = lastMetadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE),
                artist = lastMetadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST);
        //Log.d(TAG, "onChildrenLoaded: "+lastMetadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE));
        setMusicName(title,artist);
        //时长 注seekbar必须要先设置最大值！！！
        int position = mApplication.getSharedPreferences("UserLastMusicPlay",0)
                .getInt("MusicPosition",0);
        long duration = lastMetadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION);
        //Log.d(TAG, "onChildrenLoaded: 当前进度 "+position+" 总时长 "+duration);
        setTimeEnd(duration);
        setTimeStart(position);
        //音量
        int volume = (int) lastMetadata.getLong(MyAudioManager.DYQL_CUSTOM_ACTION_CURRENT_VOLUME);
        int volumeMax = (int) lastMetadata.getLong(MyAudioManager.DYQL_CUSTOM_ACTION_MAX_VOLUME);
        //Log.e(TAG, "SyncMusicInformation: "+volume+", "+volumeMax+", 当前播放状态 "+mMediaControllerCompat.getPlaybackState().getState());
        if (volumeMax > 0) {
            setMaxV(volumeMax);
            setProgressV(volume);
        }
        //更新播放状态
        setPlaybackState(mMediaControllerCompat.getPlaybackState().getState());
        //更新播放模式图标
        setPlaybackModeRes((int) lastMetadata.getLong(
                MediaPlayerManager.DYQL_CUSTOM_ACTION_PLAYBACK_MODE_CHANGE));
        //显示处理成唱片样式的歌曲封面图片
        setBackgroundDrawable(getBlurDrawable(bitmap,mApplication));

    }

    @Bindable
    public LayerDrawable getAlbumDrawable() {
        if (albumDrawable != null)
        Log.e(TAG, "获得大唱片: 弱引用对象为空"+(albumDrawable.get() == null));
        return albumDrawable == null || albumDrawable.get() == null ? null : albumDrawable.get();
    }
    public void setAlbumDrawable(LayerDrawable albumDrawable) {
        this.albumDrawable = new SoftReference<>(albumDrawable);
        notifyPropertyChanged(BR.albumDrawable);
    }

    @Bindable
    public TransitionDrawable getBackgroundDrawable() {
        return mGradualChangeBg == null || mGradualChangeBg.get() == null ?
                null : mGradualChangeBg.get();
    }
    public void setBackgroundDrawable(LayerDrawable backgroundDrawable) {
        //图片渐变
        mGradualChangeBg = new SoftReference<>(new TransitionDrawable(
                new Drawable[]{mBackgroundDrawable != null && mBackgroundDrawable.get() != null ?
                        mBackgroundDrawable.get() : backgroundDrawable, backgroundDrawable}));
        notifyPropertyChanged(BR.backgroundDrawable);
        mGradualChangeBg.get().startTransition(360);

        this.mBackgroundDrawable = new SoftReference<>(backgroundDrawable);
        Log.d(TAG, "setBackgroundDrawable: ");
    }

    @Bindable
    public String getMusicTitle() {
        return musicTitle;
    }
    @Bindable
    public String getMusicArtist() {
        return musicArtist;
    }
    public void setMusicName(String title, String artist) {
        Log.d(TAG, "setMusicName: "+title);
        this.musicTitle = title;
        this.musicArtist = artist;
        notifyPropertyChanged(BR.musicTitle);
        notifyPropertyChanged(BR.musicArtist);
    }
    public String getMusicName(){ return this.musicTitle + " - " + this.musicArtist; }

    @Bindable
    public int getProgressV() {
        return progressV;
    }
    public void setProgressV(int progressV) {
        if (progressV == this.progressV) return;
        this.progressV = progressV;
        notifyPropertyChanged(BR.progressV);
    }
    @Bindable
    public int getMaxV() {
        return maxV;
    }
    public void setMaxV(int maxV) {
        if (maxV <= this.maxV) return;
        this.maxV = maxV;
        notifyPropertyChanged(BR.maxV);
    }
    @Bindable
    public int getProgress() {
        return progress;
    }
    @Bindable
    public int getSecondProgress() {
        return secondProgress;
    }
    @Bindable
    public int getMax() {
        return max;
    }
    @Bindable
    public String getTimeStart() {
        return timeStart;
    }
    @Bindable
    public String getTimeEnd() {
        return timeEnd;
    }
    public void setTimeStart(long timeStart) {
        progress = (int) timeStart;
        this.timeStart = mDateFormat.format(timeStart);
        notifyPropertyChanged(BR.timeStart);
        notifyPropertyChanged(BR.progress);
    }
    public void setTimeEnd(long timeEnd) {
        max = (int) timeEnd;
        this.timeEnd = mDateFormat.format(timeEnd);
        notifyPropertyChanged(BR.timeEnd);
        notifyPropertyChanged(BR.max);
    }

    public volumeSeekBar getVolumeListener(){
        return new volumeSeekBar();
    }
    public MySeekBar getSeekBarListener(){
        return new MySeekBar();
    }
    public seekBarTimerTask getTimerTask(){
        return new seekBarTimerTask();
    }

    //音量进度条处理
    private class volumeSeekBar implements SeekBar.OnSeekBarChangeListener{
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            //Log.d(TAG, "onProgressChanged: 音量 "+progress);
        }
        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }
        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            if (mMediaControllerCompat == null) { return; }

            Bundle bundle = new Bundle();
            bundle.putInt(MyAudioManager.DYQL_CUSTOM_ACTION_CURRENT_VOLUME,seekBar.getProgress());
            mMediaControllerCompat.getTransportControls().sendCustomAction(
                    MyAudioManager.DYQL_CUSTOM_ACTION_CURRENT_VOLUME,bundle);
        }
    }//class volumeSeekBar end

    //音乐进度条处理
    private class MySeekBar implements SeekBar.OnSeekBarChangeListener {

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            //音乐正常播放之后才能向MediaPlayer获取当前播放进度, 此处用于定时器Timer实时刷新歌曲进度。
            //Log.d(TAG, "onProgressChanged: ");
            if (isSeekBarChanging && seekBar.getMax() > 0 || progress >= 0)
                //在播放中滑动时，TextView开始时间显示滑动的时间
                setTimeStart(progress);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            isSeekBarChanging = true;//开始触摸进度条
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            isSeekBarChanging = false;//结束触摸进度条
            if (mMediaControllerCompat == null) return;
            Log.w(TAG, "onStopTrackingTouch: ");
            mMediaControllerCompat.getTransportControls().seekTo(seekBar.getProgress());
        }
    }//class MySeekBar end

    private class seekBarTimerTask extends TimerTask{

        @Override
        public void run() {
            if (mMediaControllerCompat == null) return;
            PlaybackStateCompat playbackState = mMediaControllerCompat.getPlaybackState();

            if (!isSeekBarChanging && playbackState.getState() == PlaybackStateCompat.STATE_PLAYING) {
                //当 没有手动改变进度条并且音乐正在播放时 执行进度更新显示
                setTimeStart(playbackState.getPosition());
            }
        }
    }

    public void setAlbumViewSize(int albumViewSize) {
        this.albumViewSize = albumViewSize;
    }

}
