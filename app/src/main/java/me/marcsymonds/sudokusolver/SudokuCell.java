package me.marcsymonds.sudokusolver;

import android.graphics.Color;
import android.support.v4.content.ContextCompat;
import android.widget.TextView;

class SudokuCell {
    private static CellBlock cellBlocks[][] = new CellBlock[3][3];

    static {
        int x, y;

        for (x = 0; x < 3; x++) {
            for (y = 0; y < 3; y++) {
                cellBlocks[y][x] = new CellBlock(x, y);
            }
        }
    }

    final private String TAG = SudokuCell.class.getSimpleName();

    // Public for easier access.
    int x;
    int y;
    CellBlock cellBlock = null;

    private SudokuBoard mBoard;
    private TextView mTextView;
    private int mCurrentNumber = 0;
    private CellNumberUsage mUsage[] = new CellNumberUsage[9];
    private int mAvailable;
    private CellState mState = CellState.NOT_SET;
    private boolean mIsError = false;
    private boolean mIsSelected = false;
    private boolean mIsDirty = true;
    private boolean mVisible = false;

    /**
     * Constructor. This constructor should not be used.
     *
     * @param board reference to the parent SudokuBoard.
     */
    private SudokuCell(SudokuBoard board) {
        //super(mBoard.context);
        this.mBoard = board;
    }

    /**
     * Constructor used by SudokuBoard.
     *
     * @param board reference to the parent SudokuBoard.
     * @param x     x position of this cell on the mBoard (0-8).
     * @param y     y position of this cell on the mBoard (0-8).
     */
    public SudokuCell(SudokuBoard board, TextView textView, int x, int y) {
        //super(mBoard.context);
        this.mBoard = board;
        this.mTextView = textView;

        this.x = x;
        this.y = y;

        x = (x >= 6) ? 2 : ((x >= 3) ? 1 : 0);
        y = (y >= 6) ? 2 : ((y >= 3) ? 1 : 0);
        cellBlock = cellBlocks[y][x];

        clear();
    }

    public TextView getTextView() {
        return mTextView;
    }

    /**
     * Returns a string containing information representing the current state of this cell.
     *
     * @return string containing data to save.
     */
    String getSaveData() {
        StringBuilder data = new StringBuilder(14);

        data.append(mCurrentNumber);
        data.append(mIsSelected ? 1 : 0); // We don't use this during restore.
        data.append(mState.toInteger());
        data.append(mIsError ? 1 : 0);
        data.append(mVisible ? 1 : 0);
        data.append(mAvailable);

        for (int i = 0; i < 9; i++) {
            data.append(mUsage[i].toInteger());
        }

        return data.toString();
    }

    /**
     * Restores this cell to a saved state.
     *
     * @param savedDataReader object used for reading the saved data.
     */
    void restoreSavedData(SavedDataReader savedDataReader) {
        mCurrentNumber = savedDataReader.readInt();
        savedDataReader.readInt();
        mIsSelected = false;
        mState = CellState.fromInteger(savedDataReader.readInt());
        mIsError = savedDataReader.readBool();
        mVisible = savedDataReader.readBool();
        mAvailable = savedDataReader.readInt();

        for (int i = 0, j = 5; i < 9; i++, j++) {
            mUsage[i] = CellNumberUsage.fromInteger(savedDataReader.readInt());
        }

        mIsDirty = true;
    }

    void hide() {
        setVisible(false);
    }

    void show() {
        setVisible(true);
    }

    void setVisible(boolean state) {
        if (mVisible != state) {
            mVisible = state;
            mIsDirty = true;

            drawCell();
        }
    }

    void setNumber(int newNumber, CellState state, boolean draw, boolean visible) {
        if (mCurrentNumber != newNumber || state != mState) {
            if (state == CellState.NOT_SET || newNumber < 1 || newNumber > 9) {
                mCurrentNumber = 0;
            } else {
                mCurrentNumber = newNumber;
            }

            mState = state;
            mVisible = visible;
            mIsDirty = true;

            if (draw) {
                drawCell();
            }
        }
    }

    void setCalculatedNumber(int newNumber, boolean draw) {
        setNumber(newNumber, CellState.CALCULATED, draw, false);
    }

    void setSingleNumber(int newNumber, boolean draw) {
        setNumber(newNumber, CellState.SINGLE, draw, false);
    }

