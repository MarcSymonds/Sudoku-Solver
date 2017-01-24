package me.marcsymonds.sudokusolver;

/**
 * Created by Marc on 11/12/2016.
 */

class Util {
    static boolean toBoolean(String v) {
        return !v.equals("0");
    }

    static String toString(boolean v) {
        return v ? "1" : "0";
    }

    static String toString(byte[] b) {
        int i;
        String data = "";

        for (i = 0; i < b.length; i++) {
            data = data + String.valueOf((int) b[i]);
            if (i < (b.length - 1)) {
                data = data + ":";
            }
        }

        return data;
    }

}
