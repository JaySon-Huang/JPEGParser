package com.jayson.utils;

import java.io.PrintStream;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Created by JaySon on 14-8-9.
 */
public class Historgram implements Cloneable{

    private final Comparator<Map.Entry<Integer, Integer>> cmp = new Comparator<Map.Entry<Integer, Integer>>() {
        @Override
        public int compare(Map.Entry<Integer, Integer> lhs, Map.Entry<Integer, Integer> rhs) {
            return rhs.getValue() - lhs.getValue();
        }
    };

    Map<Integer, Integer> mHistorgram;

    public Historgram(){
        mHistorgram = new HashMap<Integer, Integer>();
    }

    public void addN(int n){
        if (mHistorgram.containsKey(n)){
            mHistorgram.put(n, mHistorgram.get(n)+1);
        }else {
            mHistorgram.put(n, 1);
        }
    }

    public void subN(int n){
        assert mHistorgram.containsKey(n);
        mHistorgram.put(n, mHistorgram.get(n)-1);
    }

    public void setN(int n, int num){
        mHistorgram.put(n, num);
    }

    public int getN(int n){
        if (mHistorgram.containsKey(n)){
            return mHistorgram.get(n);
        }else{
            return 0;
        }
    }

    /**
     * 对直方图进行平移，留出存储额外信息的空间
     */
    public void shift(){
        // FIXME: 实现之

    }

    public void unshift(){
        // FIXME: 实现之
    }

    private void findZ(int[] z, int left, int right){
        int pos = left-1;
        while (true){
            if (this.getN(pos) == 0){
                z[0] = pos;
                break;
            }else{
                --pos;
            }
        }

        pos = right + 1;
        while (true){
            if (this.getN(pos) == 0){
                z[1] = pos;
                break;
            }else{
                ++pos;
            }
        }

        // FIXME:
    }

    public void clear(){
        mHistorgram.clear();
    }

    public void print(PrintStream ps){
        ps.print('[');
        PriorityQueue< Map.Entry<Integer, Integer> > q
                = new PriorityQueue< Map.Entry<Integer, Integer> >(mHistorgram.size(), cmp);

        boolean isFirst = true;
        int max_key = 0;
        int min_key = 0;
        for (Map.Entry<Integer, Integer> entry : mHistorgram.entrySet()){
            q.add(entry);
        }
        while ( ! q.isEmpty() ){
            Map.Entry<Integer, Integer> entry = q.poll();
            if (isFirst){
                isFirst = false;
                max_key = min_key = entry.getKey();
            }else{
                max_key = max_key > entry.getKey()?
                        max_key:
                        entry.getKey();
                min_key = min_key < entry.getKey()?
                        min_key:
                        entry.getKey();
            }
            ps.print(String.format("%4d:%4d,", entry.getKey(), entry.getValue()));
        }
        ps.println(']');
        ps.println(String.format("[ %d - %d]", min_key, max_key));
    }

}
