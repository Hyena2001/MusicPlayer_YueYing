package com.example.yueying2.util;

import android.annotation.TargetApi;
import android.app.Application;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import com.example.yueying2.R;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;


/**
 * @author : 喜闻人籁
 * @since : 2021/4/6
 * 作用: 图片（Bitmap）处理工具类
 *              1.Bitmap图片高斯模糊 
 *              2.Bitmap图片裁剪 
 *              3.图片文件保存到相册 。。。
 */
public class PictureUtil {

    /**
     * 1.Bitmap高斯模糊
     * @deprecated 从 Android 12 开始，RenderScript API 已被弃用。
     * 它们将继续正常运行，但我们预计设备和组件制造商会逐渐停止提供GPU硬件加速支持。
     * https://developer.android.google.cn/guide/topics/renderscript/migrate
     * 建议Android 10以下使用，Android 10+则使用Vulkan
     * @param application 上下文对象
     * @param image   需要模糊的图片
     * @param blurRadius 设置渲染的模糊程度, 25f是最大模糊度,越大越模糊
     * @return 模糊处理后的Bitmap
     */
    @Deprecated
    public static Bitmap blurBitmap(Application application, Bitmap image, float blurRadius) {
        float BITMAP_SCALE = 0.16f;// 图片缩放比例
        // 计算图片缩小后的长宽
        int width = Math.round(image.getWidth() * BITMAP_SCALE);
        int height = Math.round(image.getHeight() * BITMAP_SCALE);

        // 将缩小后的图片做为预渲染的图片
        Bitmap inputBitmap = getItWeakReference(
                Bitmap.createScaledBitmap(image, width, height, false));
        // 创建一张渲染后的输出图片
        Bitmap outputBitmap = getItWeakReference(Bitmap.createBitmap(inputBitmap));
        // 创建RenderScript内核对象
        RenderScript rs = getItWeakReference(RenderScript.create(application));
        // 创建一个模糊效果的RenderScript的工具对象
        ScriptIntrinsicBlur blurScript = getItWeakReference(
                                    ScriptIntrinsicBlur.create(rs, Element.U8_4(rs)));
        // 由于RenderScript并没有使用VM来分配内存,所以需要使用Allocation类来创建和分配内存空间
        // 创建Allocation对象的时候其实内存是空的,需要使用copyTo()将数据填充进去
        Allocation tmpIn = getItWeakReference(Allocation.createFromBitmap(rs, inputBitmap));
        Allocation tmpOut = getItWeakReference(Allocation.createFromBitmap(rs, outputBitmap));
        // 设置渲染的模糊程度, 25f是最大模糊度
        blurScript.setRadius(blurRadius);
        // 设置blurScript对象的输入内存
        blurScript.setInput(tmpIn);
        // 将输出数据保存到输出内存中
        blurScript.forEach(tmpOut);
        // 将数据填充到Allocation中
        tmpOut.copyTo(outputBitmap);
        //释放资源：RenderScript内核对象
        tmpIn.destroy();
        tmpOut.destroy();
        blurScript.destroy();
        rs.destroy();
        System.gc();
        return getItWeakReference(outputBitmap);
    }
    @TargetApi(Build.VERSION_CODES.S)
    public static RenderEffect blurBitmapEffect(Application application, Bitmap image, float blurRadius) {
        RenderEffect effect = RenderEffect.createBlurEffect(blurRadius, blurRadius,
                RenderEffect.createBitmapEffect(image), Shader.TileMode.DECAL);
        return getItWeakReference(effect);
    }
    /**
     * 2.Bitmap图片裁剪,剪切专辑图片后得到Bitmap
     * @param bitmap 要从中截图的原始位图
     * @param x 宽与高的比值，确定横屏还是竖屏
     * @return 返回一个剪切好的Bitmap
     * @author 12453
     * date 2020/12/11 16:58
     **/
    public static Bitmap imageCropWithRect(Bitmap bitmap, float x) {
        if (bitmap == null) return null;
        // 得到图片的宽，高    [诀窍: 竖屏取中间，横屏取上边]
        boolean isLandScreen = x > 1;
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        int retX, retY,nw,nh;
        if (w > h) {//Log.d(TAG, "imageCropWithRect: W > H");
            nw = h / 2;
            nh = h;
            retX = (w - nw) / 2;
            retY = 0;
        } else {//Log.d(TAG, "imageCropWithRect: W <=Hm , w= "+w+", h= "+h+" , 比例 "+x);
            retX = isLandScreen ? 0 : (int) ((w - h * x) / 2);
            retY = 0;
            nw = isLandScreen ? w - retX : (int) (h * x);
            nh = (int) (isLandScreen ? nw / x : h);
        }
        //Log.d(TAG, "imageCropWithRect: "+retX+", "+retY+", "+nw+", "+nh);
        return getItWeakReference(
                Bitmap.createBitmap(bitmap, retX, retY, nw, nh, null, false));
    }
    /**
     * 3.图片文件保存到相册,Api>=29时调用，保存图片至相册
     * @param application 上下文
     * @param bitmap 位图对象
     * @param displayName 文件名
     * @param mineType 图片格式说明
     * @param compressFormat 生成的图片格式
     * @author 12453
     * date 2020/12/31 15:46
     **/
    public static void addBitmapToAlbum(Application application,//上下文
                                        Bitmap bitmap,//位图对象
                                        String displayName,//文件名
                                        String mineType,//图片格式说明
                                        Bitmap.CompressFormat compressFormat){//生成的图片格式

        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME,displayName);
        values.put(MediaStore.MediaColumns.MIME_TYPE,mineType);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/img");
        }else {
            values.put(MediaStore.MediaColumns.DATA, Environment.getExternalStorageDirectory().getPath()
                    +"/"+Environment.DIRECTORY_DCIM+displayName);
        }

        ContentResolver resolver = application.getContentResolver();
        Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        values.clear();
        Log.d("图片保存路径", ""+uri);
        if (uri != null) {
            FileOutputStream outputStream;
            try {
                outputStream = (FileOutputStream) resolver.openOutputStream(uri);
                bitmap.compress(compressFormat,100,outputStream);//png格式
                if (outputStream != null) {
                    outputStream.flush();
                    outputStream.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    /**
     * @param bm 原Bitmap位图对象
     * @param newWidth 新宽度
     * @param newHeight 新高度
     * @return 获得任意宽高的bitmap
     * */
    public static Bitmap zoomImg(Bitmap bm, int newWidth, int newHeight) {
        // 获得图片的宽高
        int width = bm.getWidth();
        int height = bm.getHeight();
        // 计算缩放比例
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // 取得想要缩放的matrix参数
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);

        // 得到新的图片
        return getItWeakReference(
                Bitmap.createBitmap(bm, 0, 0, width, height, matrix, true));
    }
    /** 创建一个圆形的图片drawable
     *@param application 单例持有的上下文对象
     * @param bitmap 位图对象，通过各种途径 获得
     * @param bitmapWidth 需要创建多大的Bitmap，设置其宽度
     * @param bitmapHeight 需要创建多大的Bitmap，设置其高度
     * @return 返回一个圆形的RoundedBitmapDrawable
     * */
    public static RoundedBitmapDrawable createCircleDrawable(Application application,Bitmap bitmap,
                                                             int bitmapWidth, int bitmapHeight){
        if (application == null || bitmap == null) return null;

        bitmap = zoomImg(bitmap,bitmapWidth,bitmapHeight);
        RoundedBitmapDrawable drawable = RoundedBitmapDrawableFactory.create(application.getResources(), bitmap);
        drawable.setCircular(true);
        return drawable;
    }
    /** 创建一个圆形、纯色包边的图层叠加drawable专辑唱片 - LayerDrawable
     * @param application 单例持有的上下文对象
     * @param bitmap 位图对象，通过各种途径 获得
     * @param bitmapSize 需要创建多大的Bitmap，设置其宽高大小
     * @param stokeWidth drawable包边宽度
     * @param color int型的颜色，使用源文件values/color.xml 或者 {@link Color#argb(int, int, int, int)} 获得
     * @return 返回一个圆形的RoundedBitmapDrawable
     * <a href>https://blog.csdn.net/zhangphil/article/details/52045404</a>
     * */
    public static LayerDrawable createCircleDrawable(Application application, Bitmap bitmap,
                                                     float viewSize, int bitmapSize,
                                                     int stokeWidth, @ColorInt int color){
        if (application == null) return null;
        Resources resources = application.getResources();
        if (bitmap == null) bitmap = BitmapFactory.decodeResource(resources, R.drawable.icon_fate);
        Drawable[] layers = new Drawable[3];
        //最外部的半透明边线
        OvalShape ovalShape0 = new OvalShape();
        ShapeDrawable drawable0 = new ShapeDrawable(ovalShape0);
        drawable0.getPaint().setColor(color);
        drawable0.getPaint().setStyle(Paint.Style.FILL);
        drawable0.getPaint().setAntiAlias(true);
        layers[0] = drawable0;

        Bitmap record = BitmapFactory.decodeResource(resources, R.drawable.iv_record_128);
        RoundedBitmapDrawable drawable1 = RoundedBitmapDrawableFactory.create(resources, record);
        drawable1.setCircular(true);
        layers[1] = drawable1;

        bitmap = zoomImg(bitmap,bitmapSize,bitmapSize);
        RoundedBitmapDrawable drawable2 = RoundedBitmapDrawableFactory.create(resources, bitmap);
        drawable2.setCircular(true);
        layers[2] = drawable2;

        int recordSize = (int) (viewSize * 0.21);
        LayerDrawable layerDrawable = new LayerDrawable(layers);
        layerDrawable.setLayerInset(0,0,0,0,0);
        layerDrawable.setLayerInset(1,stokeWidth,stokeWidth,stokeWidth,stokeWidth);
        layerDrawable.setLayerInset(2,recordSize,recordSize,recordSize,recordSize);
        return new WeakReference<>(layerDrawable).get();
    }

    public static LayerDrawable createCircleDrawableBig(Application application, Bitmap target, int size,
                                                                       int viewSize){

        if (application == null) return null;
        Resources resources = application.getResources();
        if (target == null)
            target = getItWeakReference(BitmapFactory.decodeResource(resources,R.drawable.icon_fate));
        Drawable[] layers = new Drawable[2];

        Bitmap record = getItWeakReference(
                BitmapFactory.decodeResource(resources, R.drawable.iv_record));
        RoundedBitmapDrawable drawable1 = getItWeakReference(RoundedBitmapDrawableFactory
                .create(resources, zoomImg(record,400,400)));
        drawable1.setCircular(true);
        layers[0] = getItWeakReference(drawable1);

        RoundedBitmapDrawable drawable2 = getItWeakReference(RoundedBitmapDrawableFactory
                .create(resources, zoomImg(target,200,200)));
        //drawable2.setAntiAlias(true);
        drawable2.setCircular(true);

        layers[1] =getItWeakReference(drawable2);
        LayerDrawable layerDrawable = getItWeakReference(new LayerDrawable(layers));
        int insetWidth = (int) (viewSize * 0.084);
        //针对每一个图层进行填充，使得各个圆环之间相互有间隔，否则就重合成一个了。
        //Log.d(TAG, "setImageBitmap: "+getMeasuredWidth());
        layerDrawable.setLayerInset(0, 0, 0 , 0 , 0);
        layerDrawable.setLayerInset(1, insetWidth, insetWidth, insetWidth, insetWidth);

        return new WeakReference<>(layerDrawable).get();
    }
    /** 创建一个圆形的纯色drawable
     * @param context 单例持有的上下文对象
     * @param color int型的颜色，使用源文件values/color.xml 或者 {@link Color#argb(int, int, int, int)} 获得
     * @param bitmapWidth 需要创建多大的Bitmap，设置其宽度
     * @param bitmapHeight 需要创建多大的Bitmap，设置其高度
     * @return 返回一个圆形、纯色的RoundedBitmapDrawable
     * <a href>https://blog.csdn.net/u010054982/article/details/52487599?utm_medium=distribute.pc_relevant.none-task-blog-2%7Edefault%7EBlogCommendFromMachineLearnPai2%7Edefault-1.control&dist_request_id=1619603308189_75864&depth_1-utm_source=distribute.pc_relevant.none-task-blog-2%7Edefault%7EBlogCommendFromMachineLearnPai2%7Edefault-1.control</a>
     * */
    public static RoundedBitmapDrawable createColorDrawable(Context context, @ColorInt int color,
                                                            int bitmapWidth, int bitmapHeight){
        if (context == null) return null;
        context = context.getApplicationContext();
        if(color == 0){ color = Color.parseColor("#EEEEEE"); }
        Bitmap colorBitmap = Bitmap.createBitmap(bitmapWidth,bitmapHeight, Bitmap.Config.ARGB_8888);
        colorBitmap.eraseColor(color);
        RoundedBitmapDrawable drawable =
                RoundedBitmapDrawableFactory.create(context.getResources(), colorBitmap);
        drawable.setCircular(true);
        return new WeakReference<>(drawable).get();
    }

    public static LayerDrawable createBlurDrawable(Application application, float width, int height,
                                                                  float blur, Bitmap targetBitmap){
        if (application == null || width <= 0 || height <= 0) return null;
        Resources resources = application.getResources();
        if (targetBitmap == null)
            targetBitmap = BitmapFactory.decodeResource(resources,R.drawable.icon_fate);

        Drawable[] layers = new Drawable[2];

        Bitmap screenshot = imageCropWithRect(targetBitmap,width / height);
        screenshot = zoomImg(screenshot, 500,500);

//        screenshot = FastBlur.blur(screenshot, 45, true);

        layers[0] = new BitmapDrawable(resources, screenshot);


        Bitmap maskBitmap = Bitmap.createBitmap(100,100,Bitmap.Config.ARGB_8888);
        maskBitmap.eraseColor(Color.argb(100,0,0,0));

        layers[1] = new BitmapDrawable(resources, maskBitmap);

        LayerDrawable layerDrawable = new LayerDrawable(layers);
        layerDrawable.setLayerInset(0,0,0,0,0);
        layerDrawable.setLayerInset(1,0,0,0,0);
        //return new BitmapDrawable(context.getResources(),screenshot);
        return new WeakReference<>(layerDrawable).get();
    }
    /*返回一个带白色圆边的头像Icon*/
    public static LayerDrawable createUserIconDrawable(Application application, Bitmap target, int size,
                                                        int viewSize){

        if (application == null) return null;
        Resources resources = application.getResources();
        if (target == null)
            target = getItWeakReference(BitmapFactory.decodeResource(resources,R.drawable.icon_fate));
        Drawable[] layers = new Drawable[2];

        RoundedBitmapDrawable colorBg = getItWeakReference(
                createColorDrawable(application,Color.parseColor("#28EEEEEE"),
                                            120,120));

        layers[0] = getItWeakReference(colorBg);

        RoundedBitmapDrawable drawable2 = getItWeakReference(RoundedBitmapDrawableFactory
                .create(resources, zoomImg(target,200,200)));
        //drawable2.setAntiAlias(true);
        drawable2.setCircular(true);

        layers[1] =getItWeakReference(drawable2);
        LayerDrawable layerDrawable = getItWeakReference(new LayerDrawable(layers));
        int insetWidth = (int) (viewSize * 0.05);
        //针对每一个图层进行填充，使得各个圆环之间相互有间隔，否则就重合成一个了。
        //Log.d(TAG, "setImageBitmap: "+getMeasuredWidth());
        layerDrawable.setLayerInset(0, 0, 0 , 0 , 0);
        layerDrawable.setLayerInset(1, insetWidth, insetWidth, insetWidth, insetWidth);

        return new WeakReference<>(layerDrawable).get();
    }
    public static Bitmap getResIdBitmap(@DrawableRes int resId,int size,Resources resources,int roundCorner){
        if (resources == null || size <= 0 || size > 800 || roundCorner < 0 || roundCorner > 360) return null;
        Drawable drawable = ResourcesCompat.getDrawable(resources, resId, null);
        if (drawable != null) {
            Bitmap bitmap = zoomImg(drawableToBitmap(drawable),size,size);
            if (roundCorner > 0){
                RoundedBitmapDrawable drawable1 = RoundedBitmapDrawableFactory.create(resources, bitmap);
                drawable1.setCornerRadius(roundCorner);
                bitmap = drawableToBitmap(drawable1);
            }
            return bitmap;
        }
        return null;
    }
    public static Bitmap getResIdBitmap(Bitmap bitmap,int size,Resources resources,int roundCorner){
        if (resources == null || size <= 0 || size > 800 || roundCorner < 0 || roundCorner > 360) return null;

        bitmap = zoomImg(bitmap,size,size);
        if (roundCorner > 0){
            RoundedBitmapDrawable drawable = RoundedBitmapDrawableFactory.create(resources, bitmap);
            drawable.setCornerRadius(roundCorner);
            bitmap = drawableToBitmap(drawable);
        }
        return bitmap;
    }
    /**
     * 将Bitmap转成本地图片
     * @param bitmap 已有网络图片
     * @param targetPath 保存的本地路径
     */
    public static void SaveBitmapCache(Bitmap bitmap, String targetPath){
        if (bitmap == null) return;
        if (TextUtils.isEmpty(targetPath)) return;
        try {
            File file = new File(targetPath);
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            Log.e("PictureUtil","Bitmap保存失败 "+e);
        }
    }

    /**
     * Drawable转换成一个Bitmap
     *
     * @param drawable drawable对象
     * @return bitmap
     */
    public static Bitmap drawableToBitmap(Drawable drawable) {
        Bitmap bitmap = Bitmap.createBitmap( drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(),
                drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    public static <T> T getItWeakReference(T obj){
        return obj == null ? null : new WeakReference<>(obj).get();
    }
}
