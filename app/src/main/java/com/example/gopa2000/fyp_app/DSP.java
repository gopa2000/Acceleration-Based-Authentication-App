package com.example.gopa2000.fyp_app;

import java.util.ArrayList;

/**
 * Created by gopa2000 on 6/16/17.
 */

public class DSP {

    public static ArrayList<Double> filter(double[] b, double[] a, ArrayList<Double> x){
        ArrayList<Double> filter = new ArrayList<>();
        double[] a1 = getRealArrayScalarDiv(a, a[0]);
        double[] b1 = getRealArrayScalarDiv(b, a[0]);

        int sx = x.size();
        filter.set(0, b1[0] * x.get(0));

        for(int i=1; i<sx; i++){
            filter.set(i, 0.0);
            for(int j=0; j<=i; j++){
                int k = i-j;
                if(j > 0){
                    if ((k < b1.length) && (j < sx)){
                        filter.set(i, filter.get(i) + b1[k] * x.get(j));
                    }

                    if ((k < filter.size()) && (j < a1.length)){
                        filter.set(i, filter.get(i) - (a1[j] * filter.get(k)));
                    }
                }
                else {
                    if ((k < b1.length) && (j < sx)){
                        filter.set(i, filter.get(i) + (b1[k] * x.get(j)));
                    }
                }
            }
        }

        for (int i=0; i<sx; i++){
            Double val = filter.get(i);
            filter.set(i, (double) Math.round(val * 10000d)/ 10000d);
        }

        return filter;
    }

    public static double[] getRealArrayScalarDiv(double[] dDividend, double dDivisor) {
        if (dDividend == null) {
            throw new IllegalArgumentException("The array must be defined or diferent to null");
        }
        if (dDividend.length == 0) {
            throw new IllegalArgumentException("The size array must be greater than Zero");
        }
        double[] dQuotient = new double[dDividend.length];

        for (int i = 0; i < dDividend.length; i++) {
            if (!(dDivisor == 0.0)) {
                dQuotient[i] = dDividend[i]/dDivisor;
            } else {
                if (dDividend[i] > 0.0) {
                    dQuotient[i] = Double.POSITIVE_INFINITY;
                }
                if (dDividend[i] == 0.0) {
                    dQuotient[i] = Double.NaN;
                }
                if (dDividend[i] < 0.0) {
                    dQuotient[i] = Double.NEGATIVE_INFINITY;
                }
            }
        }
        return dQuotient;
    }

}
