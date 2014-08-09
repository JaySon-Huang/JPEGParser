package com.jayson.utils.jpeg;

import static com.jayson.utils.jpeg.b2i.i16;

/**
 * Created by JaySon on 14-8-4.
 */
public class JPEGDQT {
    public final static int PRECISION_16 = 1;
    public final static int PRECISION_8  = 0;

    private int[][] mTable;

    public JPEGDQT(byte[] bytes, int precision) throws JPEGParser.InvalidJpegFormatException {
        int accept_length = 64*(precision+1) + 1;
        if(bytes.length != accept_length){
            throw new JPEGParser.InvalidJpegFormatException("Invalid DQT size:"+bytes.length);
        }
        mTable = new int[8][8];
        for (int i=0;i!=8;++i){
            for (int j=0;j!=8;++j){
                // 索引0处为DQT精度及标记字节，DQT内容从索引值1处开始
                mTable[i][j] =
                        ( (precision==PRECISION_16)?
                                i16(bytes, (((i<<3)+j)<<1) +1 ):
                                bytes[(i<<3)+j +1] );
//                int ind;
//                if (precision == PRECISION_16){
//                    ind = (((i<<3)+j)<<1)+1;
//                    mTable[i][j] = i16(bytes, (((i<<3)+j)<<1)+1 );
//                }else{
//                    ind = ((i<<3)+j)+1;
//                    mTable[i][j] = bytes[((i<<3)+j)+1];
//                }
            }
        }
    }
}
