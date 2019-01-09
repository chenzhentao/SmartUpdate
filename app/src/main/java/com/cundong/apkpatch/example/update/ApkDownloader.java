package com.cundong.apkpatch.example.update;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import com.blankj.utilcode.util.AppUtils;
import com.cundong.apkpatch.example.ApkUtils;
import com.cundong.apkpatch.example.Constants;
import com.cundong.apkpatch.example.SignUtils;
import com.cundong.utils.PatchUtils;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;


public class ApkDownloader {
    private final boolean isPatch;
    private String mCurentRealMD5 = "37:0D:6C:CC:7B:BD:14:F4:AA:66:9D:81:22:B5:30:BA", mNewRealMD5 = "";
    // 成功
    private static final int WHAT_SUCCESS = 1;

    // 本地安装的MD5不正确
    private static final int WHAT_FAIL_OLD_MD5 = -1;

    // 新生成的MD5不正确
    private static final int WHAT_FAIL_GEN_MD5 = -2;

    // 合成失败
    private static final int WHAT_FAIL_PATCH = -3;

    // 获取源文件失败
    private static final int WHAT_FAIL_GET_SOURCE = -4;

    // 未知错误
    private static final int WHAT_FAIL_UNKNOWN = -5;
    private static final String TAG = ApkDownloader.class.getSimpleName();

    private Context mContext;
    private String mDownloadUrl;
    private String mNoTitle;
    private String mNoDescription;
    private boolean isSilentInstall;
    private String mSaveName;

    public static final String DOWNLOAD_FOLDER_NAME = "Download";
    public static final String APK_DOWNLOAD_ID = "apkDownloadId";

    private DownloadManager mDownloadManager;
    private CompleteReceiver completeReceiver;

