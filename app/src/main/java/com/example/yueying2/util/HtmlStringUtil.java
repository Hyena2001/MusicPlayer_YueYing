package com.example.yueying2.util;

import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;

/**
 * @author : Bilibili喜闻人籁
 * @since : 2020/12/17
 * Function:  返回各种样式的字符串{Spanned类型}的工具类
 */
public class HtmlStringUtil {

    public static Spanned SongSingerName(String title, String artist){
        if (TextUtils.isEmpty(title) && TextUtils.isEmpty(artist))
            return Html.fromHtml("<font color = \"#EEEEEE\">快去听听音乐吧</font>",
                                                Html.FROM_HTML_OPTION_USE_CSS_COLORS);
        if (TextUtils.isEmpty(artist) || artist.equals("<unknown>")) artist = "Unknown";
        String highColor = "#EEEEEE", lowColor = "#CDCDCD";

        /*String SongInformation = "<font color = \"#EEEEEE\"><bold>"+title+"</bold></font>"+
                "<font color = \"#888\"><small><bold> - </bold>"+artist+"</small></font>";*/
        String SongInformation = "<font color = "+highColor+"><bold>"+title+"</bold></font>"+
                "<font color = "+lowColor+"><small><bold> - </bold>"+artist+"</small></font>";
        return Html.fromHtml(SongInformation,Html.FROM_HTML_OPTION_USE_CSS_COLORS);
    }
    public static String SheetTips(int count){
        return "已有歌单("+count+"个)";
    }
}
