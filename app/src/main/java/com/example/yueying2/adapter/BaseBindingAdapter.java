package com.example.yueying2.adapter;

import android.app.Application;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.databinding.BindingAdapter;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ObservableArrayList;
import androidx.databinding.ObservableList;
import androidx.databinding.ViewDataBinding;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * @author : Bilibili喜闻人籁
 * @since : 2021/9/25
 * 作用:
 */
public abstract class BaseBindingAdapter<M,B extends ViewDataBinding>
        extends RecyclerView.Adapter<BaseBindingAdapter.BaseBindingViewHolder>{

    private static final String TAG = "BaseBindingAdapter";

    protected Application context;
    private ObservableArrayList<M> mItems;
    protected ListChangedCallback mListChangedCallback;

    public BaseBindingAdapter(Application context) {
        this.context = context;
        this.mItems = new ObservableArrayList<>();
        this.mListChangedCallback = new ListChangedCallback();
    }

    public ObservableArrayList<M> getItems() {
        ObservableArrayList<M> m = new ObservableArrayList<>();
        m.addAll(mItems);
        return m;
    }

    public BaseBindingAdapter<M, B> setItems(List<M> newItems) {
        if (newItems == null) return this;
        if (mItems != null) {
            if (mItems.size() > 0) mItems.clear();
            mItems.addAll(newItems);
        }
        return this;
    }

    public static class BaseBindingViewHolder extends RecyclerView.ViewHolder{

        public BaseBindingViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    @Override
    public int getItemCount() {
        return mItems == null ? 0 : this.mItems.size();
    }
    /*
     * 释放资源
     * */
    protected void release(){
        if (context != null) { context = null; }
        if (mListChangedCallback != null) { mListChangedCallback = null; }
        if (mItems != null) {
            if (mItems.size() > 0) { mItems.clear(); }
            mItems = null;
        }
    }
    /*
     * 视图、数据绑定
     * */
    @NonNull
    @Override
    public BaseBindingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        B binding = DataBindingUtil.inflate(LayoutInflater.from(this.context),
                this.getLayoutResId(viewType), parent, false);
        return new BaseBindingViewHolder(binding.getRoot());
    }

    @Override
    public void onBindViewHolder(@NonNull BaseBindingViewHolder holder, int position) {
        B binding = DataBindingUtil.getBinding(holder.itemView);
        this.onBindItem(binding,this.mItems.get(position),position);
    }
    //子类实现
    @LayoutRes
    protected abstract int getLayoutResId(int ViewType);

    protected abstract void onBindItem(B binding, M item, int position);
    /*
     * RecyclerView视图分离与固定
     * */
    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        if (mItems != null) this.mItems.addOnListChangedCallback(mListChangedCallback);
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        if (mItems != null) this.mItems.removeOnListChangedCallback(mListChangedCallback);
    }
    /*
     * 处理数据集合变化
     * */
    protected void onChange(ObservableArrayList<M> newItems){
        resetItems(newItems);
        notifyDataSetChanged();
    }

    protected void onItemRangeChanged(ObservableArrayList<M> newItems, int positionStart, int itemCount) {
        resetItems(newItems);
        notifyItemRangeChanged(positionStart, itemCount);
    }

    protected void onItemRangeInserted(ObservableArrayList<M> newItems, int positionStart, int itemCount) {
        resetItems(newItems);
        notifyItemRangeInserted(positionStart, itemCount);
    }

    protected void onItemRangeMoved(ObservableArrayList<M> newItems) {
        resetItems(newItems);
        notifyDataSetChanged();
    }

    protected void onItemRangeRemoved(ObservableArrayList<M> newItems, int positionStart, int itemCount) {
        resetItems(newItems);
        notifyItemRangeRemoved(positionStart, itemCount);
    }

    protected void resetItems(ObservableArrayList<M> newItems) {
        this.mItems = newItems;
    }

    private class ListChangedCallback extends ObservableList.OnListChangedCallback<ObservableArrayList<M>>{

        @Override
        public void onChanged(ObservableArrayList<M> newItems) {
            Log.d(TAG, "onChanged: ");
            BaseBindingAdapter.this.onChange(newItems);
        }

        @Override
        public void onItemRangeChanged(ObservableArrayList<M> newItems, int positionStart, int itemCount) {
            BaseBindingAdapter.this.onItemRangeChanged(newItems, positionStart, itemCount);
            Log.d(TAG, "onItemRangeChanged: ");
        }

        @Override
        public void onItemRangeInserted(ObservableArrayList<M> newItems, int positionStart, int itemCount) {
            BaseBindingAdapter.this.onItemRangeInserted(newItems, positionStart, itemCount);
            Log.d(TAG, "onItemRangeInserted: ");
        }

        @Override
        public void onItemRangeMoved(ObservableArrayList<M> newItems, int fromPosition, int toPosition, int itemCount) {
            BaseBindingAdapter.this.onItemRangeMoved(newItems);
            Log.d(TAG, "onItemRangeMoved: ");
        }

        @Override
        public void onItemRangeRemoved(ObservableArrayList<M> newItems, int positionStart, int itemCount) {
            BaseBindingAdapter.this.onItemRangeRemoved(newItems, positionStart, itemCount);
            Log.d(TAG, "onItemRangeRemoved: ");
        }
    }

    /**
     * 作用: 解决在使用dataBinding 在布局文件给ImageView src属性绑定DrawableResId，不显示相应图片或显示颜色块的问题
     * 参考：https://blog.csdn.net/Ryfall/article/details/
     * 90750270?spm=1001.2101.3001.6650.3&utm_medium=distribute.pc_relevant.none
     * -task-blog-2%7Edefault%7ECTRLIST%7Edefault-3.no_search_link&depth_1-utm_source
     * =distribute.pc_relevant.none-task-blog-2%7Edefault%7ECTRLIST%7Edefault-3.no_search_link
     *      过程分析：https://blog.csdn.net/zhuhai__yizhi/article/details/52181697*/
    @BindingAdapter("android:src")
    public static void setSrc(ImageView view, int resId) {
        //Log.d(TAG, "setSrc: ");
        view.setImageResource(resId);
    }



}
