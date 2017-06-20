package com.example.gopa2000.fyp_app;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class SensorCombo {

    private String TAG = this.getClass().toString();

    private ArrayList<AccelData> accData;
    private ArrayList<GyroData> gyroData;
    private ArrayList<String> logData;

    public SensorCombo(){
        accData = new ArrayList<AccelData>();
        gyroData = new ArrayList<GyroData>();
        logData = new ArrayList<String>();
    }

    public void addToAcc(AccelData a){
        accData.add(a);
    }

    public void addToGyr(GyroData g){
        gyroData.add(g);
    }

    public void saveToFile(String filename){
        try {
            String path = Environment.getExternalStorageDirectory().toString() + File.separator + "fyp";
            File directory = new File(path);
            directory.mkdirs();

            File outputFile = new File(directory, filename);
            outputFile.createNewFile();

            FileOutputStream fos = new FileOutputStream(outputFile);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));

            for(String s:logData){
                bw.write(s);
                bw.newLine();
            }

            bw.close();
        }

        catch (Exception E){
            E.printStackTrace();
        }

    }

    public void generateTestFile(){
        int accLength = accData.size();
        int gyrLength = gyroData.size();

        int i=0, j=0;

        int n = (accLength > gyrLength) ? accLength : gyrLength;

        while(i < accLength && j < gyrLength){
            if(accData.get(i).getTimestamp() == gyroData.get(j).getTimestamp()){
                long currentTimestamp = accData.get(i).getTimestamp();

                Date date = new Date(currentTimestamp);
                DateFormat formatter = new SimpleDateFormat("HH mm ss ");
                String formattedTS = formatter.format(currentTimestamp);

                StringBuilder sb = new StringBuilder();
                sb.append(formattedTS);
                sb.append(accData.get(i).getX() + " ");
                sb.append(accData.get(i).getY() + " ");
                sb.append(accData.get(i).getZ() + " ");
                sb.append(gyroData.get(j).getX() + " ");
                sb.append(gyroData.get(j).getY() + " ");
                sb.append(gyroData.get(j).getZ() + " ");

                logData.add(sb.toString());
                Log.i(TAG, "generateTestFile: " + sb.toString());

                i++; j++;
            }

            else if(accData.get(i).getTimestamp() < gyroData.get(j).getTimestamp()){
                i++;
            }

            else {
                j++;
            }
        }
    }

    public void generateAccTestFile(){
        int accLength = accData.size();
        int gyrLength = gyroData.size();

        int i=0, j=0;

        int n = (accLength > gyrLength) ? accLength : gyrLength;

        while(i < accLength) {
            long currentTimestamp = accData.get(i).getTimestamp();

            Date date = new Date(currentTimestamp);
            DateFormat formatter = new SimpleDateFormat("HH mm ss ");
            String formattedTS = formatter.format(currentTimestamp);

            StringBuilder sb = new StringBuilder();
            sb.append(formattedTS);
            sb.append(accData.get(i).getX() + " ");
            sb.append(accData.get(i).getY() + " ");
            sb.append(accData.get(i).getZ() + " ");
            sb.append("0" + " ");
            sb.append("0" + " ");
            sb.append("0" + " ");

            logData.add(sb.toString());
            Log.i(TAG, "generateTestFile: " + sb.toString());

            i++;
        }
    }

    public ArrayList<AccelData> getAccData() {
        return accData;
    }

    public void setAccData(ArrayList<AccelData> accData) {
        this.accData = accData;
    }

    public ArrayList<GyroData> getGyroData() {
        return gyroData;
    }

    public void setGyroData(ArrayList<GyroData> gyroData) {
        this.gyroData = gyroData;
    }

    public ArrayList<String> getLogData() {
        return logData;
    }

    public void setLogData(ArrayList<String> logData) {
        this.logData = logData;
    }
}
