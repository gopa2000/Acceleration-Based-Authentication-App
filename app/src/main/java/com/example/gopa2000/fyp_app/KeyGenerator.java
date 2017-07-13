package com.example.gopa2000.fyp_app;

/**
 * Created by gopa2000 on 7/8/17.
 */

public interface KeyGenerator {
    public MsgKeyPair generateKey(String fingerprint);
    public String KeyDecoder(String fingerprint, String key) throws Exception;
}
