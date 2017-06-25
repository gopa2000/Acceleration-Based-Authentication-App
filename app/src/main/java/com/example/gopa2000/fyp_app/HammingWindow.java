package com.example.gopa2000.fyp_app;

/**
 * Created by gopa2000 on 6/23/17.
 */

public class HammingWindow extends WindowFunction {
    /** Constructs a Hamming window. */
    public HammingWindow() {
    }

    protected float value(int length, int index) {
        return 0.54f - 0.46f * (float) Math.cos(TWO_PI * index / (length - 1));
    }
}
