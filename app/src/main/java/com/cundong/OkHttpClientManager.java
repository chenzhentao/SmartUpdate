package com.cundong;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.internal.$Gson$Types;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import okhttp3.Authenticator;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.Credentials;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.Route;


/**
 * Created by zhy on 15/8/17.
 */
public class OkHttpClientManager {

    private String TAG = this.getClass().getName();
    private static OkHttpClientManager mInstance;
    private OkHttpClient mOkHttpClient;
    private static Context mContext;
    private Handler mDelivery;
    private Gson mGson;

    public static void init(Context context) {
        mContext = context;
    }

    private OkHttpClientManager() {
        mOkHttpClient = new OkHttpClient.Builder().readTimeout(30, TimeUnit.SECONDS) // 读取超时
                .connectTimeout(10, TimeUnit.SECONDS) // 连接超时
                //                .addInterceptor(new GzipRequestInterceptor())
                .writeTimeout(60, TimeUnit.SECONDS) // 写入超时
                .authenticator(new Authenticator() {
                    @Override
                    public Request authenticate(Route route, Response response) throws IOException {//401，认证
                        String credential = Credentials.basic("zyao89", "password1");
                        return response.request().newBuilder().header("Authorization", credential).build();
                    }
                })
                .cookieJar(new CookieJar() {//这里可以做cookie传递，保存等操作
                    @Override
                    public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {//可以做保存cookies操作
                        System.out.println("cookies url: " + url.toString());
                        for (Cookie cookie : cookies) {
                            System.out.println("cookies: " + cookie.toString());
                        }
                    }

                    @Override
                    public List<Cookie> loadForRequest(HttpUrl url) {//加载新的cookies
                        ArrayList<Cookie> cookies = new ArrayList<>();
                        Cookie cookie = new Cookie.Builder()
                                .hostOnlyDomain(url.host())
                                .name("SESSION").value("zyao89")
                                .build();
                        cookies.add(cookie);
                        return cookies;
                    }
                })
                .build();

        mDelivery = new Handler(Looper.getMainLooper());
        mGson = new Gson();
    }

    public static OkHttpClientManager getInstance() {
        if (mInstance == null) {
            synchronized (OkHttpClientManager.class) {
                if (mInstance == null) {
                    mInstance = new OkHttpClientManager();
                }
            }
        }
        return mInstance;
    }


    /**
     * 同步的Get请求
     *
     * @param url
     * @return Response
     */
    private Response _getAsyn(String url) throws IOException {
        final Request request = new Request.Builder()
                .url(url)
                .build();
        Call call = mOkHttpClient.newCall(request);
        Response execute = call.execute();
        return execute;
    }

    /**
     * 同步的Get请求
     *
     * @param url
     * @return 字符串
     */
    private String _getAsString(String url) throws IOException {
        Response execute = _getAsyn(url);
        return execute.body().string();
    }


    /**
     * 异步的get请求
     *
     * @param url
     * @param callback
     */
    private void _getAsyn(String url, final ResultCallback callback) {
        final Request request = new Request.Builder().url(url).build();
        deliveryResult(callback, request);
    }


    /**
     * 同步的Post请求
     *
     * @param url
     * @param params post的参数
     * @return
     */
    private Response _post(String url, Param... params) throws IOException {
        Request request = buildPostRequest(url, params);
        Response response = mOkHttpClient.newCall(request).execute();
        return response;
    }


    /**
     * 同步的Post请求
     *
     * @param url
     * @param params post的参数
     * @return 字符串
     */
    private String _postAsString(String url, Param... params) throws IOException {
        Response response = _post(url, params);
        return response.body().string();
    }

    /**
     * 异步的post请求
     *
     * @param url
     * @param callback
     * @param params
     */
    private void _postAsyn(String url, final ResultCallback callback, Param... params) {
        Request request = buildPostRequest(url, params);
        deliveryResult(callback, request);
    }

    /**
     * 异步的post请求
     *
     * @param url
     * @param callback
     * @param params
     */
    private void _postAsyn(String url, final ResultCallback callback, Map<String, String> params) {
//        SignUtil.createPostSign(params);
        Param[] paramsArr = map2Params(params);
        Request request = buildPostRequest(url, paramsArr);
        deliveryResult(callback, request);
    }

