package com.jayson.utils.jpeg;

import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Package : com.jayson.utils.jpeg
 * Author  : JaySon
 * Date    : 14-8-9
 */
public class JPEGImage {


    /**
     * start of image
     */
    public static final int SOI = 0xFFD8;
    public static final int DQT = 0xFFDB;
    public static final int DHT = 0xFFC4;
    public static final int SOF = 0xFFC0;
    public static final int SOS = 0xFFDA;
    public static final int EOI = 0xFFD9;

    public static final String[] COLORS_GREY = new String[]{"L"};
    public static final String[] COLORS_YCrCb = new String[]{" Y", "Cr", "Cb"};
    public static final String[] COLORS_CMYK = new String[]{"C", "M", "Y", "K"};


    /**
     * 文件的存储路径
     */
    private String mFilePath;

    /**
     * JFIF 信息
     */
    byte JFIF_major_version = 1;
    byte JFIF_minor_version = 1;
    private String JFIF_unit;
    private int JFIF_destiny_y;
    private int JFIF_destiny_x;
    private Thumbnail mThumbnail;

    /**
     * App 信息
     */
    private Map<Integer, byte[]> mApp;

    /**
     * 量化表
     */
    private List<JPEGDQT> mDQT;

    /**
     * 直流哈夫曼表
     */
    private JPEGHuffman[] mHuffmanDC;

    /**
     * 交流哈夫曼表
     */
    private JPEGHuffman[] mHuffmanAC;

    /**
     * 图像高宽
     */
    private int mHeight,mWidth;

    /**
     * 颜色空间
     */
    private String[] mColors;

    /**
     *     每一颜色层信息
     */
    private Map<Integer, JPEGLayer> mLayers;

    /**
     * 量化后每一个单元信息
     */
    private JPEGDataUnits mDataUnits;
    private int mPrecision;


    public JPEGImage(String filepath){
        mApp = new HashMap<Integer, byte[]>();
        mDQT = new ArrayList<JPEGDQT>(4);
        mHuffmanDC = new JPEGHuffman[2];
        mHuffmanAC = new JPEGHuffman[2];
        mDataUnits = new JPEGDataUnits();
        mFilePath = filepath;
    }

    /**
     * 设置图像高宽
     * @param width     宽度
     * @param height    高度
     * @param precision 精度
     */
    public void setSize(int width, int height, int precision){
        mWidth = width;
        mHeight = height;
        mPrecision = precision;
    }

    /**
     * @return 图像的高度
     */
    public int height(){
        return mHeight;
    }

    /**
     * @return 图像的宽度
     */
    public int width(){
        return mWidth;
    }

    /**
     * @return 图像高*宽
     */
    public int size(){  return mHeight*mWidth; }

    public void setAppInfo(int marker, byte[] bytes){
        mApp.put(marker, bytes);
    }

    /**
     * 设置jpeg量化表
     * @param table     量化表
     */
    public void setDQT(JPEGDQT table) {
        mDQT.add(table.getID(), table);
    }

    /**
     * 设置色彩空间
     * @param colors    色彩空间 JPEGParser.COLOR_XX
     */
    public void setColors(String[] colors){
        mColors = colors;
        mLayers = new TreeMap<Integer, JPEGLayer>();
    }

    /**
     * 得到色彩空间
     * @return  String数组,每一个String为颜色空间名字,如{"L"}、{"Y","Cr","Cb"}、{"C","M","Y","K"}
     */
    public String[] getColors(){
        return mColors;
    }

    /**
     * 设置图像颜色层信息
     * @param layer 颜色层对象
     */
    public void setLayer(JPEGLayer layer) {
        mLayers.put(layer.mColorID, layer);
    }

    /**
     * @return 图像颜色层对应id
     */
    public Set<Integer> getColorIDs(){
        return mLayers.keySet();
    }

    /**
     * 返回图像颜色层信息
     * @param color_id 颜色索引值，从1开始
     * @return color_id 对应颜色层JPEGImage.JPEGLayer对象
     */
    public JPEGLayer getLayer(int color_id){
        return mLayers.get(color_id);
    }

    /**
     * 设置哈夫曼表
     * @param huffman 哈夫曼表
     */
    public void setHuffman(JPEGHuffman huffman) {
        switch(huffman.mType)
        {
            case JPEGHuffman.TYPE_DC:
                mHuffmanDC[huffman.mID] = huffman;
                break;
            case JPEGHuffman.TYPE_AC:
                mHuffmanAC[huffman.mID] = huffman;
                break;

            default:
                System.err.println("Huffman Type error!");
                break;
        }
    }

