package org.micro.neural.common.utils;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Message Digest Utils
 *
 * @author lry
 */
public class MessageDigestUtils {

    /**
     * SHA1,SHA256
     */
    private static final String MESSAGE_DIGEST_ALGORITHM_NAME = "MD5";

    private static MessageDigest MESSAGE_DIGEST;

    static {
        try {
            MESSAGE_DIGEST = MessageDigest.getInstance(MESSAGE_DIGEST_ALGORITHM_NAME);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    /**
     * Generates a digest based on the contents of a String.
     *
     * @param val     specifies the input data.
     * @param charset specifies the encoding of the input data.
     * @return digest as long.
     */
    public static int createHash(String val, Charset charset) {
        return createHash(val.getBytes(charset));
    }

    /**
     * Generates a digest based on the contents of a String.
     *
     * @param val specifies the input data. The encoding is expected to be UTF-8.
     * @return digest as long.
     */
    public static int createHash(String val) {
        return createHash(val, StandardCharsets.UTF_8);
    }

    /**
     * Generates a digest based on the contents of an array of bytes.
     *
     * @param data specifies input data.
     * @return digest as long.
     */
    public static int createHash(byte[] data) {
        return createHashes(data, 1)[0];
    }

    /**
     * Generates digests based on the contents of an array of bytes and splits
     * the result into 4-byte int's and store them in an array.
     * The digest function is called until the required number of
     * int's are produced. For each call to digest a salt
     * is prepended to the data. The salt is increased by 1 for each call.
     *
     * @param data   specifies input data.
     * @param hashes number of hashes/int's to produce.
     * @return array of int-sized hashes
     */
    public static int[] createHashes(byte[] data, int hashes) {
        int[] result = new int[hashes];

        int k = 0;
        byte salt = 0;
        while (k < hashes) {
            byte[] digest;
            synchronized (MESSAGE_DIGEST) {
                MESSAGE_DIGEST.update(salt);
                salt++;
                digest = MESSAGE_DIGEST.digest(data);
            }

            for (int i = 0; i < digest.length / 4 && k < hashes; i++) {
                int h = 0;
                for (int j = (i * 4); j < (i * 4) + 4; j++) {
                    h <<= 8;
                    h |= ((int) digest[j]) & 0xFF;
                }
                result[k] = h;
                k++;
            }
        }

        return result;
    }

}
