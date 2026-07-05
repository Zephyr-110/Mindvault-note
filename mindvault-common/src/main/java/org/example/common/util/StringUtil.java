package org.example.common.util;

public class StringUtil {

    private StringUtil() {
    }

    /**
     * 截断字符串，超出最大长度则追加省略号
     */
    public static String truncate(String content, int maxLen) {
        if (content == null) {
            return "";
        }
        return content.length() > maxLen ? content.substring(0, maxLen) + "..." : content;
    }
}