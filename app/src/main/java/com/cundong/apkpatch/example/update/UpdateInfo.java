package com.cundong.apkpatch.example.update;

/**
 * Created by Administrator on 2018/6/29.
 */

public class UpdateInfo {

    /** 应用名称 **/
    private String appName;
    /** 版本号 **/
    private int versionCode;
    /** 版本名 **/
    private String versionName;
    /** 更新地址 **/
    private String downUrl;
    /** 更新描述 **/
    private String description;
    /**全包0，差分1，修复3*/
    private int isPatch;
    /**更新是否要经用户同意，询问0，不询问1，强制2*/

    private int openSilent;

    /**
     * @param versionCode
     * @param versionName
     * @param downUrl
     * @param description
     */
    public UpdateInfo(String appName, int versionCode, String versionName,
                      String downUrl, String description) {
        super();
        this.appName = appName;
        this.versionCode = versionCode;
        this.versionName = versionName;
        this.downUrl = downUrl;
        this.description = description;
    }

    /**
     *
     */
    public UpdateInfo() {
        super();
    }

    public int getVersionCode() {
        return versionCode;
    }

    public void setVersionCode(int versionCode) {
        this.versionCode = versionCode;
    }

    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    public String getDownUrl() {
        return downUrl;
    }

    public void setDownUrl(String downUrl) {
        this.downUrl = downUrl;
    }

    public StringBuilder getDescription() {
        String[] tips = description.split(";");
        StringBuilder updateContent = new StringBuilder();
        for (int i = 0; i < tips.length; i++) {
            if (i != tips.length - 1) {
                updateContent.append(tips[i] + "\n");
            } else {
                updateContent.append(tips[i]);
            }
        }

        return updateContent;
    }

    public int getIsPatch() {
        return isPatch;
    }

    public void setIsPatch(int isPatch) {
        this.isPatch = isPatch;
    }

    public int getOpenSilent() {
        return openSilent;
    }

    public void setOpenSilent(int openSilent) {
        this.openSilent = openSilent;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

}
