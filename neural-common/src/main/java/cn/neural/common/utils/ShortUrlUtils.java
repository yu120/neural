package cn.neural.common.utils;

import javax.xml.bind.DatatypeConverter;
import java.security.MessageDigest;

/**
 * ShortUrlUtils
 *
 * @author lry
 */
public class ShortUrlUtils {

    /**
     * 可以自定义生成 MD5 加密字符传前的混合 KEY
     */
    private static final String KEY = "lry";
    /**
     * 要使用生成URL的字符
     * 在进制表示中的字符集合，0-Z分别用于表示最大为62进制的符号表示
     */
    private static final char[] DIGITS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
            'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
            'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'};

    /**
     * 基于MD5生成短连接
     *
     * @param url url
     * @return url array
     * @throws Exception exception
     */
    public static String[] shortUrl(String url) throws Exception {
        // 对传入网址进行 MD5 加密
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update((KEY + url).getBytes());
        String encryptResult = DatatypeConverter.printHexBinary(md.digest()).toUpperCase();

        String[] resUrl = new String[4];
        for (int i = 0; i < 4; i++) {
            // 把加密字符按照8位一组16进制与0x3FFFFFFF进行位与运算
            String tempSubString = encryptResult.substring(i * 8, i * 8 + 8);
            // 这里需要使用long型来转换，因为Integer.parseInt()只能处理31位,首位为符号位,如果不用long，则会越界
            long lHexLong = 0x3FFFFFFF & Long.parseLong(tempSubString, 16);
            StringBuilder outChars = new StringBuilder();
            for (int j = 0; j < 6; j++) {
                // 把得到的值与 0x0000003D 进行位与运算，取得字符数组 chars 索引(具体需要看chars数组的长度,以防下标溢出，注意起点为0)
                long index = 0x0000003D & lHexLong;
                // 把取得的字符相加
                outChars.append(DIGITS[(int) index]);
                // 每次循环按位右移 5 位
                lHexLong = lHexLong >> 5;
            }

            // 把字符串存入对应索引的输出数组
            resUrl[i] = outChars.toString();
        }

        return resUrl;
    }

    /**
     * 将十进制的数字转换为指定进制的字符串
     *
     * @param number 十进制的数字
     * @param seed   指定的进制
     * @return 指定进制的字符串
     */
    public static String toOtherNumberSystem(long number, int seed) {
        if (number < 0) {
            number = ((long) 2 * 0x7fffffff) + number + 2;
        }

        char[] buf = new char[32];
        int charPos = 32;
        while ((number / seed) > 0) {
            buf[--charPos] = DIGITS[(int) (number % seed)];
            number /= seed;
        }

        buf[--charPos] = DIGITS[(int) (number % seed)];
        return new String(buf, charPos, (32 - charPos));
    }

    /**
     * 将其它进制的数字（字符串形式）转换为十进制的数字
     *
     * @param number 其它进制的数字（字符串形式）
     * @param seed   指定的进制，也就是参数str的原始进制
     * @return 十进制的数字
     */
    public static long toDecimalNumber(String number, int seed) {
        char[] charBuf = number.toCharArray();
        if (seed == 10) {
            return Long.parseLong(number);
        }

        long result = 0, base = 1;
        for (int i = charBuf.length - 1; i >= 0; i--) {
            int index = 0;
            for (int j = 0, length = DIGITS.length; j < length; j++) {
                // 找到对应字符的下标，对应的下标才是具体的数值
                if (DIGITS[j] == charBuf[i]) {
                    index = j;
                }
            }
            result += index * base;
            base *= seed;
        }

        return result;
    }

}