package me.marcsymonds.sudokusolver;

import android.os.AsyncTask;
import android.util.Log;

import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Attempts to solve the Sudoku mBoard.
 * <p>
 * Uses AsyncTask to run the solver on a separate thread to the main UI, allowing the main UI
 * to remain responsive.
 */
class Solver extends AsyncTask<SudokuBoard, SudokuCell, Boolean> {
    final private String TAG = Solver.class.getSimpleName();
    final private ReentrantLock mCompletionLock = new ReentrantLock();
    private ArrayList<SolveCell> mCellsToSolve = new ArrayList<>();
    private int mCellsToSolveCount = 0;
    private int mSolveCellIdx = 0;
    private SudokuBoard mBoard = null;
    private SolverState mRunState = SolverState.NOT_RUNNING;
    private ISolverControl mController;
    private boolean mPaused = false;
    private boolean mRestored = false;
    private boolean mCompleted = false;

    /**
     * Constructor.
     *
     * @param controller the object (MainActivity) that instantiated this object. When the
     *                   background process completes, either the "solveCompleted" or
     *                   "solverCancelled" methods will be called on the mController.
     */
    Solver(ISolverControl controller, SudokuBoard board) {
        mController = controller;
        mBoard = board;
    }

    /**
     * Constructor for use when we want to restore the current state of the solver.
     *
     * @param controller the object (MainActivity) that instantiated this object.
     * @param dataReader SavedDataReader object to restore the state of the solver from.
     */
    Solver(ISolverControl controller, SudokuBoard board, SavedDataReader dataReader) {
        mController = controller;
        mBoard = board;

        restoreSavedData(dataReader);
        mRestored = true;
    }

    @Override
    protected Boolean doInBackground(SudokuBoard... boards) {
        SudokuCell cell = null;
        SolveCell solveCell;
        int number = 0;
        int updCount = 0;
        boolean gotNext;

        mCompletionLock.lock();

        mRunState = SolverState.RUNNING;

        //mBoard = boards[0];

        // Build a list of cells that need to be calculated. This should make it easier to go
        // through the cells rather than check for fixed numbers every time.

        if (!mRestored) {
            for (SudokuCell scan : mBoard) {
                if (!scan.isFixed()) {
                    mCellsToSolve.add(new SolveCell(scan));
                    scan.getAvailable();
                }
            }

            mCellsToSolveCount = mCellsToSolve.size();
            mSolveCellIdx = 0;

            //Log.d(TAG, String.format("%d cells to calculate", mCellsToSolveCount));

            // Before we start solving, look for any cells that can only contain one number and set
            // them.
            lookForSingles(-1);
        }

        // Try and solve the board.
        while (mRunState == SolverState.RUNNING) {
            // Look for the next cell to solve; skip over cells that already have numbers.
            gotNext = false;
            do {
                solveCell = mCellsToSolve.get(mSolveCellIdx);
                cell = solveCell.cell;
                if (solveCell.single) {
                    //Log.d(TAG, String.format("Skipping single [%d,%d]", cell.x, cell.y));
                    ++mSolveCellIdx;
                } else if (cell.getCurrentNumber() > 0) {
                    //Log.d(TAG, String.format("Skipping cell [%d,%d] with number %d", cell.x, cell.y, cell.getCurrentNumber()));
                    ++mSolveCellIdx;
                } else {
                    gotNext = true;
                }
            } while (!gotNext && !this.isCancelled() && mSolveCellIdx < mCellsToSolveCount);

            if (this.isCancelled()) {
                // Thread has been stopped or cancelled.
                if (mPaused) {
                    mRunState = SolverState.PAUSED;
                } else {
                    mRunState = SolverState.CANCELLED;
                    mCompleted = true;
                }
            } else if (mSolveCellIdx >= mCellsToSolveCount) {
                // If gone through all the cells, then must have solved it.
                mRunState = SolverState.FINISHED_SUCCESS;
                mCompleted = true;
            } else {
                //Log.d(TAG, String.format("Looking at {%d,%d}", cell.x, cell.y));

                int unsolvable = anyUnsolvables(mSolveCellIdx);
                if (unsolvable >= 0) {
                    number = -1;
                    cell = mCellsToSolve.get(unsolvable).cell;
                    //Log.d(TAG, String.format("Cell {%d,%d} is not solvable", cell.x, cell.y));
                } else {
                    number = mBoard.getNumberToTryInCell(cell);
                    //Log.d(TAG, String.format("mCellsToSolve[%d] = {%d,%d}; Number to use=%d (%s)", mSolveCellIdx, cell.x, cell.y, number, mBoard.whatNumbers(cell)));
                }

                if (number < 0) {
                    // No numbers to try for this cell. Means we have to go back and try something else.

                    number = cell.getCurrentNumber();

                    if (number > 0) {
                        mBoard.unuseNumber(cell, number);
                        cell.setNumberUsage(number, CellNumberUsage.AVAILABLE);
                    }
                    cell.resetUsage();
                    cell.unsetNumber(false); // Don't draw the cell here because we are not on the UI Thread.

                    unsetSingles(solveCell);

                    //Log.d(TAG, String.format("Unsetting {%d,%d} from %d (%s)", cell.x, cell.y, number, mBoard.whatNumbers(cell)));

                    gotNext = false;
                    --mSolveCellIdx;
                    while (!isCancelled() && !gotNext && mSolveCellIdx >= 0) {
                        solveCell = mCellsToSolve.get(mSolveCellIdx);
                        if (solveCell.single) {
                            --mSolveCellIdx;
                        } else {
                            cell = solveCell.cell;
                            number = cell.getCurrentNumber();
                            mBoard.unuseNumber(cell, number);
                            cell.setNumberUsage(number, CellNumberUsage.TRIED);
                            cell.unsetNumber(false);
                            unsetSingles(solveCell);

                            gotNext = true;
                        }
                    }

                    if (mSolveCellIdx < 0) {
                        mRunState = SolverState.FINISHED_FAILED;
                        mCompleted = true;
                    }
                } else {
                    cell.setNumberUsage(number, CellNumberUsage.USED); // Mark the number as used.
                    cell.setCalculatedNumber(number, false);
                    mBoard.useNumber(cell, number, false); // Mark the associated cells as used.

                    //Log.d(TAG, String.format("Trying %d in [%d,%d]", number, cell.x, cell.y));

                    lookForSingles(mSolveCellIdx);

                    ++mSolveCellIdx;
                }
            }

            ++updCount;
            if (updCount > 499) {
                this.publishProgress(cell);
                updCount = 0;
            }
        }

        Log.d(TAG, "Unlocking");
        mCompletionLock.unlock();

        return null;
    }

