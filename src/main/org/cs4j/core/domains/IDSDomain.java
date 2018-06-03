package org.cs4j.core.domains;

import org.cs4j.core.MultipleGoalsSearchDomain;
import org.cs4j.core.Operator;
import org.cs4j.core.SearchState;
import org.cs4j.core.collections.PackedElement;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.*;


public class IDSDomain extends MultipleGoalsSearchDomain {

    private static IDSGraphNode currGoal = null;
//    public static boolean setHToZero = false;     // for Dijkstra comparison
    private ArrayList<IDSGraphNode> goals;
    Map<Integer, IDSGraphNode> nodes;
    Map<IDSGraphNode, ArrayList<IDSGraphEdge>> transitions;
    Map<Integer, ArrayList<ArrayList<IDSGraphNode>>> buildings;

    private static final int RANGE_MIN = 0;
    private static final int RANGE_MAX = 1;
    private static final int MIN_COST = 1;
    private static final int MAX_COST = 10;
    private static final int COST_UPPER_BOUND = MAX_COST - MIN_COST;
    private static final int STORY_COST_BOOST = 3;
    private static final int BUILDING_COST_BOOST = 5;


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
                    IDSGraphNode newState = new IDSGraphNode(building, story, device, 0, null);
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

        // connect buildings and stories
        for(int i = 0; i < this.buildings.size(); i++){
            ArrayList<ArrayList<IDSGraphNode>> building = this.buildings.get(i);

            // connect stories of building i
            for(int j = 0; j < building.size() - 1; j++){
                IDSGraphNode storyNode = building.get(j).get(0);
                IDSGraphNode nextStoryNode = building.get(j + 1).get(0);
                int cost = MIN_COST + rand.nextInt(COST_UPPER_BOUND) + STORY_COST_BOOST;
                IDSGraphEdge newEdge = new IDSGraphEdge(storyNode, nextStoryNode, cost);

                this.transitions.get(storyNode).add(newEdge);
                this.transitions.get(nextStoryNode).add(newEdge);
            }

            IDSGraphNode buildingConnectionNode = building.get(0).get(0);

            // connect building i to all other buildings
            for(int j = 0; j < this.buildings.size(); j++){
                if(i != j){
                    IDSGraphNode nextConnectionNode = this.buildings.get(j).get(0).get(0);
                    int cost = MIN_COST + rand.nextInt(COST_UPPER_BOUND) + BUILDING_COST_BOOST;
                    IDSGraphEdge newEdge = new IDSGraphEdge(buildingConnectionNode, nextConnectionNode, cost);

                    this.transitions.get(buildingConnectionNode).add(newEdge);
                }
            }
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

        this.validGoals = new boolean[this.goals.size()];
        this.validGoalsIndexes = new int[this.goals.size()];
        this.setAllGoalsValid();

        this.createInitialState();
    }


