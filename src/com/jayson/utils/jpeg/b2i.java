package com.jayson.utils.jpeg;

/**
 * Created by JaySon on 14-8-4.
 */
public class b2i {
    public static int i16(byte[] bytes){
        return i16(bytes, 0);
    }
    public static int i16(byte[] bytes, int offset){
        int ret = 0;
        ret |= (bytes[offset]&0xff)<<8;
        ret |= (bytes[offset+1]&0xff);
        return ret;
    }
}
