package com.example.yueying2.model;

import android.content.ContentResolver;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.support.v4.media.MediaMetadataCompat;;
import com.example.yueying2.bean.MusicBean;

import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * @author : Bilibili喜闻人籁
 * @since : 2021/9/23
 * 作用: Model在程序中专门用于提供数据，
 *      不管是网络请求获得的数据，还是数据库获得的数据，统统写在Model里。
 *      Model层独立性相当强，它只用来提供数据，而不管数据是用来做什么的。
 */
public interface BaseModel {
    //获得本地音乐元数据，返回List<customBean>集合数据
    void getLocalMusic(OnMusicListener onMusicListener, ContentResolver resolver);
    interface OnMusicListener{
        void OnComplete(List<MusicBean> beans);
    }
    //获得本地音乐元数据，返回List<MediaBrowserCompat.MediaItem>集合数据，适用于MediaSession媒体框架
    void getLocalMusicMetadata(OnMusicMetadataListener onMusicListener, ContentResolver resolver);
    interface OnMusicMetadataListener{
        void OnComplete(LinkedHashMap<String, MediaMetadataCompat> musicMaps);
    }
    //获得一首本地音乐的专辑图片，Bitmap
    void getLocalMusicAlbum(OnLoadPictureListener onLoadPictureListener, String path, Resources resources);
    interface OnLoadPictureListener{
        void OnComplete(WeakReference<Bitmap> bitmap);
    }
}
