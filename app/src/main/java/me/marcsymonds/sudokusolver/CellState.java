package me.marcsymonds.sudokusolver;

enum CellState {
    NOT_SET,
    CALCULATED,
    HARD_FIXED,
    SINGLE;

    static CellState fromInteger(int val) {

        switch (val) {
            case 0:
                return CALCULATED;
            case 1:
                return HARD_FIXED;
            case 2:
                return NOT_SET;
            default:
                return SINGLE;
        }
    }

    int toInteger() {
        switch (this) {
            case CALCULATED:
                return 0;
            case HARD_FIXED:
                return 1;
            case NOT_SET:
                return 2;
            default:
                return 3;
        }
    }
}
