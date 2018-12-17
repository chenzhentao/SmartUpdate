package com.cundong.apkpatch.example;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.cundong.OkHttpClientManager;
import com.cundong.apkpatch.example.update.ApkDownloader;
import com.cundong.apkpatch.example.update.UpdateInfo;
import com.cundong.apkpatch.example.update.UpdateService;

import java.lang.ref.WeakReference;
import java.util.HashMap;

/**
 * 类说明：   ApkPatchLibrary 使用Sample
 *
 * @author Cundong
 * @version 1.5
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = Constants.DEBUG ? "MainActivity" : MainActivity.class.getSimpleName();

    // 成功
    private static final int WHAT_SUCCESS = 1;

    // 本地安装的微博MD5不正确
    private static final int WHAT_FAIL_OLD_MD5 = -1;

    // 新生成的微博MD5不正确
    private static final int WHAT_FAIL_GEN_MD5 = -2;

    // 合成失败
    private static final int WHAT_FAIL_PATCH = -3;

    // 获取源文件失败
    private static final int WHAT_FAIL_GET_SOURCE = -4;

    // 未知错误
    private static final int WHAT_FAIL_UNKNOWN = -5;

    private Context mContext = null;

    private ProgressDialog mProgressDialog;
    private TextView mResultView;
    private Button mStartButton, mGithubButton;

    private long mBeginTime, mEndTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mContext = getApplicationContext();

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setMessage("doing..");
        mProgressDialog.setCancelable(false);

        mResultView = (TextView) findViewById(R.id.textview4);

        mStartButton = (Button) findViewById(R.id.start_btn);
        mGithubButton = (Button) findViewById(R.id.github_btn);
        mStartButton.setOnClickListener(this);
        mGithubButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        startService(new Intent(this, UpdateService.class));
        HashMap<String, String> dataMap = new HashMap<>();
        dataMap.put("versionCode", getVersionCode(this) + "");
        OkHttpClientManager.getAsyn(Constant.updateCheck, new OkHttpClientManager.ResultCallback<String, MainActivity>(new WeakReference<MainActivity>(this)) {
            @Override
            public void onResponse(WeakReference<MainActivity> mActivity, String response) {
//              UpdateInfo updateInfo = new Gson().fromJson(response,UpdateInfo.class);

//                super.onResponse(mActivity, response);
            }
        }, dataMap);
        UpdateInfo updateInfo = new UpdateInfo();
        updateInfo.setAppName("任行宝");
        updateInfo.setVersionCode(4);
        updateInfo.setDescription("");
        updateInfo.setDownUrl("http://rengxingbao.oss-cn-shenzhen.aliyuncs.com/androd-apk/2018/12/17/363840e9c0aa4511a40df146c9cf1c86363840e9c0aa4511a40df146c9cf1c86363840e9c0aa4511a40df146c9cf1c86.apk");
        updateInfo.setIsPatch(1);
        updateInfo.setOpenSilent(0);
        updateInfo.setVersionName("44");
        Log.e("http://rengxingbao", "http://rengxingbao");
//        showDialog(this, updateInfo);
        update(updateInfo);
    }
    private ApkDownloader downloader;
    private String mSaveName;
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

                        downloader.cancelUpgrade(MainActivity.this);
                        arg0.cancel();
                    }
                })
                .create();
//        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);

        dialog.show();

    }


    /**
     * @return
     * @描述：获取服务器的版本名
     */
    public String getVersionName(UpdateInfo info) {
        return info == null ? null : info.getVersionName();
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
    public void update(UpdateInfo info) {

        downloader = new ApkDownloader.Builder(this)
                .setIsSilentInstall(info.getOpenSilent() == 1)
                .setDownloadUrl(info.getDownUrl())
                .setNotificationDescription(info.getDescription().toString())
                .setNotificationTitle(info.getVersionName())
                .setSaveFileName(mSaveName)
                .setPatch(info.getIsPatch() == 1).builder();

        if (info.getDownUrl() != null) {//如果开启静默安装直接下载，否则显示更新对话框
            if (info.getOpenSilent() == 1) {
                //下载应用
                Log.d("TAG", "Update: 静默安装：：");
                downloader.startDownload();
            } else {
                showDialog(this, info);
            }
        } else {
            showNoUpdateDialog(this);
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
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    static {
        System.loadLibrary("ApkPatchLibrary");
    }
}