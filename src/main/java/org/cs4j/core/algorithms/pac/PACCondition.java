package org.cs4j.core.algorithms.pac;

import org.cs4j.core.SearchResult;

/**
 * Created by user on 23/02/2017.
 */
public interface PACCondition {
    public boolean shouldStop(SearchResult incumbentSolution, double epsilon, double delta);
}
