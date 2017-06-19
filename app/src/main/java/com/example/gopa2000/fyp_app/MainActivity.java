package com.example.gopa2000.fyp_app;

import android.Manifest;
import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewAnimator;

import org.w3c.dom.Text;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;

public class MainActivity extends Activity implements SensorEventListener{

    private static String TAG = "MainActivity";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;

    public String connectedDeviceName = null;

    // Name of connected device
    private String connectedDevice = null;

    //String buffer for outgoing message
    private StringBuffer outStringBuffer;

    // Local bluetooth adapter
    private BluetoothAdapter ba = null;

    // BT services
    private BluetoothManager btManager = null;

    /** Sensor variables **/

    // Sensor manager and sensors
    private SensorManager sensorManager;
    private Sensor accSensor;
    private Sensor gyrSensor;

    private boolean recording;


    private ArrayList<SensorCombo> sensorCombo;
    private SensorCombo currentRecording;

    /** ui **/
    private Button recordButton;
    private Button stopButton;

    private TextView xacc;
    private TextView yacc;
    private TextView zacc;

    private TextView xgyr;
    private TextView ygyr;
    private TextView zgyr;

    private EditText filename_field;

    /** misc **/
    private String filename;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.secure_connect_scan: {
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(this, DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
                return true;
            }
            case R.id.insecure_connect_scan: {
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(this, DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE);
                return true;
            }
            case R.id.discoverable: {
                // Ensure this device is discoverable by others
                ensureDiscoverable();
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ba = BluetoothAdapter.getDefaultAdapter();

        // if adapter is null, then BT isn't suported
        if (ba == null){
            Toast.makeText(this, "Bluetooth isn't available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }


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
            startRecording();
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            stopRecording();
            }
        });
    }

    public void startRecording(){
        if(filename_field.getText().toString().equals(""))
            Toast.makeText(MainActivity.this, "No filename specified.", Toast.LENGTH_SHORT).show();

        else if(!recording) {
            sendMessage("RECORD_START");
            recordButton.setEnabled(false);
            stopButton.setEnabled(true);
            recording = true;
            currentRecording = new SensorCombo();
            filename = filename_field.getText().toString();
        }
    }

    public void stopRecording(){
        if(recording){
            sendMessage("RECORD_STOP");
            recordButton.setEnabled(true);
            stopButton.setEnabled(false);
            recording = false;
            sensorCombo.add(currentRecording);
            currentRecording.generateAccTestFile();
            currentRecording.saveToFile(filename);
            currentRecording = null;
        }
    }

    @Override
    public void onStart(){
        super.onStart();

        // enable bt if disabled
        if(!ba.isEnabled()){
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }

        else {
            setupApp();
        }
    }

    private void setupApp(){
        // initialise bt manager
        btManager = new BluetoothManager(this, handler);
        Log.d(TAG, "setupApp: BTManager Initialised.");
        outStringBuffer = new StringBuffer("");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(btManager != null)
            btManager.stop();
    }

    private void ensureDiscoverable(){
        Log.d(TAG, "ensureDiscoverable: start");

        if(ba.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE){
            Intent discoverableIntent = new Intent(
                    BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(
                    BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    private void sendMessage(String message){
        if(btManager.getState() != BluetoothManager.STATE_CONNECTED){
            Toast.makeText(this, "No device connected.", Toast.LENGTH_SHORT).show();
            return;
        }

        if(message.length() > 0){
            byte[] send = message.getBytes();
            btManager.write(send);

            // reset buffer
            outStringBuffer.setLength(0);
        }
    }

    private void setStatus(int resId) {
        final ActionBar actionBar = this.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(resId);
    }

    private void setStatus(CharSequence subTitle) {

        final ActionBar actionBar = this.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(subTitle);
    }

    // Handler that balls out with the Bluetooth Manager
    private final Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg){
            switch(msg.what){
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothManager.STATE_CONNECTED:
                            setStatus(getString(R.string.title_connected_to, connectedDeviceName));
                            break;
                        case BluetoothManager.STATE_CONNECTING:
                            setStatus(R.string.title_connecting);
                            break;
                        case BluetoothManager.STATE_LISTEN:
                        case BluetoothManager.STATE_NONE:
                            setStatus(R.string.title_not_connected);
                            break;
                    }
                    break;

                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    break;
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);

                    if(readMessage.equals("RECORD_START"))
                        startRecording();

                    if(readMessage.equals("RECORD_STOP"))
                        stopRecording();

                    break;

                case Constants.MESSAGE_DEVICE_NAME:
                    connectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    Toast.makeText(MainActivity.this, "Connected to " + connectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case Constants.MESSAGE_TOAST:
                    Toast.makeText(MainActivity.this, msg.getData().getString(Constants.TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data){
        switch(requestCode) {
            case REQUEST_CONNECT_DEVICE_SECURE:
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, true);
                }
                break;
            case REQUEST_CONNECT_DEVICE_INSECURE:
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, false);
                }
                break;
            case REQUEST_ENABLE_BT:
                if( resultCode == Activity.RESULT_OK){
                    setupApp();
                }
                else {
                    Log.d(TAG, "onActivityResult: Bluetooth not enabled.");
                    Toast.makeText(this, "Bluetooth not enabled.", Toast.LENGTH_SHORT).show();
                    finish();
                }
        }
    }

    private void connectDevice(Intent data, boolean secure){
        String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);

        // get the bt object
        BluetoothDevice device = ba.getRemoteDevice(address);

        // attempt connection
        btManager.connect(device, secure);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        if(recording){
            Sensor sensor = sensorEvent.sensor;

            Log.i(TAG, "onSensorChanged: Sensor working!" + sensorEvent.sensor.toString());

            if(sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

                double x = Math.round(sensorEvent.values[0] * 10000d) / 10000d;
                double y = Math.round(sensorEvent.values[1] * 10000d) / 10000d;
                double z = Math.round(sensorEvent.values[2] * 10000d) / 10000d;

                long timestamp = System.currentTimeMillis();

                if(currentRecording != null)
                    currentRecording.addToAcc(new AccelData(timestamp, x, y, z));

                xacc.setText(String.valueOf(x));
                yacc.setText(String.valueOf(y));
                zacc.setText(String.valueOf(z));

                Log.i(TAG, "onSensorChanged: Accelerometer values - " + x + ", " + y + ", " + "z");
            }

            else if(sensor.getType() == Sensor.TYPE_GYROSCOPE){

                double x = Math.round(sensorEvent.values[0] * 10000d) / 10000d;
                double y = Math.round(sensorEvent.values[1] * 10000d) / 10000d;
                double z = Math.round(sensorEvent.values[2] * 10000d) / 10000d;

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
    protected synchronized void onResume() {
        super.onResume();

        if(btManager != null){
            // ok, only start btManager if STATE_NONE, else it's already been started
            if(btManager.getState() == BluetoothManager.STATE_NONE)
                btManager.start();
        }

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
