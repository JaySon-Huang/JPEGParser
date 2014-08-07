package com.jayson.utils.jpeg;

import java.util.HashMap;

/**
 * Created by JaySon on 14-8-3.
 */
public class JPEGMarker extends HashMap<Integer, JPEGMarkInfo> {

    private static HashMap<Integer, JPEGMarkInfo> mInfos;

    static{
        mInfos = new HashMap<Integer, JPEGMarkInfo>();
        mInfos.put(0xffc0, new JPEGMarkInfo("SOF0", "Baseline DCT", JPEGMarkInfo.TYPE_SOF));
        mInfos.put(0xffc1, new JPEGMarkInfo("SOF1", "Extended Sequential DCT", JPEGMarkInfo.TYPE_SOF));
        mInfos.put(0xffc2, new JPEGMarkInfo("SOF2", "Progressive DCT", JPEGMarkInfo.TYPE_SOF));
        mInfos.put(0xffc3, new JPEGMarkInfo("SOF3", "Spatial lossless", JPEGMarkInfo.TYPE_SOF));
        mInfos.put(0xffc4, new JPEGMarkInfo("DHT", "Define Huffman table", JPEGMarkInfo.TYPE_DHT));
        mInfos.put(0xffc5, new JPEGMarkInfo("SOF5", "Differential sequential DCT", JPEGMarkInfo.TYPE_SOF));
        mInfos.put(0xffc6, new JPEGMarkInfo("SOF6", "Differential progressive DCT", JPEGMarkInfo.TYPE_SOF));
        mInfos.put(0xffc7, new JPEGMarkInfo("SOF7", "Differential spatial", JPEGMarkInfo.TYPE_SOF));
        mInfos.put(0xffc8, new JPEGMarkInfo("JPG", "Extension", JPEGMarkInfo.TYPE_UNKNOWN));
        mInfos.put(0xffc9, new JPEGMarkInfo("SOF9", "Extended sequential DCT (AC)", JPEGMarkInfo.TYPE_SOF));
        mInfos.put(0xffca, new JPEGMarkInfo("SOF10", "Progressive DCT (AC)", JPEGMarkInfo.TYPE_SOF));
        mInfos.put(0xffcb, new JPEGMarkInfo("SOF11", "Spatial lossless DCT (AC)", JPEGMarkInfo.TYPE_SOF));
        mInfos.put(0xffcc, new JPEGMarkInfo("DAC", "Define arithmetic coding conditioning", JPEGMarkInfo.TYPE_SKIP));
        mInfos.put(0xffcd, new JPEGMarkInfo("SOF13", "Differential sequential DCT (AC)", JPEGMarkInfo.TYPE_SOF));
        mInfos.put(0xffce, new JPEGMarkInfo("SOF14", "Differential progressive DCT (AC)", JPEGMarkInfo.TYPE_SOF));
        mInfos.put(0xffcf, new JPEGMarkInfo("SOF15", "Differential spatial (AC)", JPEGMarkInfo.TYPE_SOF));

        mInfos.put(0xffd0, new JPEGMarkInfo("RST0", "Restart 0", JPEGMarkInfo.TYPE_UNKNOWN));
        mInfos.put(0xffd1, new JPEGMarkInfo("RST1", "Restart 1", JPEGMarkInfo.TYPE_UNKNOWN));
        mInfos.put(0xffd2, new JPEGMarkInfo("RST2", "Restart 2", JPEGMarkInfo.TYPE_UNKNOWN));
        mInfos.put(0xffd3, new JPEGMarkInfo("RST3", "Restart 3", JPEGMarkInfo.TYPE_UNKNOWN));
        mInfos.put(0xffd4, new JPEGMarkInfo("RST4", "Restart 4", JPEGMarkInfo.TYPE_UNKNOWN));
        mInfos.put(0xffd5, new JPEGMarkInfo("RST5", "Restart 5", JPEGMarkInfo.TYPE_UNKNOWN));
        mInfos.put(0xffd6, new JPEGMarkInfo("RST6", "Restart 6", JPEGMarkInfo.TYPE_UNKNOWN));
        mInfos.put(0xffd7, new JPEGMarkInfo("RST7", "Restart 7", JPEGMarkInfo.TYPE_UNKNOWN));

        mInfos.put(0xffd8, new JPEGMarkInfo("SOI", "Start of image", JPEGMarkInfo.TYPE_SOI));
        mInfos.put(0xffd9, new JPEGMarkInfo("EOI", "End of image", JPEGMarkInfo.TYPE_UNKNOWN));
        mInfos.put(0xffda, new JPEGMarkInfo("SOS", "Start of scan", JPEGMarkInfo.TYPE_SOS));
        mInfos.put(0xffdb, new JPEGMarkInfo("DQT", "Define quantization table", JPEGMarkInfo.TYPE_DQT));
        mInfos.put(0xffdc, new JPEGMarkInfo("DNL", "Define number of lines", JPEGMarkInfo.TYPE_SKIP));
        mInfos.put(0xffdd, new JPEGMarkInfo("DRI", "Define restart interval", JPEGMarkInfo.TYPE_SKIP));
        mInfos.put(0xffde, new JPEGMarkInfo("DHP", "Define hierarchical progression", JPEGMarkInfo.TYPE_SOF));
        mInfos.put(0xffdf, new JPEGMarkInfo("EXP", "Expand reference component", JPEGMarkInfo.TYPE_SKIP));

        mInfos.put(0xffe0, new JPEGMarkInfo("APP0", "Application segment 0", JPEGMarkInfo.TYPE_APP));
        mInfos.put(0xffe1, new JPEGMarkInfo("APP1", "Application segment 1", JPEGMarkInfo.TYPE_APP));
        mInfos.put(0xffe2, new JPEGMarkInfo("APP2", "Application segment 2", JPEGMarkInfo.TYPE_APP));
        mInfos.put(0xffe3, new JPEGMarkInfo("APP3", "Application segment 3", JPEGMarkInfo.TYPE_APP));
        mInfos.put(0xffe4, new JPEGMarkInfo("APP4", "Application segment 4", JPEGMarkInfo.TYPE_APP));
        mInfos.put(0xffe5, new JPEGMarkInfo("APP5", "Application segment 5", JPEGMarkInfo.TYPE_APP));
        mInfos.put(0xffe6, new JPEGMarkInfo("APP6", "Application segment 6", JPEGMarkInfo.TYPE_APP));
        mInfos.put(0xffe7, new JPEGMarkInfo("APP7", "Application segment 7", JPEGMarkInfo.TYPE_APP));
        mInfos.put(0xffe8, new JPEGMarkInfo("APP8", "Application segment 8", JPEGMarkInfo.TYPE_APP));
        mInfos.put(0xffe9, new JPEGMarkInfo("APP9", "Application segment 9", JPEGMarkInfo.TYPE_APP));
        mInfos.put(0xffea, new JPEGMarkInfo("APP10", "Application segment 10", JPEGMarkInfo.TYPE_APP));
        mInfos.put(0xffeb, new JPEGMarkInfo("APP11", "Application segment 11", JPEGMarkInfo.TYPE_APP));
        mInfos.put(0xffec, new JPEGMarkInfo("APP12", "Application segment 12", JPEGMarkInfo.TYPE_APP));
        mInfos.put(0xffed, new JPEGMarkInfo("APP13", "Application segment 13", JPEGMarkInfo.TYPE_APP));
        mInfos.put(0xffee, new JPEGMarkInfo("APP14", "Application segment 14", JPEGMarkInfo.TYPE_APP));
        mInfos.put(0xffef, new JPEGMarkInfo("APP15", "Application segment 15", JPEGMarkInfo.TYPE_APP));

        mInfos.put(0xfff0, new JPEGMarkInfo("JPG0", "Extension 0", JPEGMarkInfo.TYPE_UNKNOWN));
        mInfos.put(0xfff1, new JPEGMarkInfo("JPG1", "Extension 1", JPEGMarkInfo.TYPE_UNKNOWN));
        mInfos.put(0xfff2, new JPEGMarkInfo("JPG2", "Extension 2", JPEGMarkInfo.TYPE_UNKNOWN));
        mInfos.put(0xfff3, new JPEGMarkInfo("JPG3", "Extension 3", JPEGMarkInfo.TYPE_UNKNOWN));
        mInfos.put(0xfff4, new JPEGMarkInfo("JPG4", "Extension 4", JPEGMarkInfo.TYPE_UNKNOWN));
        mInfos.put(0xfff5, new JPEGMarkInfo("JPG5", "Extension 5", JPEGMarkInfo.TYPE_UNKNOWN));
        mInfos.put(0xfff6, new JPEGMarkInfo("JPG6", "Extension 6", JPEGMarkInfo.TYPE_UNKNOWN));
        mInfos.put(0xfff7, new JPEGMarkInfo("JPG7", "Extension 7", JPEGMarkInfo.TYPE_UNKNOWN));
        mInfos.put(0xfff8, new JPEGMarkInfo("JPG8", "Extension 8", JPEGMarkInfo.TYPE_UNKNOWN));
        mInfos.put(0xfff9, new JPEGMarkInfo("JPG9", "Extension 9", JPEGMarkInfo.TYPE_UNKNOWN));
        mInfos.put(0xfffa, new JPEGMarkInfo("JPG10", "Extension 10", JPEGMarkInfo.TYPE_UNKNOWN));
        mInfos.put(0xfffb, new JPEGMarkInfo("JPG11", "Extension 11", JPEGMarkInfo.TYPE_UNKNOWN));
        mInfos.put(0xfffc, new JPEGMarkInfo("JPG12", "Extension 12", JPEGMarkInfo.TYPE_UNKNOWN));
        mInfos.put(0xfffd, new JPEGMarkInfo("JPG13", "Extension 13", JPEGMarkInfo.TYPE_UNKNOWN));

        mInfos.put(0xfffe, new JPEGMarkInfo("COM", "Comment", JPEGMarkInfo.TYPE_COMMENT));
    }

    public static JPEGMarkInfo getMarkInfo(int mark){
        return mInfos.get(mark);
    }

}
