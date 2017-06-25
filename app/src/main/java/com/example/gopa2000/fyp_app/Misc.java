package com.example.gopa2000.fyp_app;

import java.util.ArrayList;

/**
 * Created by gopa2000 on 6/19/17.
 */

public class Misc {
    public static String padZero(String num){
        if(num.length() == 1)
            return '0' + num;
        else
            return num;
    }

    public static Double avg(ArrayList<Double> input){
        if(!input.isEmpty()){
            Double sum = 0.0;
            for(Double ele:input){
                sum += ele;
            }

            return (Double)(sum/input.size());
        }

        return 0.0;
    }

    public static int nextPowerOf2(final int a)
    {
        int b = 1;
        while (b < a)
        {
            b = b << 1;
        }
        return b;
    }

    public static ArrayList<Double> covertToRadix2(ArrayList<Double> input){
        ArrayList<Double> result = new ArrayList<>();

        int n = input.size();
        int closestPowerOfTwo = nextPowerOf2(n);

        result = input;
        while(n < closestPowerOfTwo){
            result.add(0.0);
            n++;
        }

        return result;
    }

    public static double[] toPrimitive(ArrayList<Double> array) {
        if (array == null) {
            return null;
        } else if (array.size() == 0) {
            return new double[1];
        }
        final double[] result = new double[array.size()];
        for (int i = 0; i < array.size(); i++) {
            result[i] = array.get(i);
        }
        return result;
    }

    public static Complex[] createComplexArray(double[] real, double[] imag){
        int n=real.length;
        Complex[] result = new Complex[n];

        for(int i=0; i<n; i++){
            result[i] = new Complex(real[i], imag[i]);
        }

        return result;
    }
}
