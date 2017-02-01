package me.marcsymonds.sudokusolver;

/**
 *
 */
interface ISolverControl {
    /**
     * Called if the solver completes; whether successfully, or not.
     */
    void solverCompleted(Solver solver, SolverState state);

    /** Called if the solver is cancelled */
    //void solverCancelled();

    /** Called if the solver is stopped */
    //void solverStopped();
}
