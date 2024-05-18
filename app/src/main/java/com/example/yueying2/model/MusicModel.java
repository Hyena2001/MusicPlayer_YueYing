package com.example.yueying2.model;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.v4.media.MediaMetadataCompat;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.example.yueying2.R;
import com.example.yueying2.bean.MusicBean;
import com.example.yueying2.util.PictureUtil;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * @author : Bilibili喜闻人籁
 * @since : 2021/9/23
 * 作用:
 */
public class MusicModel implements BaseModel{
    private static final String TAG = "MusicModel";

    @Override
    public void getLocalMusic(OnMusicListener onMusicListener, ContentResolver resolver) {
        onMusicListener.OnComplete(getLocalMusic(resolver));
    }


    @Override
    public void getLocalMusicMetadata(OnMusicMetadataListener onMusicListener, ContentResolver resolver) {
        onMusicListener.OnComplete(getLocalMusicMetadata(resolver));
    }

    @Override
    public void getLocalMusicAlbum(OnLoadPictureListener onLoadPictureListener, String path, Resources resource) {
        Bitmap bitmap = getAlbumBitmap(path);
        bitmap = bitmap == null ?
                PictureUtil.getResIdBitmap(R.drawable.icon_fate,500,resource,0) : bitmap;
        onLoadPictureListener.OnComplete(new WeakReference<>(bitmap));
    }
    /**
     * 项目 Bilibili@喜闻人籁[MVVM|MediaSession]音频应用示例
     * 描述 获取本地音乐列表
     * @param resolver 内容访问器，这个类提供对内容模型的应用程序访问。
     * @return List<MusicBean>类型的本地音乐列表集合
     * @author 喜闻人籁
     * time 2021/11/07 21:01
     **/

    @SuppressLint("Range")
    private List<MusicBean> getLocalMusic(ContentResolver resolver){
        Log.e(TAG, "getLocalMusic: "+(resolver == null));
        if (resolver == null) return null;

        List<MusicBean> beans = new ArrayList<>();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor cursor = resolver.query(uri,null,null,null);
        int id = 0;
        while (cursor != null && cursor.moveToNext()) {
            long duration = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
            if(duration < 90000 ) continue;

            String title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
            String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
            String album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
            String path = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
            id++;
            String sid = String.valueOf(id);

            MusicBean bean = new MusicBean(sid,
                                            getSpanishStr(title),
                                            getSpanishStr(artist),
                                            getSpanishStr(album),
                                            path,path,duration);
            beans.add(bean);
            //Log.d(TAG, "getLocalMusic: "+getUtf8(title));
        }
        if (cursor != null){
            cursor.close();
            if(!cursor.isClosed()) cursor.close();
        }
        Log.d(TAG, "getLocalMusic: "+beans.size());
        return beans;
    }

    /**
    * 将西班牙语重音乱码替换为UTF-8
    * */
    private static String getSpanishStr(String str) {
        if (str == null || TextUtils.isEmpty(str)) return str;
        return str.replaceAll("¨¢","á")
                    .replaceAll("¨¦","é")
                    .replaceAll("¨Ş","í")
                    .replaceAll("¨ª","í")
                    .replaceAll("¨®","ó")
                    .replaceAll("¨²","ú");
    }
    /**
     * 项目 Bilibili@喜闻人籁[MVVM|MediaSession]音频应用示例
     * 描述 获取本地音乐列表
     * @param resolver 内容访问器，这个类提供对内容模型的应用程序访问。
     * @return LinkedHashMap<String, MediaMetadataCompat>类型的本地音乐列表集合
     * @author 喜闻人籁
     * time 2021/11/10 20:11
     **/

    @SuppressLint("Range")
    private LinkedHashMap<String, MediaMetadataCompat> getLocalMusicMetadata(ContentResolver resolver){
        //Log.e(TAG, "getLocalMusicMetadata: "+(resolver == null));
        if (resolver == null) return null;

        LinkedHashMap<String, MediaMetadataCompat> result = new LinkedHashMap<>();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor cursor = null;
        try {
            cursor = resolver.query(uri,null,null,null);
        }catch (Exception e){
            e.printStackTrace();
        }
        while (cursor != null && cursor.moveToNext()) {
             long duration = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION));
            if(duration < 90000) continue;

            String title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
            String artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
            String album = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM));
            String path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
            String mediaId = title.replaceAll(" ", "_");
            //西班牙语重音字母替换
            title = getSpanishStr(title);
            artist = getSpanishStr(artist).replaceAll("&","/");
            album = getSpanishStr(album);

            MediaMetadataCompat metadata = new MediaMetadataCompat.Builder()
                                    .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, mediaId)
                                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, album)
                                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
                                    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)
                                    .putString(MediaMetadataCompat.METADATA_KEY_GENRE, "")
                                    .putString(
                                            MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, path)
                                    .putString(
                                            MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI, path)
                                    .putString(
                                            MediaMetadataCompat.METADATA_KEY_MEDIA_URI, path)
                                    .build();
            result.put(mediaId,metadata);
            //Log.d(TAG, "getLocalMusicMetadata: "+artist);
        }
        if (cursor != null){
            cursor.close();
            if(!cursor.isClosed()) cursor.close();
        }
        //Log.d(TAG, "getLocalMusicMetadata: "+result.size());
        return result;
    }

    /**
     * description 返回一个本地音乐文件的专辑bitmap图片
     * @param Path 给定当前点击item音乐的外部存储路径，非content
     * @return Bitmap类型专辑图片
     * @author 喜闻人籁
     * time 2020/12/18 12:22
     **/
    private Bitmap getAlbumBitmap(String Path){
        if (Path.isEmpty()) return null;//返回默认的专辑封面
        if (!FileExists(Path)) return null; //找不到文件返回空

        Log.d(TAG, "getAlbumBitmap: "+Path);

        if (!Path.contains(".mp3")) {
            Bitmap bitmap;
            BufferedInputStream bis = null;
            try {
                bis = new BufferedInputStream(new FileInputStream(Path));
                bitmap = BitmapFactory.decodeStream(bis);
            }  catch (IOException e) {
                e.printStackTrace();
                Log.d("AllSongSheetModel", "getAlbumBitmap: 本地图片转Bitmap失败");
                return null;
            }finally {
                if (bis != null) {
                    try {
                        bis.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.d("AllSongSheetModel", "getAlbumBitmap: 输出流关闭异常");
                    }
                }
            }
            //Log.d("加载本地图片", "getAlbumBitmap: ");
            return bitmap;
        }else {
            MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
            metadataRetriever.setDataSource(Path);
            byte[] picture = metadataRetriever.getEmbeddedPicture();

            /*每次拿到专辑图片后，关闭MediaMetadataRetriever对象，等待GC器回收内存
             *以便下一次再重新引用（new），避免内存泄漏*/
            metadataRetriever.release();//SDK > 26 才有close，且close与release是一样的
            //返回默认的专辑封面
            return picture == null ? null :
                    BitmapFactory.decodeByteArray(picture, 0, picture.length);
        }
    }

    public static boolean FileExists(String targetFileAbsPath){
        try {
            File f = new File(targetFileAbsPath);
            if(!f.exists()) return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
