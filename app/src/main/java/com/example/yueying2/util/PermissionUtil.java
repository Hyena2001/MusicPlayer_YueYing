package com.example.yueying2.util;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;

/**
 * @author : 喜闻人籁
 * @since : 2021/3/8
 * 作用: 获取读写权限
 */
public class PermissionUtil {
    /*动态申请读、写权限*/
    public static final int REQUEST_PERMISSION_CODE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};

    /**
     * 获取权限
     * @param activity activity活动的上下文对象
     * 注释：从Android 6.0开始 谷歌要求 获取权限 必须动态获取，已达到手机用户自主选择的目的
     * */
    public static void getStorage(Activity activity) {
        /*动态获取存储权限的函数*/
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP){ //判断Android版本是否大于6.0 || 在API(26)以后规定必须要动态获取权限
            if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE)!=
                    PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity,PERMISSIONS_STORAGE,REQUEST_PERMISSION_CODE);
            }
        }
    }

    /**
     * 检查是否未获取权限
     * @return true 未获取权限
 *             false 已获取权限
     * */
    public static boolean IsPermissionNotObtained(Activity activity){
        return ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED; //判断是否已获取权限;
    }
}
