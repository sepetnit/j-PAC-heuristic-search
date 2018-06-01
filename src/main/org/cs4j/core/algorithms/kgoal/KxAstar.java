package org.cs4j.core.algorithms.kgoal;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cs4j.core.*;
import org.cs4j.core.SearchResultImpl;
import org.cs4j.core.algorithms.weighted.GenericWAstar;
import org.cs4j.core.algorithms.weighted.WAstar;
import org.cs4j.core.domains.GridPathFinding;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by user on 2017-04-03.
 *
 */
public class KxAstar extends GenericSearchAlgorithm {
    private final static Logger logger = LogManager.getLogger(GridPathFinding.class);


    // The domain for the search
    protected MultipleGoalsSearchDomain searchDomain;
    protected SearchResultImpl result;
    protected GenericWAstar basicAlgorithm;

    protected static final Map<String, Class> KxAStarPossibleParameters;

    // Declare the parameters that can be tuned before running the search
    static
    {
        KxAStarPossibleParameters = new HashMap<>();
    }

    @Override
    public Map<String, Class> getPossibleParameters() {
        return KxAstar.KxAStarPossibleParameters;
    }

    @Override
    public void setAdditionalParameter(String parameterName, String value) { }

    /**
     * The constructor of the class
     */
    public KxAstar() {
        this.basicAlgorithm = new WAstar();
    }

    @Override
    public String getName() {
        return "K x A*";
    }

    @Override
    public SearchResultImpl concreteSearch(MultipleGoalsSearchDomain domain) {
        // TODO
        this.searchDomain = domain;
        this.result = new SearchResultImpl();
        long totalTime = 0;

        for (int i = 0; i < this.searchDomain.totalGoalsCount(); ++i) {
            SearchResultImpl singleResult = null;
            KxAstar.logger.info("Solving goal {} of {}", i+1,
                    this.searchDomain.totalGoalsCount());
            // Only the i'th goal is relevant
            this.searchDomain.setValidityOfOnlyGoal(i);
            singleResult = this.basicAlgorithm.search(this.searchDomain);
            // Get the first result
            if (singleResult.solutionsCount() == 0) {
                KxAstar.logger.warn("No solution for goal {}", i+1);
            } else {
                logger.error("EXPANDEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEED: " + singleResult.getExpanded());
                logger.error("LENNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNN: " + singleResult.getBestSolution().getLength());
                this.result.addSolution(singleResult.getSolutions().get(0));
                this.result.setExpanded(this.result.getExpanded() + singleResult.getExpanded());
                this.result.addConcreteResult(singleResult);
            }
            // In any case increase the time
            totalTime += singleResult.getWallTimeMillis();
        }
        // print total time
        System.out.println("totalTime=" + totalTime);
        System.out.println("result = ");
        System.out.println(this.result);

        return this.result;
    }
}