    /**
     * 同步基于post的文件上传
     *
     * @param params
     * @return
     */
    private Response _post(String url, File[] files, String[] fileKeys, Param... params) throws IOException {
        Request request = buildMultipartFormRequest(url, files, fileKeys, params);
        return mOkHttpClient.newCall(request).execute();
    }

    private Response _post(String url, File file, String fileKey) throws IOException {
        Request request = buildMultipartFormRequest(url, new File[]{file}, new String[]{fileKey}, null);
        return mOkHttpClient.newCall(request).execute();
    }

    private Response _post(String url, File file, String fileKey, Param... params) throws IOException {
        Request request = buildMultipartFormRequest(url, new File[]{file}, new String[]{fileKey}, params);
        return mOkHttpClient.newCall(request).execute();
    }

    /**
     * 异步基于post的文件上传
     *
     * @param url
     * @param callback
     * @param files
     * @param fileKeys
     * @throws IOException
     */
    private void _postAsyn(String url, ResultCallback callback, File[] files, String[] fileKeys, Map<String, String> params) throws IOException {
//        SignUtil.createPostSign(params);
        Param[] paramsArr = map2Params(params);
        Request request = buildMultipartFormRequest(url, files, fileKeys, paramsArr);
        deliveryResult(callback, request);
    }

    /**
     * 异步基于post的文件上传
     *
     * @param url
     * @param callback
     * @param selectList
     * @throws IOException
     */
//    private void _postAsyn(String url, ResultCallback callback, List<LocalMedia> selectList, Map<String, String> params) throws IOException {
//        SignUtil.createPostSign(params);
//        Param[] paramsArr = map2Params(params);
//        Request request = buildPicpartFormRequest(url, selectList, paramsArr);
//        deliveryResult(callback, request);
//    }

    /**
     * 异步基于post的文件上传，单文件不带参数上传
     *
     * @param url
     * @param callback
     * @param file
     * @param fileKey
     * @throws IOException
     */
    private void _postAsyn(String url, ResultCallback callback, File file, String fileKey) throws IOException {
        Request request = buildMultipartFormRequest(url, new File[]{file}, new String[]{fileKey}, null);
        deliveryResult(callback, request);
    }

    /**
     * 异步基于post的文件上传，单文件且携带其他form参数上传
     *
     * @param url
     * @param callback
     * @param file
     * @param fileKey
     * @param params
     * @throws IOException
     */
    private void _postAsyn(String url, ResultCallback callback, File file, String fileKey, Param... params) throws IOException {
        Request request = buildMultipartFormRequest(url, new File[]{file}, new String[]{fileKey}, params);
        deliveryResult(callback, request);
    }

