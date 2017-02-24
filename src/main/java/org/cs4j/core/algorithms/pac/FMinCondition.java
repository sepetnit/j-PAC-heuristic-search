package org.cs4j.core.algorithms.pac;

import org.cs4j.core.SearchResult;

/**
 * Created by Roni Stern on 24/02/2017.
 *
 * This is a trivial PAC condition, that only halts after verifying with certainty
 * that the incumbent solution is at most 1+epsilon times the optimal solution.
 * Note, there is no consideration of delta in this condition.
 */
public class FMinCondition implements PACCondition {
    @Override
    public boolean shouldStop(SearchResult incumbentSolution, double epsilon, double delta) {
        double fmin = (Double)incumbentSolution.getExtras().get("fmin");
        double incumbent = incumbentSolution.getSolutions().get(0).getCost();

        if (incumbent/fmin <= 1+epsilon)
            return true;
        else
            return false;
    }
}