    /**
     * 得到哈夫曼表
     * @param type  JPEGHuffman.TYPE_XX 直流/交流
     * @param id    哈夫曼表id值
     * @return      对应的哈夫曼表
     */
    public JPEGHuffman getHuffman(int type, int id){
        switch (type)
        {
            case JPEGHuffman.TYPE_DC:
                return mHuffmanDC[id];
            case JPEGHuffman.TYPE_AC:
                return mHuffmanAC[id];

            default:
                System.err.println("Huffman Type error!");
                return null;
        }
    }

    public void setJFIFInfo(int version, String jfif_unit, int jfif_destiny_x, int jfif_destiny_y){
        JFIF_major_version = (byte) (version >>> 8);
        JFIF_minor_version = (byte) (version & 0xFF);
        JFIF_unit = jfif_unit;
        JFIF_destiny_x = jfif_destiny_x;
        JFIF_destiny_y = jfif_destiny_y;
    }

    public void addDataUnit(int colorid, int[] unit){
        mDataUnits.add(colorid, unit);
    }

    public void setDataUnits(JPEGDataUnits newData){
        mDataUnits.clear();
        mDataUnits = null;
        mDataUnits = newData;
    }

    public JPEGDataUnits getDataUnits(){
        return mDataUnits;
    }

    public void setThumbnail(int thumbnail_horizontal_pixels,
                             int thumbnail_vertical_pixels,
                             int[] thumbnail_rgb_bitmap) {
        mThumbnail = new Thumbnail();
        mThumbnail.width = thumbnail_horizontal_pixels;
        mThumbnail.height = thumbnail_vertical_pixels;
        mThumbnail.rgb = thumbnail_rgb_bitmap;
    }

    public String getFilePath() {
        return mFilePath;
    }

