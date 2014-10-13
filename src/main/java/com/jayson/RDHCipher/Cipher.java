package com.jayson.RDHCipher;


import com.jayson.utils.BitInputStream;
import com.jayson.utils.Historgram;
import com.jayson.utils.PBKDF2Utils;
import com.jayson.utils.jpeg.JPEGDataUnits;
import com.jayson.utils.jpeg.JPEGImage;
import com.jayson.utils.jpeg.JPEGParser;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Package : com.jayson.RDHCipher
 * Author  : JaySon
 * Date    : 14-8-11
 */
public class Cipher {

    public final static int MASK_I = 0x01;
    public final static int MASK_Ia = 0x02;
    public final static int MASK_Ib = 0x04;
    public final static int MASK_Ind = 0x08;
    public final static int MASK_O1 = 0x10;
    public final static int MASK_O2 = 0x20;
    public final static int MASK_UNKNOWN = 0x40;

    public final static int POSITION_BASE = 0;

    public final static int EMB_BIT_PER_PIXEL = 1;
    public final static int L_BOUND = -127+(1<<EMB_BIT_PER_PIXEL)-1;
    public final static int R_BOUND =  127-(1<<EMB_BIT_PER_PIXEL)+1;

    private JPEGImage mImg;
    private int mMask[];
    private String AES_Key;
    private String AES_IV;
    private int m_Q;
    public final static int Q_MAX = 20;
    private Random mRandom;
    private int[] mRandomTable;

    /**
     *
     * @param filename  加密的文件
     * @param phase     用户密钥
     * @throws IOException
     * @throws JPEGParser.InvalidJpegFormatException
     */
    public Cipher(String filename, String phase) throws IOException, JPEGParser.InvalidJpegFormatException, InvalidKeySpecException, NoSuchAlgorithmException {
        JPEGParser parser = new JPEGParser(filename);
        mImg = parser.parse();
        mMask = new int[mImg.size()];

        // 生成 32+128 bytes的密钥,返回为十六进制编码后的字符串
        String pbkdf2Hash = PBKDF2Utils.getPBKDF2Hash(phase, 32+128, PBKDF2Utils.DEFAULT_ITERATION);
        String[] parts = pbkdf2Hash.split(":");
        // 0~128 bits / 0~16 bytes / 十六进制字符串中0~32个字符 作为AES加密密钥
        AES_Key = parts[2].substring(0, 32);
        // 128~256 bits / 16~32 bytes / 十六进制字符串中32~64个字符 作为AES加密的IV
        AES_IV  = parts[2].substring(32, 64);

        // Q值
        m_Q = Integer.parseInt(parts[2].substring(64, 66), 16) % Q_MAX;

        // 随机种子值，同一个phase生成同一个Random
        long seed = Long.parseLong(parts[2].substring(68, 76), 16);
        mRandom = new Random(seed);

        System.out.println("Q:"+m_Q);
    }

    public void emb(byte[] byte2emb) throws IOException {
        // 获得Y的colorid
        Integer colorid = (Integer) mImg.getColorIDs().toArray()[0];
        // 获得Y的color unit
        JPEGDataUnits dataUnits = mImg.getDataUnits();
        List<int[]> YUnits = dataUnits.getColorUnit(colorid);


        // 对图像数据进行平移,达到直方图中( POSITION_BASE-1, P_RIGHT+1 )处位置空出用以嵌入数据.
        int slot = byte2emb.length / YUnits.size() + 1;
        List<EmbInfo> embInfos = shiftData(dataUnits, colorid, slot);
        mRandomTable = new int[dataUnits.getColorUnit(colorid).size()];
        for (int i = 0; i != YUnits.size(); ++i){
            mRandomTable[i] = mRandom.nextInt();
        }
        mMask = new int[YUnits.size()];


        // 嵌入信息
        ByteArrayInputStream is = new ByteArrayInputStream(byte2emb);
        BitInputStream bis = new BitInputStream(is);


        for (EmbInfo embinfo: embInfos){
            try {
                int bit = bis.readBit();
                int newVal = dataUnits.get(colorid, embinfo.unit_no, embinfo.AC_no);
                if (embinfo.isLeftSlot) {
//                    System.out.println(String.format("%3s -> %3s",newVal,newVal-bit));
                    newVal -= bit;
                }else{
//                    System.out.println(String.format("%3s -> %3s",newVal,newVal+bit));
                    newVal += bit;
                }
                dataUnits.put(colorid, embinfo.unit_no, embinfo.AC_no, newVal);
            } catch (EOFException e) {
                // 信息已经全部嵌入,跳出
                break;
            }
        }

        // Debug :
//        dataUnits = JPEGDataUnits.getDebugDataUnit();
        // End Debug.

        Historgram[] historgrams = dataUnits.getHistorgram(colorid);
        System.out.print("Embed  = ");for (int i=0;i!=64;++i){System.out.print(i+":");historgrams[i].print(System.out);}


        try {
            mImg.save(mImg.getFilePath()+"_encrypted.jpg");
        } catch (JPEGParser.InvalidJpegFormatException e) {
            e.printStackTrace();
        }
    }

