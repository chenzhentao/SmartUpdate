package com.cundong.apkpatch.example.update;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * Created by Administrator on 2018/6/30.
 */

public class UpdateReceiver extends BroadcastReceiver {
//    <receiver android:name=".UpdateReceiver">
//            <intent-filter>
//                <action android:name="android.intent.action.PACKAGE_REPLACED"/>
//                <action android:name="android.intent.action.PACKAGE_ADDED" />
//                <action android:name="android.intent.action.PACKAGE_REMOVED" />
//                <data android:scheme="package"/>
//            </intent-filter>
//        </receiver>
//    注册广播

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO Auto-generated method stub
        Log.e("TAG", "onReceive: 收到UpdateReceiver 广播；；；；"+android.os.Process.myPid());
        Toast.makeText(context, "test收到广播了", Toast.LENGTH_LONG).show();
        String packageName = intent.getDataString();
        if (intent.getAction().equals(Intent.ACTION_PACKAGE_REPLACED)) {//接收升级广播
            Log.e("TAG", "onReceive:升级了一个安装包，重新启动此程序 " + packageName);
//            Intent startIntent = new Intent(context, SplashActivity.class);
//            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            context.startActivity(startIntent);
//            reStart(context, packageName);
        } else if (intent.getAction().equals(Intent.ACTION_PACKAGE_ADDED)) {//接收安装广播
//            String packageName = intent.getDataString();
            Log.e("TAG", "onReceive:安装了 " + packageName);
        } else if (intent.getAction().equals(Intent.ACTION_PACKAGE_REMOVED)) { //接收卸载广播
//            String packageName = intent.getDataString();
            Log.e("TAG", "onReceive:卸载了 " + packageName);
        }

    }

    private void removeFile(String apkName) {
        String path = Environment.getExternalStorageDirectory().getPath() + "/Download/" + apkName;
        File file = new File(path);
        Log.e("TAG", "path:= " + path);
        if (file.exists()) {
            file.delete();
            Log.e("TAG", "removeFile: ");
        }
    }

    private void execLinuxCommand() {
        String cmd = "sleep 10; am start -n com.sqy.vending.mqttvending_master/com.sqy.vending.mqttvending_master.SplashActivity";
        //Runtime对象
        Runtime runtime = Runtime.getRuntime();
        try {
            Process localProcess = runtime.exec("su");
            OutputStream localOutputStream = localProcess.getOutputStream();
            DataOutputStream localDataOutputStream = new DataOutputStream(localOutputStream);
            localDataOutputStream.writeBytes(cmd);
            localDataOutputStream.flush();
            Log.e("TAG", "设备准备重启");
            localProcess.waitFor();
        } catch (IOException e) {
            Log.e("TAG", "strLine：" + e.getMessage());
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param packagename 将要启动另一个App的包名
     */
    public void reStart(Context context, String packagename) {
        Log.e("TAG", "重新启动此程序 " + packagename);
        PackageInfo packageinfo = null;
        try {
            packageinfo = context.getPackageManager().getPackageInfo(packagename, 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        if (packageinfo == null) {
            return;
        }
        Intent resolveIntent = new Intent(Intent.ACTION_MAIN, null);
        resolveIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        resolveIntent.setPackage(packageinfo.packageName);
        List<ResolveInfo> resolveinfoList = context.getPackageManager()
                .queryIntentActivities(resolveIntent, 0);
        ResolveInfo resolveinfo = resolveinfoList.iterator().next();
        if (resolveinfo != null) {
            // packagename = 参数packname
            String packageName = resolveinfo.activityInfo.packageName;
            // 这个就是我们要找的该APP的LAUNCHER的Activity[组织形式：packagename.mainActivityname]
            String className = resolveinfo.activityInfo.name;
            // LAUNCHER Intent
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);

            // 设置ComponentName参数1:packagename参数2:MainActivity路径
            ComponentName cn = new ComponentName(packageName, className);
            intent.setComponent(cn);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }


}
