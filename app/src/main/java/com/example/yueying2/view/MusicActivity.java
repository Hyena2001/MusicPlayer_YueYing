package com.example.yueying2.view;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.View;
import android.view.WindowInsets;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.yueying2.BaseActivity;
import com.example.yueying2.MainMusicActivity;
import com.example.yueying2.R;
import com.example.yueying2.adapter.MusicAdapter;
import com.example.yueying2.bean.MusicBean;
import com.example.yueying2.databinding.ActivityContentMusicBinding;
import com.example.yueying2.model.BaseModel;
import com.example.yueying2.model.MusicModel;
import com.example.yueying2.viewmodel.MusicViewModel;

import java.util.List;
import java.util.Timer;

/**
 * @author : Hyena王紫涓
 * @since : 2022-06-01
 * 作用
 */
public class MusicActivity extends BaseActivity {

    private static final String TAG = "BaseActivity";

    //绑定的XML是activity_content_music
    private ActivityContentMusicBinding mMusicBinding;
    private MusicViewModel mMusicViewModel;
    private MusicAdapter mMusicAdapter;
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
    protected void onCreate(@Nullable Bundle saveInstanceState){
        super.onCreate(saveInstanceState);

        //和activity_content_music进行绑定
        mMusicBinding = DataBindingUtil.setContentView(this, R.layout.activity_content_music);
        mMusicViewModel = new MusicViewModel(getApplication());
        mMusicBinding.setMusicInfo(mMusicViewModel);

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
        if (mMusicAdapter != null) {
            mMusicAdapter.release();
            mMusicAdapter = null;
        }
        if (mMusicViewModel != null) { mMusicViewModel = null; }
        if (mMusicBinding != null) {
            mMusicBinding.unbind();
            mMusicBinding = null;
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
    public void onPointerCaptureChanged(boolean hasCapture) {
        super.onPointerCaptureChanged(hasCapture);
    }

    private class MyMediaBrowserSubscriptionCallback extends MediaBrowserCompat.SubscriptionCallback{
        @Override
        public void onChildrenLoaded(@NonNull String parentId,
                                     @NonNull List<MediaBrowserCompat.MediaItem> children) {
            super.onChildrenLoaded(parentId, children);
            Log.d(TAG, "onChildrenLoaded: ");
            mMusicAdapter.setItems(children);
            MediaControllerCompat mediaController =
                    MediaControllerCompat.getMediaController(MusicActivity.this);
            mMusicViewModel.setMediaControllerCompat(mediaController);
            mMusicViewModel.SyncMusicInformation();
            //同步播放动画
            PlaybackStateCompat playBackState = mediaController.getPlaybackState();
            mMusicViewModel.setPlaybackState(playBackState.getState());
            playbackStateChanged(playBackState, mMusicBinding.mainActivityIvPlayLoading);
        }

        @Override
        public void onError(@NonNull String parentId) { super.onError(parentId); }
    }

    private class MyMediaControllerCallback extends MediaControllerCompat.Callback{
        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            super.onMetadataChanged(metadata);
            Log.w(TAG, "onMetadataChanged() return:");
            mMusicViewModel.SyncMusicInformation();
        }

        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat playbackState) {
            super.onPlaybackStateChanged(playbackState);
            int state = playbackState.getState();
            Log.w(TAG, "onPlaybackStateChanged：" + playbackState.getState());
            mMusicViewModel.setPlaybackState(playbackState.getState());
            playbackStateChanged(playbackState, mMusicBinding.mainActivityIvPlayLoading);
        }

        @Override
        public void onSessionEvent(String event, Bundle extras) {
            super.onSessionEvent(event, extras);
        }
    }

    private void initView(){
        //绑定控件id(.后面的是xml中id的变形)
        mMusicBinding.activityContentMusicUiRoot.setOnApplyWindowInsetsListener(this);
        //绑定返回按钮(.后面的是xml中id的变形)
        mMusicBinding.musicActivityIvReturn.setOnClickListener(view -> {
                        finish();
        });

        mMusicBinding.mainActivityBottomLayout.setOnClickListener(v -> {
            startActivity(new Intent(MusicActivity.this, SongLrcActivity.class));
            overridePendingTransition(R.anim.push_in, 0);
        });

        super.initAnimation(mMusicBinding.mainActivityBottomIvAlbum);

        //初始化RecyclerView
        mMusicBinding.musicActivityRvMusic.setLayoutManager(new LinearLayoutManager(this));
        mMusicAdapter = new MusicAdapter(getApplication());
        mMusicBinding.musicActivityRvMusic.setAdapter(mMusicAdapter);
        //点击歌曲打开页面
        mMusicAdapter.setItemClickListener(new MusicAdapter.OnItemClickListener() {
            @Override
            public void ItemClickListener(View view, int position) {
                MediaControllerCompat mediaController =
                        MediaControllerCompat.getMediaController(MusicActivity.this);
                mediaController.getTransportControls().playFromMediaId(
                        mMusicAdapter.getItems().get(position).getMediaId(),
                        null);
                Log.d(TAG, "ItemClickListener：点击了列表" + mMusicAdapter.getItems().get(position).getDescription());
            }

            @Override
            public void ItemMoreClickListener(View view, int position) {
                Log.d(TAG, "ItemMoreClickListener：点击了更多" + position);
            }
        });
    }

    private void UpdateProgressBar(){
        if (mTimer != null){
            return;
        }
        mTimer = new Timer();
        mTimer.schedule(mMusicViewModel.getCircleBarTask(), 300, 300);
    }

    private void StopProgressBar() {
        if (mTimer != null) {
            mTimer.purge();
            mTimer.cancel();
            mTimer = null;
        }
    }
}
