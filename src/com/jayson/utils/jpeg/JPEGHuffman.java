package com.jayson.utils.jpeg;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by JaySon on 14-8-4.
 */
public class JPEGHuffman {

    public final static int TYPE_DC = 0;
    public final static int TYPE_AC = 1;

    int mType;
    int mID;

    Map<String, Integer> mPairs;

    JPEGHuffman(byte[] data){
        System.out.println("Building Huffman Table...");

        mPairs = new HashMap<String, Integer>();

        // 表类型 0--DC 1--AC
        mType = data[0] >>> 4;
        // 表ID
        mID = data[0] & 0x0f;
        switch (mType){
            case TYPE_AC:
                System.out.println("AC - "+mID);
                break;
            case TYPE_DC:
                System.out.println("DC - "+mID);
                break;
        }
        //不同位数的码字数量 - 16 字节
        //编码内容

        int weight_ind = 0;
        int code = 0;
        StringBuffer code_str;
        for (int i=1;i!=17;++i){
            // 码字左移1位，右方引入'0',增长码字长度
            code <<= 1;
            for(int j=0;j!=data[i];++j){
                // 前置添0，保证码字长度为i
                code_str = str_pad(i, Integer.toBinaryString(code));
                ++code;
                System.out.println("("+code_str+", "+(data[17+weight_ind] & 0xFF)+")");
                mPairs.put(code_str.toString(), (data[17 + weight_ind] & 0xFF));
                // 指向下一个权值位置
                ++weight_ind;
            }
        }
    }

    private StringBuffer str_pad(int min_length, String str){
        StringBuffer ret = new StringBuffer();
        if( str.length() >= min_length){
            ret.append(str);
            return ret;
        }else{
            for (int i=0;i!=min_length-str.length();++i){
                ret.append('0');
            }
            ret.append(str);
            return ret;
        }
    }

    public Integer find(String key){
        return mPairs.get(key);
    }
}
