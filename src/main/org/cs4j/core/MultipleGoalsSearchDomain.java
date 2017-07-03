package org.cs4j.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cs4j.core.collections.PackedElement;

import java.util.Arrays;
import java.util.List;

/**
 * Created by user on 2017-04-03.
 *
 */
public abstract class MultipleGoalsSearchDomain implements SearchDomain {

    protected Logger logger;

    private boolean[] validGoals;

    // Cache of indexes of valid goals in the goals array
    private int[] validGoalsIndexes;

    private boolean useCache;

    //private boolean startStateIsGoal;
    //private List<PackedElement> initialStatePackedAsList;

    private SearchState basicInitialState;
    private PackedElement packedBasicInitialState;

    private SearchState initialState;
    private PackedElement packedInitialState;

    @Override
    public final SearchState initialState() {
        return this.initialState;
    }


    protected abstract SearchState createInitialState();


    public void exchangeBetweenStartStateAndNthGoal(int goalIndex) {
        this.resetInitialStateToDefaultValue();
        this.makeStartStateEqualToNthGoal(goalIndex);
    }

    public void setInitialStateToDefaultAndSetNthGoal(int goalIndex) {
        this.resetInitialStateToDefaultValue();
        this.setValidityOfOnlyGoal(goalIndex);
    }

    protected boolean startStateIsGoal() {
        return (packedBasicInitialState != null) &&
                !this.packedBasicInitialState.equals(this.packedInitialState);
    }

    /**
     * The function sets the start state to be the single goal of the domain
     */
    private void makeStartStateEqualToNthGoal(int goalIndex) {
        this.logger.warn("Start state is now the single goal of the domain");
        PackedElement goal = this.getGoalFromAllGoals(goalIndex);
        this.packedInitialState = new PackedElement(goal);
        this.initialState = this.unpack(goal);
        System.out.println("Updated initial: " + initialState.dumpStateShort());
    }

    private void resetInitialStateToDefaultValue() {
        this.initialState = this.basicInitialState.copy();
        this.packedInitialState = new PackedElement(this.packedBasicInitialState);
    }


    private void setValidityOfAllGoals(boolean validity) {
        for (int i = 0; i < this.validGoals.length; ++i) {
            this.validGoals[i] = validity;
        }
    }

    public void setAllGoalsValid() {
        this.setValidityOfAllGoals(true);
        for (int i = 0; i < this.validGoalsIndexes.length; ++i) {
            this.validGoalsIndexes[i] = i;
        }
    }

    public void setAllGoalsInvalid() {
        this.setValidityOfAllGoals(false);
        Arrays.fill(validGoalsIndexes, Integer.MAX_VALUE);
    }

    private void setValidityOfSingleGoal(int goal, boolean validity) {
        if (goal < 0 || goal > this.validGoals.length - 1) {
            throw new IllegalArgumentException("Invalid goal index");
        }
        this.validGoals[goal] = validity;
    }

    private void addGoalIndex(int index) {
        // If the index is already inside - do nothing, otherwise ...
        if (Arrays.binarySearch(this.validGoalsIndexes, index) == -1) {
            this.validGoalsIndexes[this.validGoalsIndexes.length -1] = index;
            Arrays.sort(this.validGoalsIndexes);
        }
    }

    private void removeGoalIndex(int index) {
        int indexInside = Arrays.binarySearch(this.validGoalsIndexes, index);
        if (indexInside != -1) {
            this.validGoalsIndexes[indexInside] = Integer.MAX_VALUE;
            Arrays.sort(this.validGoalsIndexes);
        }
    }

    public void setGoalValid(int goal) {
        this.setValidityOfSingleGoal(goal, true);
        this.addGoalIndex(goal);
    }

    public void setGoalInvalid(int goal) {
        this.setValidityOfSingleGoal(goal, false);
        this.removeGoalIndex(goal);
    }

    public void setValidityOfOnlyGoal(int goal) {
        this.setAllGoalsInvalid();
        this.setGoalValid(goal);
    }

    protected void _initializeMultipleGoalsEnvironment(int goalsCount) {
        // Initialize the valid goals array
        this.validGoals = new boolean[goalsCount];
        this.validGoalsIndexes = new int[goalsCount];
        this.setAllGoalsValid();
        // Create the basic initial state
        // (all goals are valid, thus the basic heuristic value will be
        // based on the first valid goal)
        this.basicInitialState = createInitialState();
        this.packedBasicInitialState = this.pack(this.basicInitialState);
        // Copy basic initial state values
        this.resetInitialStateToDefaultValue();
    }

    /**
     * Default Constructor - currently empty
     */
    protected MultipleGoalsSearchDomain() {
        this.logger = LogManager.getLogger(this.getClass());
        this.useCache = true;
    }

    /**
     * @return Whether the goals of the domain are explicitly given
     */
    public abstract boolean goalsAreExplicit();

    /**
     * Calculates the index of the nth valid goal in the goals list
     *
     * @param validGoalIndex The required valid goal index
     *
     * @return The found index in the goals list or -1 if nothing was found
     */
    protected int getNthValidGoalIndex(int validGoalIndex) {
        if (this.startStateIsGoal()) {
            if (validGoalIndex == 0) {
                return 0;
            }
        } else {
            // We can use cache and return the data without searching
            if (this.useCache) {
                return this.validGoalsIndexes[validGoalIndex];
            }

            // Otherwise, search the goal
            int currentValid = 0;
            for (int i = 0; i < this.totalGoalsCount(); ++i) {
                if (this.validGoals[i]) {
                    ++currentValid;
                }
                if (currentValid == validGoalIndex) {
                    return i;
                }
            }
        }

        // Nothing was found
        return -1;
    }

