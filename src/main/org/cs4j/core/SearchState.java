package org.cs4j.core;

import org.cs4j.core.collections.PackedElement;

/**
 * The State interface.
 */
public abstract class SearchState {

    public SearchDomain domain;

    public SearchState(SearchDomain domain) {
        this.domain = domain;
    }

    /**
     * Returns the parent state which allows to reconstruct the found solution path
     *
     * @return The found parent state
     */
    public abstract SearchState getParent();

    /**
     * Returns the heuristic estimate for the state.
     *
     * @return the heuristic estimate
     */
    public abstract double getH();

    // Default implementation
    public double[] getHToAllGoals() {
        return null;
    }

    /**
     * Updates the h value with the given one
     *
     * @return The previous value
     */
    public double setH(double hValue) {
        // Do nothing by default
        return -1;
    }

    /**
     * Returns the distance estimate for the state.
     *
     * @return the distance estimate
     */
    public abstract double getD();

    /**
     * The function resets all the values of the state
     */
    public void resetStateMetaData() {}

    /**
     * Performs a deep clone of the state
     *
     * @return The copied state
     */
    // TODO
    public SearchState copy() {return null;}

    public PackedElement pack() {
        return this.domain.pack(this);
    }

    /**
     * Returns a string representation of the state
     *
     * @return The string representation of the state
     */
    public abstract String dumpState();

    /**
     * Returns an alternative SHORT string representation of the state
     *
     * @return A short representation of the state
     */
    public abstract String dumpStateShort();
}
