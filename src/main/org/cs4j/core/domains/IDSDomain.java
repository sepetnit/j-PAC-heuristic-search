package org.cs4j.core.domains;

import org.cs4j.core.MultipleGoalsSearchDomain;
import org.cs4j.core.Operator;
import org.cs4j.core.SearchState;
import org.cs4j.core.collections.PackedElement;

import java.io.IOException;
import java.util.*;


public class IDSDomain extends MultipleGoalsSearchDomain {

    private ArrayList<IDSGraphNode> goals;
    private IDSGraphNode initialState;
    Map<Integer, IDSGraphNode> nodes;
    Map<IDSGraphNode, ArrayList<IDSGraphEdge>> transitions;
    Map<Integer, ArrayList<ArrayList<IDSGraphNode>>> buildings;

    private static final int RANGE_MIN = 0;
    private static final int RANGE_MAX = 1;
    private static final int MIN_COST = 1;
    private static final int MAX_COST = 10;
    private static final int COST_UPPER_BOUND = MAX_COST - MIN_COST;


    public IDSDomain(){
        this.goals = new ArrayList<>();
        this.initialState = null;
        this.nodes = new HashMap<>();
        this.transitions = new HashMap<>();
        this.buildings = new HashMap<>();
    }

    /**
     * creates an instance of the IDS Domain
     * @param buildingsNum the number of buildings in the domain
     * @param storiesRange an array of size = 2, where index 0 is the minimal number of stories
     *                     in a building and index 1 is the maximal number
     * @param deviceRange an array of size = 2, where index 0 is the minimal number of devices (vertices)
     *                    in a story and index 1 is the maximal number
     * @param k the number of goals (number of sensors)
     * @return the new IDSDomain instance
     */
    public void setupIDSDomain(int buildingsNum, int[] storiesRange, int[] deviceRange, int k) throws IOException {

        // validate input
        if(buildingsNum <= 0)
            throw new IOException("building number has to be > 0");
        if(storiesRange == null || storiesRange.length != 2 ||
                storiesRange[RANGE_MIN] < 1 || storiesRange[RANGE_MAX] < storiesRange[RANGE_MIN])
            throw new IOException("invalid storiesRange");
        if(deviceRange == null || deviceRange.length != 2 ||
                deviceRange[RANGE_MIN] < 1 || deviceRange[RANGE_MAX] < deviceRange[RANGE_MIN])
            throw new IOException("invalid deviceRange");
        if(k <= 0)
            throw new IOException("k has to be > 0");


        // setup the instance
        Random rand = new Random();
        int storiesUpperBound = storiesRange[RANGE_MAX] - storiesRange[RANGE_MIN];
        int devicesUpperBound = deviceRange[RANGE_MAX] - deviceRange[RANGE_MIN];
        for(int building = 0; building < buildingsNum; building++){

            // get storiesNum such that: storiesRange[RANGE_MIN] <= storiesNum <= storiesRange[RANGE_MAX]
            int storiesNum = storiesRange[RANGE_MIN] + rand.nextInt(storiesUpperBound);
            ArrayList<ArrayList<IDSGraphNode>> stories = new ArrayList<>();
            for(int story = 0; story < storiesNum; story++){
                int devicesNum = deviceRange[RANGE_MIN] + rand.nextInt(devicesUpperBound);
                ArrayList<IDSGraphNode> clique = new ArrayList<>();

                // create all the devices of a story
                for(int device = 0; device < devicesNum; device++){

                    // TODO - set the heuristic correctly
                    IDSGraphNode newState = new IDSGraphNode(device, building, story, 1, null);
                    clique.add(newState);
                    this.nodes.put(newState.hashCode(), newState);
                }

                // create transitions
                for(int i = 0; i < clique.size(); i++){
                    ArrayList<IDSGraphEdge> neighbors = new ArrayList<>();
                    for(int j = 0; j < clique.size(); j++) {
                        if(i != j){
                            int cost = MIN_COST + rand.nextInt(COST_UPPER_BOUND);
                            IDSGraphEdge newEdge = new IDSGraphEdge(clique.get(i), clique.get(j), cost);
                            neighbors.add(newEdge);
                        }
                    }
                    this.transitions.put(clique.get(i), neighbors);
                }

                // add the story to the building's stories
                stories.add(clique);
            }

            // add the stories created to the buildings map
            this.buildings.put(building, stories);
        }

        // setup goals
        for(int i = 0; i < k; i++){

            // choose random building
            int randBuilding = rand.nextInt(this.buildings.size());
            ArrayList<ArrayList<IDSGraphNode>> building = this.buildings.get(randBuilding);

            // choose random story
            int randStory = rand.nextInt(building.size());
            ArrayList<IDSGraphNode> story = building.get(randStory);

            // choose random device
            int randDevice = rand.nextInt(story.size());
            IDSGraphNode device = story.get(randDevice);

            // could loop forever if all the devices of a story are goals already
            while(this.goals.contains(device)){
                randDevice = rand.nextInt(story.size());
                device = story.get(randDevice);
            }

            // if we get here - the device is not a goal already
            device.setGoal(true);
            this.goals.add(device);
        }
    }