    private List<EmbInfo> shiftData(JPEGDataUnits dataUnits,int colorid , int slot){
        // 需要进行操作的颜色空间
        List<int[]> colorUnit = dataUnits.getColorUnit(colorid);
        // 需要操作的颜色空间的直方图
        Historgram[] historgrams = dataUnits.getHistorgram(colorid);
        // 返回值,返回可嵌入的位置
        ArrayList<EmbInfo> retInfos = new ArrayList<EmbInfo>(colorUnit.size());


        // 平移留出嵌入空间,颜色空间0处为直流分量,不做处理
        for (int acIndex = 1; acIndex <= slot; ++acIndex){
            historgrams[acIndex].print(System.out);
            int[] z = historgrams[acIndex].findZ(Cipher.POSITION_BASE);
            System.out.println(String.format("z:[%3d,%3d]",z[0],z[1]));
            System.out.println("Before : ");historgrams[acIndex].print();
            // 看每一个unit第i个分量是否在区间内,是的话进行平移
            for (int unitIndex = 0; unitIndex != colorUnit.size(); ++unitIndex){
                int[] unit = colorUnit.get(unitIndex);
                if ( z[0] < unit[acIndex] && unit[acIndex] < Cipher.POSITION_BASE ){
                    System.out.println(String.format("%4d left  shifted",unit[acIndex]));
                    --unit[acIndex];// 左移
                }else if ( Cipher.POSITION_BASE < unit[acIndex] && unit[acIndex] < z[1] ){
                    System.out.println(String.format("%4d right shifted",unit[acIndex]));
                    ++unit[acIndex];// 右移
                }else if(unit[acIndex] == Cipher.POSITION_BASE){
                    retInfos.add(new EmbInfo(unitIndex, acIndex, true));
                }
            }
        }

        historgrams = dataUnits.getHistorgram(colorid);
        System.out.println("After  : ");historgrams[1].print();
        return retInfos;
    }

    private int r(int i){
        int val = mRandomTable[i%(mRandomTable.length)] % 2048;
        return val>0?val:-val;
    }

    private class EmbInfo {
        // 在第unit_no里
        public int unit_no;
        // 在第AC_no个交流分量里
        public int AC_no;
        // 进行过左移或是右移
        public boolean isLeftSlot;

        EmbInfo(int unit_no, int AC_no, boolean isLeftSlot){
            this.unit_no = unit_no;this.AC_no = AC_no;
            this.isLeftSlot = isLeftSlot;
        }

    }


    public JPEGImage getImage(){
        return mImg;
    }

    public boolean isMatchMask(int pos, int mask){
        return (mMask[pos] & mask) == mask;
    }


    public static void main(String[] args) throws JPEGParser.InvalidJpegFormatException, InvalidKeySpecException, NoSuchAlgorithmException, IOException {
        Cipher encryptor = new Cipher("/Users/JaySon/Desktop/test.jpg", "JaySon");
        byte[] message = new byte[]{(byte) 0xff, (byte) 0xee, (byte) 0xdd};
        encryptor.emb(message);
    }
}
