package me.marcsymonds.sudokusolver;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.Iterator;

/**
 * Created by Marc on 09/12/2016.
 */

class SudokuBoard implements Iterable<SudokuCell>, TextView.OnClickListener {
    final private String TAG = SudokuBoard.class.getSimpleName();

    Context context;

    private float displayDensity;

    private TableLayout mBoardTable = null;
    private SudokuCell[][] mCells = new SudokuCell[9][9];

    private CellNumberUsage[][][] mBlockUsage = new CellNumberUsage[3][3][9];
    private CellNumberUsage[][] mHorizontalUsage = new CellNumberUsage[9][9];
    private CellNumberUsage[][] mVerticalUsage = new CellNumberUsage[9][9];
    private int mHorizontalAvailable[] = new int[9];
    private int mVerticalAvailable[] = new int[9];
    private int mBlockAvailable[][] = new int[3][3];

    private SudokuCell mSelectedCell = null;
    private boolean mEditing = false;
    private boolean mHasErrors = false;

    private int mPreventUpdate = 0;

    SudokuBoard(Context c) {
        Configuration config;
        int x, y;
        DisplayMetrics dm = new DisplayMetrics();
        float maxSize;
        int numSize, boardSize;

        context = c;
        config = context.getResources().getConfiguration();
        ((Activity) context).getWindowManager().getDefaultDisplay().getMetrics(dm); //context.getResources().getDisplayMetrics();
        displayDensity = dm.density;

        // 70% of the smallest dimension - width or height.
        maxSize = ((config.screenWidthDp < config.screenHeightDp) ? config.screenWidthDp : config.screenHeightDp) * 0.7f;

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
        buildBoardTableView(numSize);
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
                mVerticalUsage[x][n] = CellNumberUsage.AVAILABLE;
                mHorizontalUsage[x][n] = CellNumberUsage.AVAILABLE;
            }
            mVerticalAvailable[x] = 9;
            mHorizontalAvailable[x] = 9;
        }

        for (y = 0; y < 3; y++) {
            for (x = 0; x < 3; x++) {
                for (n = 0; n < 9; n++) {
                    mBlockUsage[x][y][n] = CellNumberUsage.AVAILABLE;
                }
                mBlockAvailable[x][y] = 9;
            }
        }
    }

    private void buildBoardTableView(int cellSize) {
        int x, y;
        TableRow tr;
        View divider;
        SudokuCell cell;
        TextView txt;

        // Colours to use for drawing the board.,
        int frameColour = ContextCompat.getColor(context, R.color.boardFrame);
        int dividerColour = ContextCompat.getColor(context, R.color.boardDivider);
        int cellColour = ContextCompat.getColor(context, R.color.cellInvisible);
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

        // Draw the board.

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
                txt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f);
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
    }

    void endUpdate() {
        endUpdate(false);
    }

    void endUpdate(boolean force) {
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
     * rules. Also checks for cells that are not solvable due to all of the numbers being used
     * within the row, column or block.
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
            for (SudokuBlockIterator it = new SudokuBlockIterator(this, cell); it.hasNext(); ) {
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

    /**
     * Clears all cells flagged as an error.
     */
    void clearErrors() {
        mHasErrors = false;

        for (SudokuCell cell : this) {
            cell.setError(false);
        }
    }

    /**
     * Retrieve the specified SudokuCell object.
     *
     * @param x
     * @param y
     * @return the SudokuCell object.
     */
    SudokuCell getCell(int x, int y) {
        return mCells[x][y];
    }

    void startEdit() {
        startEdit(false);
    }

    /**
     * Set the board up for editing.
     * <p>
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
                    cell.unsetNumber(true);
                } else {
                    //Log.d(TAG, String.format("Using Number %d at [%d,%d]", cell.getCurrentNumber(), cell.x, cell.y));
                    useNumber(cell, cell.getCurrentNumber(), true);
                    cell.show();
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

    void showAll() {
        for (SudokuCell cell : this) {
            cell.show();
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
        }
    }

    void setSelectedCellNumber(int number) {
        if (mSelectedCell != null) {
            int prevNum = mSelectedCell.getCurrentNumber();
            if (prevNum > 0) {
                unuseNumber(mSelectedCell, prevNum);
            }

            if (number > 0) {
                mSelectedCell.setNumber(number, CellState.HARD_FIXED, true, true);
                useNumber(mSelectedCell, number, true);
            } else {
                mSelectedCell.unsetNumber(true);
            }
        }
    }

    /**
     * Moves the selected cell one position to the right, wrapping at the end of the row and the
     * end of the board.
     */
    void moveSelectedCellToNext() {
        int x = 0, y = 0;

        if (mSelectedCell != null) {
            x = mSelectedCell.x + 1;
            y = mSelectedCell.y;

            if (x > 8) {
                x = 0;
                ++y;
                if (y > 8) {
                    y = 0;
                }
            }
        }

        setSelectedCell(mCells[x][y]);
    }

    /**
     * Return data representing the current state of the board. This can be restored later to set
     * the board back to the saved state.
     *
     * @return String containing the save data for all of the cells and save data for the table.
     */
    String getSaveData() {
        int x, y, n;
        StringBuilder data = new StringBuilder();

        // Get the save data for each cell.
        for (SudokuCell cell : this) {
            data.append(cell.getSaveData());
            data.append("#");
        }

        // Get the save data for the board.
        data.append(String.format("%d%d%03d",
                mEditing ? 1 : 0,
                mHasErrors ? 1 : 0,
                mPreventUpdate));

        // Currently selected cell.
        if (mSelectedCell == null) {
            data.append("xx");
        } else {
            data.append(mSelectedCell.x);
            data.append(mSelectedCell.y);//String.format("%d%d", mSelectedCell.x, mSelectedCell.y));
        }

        // Get the save data for the number availability.
        data.append("#");

        for (x = 0; x < 9; x++) {
            data.append(mHorizontalAvailable[x]);
            data.append(mVerticalAvailable[x]);
            //data.append(String.format("%d%d", mHorizontalAvailable[x], mVerticalAvailable[x]));
            for (n = 0; n < 9; n++) {
                data.append(mHorizontalUsage[x][n].toInteger());
                data.append(mVerticalUsage[x][n].toInteger());
                //data.append(String.format("%d%d", mHorizontalUsage[x][n], mVerticalUsage[x][n]));
            }
        }

        for (y = 0; y < 3; y++) {
            for (x = 0; x < 3; x++) {
                data.append(mBlockAvailable[x][y]);
                for (n = 0; n < 9; n++) {
                    data.append(mBlockUsage[x][y][n].toInteger());
                }
            }
        }

        return data.toString();
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
        int x, y, n;
        SavedDataReader savedDataReader = new SavedDataReader(savedData);

        // Restore the cells.
        for (SudokuCell cell : this) {
            cell.restoreSavedData(savedDataReader);//savedDataReader.getSectionData(true));
            savedDataReader.nextSection();
        }

        // Saved data for the board is in the next section of the saved data.
        //boardData = cellData[i];

        mEditing = savedDataReader.readBool();
        mHasErrors = savedDataReader.readBool();
        mPreventUpdate = savedDataReader.readInt(3);

        if (restoreSelected) {
            x = savedDataReader.readInt();
            if (x < 0) {
                mSelectedCell = null;
            } else {
                y = savedDataReader.readInt();
                this.setSelectedCell(mCells[x][y]);
            }
        }

        // Saved data for the number availability.
        savedDataReader.nextSection();

        for (x = 0; x < 9; x++) {
            mHorizontalAvailable[x] = savedDataReader.readInt();// data.append(mHorizontalAvailable[x]);
            mVerticalAvailable[x] = savedDataReader.readInt(); // data.append(mVerticalAvailable[x]);
            for (n = 0; n < 9; n++) {
                mHorizontalUsage[x][n] = CellNumberUsage.fromInteger(savedDataReader.readInt()); // data.append(mHorizontalUsage[x][n]);
                mVerticalUsage[x][n] = CellNumberUsage.fromInteger(savedDataReader.readInt()); // data.append(mVerticalUsage[x][n]);
            }
        }

        for (y = 0; y < 3; y++) {
            for (x = 0; x < 3; x++) {
                mBlockAvailable[x][y] = savedDataReader.readInt();
                for (n = 0; n < 9; n++) {
                    mBlockUsage[x][y][n] = CellNumberUsage.fromInteger(savedDataReader.readInt());
                }
            }
        }
    }

    /**
     * Prepares the bold for solving by clearing any previously calculated numbers.
     */
    void prepareBoardForSolving() {
        resetUsage();

        // Clear all previously calculated (i.e. not Fixed) numbers, and reset the
        // "possible numbers" for the cell.
        for (SudokuCell cell : this) {
            if (!cell.isFixed()) {
                cell.resetPossibleNumbers();
                cell.unsetNumber(false);
            }
        }

        // Now "use" the fixed numbers so that we know which numbers are available for other cells.
        // Needs to be done separately from the above so that the cells to calculate are set up
        // before removing the list of fixed values from the available values.
        for (SudokuCell cell : this) {
            if (cell.isFixed()) {
                useNumber(cell, cell.getCurrentNumber(), true);
            }
        }
    }

    /**
     * Determines an available number that can be tried in the specified cell.
     *
     * @param cell the cell to get the number for.
     * @return the number to try (1 to 9) or -1 if no numbers are available.
     */
    int getNumberToTryInCell(SudokuCell cell) {
        int n, m;

        for (n = 0, m = 1; n < 9; n++, m++) {
            if (cell.getUsage(m) == CellNumberUsage.AVAILABLE) {
                if (mHorizontalUsage[cell.y][n] == CellNumberUsage.AVAILABLE && mVerticalUsage[cell.x][n] == CellNumberUsage.AVAILABLE) {
                    if (mBlockUsage[cell.cellBlock.x][cell.cellBlock.y][n] == CellNumberUsage.AVAILABLE) {
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
        /*if (cell.x == 4 && cell.y == 8){
            String h = "", v = "", b = "";

            for (int n = 0; n < 9; n++) {
                h = h + String.valueOf(mHorizontalUsage[cell.y][n].toInteger());
                v = v + String.valueOf(mVerticalUsage[cell.x][n].toInteger());
                b = b + String.valueOf(mBlockUsage[cell.cellBlock.x][cell.cellBlock.y][n].toInteger());
            }

            Log.d(TAG, String.format("4,8=H(%s), V(%s), B(%s)", h, v, b));
        }*/

        for (int n = 0; n < 9; n++) {
            if (mHorizontalUsage[cell.y][n] == CellNumberUsage.AVAILABLE) {
                if (mVerticalUsage[cell.x][n] == CellNumberUsage.AVAILABLE) {
                    if (mBlockUsage[cell.cellBlock.x][cell.cellBlock.y][n] == CellNumberUsage.AVAILABLE) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    void useNumber(SudokuCell cell, int number, boolean fixed) {
        CellNumberUsage usage = (fixed ? CellNumberUsage.FIXED : CellNumberUsage.USED);
        int x = cell.x;
        int y = cell.y;

        number = number - 1;

        if (mHorizontalUsage[y][number] == CellNumberUsage.AVAILABLE) {
            mHorizontalUsage[y][number] = usage;
            mHorizontalAvailable[y]--;
        }

        if (mVerticalUsage[x][number] == CellNumberUsage.AVAILABLE) {
            mVerticalUsage[x][number] = usage;
            mVerticalAvailable[x]--;
        }

        if (mBlockUsage[cell.cellBlock.x][cell.cellBlock.y][number] == CellNumberUsage.AVAILABLE) {
            mBlockUsage[cell.cellBlock.x][cell.cellBlock.y][number] = usage;
            mBlockAvailable[cell.cellBlock.x][cell.cellBlock.y]--;
        }

        //Log.d(TAG, String.format("Available {%d,%d}: H:%d, V:%d, B:%d", x, y, mHorizontalAvailable[y], mVerticalAvailable[x], mBlockAvailable[cell.cellBlock.y][cell.cellBlock.x]));
    }

    void unuseNumber(SudokuCell cell, int number) {
        int x = cell.x;
        int y = cell.y;

        number = number - 1;

        if (mHorizontalUsage[y][number] != CellNumberUsage.AVAILABLE) {
            mHorizontalUsage[y][number] = CellNumberUsage.AVAILABLE;
            mHorizontalAvailable[y]++;
        }

        if (mVerticalUsage[x][number] != CellNumberUsage.AVAILABLE) {
            mVerticalUsage[x][number] = CellNumberUsage.AVAILABLE;
            mVerticalAvailable[x]++;
        }

        if (mBlockUsage[cell.cellBlock.x][cell.cellBlock.y][number] != CellNumberUsage.AVAILABLE) {
            mBlockUsage[cell.cellBlock.x][cell.cellBlock.y][number] = CellNumberUsage.AVAILABLE;
            mBlockAvailable[cell.cellBlock.x][cell.cellBlock.y]++;
        }
    }

    /**
     * @param cell
     * @return
     */
    int findSingleNumber(SudokuCell cell) {
        int number = -1;
        int n;
        CellBlock block = cell.cellBlock;

        for (n = 0; n < 9; n++) {
            if (mHorizontalUsage[cell.y][n] == CellNumberUsage.AVAILABLE) {
                if (number > 0) {
                    number = -1;
                    break;
                }

                number = n + 1;
            }
        }

        if (number > 0) {
            n = number - 1;
            if (mVerticalUsage[cell.x][n] != CellNumberUsage.AVAILABLE || mBlockUsage[block.x][block.y][n] != CellNumberUsage.AVAILABLE) {
                number = -1;
            }
        } else {
            for (n = 0; n < 9; n++) {
                if (mVerticalUsage[cell.x][n] == CellNumberUsage.AVAILABLE) {
                    if (number > 0) {
                        number = -1;
                        break;
                    }

                    number = n + 1;
                }
            }

            if (number > 0) {
                n = number - 1;
                if (mHorizontalUsage[cell.y][n] != CellNumberUsage.AVAILABLE || mBlockUsage[block.x][block.y][n] != CellNumberUsage.AVAILABLE) {
                    number = -1;
                }
            } else {
                for (n = 0; n < 9; n++) {
                    if (mBlockUsage[block.x][block.y][n] == CellNumberUsage.AVAILABLE) {
                        if (number > 0) {
                            number = -1;
                            break;
                        }

                        number = n + 1;
                    }
                }

                if (number > 0) {
                    n = number - 1;
                    if (mHorizontalUsage[cell.y][n] != CellNumberUsage.AVAILABLE || mVerticalUsage[cell.x][n] != CellNumberUsage.AVAILABLE) {
                        number = -1;
                    }
                }
            }
        }

        return number;
    }

    /**
     * Convert density pixels in to actual pixels.
     *
     * @param dp density pixels to convert.
     * @return actual pixels.
     */
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

    /********************************************************************************/
    /* Allows for simpler iteration through all of the cells with:-
          for (SudokuCell cell : {this|mSudokuBoard}) {
              cell...
          }
     */
    @Override
    public Iterator<SudokuCell> iterator() {
        return new SudokuCellIterator(this);
    }
}


