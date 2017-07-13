package com.example.gopa2000.fyp_app;

/**
 * Created by gopa2000 on 7/12/17.
 */

public class MsgKeyPair {
    private String msg;
    private String key;

    public MsgKeyPair(String _msg, String _key){
        msg = _msg;
        key = _key;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
