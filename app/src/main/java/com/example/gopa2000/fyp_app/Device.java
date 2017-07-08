package com.example.gopa2000.fyp_app;

import android.hardware.Sensor;
import android.util.Log;

import java.lang.reflect.Array;
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
    private String fingerprint;
    private int numFrames = 21;
    private int numBands = 9;

    public Device(String fn, SensorCombo input){

        Log.d(TAG, "Device: Constructor called.");

        this.fileName = fn;
        this.rawData = input;
        this.frameMatrix = new ArrayList<>();
        this.frameMatrixFFT = new ArrayList<>();
        this.frame = new ArrayList<>();
        this.rmsUnfiltered = new ArrayList<>();
        init();
    }

    private void init(){

        Log.d(TAG, "init: init() function called.");

        this.fs = calculateSamplingRate();
        generateFrameMatrix();
        generateFrameMatrixFFT();
        generateFFTBands();
        printFinalFrames(1);
        generateKey();
    }

    private Double calculateSamplingRate(){
        ArrayList<String> logData = rawData.getLogData();
        int L = logData.size();

        DateFormat formatter = new SimpleDateFormat("HH mm ss ");

        try {
            Date Tmin = formatter.parse(logData.get(0));
            Date Tmax = formatter.parse(logData.get(L-1));

            long seconds = (Tmax.getTime() - Tmin.getTime()) / 1000;

            Log.d(TAG, "calculateSamplingRate: Sampling rate calculated - " + (double)(L/seconds));

            return (double)(L/seconds);
        } catch (Exception e){
            Log.e(TAG, "calculateSamplingRate: Parsing error", e);
        }

        return -1.0;
    }

    private void generateFrameMatrix(){

        Log.d(TAG, "generateFrameMatrix: Generating frameMatrix");

        ArrayList<AccelData> accData = rawData.getAccData();

        for(AccelData acc:accData){
            Double rmsVal = sqrt(acc.getX()*acc.getX() + acc.getY()*acc.getY() + acc.getZ()*acc.getZ());
            this.rmsUnfiltered.add(rmsVal);
        }

        Log.d(TAG, "generateFrameMatrix: " + statistics());

        // generates rmsFiltered. Look definition for details
        filterData();

        // Frame Matrix Generation
        int n=rmsFiltered.size();
        int m=Math.round(n/numFrames) + 1;
        int i=0, j=0, k=0;

        frameMatrix.add(new ArrayList<Double>());
        for(i=0; i<n; i++){
            if(i!=0 && ((i % m) == 0)){
                frameMatrix.add(new ArrayList<Double>());
                j++;
            }
            frameMatrix.get(j).add(rmsFiltered.get(i));
        }

        Log.d(TAG, "generateFrameMatrix: FrameMatrix generation complete - " + statistics());
    }

    private void generateFrameMatrixFFT(){

        Log.d(TAG, "generateFrameMatrixFFT: Generating frameMatrixFFT");

        // Hamming window
        HammingWindow hw = new HammingWindow();
        for(ArrayList<Double> arr:frameMatrix){
            hw.apply(arr);
        }

        /** FFT
            couple things done here before actually applying fft
            1. pad array to closest power of two with 0s
            2. create double array initialised to 0 for imaginary part
            3. convert all values of frame to Complex for fft

            for fft and normalisation
            4. call fft()
            5. take abs value, divide it by length of the frame
            6. push to arraylist
            7. normalise by dividing each node by max value
            8. push to frameMatrixFFT
         ***/

        for(ArrayList fil:frameMatrix){
            ArrayList x = fil;
            x = Misc.covertToRadix2(fil);
            double[] real = Misc.toPrimitive(x);
            double[] imag = new double[real.length];
            java.util.Arrays.fill(imag, 0.0);

            Complex[] frame = Misc.createComplexArray(real, imag);
            Complex[] frameFFT = DSP.fft(frame);

            ArrayList<Double> frameFFTValues = new ArrayList<>();
            Double maxVal = 0.0;
            for(int i=0; i< frameFFT.length/2; i++){
                Double abs = frameFFT[i].abs();
                Double norm = abs/frameFFT.length;

                maxVal = norm > maxVal ? norm : maxVal;
                frameFFTValues.add(norm);
            }

            for(int i=0; i<frameFFTValues.size(); i++){
                frameFFTValues.set(i, frameFFTValues.get(i)/maxVal);
            }

            //Log.d(TAG, "generateFrameMatrixFFT: frameFFTValuesSize - " + frameFFTValues.size());
            frameMatrixFFT.add(frameFFTValues);
        }

        Log.d(TAG, "generateFrameMatrixFFT: frameMatrixFFT generated - " + statistics());
    }

    private void generateFFTBands(){
        Log.d(TAG, "generateFFTBands: generating FFTBands...");

        for(ArrayList<Double> _frame:frameMatrixFFT) {
            this.frame.add(new Frame());
            ArrayList<ArrayList<Double>> current = new ArrayList<>();

            int n = _frame.size();
            int m = Math.round(n/numBands) + 1;

            current.add(new ArrayList<Double>());
            int j = 0;

            //Log.d(TAG, "generateFFTBands: frameSize - " + n);
            for(int i=0; i<n; i++){
                if(i!=0 && (i % m == 0)){
                    current.add(new ArrayList<Double>());
                    j++;
                }

                current.get(j).add(_frame.get(i));
            }

            this.frame.get(this.frame.size() - 1).setBands(current);
        }

        Log.d(TAG, "generateFFTBands: FFTBands generated - " + statistics());
    }

    // Data filtering method - modify this alone for data filtering and normalisation
    private void filterData(){
        Log.d(TAG, "filterData: Filtering data");

        double windowSize = 30.0;

        double[] b = new double[(int)windowSize];
        double[] a = {1};

        for(int i=0; i<windowSize; i++){
            b[i] = 1.0/windowSize;
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

    private void generateKey(){
        Log.d(TAG, "generateKey: generating key...");

        String _key = "";
        for(int i=1; i<numFrames; i++){
            for(int j=0; j<numBands-1; j++){
                _key += bitFunc(i,j);
            }
        }

        this.fingerprint = _key;
        Log.d(TAG, "generateKey: key generated - " + this.fingerprint);
    }

    private String bitFunc(int n, int m){
        double relation = E(n,m) - E(n,m+1) - (E(n-1,m) - E(n-1, m+1));

        if(relation > 0)
            return "1";
        else
            return "0";
    }


    public double E(int n, int m){
        ArrayList<Double> S = frame.get(n).getBand(m);
        int L = S.size();
        double energy = 0;

        for(Double s:S){
            energy = energy + s*s;
        }

        energy = (1.0/L) * energy;
        return energy;
    }

    public String getFingerprint(){
        return this.fingerprint;
    }

    private String statistics(){
        int frameMatrixFrames = this.frameMatrix.size();
        int frameMatrixFramesFFT = this.frameMatrixFFT.size();

        Double avgFrameSize = 0.0;
        for(ArrayList<Double> a:this.frameMatrix){
            avgFrameSize += a.size();
        }
        if(frameMatrixFrames > 0)
            avgFrameSize = avgFrameSize/frameMatrixFrames;

        Double avgFrameSizeFFT = 0.0;
        for(ArrayList<Double> a:this.frameMatrixFFT){
            avgFrameSizeFFT += a.size();
        }
        if(frameMatrixFramesFFT > 0)
            avgFrameSizeFFT = avgFrameSizeFFT/frameMatrixFramesFFT;

        int numberOfFrames = frame.size();
        Double avgNumOfBands = 0.0;

        for(Frame f:this.frame){
            avgNumOfBands += f.getBands().size();
        }
        if(numberOfFrames > 0)
            avgNumOfBands = avgNumOfBands/numberOfFrames;

        String result = "Number of frames in FrameMatrix = " + frameMatrixFrames +
                ", Average size of each frame in FrameMatrix = " + avgFrameSize +
                ", Number of frames in FrameMatrixFFT = " + frameMatrixFramesFFT +
                ", Average size of each frame in FrameMatrixFFT = " + avgFrameSizeFFT +
                ", Average number of bands in each frame = " + avgNumOfBands;

        return result;
    }

    private void printFinalFrames(){
        int frameCounter = 0;
        String result = "";
        for(Frame f:frame){
            result = result + "Frame #" + frameCounter + "\n";
            Log.d(TAG, "printFinalFrames: Frame #" + frameCounter);
            for(int i=0; i<numBands; i++) {
                ArrayList<Double> current = f.getBand(i);
                result = result + "Band #" + i + ": ";
                String tmp = "";
                for(Double d:current){
                    result = result + " " + d.toString();
                    tmp = tmp + " " + d.toString();
                }
                Log.d(TAG, "printFinalFrames: Band #" + i + ": "+ tmp);
                result += "\n";
            }
            result += "\n";
        }

        Log.d(TAG, "printFinalFrames: " + result);
    }

    private void printFinalFrames(int n ){
        int frameCounter = 0;
        String result = "";
        for(Frame f:frame){
            ArrayList<ArrayList<Double>> current = f.getBands();
            result = result + "Number of bands in frame #" + frameCounter + ": "+ current.size() + "\n";
            frameCounter++;
        }

        Log.d(TAG, "printFinalFrames: " + result);
    }

    private class Frame {
        private ArrayList<ArrayList<Double>> band;
        public ArrayList<Double> getBand(int index){
            return band.get(index);
        }
        public void setBands(ArrayList<ArrayList<Double>> _band){
            this.band = _band;
        }
        public ArrayList<ArrayList<Double>> getBands(){
            return band;
        }
    }
}
