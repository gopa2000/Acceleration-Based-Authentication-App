package com.example.gopa2000.fyp_app;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements SensorEventListener{

    private static String TAG = "MainActivity";

    private SensorManager sensorManager;
    private Sensor accSensor;
    private Sensor gyrSensor;

    private boolean recording;

    private Button recordButton;
    private Button stopButton;

    private ArrayList<SensorCombo> sensorCombo;
    private SensorCombo currentRecording;

    private TextView xacc;
    private TextView yacc;
    private TextView zacc;

    private TextView xgyr;
    private TextView ygyr;
    private TextView zgyr;

    private EditText filename_field;
    private String filename;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toast.makeText(MainActivity.this, Environment.getExternalStorageDirectory().toString(), Toast.LENGTH_SHORT).show();

        recording = false;

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyrSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        sensorCombo = new ArrayList<>();

        currentRecording = null;

        sensorManager.registerListener(this, accSensor, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, gyrSensor, SensorManager.SENSOR_DELAY_FASTEST);

        recordButton = (Button) findViewById(R.id.rec_button);
        stopButton = (Button) findViewById(R.id.stop_button);

        recordButton.setEnabled(true);
        stopButton.setEnabled(false);

        filename_field = (EditText) findViewById(R.id.fn_field);

        xacc = (TextView) findViewById(R.id.x_acc);
        yacc = (TextView) findViewById(R.id.y_acc);
        zacc = (TextView) findViewById(R.id.z_acc);

        xgyr = (TextView) findViewById(R.id.x_gyr);
        ygyr = (TextView) findViewById(R.id.y_gyr);
        zgyr = (TextView) findViewById(R.id.z_gyr);


        recordButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                if(filename_field.getText().toString().equals(""))
                    Toast.makeText(MainActivity.this, "No filename specified.", Toast.LENGTH_SHORT).show();

                else if(!recording) {
                    recordButton.setEnabled(false);
                    stopButton.setEnabled(true);
                    recording = true;
                    currentRecording = new SensorCombo();
                    filename = filename_field.getText().toString();
                }
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(recording){
                    recordButton.setEnabled(true);
                    stopButton.setEnabled(false);
                    recording = false;
                    sensorCombo.add(currentRecording);
                    currentRecording.generateTestFile();
                    currentRecording.saveToFile(filename);
                    currentRecording = null;
                }
            }
        });
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        if(recording){
            Sensor sensor = sensorEvent.sensor;

            Log.i(TAG, "onSensorChanged: Sensor working!" + sensorEvent.sensor.toString());

            if(sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

                double x = sensorEvent.values[0];
                double y = sensorEvent.values[1];
                double z = sensorEvent.values[2];

                long timestamp = System.currentTimeMillis();

                if(currentRecording != null)
                    currentRecording.addToAcc(new AccelData(timestamp, x, y, z));

                xacc.setText(String.valueOf(x));
                yacc.setText(String.valueOf(y));
                zacc.setText(String.valueOf(z));

                Log.i(TAG, "onSensorChanged: Accelerometer values - " + x + ", " + y + ", " + "z");
            }

            else if(sensor.getType() == Sensor.TYPE_GYROSCOPE){

                double x = sensorEvent.values[0];
                double y = sensorEvent.values[1];
                double z = sensorEvent.values[2];

                long timestamp = System.currentTimeMillis();

                if(currentRecording != null)
                    currentRecording.addToGyr(new GyroData(timestamp, x, y, z));

                xgyr.setText(String.valueOf(x));
                ygyr.setText(String.valueOf(y));
                zgyr.setText(String.valueOf(z));

                Log.i(TAG, "onSensorChanged: Gyroscope values - " + x + ", " + y + ", " + z);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    protected void onResume() {
        super.onResume();

        if(recording){
            sensorManager.registerListener(this,
                    sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(recording){
            sensorManager.unregisterListener(this);
        }
    }
}
