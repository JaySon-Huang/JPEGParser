package com.jayson.utils.jpeg;

import com.jayson.utils.Historgram;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


import static com.jayson.utils.jpeg.b2i.i16;

/**
 * Package : com.jayson.utils.jpeg
 * Author  : JaySon
 * Date    : 14-8-3
 */
public class JPEGParser implements Closeable{

    public final static int verbose = 1;


    private FileInputStream mIs;
    private String mFilePath;

    public JPEGParser(String filename) throws IOException, InvalidJpegFormatException {
        mFilePath = filename;

        mIs = new FileInputStream(filename);
        byte[] bytes = new byte[2];
        try {
            mIs.read(bytes);
            if(i16(bytes) != JPEGImage.SOI){
                this.close();
                throw new InvalidJpegFormatException();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() throws IOException {
        if (mIs != null){
            mIs.close();
            mIs = null;
        }
    }

    /**
     *
     * @return  返回解析后的jpeg格式图像对象(量化后,进行哈夫曼编码前)
     * @throws InvalidJpegFormatException
     */
    public JPEGImage parse() throws InvalidJpegFormatException, IOException {
        JPEGImage imgObject = new JPEGImage(mFilePath);
        byte[] bytes = new byte[2];
        BlockParser parser = new BlockParser(imgObject);
        while (true) {
            mIs.read(bytes);
            int marker = i16(bytes);
            JPEGMarkInfo info = JPEGMarker.getMarkInfo(marker);
            if(info == null){
                System.err.println("Cannot get mark info!");
                throw new InvalidJpegFormatException(String.format("Invalid block.mark:%4X", marker));
            }
            if (verbose > 0) {
                System.out.println("block:" + info.mName + " " + info.mDescription);
            }
            switch (info.mType)
            {
                case JPEGMarkInfo.TYPE_APP:
                    parser.parseApp(marker);
                    break;
                case JPEGMarkInfo.TYPE_DQT:
                    parser.parseDQT(marker);
                    break;
                case JPEGMarkInfo.TYPE_SOF:
                    parser.parseSOF(marker);
                    break;
                case JPEGMarkInfo.TYPE_DHT:
                    parser.parseDHT(marker);
                    break;
                case JPEGMarkInfo.TYPE_DRI:
                    System.err.println("DRI happen!");
                    parser.parseDRI(marker);
                    break;
                case JPEGMarkInfo.TYPE_SKIP:
                    parser.parseSkip(marker);
                    break;
                case JPEGMarkInfo.TYPE_SOS:
                    parser.parseSOS(marker);
                    parser.startScan();
                    close();
                    return imgObject;

                default:
                    System.err.print("unhandled mark:" + marker);
                    break;
            }

        }
    }

    private class BlockParser{

        private JPEGImage mImg;

        public BlockParser(JPEGImage imgObject) {
            mImg = imgObject;
        }

        void parseSkip(int marker) throws IOException {
            byte[] bytes = new byte[2];
            mIs.read(bytes);
            int num = i16(bytes)-2;
            bytes = new byte[num];
            mIs.read(bytes);
        }

        void parseApp(int marker) throws IOException {
            byte[] bytes = new byte[2];
            mIs.read(bytes);
            int num = i16(bytes)-2;
            bytes = new byte[num];
            mIs.read(bytes);

            String app = String.format("APP%d", (marker&0xF));
            mImg.setAppInfo(marker, bytes);
            if (marker == 0xFFE0 && cmpByte2Str(bytes,0,"JFIF\0")){

                // JFIF版本号
                int version = i16(bytes, 5);
                String jfif_unit;
                switch (bytes[7]){
                    case 1:// 点数/英寸
                        jfif_unit = "points/inch";
                        break;
                    case 2:// 点数/厘米
                        jfif_unit = "points/cm";
                        break;

                    default:// 无单位
                        jfif_unit = "None";
                        break;
                }

                // 水平分辨率、竖直分辨率
                int jfif_destiny_x,jfif_destiny_y;
                jfif_destiny_x = i16(bytes, 8);
                jfif_destiny_y = i16(bytes, 10);
                mImg.setJFIFInfo(version, jfif_unit, jfif_destiny_x, jfif_destiny_y);

                // 缩略图水平像素数目、竖直像素数目
                int thumbnail_horizontal_pixels, thumbnail_vertical_pixels;
                thumbnail_horizontal_pixels = bytes[12];
                thumbnail_vertical_pixels = bytes[13];
                int thumbnail_size = 3*thumbnail_horizontal_pixels*thumbnail_vertical_pixels;
                int[] thumbnail_RGB_bitmap = new int[thumbnail_size];
                for (int i = 0; i != thumbnail_size; ++i){
                    thumbnail_RGB_bitmap[i] = (bytes[i+14] & 0xff);
                }
                mImg.setThumbnail(thumbnail_horizontal_pixels, thumbnail_vertical_pixels, thumbnail_RGB_bitmap);
            }else if(marker == 0xFFE1 && cmpByte2Str(bytes,0,"Exif\0")){
                // TODO 解析exif信息
            }else if(marker == 0xFFE2 && cmpByte2Str(bytes,0,"FPXR\0")){
                // TODO 解析FlashPix信息
            }else if(marker == 0xFFE2 && cmpByte2Str(bytes,0,"ICC_PROFILE\0")){
                // TODO 解析ICC profile(描述设备色彩特性的数据文件)
//                    # Since an ICC profile can be larger than the maximum size of
//                    # a JPEG marker (64K), we need provisions to split it into
//                    # multiple markers. The format defined by the ICC specifies
//                    # one or more APP2 markers containing the following data:
//                    #   Identifying string      ASCII "ICC_PROFILE\0"  (12 bytes)
//                    #   Marker sequence number  1, 2, etc (1 byte)
//                    #   Number of markers       Total of APP2's used (1 byte)
//                    #   Profile data            (remainder of APP2 data)
//                    # Decoders should use the marker sequence numbers to
//                    # reassemble the profile, rather than assuming that the APP2
//                    # markers appear in the correct sequence.
//                    self.icclist.append(s)
            }else if(marker == 0xFFEE && cmpByte2Str(bytes,0,"Adobe\0")){
                // TODO 解析Adobe信息
                int adobe = i16(bytes,5);
                int adobe_transform = bytes[1];
            }
        }

        void parseDQT(int marker) throws IOException {
            byte[] bytes = new byte[2];
            mIs.read(bytes);
            int num = i16(bytes)-2;
            bytes = new byte[num];
            mIs.read(bytes);

            // 一个DQT块可能定义几个量化表
            int[] scan_index = new int[]{0};
            while (scan_index[0] < num) {
                JPEGDQT dqt = new JPEGDQT(bytes, scan_index);
                mImg.setDQT(dqt);
            }
        }

        void parseSOF(int marker) throws InvalidJpegFormatException, IOException {
            byte[] bytes = new byte[2];
            mIs.read(bytes);
            int num = i16(bytes)-2;
            bytes = new byte[num];
            mIs.read(bytes);

            int precision = bytes[0];
            if (precision != 8){
                throw new InvalidJpegFormatException("当前图像每个数据样本的位数为:"+precision+".\n暂只支持解析样本位数为8位的图像");
            }
            int height = i16(bytes, 1);
            int width = i16(bytes, 3);
            mImg.setSize(width, height, precision);
            switch (bytes[5])
            {
                case 1:
                    mImg.setColors(JPEGImage.COLORS_GREY);
                    break;
                case 3:
                    mImg.setColors(JPEGImage.COLORS_YCrCb);
                    break;
                case 4:
                    mImg.setColors(JPEGImage.COLORS_CMYK);
                    break;
                default:
                    throw new InvalidJpegFormatException();
            }
            for(int i=6;i!=bytes.length;i+=3){
                // 颜色分量id，水平采样因子，垂直采样因子，使用的量化表id
                int color_id = bytes[i];
                int h_samp = bytes[i+1] >>> 4;
                int v_samp = bytes[i+1] & 0x0f;
                int DQT_id = bytes[i+2];
                mImg.setLayer(new JPEGImage.JPEGLayer(color_id, v_samp, h_samp, DQT_id));
            }
        }

        void parseDHT(int marker) throws IOException {
            byte[] bytes = new byte[2];
            mIs.read(bytes);
            int num = i16(bytes)-2;
            bytes = new byte[num];
            mIs.read(bytes);
            if (verbose > 5) {
                System.out.print("DHT Data:");
                for (int i = 0; i != num; ++i) {
                    if (i % 8 == 0) {
                        System.out.println();
                    }
                    System.out.print(String.format("%4d,", bytes[i]));
                }
                System.out.println();
            }

            int[] scan_index = new int[]{0};
            while (scan_index[0] < num) {
                JPEGHuffman ht = new JPEGHuffman(bytes, scan_index);
                mImg.setHuffman(ht);
            }
        }

        void parseSOS(int marker) throws InvalidJpegFormatException, IOException {
            byte[] bytes = new byte[2];
            mIs.read(bytes);
            int num = i16(bytes)-2;
            bytes = new byte[num];
            mIs.read(bytes);

            int color_type = bytes[0];
            for(int i=0,base=1;i!=color_type;++i,base+=2){
                int color_id = bytes[base] ;
                int dc_huffman_id = bytes[base+1] >>> 4;
                int ac_huffman_id = bytes[base+1] & 0xf;
                JPEGImage.JPEGLayer layer = mImg.getLayer(color_id);
                layer.setHuffman(dc_huffman_id, ac_huffman_id);
            }
        }

        public void parseDRI(int marker) {

        }

        private void startScan() throws IOException {
            try {
                JPEGBitInputStream bis = new JPEGBitInputStream(mIs);
                Set<Integer> color_ids = mImg.getColorIDs();
                Map<Integer, Integer> dc_base = new HashMap<Integer, Integer>();
                while (true) {
                    scanColorUnit(bis, color_ids, dc_base);
                }
            } catch (MarkAppearException e) {
                // EOI.End of Image
                System.out.println("End of Image.");
            }
        }

        private void scanColorUnit(JPEGBitInputStream bis, Set<Integer> color_ids, Map<Integer, Integer> dc_base) throws MarkAppearException, IOException {

            // 对颜色空间进行扫描
            for (int color_id : color_ids) {
                JPEGImage.JPEGLayer layer = mImg.getLayer(color_id);
                // 对每一种颜色的采样值进行扫描
                for (int i = 0; i != layer.mHSamp*layer.mVSamp; ++i){
                    if (verbose > 1) {
                        System.out.print(mImg.getColors()[color_id - 1] + ":");
                    }
                    // 更新DC基值
                    int[] unit = new int[64];
                    Integer dc_val = dc_base.get(color_id);
                    try {
                        if (dc_val == null){dc_val = 0;}
                        dc_val = scanColor(bis, layer, dc_val, unit);
                        // 更新base值
                        dc_base.put(color_id, dc_val);
                        // 插入数据
                        mImg.addDataUnit(color_id, unit);
                    } catch (MarkAppearException e){
                        // EOI标志
                        if (e.mark == 0xd9){
                            throw e;
                        }
                        // RSTn标志,重置base值 FIXME : 这样处理对吗? 还是对所有color_id的base值都置0？
                        System.err.println("Mark : RSTn "+e.mark+" color:"+color_id);
                        dc_base.put(color_id, 0);
                    }
                }
            }
        }

        private int scanColor(JPEGBitInputStream bis, JPEGImage.JPEGLayer layer, int dc_base, int[] unit) throws MarkAppearException, IOException {
            JPEGHuffman huffman;
            StringBuffer buf = new StringBuffer();
            Integer weight;
            // 找到直流分量对应的权值，权值表示读取DC diff值需要读入多少bit
            do {
                buf.append(bis.readBit());
                huffman = mImg.getHuffman(JPEGHuffman.TYPE_DC, layer.mDCHuffmanID);
                weight = huffman.find(buf.toString());
            }while (weight == null);
            // DC实际值为diff值(读出)+上一Unit的DC值
            int dc_val = dc_base + convert(bis.readBitsString(weight));
            unit[0] = dc_val;
            if (verbose > 1) {
                System.out.print(String.format("DC:%3d",dc_val));
                System.out.print(" AC:");
            }

            // 最多63个交流分量的值
            for (int i = 1; i < 64; ++i){
                // 找到一个交流分量对应的值
                buf = new StringBuffer();
                do {
                    buf.append(bis.readBit());
                    huffman = mImg.getHuffman(JPEGHuffman.TYPE_AC, layer.mACHuffmanID);
                    weight = huffman.find(buf.toString());
                }while ( weight == null );

                // 权值为0,代表后续的交流分量全部为0
                if(weight == 0){
                    if (verbose > 1) {
                        System.out.print(String.format(", 0x%02x:", weight));
                    }
                    // 权值为0，代表剩下的AC分量全部为0
                    for ( ; i < 64; ++i){
                        // 后面的位置全部填充0
                        unit[i] = 0;
                    }
                    break;
                }else {
                    // 权值高4位表示前置有多少个0
                    int pre_zeros = weight >>> 4;
                    // 填充前置0
                    for (int j = 0; j != pre_zeros; ++j) {
                        unit[i + pre_zeros] = 0;
                    }
                    // 进行累加，i超过64则跳出
                    i += pre_zeros;
                    // 权值低4位表示读取AC值需要读入多少bit
                    int nBit_read = weight & 0x0f;
                    int ac_val = convert(bis.readBitsString(nBit_read));
                    // 填充交流分量
                    unit[i] = ac_val;
                    if (verbose > 1) {
                        System.out.print(String.format(", 0x%02x:(%2d, %2d)",
                                weight, pre_zeros, ac_val));
                    }
                }
            }
            if (verbose > 1) {
                System.out.println(" END");
            }
            return dc_val;
        }

        private int convert(String nStr){
            if (nStr == null || nStr.length()==0){
                return 0;
            }
            int num = Integer.parseInt(nStr, 2);
            int max_val = 1<<(nStr.length()-1);
            if (num < max_val ){
                num = -(max_val<<1) + num+1;
            }
            return num;
        }

        private boolean cmpByte2Str(byte[] bytes, int offset, String str){
            for (int i=0;i!=str.length();++i){
                if(str.charAt(i) != bytes[offset+i]){
                    return false;
                }
            }
            return true;
        }
    }



    public static class InvalidJpegFormatException extends Exception{
        InvalidJpegFormatException(){
            super();
        }

        InvalidJpegFormatException(String message){
            super(message);
        }

    }

    public static class MarkAppearException extends Exception{

        public int mark;

        MarkAppearException(int mark){
            super(String.format("Mark 0xFF%02x Appear!", mark));
            this.mark = mark;
        }

    }

    public static void main(String[] args){
        // 输出重定向
        BufferedOutputStream os = null;
        PrintStream ps = null;
//        try {
//            os = new BufferedOutputStream(new FileOutputStream("result_my.txt"), 1024);
//            ps = new PrintStream(os, false);
//            System.setOut(ps);
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }

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

        JPEGParser parser = null;
        try {
            for (String pic : pics) {
                try {
                    os = new BufferedOutputStream(new FileOutputStream("result_my_ori.txt"), 1024);
                    ps = new PrintStream(os, false);
                    System.setOut(ps);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                System.out.println("Parsing:"+pic);
                parser = new JPEGParser(pic);
                JPEGImage img = parser.parse();
                List<int[]> YUnits = img.getDataUnits().getColorUnit(1);
//                for (int[] unit:YUnits){
//                    for (int i = 0; i != unit.length; ++i) {
//                        if (i % 8 == 0) System.out.println();
//                        System.out.print(String.format("%5d",unit[i]));
//                    }
//                }System.out.println("\n========");
                Historgram[] YHistorgrams = img.getDataUnits().getHistorgram(1);
                for (Historgram his : YHistorgrams){
                    his.print();
                }

                ps.close();
                img.save(pic + "_saved.jpg");
                parser.close();

                try {
                    os = new BufferedOutputStream(new FileOutputStream("result_my_sav.txt"), 1024);
                    ps = new PrintStream(os, false);
                    System.setOut(ps);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                System.out.println("Parsing:"+pic+"_saved.jpg");
                parser = new JPEGParser(pic+"_saved.jpg");
                img = parser.parse();
                ps.close();

            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (InvalidJpegFormatException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (ps != null) {
                ps.close();
            }
            try {
                if (parser != null) {
                    parser.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
