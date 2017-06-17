package org.cs4j.core;

import java.util.List;
import java.util.TreeMap;

/**
 * The search result interface.
 *
 * @author Matthew Hatem
 */
public interface SearchResult {

    /**
     * Returns whether a solution to the problem exists
     *
     * @return true if a solution exists and false otherwise
     */
    boolean hasSolution();

    /**
     * Returns the solution path.
     *
     * @return the solution path
     */
    List<Solution> getSolutions();

    /**
     *
     * @return The number of the found solutions
     */
    int solutionsCount();

    /*
     * If multiple solutions were found, return the best one
     * (this is assumed to be the last one) #TODO: Is this assumption Ok?
     * @return the best solution found
     */
    Solution getBestSolution();

    /**
     * Returns expanded count in the first iteration of running
     *
     * @return The expanded count in the first iteration of running
     */
    long getFirstIterationExpanded();

    /**
     * Returns expanded count.
     *
     * @return expanded count
     */
    long getExpanded();

    /**
     * Returns generated count.
     *
     * @return generated count
     */
    long getGenerated();

    /**
     * Returns duplicates count.
     *
     * @return Count of the duplicate states
     */
    long getDuplicates();

    /**
     * Returns the number of duplicate states that were updated in the open list
     *
     * @return duplicates count in the open list
     */
    long getUpdatedInOpen();

    /**
     * Returns reopened count.
     *
     * @return reopened count
     */
    long getReopened();

    /**
     *
     * @return extra parameters for this search;
     */
    TreeMap<String,Object> getExtras();

    /**
     * Returns the wall time in milliseconds.
     *
     * @return the wall time in milliseconds
     */
    long getWallTimeMillis();

    /**
     * Returns the CPU time in milliseconds.
     *
     * @return the CPU time in milliseconds
     */
    long getCpuTimeMillis();

    /**
     * Increases the statistics by the values of the previous search
     */
    void increase(SearchResult previous);

    void addConcreteResult(SearchResult result);

    List<SearchResult> getConcreteResults();

    double getAverageExpanded();

        /**
         * Interface for search iterations.
         */
    interface Iteration {

        /**
         * Returns the bound for this iteration.
         *
         * @return the bound
         */
        double getBound();

        /**
         * Returns the number of nodes expanded.
         *
         * @return the number of nodes expanded
         */
        long getExpanded();

        /**
         * Returns the number of nodes generated.
         *
         * @return the number of nodes generated
         */
        long getGenerated();
    }

    /**
     * The Solution interface.
     */
    interface Solution {

        /**
         * Returns a list of operators used to construct this solution.
         *
         * @return list of operators
         */
        List<Operator> getOperators();

        /**
         * Returns a list of states used to construct this solution
         *
         * @return list of states
         */
        List<SearchState> getStates();

        /**
         * Returns a string representation of the solution
         *
         * @return A string that represents the solution
         */
        String dumpSolution();

        /**
         * Returns the cost of the solution.
         *
         * @return the cost of the solution
         */
        double getCost();

        /**
         * Returns the length of the solution.
         */
        int getLength();

    }
}
