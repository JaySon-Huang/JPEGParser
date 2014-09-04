package com.jayson.utils.jpeg;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static com.jayson.utils.jpeg.b2i.i16;

/**
 * Package : com.jayson.utils.jpeg
 * Author  : JaySon
 * Date    : 14-8-4
 */
public class JPEGDQT {
    public final static int PRECISION_16 = 1;
    public final static int PRECISION_8  = 0;

    private int[] mTable;
    private int mID;
    private int mPrecision;

    public JPEGDQT(byte[] bytes, int[] scan_index) {
        // 偏移值0处，高四位为精度值 0/1
        mPrecision = bytes[scan_index[0]] >>> 4;
        // 低四位为量化表id
        mID = bytes[scan_index[0]] & 0x0f;

        // 获取量化表实际值
        // DQT内容从偏移量1处开始
        mTable = new int[64];
        for (int i = 0; i != 64; ++i){
            mTable[i] =
                ( (mPrecision == PRECISION_16)?
                    i16(bytes, (i<<1) +1+scan_index[0] ):
                    bytes[i +1+scan_index[0]] );
//                int ind;
//                if (precision == PRECISION_16){
//                    ind = (((i<<3)+j)<<1)+1;
//                    mTable[i][j] = i16(bytes, (((i<<3)+j)<<1)+1 );
//                }else{
//                    ind = ((i<<3)+j)+1;
//                    mTable[i][j] = bytes[((i<<3)+j)+1];
//                }
        }
        // 更新已处理的字节索引值，以便处理一个块中定义多个量化表的情况
        scan_index[0] += 1 + 64 * (mPrecision+1);
    }

    public int getID(){
        return mID;
    }

    public int getPrecision(){
        return mPrecision;
    }

    /**
     * 把四个量化表存储为一个block，输出到out中
     * @param out       输出
     * @param tables    四个量化表
     */
    public static void saveDQTs (DataOutputStream out, JPEGDQT[] tables) throws IOException {
        assert (tables.length == 4);

        out.writeShort(JPEGImage.DQT);
        out.writeShort(2+260);// 四个块精度都为 PRECISION_8
        for (int i = 0; i != 4; ++i){
            assert (tables[i].mPrecision == PRECISION_8);
            byte b = (byte) (i&0xf);
            out.write(b);
            for (int j = 0; j != 64; ++j){
                out.write(tables[i].mTable[j]);
            }
        }
    }
}
