此项目已转移到https://github.com/JaySon-Huang/SecretCamera 进行进一步的开发
=========

加密域下jpeg格式图像隐写
==========

## JPEGParser ##
JPEG压缩编码算法的主要计算步骤如下：  
0. 8*8分块。
1. 正向离散余弦变换(FDCT)。
2. 量化(quantization)。
3. Z字形编码(zigzag scan)。
4. 使用差分脉冲编码调制(DPCM)对直流系数(DC)进行编码。
5. 使用行程长度编码(RLE)对交流系数(AC)进行编码。
6. 熵编码。

需要对jpeg格式图像量化后、哈夫曼编码前的数据(在上面步骤2、3之间)进行加工处理。
此库的作用为对jpeg格式的图像进行解析，得到量化后数据，并再次进行保存。

## RDHCipher ##
利用密钥和(PBKDF2)[http://en.wikipedia.org/wiki/PBKDF2]生成伪随机数发生器的seed值，利用伪随机数发生器生成随机序列把jpeg数据单元(data unit)打乱达到隐藏图像原有内容的目的。
利用直方图平移来嵌入隐藏数据。
