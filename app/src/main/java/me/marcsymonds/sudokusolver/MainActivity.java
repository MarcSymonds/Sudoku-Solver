package me.marcsymonds.sudokusolver;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import java.util.ArrayList;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements ISolverControl {
    final private static String TAG = MainActivity.class.getSimpleName();

    final private String SAVE_BOARD_DATA = "BoardData";
    final private String SAVE_EDIT_BOARD_DATA = "EditBoardData";
    final private String SAVE_SOLVER_RUN_STATE = "SolverRunState";
    final private String SAVE_SOLVER_DATA = "SolverData";
    final private String SAVE_SOLVED_CELL_LIST = "SolvedCells";

    private SudokuBoard mSudokuBoard = null;
    private ViewSwitcher mKeyboardSwitcher = null;
    private Solver mBoardSolver = null;
    private ArrayList<SudokuCell> mSolvedCellList = null;

    private String mBoardSaveData = null;
    private String mEditSaveData = null;
    private String mSolverSaveData = null;
    private SolverState mSolverRunState = SolverState.NOT_RUNNING;
    private String mSolvedCellListData = null;

    MainActivity() {
        Log.d(TAG, "MainActivity Constructed");
    }

    /********************************************************************************/
    /* Application control */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, String.format("onCreate - Has Bundle: %s, Has Board Data: %s",
                (savedInstanceState == null ? "NO" : "YES"),
                (mBoardSaveData == null ? "NO" : "YES")
        ));

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

        Log.d(TAG, "onStart");
        Log.d(TAG, String.format("- Has Board Data: %s",
                (mBoardSaveData == null ? "NO" : mBoardSaveData)
        ));

        Log.d(TAG, String.format("- Has Edit Data: %s",
                (mEditSaveData == null ? "NO" : mEditSaveData)
        ));

        Log.d(TAG, String.format("- Has Solver Data: %s",
                (mSolverSaveData == null ? "NO" : mSolverSaveData)
        ));

        Log.d(TAG, String.format("- Has Solved Cell List: %s",
                (mSolvedCellListData == null ? "NO" : mSolvedCellListData)
        ));

    }

    /**
     * @param savedInstanceState
     */
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        Log.d(TAG, "onRestoreInstanceState");

    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.d(TAG, "onResume");

        // Restore the saved data.

        SharedPreferences prefs = getPreferences(MODE_PRIVATE);

        // Get the data that was saved prior to the app being destroyed.
        mBoardSaveData = prefs.getString(SAVE_BOARD_DATA, null);
        if (mBoardSaveData != null) {
            Log.d(TAG, String.format("Restoring: %s=%s", SAVE_BOARD_DATA, mBoardSaveData));
        } else
            Log.d(TAG, String.format("No %s", SAVE_BOARD_DATA));

        // Get the data that was saved prior to starting an edit - if we were editing the board.
        mEditSaveData = prefs.getString(SAVE_EDIT_BOARD_DATA, null);
        if (mEditSaveData != null) {
            Log.d(TAG, String.format("Restoring: %s=%s", SAVE_EDIT_BOARD_DATA, mEditSaveData));
        } else
            Log.d(TAG, String.format("No %s", SAVE_EDIT_BOARD_DATA));

        // Get the saved data for the solver.
        mSolverSaveData = prefs.getString(SAVE_SOLVER_DATA, null);
        if (mSolverSaveData != null) {
            Log.d(TAG, String.format("Restoring: %s=%s", SAVE_SOLVER_DATA, mSolverSaveData));
        } else
            Log.d(TAG, String.format("No %s", SAVE_SOLVER_DATA));

        mSolverRunState = SolverState.fromInteger(prefs.getInt(SAVE_SOLVER_RUN_STATE, SolverState.NOT_RUNNING.toInteger()));
        Log.d(TAG, String.format("Restoring: %s=%d (%s)", SAVE_SOLVER_RUN_STATE, mSolverRunState.toInteger(), mSolverRunState.toString()));

        mSolvedCellListData = prefs.getString(SAVE_SOLVED_CELL_LIST, null);
        if (mSolvedCellListData != null) {
            Log.d(TAG, String.format("Restoring: %s=%s", SAVE_SOLVED_CELL_LIST, mSolvedCellListData));
        }

        // Restore the state of the application.

        if (mBoardSaveData != null) {
            Log.d(TAG, "Begin Update");
            mSudokuBoard.beginUpdate();

            // Restore the data to the board and cells.
            mSudokuBoard.restoreSavedData(mBoardSaveData);

            Log.d(TAG, "End Update");
            mSudokuBoard.endUpdate(true); // Redraws the board.

            // If we were editing, then switch to the edit buttons.
        }

        // Restore the solver.

        if (mSolverSaveData != null) {
            Log.d(TAG, String.format("Restoring solver data: %s, %s", mSolverRunState.toString(), mSolverSaveData));

            mBoardSolver = new Solver(this, mSudokuBoard, new SavedDataReader(mSolverSaveData));
            mSolverSaveData = null;

            // If the solver was paused mid-solving, then restart it.
            if (!mBoardSolver.isCompleted()) {
                Log.d(TAG, "Restarting solver");
                mBoardSolver.execute(mSudokuBoard);
            }
        }

        if (mSolverRunState == SolverState.PAUSED) {
            solverProgressIndicator(true, SolverProgressTextIndication.SOLVING);
        } else if (mSolverRunState == SolverState.FINISHED_SUCCESS) {
            solverProgressIndicator(false, SolverProgressTextIndication.SOLVED);
        } else if (mSolverRunState == SolverState.FINISHED_FAILED) {
            solverProgressIndicator(false, SolverProgressTextIndication.COULD_NOT_SOLVE);
        } else {
            solverProgressIndicator(false, SolverProgressTextIndication.BLANK);
        }

        if (mSolvedCellListData != null) {
            SavedDataReader reader = new SavedDataReader(mSolvedCellListData);
            mSolvedCellList = new ArrayList<>();
            while (!reader.EOS()) {
                int x, y;
                x = reader.readInt();
                y = reader.readInt();
                mSolvedCellList.add(mSudokuBoard.getCell(x, y));
            }
        }

        if (mSudokuBoard.isEditing()) {
            mSudokuBoard.startEdit(true);
            mKeyboardSwitcher.showNext();
        }

        setShowButtonsState((mSolvedCellList != null && mSolvedCellList.size() > 0));
    }

    @Override
    protected void onPause() {
        super.onPause();

        Log.d(TAG, "onPause");

        // Application is pausing, so need to pause the Solver.

        if (mBoardSolver == null) {
            Log.d(TAG, "Solver not running");

            mSolverSaveData = null;
            // Leave mSolverRunState as-is; it will indicate how the last solve attempt ended.
            //mSolverRunState = SolverState.NOT_RUNNING;
        } else {
            Log.d(TAG, "Stopping solver");
            mBoardSolver.pause();
            mBoardSolver.waitForCompletion();

            Log.d(TAG, "Getting solver state...");
            mSolverRunState = mBoardSolver.getRunState();
            mSolverSaveData = mBoardSolver.getSaveData();
            Log.d(TAG, String.format("Got solver saved data: %s, %s", mSolverRunState.toString(), mSolverSaveData));
            mBoardSolver = null;
        }

        // Save the state of everything.

        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = prefs.edit();

        // Save the current state of the board.
        String boardData = mSudokuBoard.getSaveData();
        Log.d(TAG, String.format("Saving: %s=%s", SAVE_BOARD_DATA, boardData));
        prefsEditor.putString(SAVE_BOARD_DATA, boardData);

        // Save the state of the board before editing started.
        Log.d(TAG, String.format("Saving: %s=%s", SAVE_EDIT_BOARD_DATA, (mEditSaveData == null ? "NULL" : mEditSaveData)));
        prefsEditor.putString(SAVE_EDIT_BOARD_DATA, mEditSaveData);

        // Save the state of the solver.
        Log.d(TAG, String.format("Saving: %s=%s", SAVE_SOLVER_DATA, (mSolverSaveData == null ? "NULL" : mSolverSaveData)));
        prefsEditor.putString(SAVE_SOLVER_DATA, mSolverSaveData);

        // Save the run state of the solver.
        Log.d(TAG, String.format("Saving: %s=%d (%s)", SAVE_SOLVER_RUN_STATE, mSolverRunState.toInteger(), mSolverRunState.toString()));
        prefsEditor.putInt(SAVE_SOLVER_RUN_STATE, mSolverRunState.toInteger());

        // If the board had been solved, then save a list of the solved cells that still need to be
        // shown.
        if (mSolvedCellList != null && mSolvedCellList.size() > 0) {
            StringBuilder scl = new StringBuilder(mSolvedCellList.size() * 2);
            for (SudokuCell cell : mSolvedCellList) {
                scl.append(String.format("%d%d", cell.x, cell.y));
            }

            Log.d(TAG, String.format("Saving: %s=%s", SAVE_SOLVED_CELL_LIST, scl.toString()));
            prefsEditor.putString(SAVE_SOLVED_CELL_LIST, scl.toString());
        } else
            prefsEditor.remove(SAVE_SOLVED_CELL_LIST);

        // Commit.
        prefsEditor.commit(); //.apply();
    }

    @Override
    protected void onStop() {
        super.onStop();

        Log.d(TAG, "onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.d(TAG, "onDestroy");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState); // Moved from end of function.

        Log.d(TAG, "onSaveInstanceState");
    }

    /********************************************************************************/
    /* Menu control */

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
        //if (id == R.id.action_settings) {
//            return true;
//        }

        return super.onOptionsItemSelected(item);
    }

    /********************************************************************************/
    /* Show number and board control */

    void setShowButtonsState(boolean enabled) {
        Button btn;

        btn = (Button) findViewById(R.id.btnNextNumber);
        btn.setEnabled(enabled);

        btn = (Button) findViewById(R.id.btnSolveBoard);
        btn.setEnabled(enabled);
    }

    public void btnNextNumberClick(View v) {
        int i;

        if (mSolvedCellList.size() == 1) {
            i = 0;
        } else {
            Random r = new Random();
            i = r.nextInt(mSolvedCellList.size());
        }

        mSolvedCellList.get(i).show();

        mSolvedCellList.remove(i);

        if (mSolvedCellList.size() == 0) {
            setShowButtonsState(false);
            mSolvedCellList = null;
        }
    }

    public void btnSolveBoardClick(View v) {
        mSudokuBoard.showAll();
        setShowButtonsState(false);
        mSolvedCellList.clear();
        mSolvedCellList = null;
    }

    /********************************************************************************/
    /* Solver control */

    void solverStart() {
        solverProgressIndicator(true, SolverProgressTextIndication.SOLVING);

        mSudokuBoard.prepareBoardForSolving();
        mBoardSaveData = mSudokuBoard.getSaveData();

        if (mSolvedCellList != null) {
            mSolvedCellList.clear();
            mSolvedCellList = null;
        }
        mSolvedCellListData = null;

        mBoardSolver = new Solver(this, mSudokuBoard);
        mBoardSolver.execute(mSudokuBoard);
    }

    boolean solverStop(boolean cancel) {
        boolean cancelled;

        if (mBoardSolver != null) {
            if (cancel) {
                cancelled = mBoardSolver.cancel(false);
            } else {
                cancelled = mBoardSolver.pause();
            }

            Log.d(TAG, "Stopping solver; Waiting for task to complete");
            mBoardSolver.waitForCompletion();

            if (cancelled) {
                mBoardSolver = null;

                if (cancel) {
                    mSolverRunState = SolverState.CANCELLED;
                }
            }
        }

        return true;
    }

    @Override
    public void solverCompleted(Solver solver, SolverState state) {
        if (state == SolverState.FINISHED_SUCCESS) {
            solverProgressIndicator(false, SolverProgressTextIndication.SOLVED);
            mSudokuBoard.redrawBoard();

            mSolvedCellList = mBoardSolver.getSolvedCellList();
            mBoardSolver = null;
            mSolverRunState = state;

            setShowButtonsState(true);
        } else if (state == SolverState.FINISHED_FAILED) {
            solverProgressIndicator(false, SolverProgressTextIndication.COULD_NOT_SOLVE);
            setShowButtonsState(false);

            mBoardSolver = null;
            mSolverRunState = state;

            AlertDialog.Builder msg = new AlertDialog.Builder(this);
            msg
                    .setTitle("COULD NOT SOLVE")
                    .setMessage("Could not solve board")
                    .setNegativeButton("OK", null)
                    .show();
        }
    }

    void solverProgressIndicator(boolean showProgressIndicator, SolverProgressTextIndication msgType) {
        ProgressBar pb = (ProgressBar) findViewById(R.id.solverActivityIndicator);

        if (pb != null) {
            pb.setVisibility(showProgressIndicator ? View.VISIBLE : View.INVISIBLE);
        }

        TextView tv = (TextView) findViewById(R.id.SolverStateText);
        int textColourRes;
        int textRes;

        switch (msgType) {
            case SOLVING:
                textColourRes = R.color.solver_state_solving;
                textRes = R.string.solver_state_solving;
                break;

            case SOLVED:
                textColourRes = R.color.solver_state_solved;
                textRes = R.string.solver_state_solved;
                break;

            case COULD_NOT_SOLVE:
                textColourRes = R.color.solver_state_not_solvable;
                textRes = R.string.solver_state_not_solvable;
                break;

            default:
                textColourRes = R.color.solver_state_blank;
                textRes = R.string.solver_state_blank;
                break;

        }

        tv.setTextColor(ContextCompat.getColor(this, textColourRes));
        tv.setText(textRes);
    }

    /********************************************************************************/
    /* Button Events */

    /**
     * Handle the "Edit Board" button click.
     * Allows the user to start editing the board.
     *
     * @param v Reference to the button.
     */
    public void btnEditBoardClick(View v) {
        solverStop(true);
        solverProgressIndicator(false, SolverProgressTextIndication.BLANK);

        // Switch to the edit buttons.
        mKeyboardSwitcher.showNext();

        // Save the current board state, in case the user cancels later.
        mBoardSaveData = mSudokuBoard.getSaveData();
        mSudokuBoard.startEdit();
    }

    /**
     * Handles the pressing of a number button or the "Clr" button.
     *
     * @param v the Button that was pressed.
     */
    public void editNumberClick(View v) {
        int number = Integer.parseInt(v.getTag().toString());

        mSudokuBoard.setSelectedCellNumber(number);
        if (mSudokuBoard.validateBoard()) {
            mSudokuBoard.moveSelectedCellToNext();
        }
    }

    /**
     * Handles the pressing of the "Clear Board" button.
     *
     * @param v the Button that was pressed.
     * @throws InterruptedException
     */
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
     * Handles the pressing of the "Cancel" edit button.
     * <p>
     * Restores the board back to the state it was in when the edit started, and switches back to
     * the main buttons.
     *
     * @param v the Button that was pressed.
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
     * <p>
     * Checks if the edited board is valid, and if not informs the user. Otherwise, stops the
     * editing of the board, resets the solve parameters and switches back to the main buttons.
     *
     * @param v the Button that was pressed.
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

            mEditSaveData = mSudokuBoard.getSaveData();

            // Switch back to the primary buttons.
            ViewSwitcher vs = (ViewSwitcher) findViewById(R.id.vwsKeySwitcher);
            vs.showPrevious();

            solverStart();
        }
    }
}
