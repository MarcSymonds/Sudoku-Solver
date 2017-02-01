package me.marcsymonds.sudokusolver;

/**
 * Identifies which 3x3 block a particular cell is located, by calculating the x and y positions
 * of the block.
 */
class CellBlock {
    int x, y;
    int xl, xr; // x-left, x-right.
    int yt, yb; // y-top, y-bottom.

    CellBlock(int bx, int by) {
        x = bx;
        y = by;

        xl = x * 3;
        xr = xl + 2;

        yt = y * 3;
        yb = yt + 2;
    }
}
