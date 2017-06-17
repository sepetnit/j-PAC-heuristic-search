package org.cs4j.core.algorithms.kgoal;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cs4j.core.*;
import org.cs4j.core.algorithms.auxiliary.SearchResultImpl;
import org.cs4j.core.algorithms.weighted.WAStar;
import org.cs4j.core.collections.PackedElement;
import org.cs4j.core.domains.GridPathFinding;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Vitali Sepetnitsky on 2017-04-24.
 *
 * The purpose of this class is to implement k-goal search using a single open list.
 * The search will first start for looking the first goal. Then, we have the G value of any state in
 * the closed list, is to the shortest path from Start to that state.
 *
 */
public class KBidirectionalAstar extends GenericSearchAlgorithm {

    // The domain for the search
    protected MultipleGoalsSearchDomain searchDomain;
    protected SearchResultImpl result;
    protected WAStar basicAlgorithm;

    private final static Logger logger = LogManager.getLogger(GridPathFinding.class);

    protected static final Map<String, Class>
            KBidirectionalAStarPossibleParameters;

    // Declare the parameters that can be tuned before running the search
    static
    {
        KBidirectionalAStarPossibleParameters = new HashMap<>();
    }

    @Override
    public Map<String, Class> getPossibleParameters() {
        return KBidirectionalAstar.KBidirectionalAStarPossibleParameters;
    }

    @Override
    public void setAdditionalParameter(String parameterName, String value) { }

    /**
     * The constructor of the class
     */
    public KBidirectionalAstar() {
        this.basicAlgorithm = new WAStar();
    }

    @Override
    public String getName() {
        return "K-Bidirectional A*";
    }

    public SearchResult concreteSearch(MultipleGoalsSearchDomain domain) {
        this.searchDomain = domain;
        this.result = new SearchResultImpl();
        long totalTime = 0;


        /*
        System.out.println("All goals:");
        for (int i = 0; i < this.searchDomain.goalsCount(); ++i) {
            System.out.println(this.searchDomain.unpack(this.searchDomain.getGoalFromAllGoals(i)).dumpStateShort());
        }*/


            // Go through ALL goals
        for (int i = 0; i < this.searchDomain.totalGoalsCount(); ++i) {
            SearchResult singleResult = null;
            KBidirectionalAstar.logger.info("Solving goal {} of {}", i+1,
                    this.searchDomain.totalGoalsCount());
            // Every even goal is solved while storing costs
            if (i % 2 == 0) {

                this.searchDomain.setInitialStateToDefaultAndSetNthGoal(i);
                // Only the i'th goal is relevant
                this.searchDomain.setValidityOfOnlyGoal(i);
                System.out.println("All goals:");
                for (int j = 0; j < this.searchDomain.validGoalsCount(); ++j) {
                    System.out.println(this.searchDomain.unpack(this.searchDomain.getNthValidGoal(j)).dumpStateShort());
                }
                this.basicAlgorithm.setAdditionalParameter("store-best-costs", "true");
                this.basicAlgorithm.setAdditionalParameter("use-best-costs", "false");
                singleResult = this.basicAlgorithm.search(this.searchDomain);
                System.out.println(singleResult.getExpanded());
                KBidirectionalAstar.logger.info(
                        "Goal {} solved; {} costs stored",
                        i+1,
                        calcTotalBest(basicAlgorithm.bestCosts));
            // Odd goals are solved bidirectionally ...

                /*
                System.out.println("start1:" + this.searchDomain.initialState().dumpStateShort());
                System.out.println("goal1:" + this.searchDomain.unpack(
                        this.searchDomain.getFirstValidGoal()).dumpStateShort());
                */
            } else {
                if (i == 99) {
                    System.out.println("99");
                }

                // Exchange between the initial state and the relevant goal
                this.searchDomain.exchangeBetweenStartStateAndNthGoal(i);
                /*
                System.out.println("start2:" + this.searchDomain.initialState().dumpStateShort());
                System.out.println("goal2:" + this.searchDomain.unpack(
                        this.searchDomain.getAllGoals().get(0)).dumpStateShort());
                */
                System.out.println("All goals:");
                for (int j = 0; j < this.searchDomain.validGoalsCount(); ++j) {
                    System.out.println(this.searchDomain.unpack(this.searchDomain.getNthValidGoal(j)).dumpStateShort());
                }
                this.basicAlgorithm.setAdditionalParameter("use-best-costs", "true");
                this.basicAlgorithm.setAdditionalParameter("store-best-costs", "false");


                singleResult = this.basicAlgorithm.search(this.searchDomain);
                KBidirectionalAstar.logger.info(
                        "Goal {} solved; {} costs stored",
                        i+1,
                        calcTotalBest(basicAlgorithm.bestCosts));
                logger.error("EXPANDEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEED: " + singleResult.getExpanded());
                logger.error("LENNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNN: " + singleResult.getBestSolution().getLength());
                if (i == 99) {
                    System.out.println("99");
                    System.out.println(singleResult.getBestSolution().dumpSolution());
                }
            }
            if (singleResult == null) {
                KBidirectionalAstar.logger.warn("Error occurred - no result");
            } else if (singleResult.solutionsCount() == 0) {
                KBidirectionalAstar.logger.warn("No solution for goal {}", i+1);
            } else {
                this.result.addSolution(singleResult.getSolutions().get(0));
                this.result.setExpanded(this.result.getExpanded() + singleResult.getExpanded());
                this.result.addConcreteResult(singleResult);
            }
            // In any case increase the time
            totalTime += singleResult.getWallTimeMillis();
        }
        // print total time
        System.out.println("totalTime=" + totalTime);

        return this.result;
    }


    public long calcTotalBest(Map<PackedElement, Map<PackedElement, Double>> bestCosts) {
        long total = 0;
        for (PackedElement x: bestCosts.keySet()) {
            total += bestCosts.get(x).size();
        }
        return total;
    }

}
