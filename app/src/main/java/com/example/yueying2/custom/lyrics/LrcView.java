package com.example.yueying2.custom.lyrics;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import com.example.yueying2.R;

import java.util.ArrayList;
import java.util.List;

/**
 * @author : Bilibili喜闻人籁
 * @since : 2021年9月28日
 * 作用：仿网易云音乐歌词控件 1.多行绘制 自动滚动 触摸滑动
 *                        2.触摸滑动 等待时间后回滚至高亮显示
 *                        3.组合控件 与ImageView TextView配合提示用户当前滑动位置的播放时间*/

public class LrcView extends View {
    private static final String TAG = "LrcView";

    private List<LrcBean> list;
    private TextPaint gPaint; //普通
    private TextPaint hPaint; //高亮
    private int width = 0, height = 0;
    private int currentPosition = 0;
    private MediaControllerCompat mMediaController;
    private int lastPosition = 0;
    private int highLineColor;
    private int lrcColor;
    private int mode = 0;
    private final static int textSize = 50;
    private final static int rowSpacing = 110;//行距宽度;
    private boolean isTouched = false,Tips = false,isFirstShow,isScrolling,isShowTranslate;
    private float[] mRowSpacings;
    private float lastDistance = 0, distanceAll = 0;
    /* 滑动手势事件成员变量
     * x，y : 手势滑动时计算滑动距离
     * touchTime：1.onDraw()中判断，使本控件滚动到高亮歌词处 touchTime = 0； 第一次显示歌词控件或者滑动本控件后未做其他动作
     *      2.当用户滑动本歌词控件，单击左侧播放按钮 进行播放时 执行clearTouchAction()方法，使本控件自动滚用延后800ms执行
     *      或者在控件外部更新SeekBar进度 UpdateMusicProcess() 都会使touchTime = 1；立即调整滚动至正确的位置
     * delayTime：需要等待多久才开始滚动至高亮歌词处，默认为800ms，当用户手动滑动时，设置为2000ms，或者更久
     */
    private float /*intercept_X,*/intercept_Y;
    private float /*lastOffsetX = 0,*/lastOffsetY = 0,touchedDistance = 0;
    private long touchTime = 0, delayTime = 3000;
    //自定义组合控件
    private MyLrcView mSuperLayout;
    /**构造方法****/
    public LrcView(Context context) { this(context, null);Log.d(TAG, "LrcView: 1"); }

    public LrcView(Context context, AttributeSet attrs) { this(context, attrs, 0); }

