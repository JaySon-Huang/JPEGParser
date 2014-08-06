package com.jayson.utils.jpeg;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by JaySon on 14-8-6.
 */
public class BitInputStream{

    private InputStream mIs;

    private byte[] mByteBuf;
    private int mBytePos;
    private int mBitPos;

    public BitInputStream(InputStream is) throws IOException {
        this(is, 1024);
    }

    public BitInputStream(InputStream is, int buffsize) throws IOException {
        mIs = is;
        mByteBuf = new byte[buffsize];
        mIs.read(mByteBuf);
        mBitPos = mBytePos = 0;
    }

    public int readBit() throws IOException {
        int retVal=0;
        // 当前mBytePos指向字节已经读完
        if(mBitPos == 8){

            // 缓冲区的数据完全读取完了
            if(mBytePos == mByteBuf.length-1){
                // 载入下一段数据到缓冲区中
                mIs.read(mByteBuf);
                mBitPos = mBytePos = 0;
                // 得到一位
                retVal = mByteBuf[mBytePos] >>> 7;
                mBitPos = 1;
            }else{
                // 指向缓冲区中下一字节
                ++mBytePos;
                mBitPos = 0;
                // 得到一位
                retVal = mByteBuf[mBytePos] >>> 7;
                mBitPos = 1;
            }

        }else{
            retVal = mByteBuf[mBytePos] >>> (7-mBitPos);
            ++mBitPos;
        }
        return retVal & 0x1;
    }

    public int readBits(int nBit) throws IOException {
        assert (1 <= nBit && nBit <= 32);
        int retVal=0;
        for (int i=0;i!=nBit;++i){
            retVal <<= 1;
            retVal |= readBit();
        }
        return retVal;
    }

    public String readBitsString(int nBit) throws IOException {
        StringBuffer sb = new StringBuffer(Integer.toBinaryString(readBits(nBit)));
        int pad_len = nBit - sb.length();
        for (int i=0;i!=pad_len;++i){
            sb.insert(0,'0');
        }
        return sb.toString();
    }

}
