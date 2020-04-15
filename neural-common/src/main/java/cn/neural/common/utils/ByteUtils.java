package cn.neural.common.utils;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * ByteUtils
 * <p>
 * 1.大端模式（Big-endian）：高位字节排放在内存的低地址端，低位字节排放在内存的高地址端。即高位在前,低位在后
 * 2.小端模式（Little-endian）：低位字节排放在内存的低地址端，高位字节排放在内存的高地址端。即低位在前高位在后
 * <p>
 * 案例：int value = 0x12345678
 * 地址顺序：低地址--->高地址(即下标：0,1,2,3)
 * 大端模式：byte[4]{0x12, 0x34, 0x56, 0x78}
 * 小端模式：byte[4]{0x78, 0x56, 0x34, 0x12}
 *
 * @author lry
 */
public class ByteUtils {

    /**
     * byte to hex
     *
     * @param b byte
     * @return 16进制字符串
     */
    public static String byte2Hex(byte b) {
        return String.format("0x%02X", b);
    }

    /**
     * bytes to hex
     *
     * @param bytes byte[]
     * @return hex
     */
    public static String bytes2Hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte tempByte : bytes) {
            sb.append(byte2Hex(tempByte)).append(" ");
        }

        return sb.toString();
    }

    /**
     * 异或运算
     *
     * @param bytes bytes
     * @return byte
     */
    public static byte xor(byte[] bytes) {
        byte result = bytes[0];
        for (int i = 1; i < bytes.length; i++) {
            result ^= bytes[i];
        }

        return result;
    }

    /**
     * byte,byte[] concat
     *
     * @param firstByte first byte
     * @param bytes     byte[]
     * @return byte[]
     */
    public static byte[] concat(byte firstByte, byte[] bytes) {
        byte[] result = new byte[1 + bytes.length];
        result[0] = firstByte;
        System.arraycopy(bytes, 0, result, 1, bytes.length);
        return result;
    }

    /**
     * byte,byte[] concat
     *
     * @param bytes    byte[]
     * @param lastByte last byte
     * @return byte[]
     */
    public static byte[] concat(byte[] bytes, byte lastByte) {
        byte[] result = new byte[1 + bytes.length];
        System.arraycopy(bytes, 0, result, 0, bytes.length);
        result[result.length - 1] = lastByte;
        return result;
    }

    /**
     * byte[],byte[] concat
     *
     * @param first  first byte[]
     * @param second second byte[]
     * @return byte[]
     */
    public static byte[] concat(byte[] first, byte[] second) {
        byte[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    // === short

    /**
     * byte[2] to short [Big-Endian]
     *
     * @param bytes  bytes
     * @param offset start offset
     * @return short
     */
    public static short bytes2ShortBig(byte[] bytes, int offset) {
        int tmp = 0;
        tmp += (bytes[offset] & 0xff) << 8;
        tmp += bytes[offset + 1] & 0xff;
        return (short) tmp;
    }

    /**
     * byte[2] to short [Little-Endian]
     *
     * @param bytes  bytes
     * @param offset start offset
     * @return short
     */
    public static short bytes2ShortLittle(byte[] bytes, int offset) {
        int tmp = 0;
        tmp += (bytes[offset + 1] & 0xff) << 8;
        tmp += bytes[offset] & 0xff;
        return (short) tmp;
    }

    /**
     * short to byte[2] [Big-Endian]
     *
     * @param num short number
     * @return byte[2]
     */
    public static byte[] short2BytesBig(short num) {
        byte[] bytes = new byte[2];
        bytes[0] = (byte) (num >>> 8);
        bytes[1] = (byte) (num);
        return bytes;
    }

    /**
     * short to byte[2]  [Little-Endian]
     *
     * @param num short number
     * @return byte[2]
     */
    public static byte[] short2BytesLittle(short num) {
        byte[] bytes = new byte[2];
        bytes[0] = (byte) (num);
        bytes[1] = (byte) (num >>> 8);
        return bytes;
    }

    // === int

    /**
     * byte[4] to int [Big-Endian]
     *
     * @param bytes  bytes
     * @param offset start offset
     * @return int
     */
    public static int bytes2IntBig(byte[] bytes, int offset) {
        int result = 0;
        result += (bytes[offset] & 0xff) << 24;
        result += (bytes[offset + 1] & 0xff) << 16;
        result += (bytes[offset + 2] & 0xff) << 8;
        result += bytes[offset + 3] & 0xff;
        return result;
    }

    /**
     * byte[4] to int [Little-Endian]
     *
     * @param bytes  bytes
     * @param offset start offset
     * @return int
     */
    public static int bytes2IntLittle(byte[] bytes, int offset) {
        int result = 0;
        result += bytes[offset] & 0xff;
        result += (bytes[offset + 1] & 0xff) << 8;
        result += (bytes[offset + 2] & 0xff) << 16;
        result += (bytes[offset + 3] & 0xff) << 24;
        return result;
    }

    /**
     * int to byte[4] [Big-Endian]
     *
     * @param num int number
     * @return byte[4]
     */
    public static byte[] int2bytesBig(int num) {
        byte[] result = new byte[4];
        result[0] = (byte) (num >>> 24);
        result[1] = (byte) (num >>> 16);
        result[2] = (byte) (num >>> 8);
        result[3] = (byte) num;
        return result;
    }

    /**
     * int to byte[4] [Little-Endian]
     *
     * @param num int number
     * @return byte[4]
     */
    public static byte[] int2bytesLittle(int num) {
        byte[] result = new byte[4];
        result[0] = (byte) num;
        result[1] = (byte) (num >>> 8);
        result[2] = (byte) (num >>> 16);
        result[3] = (byte) (num >>> 24);
        return result;
    }

    // === long

    /**
     * long to byte[8] [Big-Endian]
     *
     * @param num long number
     * @return byte[8]
     */
    public static byte[] long2bytesBig(long num) {
        byte[] result = new byte[8];
        result[7] = (byte) num;
        result[6] = (byte) (num >>> 8);
        result[5] = (byte) (num >>> 16);
        result[4] = (byte) (num >>> 24);
        result[3] = (byte) (num >>> 32);
        result[2] = (byte) (num >>> 40);
        result[1] = (byte) (num >>> 48);
        result[0] = (byte) (num >>> 56);
        return result;
    }

    /**
     * long to byte[8] [Little-Endian]
     *
     * @param num long number
     * @return byte[8]
     */
    public static byte[] long2bytesLittle(long num) {
        byte[] result = new byte[8];
        result[0] = (byte) num;
        result[1] = (byte) (num >>> 8);
        result[2] = (byte) (num >>> 16);
        result[3] = (byte) (num >>> 24);
        result[4] = (byte) (num >>> 32);
        result[5] = (byte) (num >>> 40);
        result[6] = (byte) (num >>> 48);
        result[7] = (byte) (num >>> 56);
        return result;
    }

    /**
     * byte[8] to long [Big-Endian]
     *
     * @param bytes  bytes
     * @param offset start offset
     * @return long
     */
    public static long bytes2LongBig(byte[] bytes, int offset) {
        long result = 0;
        result += (bytes[offset] & 0xffL) << 56;
        result += (bytes[offset + 1] & 0xffL) << 48;
        result += (bytes[offset + 2] & 0xffL) << 40;
        result += (bytes[offset + 3] & 0xffL) << 32;
        result += (bytes[offset + 4] & 0xffL) << 24;
        result += (bytes[offset + 5] & 0xffL) << 16;
        result += (bytes[offset + 6] & 0xffL) << 8;
        result += (bytes[offset + 7]) & 0xffL;
        return result;
    }

    /**
     * byte[8] to long [Little-Endian]
     *
     * @param bytes  bytes
     * @param offset start offset
     * @return long
     */
    public static long bytes2LongLittle(byte[] bytes, int offset) {
        long result = 0;
        result += bytes[offset] & 0xffL;
        result += (bytes[offset + 1] & 0xffL) << 8;
        result += (bytes[offset + 2] & 0xffL) << 16;
        result += (bytes[offset + 3] & 0xffL) << 24;
        result += (bytes[offset + 4] & 0xffL) << 32;
        result += (bytes[offset + 5] & 0xffL) << 40;
        result += (bytes[offset + 6] & 0xffL) << 48;
        result += (bytes[offset + 7] & 0xffL) << 56;
        return result;
    }

    /**
     * 十进制字符串转为十六进制后，高低位互换，再转为十进制
     *
     * @param str long类型字符
     * @return 互换后的十进制
     */
    public static String long2Little(String str) {
        String hexStr = Long.toHexString(Long.parseLong(str));
        char[] hexArray = hexStr.toCharArray();
        int maxIndex = hexArray.length - 1;
        char[] tempHexArray = new char[hexArray.length];
        for (int i = maxIndex; i > 0; i -= 2) {
            tempHexArray[maxIndex - i] = hexArray[i - 1];
            tempHexArray[maxIndex - i + 1] = hexArray[i];
        }

        return String.format("%010d", Long.valueOf(new String(tempHexArray), 16));
    }

    // === 进制转换

    /**
     * 36以内进制转换
     *
     * @param value     from hex
     * @param fromRadix from radix, 2 ≤ fromRadix ≤ 36. eg:2、8、10、16、36、62、64
     * @param toRadix   to radix 2, ≤ fromRadix ≤ 36
     * @return to hex
     */
    public static String hexMax36(String value, int fromRadix, int toRadix) {
        return new BigInteger(value, fromRadix).toString(toRadix);
    }

    /**
     * 62以内禁止相互转换
     *
     * @param value     from hex
     * @param fromRadix from radix, 2 ≤ fromRadix ≤ 62
     * @param toRadix   to radix 2, ≤ fromRadix ≤ 62
     * @return to hex
     */
    public static String hex(String value, int fromRadix, int toRadix) {
        return encode10N(decodeN10(value, fromRadix), toRadix);
    }

    /**
     * 初始化 62 进制数据，索引位置代表字符的数值，比如 A代表10，z代表61等
     */
    private static final String CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    /**
     * 将N进制字符串转为十进制字符串
     *
     * @param value N进制字符串
     * @return 十进制字符串
     */
    public static String decodeN10(final String value, final int scale) {
        String tempValue = value.replace("^0*", "");
        BigInteger num = BigInteger.valueOf(0);
        for (int i = 0; i < tempValue.length(); i++) {
            BigInteger index = BigInteger.valueOf(CHARS.indexOf(tempValue.charAt(i)));
            BigInteger scalePow = BigInteger.valueOf(scale).pow(tempValue.length() - i - 1);
            num = num.add(index.multiply(scalePow));
        }

        return num.toString();
    }

    /**
     * 将十进制转为N进制字符串
     *
     * @param number 十进制
     * @return N进制字符串
     */
    public static String encode10N(final String number, final int scale) {
        BigInteger num = new BigInteger(number);
        BigInteger scaleBigInteger = BigInteger.valueOf(scale);
        StringBuilder sb = new StringBuilder();
        while (num.compareTo(BigInteger.valueOf(scale - 1)) > 0) {
            // 对 scale 进行求余，然后将余数追加至 sb 中，由于是从末位开始追加的，因此最后需要反转（reverse）字符串
            sb.append(CHARS.charAt(num.divideAndRemainder(scaleBigInteger)[1].intValue()));
            num = num.divide(scaleBigInteger);
        }

        sb.append(CHARS.charAt(num.intValue()));
        return sb.reverse().toString();
    }

}
