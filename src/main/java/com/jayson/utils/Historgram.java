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
     * 对直方图进行平移,空出左基准-1,右基准+1,留出存储额外信息的空间
     * @param left  左基准
     * @param right 右基准
     */
    public void shift(int left, int right){
        // FIXME: 实现之
        int[] z = findZ(left, right);
        System.out.println(String.format("z1, z2:%3d, %3d", z[0], z[1]));
        System.out.print("Before shift:");this.print(System.out);

        // z[0] 左边的全部左移
        for (int i = -512+1; i != z[0]; ++i){
            if (mHistorgram.containsKey(i)){
                mHistorgram.put(i-1, mHistorgram.get(i));
            }
        }
        // z[1] 右边的全部右移
        for (int i = 511; i != z[1]; --i){
            if (mHistorgram.containsKey(i)){
                mHistorgram.put(i+1, mHistorgram.get(i));
            }
        }

        System.out.print("After shift:");this.print(System.out);
    }

    public void unshift(){
        // FIXME: 实现之
    }

    private int[] findZ(int left, int right){
        int[] z = new int[2];
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
        return z;
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
