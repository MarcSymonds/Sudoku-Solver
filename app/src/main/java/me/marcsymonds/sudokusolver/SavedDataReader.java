package me.marcsymonds.sudokusolver;

import android.util.Log;

/**
 * Class for reading a string which was created to save the data for an object.
 * <p>
 * The data being read, must be read in the same format, including number of digits as it was
 * initially written.
 * <p>
 * Could probably create a complementary SaveDataWriter class for creating the string. But not for
 * now.
 */
class SavedDataReader {
    final private String TAG = SavedDataReader.class.getSimpleName();

    private String data;
    private String[] sections;
    private int currentSection;
    private int index;

    /**
     * Constructor.
     *
     * @param savedData string containing the saved data to be read. The data may have been created
     *                  with # as section separators.
     */
    SavedDataReader(String savedData) {
        data = savedData;
        sections = data.split("#");
        currentSection = 0;
        index = 0;

        Log.d(TAG, "Saved data: " + savedData);
    }

    /**
     * Read a positive integer with a specific number of digits.
     *
     * @param digits the number of digits in the integer to read.
     * @return integer read, or -1 if value is not a valid integer.
     */
    int readInt(int digits) {
        int val = 0;
        String sVal;

        if (index < sections[currentSection].length()) {
            sVal = sections[currentSection].substring(index, index + digits);

            if (Util.isNumeric(sVal)) {
                val = Integer.parseInt(sVal);
            } else {
                val = -1;
            }

            index += digits;
        }

        return val;
    }

    /**
     * Read a single digit positive integer.
     *
     * @return integer read, or -1 if value is not a valid integer.
     */
    int readInt() {
        return readInt(1);
    }

    /**
     * Read a boolean value. If the value from the string is "1", then the result will be True,
     * for any other value the result will be False.
     *
     * @return the boolean of the value read.
     */
    boolean readBool() {
        boolean val = false;

        if (index < sections[currentSection].length()) {
            val = sections[currentSection].substring(index, index + 1).equals("1");
            ++index;
        }

        return (val);
    }

    boolean EOS() {
        return (index >= sections[currentSection].length());
    }

    /**
     * Go to the next section of the saved data.
     */
    void nextSection() {
        currentSection++;
        index = 0;
    }
}
