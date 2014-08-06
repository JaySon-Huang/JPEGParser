package com.jayson.utils.jpeg;

import java.io.Closeable;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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
    public static final String[] COLORS_YCrCb = new String[]{"Y", "Cr", "Cb"};
    public static final String[] COLORS_CMYK = new String[]{"C", "M", "Y", "K"};

    private FileInputStream mIs;

    private Map<String, byte[]> mApp;
    private JPEGDQT[] mDQT;
    private JPEGHuffman[] mHuffmanDC;
    private JPEGHuffman[] mHuffmanAC;
    private int mHeight,mWidth;
    private JPEGInfo mInfos;

    public JPEGParser(String filename) throws IOException, InvalidJpegFormatException {
        mApp = new HashMap<String, byte[]>();
        mDQT = new JPEGDQT[4];
        mHuffmanDC = new JPEGHuffman[2];
        mHuffmanAC = new JPEGHuffman[2];
        mInfos = new JPEGInfo();

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

        try {
            parse();
        } catch (JPEGDQT.InvalidDQTSizeException e) {
            e.printStackTrace();
            throw new InvalidJpegFormatException();
        }
    }

    public int height(){
        return mHeight;
    }

    public int width(){
        return mWidth;
    }

    @Override
    public void close() throws IOException {
        if (mIs != null){
            mIs.close();
            mIs = null;
        }
    }

    private void parse() throws JPEGDQT.InvalidDQTSizeException, InvalidJpegFormatException {
        byte[] bytes = new byte[2];
        BlockParser parser = new BlockParser();
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
                    case JPEGMarkInfo.TYPE_SKIP:
                        parser.parseSkip(marker);
                        break;
                    case JPEGMarkInfo.TYPE_SOS:
                        parser.parseSOS(marker);
                        parser.startScan();
                        close();
                        return;

                    default:
                        System.out.print("unhandled mark:" + marker);
                        break;
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class BlockParser{

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
                mApp.put(app, bytes);
                if (marker == 0xFFE0 && cmpByte2Str(bytes,0,"JFIF\0")){
                    // TODO 有关图片的参数
                    int version = i16(bytes, 5);
                    String jfif_unit="None";
                    int jfif_destiny_x,jfif_destiny_y;
                    switch (bytes[7]){
                        case 1:
                            jfif_unit = "points/inch";
                            break;
                        case 2:
                            jfif_unit = "points/cm";
                            break;
                    }
                    jfif_destiny_x = i16(bytes, 8);
                    jfif_destiny_y = i16(bytes, 10);
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

        void parseDQT(int marker) throws JPEGDQT.InvalidDQTSizeException {
            try {
                byte[] bytes = new byte[2];
                mIs.read(bytes);
                int num = i16(bytes)-2;
                bytes = new byte[num];
                mIs.read(bytes);

                int precision = bytes[0] >>> 4;
                int DQT_id = bytes[0] & 0x0f;
                mDQT[DQT_id] = new JPEGDQT(bytes, precision);
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
                    throw new InvalidJpegFormatException();
                }
                mHeight = i16(bytes, 1);
                mWidth = i16(bytes, 3);
                switch (bytes[5])
                {
                    case 1:
                        mInfos.setColors(COLORS_GREY);
                        System.out.println("灰度图");
                        break;
                    case 3:
                        mInfos.setColors(COLORS_YCrCb);
                        System.out.println("YCrCb分量图");
                        break;
                    case 4:
                        mInfos.setColors(COLORS_CMYK);
                        System.out.println("CMYK分量图");
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
                    mInfos.addLayer(new JPEGInfo.JPEGLayer(color_id,v_samp,h_samp,DQT_id));
                    System.out.println(
                            String.format("color:%d v_samp:%d h_samp:%d DQT_id:%d",
                                    color_id, v_samp, h_samp, DQT_id) );
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

                JPEGHuffman ht = new JPEGHuffman(bytes);
                if(ht.mType == JPEGHuffman.TYPE_DC){
                    mHuffmanDC[ht.mID] = ht;
                }else if(ht.mType == JPEGHuffman.TYPE_AC){
                    mHuffmanAC[ht.mID] = ht;
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
                    JPEGInfo.JPEGLayer layer = mInfos.getLayer(color_id);
                    layer.setHuffman(dc_huffman_id, ac_huffman_id);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void startScan(){
            try {
                BitInputStream bis = new BitInputStream(mIs);
                int color_num = mInfos.getColors().length;
                int[] dc_base = new int[color_num];

                // YCbCr 扫描
                for (int color=1;color<=color_num;++color){
                    System.out.print(mInfos.getColors()[color-1]+":");
                    // 更新DC基值
                    dc_base[color-1] = scanColorUnit(bis, color, dc_base[color-1]);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private int scanColorUnit(BitInputStream bis, int color_id, int dc_base){
            try {
                JPEGInfo.JPEGLayer layer = mInfos.getLayer(color_id);
                StringBuffer buf = new StringBuffer();
                Integer weight;
                // 找到直流分量对应的权值，权值表示读取DC diff值需要读入多少bit
                do {
                    buf.append(bis.readBit());
                    weight = mHuffmanDC[layer.mDCHuffmanID]
                                .find(buf.toString());
                }while (weight == null);
                // DC实际值为diff值(读出)+上一Unit的DC值
                int dc_val = dc_base + bis.readBits(weight);
                System.out.print(String.format("DC:%3d",dc_val));
                System.out.print(" AC:");

                // 最多63个交流分量的值
                for (int i=1;i!=64;++i){
                    // 找到一个交流分量对应的值
                    buf = new StringBuffer();
                    do {
                        buf.append(bis.readBit());
                        weight = mHuffmanAC[layer.mACHuffmanID]
                                .find(buf.toString());
                    }while ( weight == null );
                    if(weight == 0){
                        // 权值为0，代表剩下的AC分量全部为0
                        System.out.println(" END");
                        return dc_val;
                    }
                    // 权值高4位表示前置有多少个0
                    int pre_zeros = weight >>> 4;
                    // 权值低4位表示读取AC值需要读入多少bit
                    int nBit_read = weight & 0x0f;
                    int ac_val = bis.readBits(nBit_read);
                    System.out.print(String.format(", (%2d, %2d)",pre_zeros,ac_val));
                }
                System.out.println(" END");
                return dc_val;

            } catch (IOException e) {
                e.printStackTrace();
                return 0;
            }
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



    public class InvalidJpegFormatException extends Exception{

    }

    public static void main(String[] args){
        try {
            JPEGParser parser = new JPEGParser("/Users/JaySon/Desktop/test.jpg");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (InvalidJpegFormatException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