    public void setupTestIDS(){
        IDSGraphNode a1 = new IDSGraphNode(0, 0, 0, 0, null);
        IDSGraphNode a2 = new IDSGraphNode(0, 0, 1, 0, null);
        IDSGraphNode a3 = new IDSGraphNode(0, 0, 2, 0, null);
        IDSGraphNode a4 = new IDSGraphNode(0, 0, 3, 0, null);

        a1.setName("a1");
        a2.setName("a2");
        a3.setName("a3");
        a4.setName("a4");


        IDSGraphNode b1 = new IDSGraphNode(0, 1, 0, 0, null);
        IDSGraphNode b2 = new IDSGraphNode(0, 1, 1, 0, null);
        IDSGraphNode b3 = new IDSGraphNode(0, 1, 2, 0, null);
        IDSGraphNode b4 = new IDSGraphNode(0, 1, 3, 0, null);

        b1.setName("b1");
        b2.setName("b2");
        b3.setName("b3");
        b4.setName("b4");


        IDSGraphNode c1 = new IDSGraphNode(0, 2, 0, 0, null);
        IDSGraphNode c2 = new IDSGraphNode(0, 2, 1, 0, null);
        IDSGraphNode c3 = new IDSGraphNode(0, 2, 2, 0, null);
        IDSGraphNode c4 = new IDSGraphNode(0, 2, 3, 0, null);

        c1.setName("c1");
        c2.setName("c2");
        c3.setName("c3");
        c4.setName("c4");


        IDSGraphEdge a1a2 = new IDSGraphEdge(a1, a2, 5);
        IDSGraphEdge a1a3 = new IDSGraphEdge(a1, a3, 5);
        IDSGraphEdge a1a4 = new IDSGraphEdge(a1, a4, 5);
        IDSGraphEdge a2a3 = new IDSGraphEdge(a2, a3, 5);
        IDSGraphEdge a2a4 = new IDSGraphEdge(a2, a4, 5);
        IDSGraphEdge a3a4 = new IDSGraphEdge(a3, a4, 5);

        IDSGraphEdge a1b1 = new IDSGraphEdge(a1, b1, 10);


        IDSGraphEdge b1b2 = new IDSGraphEdge(b1, b2, 5);
        IDSGraphEdge b1b3 = new IDSGraphEdge(b1, b3, 5);
        IDSGraphEdge b1b4 = new IDSGraphEdge(b1, b4, 5);
        IDSGraphEdge b2b3 = new IDSGraphEdge(b2, b3, 5);
        IDSGraphEdge b2b4 = new IDSGraphEdge(b2, b4, 5);
        IDSGraphEdge b3b4 = new IDSGraphEdge(b3, b4, 5);

        IDSGraphEdge b1c1 = new IDSGraphEdge(b1, c1, 10);


        IDSGraphEdge c1c2 = new IDSGraphEdge(c1, c2, 1);
        IDSGraphEdge c1c3 = new IDSGraphEdge(c1, c3, 5);
        IDSGraphEdge c1c4 = new IDSGraphEdge(c1, c4, 5);
        IDSGraphEdge c2c3 = new IDSGraphEdge(c2, c3, 1);
        IDSGraphEdge c2c4 = new IDSGraphEdge(c2, c4, 5);
        IDSGraphEdge c3c4 = new IDSGraphEdge(c3, c4, 5);


        this.transitions.put(a1, new ArrayList<>(Arrays.asList(a1a2, a1a3, a1a4, a1b1)));
        this.transitions.put(a2, new ArrayList<>(Arrays.asList(a1a2, a2a3, a2a4)));
        this.transitions.put(a3, new ArrayList<>(Arrays.asList(a2a3, a1a3, a3a4)));
        this.transitions.put(a4, new ArrayList<>(Arrays.asList(a3a4, a2a4, a1a4)));

        this.transitions.put(b1, new ArrayList<>(Arrays.asList(b1b2, b1b3, b1b4, b1c1)));
        this.transitions.put(b2, new ArrayList<>(Arrays.asList(b1b2, b2b3, b2b4)));
        this.transitions.put(b3, new ArrayList<>(Arrays.asList(b2b3, b1b3, b3b4)));
        this.transitions.put(b4, new ArrayList<>(Arrays.asList(b3b4, b2b4, b1b4)));

        this.transitions.put(c1, new ArrayList<>(Arrays.asList(c1c2, c1c3, c1c4)));
        this.transitions.put(c2, new ArrayList<>(Arrays.asList(c1c2, c2c3, c2c4)));
        this.transitions.put(c3, new ArrayList<>(Arrays.asList(c2c3, c1c3, c3c4)));
        this.transitions.put(c4, new ArrayList<>(Arrays.asList(c3c4, c2c4, c1c4)));

        this.nodes.put(a1.hashCode(), a1);
        this.nodes.put(a2.hashCode(), a2);
        this.nodes.put(a3.hashCode(), a3);
        this.nodes.put(a4.hashCode(), a4);

        this.nodes.put(b1.hashCode(), b1);
        this.nodes.put(b2.hashCode(), b2);
        this.nodes.put(b3.hashCode(), b3);
        this.nodes.put(b4.hashCode(), b4);

        this.nodes.put(c1.hashCode(), c1);
        this.nodes.put(c2.hashCode(), c2);
        this.nodes.put(c3.hashCode(), c3);
        this.nodes.put(c4.hashCode(), c4);

        ArrayList<IDSGraphNode> s1 = new ArrayList<>(Arrays.asList(a1, a2, a3, a4));
        ArrayList<IDSGraphNode> s2 = new ArrayList<>(Arrays.asList(b1, b2, b3, b4));
        ArrayList<IDSGraphNode> s3 = new ArrayList<>(Arrays.asList(c1, c2, c3, c4));

        this.buildings.put(0, new ArrayList<>(Arrays.asList(s1, s2, s3)));

        c3.setGoal(true);
        this.goals.add(c3);

        b4.setGoal(true);
        this.goals.add(b4);

        this.validGoals = new boolean[this.goals.size()];
        this.validGoalsIndexes = new int[this.goals.size()];
        this.setAllGoalsValid();

        this.initialState = a3;

    }


