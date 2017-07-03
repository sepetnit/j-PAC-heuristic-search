package org.cs4j.core;

/**
 * @author Daniel Gilon (at 23/02/2017)
 */
public interface AnytimeSearchAlgorithm extends SearchAlgorithm {

    /**
     * Continues the search to find a better solution
     *
     * @return A better solution, if such exists
     */
    SearchResultImpl continueSearch();

    /**
     * Returns a SearchResults object that contains all the search results
     * found till now
     */
    SearchResultImpl getTotalSearchResults();
}
