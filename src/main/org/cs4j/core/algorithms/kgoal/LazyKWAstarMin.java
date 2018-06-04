package org.cs4j.core.algorithms.kgoal;

import org.apache.commons.lang3.ArrayUtils;
import org.cs4j.core.*;
import org.cs4j.core.algorithms.auxiliary.GenericNode;
import org.cs4j.core.algorithms.weighted.GenericWAstar;
import org.cs4j.core.collections.PackedElement;

import java.util.*;

/**
 * Created by Vitali Sepetnitsky on 2017-06-17.
 * <p>
 * Lazy A*
 */
public class LazyKWAstarMin extends GenericWAstar<LazyKWAstarMin.LazyAstarNode, LazyKWAstarMin
        .LazyAstarNodeComparator> {

    // The domain for the search
    protected MultipleGoalsSearchDomain domain;

    @Override
    protected boolean shouldStop(SearchResultImpl result) {
        return result.solutionsCount() == this.domain.totalGoalsCount();
    }

    @Override
    public SearchResultImpl search(SearchDomain domain) {
        return domain.searchBy(this);
    }


    @Override
    public SearchResultImpl concreteSearch(MultipleGoalsSearchDomain domain) {
        this.domain = domain;
        SearchResultImpl result = super.search(domain);
        // Now, we know each solution is a result for a single goal - let's create a concrete
        // result for each goal
        List<PackedElement> foundGoals = new ArrayList<>();

        for (SearchResultImpl.Solution sol : result.getSolutions()) {
            foundGoals.add(sol.getGoal().pack());
        }

        List<SearchResultImpl.Solution> rearrangedSolutions = new ArrayList<>();

        for (PackedElement goal : this.domain.getAllGoals()) {
            rearrangedSolutions.add(result.getSolutions().get(foundGoals.indexOf(goal)));
        }

        SearchResultImpl newResult = new SearchResultImpl();
        for (SearchResultImpl.Solution sol : rearrangedSolutions) {
            SearchResultImpl concreteResult = new SearchResultImpl();
            concreteResult.addSolution(sol);
            newResult.addConcreteResult(concreteResult);
            newResult.setExpanded(result.getExpanded());
            newResult.setGenerated(result.getGenerated());
        }

        result = newResult;

        for (SearchResultImpl s : result.getConcreteResults()) {
            System.out.println("lennnnnnnnnnnnnnnnnnnnnnnnnnnnn " + s.getBestSolution().getLength());
        }
        return result;
    }

    @Override
    protected LazyAstarNode _selectNode() {
        // Whether we need to improve the F value
        boolean FValueImproved = true;
        LazyAstarNode toReturn = null;
        while (FValueImproved) {
            toReturn = this.open.peek();
            open.remove(toReturn);
            double updatedHValue = toReturn.reComputeH();
            if (toReturn.getH() != updatedHValue) {
                toReturn.setH(updatedHValue);
                this.open.add(toReturn);
                FValueImproved = true;
            } else {
                FValueImproved = false;
            }
        }
        return toReturn;
    }

    /**
     * Assumes the index of the goal in the goals array is valid ...
     *
     * @param goal The goal whose solution should be calculated
     * @return The found solution for single goal
     */
    @Override
    protected SearchResultImpl.SolutionImpl getSolution(SearchDomain domain, LazyAstarNode goal) {
        PackedElement packedState = goal.getPacked();
        SearchState stateOfGoal = this.domain.unpack(packedState);
        int goalIndex = LazyKWAstarMin.this.domain.getGoalIndexForStateIfValid(stateOfGoal);
        this.domain.setGoalInvalid(goalIndex);
        return super.getSolution(domain, goal);
    }


    @Override
    protected LazyAstarNode getNode(SearchState s) {
        return new LazyAstarNode(s);
    }

    @Override
    protected LazyAstarNode getNode(SearchState state,
                                    LazyAstarNode parent,
                                    SearchState parentState,
                                    Operator op,
                                    Operator pop) {
        return new LazyAstarNode(state, parent, parentState, op, pop);
    }

    @Override
    protected LazyAstarNodeComparator getComparator() {
        return new LazyAstarNodeComparator();
    }

    protected class LazyAstarNode extends GenericNode<LazyAstarNode> {
        private double allHs[];
        private int responsibleGoalIndex;

        private double minValue(double[] array) {
            // Convert the primitive array into a Class array
            Double[] dArray = ArrayUtils.toObject(array);
            List<Double> dList = Arrays.asList(dArray);

            // Find the minimum value
            return Collections.min(dList);
        }

        /**
         * The h value should be recomputed only if the responsible goal is not active
         *
         * @return The updated h value (or the same h value if the responsible goal is still active)
         */
        private double reComputeH() {
            if (!LazyKWAstarMin.this.domain.isValidGoalIndex(this.responsibleGoalIndex)) {
                SearchState currentState = LazyKWAstarMin.this.domain.unpack(this.getPacked());
                return this.computeH(currentState);
            }
            return this.getH();
        }


        @Override
        protected double computeH(SearchState state) {
            this.allHs = state.getHToAllGoals();
            if (this.allHs == null) {
                assert false;
            }
            double minH = this.minValue(this.allHs);

            // Look for the index of the goal that is equal to minH -
            // this is the 'responsible' goal
            this.responsibleGoalIndex = ArrayUtils.indexOf(this.allHs, minH);

            return minH;
        }

        private LazyAstarNode(SearchState state,
                              LazyAstarNode parent,
                              SearchState parentState,
                              Operator op,
                              Operator pop) {
            super(state, parent, parentState, op, pop);
        }

        private LazyAstarNode(SearchState s) {
            super(s);
        }
    }

    /**
     * The nodes comparator class
     */
    protected final class LazyAstarNodeComparator implements Comparator<LazyAstarNode> {

        @Override
        public int compare(final LazyAstarNode a, final LazyAstarNode b) {
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
