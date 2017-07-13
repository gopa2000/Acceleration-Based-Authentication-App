package com.example.gopa2000.fyp_app;

import android.util.Base64;
import android.util.Log;

import com.google.zxing.common.reedsolomon.Util;

/**
 * Created by gopa2000 on 7/8/17.
 */

public class RSKeyGenerator implements KeyGenerator {

    private static String TAG = "RSKeyGenerator";

    public RSKeyGenerator(){

    }

    public MsgKeyPair generateKey(String fingerprint){
        EncoderDecoder encoderDecoder = new EncoderDecoder();

        try {
            String message = generateMessage();
            byte[] data = message.getBytes();

            byte[] encodedData = encoderDecoder.encodeData(data, 80);
            byte[] noisyData = encodedData.clone();

            for(int i=0; i<encodedData.length; i++){
                byte one = 0xff & 00000001;
                if(fingerprint.charAt(i) == '1') {
                    int oneInt = (int) one;
                    int edInt = (int)encodedData[i];
                    int xor = oneInt ^ edInt;

                    noisyData[i] = (byte)(0xff & xor);
                }
            }

            String keyToSend = new String(Base64.encode(noisyData, Base64.DEFAULT));
            return new MsgKeyPair(message, keyToSend);
        }
        catch (Exception e){
            Log.e(TAG, "generateKey: ", e);
        }

        return new MsgKeyPair("", "");
    }

    public String KeyDecoder(String fingerprint, String key) throws Exception{
        byte[] noisyData = Base64.decode(key, Base64.DEFAULT);

        byte[] decomCode = noisyData.clone();
        for(int i=0; i<noisyData.length; i++){
            byte one = 0xff & 00000001;
            if(fingerprint.charAt(i) == '1'){
                int oneInt = (int)one;
                int ndInt = (int) noisyData[i];
                int xor = oneInt ^ ndInt;

                decomCode[i] = (byte)(0xff & xor);
            }
        }

        EncoderDecoder encoderDecoder = new EncoderDecoder();

        byte[] decodedData = encoderDecoder.decodeData(decomCode, 80);
        String originalMessage = new String(decodedData);

        return originalMessage;
    }

    public static String generateMessage(){
        String msg = "";

        for(int i=0; i<80; i++){
            int bit = Math.round(randomWithRange(0,1));
            msg = msg + String.valueOf(bit);
        }

        return msg;
    }

    private static int randomWithRange(int min, int max) {
        int range = (max - min) + 1;
        return (int)(Math.random() * range) + min;
    }

}
