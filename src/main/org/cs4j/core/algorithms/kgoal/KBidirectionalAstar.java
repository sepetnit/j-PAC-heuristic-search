package org.cs4j.core.algorithms.kgoal;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cs4j.core.*;
import org.cs4j.core.algorithms.auxiliary.GenericNode;
import org.cs4j.core.algorithms.weighted.GenericWAstar;
import org.cs4j.core.collections.PackedElement;
import org.cs4j.core.domains.GridPathFinding;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by Vitali Sepetnitsky on 2017-04-24.
 * <p>
 * The purpose of this class is to implement k-goal search using a single open list.
 * The search will first start for looking the first goal. Then, we have the G value of any state in
 * the closed list, is to the shortest path from Start to that state.
 */
public class KBidirectionalAstar extends GenericSearchAlgorithm {

    private final static Logger logger = LogManager.getLogger(GridPathFinding.class);

    private final class BasicSingleDirectionAlgorithm extends
            GenericWAstar<BasicSingleDirectionAlgorithm.BasicNode,
                    BasicSingleDirectionAlgorithm.BasicNodeComp> {

        //            GenericWAstar<BasicSingleDirectionAlgorithm.BasicNode,
//                    BasicSingleDirectionAlgorithm.BasicNodeComp> {

        private Map<PackedElement, BasicNode> perfectHs;

        // The current index of the goal being searched
        // Note that the usage of this index implies that this class can't be used by a
        // multi-threaded
        // environment
        private int currentGoalIndex;

        private boolean isRegularSearch() {
            return this.currentGoalIndex % 2 == 0;
        }

        private BasicSingleDirectionAlgorithm() {
            this.perfectHs = new TreeMap<>();
            this.currentGoalIndex = 0;
        }

        @Override
        protected SearchResultImpl.SolutionImpl getSolution(SearchDomain domain, BasicNode goal) {
            SearchResultImpl.SolutionImpl basicSolution = super.getSolution(domain, goal);
            // In case the search is standard (without using previous information) or, the found
            // goal is actually a goal - use basic implementation
            if (this.isRegularSearch() || goal.isRegularNode()) {
                return basicSolution;
            }
            // Otherwise, we need to combine to paths:
            // 1. From the start to given node (called goal ...)
            // 2. From the current node to start ...
            BasicNode previous = this.perfectHs.get(goal.getPacked());

            SearchResultImpl.SolutionImpl previousSolution =
                    super.getSolution(domain, previous, false);

            // Now, combine :
            // We have : (basic) g1 -> ... -> found state ; (previous) found state -> ... -> start

            SearchState lastBasicSolState = basicSolution.removeLastState();
            //basicSolution.removeLastOperator();

            // TODO
            if (!lastBasicSolState.pack().equals(previousSolution.getStates().get(0).pack())) {
                System.out.println
                        ("BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBAAAAAAAAAAAAAAAD");
            }

            basicSolution.addStates(previousSolution.getStates());
            basicSolution.addOperators(previousSolution.getOperators());
            basicSolution.setCost(basicSolution.getCost() + previousSolution.getCost());
            basicSolution.reverseAll();

            return basicSolution;
        }

        @Override
        protected BasicNode getNode(SearchState state) {
            return new BasicNode(state);
        }

        @Override
        protected BasicNode getNode(SearchState state,
                                    BasicNode parent,
                                    SearchState parentState,
                                    Operator op,
                                    Operator pop) {
            return new BasicNode(state, parent, parentState, op, pop);
        }


        private SearchResultImpl search(SearchDomain domain, int goalIndex) {
            this.currentGoalIndex = goalIndex;
            return super.search(domain);
        }

        @Override
        protected boolean isGoal(SearchDomain domain,
                                 BasicNode node,
                                 SearchState state) {
            if (!this.isRegularSearch()) {
                if (this.perfectHs.containsKey(node.getPacked())) {
                    node.makeIrregularNode();
                    return true;
                }
            }
            return super.isGoal(domain, node, state);
        }

        @Override
        protected BasicNodeComp getComparator() {
            return new BasicNodeComp();
        }

        @Override
        protected BasicNode _selectNode() {
            BasicNode toReturn = super._selectNode();
            // Store perfect heuristic value only for odd goals
            if (this.isRegularSearch()) {
                this.perfectHs.put(toReturn.getPacked(), toReturn);
                toReturn.makeIrregularNode();
            }
            return toReturn;
        }

        protected boolean shouldStop(SearchResultImpl result) {
            return result.solutionsCount() == 1;
        }

        protected class BasicNode extends GenericNode<BasicNode> {

            // Whether this node was stored by previous searches
            private boolean isRegularNode;

            private boolean isRegularNode() {
                return this.isRegularNode;
            }

            private void makeIrregularNode() {
                this.isRegularNode = false;
            }

            private BasicNode(SearchState state) {
                super(state);
                this.isRegularNode = true;
            }

            private BasicNode(SearchState state,
                                BasicNode parent,
                                SearchState parentState,
                                Operator op,
                                Operator pop) {
                super(state, parent, parentState, op, pop);
                this.isRegularNode = true;
            }
        }

        /**
         * The nodes comparator class
         */
        protected class BasicNodeComp implements Comparator<BasicNode> {

            @Override
            public int compare(final BasicNode a, final BasicNode b) {
                // First compare by wF (smaller is preferred), then by g (bigger is preferred)
                if (a.getF() < b.getF()) {
                    return -1;
                }
                if (a.getF() > b.getF()) {
                    return 1;
                }
                if (a.getG() > b.getG()) {
                    return -1;
                }
                if (a.getG() < b.getG()) {
                    return 1;
                }
                return 0;
            }
        }
    }

