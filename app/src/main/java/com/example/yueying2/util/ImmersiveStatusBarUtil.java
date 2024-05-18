package com.example.yueying2.util;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

/**
 * @author : Bilibili喜闻人籁
 * @since : 2020/12/1
 */

public class ImmersiveStatusBarUtil {
    private static final String TAG = "ImmersiveStatusBarUtil";
    /**
     * description 将状态栏设置为全透明
     * @param activity activity的上下文对象
     * @param IS_HIDE_NAVIGATION 是否隐藏底部导航栏(虚拟按键)
     * @author Alonso
     * time 2020/12/1 19:50
     **/
    @TargetApi(19)
    public static void transparentBar(Activity activity, boolean IS_HIDE_NAVIGATION) {

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {//高于Android4.4版本
            //logJsonUtil.e("StatusBar","高于4.0");
            Window window = activity.getWindow();
            //WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS 窗口标志：请求提供最小系统的半透明状态栏
            //WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION  窗口标志：请求提供最小系统的半透明导航栏
            //clearFlags()和addFlags()--->setFlags()
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                    | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);//清除标志
            if (IS_HIDE_NAVIGATION) {
                window.getDecorView().setSystemUiVisibility(
                                      View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |   //请求系统暂时隐藏导航栏（navigation bar）
                                      View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN  //全屏
                                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY   //如果不设置此标志，则任何用户皆可交互
                                                            );
            }else {
                window.getDecorView().setSystemUiVisibility(
                                //View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |   //请求系统暂时隐藏导航栏（navigation bar）
                                  View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN  //全屏
                                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY   //如果不设置此标志，则任何用户皆可交互
                                                            );
            }
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);//请求提供最小半透明状态栏
            //给导航栏、状态栏设置透明颜色，以保护全局背景不被隔断、剪切、显示不美观
            window.setStatusBarColor(Color.TRANSPARENT);
            window.setNavigationBarColor(Color.TRANSPARENT);
        }else {
            Toast.makeText(activity,"系统未提供透明信息栏方法", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 谷歌原生修改状态栏字体颜色
     * @param activity activity 上下文对象
     * @param dark 是否设置成黑色字体
     */
    public static void setAndroidNativeLightStatusBar(Activity activity, boolean dark) {
        View decor = activity.getWindow().getDecorView();
        if (dark) {
            decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        } else {
            decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
    }
    /**
     * 隐藏界面打开的软键盘*/
    public static void HideSoftInput(Activity activity){
        if (activity == null) return;
        Log.d(TAG, "HideSoftInput: ");
        InputMethodManager imm =
                (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm.isActive() && activity.getCurrentFocus() != null) {
            if (activity.getCurrentFocus().getWindowToken() != null) {
                imm.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);
            }
        }
    }
    public static void ShowSoftInput(Application application, View v){
        if (application == null || v == null) return;
        InputMethodManager imm =
                (InputMethodManager) application.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(v,InputMethodManager.SHOW_IMPLICIT);
    }
}
