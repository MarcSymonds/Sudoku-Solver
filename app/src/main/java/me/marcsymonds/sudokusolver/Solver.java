package me.marcsymonds.sudokusolver;

import android.os.AsyncTask;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by Marc on 14/12/2016.
 */

class Solver extends AsyncTask<SudokuBoard, SudokuCell, Boolean> {
    final private String TAG = Solver.class.getSimpleName();
    ArrayList<SolveCell> cellsToSolve;
    int cellListCount;
    private SudokuBoard board;

    @Override
    protected Boolean doInBackground(SudokuBoard... boards) {
        SudokuCell cell = null;
        SolveCell solveCell;
        int cellListIdx = 0;
        int number = 0;
        int updCount = 0;
        boolean gotNext;

        board = boards[0];

        // Build a list of cells that need to be calculated. This should make it easier to go
        // through the cells rather than check for fixed numbers every time.

        cellsToSolve = new ArrayList<>();
        cellListCount = 0;
        for (SudokuCell scan : board) {
            if (!scan.isFixed()) {
                cellsToSolve.add(new SolveCell(scan));
                scan.getAvailable();
                ++cellListCount;
            }
        }

        Log.d(TAG, String.format("%d cells to calculate", cellListCount));

        lookForSingles(0);

        while (!this.isCancelled() && cellListIdx >= 0 && cellListIdx < cellListCount) {
            gotNext = false;
            do {
                solveCell = cellsToSolve.get(cellListIdx);
                cell = solveCell.cell;
                if (solveCell.single) {
                    Log.d(TAG, String.format("Skipping single {%d,%d}", cell.x, cell.y));
                    ++cellListIdx;
                } else {
                    if (cell.getCurrentNumber() > 0) {
                        Log.d(TAG, String.format("Skipping cell {%d,%d} with number %d", cell.x, cell.y, cell.getCurrentNumber()));
                        ++cellListIdx;
                    } else {
                        gotNext = true;
                    }
                }
            } while (!gotNext && !this.isCancelled() && cellListIdx < cellListCount);

            if (gotNext) {
                Log.d(TAG, String.format("Looking at {%d,%d}", cell.x, cell.y));

                int unsolvable = anyUnsolvables(cellListIdx);
                if (unsolvable >= 0) {
                    number = -1;
                    cell = cellsToSolve.get(unsolvable).cell;
                    Log.d(TAG, String.format("Cell {%d,%d} is not solvable", cell.x, cell.y));
                } else {
                    number = board.getNumberToTryInCell(cell);
                    Log.d(TAG, String.format("cellsToSolve[%d] = {%d,%d}; Number to use=%d (%s)", cellListIdx, cell.x, cell.y, number, board.whatNumbers(cell)));
                }

                if (number < 0) { // No numbers to try for this cell. Means we have to go back and try something else.
                    number = cell.getCurrentNumber();

                    if (number > 0) {
                        board.unuseNumber(cell, number);
                        cell.setUsage(number, CellUsage.AVAILABLE);
                    }
                    cell.resetUsage();
                    cell.setNumber(0, CellState.CALCULATED, false); // Don't draw the cell here because we are not on the UI Thread.

                    unsetSingles(solveCell);

                    //Log.d(TAG, String.format("Unsetting {%d,%d} from %d (%s)", cell.x, cell.y, number, board.whatNumbers(cell)));

                    gotNext = false;
                    --cellListIdx;
                    while (!isCancelled() && !gotNext && cellListIdx >= 0) {
                        solveCell = cellsToSolve.get(cellListIdx);
                        if (solveCell.single) {
                            --cellListIdx;
                        } else {
                            cell = solveCell.cell;
                            number = cell.getCurrentNumber();
                            board.unuseNumber(cell, number);
                            cell.setUsage(number, CellUsage.TRIED);
                            cell.setCalculatedNumber(0, false);
                            unsetSingles(solveCell);

                            gotNext = true;
                        }
                    }
                } else {
                    //Log.d(TAG, String.format("Setting {%d,%d} to %d", cell.x, cell.y, number));

                    cell.setUsage(number, CellUsage.USED); // Mark the number as used.
                    cell.setCalculatedNumber(number, false);
                    board.useNumber(cell, number, false); // Mark the associated cells as used.

                    //this.publishProgress(cell);

                    ++cellListIdx;

                    lookForSingles(cellListIdx);
                }
            }

            ++updCount;
            if (updCount > 0) {
                this.publishProgress(cell);
                updCount = 0;
            }
        }

        return null;
    }

    private void unsetSingles(SolveCell solveCell) {
        int s = solveCell.singlesCount;
        int number;
        while (s > 0) {
            --s;
            SolveCell unSolveCell = cellsToSolve.get(solveCell.singles.get(s));
            number = unSolveCell.cell.getCurrentNumber();
            board.unuseNumber(unSolveCell.cell, number);
            unSolveCell.cell.setUsage(number, CellUsage.AVAILABLE);
            unSolveCell.cell.setCalculatedNumber(0, false);
            unSolveCell.skipped = false;
        }
        solveCell.singles.clear();
        solveCell.singlesCount = 0;
    }

    /**
     * Looks for cells which could only contain one value based on the current state of the board,
     * and assigns those cells the number.
     *
     * @param cellListIdx index of the cell in the cell list to start looking for singles. It
     *                    searches from this position to the end of the cell list.
     */
    private void lookForSingles(int cellListIdx) {
        int singleScan;
        boolean singleRescan;
        int number;
        SudokuCell cell;

        do {
            singleScan = cellListIdx;
            singleRescan = false;

            while (singleScan < cellListCount) {
                SolveCell scanCell = cellsToSolve.get(singleScan);
                if (!scanCell.single) {
                    cell = scanCell.cell;
                    number = board.findSingleNumber(cell);
                    if (number > 0) {
                        scanCell.singles.add(singleScan);
                        scanCell.singlesCount++;
                        scanCell.single = true;

                        cell.setUsage(number, CellUsage.USED);
                        cell.setSingleNumber(number, false);
                        board.useNumber(cell, number, false);

                        singleRescan = true;
                    }
                }
                ++singleScan;
            }

            // If a single was found, then go round again in case we created more singles.
        } while (singleRescan);
    }

    /**
     * @param cellListIdx
     * @return
     */
    private int anyUnsolvables(int cellListIdx) {
        int unsolvable = -1;
        SolveCell solveCell;
        SudokuCell cell;

        while (cellListIdx < cellListCount) {
            solveCell = cellsToSolve.get(cellListIdx);
            cell = solveCell.cell;
            if (cell.getCurrentNumber() == 0 && !board.isCellSolvable(cell)) {
                unsolvable = cellListIdx;
                break;
            } else {
                ++cellListIdx;
            }
        }

        return unsolvable;
    }

    @Override
    protected void onPostExecute(Boolean aBoolean) {
        super.onPostExecute(aBoolean);

        board.redrawBoard();
    }

    @Override
    protected void onProgressUpdate(SudokuCell... values) {
        super.onProgressUpdate(values);

        //values[0].drawCell();
        board.redrawBoard();
    }

    @Override
    protected void onCancelled(Boolean aBoolean) {
        super.onCancelled(aBoolean);
    }

    private class SolveCell {
        SudokuCell cell;
        ArrayList<Integer> singles = new ArrayList<>();
        int singlesCount;
        boolean skipped;
        boolean single;

        SolveCell(SudokuCell cell) {
            this.cell = cell;
            singles.clear();
            singlesCount = 0;
            skipped = false;
            single = false;
        }
    }
}
