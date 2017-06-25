package com.example.gopa2000.fyp_app;

import java.util.ArrayList;

/**
 * Created by gopa2000 on 6/23/17.
 */

public abstract class WindowFunction {

        /** The float value of 2*PI. Provided as a convenience for subclasses. */
        protected static final float TWO_PI = (float) (2 * Math.PI);
        protected int length;

        public WindowFunction() {
        }

        /**
         * Apply the window function to a sample buffer.
         *
         * @param samples
         *            a sample buffer
         */
        public void apply(ArrayList<Double> samples) {
            this.length = samples.size();


            for (int n = 0; n < samples.size(); n++) {
                samples.set(n, samples.get(n) * value(samples.size(), n));
            }
        }

        /**
         * Generates the curve of the window function.
         *
         * @param length
         *            the length of the window
         * @return the shape of the window function
         */
        public ArrayList<Double> generateCurve(int length) {
            ArrayList<Double> samples = new ArrayList<>();
            for (int n = 0; n < length; n++) {
                samples.add((double)1f * value(length, n));
            }
            return samples;
        }

        protected abstract float value(int length, int index);
}

