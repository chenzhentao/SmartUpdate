package com.cundong;

import com.google.gson.Gson;

/**
 * Created by Dell on 2017/8/30.
 */
public class BaseResponse {
   private String msg;
   private String status;

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
    public static BaseResponse objectFromData(String str) {

        return new Gson().fromJson(str, BaseResponse.class);
    }

    @Override
    public String toString() {
        return "BaseResponse{" +
                "msg='" + msg + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
