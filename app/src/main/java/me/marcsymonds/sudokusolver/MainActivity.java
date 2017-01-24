package me.marcsymonds.sudokusolver;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ViewSwitcher;

public class MainActivity extends AppCompatActivity {
    final private static String TAG = MainActivity.class.getSimpleName();

    final private String SAVE_BOARD_DATA = "BoardData";
    final private String SAVE_EDITING = "EditingBoard";
    final private String SAVE_EDIT_BOARD_DATA = "EditBoardData";

    private SudokuBoard mSudokuBoard = null;
    private String mBoardSaveData = null;
    private ViewSwitcher mKeyboardSwitcher = null;
    private Handler SolverMessageHandler = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        // Instantiate the Sudoku board.
        // Must be done after setting the view because it needs to find the board TableLayout so
        // that it can add the rows and cells to the table.
        mSudokuBoard = new SudokuBoard(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mKeyboardSwitcher = (ViewSwitcher) findViewById(R.id.vwsKeySwitcher);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    /**
     * @param savedInstanceState
     */
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        // Get the data that was saved prior to starting an edit - if we were editing the board.
        mBoardSaveData = savedInstanceState.getString(SAVE_EDIT_BOARD_DATA);

        // Get the data that was saved prior to the app being destroyed.
        String data = savedInstanceState.getString(SAVE_BOARD_DATA);
        if (data != null) {
            Log.d(TAG, "Begin Update");
            mSudokuBoard.beginUpdate();

            // Restore the data to the board and cells.
            mSudokuBoard.restoreSavedData(data);

            Log.d(TAG, "End Update");
            mSudokuBoard.endUpdate(true); // Redraws the board.

            // If we were editing, then switch to the edit buttons.
            if (mSudokuBoard.isEditing()) {
                mSudokuBoard.startEdit(true);
                mKeyboardSwitcher.showNext();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        String boardData = mSudokuBoard.getSaveData();
        outState.putString(SAVE_BOARD_DATA, boardData);
        outState.putBoolean(SAVE_EDITING, mSudokuBoard.isEditing());
        if (mSudokuBoard.isEditing()) {
            outState.putString(SAVE_EDIT_BOARD_DATA, mBoardSaveData);
        }

        super.onSaveInstanceState(outState);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    /********************************************************************************/
    /* Button Events */

    /**
     * Handle the "Edit Board" button click.
     * Allows the user to start editing the board.
     *
     * @param v Reference to the button.
     */
    public void editBoardClick(View v) {
        // Switch to the edit buttons.
        mKeyboardSwitcher.showNext();

        // Save the current board set up, in case the user cancels later.
        mBoardSaveData = mSudokuBoard.getSaveData();
        /*AlertDialog.Builder msg = new AlertDialog.Builder(this);
        msg.setTitle("Data");
        msg.setMessage(mBoardSaveData).setNegativeButton("OK", null).show();*/

        mSudokuBoard.startEdit();
    }

    /**
     * @param v
     */
    public void editNumberClick(View v) {
        int number = Integer.parseInt(v.getTag().toString());
        mSudokuBoard.setSelectedCellNumber(number);
        mSudokuBoard.validateBoard();
    }

    public void editClearBoardClick(View v) throws InterruptedException {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    mSudokuBoard.beginUpdate();
                    mSudokuBoard.clearBoard();
                    mSudokuBoard.endUpdate();

                    mSudokuBoard.setSelectedCell(null); // Reselect the last selected cell.
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getResources().getString(R.string.btnEditClearBoard));
        builder.setMessage("Are you sure?")
                .setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener)
                .show();
    }

    /**
     * Handles the "Cancel" edit button.
     * Restores the board back to the state it was in when the edit started, and switches back to
     * the main buttons.
     *
     * @param v the clicked button.
     */
    public void editCancelClick(View v) {
        mSudokuBoard.endEdit();
        mSudokuBoard.restoreSavedData(mBoardSaveData, false);
        mSudokuBoard.redrawBoard();

        ViewSwitcher vs = (ViewSwitcher) findViewById(R.id.vwsKeySwitcher);
        vs.showPrevious();
    }

    /**
     * Handles the "Accept" edit button.
     * Checks if the edited board is valid, and if not informs the user.
     * Otherwise, stops the editing of the board, resets the solve parameters and switches back
     * to the main buttons.
     *
     * @param v the clicked button.
     */
    public void editAcceptClick(View v) {
        if (mSudokuBoard.hasErrors()) {
            AlertDialog.Builder msg = new AlertDialog.Builder(this);
            msg.setTitle("Board Is Invalid");

            msg
                    .setMessage("Correct the errors highlighted before continuing!")
                    .setNegativeButton("OK", null)
                    .show();
        } else {
            mSudokuBoard.endEdit();

            ViewSwitcher vs = (ViewSwitcher) findViewById(R.id.vwsKeySwitcher);
            vs.showPrevious();

            mSudokuBoard.prepareBoardForSolving();
            Solver s = new Solver();
            s.execute(mSudokuBoard);
        }
    }
}