    private static final Map<String, Class>
            KBidirectionalAStarPossibleParameters;

    // Declare the parameters that can be tuned before running the search
    static {
        KBidirectionalAStarPossibleParameters = new HashMap<>();
    }

    // The domain for the search
    private MultipleGoalsSearchDomain searchDomain;
    protected SearchResultImpl result;
    private BasicSingleDirectionAlgorithm basicAlgorithm;

    @Override
    public Map<String, Class> getPossibleParameters() {
        return KBidirectionalAstar.KBidirectionalAStarPossibleParameters;
    }

    @Override
    public void setAdditionalParameter(String parameterName, String value) {
    }

    /**
     * The constructor of the class
     */
    public KBidirectionalAstar() {
        this.basicAlgorithm = new BasicSingleDirectionAlgorithm();
    }

    @Override
    public String getName() {
        return "K-Bidirectional A*";
    }

    @Override
    public SearchResultImpl concreteSearch(MultipleGoalsSearchDomain domain) {
        this.searchDomain = domain;
        this.result = new SearchResultImpl();

        long totalTime = 0;


        /*
        System.out.println("All goals:");
        for (int i = 0; i < this.searchDomain.goalsCount(); ++i) {
            System.out.println(this.searchDomain.unpack(this.searchDomain.getGoalFromAllGoals(i))
            .dumpStateShort());
        }*/


        // Go through ALL goals
        for (int i = 0; i < this.searchDomain.totalGoalsCount(); ++i) {
            SearchResultImpl singleResult;
            KBidirectionalAstar.logger.info("Solving goal {} of {}", i + 1,
                    this.searchDomain.totalGoalsCount());
            // Every even goal is solved while storing costs
            if (i % 2 == 0) {
                this.searchDomain.setInitialStateToDefaultAndSetNthGoal(i);
                // Only the i'th goal is relevant
                this.searchDomain.setValidityOfOnlyGoal(i);
                /*
                System.out.println("All goals:");
                for (int j = 0; j < this.searchDomain.validGoalsCount(); ++j) {
                    System.out.println(this.searchDomain.unpack(this.searchDomain.getNthValidGoal
                            (j)).dumpStateShort());
                }*/
                //this.basicAlgorithm.setAdditionalParameter("store-best-costs", "true");
                //this.basicAlgorithm.setAdditionalParameter("use-best-costs", "false");
                singleResult = this.basicAlgorithm.search(this.searchDomain, i);
                /*
                KBidirectionalAstar.logger.info(
                        "Goal {} solved; {} costs stored",
                        i + 1,
                        calcTotalBest(basicAlgorithm.bestCosts));
                */
                KBidirectionalAstar.logger.info("Goal {} solved", i + 1);

                logger.error("EXPANDEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEED: " + singleResult
                        .getExpanded());
                logger.error("LENNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNN: " + singleResult
                        .getBestSolution().getLength());

                // Odd goals are solved bidirectionally ...

                /*
                System.out.println("start1:" + this.searchDomain.initialState().dumpStateShort());
                System.out.println("goal1:" + this.searchDomain.unpack(
                        this.searchDomain.getFirstValidGoal()).dumpStateShort());
                */
            } else {
                // Exchange between the initial state and the relevant goal
                this.searchDomain.exchangeBetweenStartStateAndNthGoal(i);
                /*
                System.out.println("start2:" + this.searchDomain.initialState().dumpStateShort());
                System.out.println("goal2:" + this.searchDomain.unpack(
                        this.searchDomain.getAllGoals().get(0)).dumpStateShort());
                System.out.println("All goals:");
                for (int j = 0; j < this.searchDomain.validGoalsCount(); ++j) {
                    System.out.println(this.searchDomain.unpack(this.searchDomain.getNthValidGoal
                            (j)).dumpStateShort());
                }
                */
                //this.basicAlgorithm.setAdditionalParameter("use-best-costs", "true");
                //this.basicAlgorithm.setAdditionalParameter("store-best-costs", "false");
                singleResult = this.basicAlgorithm.search(this.searchDomain, i);
                /*
                KBidirectionalAstar.logger.info(
                        "Goal {} solved; {} costs stored",
                        i + 1,
                        calcTotalBest(basicAlgorithm.bestCosts));
                        */
                KBidirectionalAstar.logger.info("Goal {} solved", i + 1);

                logger.error("EXPANDEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEED: " + singleResult
                        .getExpanded());
                logger.error("LENNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNN: " + singleResult
                        .getBestSolution().getLength());
            }
            if (singleResult.solutionsCount() == 0) {
                KBidirectionalAstar.logger.warn("Error occurred - no result");
            } else if (singleResult.solutionsCount() == 0) {
                KBidirectionalAstar.logger.warn("No solution for goal {}", i + 1);
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


    /*
    public long calcTotalBest(Map<PackedElement, Map<PackedElement, Double>> bestCosts) {
        long total = 0;
        for (PackedElement x : bestCosts.keySet()) {
            total += bestCosts.get(x).size();
        }
        return total;
    }
    */

}
