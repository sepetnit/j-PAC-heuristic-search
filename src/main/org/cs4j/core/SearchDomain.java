package org.cs4j.core;

import org.cs4j.core.collections.PackedElement;

/**
 * The search domain interface.
 *
 * @author Matthew Hatem & Vitali Sepetnitsky
 */
public interface SearchDomain extends SearchConfigurable {

    /**
     * Returns the initial state for an instance of Domain.
     *
     * @return the initial state
     */
    SearchState initialState();

    /**
     * Returns true if the specified state is the goal state, false otherwise.
     *
     * @param state the state
     * @return true if state is a goal state, false otherwise
     */
    boolean isGoal(SearchState state);

    /**
     * Returns the number of operators applicable for the specified state.
     *
     * @param state the state
     * @return the number of operators
     */
    int getNumOperators(SearchState state);

    /**
     * Returns the specified operator applicable for the specified state.
     *
     * @param state the state
     * @param index the nth operator index
     * @return the nth operator
     */
    Operator getOperator(SearchState state, int index);

    /**
     * Applies the specified operator to the specified state and returns an
     * a new edge.
     *
     * @param state the state
     * @param op    the operator
     * @return the new edge
     */
    SearchState applyOperator(SearchState state, Operator op);

    /**
     * Returns a copy of the specified state.
     *
     * @param state the state
     * @return the copy
     */
    SearchState copy(SearchState state);

    /**
     * Packs a representation of the specified state into a long.
     *
     * @param state the state
     * @return the packed state as a long
     */
    PackedElement pack(SearchState state);

    /**
     * Unpacks the specified packed representation into a new state.
     *
     * @param packed the long representation
     * @return the new state
     */
    SearchState unpack(PackedElement packed);

    /**
     * Unpacks a GridPathFinding state from a long number without calculating h values
     * Caution : Be careful - H is not calculated!
     */
    SearchState unpackLite(PackedElement packed);

    /**
     * This function allows to dump a collection of states based on the domain (e.g. dump all
     * the states of a
     * path-finding problem)
     * <p>
     * NOTE: The implementation of this function is optional and not required for the search
     *
     * @param states The states to dump
     * @return A unified string representation of all the states
     */
    String dumpStatesCollection(SearchState[] states);

    /**
     * @return Whether the currently used heuristic is consistent
     */
    boolean isCurrentHeuristicConsistent();

    /**
     * For tests with oracles, set the optimal cost of the solution
     */
    void setOptimalSolutionCost(double cost);

    /**
     * @return The cost of the optimal solution if defined (or -1 if the cost
     * hasn't been set)
     */
    double getOptimalSolutionCost();

    /**
     * @return the number of instances to generate of this domain before
     * OutOfMemory
     */
    int maxGeneratedSize();

    /**
     * Tries to improve the heuristic value of the given state, using some
     * specific parameters of the domain
     *
     * @param s The state whose heuristic value should be improved
     * @return Whether the h value was changed
     */
    boolean improveStateHValue(SearchState s, SearchAlgorithm alg);

    /**
     * The function performs a search on the domain, using the given algorithm
     *
     * @param alg The algorithm to search with
     * @return The found result of the search
     */
    SearchResultImpl searchBy(SearchAlgorithm alg);

    /**
     * The function returns the name of the file from which the domain was read or "" if there is
     * no file
     *
     * @return The pre-defined name
     */
    String getInputFileName();
}
