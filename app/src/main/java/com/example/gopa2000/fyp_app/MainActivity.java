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
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewAnimator;

import org.w3c.dom.Text;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity implements SensorEventListener{

    private static String TAG = "MainActivity";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;

    // Key derivation states
    private static final int KD_FIRST_STATE = 1;
    private static final int KD_SECOND_STATE = 2;
    private static final int KD_THIRD_STATE = 3;

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
    private Device currentDevice;

    private ArrayList<SensorCombo> sensorCombo;
    private SensorCombo currentRecording;

    /** ui **/
    private Button recordButton;
    private Button stopButton;
    private Button clearButton;
    private Button authButton;
    private Button deriveButton;
    private Switch autoSwitch;

    private TextView xacc;
    private TextView yacc;
    private TextView zacc;

    private TextView xgyr;
    private TextView ygyr;
    private TextView zgyr;

    private TextView statusView;
    private EditText filename_field;

    /* recording timer tasks */
    private StopRecordingTask stopRecordingTask;
    private StartRecordingTask startRecordingTask;
    private Timer startTimer;
    private Timer stopTimer;

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

        autoSwitch = (Switch) findViewById(R.id.auto_switch);
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

        deriveButton = (Button) findViewById(R.id.derive_btn);
        authButton = (Button) findViewById(R.id.auth_btn);
        clearButton = (Button) findViewById(R.id.clear_btn);
        autoSwitch = (Switch) findViewById(R.id.auto_switch);

        recordButton.setEnabled(true);
        stopButton.setEnabled(false);

        statusView = (TextView) findViewById(R.id.status_tv);
        filename_field = (EditText) findViewById(R.id.fn_field);

        xacc = (TextView) findViewById(R.id.x_acc);
        yacc = (TextView) findViewById(R.id.y_acc);
        zacc = (TextView) findViewById(R.id.z_acc);

        xgyr = (TextView) findViewById(R.id.x_gyr);
        ygyr = (TextView) findViewById(R.id.y_gyr);
        zgyr = (TextView) findViewById(R.id.z_gyr);

        currentDevice = null;

        keyDerivationSetState(KD_FIRST_STATE);

        recordButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                if(filename_field.getText().toString().equals(""))
                    Toast.makeText(MainActivity.this, "No filename specified.", Toast.LENGTH_SHORT).show();
                else {

                    if(!autoSwitch.isChecked()){
                        sendMessage("RECORD|START");
                        startRecording();

                        recordButton.setEnabled(false);
                        stopButton.setEnabled(true);
                    }

                    else {

                        startTimer = new Timer();
                        stopTimer = new Timer();

                        startRecordingTask = new StartRecordingTask(MainActivity.this);
                        stopRecordingTask = new StopRecordingTask(MainActivity.this);

                        long startDelay = 5000;
                        long endDelay = 15000;

                        Date timeToStart = new Date(System.currentTimeMillis() + startDelay);
                        long timeToStartInMillis = timeToStart.getTime();
                        long timeToStopInMillis = timeToStartInMillis + endDelay;
                        Date timeToStop = new Date(timeToStopInMillis);

                        Log.d("SyncFix", "TimeToStart: " + timeToStart.getTime() + ", TimeToStop: " + timeToStop.getTime());

                        sendMessage("RECORD|START|" + Long.toString(timeToStartInMillis));

                        startTimer.schedule(startRecordingTask, timeToStart);
                        stopTimer.schedule(stopRecordingTask, timeToStop);

                        recordButton.setEnabled(false);
                        stopButton.setEnabled(true);
                    }
                }
            }
        });
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopRecording();
            }
        });

        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clearSession();
            }
        });

        deriveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SensorCombo cur = sensorCombo.get(sensorCombo.size() - 1);
                initDevice(filename, cur);
                keyDerivationSetState(KD_THIRD_STATE);
            }
        });

        authButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                RSKeyGenerator rsKeyGen = new RSKeyGenerator();
                MsgKeyPair msgKeyPair = rsKeyGen.generateKey(currentDevice.getFingerprint());

                String rsKey = msgKeyPair.getKey();
                String msg = msgKeyPair.getMsg();

                currentDevice.setMsgKeyPair(msgKeyPair);
                sendMessage("SEND|RSKEY|"+rsKey);
            }
        });
    }

    public void cancelStartTimer(){
        startTimer.cancel();
        startTimer.purge();
    }

    public void cancelStopTimer(){
        stopTimer.cancel();
        stopTimer.purge();
    }

    public void setStatusText(final String text){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                statusView.setText(text);
            }
        });
    }

    public void startRecording(){
        if(!recording) {
            recording = true;
            currentRecording = new SensorCombo();
            filename = filename_field.getText().toString();
        }
    }

    public void stopRecording(){
        if(recording){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    recordButton.setEnabled(true);
                    stopButton.setEnabled(false);
                }
            });

            recording = false;
            sensorCombo.add(currentRecording);
            currentRecording.generateAccTestFile();
            currentRecording.saveToFile(filename);
            currentRecording = null;
            setStatusText("Ready to derive key.");
            keyDerivationSetState(KD_SECOND_STATE);
        }
    }

    private void initDevice(String fn, SensorCombo current){
        currentDevice = new Device(fn, current);
    }

    private void clearSession(){
        currentDevice = null;
        setStatusText("Session cleared. Ready to record.");
        keyDerivationSetState(KD_FIRST_STATE);
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
                    String msgParts[] = readMessage.split("\\|");

                    Log.d(TAG, "handleMessage: Received message - " + readMessage);
                    Log.d(TAG, "handleMessage: msgParts - " + msgParts[0] + ", " + msgParts[1] + "," + msgParts[2]);

                    if(msgParts[0].equals("RECORD")){
                        Log.d(TAG, "handleMessage: HIT RECORD");
                        if(msgParts[1].equals("START")){
                            Log.d(TAG, "handleMessage: HIT START");

                            startTimer = new Timer();
                            stopTimer = new Timer();

                            startRecordingTask = new StartRecordingTask(MainActivity.this);
                            stopRecordingTask = new StopRecordingTask(MainActivity.this);

                            long stopDelay = 15000;

                            Timer timer = new Timer();
                            long timeToStartValue = Long.parseLong(msgParts[2]);
                            long timeToStopValue = timeToStartValue + stopDelay;
                            Date timeToStart = new Date(timeToStartValue);
                            Date timeToStop = new Date(timeToStopValue);

                            Log.d("SyncFix", "TimeToStart: " + timeToStart.getTime() + ", TimeToStop: " + timeToStop.getTime());

                            startTimer.schedule(startRecordingTask, timeToStart);
                            stopTimer.schedule(stopRecordingTask, timeToStop);

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    recordButton.setEnabled(false);
                                    stopButton.setEnabled(true);
                                }
                            });
                        }

                        else if (msgParts[1].equals("STOP")){
                            stopRecording();
                        }
                    }

                    else if(msgParts[0].equals("SEND")) {
                        if(msgParts[1].equals("RSKEY")){

                            RSKeyGenerator rsKeyGen = new RSKeyGenerator();
                            String receivedKey = msgParts[2];

                            String message;
                            try {
                                message = rsKeyGen.KeyDecoder(currentDevice.getFingerprint(), receivedKey);
                                MainActivity.this.sendMessage("SEND|ACK");

                                Intent intent = new Intent(getBaseContext(), KeyExchangeActvityReceiver.class);
                                intent.putExtra("RSCODEBASE64", receivedKey);
                                intent.putExtra("RESULT", message);

                            } catch(Exception e){
                                MainActivity.this.sendMessage("SEND|NACK");

                                Intent intent = new Intent(getBaseContext(), KeyExchangeActvityReceiver.class);
                                intent.putExtra("RSCODEBASE64", receivedKey);
                                intent.putExtra("RESULT", "Device authentication unsuccessful.");
                            }
                        }

                        else if(msgParts[1].equals("ACK")){
                            Intent intent = new Intent(getBaseContext(), KeyExchangeActivity.class);
                            intent.putExtra("MESSAGE", currentDevice.getMsgKeyPair().getMsg());
                            intent.putExtra("RSCODEB64", currentDevice.getMsgKeyPair().getKey());
                            intent.putExtra("RESULT", "Success");

                            setStatusText("Devices authenticated.");

                            startActivity(intent);
                        }

                        else if(msgParts[1].equals("NACK")){
                            Intent intent = new Intent(getBaseContext(), KeyExchangeActivity.class);
                            intent.putExtra("MESSAGE", currentDevice.getMsgKeyPair().getMsg());
                            intent.putExtra("RSCODEB64", currentDevice.getMsgKeyPair().getKey());
                            intent.putExtra("RESULT", "Failure");

                            setStatusText("Key exchange failed.");

                            startActivity(intent);
                        }
                    }

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

           // Log.i(TAG, "onSensorChanged: Sensor working!" + sensorEvent.sensor.toString());

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

                //Log.i(TAG, "onSensorChanged: Accelerometer values - " + x + ", " + y + ", " + "z");
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

               // Log.i(TAG, "onSensorChanged: Gyroscope values - " + x + ", " + y + ", " + z);
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

    private void keyDerivationSetState(int state){
        if(state == KD_FIRST_STATE){
            deriveButton.setEnabled(false);
            authButton.setEnabled(false);
            setStatusText("Record to begin.");
        }

        if(state == KD_SECOND_STATE){
            deriveButton.setEnabled(true);
            authButton.setEnabled(false);
            setStatusText("Key ready to be derived.");
        }

        if(state == KD_THIRD_STATE){
            deriveButton.setEnabled(false);
            authButton.setEnabled(true);
            setStatusText("Ready for Key Exchange.");
        }
    }

    private static class StartRecordingTask extends TimerTask {

        private MainActivity mainActivity;

        public StartRecordingTask(MainActivity _activity){
            mainActivity = _activity;
        }

        public void run(){
            mainActivity.startRecording();
            mainActivity.cancelStartTimer();
        }
    }

    private class StopRecordingTask extends TimerTask {

        private MainActivity mainActivity;

        public StopRecordingTask(MainActivity _activity){
            mainActivity = _activity;
        }

        public void run(){
            mainActivity.stopRecording();
            mainActivity.cancelStopTimer();
        }
    }
}
