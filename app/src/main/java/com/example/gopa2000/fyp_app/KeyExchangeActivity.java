package com.example.gopa2000.fyp_app;

import android.app.Activity;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Base64;
import android.widget.TextView;

import com.google.zxing.common.reedsolomon.Util;

public class KeyExchangeActivity extends Activity {

    private TextView messageTextView;
    private TextView base64TextView;
    private TextView hexTextView;
    private TextView resultTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_key_exchange);

        messageTextView = (TextView) findViewById(R.id.message_tv);
        base64TextView = (TextView) findViewById(R.id.b64_tv);
        hexTextView = (TextView) findViewById(R.id.hex_message);
        resultTextView = (TextView) findViewById(R.id.result_tv);

        String message = getIntent().getStringExtra("MESSAGE");
        String rscodeb64 = getIntent().getStringExtra("RSCODEB64");
        String rscode = Util.toHex(Base64.decode(rscodeb64, Base64.DEFAULT));
        String result = getIntent().getStringExtra("RESULT");

        messageTextView.setText(message);
        base64TextView.setText(rscodeb64);
        hexTextView.setText(rscode);
        resultTextView.setText(result);
    }
}
