package com.sunyard.asr.utils;

public class StrUtils {

    public static String toString(Object obj, String emptyStr) {
        String str = obj == null ? emptyStr : obj.toString();
        if (obj == null || str.isEmpty() || str.trim().isEmpty()) {
            str = emptyStr;
        }
        return str;
    }
    
    public static String toString(Object obj) {
        return toString(obj, "");
    }
    
    
    public static String getSuffix(String filePath) {
        return filePath.substring(filePath.lastIndexOf(".") + 1);
    }
}
