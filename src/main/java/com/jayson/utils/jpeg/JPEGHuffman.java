package com.jayson.utils.jpeg;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

/**
 * Package : com.jayson.utils.jpeg
 * Author  : JaySon
 * Date    : 14-8-4
 */
public class JPEGHuffman {

    public final static int TYPE_DC = 0;
    public final static int TYPE_AC = 1;

    int mType;
    int mID;

    Map<String, Integer> mPairs;

    JPEGHuffman(byte[] data, int[] scan_index){
//        System.out.println("Building Huffman Table...");

        mPairs = new TreeMap<String, Integer>();

        // 表类型 0--DC 1--AC
        mType = data[scan_index[0]] >>> 4;
        // 表ID
        mID = data[scan_index[0]] & 0x0f;
//        switch (mType){
//            case TYPE_AC:
//                System.out.println("AC - "+mID);
//                break;
//            case TYPE_DC:
//                System.out.println("DC - "+mID);
//                break;
//        }

        //不同位数的码字数量 - 16 字节
        //编码内容
        int code_ind = scan_index[0]+1;
        int weight_ind = scan_index[0] + 17;
        int code = 0;
        StringBuffer code_str;
        for (int i = 1; i != 17; ++i){
            // 码字左移1位，右方引入'0',增长码字长度
            code <<= 1;
            for(int j = 0; j != data[code_ind]; ++j){
                // 前置添0，保证码字长度为i
                code_str = str_pad(i, Integer.toBinaryString(code));
                ++code;
//                System.out.println("("+code_str+", "+
//                        (data[weight_ind] & 0xFF)+")");
                mPairs.put(code_str.toString(),
                        (data[weight_ind] & 0xFF));
                // 指向下一个权值位置
                ++weight_ind;
            }
            // 指向下一个码字数量
            ++code_ind;
        }
        // 更新已处理的字节索引值，以便处理一个块中定义多个哈夫曼表的情况
        scan_index[0] = weight_ind;
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

    public String findKey(int value){
        for (Map.Entry<String, Integer> entry: mPairs.entrySet()){
            if (entry.getValue() == value){
                return entry.getKey();
            }
        }
        return null;
    }

    public void save(DataOutputStream out) throws IOException {
        out.writeShort(JPEGImage.DHT);// DHT标志
        int len = 16+mPairs.size();
        byte[] bytes = new byte[len];
        out.writeShort(2 + 1 + len);// 写入段长度
        out.write( mType<<4 | mID );// 写入Huffman树类型、ID。
        ArrayList< ArrayList<Integer> > data = new ArrayList< ArrayList<Integer> >(17);
        for (int i = 0; i != 17; ++i){
            data.add(new ArrayList<Integer>());
        }
        for (Map.Entry<String, Integer> entry : mPairs.entrySet()){
            data.get( entry.getKey().length()-1 ).add( entry.getValue() );
        }
        int valIndex = 16;
        for (int i = 0; i != 16; ++i) {
            bytes[i] = (byte) data.get(i).size();
            for (int j = 0; j != data.get(i).size(); ++j) {
                bytes[valIndex++] = data.get(i).get(j).byteValue();
            }
        }
        // 写入哈夫曼数据
        out.write(bytes);
    }
}
