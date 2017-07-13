package com.example.gopa2000.fyp_app;

import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.widget.TextView;

import com.google.zxing.common.reedsolomon.Util;

public class KeyExchangeActvityReceiver extends Activity {

    private TextView base64TextView;
    private TextView hexTextView;
    private TextView resultTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_key_exchange_actvity_receiver);

        base64TextView = (TextView) findViewById(R.id.b64_tv);
        hexTextView = (TextView) findViewById(R.id.hex_msg);
        resultTextView = (TextView) findViewById(R.id.org_msg);

        String rscodebase64 = getIntent().getStringExtra("RSCODEBASE64");
        String rscodehex = Util.toHex(Base64.decode(rscodebase64, Base64.DEFAULT));
        String result = getIntent().getStringExtra("RESULT");

        base64TextView.setText(rscodebase64);
        hexTextView.setText(rscodehex);
        resultTextView.setText(result);
    }
}
