package org.cs4j.core;

/**
 * Created by user on 2017-05-09.
 *
 */
public abstract class SingleGoalSearchDomain implements SearchDomain {

    public boolean improveStateHValue(SearchState s, SearchAlgorithm alg) {
        return false;
    }

    @Override
    public final SearchResultImpl searchBy(SearchAlgorithm alg) {
        return alg.concreteSearch(this);
    }
}
