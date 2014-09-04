package com.jayson.utils;

import java.io.PrintStream;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Package : com.jayson.utils
 * Author  : JaySon
 * Date    : 14-8-9
 */
public class Historgram implements Cloneable{

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

    int[] mRange;
    /**
     * @return 得到当前直方图的左右边界.[0]为下界,[1]为上界
     */
    public int[] getRange(){
        if (mRange != null){
            return mRange;
        }
        mRange = new int[2];
        boolean isFirst = true;
        for (Map.Entry<Integer, Integer> entry : mHistorgram.entrySet()) {
            if (isFirst) {
                isFirst = false;
                mRange[0] = mRange[1] = entry.getKey();
            } else {
                mRange[0] = mRange[0] < entry.getKey() ?
                        mRange[0] :
                        entry.getKey();
                mRange[1] = mRange[1] > entry.getKey() ?
                        mRange[1] :
                        entry.getKey();
            }
        }
        return mRange;
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

        // (z[0] , left-1] 区间全部左移
        for (int i = z[0]+1; i != left; ++i){
            if (mHistorgram.containsKey(i)){
                mHistorgram.put(i-1, mHistorgram.get(i));
            }
        }
        mHistorgram.put(left-1,0);

        // [ right+1 , z[1] ) 区间的全部右移
        for (int i = z[1]-1; i != right; --i){
            if (mHistorgram.containsKey(i)){
                mHistorgram.put(i+1, mHistorgram.get(i));
            }
        }
        mHistorgram.put(right+1,0);

        System.out.print("After  shift:");this.print(System.out);
    }

    public void unshift(){
        // FIXME: 实现之
    }

    public int[] findZ(int left, int right){
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

    public void print(){
        this.print(System.out);
    }

    private final Comparator<Map.Entry<Integer, Integer>> cmp = new Comparator<Map.Entry<Integer, Integer>>() {
        @Override
        public int compare(Map.Entry<Integer, Integer> lhs, Map.Entry<Integer, Integer> rhs) {
            return rhs.getValue() - lhs.getValue();
        }
    };
    public void print(PrintStream ps){
        ps.print('{');
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
        ps.println('}');
        ps.println(String.format("[ %d , %d]", min_key, max_key));

    }


    public static void main(String[] args){
        Historgram historgram = new Historgram();
        historgram.setN(-6, 8);
        historgram.setN(-5, 8);
        historgram.setN(-3, 16);
        historgram.setN(-2, 32);
        historgram.setN(-1, 512+64);
        historgram.setN(0, 1024);
        historgram.setN(1, 512+128);
        historgram.setN(2, 64);
        historgram.setN(3, 32);
        historgram.setN(4, 16);
        historgram.setN(5, 16+8);
        historgram.setN(6, 16);
        historgram.setN(8, 8);
        historgram.setN(9, 8);

        historgram.print();
        historgram.shift(0,1);
        historgram.print();

    }
}
