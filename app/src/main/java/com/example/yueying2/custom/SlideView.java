package com.example.yueying2.custom;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.yueying2.R;


/**
 * @author : 喜闻人籁
 * @since : 2021/3/18
 * 作用: 跟随手指滑动，而关闭的透明样式的Activity | ViewGroup
 */
public class SlideView {
    private MySlideView mSlideView;
    private static final String SLIDE_DIRECTION_LEFT = "SlideView_left";
    private static final String SLIDE_DIRECTION_RIGHT = "SlideView_right";
    private static final String SLIDE_DIRECTION_UP = "SlideView_up";
    public static final String SLIDE_DIRECTION_DOWN = "SlideView_down";

    public SlideView(Activity activity, String slideDirection) {
        mSlideView = new MySlideView(activity);
        mSlideView.setActivity(activity,slideDirection);
    }

    public void setNoScrollEvent(boolean noScrollEvent) {
        if (mSlideView != null) mSlideView.setNoScrollEvent(noScrollEvent);
    }

    public void onDestroy(){
        if (mSlideView != null) { mSlideView.release(); mSlideView = null; }
    }

    /**
     * 核心的View 提供activity滑动是否关闭，手势监听， 触摸事件是否消费
     * */
    public class MySlideView extends FrameLayout {

        public MySlideView(@NonNull Context context) {
            super(context);
            touchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
            setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        }

        public MySlideView(@NonNull Context context, @Nullable AttributeSet attrs) {
            super(context, attrs);
        }

        public MySlideView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
        }

        private final String TAG = MySlideView.class.getSimpleName();
        private Activity activity;// 绑定的Activity
        //private ShowCoverActivity mShowCoverActivity;
        private ViewGroup decorView;
        private View contentView;// activity的ContentView
        private float intercept_X = 0;// onInterceptTouchEvent刚触摸时的X坐标
        private float intercept_Y = 0,contentViewY_last,moveY_last;// onInterceptTouchEvent手指刚触摸时的y坐标
        private int touchSlop = 0,closedArea = 3;// 产生滑动的最小值,关闭区域默认3，当滑动到至少屏幕高度三分之一后松手关闭activity
        private int /*mPhoneWidth,*/mPhoneHeight;
        private String direction; //滑动方向
        private boolean isVerticalScroll, NoScrollEvent = true;//初始不可滑动关闭

        @Override
        protected void onConfigurationChanged(Configuration newConfig) {
            super.onConfigurationChanged(newConfig);
            NoScrollEvent = true;
        }