    /**
     * 异步下载文件
     *
     * @param url
     * @param destFileDir 本地文件存储的文件夹
     * @param callback
     */
    private void _downloadAsyn(final String url, final String destFileDir, final ResultCallback callback) {
        final Request request = new Request.Builder()
                .url(url)
                .build();
        final Call call = mOkHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                sendFailedStringCallback(request, e, callback);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                InputStream is = null;
                byte[] buf = new byte[2048];
                int len = 0;
                FileOutputStream fos = null;
                try {
                    is = response.body().byteStream();
                    File file = new File(destFileDir, getFileName(url));
                    fos = new FileOutputStream(file);
                    while ((len = is.read(buf)) != -1) {
                        fos.write(buf, 0, len);
                    }
                    fos.flush();
                    //如果下载文件成功，第一个参数为文件的绝对路径
                    sendSuccessResultCallback(file.getAbsolutePath(), callback);
                } catch (IOException e) {
                    sendFailedStringCallback(response.request(), e, callback);
                } finally {
                    try {
                        if (is != null)
                            is.close();
                    } catch (IOException e) {
                    }
                    try {
                        if (fos != null)
                            fos.close();
                    } catch (IOException e) {
                    }
                }
            }


        });
    }

    private String getFileName(String path) {
        int separatorIndex = path.lastIndexOf("/");
        return (separatorIndex < 0) ? path : path.substring(separatorIndex + 1, path.length());
    }

    /**
     * 加载图片
     *
     * @param view
     * @param url
     * @throws IOException
     */
    private void _displayImage(final ImageView view, final String url, final int errorResId) {
        setErrorResId(view, errorResId);
        final Request request = new Request.Builder()
                .url(url)
                .build();
        Call call = mOkHttpClient.newCall(request);
        call.enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                setErrorResId(view, errorResId);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                InputStream is = null;
                try {
                    is = response.body().byteStream();
                    ImageUtils.ImageSize actualImageSize = ImageUtils.getImageSize(is);
                    ImageUtils.ImageSize imageViewSize = ImageUtils.getImageViewSize(view);
                    int inSampleSize = ImageUtils.calculateInSampleSize(actualImageSize, imageViewSize);
                    try {
                        is.reset();
                    } catch (IOException e) {
                        response = _getAsyn(url);
                        is = response.body().byteStream();
                    }

                    BitmapFactory.Options ops = new BitmapFactory.Options();
                    ops.inJustDecodeBounds = false;
                    ops.inSampleSize = inSampleSize;
                    final Bitmap bm = BitmapFactory.decodeStream(is, null, ops);
                    if (bm == null) {
                        setErrorResId(view, errorResId);
                    } else {
                        mDelivery.post(new Runnable() {
                            @Override
                            public void run() {
                                view.setImageBitmap(bm);
                            }
                        });
                    }

                } catch (Exception e) {
                    setErrorResId(view, errorResId);

                } finally {
                    if (is != null)
                        try {
                            is.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                }
            }
        });


    }


    /**
     * 加载圆形图片
     *
     * @param view
     * @param url
     * @throws IOException
     */
    private void _displayCircleImage(final ImageView view, final String url, final int errorResId) {
        setErrorResId(view, errorResId);
        final Request request = new Request.Builder()
                .url(url)
                .build();
        Call call = mOkHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                setErrorResId(view, errorResId);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {


                InputStream is = null;
                try {
                    is = response.body().byteStream();
                    ImageUtils.ImageSize actualImageSize = ImageUtils.getImageSize(is);
                    ImageUtils.ImageSize imageViewSize = ImageUtils.getImageViewSize(view);
                    int inSampleSize = ImageUtils.calculateInSampleSize(actualImageSize, imageViewSize);
                    try {
                        is.reset();
                    } catch (IOException e) {
                        response = _getAsyn(url);
                        is = response.body().byteStream();
                    }

                    BitmapFactory.Options ops = new BitmapFactory.Options();
                    ops.inJustDecodeBounds = false;
                    ops.inSampleSize = inSampleSize;
                    final Bitmap bm = BitmapFactory.decodeStream(is, null, ops);
                    final Bitmap circlebm = toRoundBitmap(bm);
                    if (circlebm == null) {
                        setErrorResId(view, errorResId);
                    } else {
                        mDelivery.post(new Runnable() {
                            @Override
                            public void run() {
                                view.setImageBitmap(circlebm);
                            }
                        });
                    }

                } catch (Exception e) {
                    setErrorResId(view, errorResId);

                } finally {
                    if (is != null)
                        try {
                            is.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                }
            }
        });
    }

    /**
     * 加载圆形图片
     *
     * @param view
     * @param url
     * @throws IOException
     */
    private void _displayonlyCircleImage(final ImageView view, final String url) {
        final Request request = new Request.Builder()
                .url(url)
                .build();
        Call call = mOkHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {


                InputStream is = null;
                try {
                    is = response.body().byteStream();
                    ImageUtils.ImageSize actualImageSize = ImageUtils.getImageSize(is);
                    ImageUtils.ImageSize imageViewSize = ImageUtils.getImageViewSize(view);
                    int inSampleSize = ImageUtils.calculateInSampleSize(actualImageSize, imageViewSize);
                    try {
                        is.reset();
                    } catch (IOException e) {
                        response = _getAsyn(url);
                        is = response.body().byteStream();
                    }

                    BitmapFactory.Options ops = new BitmapFactory.Options();
                    ops.inJustDecodeBounds = false;
                    ops.inSampleSize = inSampleSize;
                    final Bitmap bm = BitmapFactory.decodeStream(is, null, ops);
                    final Bitmap circlebm = toRoundBitmap(bm);
                    if (circlebm == null) {
                    } else {
                        mDelivery.post(new Runnable() {
                            @Override
                            public void run() {
                                view.setImageBitmap(circlebm);
                            }
                        });
                    }

                } catch (Exception e) {

                } finally {
                    if (is != null)
                        try {
                            is.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                }
            }
        });

    }


    private void setErrorResId(final ImageView view, final int errorResId) {
        mDelivery.post(new Runnable() {
            @Override
            public void run() {
                view.setImageResource(errorResId);
            }
        });
    }

    /**
     * 转换图片成圆形
     *
     * @param bitmap 传入Bitmap对象
     * @return
     */
    public Bitmap toRoundBitmap(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float roundPx;
        float left, top, right, bottom, dst_left, dst_top, dst_right, dst_bottom;
        if (width <= height) {
            roundPx = width / 2;
            left = 0;
            top = 0;
            right = width;
            bottom = width;
            height = width;
            dst_left = 0;
            dst_top = 0;
            dst_right = width;
            dst_bottom = width;
        } else {
            roundPx = height / 2;
            float clip = (width - height) / 2;
            left = clip;
            right = width - clip;
            top = 0;
            bottom = height;
            width = height;
            dst_left = 0;
            dst_top = 0;
            dst_right = height;
            dst_bottom = height;
        }

        Bitmap output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect src = new Rect((int) left, (int) top, (int) right, (int) bottom);
        final Rect dst = new Rect((int) dst_left, (int) dst_top, (int) dst_right, (int) dst_bottom);
        final RectF rectF = new RectF(dst);

        paint.setAntiAlias(true);// 设置画笔无锯齿

        canvas.drawARGB(0, 0, 0, 0); // 填充整个Canvas
        paint.setColor(color);

        // 以下有两种方法画圆,drawRounRect和drawCircle
        // canvas.drawRoundRect(rectF, roundPx, roundPx, paint);// 画圆角矩形，第一个参数为图形显示区域，第二个参数和第三个参数分别是水平圆角半径和垂直圆角半径。
        canvas.drawCircle(roundPx, roundPx, roundPx, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));// 设置两张图片相交时的模式,参考http://trylovecatch.iteye.com/blog/1189452
        canvas.drawBitmap(bitmap, src, dst, paint); //以Mode.SRC_IN模式合并bitmap和已经draw了的Circle

        return output;
    }


    //*************对外公布的方法************


    public static Response getAsyn(String url) throws IOException {
        return getInstance()._getAsyn(url);
    }


    public static String getAsString(String url) throws IOException {
        return getInstance()._getAsString(url);
    }

    public static void getAsyn(String url, ResultCallback callback) {
        getInstance()._getAsyn(url, callback);
    }

    public static void getAsyn(String url, ResultCallback callback, Param... params) {
        getInstance()._getAsyn(url, callback);
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < params.length; i++) {
            buffer.append(params[i].key + ":" + params[i].value + "\n");
        }

    }

    public static void getAsyn(String url, ResultCallback callback, Map<String, String> params) {
        String signUrl = url + "?" + SignUtil.createSign(true, params);
//        XLog.e("OkHttpClientManager", signUrl);
        getInstance()._getAsyn(signUrl, callback);
    }

    public static void postAsyn(String url, final ResultCallback callback, Param... params) {
        getInstance()._postAsyn(url, callback, params);
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < params.length; i++) {
            buffer.append(params[i].key + ":" + params[i].value + "\n");
        }
    }

    public static Response post(String url, Param... params) throws IOException {
        return getInstance()._post(url, params);
    }

    public static String postAsString(String url, Param... params) throws IOException {
        return getInstance()._postAsString(url, params);
    }


    public static void postAsyn(String url, final ResultCallback callback, Map<String, String> params) {
        getInstance()._postAsyn(url, callback, params);
    }


    public static Response post(String url, File[] files, String[] fileKeys, Param... params) throws IOException {
        return getInstance()._post(url, files, fileKeys, params);
    }

    public static Response post(String url, File file, String fileKey) throws IOException {
        return getInstance()._post(url, file, fileKey);
    }

    public static Response post(String url, File file, String fileKey, Param... params) throws IOException {
        return getInstance()._post(url, file, fileKey, params);
    }

    public static void postAsyn(String url, ResultCallback callback, File[] files, String[] fileKeys, Map<String, String> params) throws IOException {
        getInstance()._postAsyn(url, callback, files, fileKeys, params);
    }

