package org.cs4j.core.domains;

import org.cs4j.core.MultipleGoalsSearchDomain;
import org.cs4j.core.Operator;
import org.cs4j.core.SearchState;
import org.cs4j.core.collections.PackedElement;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class IDSDomain extends MultipleGoalsSearchDomain {

    private Map<String, Class> parameters;
    private SearchState [] goals;









    // ---------------- IDS State ----------------
    private class IDSState extends SearchState {

        private String name;
        private boolean isGoal;
        private double h;

        // parent in the search path
        private SearchState parent;
        private ArrayList<SearchState> neighbors;

        private IDSState(String name, boolean isGoal, double h, ArrayList<SearchState> neighbors) {
            super(IDSDomain.this);
            this.name = name;
            this.isGoal = isGoal;
            this.h = h;
            this.parent = null;

            if(neighbors != null)
                this.neighbors = neighbors;
            else
                this.neighbors = new ArrayList<SearchState>();
        }

        @Override
        public SearchState getParent() {
            return this.parent;
        }

        @Override
        public double getH() {
            return this.h;
        }

        @Override
        public double getD() {
            return this.h;
        }

        @Override
        public String dumpState() {
            return this.name + " has H = " + this.h + " and " + this.neighbors.size() + " neighbors";
        }

        @Override
        public String dumpStateShort() {
            return null;
        }

        private String _getName() {
            return name;
        }

        private void _setName(String name) {
            this.name = name;
        }

        private boolean _isGoal() {
            return isGoal;
        }

        private void _setNeighbors(ArrayList<SearchState> neighbors){
            if(this.neighbors.isEmpty() && (neighbors != null))
                this.neighbors = neighbors;
            else
                IDSDomain.this.logger.error("failed to set neighbors");
        }

        private void _addNeighbor(SearchState neighbor){
            if(neighbor != null)
                this.neighbors.add(neighbor);
            else
                IDSDomain.this.logger.error("trying to add null state as neighbor");
        }

    }





    // ---------------- MultipleGoalsSearchDomain Override ----------------

    @Override
    protected SearchState createInitialState() {
        return null;
    }

    @Override
    public boolean goalsAreExplicit() {
        return false;
    }

    @Override
    protected PackedElement getNthGoalFromAllGoals(int goalIndex) {
        return null;
    }

    @Override
    protected List<PackedElement> getAllGoalsInternal() {
        return null;
    }

    @Override
    public int totalGoalsCount() {
        return 0;
    }

    @Override
    protected boolean stateIsOneOfValidGoals(SearchState s) {
        return false;
    }

    @Override
    public int getGoalIndexForStateIfValid(SearchState s) {
        return 0;
    }

    @Override
    public int getNumOperators(SearchState state) {
        return 0;
    }

    @Override
    public Operator getOperator(SearchState state, int index) {
        return null;
    }

    @Override
    public SearchState applyOperator(SearchState state, Operator op) {
        return null;
    }

    @Override
    public SearchState copy(SearchState state) {
        return null;
    }

    @Override
    public PackedElement pack(SearchState state) {
        return null;
    }

    @Override
    public SearchState unpack(PackedElement packed) {
        return null;
    }

    @Override
    public SearchState unpackLite(PackedElement packed) {
        return null;
    }

    @Override
    public String dumpStatesCollection(SearchState[] states) {
        return null;
    }

    @Override
    public boolean isCurrentHeuristicConsistent() {
        return false;
    }

    @Override
    public void setOptimalSolutionCost(double cost) {

    }

    @Override
    public double getOptimalSolutionCost() {
        return -1;
    }

    @Override
    public int maxGeneratedSize() {
        return 0;
    }

    @Override
    public String getInputFileName() {
        return null;
    }

    @Override
    public Map<String, Class> getPossibleParameters() {
        return null;
    }

    @Override
    public void setAdditionalParameter(String parameterName, String value) {

    }
}
