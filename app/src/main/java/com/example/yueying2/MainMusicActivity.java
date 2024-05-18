package com.example.yueying2;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.View;
import android.view.WindowInsets;

import com.example.yueying2.databinding.ActivityMainMusicBinding;
import com.example.yueying2.util.PermissionUtil;
import com.example.yueying2.view.MusicActivity;
import com.example.yueying2.view.SongLrcActivity;
import com.example.yueying2.viewmodel.MusicViewModel;

import java.util.List;
import java.util.Timer;

public class MainMusicActivity extends BaseActivity {

    private static final String TAG = "MainMusicActivity";
    private ActivityMainMusicBinding activityMainMusicBinding;
    private MusicViewModel musicViewModel;
    private Timer mTimer;

    @Override
    protected MediaControllerCompat.Callback getControllerCallback() {
        return new MyMediaControllerCallback();
    }

    @Override
    protected MediaBrowserCompat.SubscriptionCallback getSubscriptionCallback() {
        return new MyMediaBrowserSubscriptionCallback();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //权限获取声明
        if(PermissionUtil.IsPermissionNotObtained(this)){
            PermissionUtil.getStorage(this);
        }

        super.onCreate(savedInstanceState);

        //DataBinding来绑定视图
        activityMainMusicBinding = DataBindingUtil.setContentView(this,R.layout.activity_main_music);
        musicViewModel = new MusicViewModel(getApplication());
        activityMainMusicBinding.setUserInfo(musicViewModel);

        initView();
    }

    @Override
    protected void onStart() {
        super.onStart();
        UpdateProgressBar();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        StopProgressBar();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (musicViewModel != null) { musicViewModel = null; }
        if (activityMainMusicBinding != null) {
            activityMainMusicBinding.unbind();
            activityMainMusicBinding = null;
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
    public void onRequestPermissionsResult(int requestCode, String[] permissions, @NonNull int[] grantResult){
        super.onRequestPermissionsResult(requestCode, permissions, grantResult);

        if (grantResult[0] == PermissionUtil.REQUEST_PERMISSION_CODE){
            //权限声明
            if(PermissionUtil.IsPermissionNotObtained(this)){
                PermissionUtil.getStorage(this);
            }
            else {
                Log.d(TAG, "onRequestPermissionsResult，已获取读写权限");
            }
        }
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        super.onPointerCaptureChanged(hasCapture);
    }

    private class MyMediaBrowserSubscriptionCallback extends MediaBrowserCompat.SubscriptionCallback{
        @Override
        public void onChildrenLoaded(@NonNull String parentId,
                                     @NonNull List<MediaBrowserCompat.MediaItem> children) {
            super.onChildrenLoaded(parentId, children);

            MediaControllerCompat mediaController = MediaControllerCompat.getMediaController(MainMusicActivity.this);
            musicViewModel.setMediaControllerCompat(mediaController);
            musicViewModel.SyncMusicInformation();

            //同步播放动画
            PlaybackStateCompat playBackState = mediaController.getPlaybackState();
            musicViewModel.setPlaybackState(playBackState.getState());
            playbackStateChanged(playBackState, activityMainMusicBinding.mainActivityIvPlayLoading);

            //添加列表
            for(final MediaBrowserCompat.MediaItem mediaItem : children){
                mediaController.addQueueItem(mediaItem.getDescription());
            }
            mediaController.getTransportControls().prepare();

        }

        @Override
        public void onError(@NonNull String parentId) { super.onError(parentId); }
    }

    private class MyMediaControllerCallback extends MediaControllerCompat.Callback{
        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            super.onMetadataChanged(metadata);
            musicViewModel.SyncMusicInformation();
        }

        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat playbackState) {
            super.onPlaybackStateChanged(playbackState);
            int state = playbackState.getState();
            Log.w(TAG, "onPlaybackStateChanged：" + playbackState.getState());
            musicViewModel.setPlaybackState(playbackState.getState());
            playbackStateChanged(playbackState, activityMainMusicBinding.mainActivityIvPlayLoading);
        }

        @Override
        public void onSessionEvent(String event, Bundle extras) {
            super.onSessionEvent(event, extras);
        }
    }

    private void initView(){
        //绑定控件id
        activityMainMusicBinding.activityMainUiRoot.setOnApplyWindowInsetsListener(this);
        //绑定进入MainMusic界面
        activityMainMusicBinding.activityMainGridLayout.setOnClickListener(view -> {
            startActivity(new Intent(MainMusicActivity.this, MusicActivity.class));
        });
        //绑定进入歌词界面
        activityMainMusicBinding.mainActivityBottomLayout.setOnClickListener(v -> {
            startActivity(new Intent(MainMusicActivity.this, SongLrcActivity.class));
            overridePendingTransition(R.anim.push_in, 0);
                }
        );

        super.initAnimation(activityMainMusicBinding.mainActivityBottomIvAlbum);
    }

    private void UpdateProgressBar(){
        if (mTimer != null){
            return;
        }
        mTimer = new Timer();
        mTimer.schedule(musicViewModel.getCircleBarTask(), 300, 300);
    }

    private void StopProgressBar() {
        if (mTimer != null) {
            mTimer.purge();
            mTimer.cancel();
            mTimer = null;
        }
    }
}