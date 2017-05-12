package com.example.gopa2000.fyp_app;

/**
 * Created by gopa2000 on 4/12/17.
 */

public class AccelData {
    private double x;
    private double y;
    private double z;

    private long timestamp;

    public AccelData(long ts, double xv, double yv, double zv){
        this.timestamp = ts;

        this.x = xv;
        this.y = yv;
        this.z = zv;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