    public LrcView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        Log.d(TAG, "LrcView: 3");
        isFirstShow = true;
        isShowTranslate = true;
        setClickable(true);
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.LrcView);
        highLineColor = ta.getColor(R.styleable.LrcView_highLineColor, getResources().getColor(R.color.colorWhite,null));
        lrcColor = ta.getColor(R.styleable.LrcView_lrcColor, getResources().getColor(android.R.color.darker_gray,null));
        mode = ta.getInt(R.styleable.LrcView_lrcMode,mode);
        ta.recycle();

        SharedPreferences preferences = getContext().getSharedPreferences("UserLastMusicPlay", 0);
        int textSize = preferences.getInt("lrc_view_size",42);
        highLineColor = preferences.getInt("lrc_view_color",highLineColor);

        gPaint = new TextPaint();
        gPaint.setAntiAlias(true);
        gPaint.setColor(lrcColor);
        gPaint.setTextSize(textSize);//像素为单位
        gPaint.setTextAlign(Paint.Align.CENTER);
        hPaint = new TextPaint();
        hPaint.setAntiAlias(true);
        hPaint.setColor(highLineColor);
        hPaint.setTextSize(textSize);
        hPaint.setStyle(Paint.Style.FILL);
        hPaint.setTextAlign(Paint.Align.CENTER);

    }
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        //设置虚化模式（上下），虚化区域（顶部和底部向中间的距离区域 - 200）,
        // 【MIUI】深色模式下无效，暂不知原因[2021.6.3]
        this.setVerticalFadingEdgeEnabled(true);
        this.setFadingEdgeLength(200);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        release();
        //System.gc();
    }
    /** 释放资源*/
    public void release(){
        if (mSuperLayout != null) mSuperLayout = null;
        if (mMediaController != null) mMediaController = null;
        Log.d(TAG, "onDetachedFromWindow: ");
        if (list != null) {
            list.clear();
            list = null;
        }
        if (gPaint != null) {
            gPaint.reset();
            gPaint = null;
        }
        if (hPaint != null) {
            hPaint.reset();
            hPaint = null;
        }
        mRowSpacings = null;
    }
    /*** 绘制歌词 * */
    @Override
    protected void onDraw(Canvas canvas) {
        if (width == 0 || height == 0) {
            width = getMeasuredWidth();
            height = getMeasuredHeight();
            Log.d(TAG, "onDraw: "+width+" "+(width / textSize));
        }
        //暂无歌词与加载歌词提示
        if (this.list == null || list.size() == 0) {
            if (!Tips) {
                gPaint.setFlags(Paint.UNDERLINE_TEXT_FLAG);//FLAG ：带下划线
                canvas.drawText("暂无歌词", width >> 1, height >> 1, gPaint);
            }else {canvas.drawText("加载歌词中...", width >> 1, height >> 1, gPaint); }
            return;
        }
        //绘制歌词
        gPaint.setFlags(Paint.LINEAR_TEXT_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        //画出高亮和普通部分歌词
        drawLrc(canvas);
        if (!isTouched) { NoTouchedDraw(); } //滚动方法

        postInvalidateDelayed(100);
    }
    private void NoTouchedDraw(){
        //获取当前的播放进度 和 到该播放哪句歌词的位置list.get(x) -> currentPosition
        int currentMillis = getCurrentPosition();
        float distance = 0;
        for (int i = 0; i < currentPosition; i++){
            distance += mRowSpacings[i];
        }
        long currentDelayTime = isFirstShow ? 800 : delayTime;
        //     触摸结束后的delayTime里歌词控件保持不动，等待用户再次触摸滑动，
        //                  或者delayTime结束后花费1000ms后滚动至高亮歌词处并回归自动滚动状态。
        if (touchTime == 0) touchTime = System.currentTimeMillis();
            //更改音乐进度条时
        else if(touchTime == 1) touchTime = System.currentTimeMillis() - delayTime;
        //回滚或自动滚动歌词
        if (System.currentTimeMillis() - touchTime > currentDelayTime
                && System.currentTimeMillis() - touchTime < currentDelayTime + 1000) {
            ReturnScrollLrcState(distance,currentDelayTime);
        }else if(System.currentTimeMillis() - touchTime > delayTime + 1000)
            AutoScrollLrcState(currentMillis,distance);
        //Log.d(TAG, "onDraw: 滚动至 "+getScrollY()+" 间距："+distance);
        /*
         * {@link getScrollY()} 返回此视图的滚动顶部位置。
         * 这是视图显示部分的上边缘。你不需要在上面画任何像素，因为它们在屏幕上你的视图框架之外。*/
        if (getScrollY() == distance) {
            //Log.d("", "onDraw: lastPosition"+lastPosition);
            lastPosition = currentPosition;
            lastDistance = distance;
        }
    }
    private void ReturnScrollLrcState(float distance,long delayTime){
        //Log.d(TAG, "ReturnScrollLrcState: "+isFirstShow);
        if (mSuperLayout != null) mSuperLayout.hideLrcInfoView();
        //由触摸滑动位置滚动至高亮歌词位置，1000ms完成
        //当高亮歌词里程大于触摸结束时里程，歌词应向上滚动至歌词高亮处,反之则向下滚动
        //当前时间与花费1000ms后滚动至高亮歌词处的时间比值
        float percentDistance = (float)(System.currentTimeMillis() - touchTime - delayTime) / 1000;
        float scrollY;
        if (distance > touchedDistance) {
            scrollY = percentDistance * Math.abs(distance - touchedDistance) + touchedDistance;
        }else scrollY = touchedDistance - percentDistance * Math.abs(distance - touchedDistance);
        setScrollY((int) scrollY);
    }
    private void AutoScrollLrcState(int currentMillis,float distance){
        //Log.d(TAG, "AutoScrollLrcState: "+isFirstShow);
        //获得正唱歌词的开始时间（当前播放进度应该显示的歌词）
        long start = list.get(currentPosition).getStart();
        long different = currentMillis - start;
        //int differentPosition = currentPosition - lastPosition;
        //Log.d(TAG, "onDraw: "+currentMillis+", "+start);
        /*
         * v : 行与行之间的跳转速度
         * different <= 500(ms): 高亮歌词才开始唱的时候
         * different > 500(ms) : 高亮歌词至少唱了 500ms 直到该句歌词结束
         * differentPosition 不能少，在调整进度时，会有平滑滚动过渡*/
            /*float v = different > 500 ? currentPosition * rowSpacing :
                    (lastPosition + differentPosition * (different / 500f)) * rowSpacing;*/
        float v = different > 500 ? distance : different / 500f * (distance - lastDistance) + lastDistance;
        //if( different <= 500 ) Log.d(TAG, "onDraw: "+(different / 500f)+" "+differentPosition);
        //Log.d(TAG, "onDraw: 绘制距离 "+distance);
        //Log.d(TAG, "v = "+v+", "+((currentMillis - start) > 500));
        int scrollY = v > distance ? (int) distance : (int)v;
        setScrollY(mMediaController.getPlaybackState().getState() == PlaybackStateCompat.STATE_STOPPED
                ? (int)distance : scrollY);//设置滚动到的位置
        isFirstShow = false;
    }

    /**根据getScrollY()的位置，获得应该高亮显示哪句歌词（currentPosition）*/
    private int getScrollPosition(int scrollY){
        if(list == null || list.size() <= 0) return 0;

        //1.首先通过里程获得当前滚动到哪句歌词了（scrollPosition）
        float distance = 0;
        for (int i = 0; i < list.size(); i++){
            if (scrollY < distance + (rowSpacing >> 1)) {
                //Log.d(TAG, "getScrollFirstTime: "+scrollY +", "+i);
                return i;
            }
            else distance += mRowSpacings[i];
        }
        //Log.d(TAG, "getScrollPosition:  distance = "+distance+", "+scrollY);
        return 0;
    }
    public void setSuperLayout(MyLrcView superLayout) {
        mSuperLayout = superLayout;
    }
    //预留给桌面悬浮歌词的方法
    public void setHighLineColor(@ColorInt int highLineColor) {
        this.highLineColor = highLineColor;
    }

    public int getHighLineColor() {
        return highLineColor;
    }

    public void setLrcColor(@ColorInt int lrcColor) {
        this.lrcColor = lrcColor;
    }
    public void setTextSize() {
        if (hPaint == null) return;
        float x = hPaint.getTextSize() + 2;
        if(x < 36) x = 56;
        if(x > 56) x = 36;
        this.hPaint.setTextSize(x);
        this.gPaint.setTextSize(x);
        //保存字体大小设置
        SharedPreferences.Editor editor = getContext().getSharedPreferences(
                                                        "UserLastMusicPlay", 0).edit();
        editor.putInt("lrc_view_size", (int) x);
        editor.apply();

    }
    public int setTextColor() {
        if (hPaint == null) return highLineColor;
        Log.d(TAG, "setTextColor: "+highLineColor);
        if (highLineColor == getResColor("#EEEEEE")) {
            highLineColor = getResColor("#EE0000");
        }else if (highLineColor == getResColor("#EE0000")) {
            highLineColor = getResColor("#EE0C58");
        }else if (highLineColor == getResColor("#EE0C58")) {
            highLineColor = getResColor("#FFEB3B");
        }else if (highLineColor == getResColor("#FFEB3B")) {
            highLineColor = getResColor("#FF6736");
        }else if (highLineColor == getResColor("#FF6736")) {
            highLineColor = getResColor("#9F6FFD");
        }else if (highLineColor == getResColor("#9F6FFD")) {
            highLineColor = getResColor("#EEEEEE");
        }
        Log.d(TAG, "setTextColor: "+highLineColor);
        this.hPaint.setColor(highLineColor);
        //保存字体大小设置
        SharedPreferences.Editor editor = getContext().getSharedPreferences(
                "UserLastMusicPlay", 0).edit();
        editor.putInt("lrc_view_color", highLineColor);
        editor.apply();
        return highLineColor;
    }
    private int getResColor(@NonNull String res){
        return Color.parseColor(res);
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public void setTouched(boolean touched) {
        isTouched = touched;
    }
    /**当外部的{@link android.widget.SeekBar}进度条随用户操作而变化，执行此方法*/
    public void UpdateMusicProcess(){
        if (list != null && list.size() > 0) {
            touchTime = 1;
            touchedDistance = getScrollY();
        }
    }
    /**
     * 引用服务对象
     * 条件：1.当本view的MusicService对象为空时
     *       2. 当两个MusicService对象引用不一致时
     *       （更改系统样式 或者 久居后台Activity界面被回收 导致的Activity ReCreate 重新绑定MusicService）
     * */
    public void setMediaControllerCompat(MediaControllerCompat mediaControllerCompat) {
        if (mMediaController == null || !mMediaController.equals(mediaControllerCompat)) {
            this.mMediaController = mediaControllerCompat;
            //Log.d("LrcView", "setMusicService: ");
        }
    }
    /**
     * 显示加载提示 {"加载歌词中..."}
     * @param tips boolean 是否显示加载歌词提示
     * */
    public void setTips(boolean tips) {

        this.Tips = tips;
        //if (list != null && list.size() > 0) list.clear();

        Log.d("LrcView", "加载歌词提示");
        //init();
    }

    /**
     * 设置标准歌词字符串
     *【判断】什么时候不更新歌词，当前歌词与获取到的歌词一模一样 或者 当前显示获取歌词提示{boolean Tips}
     * @param lrcBeans LrcBean形式的歌词
     * @return true 表示可以更新歌词， false则不更新歌词
     */
    public boolean setLrc(List<LrcBean> lrcBeans) {
        Log.d(TAG, "setLrc: "+MatchesLrcBeans(lrcBeans));

        if (list != null && list.size() > 0) list.clear();
        else list = new ArrayList<>();

        if (!MatchesLrcBeans(lrcBeans)) {

            boolean updateState = lrcBeans != null;
            if (updateState) this.list.addAll(lrcBeans);
            //Log.d(TAG, "setLrc: "+(list == null)+this);
            if (Tips) Tips = false;
            Log.e("LrcView", "在线歌词更新"+(updateState ? "成功" : "失败"));
            postInvalidateDelayed(100);//可以重新调用OnDraw()绘制歌词
            return true;
        }else return false;
    }
    /**
     * 判断两个List<LrcBean>是否相同
     * @param lrcBeans LrcBean形式的歌词
     * @return true 表示两个List<LrcBean>相同， false则不相同
     * */
    public boolean MatchesLrcBeans(List<LrcBean> lrcBeans){
        if(list == null && lrcBeans == null) return false;//1.都为空 返回true
        //2.list或者lrcBeans任意一个为空，其size不相等 返回false
        if (list == null || lrcBeans == null || list.size() != lrcBeans.size()) return false;
        //3.当 它们的size都为0时 返回 true
        if (list.size() == 0) return true;

        for(int i = 0; i < list.size(); i++){
            if(list.get(i).getStart() != lrcBeans.get(i).getStart()) return false;
            if(list.get(i).getEnd() != lrcBeans.get(i).getEnd()) return false;
            if(!list.get(i).getLrc().equals(lrcBeans.get(i).getLrc())) return false;
        }
        return true;
    }

    public boolean isShowTranslate() {
        return isShowTranslate;
    }
    public void setShowTranslate(boolean showTranslate) {
        this.isShowTranslate = showTranslate;
    }
    public void changeShowTranslate(){
        this.isShowTranslate = !isShowTranslate;
    }

    private void drawLrc(Canvas canvas) {
        distanceAll = 0;
        float x = width >> 1, nextY = (height >> 1) - 50 , layoutY = 0, translateY, lastY = 0;
        String lrc,translate;
        TextPaint paint;
        mRowSpacings = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            //Log.d(TAG, "drawLrc: "+y+", "+i);
            lrc = list.get(i).getLrc();
            translate = list.get(i).getTranslateLrc();
            paint = i == currentPosition ? hPaint : gPaint;
            /*Log.d(TAG, "drawLrc: "+lrc);
            Log.d(TAG, "drawLrc: 单行宽度："+width+" , 歌词绘制宽度："+paint.measureText(lrc));*/
            if (width < paint.measureText(lrc)) { //歌词多行绘制
                StaticLayout layout =
                        new StaticLayout(lrc, paint, canvas.getWidth(),
                        Layout.Alignment.ALIGN_NORMAL,
                        1.0f, 0.0f, true);
                canvas.save();
                canvas.translate(x,nextY - (rowSpacing >> 1));
                layout.draw(canvas);
                canvas.restore();
                layoutY = layout.getHeight();
            }else canvas.drawText(lrc, x, nextY, paint);

            //绘制翻译 如果有的话
            if (isShowTranslate() && translate != null && !TextUtils.isEmpty(translate)) {
                translateY = layoutY > 0 ? nextY + layoutY : nextY + textSize * 1.5f;
                if (width < paint.measureText(translate)){//为真则绘制多行翻译
                    StaticLayout layout =
                            new StaticLayout(translate, paint, canvas.getWidth(),
                                    Layout.Alignment.ALIGN_NORMAL,
                                    1.0f, 0.0f, true);
                    canvas.save();
                    canvas.translate(x,translateY - (rowSpacing >> 1));
                    layout.draw(canvas);
                    canvas.restore();
                    nextY = translateY +  rowSpacing - textSize * 1.5f + layout.getHeight();
                }else {
                    canvas.drawText(translate,x,translateY,paint);
                    nextY = translateY + rowSpacing;
                }
            }else nextY += width < paint.measureText(lrc) ? layoutY + rowSpacing - textSize * 1.5f : rowSpacing;

            mRowSpacings[i] = i == 0 ? nextY - (height >> 1) : nextY - lastY;//第一次的间距要减去初始位置（height/2）
            distanceAll += nextY - lastY;

            lastY = nextY;
            layoutY = 0;
            //translateY = 0;
            //Log.d(TAG, "drawLrc: 当前绘制的Y值："+nextY+" 上次绘制的Y值："+lastY+" 歌词间距："+mRowSpacings[i]);
        }
    }

    public void init() {
        currentPosition = 0;
        lastPosition = 0;
        lastDistance = 0;
        setScrollY(0);
        invalidate();
    }

    /**根据player的播放位置，获得歌词的播放位置（与startTime比较）*/
    private int getCurrentPosition() {
        int currentMillis;
        try {
            //1.首先确定当前的播放进度
            currentMillis = (int) mMediaController.getPlaybackState().getPosition();
            //2.根据播放进度确定该高亮显示哪条歌词（currentPosition）
            if (currentMillis < list.get(0).getStart()) {
                currentPosition = 0;
                return currentMillis;
            }
            if (currentMillis > list.get(list.size() - 1).getStart()) {
                currentPosition = list.size() - 1;
                return currentMillis;
            }
            for (int i = 0; i < list.size(); i++) {
                if (currentMillis >= list.get(i).getStart() && currentMillis < list.get(i).getEnd()) {
                    currentPosition = i;
                    return currentMillis;
                }
            }
        } catch (Exception e) {
//            e.printStackTrace();
            postInvalidateDelayed(100);
        }
        return 0;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        Log.d(TAG, "dispatchTouchEvent: "+event.getAction());
        if (list == null || list.size() == 0) return false; //不消耗点击或滑动手势事件
        //触摸事件
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isTouched = true;
                touchTime = 0;
                //intercept_X = event.getX();
                intercept_Y = event.getY();
                Log.d(TAG, "dispatchTouchEvent: Down X轴："+" Y轴："+intercept_Y);
                break;
            case MotionEvent.ACTION_MOVE:
                if (list != null && list.size() <= 0) return false;
                isScrolling = true;
                if (mSuperLayout != null) mSuperLayout.showLrcInfoView();
                float offsetY = event.getY() - intercept_Y;
                if (delayTime < 800) delayTime = 5000;//等待2秒
                //1.先实现手指向上滑动
                Log.d(TAG, "dispatchTouchEvent: Move "+offsetY+" lastY ="+lastOffsetY+" 移动距离 "+Math.abs(offsetY - lastOffsetY));
                int scrollY;
                if (offsetY - lastOffsetY < 0) {//手指向上滑动
                    scrollY = (int) (getScrollY() + Math.abs(offsetY - lastOffsetY));
                }else scrollY = (int) (getScrollY() - Math.abs(offsetY - lastOffsetY));
                //2.上边界与下边界
                scrollY = (int) Math.min(scrollY, distanceAll - mRowSpacings[list.size() - 1]);
                setScrollY(Math.max(scrollY, 0));

                if (mSuperLayout != null)
                    mSuperLayout.setText(this.list.get(getScrollPosition(scrollY)).getStart());
                //setScrollY(scrollY);
                lastOffsetY = offsetY;
                break;
            case MotionEvent.ACTION_UP:
                Log.d(TAG, "dispatchTouchEvent: Up");
                lastOffsetY = 0;
                isTouched = false;
                touchedDistance = getScrollY();
                if (!isScrolling) {
                    mSuperLayout.performClick();
                }
                isScrolling = false;
                break;
        }
        return super.dispatchTouchEvent(event);

    }

    /**
     * 重写父类方法，使顶部也能虚化*/
    @Override
    protected float getTopFadingEdgeStrength() { return 1f; }
    /**
     * 重写父类方法，使底部也能虚化*/
    @Override
    protected float getBottomFadingEdgeStrength() { return 1f; }
}