    void unsetNumber(boolean draw) {
        setNumber(0, CellState.NOT_SET, draw, false);
    }

    int getCurrentNumber() {
        return mCurrentNumber;
    }

    void setNumberUsage(int number, CellNumberUsage usage) {
        --number;
        if (mUsage[number] != usage) {
            if (usage == CellNumberUsage.AVAILABLE) {
                ++mAvailable;
            } else if (mUsage[number] == CellNumberUsage.AVAILABLE) {
                --mAvailable;
            }

            mUsage[number] = usage;
        }
    }

    CellNumberUsage getUsage(int number) {
        return mUsage[number - 1];
    }

    int getAvailable() {
        return mAvailable;
    }

    void resetUsage() {
        int n;

        mAvailable = 0;

        for (n = 0; n < 9; n++) {
            if (mUsage[n] == CellNumberUsage.USED || mUsage[n] == CellNumberUsage.TRIED) {
                mUsage[n] = CellNumberUsage.AVAILABLE;
            }

            if (mUsage[n] == CellNumberUsage.AVAILABLE) {
                ++mAvailable;
            }
        }
    }

    void setSelected(boolean newState) {
        if (mIsSelected != newState) {
            mIsSelected = newState;
            mIsDirty = true;
            drawCell();
        }
    }

    void setError(boolean newState) {
        if (mIsError != newState) {
            mIsError = newState;
            mIsDirty = true;
            drawCell();
        }
    }

    boolean isFixed() {
        return (mState == CellState.HARD_FIXED);
    }

    boolean isHardFixed() {
        return (mState == CellState.HARD_FIXED);
    }

    /**
     * Redraws the cell using based on its current state.
     * <p>
     * The cell is only redrawn if the parent SudokuBoard.isUpdating() method returns false.
     */
    void drawCell() {
        int bg, fg;

        if (!mBoard.isUpdating() && mIsDirty) {
            if (mVisible || mIsSelected || mIsError) {
                if (mIsSelected) {
                    bg = ContextCompat.getColor(mBoard.context, R.color.cellEdit);
                } else if (mIsError) {
                    bg = ContextCompat.getColor(mBoard.context, R.color.cellError);
                } else if (mState == CellState.SINGLE || mState == CellState.CALCULATED) {
                    bg = ContextCompat.getColor(mBoard.context, R.color.cellCalculated);
                } else {
                    bg = ContextCompat.getColor(mBoard.context, R.color.cellNormal);
                }

                if (mIsError) {
                    fg = ContextCompat.getColor(mBoard.context, R.color.numberError);
                } else if (mState == CellState.SINGLE || mState == CellState.CALCULATED) {
                    fg = ContextCompat.getColor(mBoard.context, R.color.numberCalculated);
                } else {
                    fg = ContextCompat.getColor(mBoard.context, R.color.numberNormal);
                }
            } else {
                bg = ContextCompat.getColor(mBoard.context, R.color.cellInvisible);
                fg = Color.BLACK;
            }

            mTextView.setBackgroundColor(bg);
            mTextView.setTextColor(fg);

            if ((mVisible || mIsSelected || mIsError) && mCurrentNumber > 0) {
                mTextView.setText(String.valueOf(mCurrentNumber));
            } else {
                mTextView.setText(" ");
            }

            //if (mCurrentNumber > 0) {
            //Log.d(TAG, String.format("[%d][%d] Selected: %d, Error: %d, Fixed: %d", x, y, mIsSelected?1:0, mIsError?1:0, mIsFixed?1:0));
            //}

            mIsDirty = false;
        }
    }

    /**
     * Resets the cell to a blank state.
     */
    void clear() {
        mState = CellState.NOT_SET;
        mIsError = false;
        mIsSelected = false;
        mVisible = false;
        mCurrentNumber = 0;

        for (int i = 0; i < 9; i++) {
            mUsage[i] = CellNumberUsage.AVAILABLE;
        }
        mAvailable = 9;

        mIsDirty = true;
    }

    void resetPossibleNumbers() {
        mAvailable = 0;
        for (int i = 0; i < 9; i++) {
            if (mUsage[i] != CellNumberUsage.FIXED) {
                mUsage[i] = CellNumberUsage.AVAILABLE;
                ++mAvailable;
            }
        }
    }
}
