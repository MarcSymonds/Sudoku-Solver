package me.marcsymonds.sudokusolver;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Created by Marc on 09/12/2016.
 */

class SudokuBoard implements Iterable<SudokuCell>, TextView.OnClickListener {
    final private String TAG = SudokuBoard.class.getSimpleName();

    Context context;
    private float displayDensity;

    private TableLayout mBoardTable = null;
    private SudokuCell[][] mCells = new SudokuCell[9][9];

    private CellUsage[][][] mBlockUsage = new CellUsage[3][3][9];
    private CellUsage[][] mHorizontalUsage = new CellUsage[9][9];
    private CellUsage[][] mVerticalUsage = new CellUsage[9][9];
    private int mHorizontalAvailable[] = new int[9];
    private int mVerticalAvailable[] = new int[9];
    private int mBlockAvailable[][] = new int[3][3];

    private SudokuCell mSelectedCell = null;
    private boolean mEditing = false;
    private boolean mHasErrors = false;

    private int mPreventUpdate = 0;

    SudokuBoard(Context c) {
        Configuration config;
        int x, y, n;
        DisplayMetrics dm = new DisplayMetrics();
        float maxSize;
        int numSize, boardSize;

        context = c;
        config = context.getResources().getConfiguration();
        ((Activity) context).getWindowManager().getDefaultDisplay().getMetrics(dm); //context.getResources().getDisplayMetrics();
        displayDensity = dm.density;

        // 80% of the smallest dimension - width or height.
        maxSize = ((config.screenWidthDp < config.screenHeightDp) ? config.screenWidthDp : config.screenHeightDp) * 0.8f;

        // Calculate size of each number cell.
        numSize = (int) ((maxSize - (8.0f + 6.0f + 4.0f)) / 9.0f); // 8=4 for each border, 6=1 for each thin divider between cells, 4=2 for each thick divider between cells.

        // Calculate size of entire board table.
        boardSize = 8 + 6 + 4 + (9 * numSize);

        mBoardTable = (TableLayout) ((Activity) context).findViewById(R.id.SudokuBoardTable);
        LinearLayout.LayoutParams boardLayout = new LinearLayout.LayoutParams(dp2px(boardSize), dp2px(boardSize));
        mBoardTable.setLayoutParams(boardLayout);

        for (x = 0; x < 9; x++) {
            for (y = 0; y < 9; y++) {
                mCells[x][y] = new SudokuCell(this, new TextView(context), x, y);
            }
        }

        clearBoard();
        resetUsage();
        BuildBoardTableView(numSize);
    }

    void clearBoard() {
        mHasErrors = false;

        for (SudokuCell cell : this) {
            cell.clear();
        }
    }

    void resetUsage() {
        int x, y, n;

        for (x = 0; x < 9; x++) {
            for (n = 0; n < 9; n++) {
                mVerticalUsage[x][n] = CellUsage.AVAILABLE;
                mHorizontalUsage[x][n] = CellUsage.AVAILABLE;
            }
            mVerticalAvailable[x] = 9;
            mHorizontalAvailable[x] = 9;
        }

        for (x = 0; x < 3; x++) {
            for (y = 0; y < 3; y++) {
                for (n = 0; n < 9; n++) {
                    mBlockUsage[y][x][n] = CellUsage.AVAILABLE;
                }
                mBlockAvailable[y][x] = 9;
            }
        }
    }