    @Override
    protected SearchState createInitialState() {
        Random rand = new Random();

        // choose random building
        int randBuilding = rand.nextInt(this.buildings.size());
        ArrayList<ArrayList<IDSGraphNode>> building = this.buildings.get(randBuilding);

        // choose random story
        int randStory = rand.nextInt(building.size());
        ArrayList<IDSGraphNode> story = building.get(randStory);

        // choose random device
        int randDevice = rand.nextInt(story.size());
        IDSGraphNode device = story.get(randDevice);

        // could loop forever if all the devices of a story are goals already
        while(this.goals.contains(device)){
            randDevice = rand.nextInt(story.size());
            device = story.get(randDevice);
        }

        this.initialState = device;
        return device;
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
        IDSGraphNode n = (IDSGraphNode)state;
        return transitions.get(n).size();
    }

    @Override
    public Operator getOperator(SearchState state, int index) {
        IDSGraphNode n = (IDSGraphNode)state;
        return transitions.get(n).get(index);
    }

    @Override
    public SearchState applyOperator(SearchState state, Operator op) {
        IDSGraphEdge edge = (IDSGraphEdge)op;
        IDSGraphNode node = (IDSGraphNode)state;
        if (node.equals(edge.a)) {
            return edge.b;
        } else if (node.equals(edge.b)) {
            return edge.a;
        } else {
            // TODO
            assert false;
            return null;
        }
    }

    @Override
    public SearchState copy(SearchState state) {
        IDSGraphNode node = (IDSGraphNode)state;
        return new IDSGraphNode(node.building, node.story, node.device, node.h, node.parent);
    }

    @Override
    public PackedElement pack(SearchState state) {
        IDSGraphNode node = (IDSGraphNode)state;
        return new PackedElement(node.hashCode());
    }

    public SearchState unpackLite(PackedElement packed) {
        return this.unpack(packed);
    }

    @Override
    public SearchState unpack(PackedElement packed) {
        assert packed.getLongsCount() == 1;
        return this.nodes.get((int)packed.getFirst());
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
        return 0;
    }

    @Override
    public int maxGeneratedSize() {
        throw new UnsupportedOperationException();
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

    private class IDSGraphNode extends SearchState {
        private String name;
        private int device;
        private int building;
        private int story;
        private double h;
        private boolean isGoal;
        private IDSGraphNode parent;

        private IDSGraphNode(String name,double h) {
            super(IDSDomain.this);
            this.name = name;
            this.h = h;
        }

        private IDSGraphNode(int building, int story, int device, double h, IDSGraphNode parent) {
            this("device " + device + " story " + story + " building " + building, h);
            this.building = building;
            this.story = story;
            this.device = device;
            this.parent = parent;
            this.isGoal = false;
        }

        @Override
        public boolean equals(Object other) {
            try {
                IDSGraphNode node = (IDSGraphNode)other;
                return node.name == this.name && node.h == this.h;
            } catch (ClassCastException e) {
                return false;
            }
        }

        @Override
        public SearchState getParent() {
            return this.parent;
        }

        public double getH() {
            return this.h;
        }

        @Override
        public double getD() {
            return this.h;
        }

        @Override
        public String dumpState() {
            return this.name;
        }

        @Override
        public String dumpStateShort() {
            return this.dumpState();
        }

        public boolean isGoal() {
            return isGoal;
        }

        public void setGoal(boolean goal) {
            isGoal = goal;
        }
    }

    private class IDSGraphEdge implements Operator {
        private IDSGraphNode a;
        private IDSGraphNode b;
        private double cost;

        private IDSGraphEdge(IDSGraphNode a, IDSGraphNode b, double cost) {
            this.a = a;
            this.b = b;
            this.cost = cost;
        }

        @Override
        public double getCost(SearchState state, SearchState parent) {
            IDSGraphNode a = (IDSGraphNode)state;
            IDSGraphNode b = (IDSGraphNode)parent;
            assert (a.equals(this.a) && b.equals(this.b)) || (a.equals(this.b) && b.equals(this.a));
            return this.cost;
        }

        @Override
        public Operator reverse(SearchState state) {
            IDSGraphNode s = (IDSGraphNode)state;
            assert s.equals(this.a) || s.equals(this.b);
            return this;
        }
    }










}

