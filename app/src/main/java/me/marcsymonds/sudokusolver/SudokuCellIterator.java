package me.marcsymonds.sudokusolver;

/**
 * Created by Marc on 29/01/2017.
 */

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Iterator to go through all of the cells in the board.
 */
class SudokuCellIterator implements Iterator<SudokuCell> {
    private int x, y;
    private SudokuBoard board;

    SudokuCellIterator(SudokuBoard board) {
        x = 0;
        y = 0;
        this.board = board;
    }

    @Override
    public boolean hasNext() {
        return (y < 9);
    }

    @Override
    public SudokuCell next() {
        if (y >= 9) {
            throw new NoSuchElementException("No more cells in iteration.");
        }

        SudokuCell cell = board.getCell(this.x, this.y);

        ++x;
        if (x >= 9) {
            x = 0;
            ++y;
        }

        return cell;
    }
}