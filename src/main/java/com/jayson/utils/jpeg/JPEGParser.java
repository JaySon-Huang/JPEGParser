package com.jayson.utils.jpeg;

import com.jayson.utils.Historgram;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


import static com.jayson.utils.jpeg.b2i.i16;

/**
 * Created by JaySon on 14-8-3.
 */
public class JPEGParser implements Closeable{


    /**
     * start of image
     */
    public static final int SOI = 0xFFD8;

    public static final String[] COLORS_GREY = new String[]{"L"};
    public static final String[] COLORS_YCrCb = new String[]{" Y", "Cr", "Cb"};
    public static final String[] COLORS_CMYK = new String[]{"C", "M", "Y", "K"};

    private FileInputStream mIs;

    public JPEGParser(String filename) throws IOException, InvalidJpegFormatException {

        mIs = new FileInputStream(filename);
        byte[] bytes = new byte[2];
        try {
            mIs.read(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(i16(bytes) != SOI){
            throw new InvalidJpegFormatException();
        }
        mIs.read(bytes);

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
    public JPEGImage parse() throws InvalidJpegFormatException {
        JPEGImage imgObject = new JPEGImage();
        byte[] bytes = new byte[2];
        BlockParser parser = new BlockParser(imgObject);
        try {
            while (true){
                mIs.read(bytes);
                int marker = i16(bytes);
                JPEGMarkInfo info = JPEGMarker.getMarkInfo(marker);
                if(info == null){
                    System.err.println("null info!");
                    continue;
                }

                System.out.println("block:"+info.mName+" "+info.mDescription);
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
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private class BlockParser{

        private JPEGImage mImg;

        public BlockParser(JPEGImage imgObject) {
            try {
                mImg = imgObject;
                byte[] bytes = new byte[2];
                mIs.read(bytes);
                int num = i16(bytes)-2;
                bytes = new byte[num];
                mIs.read(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        void parseSkip(int marker){
            try {
                byte[] bytes = new byte[2];
                mIs.read(bytes);
                int num = i16(bytes)-2;
                bytes = new byte[num];
                mIs.read(bytes);

            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        void parseApp(int marker){
            try {
                byte[] bytes = new byte[2];
                mIs.read(bytes);
                int num = i16(bytes)-2;
                bytes = new byte[num];
                mIs.read(bytes);

                String app = String.format("APP%d", (marker&0xF));
                mImg.setAppInfo(app, bytes);
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
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        void parseDQT(int marker) {
            try {
                byte[] bytes = new byte[2];
                mIs.read(bytes);
                int num = i16(bytes)-2;
                bytes = new byte[num];
                mIs.read(bytes);

                // 一个DQT块可能定义几个量化表
                int[] scan_index = new int[]{0};
                while (scan_index[0] < num){
                    JPEGDQT dqt = new JPEGDQT(bytes, scan_index);
                    mImg.setDQT(dqt);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        void parseSOF(int marker) throws InvalidJpegFormatException {
            try {
                byte[] bytes = new byte[2];
                mIs.read(bytes);
                int num = i16(bytes)-2;
                bytes = new byte[num];
                mIs.read(bytes);

                int precision = bytes[0];
                if (precision != 8){
                    throw new InvalidJpegFormatException("当前图像每个数据样本的位数为:"+precision+".只支持解析样本位数为8位的图像");
                }
                int height = i16(bytes, 1);
                int width = i16(bytes, 3);
                mImg.setSize(width, height);
                switch (bytes[5])
                {
                    case 1:
                        mImg.setColors(COLORS_GREY);
                        break;
                    case 3:
                        mImg.setColors(COLORS_YCrCb);
                        break;
                    case 4:
                        mImg.setColors(COLORS_CMYK);
                        break;
                    default:
                        throw new InvalidJpegFormatException();
                }
                for(int i=6;i!=bytes.length;i+=3){
                    // 颜色分量id，水平采样因子，垂直采样因子，使用的量化表id
                    int color_id = bytes[i];
                    int v_samp = bytes[i+1] >>> 4;
                    int h_samp = bytes[i+1] & 0x0f;
                    int DQT_id = bytes[i+2];
                    mImg.setLayer(new JPEGImage.JPEGLayer(color_id, v_samp, h_samp, DQT_id));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        void parseDHT(int marker){
            try {
                byte[] bytes = new byte[2];
                mIs.read(bytes);
                int num = i16(bytes)-2;
                bytes = new byte[num];
                mIs.read(bytes);

                int[] scan_index = new int[]{0};
                while (scan_index[0] < num) {
                    JPEGHuffman ht = new JPEGHuffman(bytes, scan_index);
                    mImg.setHuffman(ht);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        void parseSOS(int marker) throws InvalidJpegFormatException {
            try {
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
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void parseDRI(int marker) {
            
        }

        private void startScan(){
            try {
                BitInputStream bis = new BitInputStream(mIs);
                Set<Integer> color_ids = mImg.getColorIDs();
                Map<Integer, Integer> dc_base = new HashMap<Integer, Integer>();
                while (true) {
                    scanColorUnit(bis, color_ids, dc_base);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (MarkAppearException e) {
                if (e.mark == (byte)0xD9){
                    // EOI.End of Image
//                    System.out.println("End of Image.");
                    return;
                }else{
                    e.printStackTrace();
                }
            }
        }

        private void scanColorUnit(BitInputStream bis, Set<Integer> color_ids, Map<Integer, Integer> dc_base) throws MarkAppearException {

            // 对颜色空间进行扫描
            for (int color_id : color_ids) {
                JPEGImage.JPEGLayer layer = mImg.getLayer(color_id);
                // 对每一种颜色的采样值进行扫描
                for (int i = 0; i != layer.mHSamp*layer.mVSamp; ++i){
//                    System.out.print(mImg.getColors()[color - 1] + ":");
                    // 更新DC基值
                    int dc_new = scanColor(bis, layer, dc_base.getOrDefault(color_id, 0));
                    dc_base.put(color_id, dc_new);
                }
            }
        }

        private int scanColor(BitInputStream bis, JPEGImage.JPEGLayer layer, int dc_base) throws MarkAppearException {
            try {
                JPEGHuffman huffman;
                StringBuffer buf = new StringBuffer();
                Integer weight;
                int[] unit = new int[64];
                // 找到直流分量对应的权值，权值表示读取DC diff值需要读入多少bit
                do {
                    buf.append(bis.readBit());
                    huffman = mImg.getHuffman(JPEGHuffman.TYPE_DC, layer.mDCHuffmanID);
                    weight = huffman.find(buf.toString());
                }while (weight == null);
                // DC实际值为diff值(读出)+上一Unit的DC值
                int dc_val = dc_base + convert(bis.readBitsString(weight));
                unit[0] = dc_val;
//                System.out.print(String.format("DC:%3d",dc_val));
//                System.out.print(" AC:");

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
//                        System.out.print(String.format(", 0x%02x:",weight));
                        // 权值为0，代表剩下的AC分量全部为0
                        for ( ; i < 64; ++i){
                            // 后面的位置全部填充0
                            unit[i] = 0;
                        }
                        break;
                    }

                    // 权值高4位表示前置有多少个0
                    int pre_zeros = weight >>> 4;
                    // 填充前置0
                    for (int j = 0; j != pre_zeros; ++j){
                        unit[i+pre_zeros] = 0;
                    }
                    // 进行累加，i超过64则跳出
                    i += pre_zeros;
                    // 权值低4位表示读取AC值需要读入多少bit
                    int nBit_read = weight & 0x0f;
                    int ac_val = convert(bis.readBitsString(nBit_read));
                    // 填充交流分量
                    unit[i] = ac_val;
//                    System.out.print(String.format(", 0x%02x:(%2d, %2d)",
//                            weight, pre_zeros, ac_val));
                }
                mImg.addDataUnit(unit);
//                System.out.println(" END");
                return dc_val;

            } catch (IOException e) {
                e.printStackTrace();
                return 0;
            } catch (MarkAppearException e) {
                // TODO 部分mark可以处理？
//                if ((e.mark&0xf0) == 0xd0){
//
//                }
                throw e;
            }
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

        protected boolean cmpByte2Str(byte[] bytes, int offset, String str){
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

        MarkAppearException(byte mark){
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
                "/Users/JaySon/Pictures/IMG_20140508_085331.jpg",
                "/Users/JaySon/Pictures/IMG_20140508_085558.jpg",
                "/Users/JaySon/Pictures/IMG_20140508_090150.jpg",
                "/Users/JaySon/Pictures/IMG_20140508_092000.jpg",
                "/Users/JaySon/Pictures/IMG_20140508_115427.jpg",
                "/Users/JaySon/Pictures/IMG_20140508_140426.jpg",
                "/Users/JaySon/Pictures/IMG_20140810_122739.jpg",
                "/Users/JaySon/Pictures/IMG_20140810_122739_1.jpg",
                "/Users/JaySon/Pictures/IMG_20140810_122741.jpg",
                "/Users/JaySon/Pictures/IMG_20140810_151927.jpg",
                "/Users/JaySon/Pictures/PANO_20140613_191243.jpg",
                "/Users/JaySon/Pictures/周 颠倒.jpg"
        };

        try {
            Historgram[] historgrams = new Historgram[64];
            for (String pic : pics) {
                System.out.println("Parsing:"+pic);
                JPEGParser parser = new JPEGParser(pic);
                JPEGImage img = parser.parse();

                for (int i = 0; i != historgrams.length; ++i) {
                    historgrams[i] = new Historgram();
                }
                for (int[] dataUnit : img.getDataUnits()) {
//                System.out.println("[");
                    for (int i = 0; i != 8; ++i) {
                        for (int j = 0; j != 8; ++j) {
                            historgrams[i * 8 + j].addN(dataUnit[i * 8 + j]);
//                        System.out.print(String.format("%3d ,", dataUnit[i*8+j]));
                        }
//                    System.out.println();
                    }
//                System.out.println("]");
                }

                for (int i = 0; i != 64; ++i) {
                    System.out.print(String.format("%3d:", i));
                    historgrams[i].print(System.out);
                }
                System.out.println("total:" + img.getDataUnits().size() + " units");
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
        }

    }
}
