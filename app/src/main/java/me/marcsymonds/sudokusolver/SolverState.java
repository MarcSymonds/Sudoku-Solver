package me.marcsymonds.sudokusolver;

/**
 * Identifies the current state of the Solver.
 */
enum SolverState {
    /**
     * Not running; i.e. before it has started
     */
    NOT_RUNNING,
    /**
     * Currently running
     */
    RUNNING,
    /**
     * Paused - Different to cancelled; means we may save and restore the state of the solver
     */
    PAUSED,
    /**
     * Solver was cancelled
     */
    CANCELLED,
    /**
     * Solver has finished and successfully solved the board
     */
    FINISHED_SUCCESS,
    /**
     * Solver has finished, but failed to solve the board
     */
    FINISHED_FAILED;

    static SolverState fromInteger(int val) {
        switch (val) {
            case 1:
                return RUNNING;

            case 2:
                return PAUSED;

            case 3:
                return CANCELLED;

            case 4:
                return FINISHED_SUCCESS;

            case 5:
                return FINISHED_FAILED;

            default:
                return NOT_RUNNING;
        }
    }

    int toInteger() {
        switch (this) {
            case RUNNING:
                return 1;

            case PAUSED:
                return 2;

            case CANCELLED:
                return 3;

            case FINISHED_SUCCESS:
                return 4;

            case FINISHED_FAILED:
                return 5;

            default:
                return 0;
        }
    }
};