    @Override
    protected void onProgressUpdate(SudokuCell... values) {
        super.onProgressUpdate(values);

        //values[0].drawCell();
        mBoard.redrawBoard();
    }

    @Override
    protected void onPostExecute(Boolean aBoolean) {
        super.onPostExecute(aBoolean);

        if (mController != null) {
            mController.solverCompleted(this, mRunState);
        }
    }

    @Override
    protected void onCancelled(Boolean aBoolean) {
        super.onCancelled(aBoolean);

        Log.d(TAG, String.format("onCancelled: state=%s", mRunState.toString()));
        /*if (mController != null) {
            if (mPaused) {
                mController.solverStopped();
            }
            else {
                mController.solverCancelled();
            }
        }*/
    }

    boolean pause() {
        if (mRunState == SolverState.RUNNING) {
            mPaused = true;
            return this.cancel(false);
        } else {
            return false;
        }
    }

    boolean isPaused() {
        return mPaused;
    }

    boolean isCompleted() {
        return mCompleted;
    }

    boolean waitForCompletion() {
        Log.d(TAG, "waiting for completion");
        mCompletionLock.lock();
        mCompletionLock.unlock();
        Log.d(TAG, "task has completed");

        return true;
    }

    SolverState getRunState() {
        return mRunState;
    }

    private void unsetSingles(SolveCell solveCell) {
        int s = solveCell.singlesCount;
        int number;

        //Log.d(TAG, String.format("Unsetting %d singles for {%d,%d}", s, solveCell.cell.x, solveCell.cell.y));

        while (s > 0) {
            --s;
            SolveCell unSolveCell = mCellsToSolve.get(solveCell.singles.get(s));
            number = unSolveCell.cell.getCurrentNumber();
            //Log.d(TAG, String.format("Removing single %d from [%d,%d]", number, unSolveCell.cell.x, unSolveCell.cell.y));
            mBoard.unuseNumber(unSolveCell.cell, number);
            unSolveCell.cell.setNumberUsage(number, CellNumberUsage.AVAILABLE);
            unSolveCell.cell.unsetNumber(false);
            //unSolveCell.skipped = false;
            unSolveCell.single = false;
        }
        solveCell.singles.clear();
        solveCell.singlesCount = 0;
    }

