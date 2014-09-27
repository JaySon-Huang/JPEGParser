package com.jayson.utils.jpeg;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Stack;

/**
 * Package : com.jayson.utils.jpeg
 * Author  : JaySon
 * Date    : 9/21/14
 */
public class JPEGBitOutputStream {
    public final static int DEFAULT_BUFFER_SIZE = 1024;
    protected OutputStream mOs;

    protected byte[] mByteBuf;
    protected int mBytePos;
    protected int mBitPos;

    public JPEGBitOutputStream(OutputStream os){
        this(os, DEFAULT_BUFFER_SIZE);
    }

    public JPEGBitOutputStream(OutputStream os, int buffsize){
        mOs = os;
        mByteBuf = new byte[buffsize];
        mBitPos = mBytePos = 0;
    }

    public void putBitString(String bits) throws IOException {
        for (int i = 0; i != bits.length(); ++i){
            if (bits.charAt(i) == '0'){
                putBit(0);
            }else if (bits.charAt(i) == '1'){
                putBit(1);
            }else{
                System.err.println("Not valid bit string!");
                return;
            }
        }
    }

    public void putByte(byte num) throws IOException {
        Stack<Integer> bit_stack = new Stack<Integer>();
        for (int i = 0; i != 8; ++i){
            bit_stack.add(num & 0x01);
            num >>= 1;
        }
        Integer one_bit;
        while (!bit_stack.empty()){
            one_bit = bit_stack.pop();
            putBit(one_bit);
        }
    }

    public void putBit(int oneBit) throws IOException {
        mByteBuf[mBytePos] |= (oneBit<<(7-mBitPos++));
        if (mBitPos == 8){
            byte lastByte = mByteBuf[mBytePos];
            ++mBytePos;mBitPos = 0;
            if (mBytePos == mByteBuf.length){
                mOs.write(mByteBuf);
                mBytePos = mBitPos = 0;
                for (int i=0; i!=mByteBuf.length; ++i){
                    mByteBuf[i] = 0;
                }
            }
            if (lastByte == (byte)0xff){
                this.putByte( (byte) 0x00 );
            }
        }
    }
}
