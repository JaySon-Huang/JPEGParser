package com.jayson.utils.jpeg;

import com.jayson.utils.Historgram;

import java.util.ArrayList;
import java.util.List;

/**
 * Package : com.jayson.utils.jpeg
 * Author  : JaySon
 * Date    : 8/17/14
 */
public class JPEGDataUnits {
    private List<int[]> mUnits;

    public JPEGDataUnits(){
        mUnits = new ArrayList<int[]>();
    }

    public void add(int[] unit) {
        mUnits.add(unit);
    }

    public int[] getUnit(int loc){
        return mUnits.get(loc);
    }

    public List<int[]> allUnits(){
        return mUnits;
    }

    public int numOfUnits(){
        return mUnits.size();
    }

    public void clear(){
        mUnits.clear();
    }

    public Historgram[] getHistorgram(){
        Historgram[] ret = new Historgram[64];
        for (int i = 0; i != 64; ++i){
            ret[i] = new Historgram();
        }
        for (int[] unit : mUnits){
            for (int i = 0; i != 64; ++i){
                ret[i].addN( unit[i] );
            }
        }
        return ret;
    }
}
