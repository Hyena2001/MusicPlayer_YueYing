package com.example.yueying2.custom;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.Nullable;

@SuppressLint("AppCompatCustomView")
public class MarqueeTextView extends TextView {
    private static final String TAG = "MarqueeTextView";
    /**
     * 文字长度
     */
    private float textLength = 0f;
    /**
     * 滚动条长度
     */
    private float viewWidth = 0f;
    /**
     * 文本x轴 的坐标
     */
    private float tx = 0f;
    /**
     * 文本Y轴的坐标
     */
    private float ty = 0f;
    /**
     * 文本当前长度
     */
    private float temp_tx1 = 0.0f;
    /**
     * 文本当前变换的长度
     */
    private float temp_tx2 = 0x0f;
    /**
     * 文本滚动开关
     */
    private boolean isStarting = false;
    /**
     * 画笔对象
     */
    private Paint paint = null;
    /**
     * 显示的文字
     */
    private String text = "";
    private WindowManager mManager;
    /**
     * 文本滚动速度
     **/
    private float speed;
    /**
     * 是否是第一次滚动，用于设置首次滚动的位置
     **/
    private boolean isFirstScroll = true;
    /**
     * 设置首次滚动的位置,第二次从最右边开始
     * FirstScroll = 3 首次滚动的位置从整个Text宽度的(从左到右) {FirstScroll}分之一处开始滚动
     **/
    private static final int FirstScroll = 3;
    private int width = 0 ,height = 0;

    public MarqueeTextView(Context context) {
        super(context);
        //Log.d(TAG, "MarqueeTextView: 0");
        this.setHorizontalFadingEdgeEnabled(true);
        this.setFadingEdgeLength(36);
    }

    public MarqueeTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        //Log.d(TAG, "MarqueeTextView: 1");
        this.setHorizontalFadingEdgeEnabled(true);
        this.setFadingEdgeLength(36);
    }

    public MarqueeTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        //Log.d(TAG, "MarqueeTextView: 2");
        this.setHorizontalFadingEdgeEnabled(true);
        this.setFadingEdgeLength(36);
    }
    /**
     * @param textSp 字体设置未多少sp
     * @return 返回以像素为单位的TextSize
     * sp 转化为 px*/
    private int getTextSpSize(int textSp){
        float v = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, textSp, getResources().getDisplayMetrics());
        //Log.d(TAG, "getTextSpSize: "+v);
        return v % (int)v < 0.5 ? (int)v : (int) v + 1;
    }
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        //此处不推荐获得本控件的宽高
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        //Log.d("滚动Text", "onDetachedFromWindow: ");
        if (paint != null) {
            paint.reset();
            paint = null;
        }
        if (mManager != null) mManager = null;
        text = null;
    }

    /**
     * 初始化自动滚动条,每次改变文字内容时，都需要重新初始化一次
     *
     * @param speed         文字滚动的速度
     */
    public void initScrollTextView(float speed) {

        this.speed = speed;
        textLength = paint.measureText(text);// 获得当前文本字符串长度
        viewWidth = this.getWidth();// 获取宽度return mRight - mLeft;

        if (viewWidth == 0) viewWidth = width >> 1;
        tx = textLength;
        temp_tx1 = viewWidth / FirstScroll + textLength;
        temp_tx2 = viewWidth / FirstScroll+ textLength * 2;// 自己定义，文字变化多少
        // // 文字的大小+距顶部的距离
        ty = this.getTextSize() + this.getPaddingTop();
    }

    /**
     * 开始滚动
     */
    public void starScroll() {
        if (!isFirstScroll) isFirstScroll = true;
        // 开始滚动
        isStarting = true;
        this.invalidate();// 刷新屏幕
    }

    /**
     * 停止方法,停止滚动
     */
    public void stopScroll() {
        if (isFirstScroll) isFirstScroll = false;
        // 停止滚动
        isStarting = false;
        this.invalidate();// 刷新屏幕
    }
    /**
     * 初始化滚动或者文本内容，根据文字的长度而定
     * @param marqueeText 判断 是否需要 横向滚动显示 的内容
     * 如果长度大于控件宽度width，那么选择滚动，没有达到就不滚动，需要自定义*/
    public void setText(String marqueeText) {
        //Log.d(TAG, "setText: "+marqueeText);
        if (text == null || !text.equals(marqueeText)) this.text = marqueeText;
        if(width == 0 || TextUtils.isEmpty(text)) return;//第一次绘制

        if (paint != null && width - 36 < paint.measureText(marqueeText)) {
            if (isStarting) stopScroll();
            super.setText("");
            initScrollTextView(2);
            starScroll();
        }else {
            stopScroll();
            super.setText(marqueeText);
        }
    }

    /**
     * 重写父类方法，如果文本滚动，父控件中文本缓存为空或者为上次不滚动的内容，不利于做一些判断*/
    @Override
    public String getText() {
        return text;
    }

    @Override
    protected float getLeftFadingEdgeStrength() {
        return 1f;
    }

    @Override
    protected float getRightFadingEdgeStrength() {
        return 1f;
    }

    /**
     * 重写onDraw方法
     */
    @Override
    protected void onDraw(Canvas canvas) {
        if (width == 0 || height == 0) {
            width = getMeasuredWidth();
            height = getMeasuredHeight();
            //Log.d(TAG, "onDraw: "+width+" "+height+", "+getTextSpSize(18));
            if (paint == null) {
                paint = this.getPaint();
                paint.setTextSize(getTextSpSize(18));
            }
            setText(text);
        }
        if (isStarting) {
            isFirstScroll = false;
            // A-Alpha透明度/R-Read红色/g-Green绿色/b-Blue蓝色
            //paint.setARGB(255, 200, 200, 200);
            canvas.drawText(text, temp_tx1 - tx, ty, paint);
            tx += speed;
            // 当文字滚动到屏幕的最左边
            if (tx > temp_tx2) {
                //Log.d("滚动Text", "onDraw: 滚动到最左边");
                // 把文字设置到最右边开始
                tx = textLength;
                if (!isFirstScroll) {
                    temp_tx1 = viewWidth + textLength;
                    temp_tx2 = viewWidth + textLength * 2;// 自己定义，文字变化多少
                }
            }
            this.invalidate();// 刷新屏幕
        }
        super.onDraw(canvas);
    }
}
