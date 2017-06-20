package com.example.gopa2000.fyp_app;

import android.hardware.Sensor;
import android.util.Log;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

import com.example.gopa2000.fyp_app.Misc;

import static java.lang.Math.sqrt;

/**
 * Created by gopa2000 on 6/19/17.
 */

public class Device {
    /* Hold all the prickly device data */

    static String TAG = "Device";

    private String fileName;
    private SensorCombo rawData;
    private ArrayList<Double> rmsUnfiltered;
    private ArrayList<Double> rmsFiltered;

    private double fs;

    private ArrayList<ArrayList<Double>> frameMatrix;
    private ArrayList<ArrayList<Double>> frameMatrixFFT;
    private ArrayList<Frame> frame;

    // Key specifics
    private String key;
    private int numFrames = 9;
    private int numBands = 21;

    public Device(String fn, SensorCombo input){
        this.fileName = fn;
        this.rawData = input;
        init();
    }

    private void init(){
        this.fs = calculateSamplingRate();
        generateFrameMatrix();
    }

    private Double calculateSamplingRate(){
        ArrayList<String> logData = rawData.getLogData();
        int L = logData.size();

        DateFormat formatter = new SimpleDateFormat("HH mm ss ");

        try {
            Date Tmin = formatter.parse(logData.get(0));
            Date Tmax = formatter.parse(logData.get(L-1));

            long seconds = (Tmax.getTime() - Tmin.getTime()) / 1000;
            return (double)(L/seconds);

        } catch (Exception e){
            Log.e(TAG, "calculateSamplingRate: Parsing error", e);
        }

        return -1.0;
    }

    private void generateFrameMatrix(){
        ArrayList<AccelData> accData = rawData.getAccData();

        for(AccelData acc:accData){
            Double rmsVal = sqrt(acc.getX()*acc.getX() + acc.getY()*acc.getY() + acc.getZ()*acc.getZ());
            rmsUnfiltered.add(rmsVal);
        }

        // generates rmsFiltered. Look definition for details
        filterData();

        // Frame Matrix Generation
        int n=rmsFiltered.size();
        int m=Math.round(n/numFrames) + 1;
        int i=0, j=0, k=0;

        frameMatrix.add(new ArrayList<Double>());
        for(i=0; i<n; i++){
            if((i % m) == 0){
                frameMatrix.add(new ArrayList<Double>());
                j++;
            }
            frameMatrix.get(j).add(rmsFiltered.get(i));
        }
    }

    // Data filtering method - modify this alone for data filtering and normalisation
    private void filterData(){
        double windowsize = 30.0;

        double[] b = new double[(int)windowsize];
        double[] a = {1};

        for(int i=0; i<windowsize; i++){
            b[i] = 1.0/windowsize;
        }

        this.rmsFiltered = DSP.filter(b, a, rmsUnfiltered);

        Double max = Collections.max(this.rmsFiltered);
        for(int i=0; i<rmsFiltered.size(); i++){
            Double newVal = rmsFiltered.get(i)/max;
            rmsFiltered.set(i, newVal);
        }

        Double average = Misc.avg(this.rmsFiltered);
        for(int i=0; i<rmsFiltered.size(); i++){
            Double newVal = rmsFiltered.get(i) - average;
            rmsFiltered.set(i, newVal);
        }
    }

    private class Frame {
        private ArrayList<ArrayList<Double>> band;
    }
}