//    public static void postAsyn(String url, ResultCallback callback, List<LocalMedia> selectList, Map<String, String> params) throws IOException {
//        getInstance()._postAsyn(url, callback, selectList, params);
//    }


    public static void postAsyn(String url, ResultCallback callback, File file, String fileKey) throws IOException {
        getInstance()._postAsyn(url, callback, file, fileKey);
    }


    public static void postAsyn(String url, ResultCallback callback, File file, String fileKey, Param... params) throws IOException {
        getInstance()._postAsyn(url, callback, file, fileKey, params);
    }

    //    public static void displayImage(final ImageView view, String url, int errorResId) throws IOException
    //    {
    //        getInstance()._displayImage(view, url, errorResId);
    //    }
    //
    //
    //    public static void displayImage(final ImageView view, String url)
    //    {
    //        getInstance()._displayImage(view, url, R.drawable.default_image);
    //    }
    //
    //    public static void displayCircleImage(final ImageView view, String url, int errorResId) throws IOException
    //    {
    //        getInstance()._displayCircleImage(view, url, errorResId);
    //    }
    //
    //
    //    public static void displayCircleImage(final ImageView view, String url)
    //    {
    //        getInstance()._displayCircleImage(view, url, R.drawable.profile_no_avarta_icon);
    //    }
    //
    //    public static void displayonlyCircleImage(final ImageView view, String url)
    //    {
    //        getInstance()._displayonlyCircleImage(view, url);
    //    }

    public static void downloadAsyn(String url, String destDir, ResultCallback callback) {
        getInstance()._downloadAsyn(url, destDir, callback);
    }

    //****************************
