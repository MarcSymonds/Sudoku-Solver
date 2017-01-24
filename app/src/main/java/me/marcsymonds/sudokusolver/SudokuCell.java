package me.marcsymonds.sudokusolver;

import android.support.v4.content.ContextCompat;
import android.widget.TextView;

/**
 * Created by Marc on 09/12/2016.
 */

enum CellState {
    CALCULATED,
    HARD_FIXED,
    SOFT_FIXED,
    SINGLE;

    static CellState fromInteger(int val) {
        switch (val) {
            case 0:
                return CALCULATED;
            case 1:
                return HARD_FIXED;
            case 2:
                return SOFT_FIXED;
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
            case SOFT_FIXED:
                return 2;
            default:
                return 3;
        }
    }
}

enum CellUsage {
    FIXED,
    TRIED,
    AVAILABLE,
    USED;

    static CellUsage fromInteger(int val) {
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

    final private String TAG = SudokuBoard.class.getSimpleName();
    int x;
    int y;
    private SudokuBoard board;
    private TextView mTextView;
    private int mCurrentNumber = 0;
    private CellUsage mUsage[] = new CellUsage[9];
    private int mAvailable;
    private CellState mState = CellState.CALCULATED;
    private boolean mIsError = false;
    private boolean mIsSelected = false;
    private boolean mIsDirty = true;
    private CellBlock mBlock = null;

    /**
     * Constructor. This constructor should not be used.
     *
     * @param board reference to the parent SudokuBoard.
     */
    private SudokuCell(SudokuBoard board) {
        //super(board.context);
        this.board = board;
    }

    /**
     * Constructor used by SudokuBoard.
     *
     * @param board reference to the parent SudokuBoard.
     * @param x     x position of this cell on the board (0-8).
     * @param y     y position of this cell on the board (0-8).
     */
    public SudokuCell(SudokuBoard board, TextView textView, int x, int y) {
        //super(board.context);
        this.board = board;
        this.mTextView = textView;

        this.x = x;
        this.y = y;

        x = (x >= 6) ? 2 : ((x >= 3) ? 1 : 0);
        y = (y >= 6) ? 2 : ((y >= 3) ? 1 : 0);
        mBlock = cellBlocks[y][x];

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
        String data;

        data = String.format("%d%d%d%d%d", mCurrentNumber, mIsSelected ? 1 : 0, mState.toInteger(), mIsError ? 1 : 0, mAvailable);
        for (int i = 0; i < 9; i++) {
            data = data + String.valueOf(mUsage[i].toInteger());
        }

        return data;
    }

    /**
     * Restores this cell to a saved state.
     *
     * @param data string containing the data to restore the cell to a saved state.
     */
    void restoreSavedData(String data) {
        mCurrentNumber = Integer.parseInt(data.substring(0, 1));
        mIsSelected = false;
        mState = CellState.fromInteger(Integer.parseInt(data.substring(2, 3)));
        mIsError = !data.substring(3, 4).equals("0");
        mAvailable = Integer.parseInt(data.substring(4, 5));

        for (int i = 0, j = 5; i < 9; i++, j++) {
            mUsage[i] = CellUsage.fromInteger(Integer.parseInt(data.substring(j, j + 1)));
        }

        mIsDirty = true;
    }

    void setNumber(int newNumber, CellState state, boolean draw) {
        if (mCurrentNumber != newNumber || state != mState) {
            if (newNumber < 1 || newNumber > 9) {
                mCurrentNumber = 0;
            } else {
                mCurrentNumber = newNumber;
            }

            mState = state;
            mIsDirty = true;

            if (draw) {
                drawCell();
            }
        }
    }

    void setCalculatedNumber(int newNumber, boolean draw) {
        setNumber(newNumber, CellState.CALCULATED, draw);
    }

    void setSingleNumber(int newNumber, boolean draw) {
        setNumber(newNumber, CellState.SINGLE, draw);
    }

    int getCurrentNumber() {
        return mCurrentNumber;
    }


    void setUsage(int number, CellUsage usage) {
        --number;
        if (mUsage[number] != usage) {
            if (usage == CellUsage.AVAILABLE) {
                ++mAvailable;
            } else if (mUsage[number] == CellUsage.AVAILABLE) {
                --mAvailable;
            }

            mUsage[number] = usage;
        }
    }

    CellUsage getUsage(int number) {
        return mUsage[number - 1];
    }

    int getAvailableNumber() {
        for (int n = 0; n < 9; n++) {
            if (mUsage[n] == CellUsage.AVAILABLE) {
                return (n + 1);
            }
        }

        return -1;
    }

    int getAvailable() {
        return mAvailable;
    }

    void resetUsage() {
        int n;

        mAvailable = 0;

        for (n = 0; n < 9; n++) {
            if (mUsage[n] == CellUsage.USED || mUsage[n] == CellUsage.TRIED) {
                mUsage[n] = CellUsage.AVAILABLE;
            }

            if (mUsage[n] == CellUsage.AVAILABLE) {
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

    CellBlock cellBlock() {
        return mBlock;
    }

    boolean isFixed() {
        return (mState == CellState.HARD_FIXED || mState == CellState.SOFT_FIXED);
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

        if (!board.isUpdating() && mIsDirty) {

            if (mIsSelected) {
                bg = ContextCompat.getColor(board.context, R.color.cellEdit);
            } else if (mIsError) {
                bg = ContextCompat.getColor(board.context, R.color.cellError);
            } else if (mState == CellState.SINGLE) {
                bg = ContextCompat.getColor(board.context, R.color.cellSingle);
            } else {
                bg = ContextCompat.getColor(board.context, R.color.cellNormal);
            }

            if (mIsError) {
                fg = ContextCompat.getColor(board.context, R.color.numberError);
            } else if (mState == CellState.CALCULATED) {
                fg = ContextCompat.getColor(board.context, R.color.numberCalculate);
            } else if (mState == CellState.SOFT_FIXED) {
                fg = ContextCompat.getColor(board.context, R.color.numberSoftFix);
            } else {
                fg = ContextCompat.getColor(board.context, R.color.numberNormal);
            }

            mTextView.setBackgroundColor(bg);
            mTextView.setTextColor(fg);

            if (mCurrentNumber < 1) {
                mTextView.setText(".");
            } else {
                mTextView.setText(String.valueOf(mCurrentNumber));
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
        mState = CellState.CALCULATED;
        mIsError = false;
        mIsSelected = false;
        mCurrentNumber = 0;

        for (int i = 0; i < 9; i++) {
            mUsage[i] = CellUsage.AVAILABLE;
        }
        mAvailable = 9;

        mIsDirty = true;
    }

    void resetPossibleNumbers() {
        mAvailable = 0;
        for (int i = 0; i < 9; i++) {
            if (mUsage[i] != CellUsage.FIXED) {
                mUsage[i] = CellUsage.AVAILABLE;
                ++mAvailable;
            }
        }
    }

/*    String whatNumbers() {
        String s = "";

        for (int i = 0; i < 9; i++) {
            if (i > 0) s = s + ", ";

            s = s + String.valueOf(i + 1);
            switch(mUsage[i]) {
                case AVAILABLE:
                    s = s + "A";
                    break;
                case USED:
                    s = s + "U";
                    break;
                case FIXED:
                    s = s + "F";
                    break;
                case TRIED:
                    s = s + "T";
                    break;
            }
        }

        return s;
    }*/
}
