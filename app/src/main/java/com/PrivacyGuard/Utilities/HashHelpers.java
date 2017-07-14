package com.PrivacyGuard.Utilities;

import java.security.MessageDigest;

/**
 * Created by frank on 12/19/14.
 */
public class HashHelpers {
    private static String convertToHex(byte[] data, int maxLen) {
        StringBuilder buf = new StringBuilder();
        int crtLen = 0;
        for (byte b : data) {
            int halfbyte = (b >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                buf.append((0 <= halfbyte) && (halfbyte <= 9) ? (char) ('0' + halfbyte) : (char) ('a' + (halfbyte - 10)));
                halfbyte = b & 0x0F;
            } while (two_halfs++ < 1);
            crtLen++;
            if (crtLen == maxLen) break;
        }
        return buf.toString();
    }

    public static String SHA1(String text, int maxLen) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(text.getBytes("iso-8859-1"), 0, text.length());
            byte[] sha1hash = md.digest();
            return convertToHex(sha1hash, maxLen);
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    public static String SHA2(String text, int maxLen) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(text.getBytes("iso-8859-1"), 0, text.length());
            byte[] sha1hash = md.digest();
            return convertToHex(sha1hash, maxLen);
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    public static String MD5(String text, int maxLen) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(text.getBytes("iso-8859-1"), 0, text.length());
            byte[] sha1hash = md.digest();
            return convertToHex(sha1hash, maxLen);
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