/*    private Request buildPicpartFormRequest(String url, List<LocalMedia> selectList, Param[] params) {
        params = validateParam(params);

        MultipartBody.Builder builder = new MultipartBody.Builder("AaB03x")
                .setType(MultipartBody.FORM);

        for (Param param : params) {

            if (param.value != null)
                builder.addPart(Headers.of("Content-Disposition", "form-data; name=\"" + param.key + "\""), RequestBody.create(null, param.value));
        }
        if (selectList != null) {
            RequestBody fileBody = null;
            for (int i = 0; i < selectList.size(); i++) {
                LocalMedia pathBean = selectList.get(i);
                String path = pathBean.getCompressPath();
                if (TextUtils.isEmpty(path)) {
                    path = pathBean.getCutPath();
                    if (TextUtils.isEmpty(path))
                        path = pathBean.getPath();
                    if (TextUtils.isEmpty(path))
                        continue;
                }
                File file = new File(path);
                String fileName = file.getName();
                fileBody = RequestBody.create(MediaType.parse(guessMimeType(fileName)), file);
                //TODO 根据文件名设置contentType
                builder.addPart(Headers.of("Content-Disposition",
                        "form-data; name=\"" + "photo" + "\"; filename=\"" + fileName + "\""),
                        fileBody);
            }
        }

        RequestBody requestBody = builder.build();
        return new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();
    }*/

    private Request buildMultipartFormRequest(String url, File[] files,
                                              String[] fileKeys, Param[] params) {
        params = validateParam(params);

        MultipartBody.Builder builder = new MultipartBody.Builder("AaB03x")
                .setType(MultipartBody.FORM);

        for (Param param : params) {
            builder.addPart(Headers.of("Content-Disposition", "form-data; name=\"" + param.key + "\""),
                    RequestBody.create(null, param.value));
        }
        if (files != null) {
            RequestBody fileBody = null;
            for (int i = 0; i < files.length; i++) {
                File file = files[i];
                String fileName = file.getName();
                fileBody = RequestBody.create(MediaType.parse(guessMimeType(fileName)), file);
                //TODO 根据文件名设置contentType
                builder.addPart(Headers.of("Content-Disposition",
                        "form-data; name=\"" + fileKeys[i] + "\"; filename=\"" + fileName + "\""),
                        fileBody);
            }
        }

        RequestBody requestBody = builder.build();
        return new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();
    }

    private String guessMimeType(String path) {
        FileNameMap fileNameMap = URLConnection.getFileNameMap();
        String contentTypeFor = fileNameMap.getContentTypeFor(path);
        if (contentTypeFor == null) {
            contentTypeFor = "application/octet-stream";
        }
        return contentTypeFor;
    }


    private Param[] validateParam(Param[] params) {
        if (params == null)
            return new Param[0];
        else
            return params;
    }

    private Param[] map2Params(Map<String, String> params) {
        if (params == null)
            return new Param[0];
        int size = params.size();
        Param[] res = new Param[size];
        Set<Map.Entry<String, String>> entries = params.entrySet();
        int i = 0;
        for (Map.Entry<String, String> entry : entries) {
            res[i++] = new Param(entry.getKey(), entry.getValue());
        }
        return res;
    }

    private static final String SESSION_KEY = "Set-Cookie";
    private static final String mSessionKey = "JSESSIONID";

    private Map<String, String> mSessions = new HashMap<String, String>();

    private void deliveryResult(final ResultCallback callback, final Request request) {
        mOkHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                sendFailedStringCallback(request, e, callback);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {


                try {
                    final String string = response.body().string();
                    if (callback.mType == String.class) {
                        sendSuccessResultCallback(string, callback);
                    } else {
                        Object o = mGson.fromJson(string, callback.mType);
                        sendSuccessResultCallback(o, callback);
                    }


                } catch (IOException e) {
                    sendFailedStringCallback(response.request(), e, callback);
                } catch (com.google.gson.JsonParseException e)//Json解析的错误
                {
                    sendFailedStringCallback(response.request(), e, callback);
                }

            }
        });
    }

    private void sendFailedStringCallback(final Request request, final Exception e, final ResultCallback callback) {
        mDelivery.post(new Runnable() {
            @Override
            public void run() {
                if (callback != null&&callback.mActivity!=null)
                    callback.onError(callback.mActivity,request, e);
            }
        });
    }

    private void sendSuccessResultCallback(final Object object, final ResultCallback callback) {
        mDelivery.post(new Runnable() {
            @Override
            public void run() {
                if (callback != null&&callback.mActivity!=null) {
                    callback.onResponse(callback.mActivity,object);
                }
            }
        });
    }

    private Request buildPostRequest(String url, Param[] params) {
        if (params == null) {
            params = new Param[0];
        }
        FormBody.Builder builder = new FormBody.Builder();
        //                FormEncodingBuilder builder = new FormEncodingBuilder();
        //        StringBuffer sb = new StringBuffer();
        //        sb.append("地址:" + url + "\n");
        for (Param param : params) {
            if (param.value != null)
                builder.add(param.key, param.value);
            //            sb.append(param.key + " : " + param.value + "\n");
        }
        RequestBody requestBody = builder.build();
        return new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();
    }


    public static abstract class ResultCallback<T, P> {
        Type mType;
        WeakReference<P> mActivity;

        public ResultCallback(WeakReference<P> nP) {
            this.mActivity = nP;
            mType = getSuperclassTypeParameter(getClass());
        }

        static Type getSuperclassTypeParameter(Class<?> subclass) {
            Type superclass = subclass.getGenericSuperclass();
            if (superclass instanceof Class) {
                throw new RuntimeException("Missing type parameter.");
            }
            ParameterizedType parameterized = (ParameterizedType) superclass;
            return $Gson$Types.canonicalize(parameterized.getActualTypeArguments()[0]);
        }

        public void onBefore() {

        }

        public void onError(WeakReference<P> mActivity,Request request, Exception e) {
            //Toast.makeText(mContext,"网络状态不佳~", Toast.LENGTH_SHORT).show();

        }

        public void onResponse(WeakReference<P> mActivity,T response) {
            String JoStr = (String) response;
            //            BaseResponse br = (BaseResponse) response;
            try {
                BaseResponse br = BaseResponse.objectFromData(JoStr);
                if ("1".equals(br.getStatus())) {
                    JSONObject jsonObject = new JSONObject(JoStr);
                    doRightResponse(JoStr, jsonObject);
                }

                if (br == null) {
                    Toast.makeText(mContext, "网络状态不佳", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (br.getStatus().equals("2")) {
                    //                    Toast.makeText(mContext,"登陆失效,请重新登录", Toast.LENGTH_SHORT).show();
                    //                    Intent intent=new Intent(mContext,E0_LoginActivity.class);
                    //                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    //                    intent.putExtra("session","2");
                    //                    mContext.startActivity(intent);
                } else if (br.getStatus().equals("0")) {
                    Toast.makeText(mContext, br.getMsg() + "", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
                //                Toast.makeText(mContext,"网络状态不佳", Toast.LENGTH_SHORT).show();
            }
        }

        /**
         * 只有status=1 才会调用这个方法
         *
         * @param result     返回的字符串
         * @param jsonObject json格式
         */
        public void doRightResponse(String result, JSONObject jsonObject) {

        }
    }

    public static abstract class MyCallback<T> {
        Type mType;

        public MyCallback() {
            mType = getSuperclassTypeParameter(getClass());
        }

        static Type getSuperclassTypeParameter(Class<?> subclass) {
            Type superclass = subclass.getGenericSuperclass();
            if (superclass instanceof Class) {
                throw new RuntimeException("Missing type parameter.");
            }
            ParameterizedType parameterized = (ParameterizedType) superclass;
            return $Gson$Types.canonicalize(parameterized.getActualTypeArguments()[0]);
        }

        public abstract void onError(Request request, Exception e);

        public void onResponse(T response) {
            String JoStr = (String) response;
            BaseResponse br = BaseResponse.objectFromData(JoStr);
            if (br == null) {
                Toast.makeText(mContext, "网络状态不佳", Toast.LENGTH_SHORT).show();
                return;
            }
            if (br.getStatus().equals("403")) {
                //                Toast.makeText(mContext,"登陆失效,请重新登录", Toast.LENGTH_SHORT).show();
                //                mContext.startActivity(new Intent(mContext, E0_LoginActivity.class));
            } else if (br.getStatus().equals("0")) {
                Toast.makeText(mContext, br.getMsg() + "", Toast.LENGTH_SHORT).show();
            }

        }

        ;
    }

    public static class Param {
        public Param() {
        }

        public Param(String key, String value) {
            this.key = key;
            this.value = value;
        }

        String key;
        String value;
    }


    ////////////////////////////////////////////////////////////////////////////////////////


    /**
     * post请求参数
     *
     * @param BodyParams
     * @return
     */
    private RequestBody SetRequestBody(Map<String, Object> BodyParams) {
        RequestBody body = null;
        // 创建请求的参数body
        FormBody.Builder builder = new FormBody.Builder();

        // 遍历key
        if (null != BodyParams) {
            for (Map.Entry<String, Object> entry : BodyParams.entrySet()) {

                System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue());

                builder.add(entry.getKey(), entry.getValue().toString());

            }
        }

        body = builder.build();
        return body;
    }


    /**
     * get方法连接拼加参数
     *
     * @param mapParams
     * @return
     */
    private String setUrlParams(Map<String, Object> mapParams) {
        String strParams = "";
        if (mapParams != null) {
            Iterator<String> iterator = mapParams.keySet().iterator();
            String key = "";
            while (iterator.hasNext()) {
                key = iterator.next().toString();
                strParams += "&" + key + "=" + mapParams.get(key);
            }
        }

        return strParams;
    }

    /**
     * POST方法获取实体bean
     *
     * @param reqUrl
     * @param map
     * @param mHandler
     * @param rspClass
     */
    public void postBeanExecute(String reqUrl, Map<String, Object> map, final Handler mHandler,
                                final Class<?> rspClass) {
        RequestBody body = SetRequestBody(map);
        Request request = new Request.Builder().url(reqUrl).post(body).build();
        Call call = mOkHttpClient.newCall(request);
        call.enqueue(new Callback() {

            @Override
            public void onResponse(Call arg0, Response arg1) throws IOException {
                String Result = arg1.body().string();
                //          Result = PUB.decodeBase64(Result);
                Log.d(TAG, "[postBeanExecute]Call===" + arg1.code() + "Response===" + Result);

                Message mess = mHandler.obtainMessage();
                if (arg1.code() == 200) {
                    try {
                        // 转换返回结果信息给BEAN
                        mess.obj = new Gson().fromJson(Result, rspClass);
                    } catch (Exception e) {
                        mess.what = 404;
                        mess.obj = "HTTP数据异常-0002";
                    }
                } else {
                    mess.what = arg1.code();
                    mess.obj = "HTTP状态异常-" + arg1.code();
                }
                mHandler.sendMessage(mess);
            }

            @Override
            public void onFailure(Call arg0, IOException arg1) {
                Log.d(TAG, "[onFailure]Call===" + arg0 + "IOException===" + arg1.toString());

                Message mess = mHandler.obtainMessage();
                mess.what = 404;
                mess.obj = "HTTP通讯错误-0001";
                mHandler.sendMessage(mess);
            }
        });
    }

    /**
     * Get方法获取实体bean
     *
     * @param reqUrl
     * @param map
     * @param mHandler
     * @param rspClass
     */
    public void getBeanExecute(String reqUrl, Map<String, Object> map, final Handler mHandler,
                               final Class<?> rspClass) {
        String UrlParams = setUrlParams(map);
        String URL = reqUrl + "?" + UrlParams;
        Log.d(TAG, "[getBeanExecute]URL===" + URL);

        Request request = new Request.Builder().url(URL).build();
        Call call = mOkHttpClient.newCall(request);
        call.enqueue(new Callback() {

            @Override
            public void onResponse(Call arg0, Response arg1) throws IOException {
                String Result = arg1.body().string();
                //          Result = PUB.decodeBase64(Result);
                Log.d(TAG, "[onResponse]Call===" + arg1.code() + "Response===" + Result);

                Message mess = mHandler.obtainMessage();
                if (arg1.code() == 200) {
                    try {
                        // 转换返回结果信息给BEAN
                        mess.obj = new Gson().fromJson(Result, rspClass);
                    } catch (Exception e) {
                        mess.what = 404;
                        mess.obj = "HTTP数据异常-0002";
                    }
                } else {
                    mess.what = arg1.code();
                    mess.obj = "HTTP状态异常-" + arg1.code();
                }
                mHandler.sendMessage(mess);
            }

            @Override
            public void onFailure(Call arg0, IOException arg1) {
                Log.d(TAG, "[onFailure]Call===" + arg0 + "IOException===" + arg1.toString());

                Message mess = mHandler.obtainMessage();
                mess.what = 404;
                mess.obj = "HTTP通讯错误-0001";
                mHandler.sendMessage(mess);
            }
        });
    }


    /**
     * Post 获取String数据
     *
     * @param reqUrl
     * @param mHandler
     */
    public void postStringExecute(String reqUrl, Map<String, Object> map, final Handler mHandler) {
        RequestBody body = SetRequestBody(map);
        Request request = new Request.Builder().url(reqUrl).post(body).build();
        Log.d(TAG, "[postStringExecute]URL===" + reqUrl);

        Call call = mOkHttpClient.newCall(request);
        call.enqueue(new Callback() {

            @Override
            public void onResponse(Call arg0, Response arg1) throws IOException {
                String Result = arg1.body().string();
                //          Result = PUB.decodeBase64(Result);
                Log.d(TAG, "[onResponse]Call===" + arg1.code() + "Response===" + Result);

                Message mess = mHandler.obtainMessage();
                if (arg1.code() == 200) {
                    try {
                        // 转换返回结果信息给BEAN
                        mess.obj = Result;
                    } catch (Exception e) {
                        mess.what = 404;
                        mess.obj = "HTTP数据异常-0002";
                    }
                } else {
                    mess.what = arg1.code();
                    mess.obj = "HTTP状态异常-" + arg1.code();
                }
                mHandler.sendMessage(mess);
            }

            @Override
            public void onFailure(Call arg0, IOException arg1) {
                Log.d(TAG, "[onFailure]Call===" + arg0 + "IOException===" + arg1.toString());

                Message mess = mHandler.obtainMessage();
                mess.what = 404;
                mess.obj = "HTTP通讯错误-0001";
                mHandler.sendMessage(mess);
            }
        });
    }


    /**
     * Get 获取String数据
     *
     * @param reqUrl
     * @param mHandler
     */
    public void getStringExecute(String reqUrl, Map<String, Object> map, final Handler mHandler) {
        String UrlParams = setUrlParams(map);
        String URL = reqUrl + "?" + UrlParams;
        Log.d(TAG, "[getBeanExecute]URL===" + URL);

        Request request = new Request.Builder().url(URL).build();
        Call call = mOkHttpClient.newCall(request);
        call.enqueue(new Callback() {

            @Override
            public void onResponse(Call arg0, Response arg1) throws IOException {
                String Result = arg1.body().string();
                //          Result = PUB.decodeBase64(Result);
                Log.d(TAG, "[onResponse]Call===" + arg1.code() + "Response===" + Result);

                Message mess = mHandler.obtainMessage();
                if (arg1.code() == 200) {
                    try {
                        // 转换返回结果信息给BEAN
                        mess.obj = Result;
                    } catch (Exception e) {
                        mess.what = 404;
                        mess.obj = "HTTP数据异常-0002";
                    }
                } else {
                    mess.what = arg1.code();
                    mess.obj = "HTTP状态异常-" + arg1.code();
                }
                mHandler.sendMessage(mess);
            }

            @Override
            public void onFailure(Call arg0, IOException arg1) {
                Log.d(TAG, "[onFailure]Call===" + arg0 + "IOException===" + arg1.toString());

                Message mess = mHandler.obtainMessage();
                mess.what = 404;
                mess.obj = "HTTP通讯错误-0001";
                mHandler.sendMessage(mess);
            }
        });
    }
}