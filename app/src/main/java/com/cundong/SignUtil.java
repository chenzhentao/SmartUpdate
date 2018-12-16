package com.cundong;

import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;


public class SignUtil {

    public static String createSign(Boolean encode, Map<String, String> params) {
        Set<String> keysSet = params.keySet();
        Object[] keys = keysSet.toArray();
        Arrays.sort(keys);
        StringBuffer temp = new StringBuffer();
        Boolean first = true;
        for (Object key : keys) {
            if (first) {
                first = false;
            } else {
                temp.append("&");
            }
            temp.append(key).append("=");
            Object value = params.get(key);
            String valueString = "";
            if (null != value) {
                valueString = String.valueOf(value);
            }
            if (encode) {
                try {
                    temp.append(URLEncoder.encode(valueString, "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            } else {
                temp.append(valueString);
            }
        }
        Log.e("SignUtil", temp.toString());
        return temp + "&sign=" + MD5util.string2MD5(temp.toString()).toUpperCase();
    }

    public static Map<String, String> createPostSign(Map<String, String> params) {
        Set<String> keysSet = params.keySet();
        Object[] keys = keysSet.toArray();
        Arrays.sort(keys);
        StringBuffer temp = new StringBuffer();
        Boolean first = true;
        for (Object key : keys) {
            if (first) {
                first = false;
            } else {
                temp.append("&");
            }
            temp.append(key).append("=");
            Object value = params.get(key);
            String valueString = "";
            if (null != value) {
                valueString = String.valueOf(value);
            }
            try {
                temp.append(URLEncoder.encode(valueString, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        Log.e("SignUtil", temp.toString());
        params.put("sign", MD5util.string2MD5(temp.toString()).toUpperCase());
        return params;
    }
}
