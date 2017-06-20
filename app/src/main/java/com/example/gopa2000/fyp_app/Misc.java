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
}