    /**
     * An internal method which returns a specific VALID goal from the goals
     * list. The function assumes that the goalIndex is valid.
     *
     * @param goalIndex The index of the required goal.
     *                  Note that the index should refer to the valid goals,
     *                  e.g., there can be 10 goals and only 2 valid goals. In
     *                  that case, goalIndex must be between 0 and 1.
     *
     * @return The required goal
     */
    public PackedElement getNthValidGoal(int goalIndex) {
        // We can't retrieve goals in case they are not explicitly defined by
        // the domain
        if (!goalsAreExplicit()) {
            this.logger.error("This domain ({}) has no explicit goals",
                    this.getClass().getName());
            throw new IllegalArgumentException();
        }
        if (goalIndex < 0 || goalIndex > this.validGoalsCount() - 1) {
            this.logger.error("Invalid goal index ({}), should be at least" +
                    " 0 and at most {}", goalIndex,
                    this.validGoalsCount() -1);
        }

        if (this.startStateIsGoal()) {
            return this.packedBasicInitialState;
        }

        // Calculate the index of the real goal in the goals list and return the
        // relevant goal
        return this.getNthGoalFromAllGoals(this.getNthValidGoalIndex(goalIndex));
    }

    /**
     * An internal method which returns a specific goal from the goals list
     * The function assumes that the goalIndex is valid
     *
     * @param goalIndex The index of the required goal
     *
     * @return The required goal
     */
    protected abstract PackedElement getNthGoalFromAllGoals(int goalIndex);

    /**
     * A wrapper function which retrieves the goal at the given index. In
     * addition the function assures that goals can be retrieved (goals are
     * explicit) and also the index of the required goal is valid
     *
     * @param goalIndex The index of the goal that should be retrieved
     *
     * @return The required goal
     */
    public PackedElement getGoalFromAllGoals(int goalIndex) {
        // We can't retrieve goals in case they are not explicitly defined by
        // the domain
        if (!goalsAreExplicit()) {
            this.logger.error("This domain ({}) has no explicit goals",
                    this.getClass().getName());
            throw new IllegalArgumentException();
        }
        if (goalIndex < 0 || goalIndex > this.totalGoalsCount() - 1) {
            this.logger.error("Invalid goal index ({}), should be at least" +
                    " 0 and at most {}", goalIndex, totalGoalsCount() -1);
        }
        return this.getNthGoalFromAllGoals(goalIndex);
    }

    /**
     * @return The first goal in the goals list (not necessary valid goal!)
     */
    public PackedElement getFirstGoalFromAllGoals() {
        return this.getNthGoalFromAllGoals(0);
    }

    /**
     * @return The first VALID goal in the goals list
     */
    public PackedElement getFirstValidGoal() {
        if (this.startStateIsGoal()) {
            return this.packedBasicInitialState;
        }
        return this.getNthGoalFromAllGoals(this.getNthValidGoalIndex(0));
    }

    /**
     * @return All the goals defined in the domain
     * @throws IllegalArgumentException in case the domain doesn't have
     * explicit goals
     */
    public List<PackedElement> getAllGoals() {
        // We can't retrieve goals in case they are not explicitly defined by
        // the domain
        if (!goalsAreExplicit()) {
            this.logger.error("This domain ({}) has no explicit goals",
                    this.getClass().getName());
            throw new IllegalArgumentException();
        }
        return this.getAllGoalsInternal();
    }

    protected abstract List<PackedElement> getAllGoalsInternal();

    /**
     * @return The number of goals defined in the domain
     */
    public abstract int totalGoalsCount();

    @Override
    public boolean improveStateHValue(
            SearchState s, SearchAlgorithm alg) {
        return alg.concreteImproveStateHValue(s, this);
    }

    /**
     * @return The number of goals defined as valid
     */
    public int validGoalsCount() {
        // If the start state is the single goal - return 1
        if (this.startStateIsGoal()) {
            return 1;
        }

        int toReturn = 0;
        for (boolean validity : this.validGoals) {
            if (validity) {
                ++toReturn;
            }
        }
        return toReturn;
    }

    /**
     * @return The number of the goals defined as invalid
     */
    public int invalidGoalsCount() {
        if (this.startStateIsGoal()) {
            this.logger.warn("Irrelevant question: the only valid goal is the start state");
            return -1;
        }
        return this.totalGoalsCount() - this.validGoalsCount();
    }

    public boolean isValidGoalIndex(int goal) {
        try {
            return this.validGoals[goal];
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Invalid goal");
        }
    }

    protected abstract boolean stateIsOneOfValidGoals(SearchState s);

    // In case s is goal & is valid - return its index, otherwise, return -1!
    public abstract int getGoalIndexForStateIfValid(SearchState s);

    // In multiple goals search domain, the meaning of isGoal() is - isValidGoal()!
    @Override
    public final boolean isGoal(SearchState state) {
       if (this.startStateIsGoal()) {
           return state.equals(this.basicInitialState);
       }
       return this.stateIsOneOfValidGoals(state);
    }

    @Override
    public final SearchResultImpl searchBy(SearchAlgorithm alg) {
        return alg.concreteSearch(this);
    }

    protected double[] getDefaultHeuristicValuesToAllGoals() {
        double[] toReturn = new double[this.validGoals.length];
        // Default values
        Arrays.fill(toReturn, Double.MAX_VALUE);
        return toReturn;
    }

    /**
     * Given a state that represents specific status of search in the domain, the function returns
     * an array of heuristic values to ALL the goals defined for the search.
     *
     * @param s The state for which the heuristic values should be calculated
     * @return An array of the heuristic values. Double.MAX_VALUE will be returned for each goal
     *         that is unreachable from the current state.
     */
    public double[] getHeuristicValuesToAllGoals(SearchState s) {
        return s.getHToAllGoals();
    }
}