    private ApkDownloader(Builder builder) {
        this.mContext = builder.mContext;
        this.mDownloadUrl = builder.mDownloadUrl;
        this.mNoTitle = builder.mNoTitle;
        this.mNoDescription = builder.mNoDescription;
        this.isSilentInstall = builder.isSilentInstall;
        this.mSaveName = builder.mSaveName;
        this.isPatch = builder.isPatch;
        mDownloadManager = (DownloadManager) mContext.getSystemService(Context.DOWNLOAD_SERVICE);
        completeReceiver = new CompleteReceiver();

        /** register download success broadcast **/
        mContext.registerReceiver(completeReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    public void startDownload() {
        //清除已下载的内容重新下载
        long downloadId = UpdateUtils.getLong(mContext, APK_DOWNLOAD_ID);
        if (downloadId != -1) {
            mDownloadManager.remove(downloadId);
            UpdateUtils.removeSharedPreferenceByKey(mContext, APK_DOWNLOAD_ID);
        }
        File apkFile = new File(Environment.getExternalStorageDirectory() + File.separator + DOWNLOAD_FOLDER_NAME + File.separator + mSaveName);
        if (apkFile != null && apkFile.exists()) {
            apkFile.delete();
        }
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(mDownloadUrl));
        //设置Notification中显示的文字
        request.setTitle(mNoTitle);
        request.setDescription(mNoDescription);
        //设置可用的网络类型
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE | DownloadManager.Request.NETWORK_WIFI);
        //设置状态栏中显示Notification
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        //不显示下载界面
        request.setVisibleInDownloadsUi(false);
        //设置下载后文件存放的位置
        File folder = Environment.getExternalStoragePublicDirectory(DOWNLOAD_FOLDER_NAME);
        if (!folder.exists() || !folder.isDirectory()) {
            folder.mkdirs();
        }
        //设置下载文件的保存路径
        request.setDestinationInExternalPublicDir(DOWNLOAD_FOLDER_NAME, mSaveName);
        //设置文件类型
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        String mimeString = mimeTypeMap.getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(mDownloadUrl));
        request.setMimeType(mimeString);
        //保存返回唯一的downloadId
        long id = mDownloadManager.enqueue(request);
        UpdateUtils.putLong(mContext, APK_DOWNLOAD_ID, id);
    }

    private long mCompleteDownloadId;

    class CompleteReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(final Context context, final Intent intent) {
            /**
             * get the id of download which have download success, if the id is my id and it's status is successful,
             * then install it
             **/
            mCompleteDownloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
            long downloadId = UpdateUtils.getLong(context, APK_DOWNLOAD_ID);

            if (mCompleteDownloadId == downloadId) {

                // if download successful
                if (queryDownloadStatus(mDownloadManager, downloadId) == DownloadManager.STATUS_SUCCESSFUL) {
                    //unregisterReceiver
                    context.unregisterReceiver(completeReceiver);
                    boolean haveInstallPermission;
                    // 兼容Android 8.0
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                        //先获取是否有安装未知来源应用的权限
                        haveInstallPermission = context.getPackageManager().canRequestPackageInstalls();
                        if (!haveInstallPermission) {//没有权限
                            // 弹窗，并去设置页面授权
                            final AndroidOInstallPermissionListener listener = new AndroidOInstallPermissionListener() {
                                @Override
                                public void permissionSuccess() {
                                    beforeInstallApk(context);
                                }

                                @Override
                                public void permissionFail() {
                                    Toast.makeText(context, "授权失败，无法安装应用", Toast.LENGTH_SHORT).show();
                                }
                            };

                            AndroidOPermissionActivity.sListener = listener;
                            Intent intent1 = new Intent(context, AndroidOPermissionActivity.class);
                            context.startActivity(intent1);


                        } else
                            beforeInstallApk(context);
                    } else
                        beforeInstallApk(context);
                }
            }
        }
    }

    private void beforeInstallApk(Context context) {
        Log.e("ApkDownloader", "beforeInstallApk" + Thread.currentThread().getName());
        String apkFilePath = new StringBuilder(Environment.getExternalStorageDirectory().getAbsolutePath())
                .append(File.separator).append(DOWNLOAD_FOLDER_NAME).append(File.separator)
                .append(mSaveName).toString();
        if (isPatch) {
            doInBackground(apkFilePath);
        } else if (isSilentInstall) {
//            Log.e(TAG, ": 静默安装路径=" + apkFilePath);
            Log.e(TAG, ": 静默安装路径=" + installSilent(context, apkFilePath));
        } else {
            Log.e(TAG, "onReceive: 非静默安装");
            installApk(context);
        }
    }

    /**
     * 查询下载状态
     */
    public static int queryDownloadStatus(DownloadManager downloadManager, long downloadId) {
        int result = -1;
        DownloadManager.Query query = new DownloadManager.Query().setFilterById(downloadId);
        Cursor c = null;
        try {
            c = downloadManager.query(query);
            if (c != null && c.moveToFirst()) {
                result = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return result;
    }

    protected Integer doInBackground(String patchPath) {

        PackageInfo packageInfo = ApkUtils.getInstalledApkPackageInfo(mContext, Constants.TEST_PACKAGENAME);

        if (packageInfo != null) {

//                        requestOldMD5(packageInfo.versionCode, packageInfo.versionName);
            mCurentRealMD5 = AppUtils.getAppSignatureMD5();
            String oldApkSource = ApkUtils.getSourceApkPath(mContext, Constants.TEST_PACKAGENAME);

            if (!TextUtils.isEmpty(oldApkSource)) {

                // 校验一下本地安装APK的MD5是不是和真实的MD5一致
                if (SignUtils.checkMd5(oldApkSource, mCurentRealMD5)) {
                    int patchResult = PatchUtils.patch(oldApkSource, Constants.NEW_APK_PATH, patchPath);

                    if (patchResult == 0) {

                        if (SignUtils.checkMd5(Constants.NEW_APK_PATH, mNewRealMD5)) {
                            installApk(mContext, Constants.NEW_APK_PATH);
                            Log.e("doInBackground", "11111111111111111");
                            return WHAT_SUCCESS;
                        } else {
                            Log.e("doInBackground", "222222222222");
                            return WHAT_FAIL_GEN_MD5;
                        }
                    } else {
                        Log.e("doInBackground", "333333333");
                        return WHAT_FAIL_PATCH;
                    }
                } else {
                    Log.e("doInBackground", "4444444444");
                    return WHAT_FAIL_OLD_MD5;
                }
            } else {
                Log.e("doInBackground", "5555555555555");
                return WHAT_FAIL_GET_SOURCE;
            }
        } else {
            Log.e("doInBackground", "6666666666666");
            return WHAT_FAIL_UNKNOWN;
        }
    }

    /**
     * @param context
     */
    private void installApk(Context context) {
        Uri uri;
        Intent intentInstall = new Intent();
        intentInstall.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intentInstall.setAction(Intent.ACTION_VIEW);
        long downloadId = UpdateUtils.getLong(context, APK_DOWNLOAD_ID);
        //clear downloadId
        UpdateUtils.removeSharedPreferenceByKey(context, APK_DOWNLOAD_ID);
        if (mCompleteDownloadId == downloadId) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) { // 6.0以下
                uri = mDownloadManager.getUriForDownloadedFile(downloadId);
            } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) { // 6.0 - 7.0
                File apkFile = queryDownloadedApk(context, downloadId);
                uri = Uri.fromFile(apkFile);
            } else { // Android 7.0 以上
                uri = FileProvider.getUriForFile(context,
                        "com.cundong.apkpatch.example.fileProvider",
                        new File(Environment.getExternalStorageDirectory() + File.separator + DOWNLOAD_FOLDER_NAME, mSaveName));
                Log.e(TAG, "installApk: uri=" + uri);
                intentInstall.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            }

            // 安装应用
            Log.e(TAG, "下载完成了");

            intentInstall.setDataAndType(uri, "application/vnd.android.package-archive");
            context.startActivity(intentInstall);
        }
    }

    /**
     * @param context
     */
    private void installApk(Context context, String apkPath) {
        Uri uri;
        Intent intentInstall = new Intent();
        intentInstall.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intentInstall.setAction(Intent.ACTION_VIEW);
        long downloadId = UpdateUtils.getLong(context, APK_DOWNLOAD_ID);
        //clear downloadId
        UpdateUtils.removeSharedPreferenceByKey(context, APK_DOWNLOAD_ID);
        if (mCompleteDownloadId == downloadId) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) { // 6.0以下
                uri = Uri.parse("file://" + apkPath);
            } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) { // 6.0 - 7.0
                File apkFile = new File(apkPath);
                uri = Uri.fromFile(apkFile);
            } else { // Android 7.0 以上
                uri = FileProvider.getUriForFile(context,
                        "com.cundong.apkpatch.example.fileProvider",
                        new File(apkPath));
                Log.e(TAG, "installApk: uri=" + uri);
                intentInstall.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            }

            // 安装应用
            Log.e(TAG, "下载完成了");

            intentInstall.setDataAndType(uri, "application/vnd.android.package-archive");
            context.startActivity(intentInstall);
        }
    }

    //通过downLoadId查询下载的apk，解决6.0以后安装的问题
    private File queryDownloadedApk(Context context, long downloadId) {
        File targetApkFile = null;
        DownloadManager downloader = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);

        if (downloadId != -1) {
            DownloadManager.Query query = new DownloadManager.Query();
            query.setFilterById(downloadId);
            query.setFilterByStatus(DownloadManager.STATUS_SUCCESSFUL);
            Cursor cur = downloader.query(query);
            if (cur != null) {
                if (cur.moveToFirst()) {
                    String uriString = cur.getString(cur.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
                    if (!TextUtils.isEmpty(uriString)) {
                        targetApkFile = new File(Uri.parse(uriString).getPath());
                    }
                }
                cur.close();
            }
        }
        return targetApkFile;
    }

    public void cancelUpgrade(Context context) {
        context.unregisterReceiver(completeReceiver);
    }

    //静默安装方法，返回值是0代表成功，1失败，其他不知
    public static int installSilent(Context context,String path) {
        PrintWriter PrintWriter = null;
        Process process = null;
        try {
            process = Runtime.getRuntime().exec("su");
            PrintWriter = new PrintWriter(process.getOutputStream());
            PrintWriter.println("chmod 777 " + path);
            PrintWriter
                    .println("export LD_LIBRARY_PATH=/vendor/lib:/system/lib");
            PrintWriter.println("pm install -r " + path);
            // PrintWriter.println("exit");
            PrintWriter.flush();
            PrintWriter.close();
            int value = process.waitFor();
            return (Integer) value;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    private void execLinuxCommand() {
        String cmd = "sleep 120; am start -n com.sanqiyi.test/com.sanqiyi.test.SplashActivity";
        //Runtime对象
        Runtime runtime = Runtime.getRuntime();
        Process localProcess = null;
        try {
            localProcess = runtime.exec("su");
            OutputStream localOutputStream = localProcess.getOutputStream();
            DataOutputStream localDataOutputStream = new DataOutputStream(localOutputStream);
            localDataOutputStream.writeBytes(cmd);
            localDataOutputStream.flush();
            Log.e(TAG, "设备准备重启");
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "execLinuxCommand: IOException=" + e.toString());
        }
    }

    protected void excutesucmd(Context context, String currenttempfilepath) {
        Process process = null;
        OutputStream out = null;
        InputStream in = null;
        try {
            // 请求root
            process = Runtime.getRuntime().exec("su");
            out = process.getOutputStream();
            // 调用安装
            out.write(("pm install -r " + currenttempfilepath + "\n").getBytes());
            in = process.getInputStream();
            int len = 0;
            byte[] bs = new byte[256];
            while (-1 != (len = in.read(bs))) {
                String state = new String(bs, 0, len);
                if (state.equals("success\n")) {
                    //安装成功后的操作
                    Log.e(TAG, "excutesucmd: 安装成功了；；；；");
                    //静态注册自启动广播
                    Intent intent = new Intent();
                    //与清单文件的receiver的anction对应
                    intent.setAction("android.intent.action.PACKAGE_REPLACED");
                    //发送广播
                    mContext.sendBroadcast(intent);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            installApk(context, currenttempfilepath);
        } finally {
            try {
                if (out != null) {
                    out.flush();
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public interface AndroidOInstallPermissionListener {
        void permissionSuccess();

        void permissionFail();
    }

    public static class Builder {
        private Context mContext;
        private String mDownloadUrl;
        private String mNoTitle;
        private String mNoDescription;
        private boolean isSilentInstall = false;
        private String mSaveName;
        private boolean isPatch = false;

        public Builder(Context context) {
            mContext = context;
        }

        public Builder setDownloadUrl(String url) {
            mDownloadUrl = url;
            return this;
        }

        public Builder setNotificationTitle(String title) {
            mNoTitle = title;
            return this;
        }

        public boolean isPatch() {
            return isPatch;
        }

        public Builder setPatch(boolean patch) {
            isPatch = patch;
            return this;
        }

        public Builder setNotificationDescription(String content) {
            mNoDescription = content;
            return this;
        }

        public Builder setIsSilentInstall(boolean silentInstall) {
            isSilentInstall = silentInstall;
            return this;
        }

        public Builder setSaveFileName(String fileName) {
            mSaveName = fileName;
            return this;
        }

        public ApkDownloader builder() {
            if (TextUtils.isEmpty(mSaveName)) {
                this.mSaveName = mDownloadUrl.substring(mDownloadUrl.lastIndexOf("/") + 1);
            }
            return new ApkDownloader(this);
        }
    }

}
