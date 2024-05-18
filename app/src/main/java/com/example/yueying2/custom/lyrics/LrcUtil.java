package com.example.yueying2.custom.lyrics;

import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author : 喜闻人籁
 * @since : 2021/6/25
 * 作用: 将歌词文件或者json数据解析成List<LrcBean>
 */
public class LrcUtil {

    private static final String TAG = "LrcUtil";

    public static List<LrcBean> getLocalLrc(String musicName){
        return parseStr2List(parseLrcFile(getLrcFilePath(musicName)));
    }

    /**
     * 传入的参数为标准歌词字符串
     * @param lrcStr 歌词字符串
     * @return List<LrcBean> 处理好的歌词集合
     */
    public static List<LrcBean> parseStr2List(String lrcStr) {
        if (TextUtils.isEmpty(lrcStr)) return null;
        List<LrcBean> list = new ArrayList<>();
        //1.更替转义字符
        String lrcText = lrcStr.replaceAll("&#58;", ":")
                .replaceAll("&#10;", "\n")
                .replaceAll("&#46;", ".")
                .replaceAll("&#32;", " ")
                .replaceAll("&#45;", "-")
                .replaceAll("&#13;", "\r")
                .replaceAll("&#39;", "'")
                .replaceAll("&nbsp;", " ")//空格替换
                .replaceAll("&apos;", "'")//分号替换
                .replaceAll("&&", "/")//空格替换
                .replaceAll("\\|", "/");

        //logJsonUtil.e("更替转义字符后",lrcStr);
        //2.更替转义字符后，将此字符串转换成字符数组，区分每个字符的边界是"\n"换行符
        String[] split = lrcText.split("\n");
        //boolean isWithTranslation = false;
        //3.根据定界正则表达式【换行符】为条件，转换成字符数组后，对每行字符串进行信息提炼（开始时间，每行歌词）
        for (int i = 0; i < split.length; i++) {
            String lrcInfo = split[i];
            //Log.d("未处理: 第"+i+"行", lrcInfo);
            if (" ".equals(lrcInfo) || TextUtils.isEmpty(lrcInfo)) continue;
            if (lrcInfo.contains("[ti:") || lrcInfo.contains("[ar:") || lrcInfo.contains("[offset:") ||
                    lrcInfo.contains("[al:") || lrcInfo.contains("[by:")) {
                continue;
            }
            String lrc = lrcInfo.substring(lrcInfo.indexOf("]") + 1);
            //如果该行文字为空或者1个空格符
            if (TextUtils.isEmpty(lrc) || " ".equals(lrc) || "//".equals(lrc)) continue;

           // Log.d(i+"行", lrc);
            //判断是否为翻译
            if (list.size() > 0 && !lrc.contains("by:")&& !lrc.contains("：")
                    && !lrc.contains("词：") && !lrc.contains("曲：")
                    && JudgingLanguage(list.get(list.size() - 1).getLrc(),lrc)) {
                //Log.d(i+"行", text);
                //isWithTranslation = true;
                list.get(list.size() - 1).setTranslateLrc(lrc);
                continue;
            }
            //解析当前行歌词信息
            String min = lrcInfo.substring(lrcInfo.indexOf("[")+ 1, lrcInfo.indexOf("[")+ 3);
            String seconds = lrcInfo.substring(lrcInfo.indexOf(":")+ 1, lrcInfo.indexOf(":") + 3);
            String mills = lrcInfo.substring(lrcInfo.indexOf(".") + 1, lrcInfo.indexOf(".") + 3);
            long startTime = Long.parseLong(min) * 60 * 1000 +   //解析分钟
                                Long.parseLong(seconds) * 1000 + //解析秒钟
                                Long.parseLong(mills)*10;  //解析毫秒
            if (list.size() > 1 && startTime < list.get(list.size() - 2).getStart())
                startTime = list.get(list.size() - 2).getStart() + 600;
            //处理完成，装载歌词
            LrcBean lrcBean = new LrcBean(lrc,startTime);
            list.add(lrcBean);
            //设置当前句歌词结束时间为下一句歌词开始时间
            if (list.size() > 1) list.get(list.size() - 2).setEnd(startTime);
            //如果是最后一句歌词，则设置结束时间为无限长，超过歌曲播放时长也可
            if (i == split.length - 1) list.get(list.size() - 1).setEnd(startTime + 100000);
        }
        return list;
    }
    /**
     * 判断两句歌词是否有翻译，分为第一句歌词str1，第二句歌词str2
     * @return true则代表str1为歌词，str2为翻译、歌词，其他皆为false
     * 判断思路：
     * 1.首先判断 str1 确定 str1NotChinese 的布尔值，
     * 判断逻辑为 含有日文及日文字符 或者 不含有中文及中文字符
     * 2.再判断 str2 确定 str2IsChinese 的布尔值
     * 判断逻辑为 不含有日文及日文字符 并且 含有中文及中文字符
     * 3.str1NotChinese 和 str2IsChinese 都为真时 确定此句为翻译
     * 处理好了如何确定是否带有翻译？ 随机取几处翻译看是否有值即可判定*/
    private static boolean JudgingLanguage(@NonNull String str1,@NonNull String str2){
        boolean str1NotChinese, str2IsChinese;
        //Log.d(TAG, "str1: "+str1+" ,\n"+str2);
        Pattern p = Pattern.compile("[\u4e00-\u9fa5]");//中文与中文字符
        Pattern r = Pattern.compile("[\u3040-\u309F\u30A0-\u30FF]");//日文字符
        //p.matcher(str1).find()  //含有中文
        str1NotChinese = r.matcher(str1).find() || !p.matcher(str1).find();
        str2IsChinese = !r.matcher(str2).find() && p.matcher(str2).find();
        //Log.d(TAG, str1NotChinese+", "+str2IsChinese+", "+p.matcher(str2).find());
        return str1NotChinese && str2IsChinese;
    }
    /**
     * 读取本地文件中的歌词
     * @param MusicPath 本地歌词文件的绝对路径
     * */
    public static String parseLrcFile(String MusicPath) {
        Log.d(TAG, "parseLrcFile: "+MusicPath);
        if (TextUtils.isEmpty(MusicPath) || !MusicPath.contains(".lrc")) return null;
        System.out.println("你的歌词存储路径--------->"+MusicPath);

        File file = new File(MusicPath);
        FileInputStream fis = null;
        BufferedInputStream bis = null;
        BufferedReader reader;
        StringBuilder LrcStr = new StringBuilder();
        try {
            fis = new FileInputStream(file);
            bis = new BufferedInputStream(fis);
            //首先确定文件的文本编码类型，"utf-8"或者"gbk"不然乱码
            reader = new BufferedReader(new InputStreamReader(bis, StandardCharsets.UTF_8));
            String str = reader.readLine();
            while (str != null) {
                LrcStr.append(str).append("\n");
                str = reader.readLine();
            }//logJsonUtil.e("LrcUtil",LrcStr);
        } catch (IOException e) {
            return null;
        }finally {
            if (fis != null) {
                try {
                    fis.close();
                    if(bis != null) bis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return LrcStr.toString();
    }

    /**
     * @param musicFileName "歌名 - 歌手"
     * @return String 存在歌词文件则返回其绝对路径，否则返回空
     * */
    public static String getLrcFilePath(String musicFileName){

        if (musicFileName == null || TextUtils.isEmpty(musicFileName)) {
            return null;
        }

        String absPath = getLocalPathPictureCaches(musicFileName);

        Log.e(TAG, "getLrcFilePath: "+absPath+", 是否存在 "+FileExists(absPath));

        return FileExists(absPath) ? absPath : null;
    }

    /** 获取Picture文件夹的绝对路径*/
    public static String getLocalPathPictures(String fileName){
        fileName = fileName.replaceAll("/","&");
        String absPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)+"/";
        return absPath+fileName;
    }
    /** 获取Picture文件夹的绝对路径*/
    public static String getLocalPathPictureCaches(String fileName){
        fileName = fileName.replaceAll("/","&");
        String absPath = Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)+"/caches/lrc/";
        return absPath+fileName;
    }
    /** 判断该路径的文件是否存在*/
    public static boolean FileExists(String targetFileAbsPath){
        try {
            File f = new File(targetFileAbsPath);
            if(!f.exists()) return false;
        } catch (Exception e) {
            return false;
        }
        return true;
    }
}
