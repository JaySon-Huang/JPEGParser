package com.jayson.utils.jpeg;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by JaySon on 14-8-4.
 */
public class JPEGInfo{
    private List<JPEGLayer> mLayers;

    private String mColors[];

    int JFIF_major_version = 1;
    int JFIF_minor_version = 1;

    public void setColors(String[] colors){
        mColors = colors;
        mLayers = new ArrayList<JPEGLayer>(colors.length);
    }
    public String[] getColors(){
        return mColors;
    }

    public void addLayer(JPEGLayer layer){
        mLayers.add(layer.mColorID-1, layer);
    }

    public JPEGLayer getLayer(int color_id){
        return mLayers.get(color_id-1);
    }

    public static class JPEGLayer{
        int mColorID;
        int mVSamp;
        int mHSamp;
        int mDQTID;

        int mDCHuffmanID;
        int mACHuffmanID;

        JPEGLayer(int colorID, int vSamp, int hSamp, int DQT_id){
            mColorID = colorID;
            mVSamp = vSamp;
            mHSamp = hSamp;
            mDQTID = DQT_id;
        }

        void setHuffman(int dc_huffman_id, int ac_huffman_id){
            mDCHuffmanID = dc_huffman_id;
            mACHuffmanID = ac_huffman_id;
        }
    }

}
