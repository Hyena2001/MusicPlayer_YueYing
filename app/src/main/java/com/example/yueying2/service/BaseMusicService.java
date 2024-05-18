package com.example.yueying2.service;

import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media.MediaBrowserServiceCompat;

import java.util.List;

/**
 * @author : Hyena王紫涓
 * @since : 2022-06-03
 * 作用
 */
public abstract class BaseMusicService extends MediaBrowserServiceCompat {

    private static final String TAG = "BaseMusicService";

    private MediaControllerCompat mMediaController;
//    private MediaNotificationManager mMediaNotificationManager;
//    private MyBlueToothBroadcastReceiver mBlueToothReceiver;
    private boolean isStartForeground;
    private static final int MEDIA_CHANNEL_ID = 130;
    //通知Action，componentName
    public static final String DYQL_CUSTOM_ACTION_COLLECT_SONGS = "collect_songs_dyql";
    public static final String DYQL_CUSTOM_ACTION_SHOW_LYRICS = "show_lyrics_dyql";
    public static final String DYQL_CUSTOM_ACTION_PLAY = "play_dyql";
    public static final String DYQL_CUSTOM_ACTION_PAUSE = "pause_dyql";
    public static final String DYQL_CUSTOM_ACTION_PREVIOUS = "previous_dyql";
    public static final String DYQL_CUSTOM_ACTION_NEXT = "next_dyql";
    public static final String DYQL_CUSTOM_ACTION_STOP = "stop_dyql";
    public static final String DYQL_NOTIFICATION_STYLE = "notification_style_dyql";

}
