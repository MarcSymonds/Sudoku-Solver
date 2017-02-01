package me.marcsymonds.sudokusolver;

/**
 * Indicates how a number has been used in a cell.
 */
enum CellNumberUsage {
    /**
     * The cell has been set by the user manually editing the board
     */
    FIXED,
    /**
     * The number has been tried in the cell
     */
    TRIED,
    /**
     * The number is available to try in the cell
     */
    AVAILABLE,
    /**
     * The number is currently used in the cell
     */
    USED;

    /**
     * Convert an integer in to a CellNumberUsage enum.
     *
     * @param val value to convert.
     * @return CellNumberUsage enum value.
     */
    static CellNumberUsage fromInteger(int val) {
        switch (val) {
            case 0:
                return FIXED;
            case 1:
                return TRIED;
            case 2:
                return AVAILABLE;
            default:
                return USED;
        }
    }

    /**
     * Convert the enum value to an integer.
     *
     * @return integer representing the enum value.
     */
    int toInteger() {
        switch (this) {
            case FIXED:
                return 0;
            case TRIED:
                return 1;
            case AVAILABLE:
                return 2;
            default:
                return 3;
        }
    }
}
