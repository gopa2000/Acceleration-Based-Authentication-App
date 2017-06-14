package com.example.gopa2000.fyp_app;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Created by gopa2000 on 6/12/17.
 */

public class BluetoothManager {
    private static final String TAG = "BTManager";

    // Name for SDP record when creating BT socket
    private static final String NAME = "FYP";

    // Unique UUID
    private static final UUID COM_UUID = UUID.fromString("0001101-0000-1000-8000-00805F9B34FB");

    private final BluetoothAdapter bluetoothAdapter;
    private final Handler handler;
    private AcceptThread acceptThread;
    private ConnectThread connectThread;
    private ConnectedThread connectedThread;
    private int state;
    private BluetoothDevice device;
    boolean deviceConnection;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0; // doing nothing
    public static final int STATE_LISTEN = 1; // listening for incoming
    public static final int STATE_CONNECTING = 2; // initiating an outgoing
    public static final int STATE_CONNECTED = 3; // connected to a remote
    public static final int STATE_SEND = 4; // sending message

    public BluetoothManager(Context context, Handler handler){
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        state = STATE_NONE;
        this.handler = handler;
    }

    public synchronized void setState(int new_state){
        if(true)
            Log.d(TAG, "setState: " + state + " -> " + new_state);
        state = new_state;
        handler.obtainMessage(MainActivity.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    public synchronized int getState(){
        return state;
    }

    public synchronized void start() {
        if(true)
            Log.d(TAG, "start");

        if(connectThread != null){
            connectThread.cancel();
            connectThread = null;
        }

        if(connectedThread != null){
            connectedThread.cancel();
            connectedThread = null;
        }

        setState(STATE_LISTEN);
        if(acceptThread == null){
            acceptThread = new AcceptThread();
            acceptThread.start();
        }
    }

    public synchronized void connect(BluetoothDevice new_device){
        device = new_device;
        if(true){
            Log.d(TAG, "connect: " + device);
        }

        if(state == STATE_CONNECTING){
            if(connectThread != null){
                connectThread.cancel();
                connectThread = null;
            }
        }

        if(connectedThread != null){
            connectedThread.cancel();
            connectedThread = null;
        }

        connectThread = new ConnectThread(device);
        connectThread.start();
        setState(STATE_CONNECTING);
    }

    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device, final String socketType){
        if(true)
            Log.d(TAG, "connected: Socket type - " + socketType);

        if(connectThread != null){
            connectThread.cancel();
            connectThread = null;
        }

        if(connectedThread != null){
            connectedThread.cancel();
            connectedThread = null;
        }

        if(acceptThread != null){
            acceptThread.cancel();
            acceptThread = null;
        }

        connectedThread = new ConnectedThread(socket, socketType);
        connectedThread.start();

        deviceConnection = true;

        // send name of device to UI activity
        Message msg = handler.obtainMessage(MainActivity.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        handler.sendMessage(msg);

        setState(STATE_CONNECTED);
    }

    public synchronized void stop() {
        if (true)
            Log.d(TAG, "stop");

        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        if (acceptThread != null) {
            acceptThread.cancel();
            acceptThread = null;
        }
        setState(STATE_NONE);
    }

    public void write(byte[] out){
        ConnectedThread r;

        synchronized (this) {
            if (state != STATE_CONNECTED)
                return;
            r = connectedThread;
        }

        r.write(out);
    }

    private void connectionFailed() {
        // Send a failure message back to the Activity
        Message msg = handler.obtainMessage(MainActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.TOAST, "Unable to connect device");
        msg.setData(bundle);
        handler.sendMessage(msg);

        // Start the service over to restart listening mode
        BluetoothManager.this.start();
    }

    private void connectionLost() {
        // Send a failure message back to the Activity
        Message msg = handler.obtainMessage(MainActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.TOAST, "Device connection was lost");
        msg.setData(bundle);
        handler.sendMessage(msg);
        deviceConnection=false;
        setState(STATE_LISTEN);
        // Start the service over to restart listening mode
        BluetoothManager.this.start();
        //Timer to reconnect broken bluetooth linked device
        Thread thread = new Thread()
        {

            public void run() {
                Log.i("tag1", "Connection Lost Reached here");
                int count1 = 0;
                while (true){

                    if (deviceConnection) {
                        Log.i("tag", "Break");
                        break;
                    }
                    count1=count1+1;
                    Log.i("Count: ", Integer.toString(count1));
                    try
                    {
                        Log.i("tag1", "connection lost Reached");
                        connect(device);
                        Thread.sleep(7000); // 20 second
                    } catch (Exception e)
                    {
                        e.printStackTrace();
                    }

                }
            }
        };
        thread.start();
    }

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket serverSocket;
        private String socketType;

        public AcceptThread(){
            BluetoothServerSocket tmp = null;

            try {
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, COM_UUID);
            } catch (Exception e){
                Log.e(TAG, "Socket Type: " + socketType + "listen() failed", e);
            }

            serverSocket = tmp;
        }

        public void run(){
            if(true){
                Log.d(TAG, "run: SocketType " + socketType + " BEGIN AcceptThread " + this);
            }

            setName("AcceptThread"+socketType);
            BluetoothSocket socket = null;

            while(state != STATE_CONNECTED){
                try{
                    socket = serverSocket.accept();
                } catch(IOException e){
                    Log.e(TAG, "SocketType + " + socketType + " accept() failed", e);
                    break;
                }

                if(socket != null){
                    synchronized (BluetoothManager.this){
                        switch(state){
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                connected(socket, socket.getRemoteDevice(), socketType);
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                try {
                                    socket.close();
                                } catch (IOException e){
                                    Log.e(TAG, "run: could not close unwanted socket", e);
                                }
                                break;
                        }
                    }
                }
            }

            if(true){
                Log.i(TAG, "END AcceptThread, socket type: " + socketType);
            }
        }

        public void cancel(){
            if(true){
                Log.d(TAG, "Socket type " + socketType + " cancel " + this);
            }

            try {
                serverSocket.close();
            } catch (IOException e){
                Log.e(TAG, "Socket type +" + socketType + " close() server failed", e);
            }
        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket socket;
        private final BluetoothDevice device;

        private String socketType;

        public ConnectThread(BluetoothDevice new_device){
            device = new_device;
            BluetoothSocket tmp =  null;

            try {
                tmp = device.createRfcommSocketToServiceRecord(COM_UUID);
            } catch (IOException e){
                Log.e(TAG, "Socket type " + socketType + " create() failed. ", e);
            }

            socket = tmp;
        }

        public void run(){
            Log.i(TAG, "BEGIN ConnectThread Socket type: "+ socketType);
            setName("ConnectThread " + socketType);

            bluetoothAdapter.cancelDiscovery();

            try {
                socket.connect();
            } catch (IOException e){
                // close socket
                try {
                    socket.close();
                } catch (IOException e2){
                    Log.e(TAG, "run: Unable to close socket " + socketType + " socket during connection failure.", e2);
                }
                connectionFailed();
                return;
            }

            synchronized (BluetoothManager.this){
                connectThread = null;
            }

            connected(socket, device, socketType);
        }

        public void cancel(){
            try {
                socket.close();
            } catch (IOException e){
                Log.e(TAG, "close() of connect " + socketType + " socket failed", e);
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket socket;
        private final InputStream inStream;
        private final OutputStream outStream;

        public ConnectedThread(BluetoothSocket new_socket, String socketType){
            Log.d(TAG, "create ConnectedThread " + socketType);
            socket = new_socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e){
                Log.e(TAG, "tmp socket not created ", e);
            }

            inStream = tmpIn;
            outStream = tmpOut;
        }

        public void run() {
            Log.i(TAG, "BEGIN connectedThread");
            byte[] buffer = new byte[1024];
            int bytes;

            //keep listening to inputStream while connected
            while(true){
                try {
                    bytes = inStream.read(buffer);

                    // send to handler
                    handler.obtainMessage(MainActivity.MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                } catch (IOException e) {
                    Log.e(TAG, "disconnected ", e);
                    connectionLost();

                    BluetoothManager.this.start();
                    break;
                }
            }
        }

        public void write(byte[] buffer){
            try {
                outStream.write(buffer);
                handler.obtainMessage(MainActivity.MESSAGE_WRITE, -1, -1, buffer).sendToTarget();
                setState(STATE_SEND);
                setState(STATE_CONNECTED);
            } catch(IOException e){
                Log.e(TAG, "Exception during write.", e);
            }
        }

        public void cancel() {
            try {
                socket.close();
            } catch (IOException e){
                Log.e(TAG, "close() of connect socket failed.", e);
            }
        }
    }
}
