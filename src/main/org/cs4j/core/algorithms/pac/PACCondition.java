package org.cs4j.core.algorithms.pac;

import org.cs4j.core.SearchResultImpl;
import org.cs4j.core.SearchDomain;

/**
 * Created by user on 23/02/2017.
 */
public interface PACCondition {
    public boolean shouldStop(SearchResultImpl incumbentSolution);
    public void setup(SearchDomain domain, double epsilon, double delta);
}
