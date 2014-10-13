package com.jayson.utils;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Package : com.jayson.utils
 * Author  : JaySon
 * Date    : 8/14/14
 */
public class BitInputStream {
    public final static int DEFAULT_BUFFER_SIZE = 1024;

    protected InputStream mIs;

    protected byte[] mByteBuf;
    protected int mBytePos;
    protected int mBitPos;

    protected boolean isEnd;
    protected int mEndBytePos;

    public BitInputStream(InputStream is) throws IOException {
        this(is, DEFAULT_BUFFER_SIZE);
    }

    public BitInputStream(InputStream is, int buffsize) throws IOException {
        mIs = is;
        mByteBuf = new byte[buffsize];
        int byteRead = mIs.read(mByteBuf);
        if (byteRead != buffsize){
            mEndBytePos = byteRead-1;
            isEnd = true;
        }else{
            isEnd = false;
        }
        mBitPos = mBytePos = 0;
    }

    public int readBit() throws IOException {
        int retVal;
        // 当前mBytePos指向字节已经读完
        if(mBitPos == 8){

            if (isEnd && mBytePos == mEndBytePos){
                // 到达数据流的末端
                throw new EOFException();
            }
            // 缓冲区的数据完全读取完了
            if(mBytePos == mByteBuf.length-1){
                // 载入下一段数据到缓冲区中
                int byteRead = mIs.read(mByteBuf);
                if (byteRead != mByteBuf.length){
                    if (byteRead == -1){
                        throw new EOFException();
                    }
                    mEndBytePos = byteRead-1;
                    isEnd = true;
                }
                mBitPos = mBytePos = 0;
            }else{
                // 指向缓冲区中下一字节
                ++mBytePos;
                mBitPos = 0;
            }

            // 得到一位
            return readBit();
        }else{
            retVal = mByteBuf[mBytePos] >>> (7-mBitPos);
            ++mBitPos;
            return retVal & 0x1;
        }
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

    public String readBitsString(int nBit) throws IOException{
        if (nBit == 0){
//            System.out.print("codec length 0!");
            return "";
        }
        StringBuffer sb = new StringBuffer(Integer.toBinaryString(readBits(nBit)));
        int pad_len = nBit - sb.length();
        for (int i=0;i!=pad_len;++i){
            sb.insert(0,'0');
        }
        return sb.toString();
    }
}
