package com.cundong.apkpatch.example;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.cundong.apkpatch.example.update.UpdateService;

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