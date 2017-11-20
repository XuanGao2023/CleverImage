package com.pan.cleverimage.task.base;

import android.text.TextUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by pan on 20/11/2017.
 */

public class Setting {
    public static final String URL = "url";
    protected String strFileFolder;
    protected Map<String, Object> mapSetting = new HashMap<>();

    public Setting() {
        strFileFolder = "";
    }

    public Setting(String filedir) {
        strFileFolder = filedir;
    }

    public String buildCacheKey() {
        String url = getUrl();
        return getUrlKey(url);
    }

    public String buildDiskFileName() {
        String url = getUrl();
        return getUrlKey(url);
    }

    public String buildDiskFileDir() {
        return strFileFolder + buildDiskFileName();
    }

    public boolean isDiskFileValid() {
        String filepath = strFileFolder + buildDiskFileName();
        File file = new File(filepath);
        if (file.exists()) {
            return true;
        } else {
            return false;
        }
    }

    protected static String getUrlKey(String url) {
        if (url == null) {
            throw new RuntimeException("Null url passed in");
        } else {
            return url.replaceAll("[.:/,%?&=]", "_").replaceAll("[_]+", "_");
        }
    }

    public void putAll(Setting setting) {
        if (setting != null) {
            if (TextUtils.isEmpty(strFileFolder)) {
                strFileFolder = setting.strFileFolder;
            }
            mapSetting.putAll(setting.mapSetting);
        }
    }

    public void putUrl(String url) {
        mapSetting.put(URL, url);
    }

    public String getUrl() {
        Object obj = mapSetting.get(URL);
        if (obj != null && obj instanceof String) {
            return (String) obj;
        }
        throw new IllegalStateException("url is not initialized!");
    }
}
