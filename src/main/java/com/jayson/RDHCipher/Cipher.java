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
import java.util.Set;

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
     * 构造函数
     *
     * @param filename  加密的文件
     * @param phase     用户密钥
     * @throws IOException
     * @throws JPEGParser.InvalidJpegFormatException
     */
    public Cipher(String filename, String phase) throws IOException, JPEGParser.InvalidJpegFormatException, InvalidKeySpecException, NoSuchAlgorithmException {
        JPEGParser parser = new JPEGParser(filename);
        mImg = parser.parse();
        parser.close();
//        mMask = new int[mImg.size()];

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

    /**
     * 对图像内容加密，并嵌入byte2emb中的信息
     *
     * @param byte2emb      需要嵌入的信息字节流
     * @throws IOException
     */
    public void emb(byte[] byte2emb) throws IOException {
        // 获得Y的colorid
        Integer colorid = (Integer) mImg.getColorIDs().toArray()[0];
        // 获得Y的color unit
        JPEGDataUnits dataUnits = mImg.getDataUnits();
        List<int[]> YUnits = dataUnits.getColorUnit(colorid);

        // 打乱数据顺序
        try {
            JPEGDataUnits shuffled = shuffleDataUnits(dataUnits, mImg.getColorIDs());
            mImg.setDataUnits(shuffled);
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }

        // 重新获取jpeg数据块
        dataUnits = mImg.getDataUnits();

        // 对图像数据进行平移,达到直方图中( POSITION_BASE-1, P_RIGHT+1 )处位置空出用以嵌入数据.
        int slot = byte2emb.length / YUnits.size() + 1;
        List<EmbInfo> embInfos = shiftData(dataUnits, colorid, slot);

        // 无用的成员变量
//        mRandomTable = new int[dataUnits.getColorUnit(colorid).size()];
//        for (int i = 0; i != YUnits.size(); ++i){
//            mRandomTable[i] = mRandom.nextInt();
//        }
//        mMask = new int[YUnits.size()];

        // 嵌入信息
        ByteArrayInputStream is = new ByteArrayInputStream(byte2emb);
        BitInputStream bis = new BitInputStream(is);

        for (EmbInfo embinfo: embInfos){
            try {
                int bit = bis.readBit();
                int newVal = dataUnits.get(colorid, embinfo.unit_no, embinfo.AC_no);
                if ( bit==0 ) { //嵌入0，0 --> -1
                    System.out.println(String.format("%3s -> %3s",newVal,newVal-bit));
                    newVal -= 1;
                }else{          //嵌入1，0 --> 1
                    System.out.println(String.format("%3s -> %3s",newVal,newVal+bit));
                    newVal += 1;
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
            System.err.println("错误的图像信息!");
        }
    }

    /**
     * 对颜色空间的jpeg块进行打乱，使图像内容不可识别
     *
     * @return 打乱后新的DataUnit
     */
    private JPEGDataUnits shuffleDataUnits(JPEGDataUnits oldData, Set<Integer> colorids) throws CloneNotSupportedException {
        JPEGDataUnits newData = oldData.clone();
        List<Integer> shuffledIndex = null;
        List<int[]> oldUnit = null;

        // 对每个颜色分量进行混淆
        for (int colorid : colorids){
            shuffledIndex = new ArrayList<Integer>();
            oldUnit = oldData.getColorUnit(colorid);
            for (int i = 0; i != oldUnit.size(); ++i){
                shuffledIndex.add(i);
            }
            Collections.shuffle(shuffledIndex, mRandom);

            List<int[]> newUnit = newData.getColorUnit(colorid);
            for (int i=0; i!= oldUnit.size(); ++i){
                newUnit.set(shuffledIndex.get(i), oldUnit.get(i));
            }

        }
        return newData;
    }

    /**
     * 对jpeg数据块进行平移，并留出位置嵌入隐写信息。
     *
     * @param dataUnits jpeg数据块
     * @param colorid   进行平移的颜色分量id
     * @param slot      平移的槽数
     * @return          可以嵌入信息的位置信息。
     */
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
            // 看每一个unit第acIndex个分量是否在区间内,是的话进行平移
            for (int unitIndex = 0; unitIndex != colorUnit.size(); ++unitIndex){
                int[] unit = colorUnit.get(unitIndex);
                if ( z[0] < unit[acIndex] && unit[acIndex] < Cipher.POSITION_BASE ){
//                    System.out.println(String.format("%4d left  shifted",unit[acIndex]));
                    --unit[acIndex];// 左移
                }else if ( Cipher.POSITION_BASE < unit[acIndex] && unit[acIndex] < z[1] ){
//                    System.out.println(String.format("%4d right shifted",unit[acIndex]));
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

    /**
     * 不解密图像内容，只获取嵌入的字节流
     *
     * @return 被嵌入的信息
     */
    public byte[] extract() {
        List<Byte> bitData = new ArrayList<Byte>();
        // 获得Y的colorid
        Integer colorid = (Integer) mImg.getColorIDs().toArray()[0];
        // 获得Y的color unit
        JPEGDataUnits dataUnits = mImg.getDataUnits();
        List<int[]> YUnits = dataUnits.getColorUnit(colorid);

        for (int acIndex=1; acIndex != 2; ++acIndex){
            for (int unitIndex=0; unitIndex != YUnits.size(); ++unitIndex){
                int val = dataUnits.get(colorid, unitIndex, acIndex);
                if (val == -1){
                    bitData.add((byte)0);
                }else if (val == 1){
                    bitData.add((byte)1);
                }
            }
        }

        byte[] data = new byte[bitData.size()>>3];
        for (int i=0; i!= bitData.size(); ++i){
            data[i>>3] <<= 1;
            if (bitData.get(i) == -1){
                data[i>>3] |= 0;
            }else if (bitData.get(i) == 1){
                data[i>>3] |= 1;
            }
        }
        return data;
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
        String[] pics = {
                "/Users/JaySon/Desktop/test.jpg",
//                "/Users/JaySon/Pictures/IMG_20140508_085331.jpg",
//                "/Users/JaySon/Pictures/IMG_20140508_085558.jpg",
//                "/Users/JaySon/Pictures/IMG_20140508_090150.jpg",
//                "/Users/JaySon/Pictures/IMG_20140508_092000.jpg",
//                "/Users/JaySon/Pictures/IMG_20140508_115427.jpg",
//                "/Users/JaySon/Pictures/IMG_20140508_140426.jpg",
//                "/Users/JaySon/Pictures/IMG_20140810_122739.jpg",
//                "/Users/JaySon/Pictures/IMG_20140810_122739_1.jpg",
//                "/Users/JaySon/Pictures/IMG_20140810_122741.jpg",
//                "/Users/JaySon/Pictures/IMG_20140810_151927.jpg",
//                "/Users/JaySon/Pictures/PANO_20140613_191243.jpg",
//                "/Users/JaySon/Pictures/周 颠倒.jpg",
        };
        for (String pic : pics) {
            Cipher encryptor = new Cipher(pic, "JaySon");
            byte[] message = new byte[]{(byte) 0xff, (byte) 0xee, (byte) 0xdd};
            encryptor.emb(message);

            System.out.println("Extracting Data...");
            Cipher decryptor = new Cipher(pic+"_encrypted.jpg", "JaySon");
            message = decryptor.extract();
            for (byte b : message){
                System.out.print(String.format("%02X, ",b));
            }
            System.out.println();
        }
    }
}
