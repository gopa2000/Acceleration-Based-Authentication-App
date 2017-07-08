package com.example.gopa2000.fyp_app;

import android.util.Log;

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

        Log.d("Device ", "filter: array size check - b: " + b.length + ", a: " + a.length + ", x: " + x.size() + ", x first value - " + x.get(0));

        while(filter.size() < sx) filter.add(0.0);
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

    // compute the FFT of x[], assuming its length is a power of 2
    public static Complex[] fft(Complex[] x) {
        int n = x.length;

        // base case
        if (n == 1) return new Complex[] { x[0] };

        // radix 2 Cooley-Tukey FFT
        if (n % 2 != 0) { throw new RuntimeException("n is not a power of 2"); }

        // fft of even terms
        Complex[] even = new Complex[n/2];
        for (int k = 0; k < n/2; k++) {
            even[k] = x[2*k];
        }
        Complex[] q = fft(even);

        // fft of odd terms
        Complex[] odd  = even;  // reuse the array
        for (int k = 0; k < n/2; k++) {
            odd[k] = x[2*k + 1];
        }
        Complex[] r = fft(odd);

        // combine
        Complex[] y = new Complex[n];
        for (int k = 0; k < n/2; k++) {
            double kth = -2 * k * Math.PI / n;
            Complex wk = new Complex(Math.cos(kth), Math.sin(kth));
            y[k]       = q[k].plus(wk.times(r[k]));
            y[k + n/2] = q[k].minus(wk.times(r[k]));
        }
        return y;
    }


    // compute the inverse FFT of x[], assuming its length is a power of 2
    public static Complex[] ifft(Complex[] x) {
        int n = x.length;
        Complex[] y = new Complex[n];

        // take conjugate
        for (int i = 0; i < n; i++) {
            y[i] = x[i].conjugate();
        }

        // compute forward FFT
        y = fft(y);

        // take conjugate again
        for (int i = 0; i < n; i++) {
            y[i] = y[i].conjugate();
        }

        // divide by n
        for (int i = 0; i < n; i++) {
            y[i] = y[i].scale(1.0 / n);
        }

        return y;

    }

    // compute the circular convolution of x and y
    public static Complex[] cconvolve(Complex[] x, Complex[] y) {

        // should probably pad x and y with 0s so that they have same length
        // and are powers of 2
        if (x.length != y.length) { throw new RuntimeException("Dimensions don't agree"); }

        int n = x.length;

        // compute FFT of each sequence
        Complex[] a = fft(x);
        Complex[] b = fft(y);

        // point-wise multiply
        Complex[] c = new Complex[n];
        for (int i = 0; i < n; i++) {
            c[i] = a[i].times(b[i]);
        }

        // compute inverse FFT
        return ifft(c);
    }


    // compute the linear convolution of x and y
    public static Complex[] convolve(Complex[] x, Complex[] y) {
        Complex ZERO = new Complex(0, 0);

        Complex[] a = new Complex[2*x.length];
        for (int i = 0;        i <   x.length; i++) a[i] = x[i];
        for (int i = x.length; i < 2*x.length; i++) a[i] = ZERO;

        Complex[] b = new Complex[2*y.length];
        for (int i = 0;        i <   y.length; i++) b[i] = y[i];
        for (int i = y.length; i < 2*y.length; i++) b[i] = ZERO;

        return cconvolve(a, b);
    }

    /*
    // display an array of Complex numbers to standard output
    public static void show(Complex[] x, String title) {
        StdOut.println(title);
        StdOut.println("-------------------");
        for (int i = 0; i < x.length; i++) {
            StdOut.println(x[i]);
        }
        StdOut.println();
    }
    */

}