    public void printDomain(){
        for(int i = 0; i < this.buildings.size(); i++){
            ArrayList<ArrayList<IDSGraphNode>> building = this.buildings.get(i);
            for(int j = 0; j < building.size(); j++){
                ArrayList<IDSGraphNode> story = building.get(j);
                for(int k = 0; k < story.size(); k++){
                    System.out.println(story.get(k).dumpState());
                    for(IDSGraphEdge edge : this.transitions.get(story.get(k)))
                        System.out.println(edge.dumpEdge());
                }
            }
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
        return true;
    }

    @Override
    protected PackedElement getNthGoalFromAllGoals(int goalIndex) {
        return new PackedElement(this.goals.get(goalIndex).hashCode());
    }

    @Override
    protected List<PackedElement> getAllGoalsInternal() {
        return null;
    }

    @Override
    public int totalGoalsCount() {
        return this.goals.size();
    }

    @Override
    protected boolean stateIsOneOfValidGoals(SearchState s) {
        if(this.goals.contains(s))
            return this.validGoals[this.goals.indexOf(s)];

        return false;
    }

    @Override
    public void setValidityOfOnlyGoal(int goal) {
        this.setAllGoalsInvalid();
        this.setGoalValid(goal);
        IDSDomain.currGoal = goals.get(goal);
    }

    @Override
    public int getGoalIndexForStateIfValid(SearchState s) {
        return this.goals.indexOf(s);
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
            this.logger.error("applying bad operator");
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
        return Integer.MAX_VALUE;
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

    public int getNumOfNodes() {
        int sum = 0;
        for (int i = 0; i < this.buildings.size(); i++){
            ArrayList<ArrayList<IDSGraphNode>> building = this.buildings.get(i);
            for(int j = 0; j < building.size(); j++){
                sum += building.get(j).size();
            }
        }

        return sum;
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

            // if there is no currGoal something went wrong - but i don't want to crash
            if(IDSDomain.currGoal == null){
                System.out.println("no currGoal - h set to 0");
                return 0;
            }

            // else - calculate the h according to the current goal
            int goalBuilding = IDSDomain.currGoal.building;
            int goalStory = IDSDomain.currGoal.story;

            // if goal is in the same building
            if(goalBuilding == this.building) {

                // the h is at least the cost of traveling stories times the difference between the stories
                return Math.abs(goalStory - this.story) * STORY_COST_BOOST;
            }

            // the goal is in a different building - need to get to story 0, go to the other building
            // and go to the goal's story
            return (goalStory + this.story) * STORY_COST_BOOST + BUILDING_COST_BOOST;
        }

        @Override
        public double getD() {
            return this.h;
        }

        @Override
        public String dumpState() {
            return this.name + " is goal = " + this.isGoal;
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

        public String getName(){
            return this.name;
        }

        public void setName(String name){
            this.name = name;
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

        public String dumpEdge(){
            return "a = " + a.getName() + " b = " + b.getName();
        }
    }










}

