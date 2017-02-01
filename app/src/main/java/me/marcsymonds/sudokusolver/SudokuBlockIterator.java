package me.marcsymonds.sudokusolver;

import java.util.Iterator;

/**
 * Iterator to go through the cells in a 3x3 block. The block is determined from the cell
 * specified.
 * <p>
 * for (SudokuBlockIterator it = new SudokuBlockIterator(board, cell); it.hasNext(); ) {
 * ...
 * }
 */
class SudokuBlockIterator implements Iterator<SudokuCell> {
    private SudokuBoard board;
    private CellBlock block;
    private int x, y;

    /**
     * Class constructor.
     *
     * @param cell a cell within the block to be iterated over.
     */
    SudokuBlockIterator(SudokuBoard board, SudokuCell cell) {
        this.board = board;
        block = cell.cellBlock;

        x = block.xl;
        y = block.yt;
    }

    @Override
    public boolean hasNext() {
        return (y <= block.yb);
    }

    @Override
    public SudokuCell next() {
        SudokuCell cell = board.getCell(x, y);
        ++x;
        if (x > block.xr) {
            x = block.xl;
            ++y;
        }
        return cell;
    }
}

