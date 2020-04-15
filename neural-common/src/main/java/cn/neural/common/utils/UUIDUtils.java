package cn.neural.common.utils;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * UUIDUtils
 * <br>
 * 注意:<br>
 * 1.支持获取36位String类型UUID.<br>
 * 2.支持获取32位String类型UUID.<br>
 * 3.支持获取19位String类型UUID.<br>
 * 4.支持获取15位String类型UUID.<br>
 * 5.支持获取15位Long类型UUID.<br>
 *
 * @author lry
 */
public class UUIDUtils {

    private final static String STR_BASE = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private final static char[] DIGITS = STR_BASE.toCharArray();
    private final static Map<Character, Integer> DIGIT_MAP = new HashMap<>();

    static {
        for (int i = 0; i < DIGITS.length; i++) {
            DIGIT_MAP.put(DIGITS[i], i);
        }
    }

    /**
     * 支持的最小进制数
     */
    private static final int MIN_RADIX = 2;
    /**
     * 支持的最大进制数
     */
    private static final int MAX_RADIX = DIGITS.length;

    /**
     * 获取36位UUID(原生UUID)
     */
    public static String randomUUID() {
        return UUID.randomUUID().toString();
    }

    /**
     * 获取32位UUID
     */
    public static String randomUUID32() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 获取19位的UUID
     */
    public static String randomUUID19() {
        // 产生UUID
        UUID uuid = UUID.randomUUID();
        // 分区转换
        return digits(uuid.getMostSignificantBits() >> 32, 8) +
                digits(uuid.getMostSignificantBits() >> 16, 4) +
                digits(uuid.getMostSignificantBits(), 4) +
                digits(uuid.getLeastSignificantBits() >> 48, 4) +
                digits(uuid.getLeastSignificantBits(), 12);
    }

    /**
     * 获取15位的UUID（精度有所损失）
     */
    public static String randomUUID15() {
        return UUIDMaker.generate();
    }

    /**
     * 获取15位的Long型UUID（精度有所损失）
     */
    public static long randomUUID15Long() {
        return toNumber(randomUUID15(), 10);
    }

    public static String randomUUIDBase64() {
        UUID uuid = UUID.randomUUID();
        byte[] byUuid = new byte[16];
        long least = uuid.getLeastSignificantBits();
        long most = uuid.getMostSignificantBits();
        long2bytes(most, byUuid, 0);
        long2bytes(least, byUuid, 8);

        return Base64.getEncoder().encodeToString(byUuid);
    }

    private static void long2bytes(long value, byte[] bytes, int offset) {
        for (int i = 7; i > -1; i--) {
            bytes[offset++] = (byte) ((value >> 8 * i) & 0xFF);
        }
    }

    /**
     * 将字符串转换为长整型数字
     *
     * @param s     数字字符串
     * @param radix 进制数
     */
    private static long toNumber(String s, int radix) {
        if (s == null) {
            throw new NumberFormatException("null");
        }
        if (radix < MIN_RADIX) {
            throw new NumberFormatException("radix " + radix + " less than Numbers.MIN_RADIX");
        }
        if (radix > MAX_RADIX) {
            throw new NumberFormatException("radix " + radix + " greater than Numbers.MAX_RADIX");
        }

        boolean negative = false;
        Integer digit, i = 0, len = s.length();
        long result = 0, limit = -Long.MAX_VALUE, multmin;
        if (len <= 0) {
            throw forInputString(s);
        }

        char firstChar = s.charAt(0);
        if (firstChar < '0') {
            if (firstChar == '-') {
                negative = true;
                limit = Long.MIN_VALUE;
            } else if (firstChar != '+') {
                throw forInputString(s);
            }
            if (len == 1) {
                throw forInputString(s);
            }
            i++;
        }

        multmin = limit / radix;
        while (i < len) {
            digit = DIGIT_MAP.get(s.charAt(i++));
            if (digit == null || digit < 0 || result < multmin) {
                throw forInputString(s);
            }
            result *= radix;
            if (result < limit + digit) {
                throw forInputString(s);
            }
            result -= digit;
        }

        return negative ? result : -result;
    }

    /**
     * 将长整型数值转换为指定的进制数（最大支持62进制，字母数字已经用尽）
     *
     * @param num   num
     * @param radix radix
     * @return string value
     */
    private static String toString(long num, int radix) {
        if (radix < MIN_RADIX || radix > MAX_RADIX) {
            radix = 10;
        }
        if (radix == 10) {
            return Long.toString(num);
        }

        final int size = 65;
        int charPos = 64;
        char[] buf = new char[size];
        boolean negative = (num < 0);
        if (!negative) {
            num = -num;
        }
        while (num <= -radix) {
            buf[charPos--] = DIGITS[(int) (-(num % radix))];
            num = num / radix;
        }
        buf[charPos] = DIGITS[(int) (-num)];
        if (negative) {
            buf[--charPos] = '-';
        }

        return new String(buf, charPos, (size - charPos));
    }

    private static String digits(long val, int digits) {
        long hi = 1L << (digits * 4);
        return toString(hi | (val & (hi - 1)), MAX_RADIX).substring(1);
    }

    private static NumberFormatException forInputString(String s) {
        return new NumberFormatException("For input string: " + s);
    }

    private static class UUIDMaker {

        private final static String STR = "0123456789abcdefghijklmnopqrstuvwxyz";
        private final static int PIX_LEN = STR.length();
        private static volatile int pixOne = 0;
        private static volatile int pixTwo = 0;
        private static volatile int pixThree = 0;
        private static volatile int pixFour = 0;

        /**
         * 生成短时间内不会重复的长度为15位的字符串，主要用于模块数据库主键生成使用。<br/>
         * 生成策略为获取自1970年1月1日零时零分零秒至当前时间的毫秒数的16进制字符串值，该字符串值为11位<br/>
         * 并追加四位"0-z"的自增字符串.<br/>
         * 如果系统时间设置为大于<b>2304-6-27 7:00:26<b/>的时间，将会报错！<br/>
         * 由于系统返回的毫秒数与操作系统关系很大，所以本方法并不准确。<br/>
         * 本方法可以保证在系统返回的一个毫秒数内生成36的4次方个（1679616）ID不重复。<br/>
         */
        private synchronized static String generate() {
            String hexString = Long.toHexString(System.currentTimeMillis());
            pixFour++;
            if (pixFour == PIX_LEN) {
                pixFour = 0;
                pixThree++;
                if (pixThree == PIX_LEN) {
                    pixThree = 0;
                    pixTwo++;
                    if (pixTwo == PIX_LEN) {
                        pixTwo = 0;
                        pixOne++;
                        if (pixOne == PIX_LEN) {
                            pixOne = 0;
                        }
                    }
                }
            }

            return hexString + STR.charAt(pixOne) + STR.charAt(pixTwo) +
                    STR.charAt(pixThree) + STR.charAt(pixFour);
        }
    }

}