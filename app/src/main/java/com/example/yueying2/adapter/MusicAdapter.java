package com.example.yueying2.adapter;

import static android.support.v4.media.MediaBrowserCompat.*;

import android.app.Application;
import android.media.browse.MediaBrowser;
import android.support.v4.media.MediaBrowserCompat;
import android.view.View;

import androidx.databinding.ObservableArrayList;

import com.example.yueying2.R;
import com.example.yueying2.bean.MusicBean;
import com.example.yueying2.databinding.ItemMusicBinding;

import java.util.Objects;

/**
 * @author : Hyena王紫涓
 * @since : 2022-06-01
 * 作用
 */
public class MusicAdapter extends BaseBindingAdapter<MediaItem, ItemMusicBinding>{

    private static final String TAG = "MusicAdapter";
    private ObservableArrayList<MediaItem> mSearchMediaItems, mSheetMediaItems;
    private OnItemClickListener mItemClickListener;

    public interface OnItemClickListener{
        void ItemClickListener(View view, int position);
        void ItemMoreClickListener(View view, int position);
    }

    public void setItemClickListener(OnItemClickListener itemClickListener){
        this.mItemClickListener = itemClickListener;
    }

    public MusicAdapter(Application context) {
        super(context);
    }

    @Override
    protected int getLayoutResId(int ViewType) {
        return R.layout.item_music;
    }

    @Override
    protected void onBindItem(ItemMusicBinding binding, MediaItem item, int position) {

        //将MediaItem转化为MusicBean
        int number = position;
        String artist = Objects.requireNonNull(item.getDescription().getSubtitle()).toString(),
                album = Objects.requireNonNull(item.getDescription().getDescription()).toString();
        MusicBean bean = new MusicBean(String.valueOf(++number),
                Objects.requireNonNull(item.getDescription().getTitle()).toString(),
                artist,album,
                Objects.requireNonNull(item.getDescription().getMediaUri()).toString(),
                Objects.requireNonNull(item.getDescription().getMediaUri()).toString(),
                100000);

        //原本传入item，现在是bean
        binding.setMusicInfo(bean);

        //绑定点击事件
        if (mItemClickListener == null){
            return;
        }
        binding.itemMusicListLayout.setOnClickListener(view -> mItemClickListener.ItemClickListener(view, position));
        binding.itemLocalMusicMore.setOnClickListener(view -> mItemClickListener.ItemMoreClickListener(view, position));
    }

    public void release(){
        super.release();
        if (mItemClickListener != null){
            mItemClickListener = null;
        }
    }
}
