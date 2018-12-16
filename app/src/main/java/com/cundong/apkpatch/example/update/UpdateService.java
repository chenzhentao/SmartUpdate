package com.cundong.apkpatch.example.update;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.IntentService;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.WindowManager;

import com.cundong.OkHttpClientManager;
import com.cundong.apkpatch.example.Constant;
import com.cundong.apkpatch.example.R;

import java.lang.ref.WeakReference;
import java.util.HashMap;

public class UpdateService extends IntentService {
    private ApkDownloader downloader;
    private String mSaveName;
    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public UpdateService(String name) {
        super(name);
    }

    public UpdateService() {
        super("someName");
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        HashMap<String,String> dataMap = new HashMap<>();
        dataMap.put("versionCode",getVersionCode(this)+"");
        OkHttpClientManager.getAsyn(Constant.updateCheck, new OkHttpClientManager.ResultCallback<String,UpdateService>(new WeakReference<UpdateService>(this)) {
            @Override
            public void onResponse(WeakReference<UpdateService> mActivity, String response) {
//              UpdateInfo updateInfo = new Gson().fromJson(response,UpdateInfo.class);

//                super.onResponse(mActivity, response);
            }
        },dataMap);
        UpdateInfo updateInfo = new UpdateInfo();
        updateInfo.setAppName("任行宝");
        updateInfo.setVersionCode(4);
        updateInfo.setDescription("");
        updateInfo.setDownUrl("http://rengxingbao.oss-cn-shenzhen.aliyuncs.com/androd-apk/2018/12/04/f10973e330a94e108ff1b0ccce618c0ef10973e330a94e108ff1b0ccce618c0ef10973e330a94e108ff1b0ccce618c0e.apk");
        updateInfo.setIsPatch(1);
        updateInfo.setOpenSilent(0);
        updateInfo.setVersionName("44");
        Log.e("http://rengxingbao","http://rengxingbao");
        showDialog(this,updateInfo);
//        update(updateInfo);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

    }
    private void showDialog(final Context context, final UpdateInfo info) {

        final Dialog dialog = new AlertDialog.Builder(context)
                .setIcon(R.mipmap.ic_launcher)
                .setTitle("更新提示")
                .setMessage(info.getDescription())
                .setCancelable(false)
                .setPositiveButton("后台更新", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        // TODO Auto-generated method stub
                        downloader.startDownload();
                        arg0.cancel();
                    }
                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        // TODO Auto-generated method stub

                        downloader.cancelUpgrade(UpdateService.this);
                        arg0.cancel();
                    }
                })
                .create();
        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);

        dialog.show();

    }
    public void update(UpdateInfo info) {

        downloader = new ApkDownloader.Builder(UpdateService.this).setIsSilentInstall(info.getOpenSilent()==1).setDownloadUrl(info.getDownUrl()).setNotificationDescription(info.getDescription().toString()).setNotificationTitle(info.getVersionName()).setSaveFileName(mSaveName).setPatch(info.getIsPatch()==1).builder();

        if (info.getDownUrl()!=null) {//如果开启静默安装直接下载，否则显示更新对话框
            if (info.getOpenSilent()==1) {
                //下载应用
                Log.d("TAG", "Update: 静默安装：：");
                downloader.startDownload();
            } else {
                showDialog(UpdateService.this, info);
            }
        } else {
            showNoUpdateDialog(UpdateService.this);
        }
    }
    private void showNoUpdateDialog(final Context context) {
        Looper.prepare();
        final Dialog dialog = new AlertDialog.Builder(context)
                .setIcon(R.mipmap.ic_launcher)
                .setTitle("更新")
                .setMessage("当前已是最新版本")
                .setCancelable(false)
                .setPositiveButton("确认", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int arg1) {
                        dialog.dismiss();
                    }
                })
                .create();
        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);

        dialog.show();


    }
    /**
     * @param context
     * @return
     * @描述：获取应用的版本信号
     */
    private int getVersionCode(Context context) {
        int versionCode = 0;
        try {
            versionCode = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return versionCode;
    }
    /**
     * @return
     * @描述：获取服务器的版本名
     */
    public String getVersionName(UpdateInfo info) {
        return info == null ? null : info.getVersionName();
    }
}
