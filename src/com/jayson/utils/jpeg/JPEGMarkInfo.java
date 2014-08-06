package com.jayson.utils.jpeg;

/**
 * Created by JaySon on 14-8-3.
 */
public class JPEGMarkInfo {
    public static final int TYPE_UNKNOWN = 0x0;
    public static final int TYPE_SOF = 0x1;
    public static final int TYPE_APP = 0x2;
    public static final int TYPE_DQT = 0x3;
    public static final int TYPE_DHT = 0x4;
    public static final int TYPE_SOS = 0x5;
    public static final int TYPE_SKIP = 0x6;
    public static final int TYPE_COMMENT = 0x7;

    public final String mName;
    public final String mDescription;
    public final int mType;

    JPEGMarkInfo(String name, String desc, int type){
        mName = name;
        mDescription = desc;
        mType = type;
    }
}