    public void save(String savePath) throws IOException, JPEGParser.InvalidJpegFormatException {
        DataOutputStream fsave = null;
        try {
            fsave = new DataOutputStream(new FileOutputStream(savePath));
            // 写入SOI

            fsave.writeShort(JPEGImage.SOI);
            // 写入APPn
            for ( Map.Entry<Integer, byte[]> appinfo : mApp.entrySet() ){
                fsave.writeShort(appinfo.getKey());
                fsave.writeShort(appinfo.getValue().length+2);
                fsave.write(appinfo.getValue());
            }
            // 写入DQT
            JPEGDQT.saveDQTs(fsave, mDQT);

            // 写入DHT
            for (JPEGHuffman h : mHuffmanDC){
                h.save(fsave);
            }
            for (JPEGHuffman h : mHuffmanAC){
                h.save(fsave);
            }

            // 写入SOF0
            this.saveSOF0(fsave);

            // 写入SOS
            this.saveSOS(fsave);

            // 写入图像数据
            this.saveData(fsave);

            // 写入EOI
            fsave.writeShort(EOI);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (fsave != null) {
                try {
                    fsave.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void saveData(DataOutputStream out) throws IOException {
        JPEGBitOutputStream bout = new JPEGBitOutputStream(out);
        // Y,Cr,Cb 待写入的下一个color单元索引值
        Map<Integer, Integer> indexs = new TreeMap<Integer, Integer>();
        // 前一个单元的DC值
        Map<Integer, Integer> pre_dcs = new TreeMap<Integer, Integer>();
        for (int colorid : getColorIDs()){
            indexs.put(colorid, 0);
            pre_dcs.put(colorid, 0);
        }

        while (true) {
            int colorid = saveOneUnit(indexs, pre_dcs, bout);
            if (indexs.get(colorid) == mDataUnits.getColorUnit(colorid).size()){
                break;
            }
        }
    }

    private int saveOneUnit(Map<Integer, Integer> indexs, Map<Integer, Integer> pre_dcs,
                            JPEGBitOutputStream bout) throws IOException {
        int ret = 0;
        for (int colorid : getColorIDs()) {
            ret = colorid;
            JPEGLayer layer = mLayers.get(colorid);
            for (int i = 0; i != layer.mHSamp*layer.mVSamp; ++i){
                int ind_of_color = indexs.get(colorid);
                int[] dataUnit = mDataUnits.getColorUnit(colorid).get(ind_of_color);
                indexs.put(colorid, ind_of_color+1);

                if (JPEGParser.verbose > 2) {
                    System.out.println("DC:" + dataUnit[0]);
                }
                int dc_diff = dataUnit[0] - pre_dcs.get(colorid);
                pre_dcs.put(colorid, dataUnit[0]);

                // 写入 dc_diff
                String bits_to_write = convert(dc_diff);
                JPEGHuffman huffman = getHuffman(JPEGHuffman.TYPE_DC, layer.mDCHuffmanID);
                String h_key = huffman.findKey(bits_to_write.length());
                // 写入
                bout.putBitString(h_key);
                bout.putBitString(bits_to_write);


                // 写入 ac
                huffman = getHuffman(JPEGHuffman.TYPE_AC, layer.mACHuffmanID);
                int zero_cnt = 0;
                for (int j = 1; j != 64; ++j){
                    int weight_to_write;
                    if (dataUnit[j] != 0){
                        // 若0的个数不能以4bit表示，需要先存下前0x0f个0，再继续处理
                        while (zero_cnt > 0x0f){
                            weight_to_write = 0xf0;
                            h_key = huffman.findKey(weight_to_write);
                            bout.putBitString(h_key);
                            zero_cnt -= 16;
                        }
                        weight_to_write = zero_cnt<<4;
                        bits_to_write = convert(dataUnit[j]);
                        if (JPEGParser.verbose > 2) {
                            System.out.println(dataUnit[j] + " -> " + bits_to_write);
                        }
                        weight_to_write |= bits_to_write.length();
                        h_key = huffman.findKey(weight_to_write);
                        if (h_key != null){
                            bout.putBitString(h_key);
                            bout.putBitString(bits_to_write);
                        }else{
                            // TODO: 只为DEBUG存在
                            System.err.println("Error in finding AC huffman key of "+ weight_to_write +"!");
                            System.err.print("Block:");
                            for (int index=0;index!=64;++index){
                                if (index%8==0) System.err.println();
                                System.out.print(String.format("%3d,",dataUnit[index]));
                            }System.out.println();
                            throw new IOException();
                        }
                        zero_cnt = 0;
                    }else{
                        ++zero_cnt;
                    }
                }
                if (zero_cnt != 0){
                    h_key = huffman.findKey(0);
                    bout.putBitString(h_key);
                }
            }
        }
        if (JPEGParser.verbose > 2) {
            System.out.println("One Unit end~");
        }
        return ret;
    }

    private String convert(int num){
        if (num == 0){
            return "";
        }
        int bit_of_num = 0;
        int tmp = Math.abs(num);
        while( tmp != 0){
            tmp >>= 1;
            ++bit_of_num;
        }

        StringBuffer res;
        if (num > 0) {
            res = new StringBuffer(Integer.toBinaryString(num));
        }else{
            num = Math.abs(num);
            res = new StringBuffer(Integer.toBinaryString((1<<bit_of_num)-1 - num));
        }
        while ( res.length() < bit_of_num) {
            res.insert(0, '0');
        }
        return res.toString();
    }

    private void saveSOS(DataOutputStream out) throws IOException {
        out.writeShort(JPEGImage.SOS);
        int len = 2+1+mColors.length*2+3;
        out.writeShort(len);
        out.write(mColors.length);
        for (Map.Entry<Integer, JPEGLayer> entry : mLayers.entrySet()) {
            JPEGLayer layer = entry.getValue();
            out.write(layer.mColorID);
            out.write(layer.mDCHuffmanID<<4 | layer.mACHuffmanID);
        }
        out.write(new byte[]{0x00, 0x3f, 0x00});
    }

    private void saveSOF0(DataOutputStream out) throws IOException {
        out.writeShort(JPEGImage.SOF);
        int len = 2+1+2+2+1+3*mColors.length;
        out.writeShort(len);// 段长度
        out.write(mPrecision);// 图像精度
        out.writeShort(mHeight);out.writeShort(mWidth);// 图像高、宽
        out.write(mColors.length);// 颜色空间
        for (Map.Entry<Integer, JPEGLayer> entry : mLayers.entrySet()){
            JPEGLayer layer = entry.getValue();
            out.write(layer.mColorID);
            out.write(layer.mHSamp<<4 | layer.mVSamp);
            out.write(layer.mDQTID);
        }
    }

    /**
     * 略缩图信息
     */
    public static class Thumbnail{
        int width, height;
        int[] rgb;
    }

    /**
     * JPEG 颜色层信息对象
     */
    public static class JPEGLayer{

        /**
         * 颜色空间索引值,从1开始
         */
        int mColorID;

        /**
         * 水平量化因子
         */
        int mVSamp;

        /**
         * 垂直量化因子
         */
        int mHSamp;

        /**
         * 颜色层对应量化表id
         */
        int mDQTID;

        /**
         * 颜色层对应直流哈夫曼表id
         */
        int mDCHuffmanID;

        /**
         * 颜色层对于交流哈夫曼表id
         */
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