    /**
     * Looks for cells which could only contain one value based on the current mRunState of the mBoard,
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
        SolveCell source;

        if (cellListIdx >= 0) {
            source = mCellsToSolve.get(cellListIdx);
            ++cellListIdx;
        } else {
            source = null;
            cellListIdx = 0;
        }

        do {
            singleScan = cellListIdx;
            singleRescan = false;

            while (singleScan < mCellsToSolveCount) {
                SolveCell scanCell = mCellsToSolve.get(singleScan);
                if (!scanCell.single) {
                    cell = scanCell.cell;
                    number = mBoard.findSingleNumber(cell);
                    if (number > 0) {
                        if (source != null) {
                            source.singles.add(singleScan);
                            source.singlesCount++;

                            //Log.d(TAG, String.format("Created single for {%d,%d}[%d] at {%d,%d} with %d", source.cell.x, source.cell.y, source.singlesCount, cell.x, cell.y, number));
                        }
                        //else
                        //Log.d(TAG, String.format("Created single {%d,%d} with %d", cell.x, cell.y, number));

                        scanCell.single = true;

                        cell.setNumberUsage(number, CellNumberUsage.USED);
                        cell.setSingleNumber(number, false);
                        mBoard.useNumber(cell, number, false);

                        singleRescan = true;
                    }
                }
                ++singleScan;
            }

            // If a single was found, then go round again in case we created more singles.
        } while (singleRescan);
    }

    /**
     * Looks for any cells that cannot be solved; i.e. all of the numbers are used elsewhere
     * within the same row, column or block.
     *
     * @param cellListIdx index of the first cell to start looking for unsolvables.
     * @return index of the first unsolvable cell found.
     */
    private int anyUnsolvables(int cellListIdx) {
        int unsolvable = -1;
        SolveCell solveCell;
        SudokuCell cell;

        while (cellListIdx < mCellsToSolveCount) {
            solveCell = mCellsToSolve.get(cellListIdx);
            cell = solveCell.cell;
            if (cell.getCurrentNumber() == 0 && !mBoard.isCellSolvable(cell)) {
                unsolvable = cellListIdx;
                break;
            } else {
                ++cellListIdx;
            }
        }

        return unsolvable;
    }

    ArrayList<SudokuCell> getSolvedCellList() {
        ArrayList<SudokuCell> list = new ArrayList<>();

        for (int i = 0; i < mCellsToSolveCount; i++) {
            list.add(mCellsToSolve.get(i).cell);
        }

        return list;
    }

    String getSaveData() {
        StringBuilder data = new StringBuilder();

        data.append(String.format("%02d%02d%d%d%d", mCellsToSolveCount, mSolveCellIdx, mRunState.toInteger(), (mPaused ? 1 : 0), (mCompleted ? 1 : 0)));

        for (int i = 0; i < mCellsToSolveCount; i++) {
            mCellsToSolve.get(i).getSaveData(data);
        }
        data.append("#");

        return data.toString();
    }

    private void restoreSavedData(SavedDataReader dataReader) {
        mCellsToSolveCount = dataReader.readInt(2);
        mSolveCellIdx = dataReader.readInt(2);
        mRunState = SolverState.fromInteger(dataReader.readInt());
        mPaused = dataReader.readBool();
        mCompleted = dataReader.readBool();

        Log.d(TAG, String.format("Restore: Cells=%d, Idx=%d, State=%s", mCellsToSolveCount, mSolveCellIdx, mRunState.toString()));

        mCellsToSolve.clear();
        for (int i = 0; i < mCellsToSolveCount; i++) {
            mCellsToSolve.add(new SolveCell(dataReader));
        }
    }

    /**
     *
     */
    private class SolveCell {
        SudokuCell cell;
        ArrayList<Integer> singles = new ArrayList<>();
        int singlesCount;
        boolean single;

        SolveCell(SudokuCell cell) {
            this.cell = cell;
            singles.clear();
            singlesCount = 0;
            single = false;
        }

        SolveCell(SavedDataReader dataReader) {
            restoreSavedData(dataReader);
        }

        void getSaveData(StringBuilder data) {
            data.append(cell.x);
            data.append(cell.y);
            data.append(String.format("%02d", singlesCount));
            for (int i = 0; i < singlesCount; i++) {
                data.append(String.format("%02d", singles.get(i)));
            }
            data.append(single ? 1 : 0);
        }

        void restoreSavedData(SavedDataReader dataReader) {
            int x, y;

            x = dataReader.readInt();
            y = dataReader.readInt();
            this.cell = mBoard.getCell(x, y);

            singlesCount = dataReader.readInt(2);
            for (x = 0; x < singlesCount; x++) {
                singles.add(dataReader.readInt(2));
            }
            single = dataReader.readBool();
        }
    }
}