    private void BuildBoardTableView(int cellSize) {
        int x, y;
        TableRow tr;
        View divider;
        SudokuCell cell;
        TextView txt;

        // Colours to use for drawing the board.,
        int frameColour = ContextCompat.getColor(context, R.color.boardFrame);
        int dividerColour = ContextCompat.getColor(context, R.color.boardDivider);
        int cellColour = ContextCompat.getColor(context, R.color.cellNormal);
        int textColour = ContextCompat.getColor(context, R.color.numberNormal);

        TableRow.LayoutParams txtlp;
        TableRow.LayoutParams vdivlp, vdivlpw, vframe;
        TableRow.LayoutParams hdivlp, hdivlpw, hframe;

        // Layout parameters for the various elements of the board - frame, cell, dividers.
        txtlp = new TableRow.LayoutParams(dp2px(cellSize), dp2px(cellSize)); //TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT);

        vframe = new TableRow.LayoutParams(dp2px(4), TableRow.LayoutParams.MATCH_PARENT);
        vdivlp = new TableRow.LayoutParams(dp2px(1), TableRow.LayoutParams.MATCH_PARENT);
        vdivlpw = new TableRow.LayoutParams(dp2px(2), TableRow.LayoutParams.MATCH_PARENT);

        hframe = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, dp2px(4));
        hdivlp = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, dp2px(1));
        hdivlpw = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, dp2px(2));

        // Generate the board.

        // Top frame border.
        divider = new View(context);
        divider.setBackgroundColor(frameColour);
        mBoardTable.addView(divider, hframe);

        for (y = 0; y < 9; y++) {
            // New row for the table.
            tr = new TableRow(context);
            tr.setGravity(Gravity.FILL_HORIZONTAL);

            // Left edge frame border.
            divider = new View(context);
            divider.setBackgroundColor(frameColour);
            tr.addView(divider, vframe);

            for (x = 0; x < 9; x++) {
                // Cell with the number in it.
                txt = mCells[x][y].getTextView();// TextView(context);
                txt.setText(" ");
                txt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f);
                txt.setTextColor(textColour);
                txt.setBackgroundColor(cellColour);
                txt.setGravity(Gravity.CENTER_HORIZONTAL + Gravity.CENTER_VERTICAL);

                tr.addView(txt, txtlp);

                // Add a vertical divider between the columns.
                if (x < 8) {
                    divider = new View(context);
                    divider.setBackgroundColor(dividerColour);

                    tr.addView(divider, (x == 2 || x == 5) ? vdivlpw : vdivlp); // Thicker divider between the 3x3 groups.
                }
            }

            // Right edge frame border.
            divider = new View(context);
            divider.setBackgroundColor(frameColour);
            tr.addView(divider, vframe);

            // Add the row to the table.
            mBoardTable.addView(tr);

            // Add a horizontal dividing line between the rows.
            if (y < 8) {
                tr = new TableRow(context);
                tr.setGravity(Gravity.FILL_HORIZONTAL);

                divider = new View(context);
                divider.setBackgroundColor(frameColour);
                tr.addView(divider, vframe);

                for (int i = 0; i < (9 + 8); i++) {
                    divider = new View(context);
                    divider.setBackgroundColor(dividerColour);
                    tr.addView(divider, (y == 2 || y == 5) ? hdivlpw : hdivlp);
                }

                divider = new View(context);
                divider.setBackgroundColor(frameColour);
                tr.addView(divider, vframe);

                mBoardTable.addView(tr);
            }
        }

        //Bottom frame border.
        divider = new View(context);
        divider.setBackgroundColor(frameColour);
        mBoardTable.addView(divider, hframe);
    }

    void beginUpdate() {
        ++mPreventUpdate;
        Log.d(TAG, String.format("beginUpdate %d", mPreventUpdate));
    }

    void endUpdate() {
        endUpdate(false);
    }

    void endUpdate(boolean force) {
        Log.d(TAG, String.format("endUpdate %d", mPreventUpdate));
        if (mPreventUpdate > 0 || force) {
            if (force) {
                mPreventUpdate = 0;
            } else {
                --mPreventUpdate;
            }

            if (mPreventUpdate == 0) {
                redrawBoard();
            }
        }
    }

    boolean isEditing() {
        return mEditing;
    }

    boolean isUpdating() {
        return (mPreventUpdate > 0);
    }

    void redrawBoard() {
        for (SudokuCell cell : this) {
            cell.drawCell();
        }
    }

    boolean hasErrors() {
        return mHasErrors;
    }

    /**
     * Validates each cell on the board with a number, to make sure the number matches the Sudoku
     * rules.
     *
     * @return indicates if there are any errors in the board.
     */
    boolean validateBoard() {
        beginUpdate();

        clearErrors();

        for (SudokuCell cell : this) {
            if (cell.isFixed() && cell.getCurrentNumber() > 0) {
                if (!validateCell(cell)) {
                    mHasErrors = true;
                }
            } else if (!isCellSolvable(cell)) {
                cell.setError(true);
                mHasErrors = true;
            }
        }

        endUpdate();

        return !mHasErrors;
    }

    /**
     * Validates the value of a cell against the board. Cells in the same row, the same column, or
     * the same 3x3 block can't have the same value.
     * <p>
     * If the cell is found to be invalid, then error flag for this cell and the matching cells is
     * set.
     *
     * @param cell the cell to validate.
     * @return true if the cell value is valid, false otherwise.
     */
    private boolean validateCell(SudokuCell cell) {
        int x, y;
        int num;
        SudokuCell cmp;
        boolean err = false;

        num = cell.getCurrentNumber();
        if (num > 0) {
            // Check horizontal cells.
            y = cell.y;
            for (x = 0; x < 9; x++) {
                if (x != cell.x) {
                    cmp = mCells[x][y];
                    if (cmp.getCurrentNumber() == num) {
                        cell.setError(true);
                        cmp.setError(true);
                        err = true;
                    }
                }
            }

            // Check vertical cells.
            x = cell.x;
            for (y = 0; y < 9; y++) {
                if (y != cell.y) {
                    cmp = mCells[x][y];
                    if (cmp.getCurrentNumber() == num) {
                        cell.setError(true);
                        cmp.setError(true);
                        err = true;
                    }
                }
            }

            // Check cells in same 3x3 section.
            for (SudokuBlockIterator it = new SudokuBlockIterator(cell); it.hasNext(); ) {
                cmp = it.next();
                if (cmp.x != cell.x || cmp.y != cell.y) {
                    if (cmp.getCurrentNumber() == num) {
                        cell.setError(true);
                        cmp.setError(true);
                        err = true;
                    }
                }
            }
        }

        return !err;
    }

    void clearErrors() {
        mHasErrors = false;

        for (SudokuCell cell : this) {
            cell.setError(false);
        }
    }

    void startEdit() {
        startEdit(false);
    }

    /**
     * Set the board up for editing.
     * Sets the onClick handler for each cell so that it can be selected.
     * Highlights the selected cell, defaulting to 0,0 if none previously selected.
     */
    void startEdit(boolean force) {
        if (!mEditing || force) {
            // Should be called before setSelectedCell, otherwise it won't highlight the cell.
            mEditing = true;

            resetUsage();

            // Set onClick listeners for each cell.
            for (SudokuCell cell : this) {
                cell.getTextView().setOnClickListener(this);

                if (!cell.isHardFixed()) {
                    cell.setCalculatedNumber(0, true);
                } else {
                    useNumber(cell, cell.getCurrentNumber(), true);
                }
            }

            // Select the last selected, or 0,0 if no last selected.
            unsetSelectedCell();

            if (mSelectedCell == null) {
                setSelectedCell(mCells[0][0]);
            } else {
                setSelectedCell(mSelectedCell);
            }
        }
    }

    /**
     * Ends editing mode for the board.
     * Removes the onClick handlers for each cell and unhighlights the selected cell.
     */
    void endEdit() {
        if (mEditing) {
            unsetSelectedCell();

            // Remove onClick listeners for each cell.
            for (SudokuCell cell : this) {
                cell.getTextView().setOnClickListener(null);
            }

            mEditing = false;
        }
    }

    /**
     * Set which is the selected cell. The currently selected cell is first unselected.
     *
     * @param cell the cell to select. If null, the currently selected cell is reselected.
     */
    void setSelectedCell(SudokuCell cell) {
        if (cell == null) {
            cell = mSelectedCell;
        }

        unsetSelectedCell();

        mSelectedCell = cell;
        if (mEditing && mSelectedCell != null) {
            mSelectedCell.setSelected(true);
        }
    }

    void unsetSelectedCell() {
        if (mSelectedCell != null) {
            mSelectedCell.setSelected(false);
            //mSelectedCell = null;
        }
    }

    void setSelectedCellNumber(int number) {
        if (mSelectedCell != null) {
            int prevNum = mSelectedCell.getCurrentNumber();
            if (prevNum > 0) {
                unuseNumber(mSelectedCell, prevNum);
            }

            if (number > 0) {
                mSelectedCell.setNumber(number, CellState.HARD_FIXED, true);
                useNumber(mSelectedCell, number, true);
            } else {
                mSelectedCell.setNumber(0, CellState.CALCULATED, true);
            }

            //mSelectedCell.setNumber(number, (number > 0 ? CellState.HARD_FIXED : CellState.CALCULATED), true);
        }
    }

    /**
     * Return data representing the current state of the board. This can be restored later to set
     * the board back to the saved state.
     *
     * @return String containing the save data for all of the cells and save data for the table.
     */
    String getSaveData() {
        int x, y;
        String data = "";

        // Get the save data for each cell.
        for (SudokuCell cell : this) {
            data = data + cell.getSaveData() + "#";
        }

        // Get the save data for the board.
        data = data + String.format("%d%d%03d",
                mEditing ? 1 : 0,
                mHasErrors ? 1 : 0,
                mPreventUpdate);

        if (mSelectedCell == null) {
            data = data + "xx";
        } else {
            data = data + String.format("%d%d", mSelectedCell.x, mSelectedCell.y);
        }

        return data;
    }

    void restoreSavedData(String savedData) {
        restoreSavedData(savedData, true);
    }

    /**
     * Restores the table and cells from a set of saved data.
     *
     * @param savedData       the saved data to restore.
     * @param restoreSelected indicates if the selected cell that was saved should be restored.
     */
    void restoreSavedData(String savedData, boolean restoreSelected) {
        int x, y, i;
        String[] cellData = savedData.split("#"); // Data for each cell is separated by a #
        String boardData;

        // Restore the cells.
        i = 0;
        for (SudokuCell cell : this) {
            cell.restoreSavedData(cellData[i]);
            ++i;
        }

        // Saved data for the board is in the last section of the saved data.
        boardData = cellData[cellData.length - 1];

        mEditing = !(boardData.substring(0, 1).equals("0"));
        mHasErrors = !(boardData.substring(1, 2).equals("0"));
        mPreventUpdate = Integer.parseInt(boardData.substring(2, 5));

        if (restoreSelected) {
            if (!boardData.substring(5, 7).equals("xx")) {
                x = Integer.parseInt(boardData.substring(5, 6));
                y = Integer.parseInt(boardData.substring(6, 7));
                this.setSelectedCell(mCells[x][y]);
            } else {
                mSelectedCell = null;
            }
        }
    }

    void prepareBoardForSolving() {
        resetUsage();

        for (SudokuCell cell : this) {
            if (!cell.isFixed()) {
                cell.resetPossibleNumbers();
                cell.setCalculatedNumber(0, false);
            }
        }

        // Needs to be done separately from the above so that the cells to calculate are set up
        // before removing the list of fixed values from the available values.
        for (SudokuCell cell : this) {
            if (cell.isFixed()) {
                useNumber(cell, cell.getCurrentNumber(), true);
            }
        }
    }

    int getNumberToTryInCell(SudokuCell cell) {
        int n, m;

        for (n = 0, m = 1; n < 9; n++, m++) {
            if (cell.getUsage(m) == CellUsage.AVAILABLE) {
                if (mHorizontalUsage[cell.y][n] == CellUsage.AVAILABLE && mVerticalUsage[cell.x][n] == CellUsage.AVAILABLE) {
                    if (mBlockUsage[cell.cellBlock().y][cell.cellBlock().x][n] == CellUsage.AVAILABLE) {
                        return m;
                    }
                }
            }
        }

        return -1;
    }

    /**
     * Determines whether the specified cell is solvable; that is, if there are any numbers that
     * haven't been used vertically, horizontally or within the block.
     *
     * @param cell the cell to check.
     * @return boolean indicating if the cell is solvable.
     */
    boolean isCellSolvable(SudokuCell cell) {
        for (int n = 0; n < 9; n++) {
            if (mHorizontalUsage[cell.y][n] == CellUsage.AVAILABLE) {
                if (mVerticalUsage[cell.x][n] == CellUsage.AVAILABLE) {
                    CellBlock block = cell.cellBlock();
                    if (mBlockUsage[block.y][block.x][n] == CellUsage.AVAILABLE) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    void useNumber(SudokuCell cell, int number, boolean fixed) {
        CellBlock block = cell.cellBlock();
        CellUsage usage = (fixed ? CellUsage.FIXED : CellUsage.USED);
        int x = cell.x;
        int y = cell.y;

        number = number - 1;

        if (mHorizontalUsage[y][number] == CellUsage.AVAILABLE) {
            mHorizontalUsage[y][number] = usage;
            mHorizontalAvailable[y]--;
        }

        if (mVerticalUsage[x][number] == CellUsage.AVAILABLE) {
            mVerticalUsage[x][number] = usage;
            mVerticalAvailable[x]--;
        }

        if (mBlockUsage[block.y][block.x][number] == CellUsage.AVAILABLE) {
            mBlockUsage[block.y][block.x][number] = usage;
            mBlockAvailable[block.y][block.x]--;
        }

        Log.d(TAG, String.format("Available {%d,%d}: H:%d, V:%d, B:%d", x, y, mHorizontalAvailable[y], mVerticalAvailable[x], mBlockAvailable[block.y][block.x]));
    }

    void unuseNumber(SudokuCell cell, int number) {
        CellBlock block = cell.cellBlock();
        int x = cell.x;
        int y = cell.y;

        number = number - 1;

        if (mHorizontalUsage[y][number] != CellUsage.AVAILABLE) {
            mHorizontalUsage[y][number] = CellUsage.AVAILABLE;
            mHorizontalAvailable[y]++;
        }

        if (mVerticalUsage[x][number] != CellUsage.AVAILABLE) {
            mVerticalUsage[x][number] = CellUsage.AVAILABLE;
            mVerticalAvailable[x]++;
        }

        if (mBlockUsage[block.y][block.x][number] != CellUsage.AVAILABLE) {
            mBlockUsage[block.y][block.x][number] = CellUsage.AVAILABLE;
            mBlockAvailable[block.y][block.x]++;
        }
    }

    /**
     * @param cell
     * @return
     */
    int findSingleNumber(SudokuCell cell) {
        int number = -1;
        int n;
        CellBlock block = cell.cellBlock();

        for (n = 0; n < 9; n++) {
            if (mHorizontalUsage[cell.y][n] == CellUsage.AVAILABLE) {
                if (number > 0) {
                    number = -1;
                    break;
                }

                number = n + 1;
            }
        }

        if (number > 0) {
            n = number - 1;
            if (mVerticalUsage[cell.x][n] != CellUsage.AVAILABLE || mBlockUsage[block.y][block.x][n] != CellUsage.AVAILABLE) {
                number = -1;
            }
        } else {
            for (n = 0; n < 9; n++) {
                if (mVerticalUsage[cell.x][n] == CellUsage.AVAILABLE) {
                    if (number > 0) {
                        number = -1;
                        break;
                    }

                    number = n + 1;
                }
            }

            if (number > 0) {
                n = number - 1;
                if (mHorizontalUsage[cell.y][n] != CellUsage.AVAILABLE || mBlockUsage[block.y][block.x][n] != CellUsage.AVAILABLE) {
                    number = -1;
                }
            } else {
                for (n = 0; n < 9; n++) {
                    if (mBlockUsage[block.y][block.x][n] == CellUsage.AVAILABLE) {
                        if (number > 0) {
                            number = -1;
                            break;
                        }

                        number = n + 1;
                    }
                }

                if (number > 0) {
                    n = number - 1;
                    if (mHorizontalUsage[cell.y][n] != CellUsage.AVAILABLE || mVerticalUsage[cell.x][n] != CellUsage.AVAILABLE) {
                        number = -1;
                    }
                }
            }
        }

        return number;
    }

    private int dp2px(int dp) {
        return (int) (dp * displayDensity + 0.5f);
    }

    /********************************************************************************/

    /**
     * Handles the onClick event for each cell.
     * When editing, each cell's onClick event handler is assigned to this function.
     * <p>
     * When a user clicks a cell, it becomes the currently selected cell.
     *
     * @param view the TextView that was clicked.
     */
    @Override
    public void onClick(View view) {
        SudokuCell found = null;
        TextView tv = (TextView) view;

        // Need to find which cell the clicked TextView is related to.
        for (SudokuCell cell : this) {
            if (tv == cell.getTextView()) {
                found = cell;
                break;
            }
        }

        if (found != null) {
            setSelectedCell(found);
        } else {
            unsetSelectedCell();
        }
    }

    /**
     * Debug function that indicates how numbers are currently available for a cell.
     *
     * @param cell the SudokuCell to get results for.
     * @return a string with a character for each number, where the character represents the
     * availability of that number for this cell; F=Fixed, T=Tried, A=Available, U=Unavailable.
     */
    String whatNumbers(SudokuCell cell) {
        CellBlock block = cell.cellBlock();
        int n;
        String s = "";

        for (n = 0; n < 9; n++) {
            if (n > 0) s = s + ", ";
            s = s + String.valueOf(n + 1);
            if (cell.getUsage(n + 1) == CellUsage.FIXED) {
                s = s + "F";
            } else if (cell.getUsage(n + 1) == CellUsage.TRIED) {
                s = s + "T";
            } else if (mHorizontalUsage[cell.x][n] == CellUsage.AVAILABLE && mVerticalUsage[cell.y][n] == CellUsage.AVAILABLE && mBlockUsage[block.x][block.y][n] == CellUsage.AVAILABLE) {
                s = s + "A";
            } else {
                s = s + "U";
            }
        }

        return s;
    }

    /********************************************************************************/
    /* Allows for simpler iteration through all of the cells with:-
          for (SudokuCell cell : {this|mSudokuBoard}) {
              cell...
          }
     */
    @Override
    public Iterator<SudokuCell> iterator() {
        return new SudokuCellIterator();
    }


    class SudokuCellIterator implements Iterator<SudokuCell> {
        private int x, y;

        SudokuCellIterator() {
            x = 0;
            y = 0;
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

            SudokuCell cell = mCells[this.x][this.y];

            ++x;
            if (x >= 9) {
                x = 0;
                ++y;
            }

            return cell;
        }
    }

/*
    void outUsage() {
        int x, y, n, m;
        SudokuCell c;
        String s;

        for (y = 0; y < 9; y++) {
            for (n = 0; n < 9; n+=3) {
                s = "";
                for (x = 0; x < 9; x++) {
                    c = mCells[x][y];
                    for (m = 0; m < 3; m++) {
                        CellUsage usage = c.getUsage(n + m + 1);
                        switch (usage) {
                            case USED:
                                s = s + "U";
                                break;
                            case FIXED:
                                s = s + "F";
                                break;
                            case AVAILABLE:
                                s = s + "A";
                                break;
                            default:
                                s = s + "T";
                                break;
                        }
                    }
                    if (x<8) s = s + "|";
                }
                Log.d("#", s);
            }
            Log.d("#", "------------------------------");
        }
    }
*/

    class SudokuBlockIterator implements Iterator<SudokuCell> {
        private CellBlock block;
        private int x, y;

        SudokuBlockIterator(SudokuCell cell) {
            block = cell.cellBlock();
            x = block.xl;
            y = block.yt;
        }

        @Override
        public boolean hasNext() {
            return (y <= block.yb);
        }

        @Override
        public SudokuCell next() {
            SudokuCell cell = mCells[x][y];
            ++x;
            if (x > block.xr) {
                x = block.xl;
                ++y;
            }
            return cell;
        }
    }

}
