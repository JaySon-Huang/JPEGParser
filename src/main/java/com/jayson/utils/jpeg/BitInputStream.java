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

    public int readBit() throws IOException, JPEGParser.MarkAppearException {
        int retVal;
        // 当前mBytePos指向字节已经读完
        if(mBitPos == 8){

            // 缓冲区的数据完全读取完了
            if(mBytePos == mByteBuf.length-1){
                // 载入下一段数据到缓冲区中
                mIs.read(mByteBuf);
                mBitPos = mBytePos = 0;
            }else{
                // 指向缓冲区中下一字节
                ++mBytePos;
                mBitPos = 0;
            }
            /*
                在图像数据流中遇到0xFF，应该检测其紧接着的字符，如果是
                1）0x00，则表示0xFF是图像流的组成部分，需要进行译码；
                2）0xD9，则与0xFF组成标记EOI，则图像流结束，同时图像文件结束；
                3）0xD0~0xD7,则组成RSTn标记，则要忽视整个RSTn标记，即不对当前0xFF和紧接的0xDn两个字节进行译码，
                    并按RST标记的规则调整译码变量；
                4）0xFF，则忽视当前0xFF，对后一个0xFF再作判断；
                5）其他数值，则忽视当前0xFF，并保留紧接的此数值用于译码。
            */
            // 0xff标志
            if ((mByteBuf[mBytePos]&0xff) == 0xff){
                // 0xff 后一个字节在缓冲区外
                if ( mBytePos == mByteBuf.length-1 ){
                    // 0xff 保留在索引值为0处，读入的数据读入到缓冲区内
                    mByteBuf[0] = (byte) 0xff;
                    mIs.read(mByteBuf, 1, mByteBuf.length-1);
                    mBytePos = 0;
                }
                switch ((mByteBuf[mBytePos+1]&0xff))
                {
                    // 0xff00 为 0xff 的扩展，当做0xff处理
                    case 0x00:
                        mByteBuf[mBytePos+1] = (byte) 0xff;
                        ++mBytePos;
                        break;

                    // 0xD9 - EOI
                    case 0xD9:
                        throw new JPEGParser.MarkAppearException(0xD9);

                        // 0xD0~D7 - RSTn
                    case 0xD0:case 0xD1:case 0xD2:case 0xD3:case 0xD4:case 0xD5:case 0xD6:case 0xD7:
                        System.err.println(String.format("0xff%02x happen!",mByteBuf[mBytePos+1]));
                        // 忽略RST标志
                        mBytePos += 2;
                        // 抛出异常，在上层函数处理RST调整译码变量
                        throw new JPEGParser.MarkAppearException(mByteBuf[mBytePos-1]);

                    // 忽视之?
                    case 0xFF:// FIXME : 这样处理?
                        System.err.println(String.format("0xff%02x happen!",mByteBuf[mBytePos+1]));
                        mBytePos += 2;
                        break;

                    default:// FIXME : 这样处理?
                        System.err.println(String.format("0xff%02x happen!",mByteBuf[mBytePos+1]));
                        ++mBytePos;
                        break;
                }
            }
            // 得到一位
            return readBit();
        }else{
            retVal = mByteBuf[mBytePos] >>> (7-mBitPos);
            ++mBitPos;
            return retVal & 0x1;
        }
    }

    public int readBits(int nBit) throws IOException, JPEGParser.MarkAppearException {
        assert (1 <= nBit && nBit <= 32);
        int retVal=0;
        for (int i=0;i!=nBit;++i){
            retVal <<= 1;
            retVal |= readBit();
        }
        return retVal;
    }

    public String readBitsString(int nBit) throws IOException, JPEGParser.MarkAppearException {
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