        @Override
        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            //if (mShowCoverActivity != null) mShowCoverActivity = null;
            release();
        }
        public void release(){
            if (activity != null) activity = null;
            if (decorView != null) decorView = null;
            if (contentView != null) contentView = null;
            if (direction != null) direction = null;
        }

        public void setNoScrollEvent(boolean noScrollEvent) {
            this.NoScrollEvent = noScrollEvent;
        }

        /**
         * 绑定Activity
         * @param activity 需要滑动关闭的Activity对象
         * @param direction activity滑动关闭的方向
         */
        public void setActivity(Activity activity, String direction) {
            //if (activity instanceof ShowCoverActivity) this.mShowCoverActivity = (ShowCoverActivity) activity;
            this.activity = activity;
            this.direction = direction;
            isVerticalScroll = direction.equals(SLIDE_DIRECTION_UP) || direction.equals(SLIDE_DIRECTION_DOWN);
            //Log.d(TAG, "setActivity: "+isVerticalScroll);
            initCoverView();
            //Log.d(TAG, "showCoverActivity 是否等于空"+(mShowCoverActivity == null));
        }
        /**
         * 将contentView从DecorView中移除，并添加到CoverView中，最后再将CoverView添加到DecorView中
         */
        private void initCoverView() {
            decorView = (ViewGroup) activity.getWindow().getDecorView();
            //if (mShowCoverActivity != null) decorView.setBackgroundColor(Color.parseColor("#000000"));
            contentView = decorView.findViewById(android.R.id.content);
            ViewGroup contentParent = (ViewGroup) contentView.getParent();
            contentParent.removeView(contentView);
            addView(contentView);
            contentView.setBackgroundColor(Color.TRANSPARENT);
            contentParent.addView(this);
            //
            DisplayMetrics dm = new DisplayMetrics();
            activity.getWindow().getWindowManager().getDefaultDisplay().getMetrics(dm);
            //mPhoneWidth = dm.widthPixels;
            mPhoneHeight = dm.heightPixels;
            if(dm.heightPixels >= 1600) closedArea = 4;
            //Log.d(TAG, "initCoverView: "+/*mPhoneWidth+*/" "+mPhoneHeight+" "+closedArea);
        }

        @Override
        public boolean onInterceptTouchEvent(MotionEvent ev) {
            return shouldInterceptEvent(ev);
        }
        /**
         * 判断是否应该拦截事件。
         * 如果水平方向的偏移量(不取绝对值) > 垂直方向的偏移量(取绝对值)，并且水平方向的偏移量大于最小滑动距离，我们将拦截事件。
         * 【实际过程中，我们发现touchSlope还是偏小，所以取了其3倍的数值作为最小滑动距离】
         * @param event 事件对象
         * @return true表示拦截，false反之
         */
        private boolean shouldInterceptEvent(MotionEvent event) {
            boolean shouldInterceptEvent = false;
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    intercept_X = event.getX();
                    intercept_Y = event.getY();
                    //Log.d(TAG, "shouldInterceptEvent: "+intercept_Y);
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (NoScrollEvent) return false;
                    Log.d(TAG, "shouldInterceptEvent: ");
                    float offsetY = Math.abs(event.getY() - intercept_Y);
                    float offsetX = Math.abs(event.getX() - intercept_X);
                    if ((isVerticalScroll ? offsetY : offsetX) < touchSlop || (isVerticalScroll ? offsetX > offsetY : offsetY > offsetX)) {
                        shouldInterceptEvent = false;
                    } else if ((isVerticalScroll ? offsetY : offsetX) >= touchSlop) {
                        shouldInterceptEvent = true;
                    } else {
                        shouldInterceptEvent = false;
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    shouldInterceptEvent = false;
                    NoScrollEvent = true;
                    break;
                default:
                    break;
            }
            Log.d(TAG, "shouldInterceptEvent: "+shouldInterceptEvent);
            return shouldInterceptEvent;
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouchEvent(MotionEvent event) {
            processTouchEvent(event);
            return true;
        }
        /**
         * 对onTouchEvent事件进行处理
         * @param event 事件对象
         **/
        private void processTouchEvent(MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    //Log.d(TAG, "processTouchEvent: ");
                    break;
                case MotionEvent.ACTION_MOVE:
                    float offsetX = event.getX() - intercept_X;
                    float offsetY = event.getY() - intercept_Y;
                    /*if (mShowCoverActivity != null ) {
                        //Log.d(TAG, "offsetX = "+offsetX+", offsetY = "+offsetY)+" moveLastY = "+moveY_last);
                        //Log.d(TAG, " contentView.getY() = "+contentView.getY()+" contentViewY_last = "+contentViewY_last);
                        //确定触摸滑动方向
                        if(contentView.getY() == contentViewY_last){
                            if(offsetY <= moveY_last) direction = SLIDE_DIRECTION_UP;
                            else direction = SLIDE_DIRECTION_DOWN;
                            moveY_last = event.getY() - intercept_Y;
                        }//Log.d(TAG, "确定滑动方向：  "+direction);
                        contentViewY_last = contentView.getY();
                        mShowCoverActivity.hideSaveButton();
                    }*/
                    switch (direction) {
                        case SLIDE_DIRECTION_RIGHT:
                            //如果是向右滑动，我们动态改变contentView的偏移值
                            if (offsetX > 0) {
                                contentView.setTranslationX(offsetX);
                            }
                            break;
                        case SLIDE_DIRECTION_LEFT:
                            if (offsetX < 0) {
                                contentView.setTranslationX(offsetX);
                            }
                            break;
                        case SLIDE_DIRECTION_UP:
                            //如果是向下滑动，我们动态改变contentView的偏移值
                            if (offsetY < 0) {
                                contentView.setTranslationY(offsetY);
                            }
                            break;
                        case SLIDE_DIRECTION_DOWN:
                            //如果是向下滑动，我们动态改变contentView的偏移值
                            if (offsetY > 0) {
                                contentView.setTranslationY(offsetY);
                            }
                            break;
                    }
                    /*if (mShowCoverActivity != null)
                        setDecorViewBgAlpha(contentView.getY() > 0 ? contentView.getY() : 0 - contentView.getY());*/
                    //Log.d(TAG, "processTouchEvent: "+contentView.getTranslationY());
                    break;

                case MotionEvent.ACTION_UP:
                    //如果手释放时是在屏幕的1/3之内，我们视为用户不想关闭Activity，则弹回。反之，关闭
                    switch (direction) {
                        case SLIDE_DIRECTION_RIGHT:
                             if (contentView.getTranslationX() >= (float) contentView.getMeasuredWidth() / closedArea) {
                                 collapse();
                             } else open();
                            break;
                        case SLIDE_DIRECTION_LEFT:
                            if (contentView.getTranslationX() <= 0 - (float) contentView.getMeasuredWidth() / closedArea) {
                                collapse();
                            } else open();
                            break;
                        case SLIDE_DIRECTION_UP:
                            if (contentView.getTranslationY() <= 0 - (float) contentView.getMeasuredHeight() / closedArea) {
                                collapse();
                            } else open();
                            break;
                        case SLIDE_DIRECTION_DOWN:
                            if (contentView.getTranslationY() >= (float) contentView.getMeasuredHeight() / closedArea) {
                                collapse();
                            } else open();
                            break;
                    }
                    break;

                default:
                    break;
            }
        }
        /**
         * 展开Activity
         */
        private void open() {
            contentView.clearAnimation();
            ObjectAnimator anim = null;
            switch (direction) {
                case SLIDE_DIRECTION_RIGHT:
                case SLIDE_DIRECTION_LEFT:
                    anim = ObjectAnimator.ofFloat(contentView, View.TRANSLATION_X, 0);
                    break;
                case SLIDE_DIRECTION_UP:
                case SLIDE_DIRECTION_DOWN:
                    anim = ObjectAnimator.ofFloat(contentView, View.TRANSLATION_Y, 0);
                    break;
            }
            if (anim != null) {
                /*if (mShowCoverActivity != null) {
                    anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(ValueAnimator animation) {
                            //Log.d(TAG, "onAnimationUpdate: "+animation.getAnimatedValue());
                            float y = (float) animation.getAnimatedValue();
                            setDecorViewBgAlpha( y < 0 ? 0 - y : y);
                        }
                    });
                    anim.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            contentViewY_last = 0;
                            moveY_last = 0;
                            mShowCoverActivity.showSaveButton();
                            //Log.d(TAG, "Activity展开 AnimationEnd: ");
                        }
                    });
                }*/
                anim.start();
            }
        }

        /**
         * 折叠Activity(finish掉)
         */
        private void collapse() {
            contentView.clearAnimation();
            ObjectAnimator anim = null;
            switch (direction) {
                case SLIDE_DIRECTION_RIGHT:
                    anim = ObjectAnimator.ofFloat(contentView, View.TRANSLATION_X, contentView.getMeasuredWidth());
                    break;
                case SLIDE_DIRECTION_LEFT:
                    anim = ObjectAnimator.ofFloat(contentView, View.TRANSLATION_X, -contentView.getMeasuredWidth());
                    break;
                case SLIDE_DIRECTION_UP:
                    anim = ObjectAnimator.ofFloat(contentView, View.TRANSLATION_Y, -contentView.getMeasuredHeight());
                    break;
                case SLIDE_DIRECTION_DOWN:
                    anim = ObjectAnimator.ofFloat(contentView, View.TRANSLATION_Y, contentView.getMeasuredHeight());
                    break;
            }
            if (anim != null) {
                anim.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if(activity != null){
                            activity.finish();
                            activity.overridePendingTransition(0, R.style.noAnimation);
                        }
                        //Log.d(TAG, "onAnimationEnd: ");
                    }
                });
                anim.setDuration(100);//设置滑动动画的演示时长
                anim.start();
            }
        }
        /**
         * 根据屏幕Y轴移动距离 设置背景黑色的透明度
         * @param  y contentView 上下滑动时与屏幕顶部的绝对值
         * */
        //当透明Activity 是{@link ShowCoverActivity}时调用
        private void setDecorViewBgAlpha(float y){
            if (decorView == null || mPhoneHeight <= 0 || y > mPhoneHeight) return;
            float ySum = mPhoneHeight >> 2;
            int AlphaMultiple = (int)( (ySum - y) / ySum * 256 );
            if(AlphaMultiple < 76) AlphaMultiple = 76;// 256 * 0.3 = 76.8
            /* //Log.d(TAG, "setDimAmount: "+Integer.toHexString(AlphaMultiple).toUpperCase());
            * Integer.toHexString(int) : 2进制转16进制(小写)
            * String.totoUpperCase() ：有小写字母就转化成大写字母*/
            if (AlphaMultiple < 256)
                decorView.setBackgroundColor(Color.parseColor("#"+Integer.toHexString(AlphaMultiple).toUpperCase()+"000000"));
            else decorView.setBackgroundColor(Color.parseColor("#000000"));

            /*if (mShowCoverActivity != null){
                float multiple = (ySum - y) / ySum;
                multiple = Float.parseFloat(String.format(Locale.CHINA,"%.2f",multiple));
                mShowCoverActivity.dynamicCover(multiple);
            }*/
        }
    }
}
