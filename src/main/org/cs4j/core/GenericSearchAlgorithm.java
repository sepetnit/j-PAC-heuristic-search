package org.cs4j.core;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * @author Vitali Sepetnitsky (created at 2017-04-03)
 */
public abstract class GenericSearchAlgorithm
        implements SearchAlgorithm {

    /**
     * The default implementation of the heuristic function improvement is to
     * do nothing
     *
     * @see SearchAlgorithm#improveStateHValue
     */
    @Override
    public  boolean improveStateHValue(
            SearchState s, SearchDomain domain) {
        return domain.improveStateHValue(s, this);
    }

    public boolean concreteImproveStateHValue(SearchState s,
                                              SingleGoalSearchDomain domain) {
        return false;
    }

    public boolean concreteImproveStateHValue(SearchState s,
                                              MultipleGoalsSearchDomain domain) {
        return false;
    }

    public SearchResultImpl search(SearchDomain domain) {
        return domain.searchBy(this);
    }

    @Override
    public SearchResultImpl concreteSearch(MultipleGoalsSearchDomain domain) {
        throw new NotImplementedException();
    }

    @Override
    public SearchResultImpl concreteSearch(SingleGoalSearchDomain domain) {
        throw new NotImplementedException();
    }

}
