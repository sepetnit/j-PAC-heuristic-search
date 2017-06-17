/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.cs4j.core.algorithms.weighted;

import jdk.nashorn.internal.ir.annotations.Ignore;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cs4j.core.*;
import org.cs4j.core.algorithms.auxiliary.SearchQueueElementImpl;
import org.cs4j.core.algorithms.auxiliary.SearchResultImpl;
import org.cs4j.core.collections.*;
import org.cs4j.core.domains.GridPathFinding;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * A* Search and Weighted A* Search
 *
 * @author Matthew Hatem
 *
 * (Edited by Vitali Sepetnitsky)
 */
public class WAStar extends GenericSearchAlgorithm {

    private final Logger logger = LogManager.getLogger(WAStar.class);


    protected static final Map<String, Class> WAStarPossibleParameters;

    // Declare the parameters that can be tuned before running the search
    static
    {
        WAStarPossibleParameters = new HashMap<>();
        WAStar.WAStarPossibleParameters.put("weight", Double.class);
        WAStar.WAStarPossibleParameters.put("reopen", Boolean.class);
        WAStar.WAStarPossibleParameters.put("max-cost", Double.class);
        WAStar.WAStarPossibleParameters.put("bpmx", Boolean.class);
        WAStar.WAStarPossibleParameters.put("store-best-costs", Boolean.class);
        WAStar.WAStarPossibleParameters.put("use-best-costs", Boolean.class);
    }

    // The domain for the search
    protected SearchDomain domain;
    // Open list (frontier)
    protected SearchQueue<Node> open;
    // private BinHeapF<Node> openF;
    // Closed list (seen states)
    protected Map<PackedElement, Node> closed;

    // Used for K-Goal Search:
    // Shortest path between two states - recorded between searches:
    // Each state, s, is mapped to state, s' iff the shortest path between
    // s and s' was already found
    // TODO:
    public Map<PackedElement, Map<PackedElement, Double>> bestCosts;

    // TODO ...
    protected HeapType heapType;

    public enum HeapType {BIN, BUCKET}

    // For weighted A*
    protected double weight;
    // Whether to perform reopening of states
    protected boolean reopen;

    protected double maxCost;

    protected boolean useBPMX;

    // TODO : BestCosts ...
    private boolean storeBestCosts;
    private boolean useBestCosts;

    protected int FR;

    protected SearchResultImpl result;

    /**
     * Sets the default values for the relevant fields of the algorithm
     */
    private void _initDefaultValues() {
        // Default values
        this.weight = 1.0;
        this.reopen = true;
        this.maxCost = Double.MAX_VALUE;
        this.useBPMX = false;
        this.FR = Integer.MAX_VALUE;
        this.storeBestCosts = false;
        this.useBestCosts = false;
        this.bestCosts = null;
    }


    /**
     * A constructor
     *
     * @param heapType the type of heap to use (BIN | BUCKET)
     *
     */
    protected WAStar(HeapType heapType) {
        this.heapType = heapType;
        this._initDefaultValues();
    }

    /**
     * A default constructor of the class (weight of 1.0, binary heap and AR)
     *
     */
    public WAStar() {
        this(HeapType.BIN);
    }

    @Override
    public String getName() {
        return "wastar";
    }

    /**
     * Creates a heap according to the required type (Builder design pattern)
     *
     * @param heapType Type of the required heap (choose from the available types)
     * @param size Initial size of the heap
     *
     * NOTE: In case of unknown type, null is returned (no exception is thrown)
     * @return The created heap
     */
    protected SearchQueue<Node> buildHeap(HeapType heapType, int size) {
        SearchQueue<Node> heap = null;
        switch (heapType) {
            case BUCKET:
                // heap = new BucketHeap<>(size, QID);
                break;
            case BIN:
                heap = new BinHeap<>(new NodeComparator(), 0);
                break;
        }
        return heap;
    }

    protected void _initDataStructures(SearchDomain domain) {
        this.domain = domain;
        this.open = new BinHeap<>(new NodeComparator(), 0);
        // this.openF = new BinHeapF<>(1,domain);
        // this.open = buildHeap(heapType, 100);
        this.closed = new TreeMap<>();
        // this.closed = new HashMap<>();
    }

    private boolean assureCorrectInitialization() {
        if ((this.useBestCosts || this.storeBestCosts) && this.bestCosts == null) {
            // When we want calculating best costs using previous perfect heuristic calculation we
            // need the relevant data structure to be initialized
            this.logger.warn("Can't use best costs since their data structure is not " +
                    "initialized");
            // We don't want to fail here
        }
        return true;
    }

    @Override
    public boolean concreteImproveStateHValue(SearchState s,
                                              MultipleGoalsSearchDomain d) {
        // get the single goal
        // check the perfect heuristic between the state and the goal
        // we should take the state, CHECK IF THE DOMAIN HAS A SINGLE GOAL AND
        // RETRIEVE IT

        if (d.validGoalsCount() == 1) { // TODO: May be different!
            PackedElement goalElement = d.getFirstValidGoal();
            //System.out.println("GOOOOOOOOOOOOOOOOAL: " + domain.unpack(goalElement).dumpStateShort());
            Map<PackedElement, Double> currentElementCosts =
                    this.bestCosts.get(domain.pack(s));
            if (currentElementCosts != null) {
                Double perfectH = currentElementCosts.get(goalElement);
                if (perfectH != null) {

                    if (perfectH != s.getH()) {
                        //System.out.println("WIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIN");
                        //System.out.println("Perfect: " + perfectH + " Real: " + s.getH());
                    }
                    if (perfectH < s.getH()) {
                        System.out.println("PERFECT :" + perfectH + " REAL: " + s.getH());
                        System.out.println("eeeeeeeerrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrror");
                    }


                    // Update the h value with the found one
                    s.setH(perfectH);

                    return true;
                } else {
                   // this.logger.info("Perfect H wasn't found");
                }
            }
        }
        return false;
    }


    @Override
    public SearchResult concreteSearch(MultipleGoalsSearchDomain domain) {
        return this.search(domain);
    }

    @Override
    public SearchResult search(SearchDomain domain) {
        // Initialize all the data structures required for the search
        this._initDataStructures(domain);
        this.assureCorrectInitialization();
        System.out.println("store: " + this.storeBestCosts + "; use: " + this.useBestCosts);


        Node goal = null;

        this.result = new SearchResultImpl();

        this.result.startTimer();

        // Let's instantiate the initial state
        SearchState currentState = domain.initialState();

        System.out.println("Initial state : " + currentState.dumpStateShort());
        // Create a graph node from this state
        Node initNode = new Node(currentState);
        PackedElement initialPacked = initNode.packed;

        // And add it to the frontier
        this._addNode(initNode);

        try {
            // Loop over the frontier
            while (!this.open.isEmpty() &&
                    result.getGenerated() < this.domain.maxGeneratedSize() &&
                    result.checkMinTimeOut()) {
                // Take the first state (still don't remove it)
                // Node currentNode = this.open.poll();
                Node currentNode = _selectNode();

                ///////////////////////////////////////////////////////////////

                if (this.storeBestCosts) {
                    // Store best cost if required
                    Map<PackedElement, Double> found =
                            this.bestCosts.computeIfAbsent(currentNode.packed, k -> new TreeMap<>());
                    // Store the perfect heuristic (even if a previous value is
                    // stored)
                    //System.out.println("Stored: " +
                    //        this.domain.unpack(currentNode.packed).dumpStateShort() + " " +
                    //        this.domain.unpack(initialPacked).dumpStateShort());
                    found.put(initialPacked, currentNode.getG());
                }

                ///////////////////////////////////////////////////////////////

                // Prune
                if (currentNode.getRf() >= this.maxCost) {
                    continue;
                }

                // Extract the state from the packed value of the node
                currentState = domain.unpack(currentNode.packed);

                if (((GridPathFinding.GridPathFindingState)currentState).agentLocation == 33967) {
                    System.out.println("dddddddddddd");
                }

                //System.out.println(currentState.dumpStateShort());
                // Check for goal condition
                if (domain.isGoal(currentState)) {
                    goal = currentNode;
                    break;
                }

                List<Pair<SearchState, Node>> children = new ArrayList<>();

                // Expand the current node
                ++result.expanded;
                // Stores parent h-cost (from path-max)
                double bestHValue = 0.0d;
                // First, let's generate all the children
                // Go over all the possible operators and apply them
                for (int i = 0; i < domain.getNumOperators(currentState); ++i) {
                    Operator op = domain.getOperator(currentState, i);
                    // Try to avoid loops - no need to generate parent
                    if (op.equals(currentNode.pop)) {
                        continue;
                    }
                    SearchState childState =
                            domain.applyOperator(currentState, op);

                    ////////////////////////////////////////////////////////////
                    // Update the H value of the state if required
                    ////////////////////////////////////////////////////////////

                    // First we need to check if there is a single goal that
                    // should be found during the current search

                    // domain.countValidStates(). getFirstValidState()
                    // check(currentstate, goal).cost == perfect heuristic
                    // hImprovable
                    Node childNode = new Node(childState, currentNode,
                            currentState, op, op.reverse(currentState));

                    if (this.useBestCosts) {
                        if (this.bestCosts.get(childNode.packed) != null) {
                            double previous = childNode.getH();
                            if (this.domain.improveStateHValue(childState,
                                    this)) {
                                childNode.setH(childState.getH());
                                /*
                                logger.info("Value improved: was {}, now {}",
                                        previous, childNode.getH());
                                        */
                            }
                        }
                    }

                    ////////////////////////////////////////////////////////////

                    // Here we actually generated a new state
                    ++result.generated;
/*                    if(result.getGenerated() % 1000 == 0){
                        DecimalFormat formatter = new DecimalFormat("#,###");
//                        System.out.println("[INFO] WA Generated:" + formatter.format(result.getGenerated()) + "\tTime:"+passed);
                        System.out.print("\r[INFO] WA Generated:" + formatter.format(result.getGenerated()));

                    }*/
                    // Perform only if BPMX is required
                    if (this.useBPMX) {
                        bestHValue = Math.max(bestHValue, childNode.h - op.getCost(childState, currentState));
                    }
                    children.add(new Pair<>(childState, childNode));
                }

                // Update the H Value of the parent in case of BPMX
                if (this.useBPMX) {
                    currentNode.h = Math.max(currentNode.h, bestHValue);
                    // Prune
                    if (currentNode.getRf() >= this.maxCost) {
                        continue;
                    }
                }

                // Go over all the possible operators and apply them
                for (Pair<SearchState, Node> currentChild : children) {
                    SearchState childState = currentChild.getKey();
                    Node childNode = currentChild.getValue();
                    double edgeCost = childNode.op.getCost(childState, currentState);

                    // Prune
                    if (childNode.getRf() >= this.maxCost) {
                        continue;
                    }
                    // Treat duplicates
                    boolean contains = true;
                    // Get the previous copy of this node (and extract it)
                    Node dupChildNode = this.closed.get(childNode.packed);
                    if (dupChildNode == null){
                        contains = false;
                    }
                    // contains = this.closed.containsKey(childNode.packed);
                    if (contains) {
                        // Count the duplicates
                        ++result.duplicates;

                        if (this.useBestCosts) {
                            dupChildNode.setH(childNode.getH());
                        }

                        // Propagate the H value to child (in case of BPMX)
                        if (this.useBPMX) {
                            dupChildNode.h = Math.max(dupChildNode.h, currentNode.h - edgeCost);
                        }

                        // Found a shorter path to the node
                        if (dupChildNode.g > childNode.g) {
                            // Check that the f actually decreases
                            if (dupChildNode.getWf() > childNode.getWf()) {
                                // Do nothing
                            } else {
                                if (this.domain.isCurrentHeuristicConsistent()) {
                                    assert false;
                                }
                                continue;
                            }
                            // In any case update the duplicate with the new values - we reached it via a shorter path
                            dupChildNode.g = childNode.g;
                            dupChildNode.op = childNode.op;
                            dupChildNode.pop = childNode.pop;
                            dupChildNode.parent = childNode.parent;

                            // if dupChildNode is in open, update it there too
                            if (dupChildNode.getIndex(this.open.getKey()) != -1) {
                                ++result.opupdated;
                                this.open.update(dupChildNode);
                                // this.openF.updateF(dupChildNode, dupF);
                            }
                            // Otherwise, consider to reopen dupChildNode
                            else {
                                // Return to OPEN list only if reopening is allowed
                                if (this.reopen) {
                                    ++result.reopened;
                                    this._addNode(dupChildNode);
                                }
                            }
                            // in any case, update closed to be bestChild
                            this.closed.put(dupChildNode.packed, dupChildNode);
                        } else {
                            // A shorter path has not been found, but let's update the node in open if its h increased
                            if (this.useBPMX) {
                                if (dupChildNode.getIndex(this.open.getKey()) != -1) {
                                    this.open.update(dupChildNode);
                                }
                            }
                        }
                        // Otherwise, the node is new (hasn't been reached yet)
                    } else {
                        // Propagate the H value to child (in case of BPMX)
                        if (this.useBPMX) {
                            childNode.h = Math.max(childNode.h, currentNode.h - edgeCost);
                        }
                        this._addNode(childNode);
                    }
                }
            }
        }
        catch(OutOfMemoryError e){
            this.logger.error("WAstar OutOfMemory :-( {}", e);
            this.logger.error("OutOfMemory WAstar on: {}, generated: {}",
                    this.domain.getClass().getSimpleName(), result.getGenerated());
        }

        result.stopTimer();
//        System.out.println("Generated:\t"+result.getGenerated());
//        System.out.println("closed Size:\t"+this.closed.size());
//        result.printArrCpuTimeMillis();

        // If a goal was found: update the solution
        if (goal != null) {
            System.out.print("\r");
            SearchResultImpl.SolutionImpl solution =
                    new SearchResultImpl.SolutionImpl(this.domain);
            List<Operator> path = new ArrayList<>();
            List<SearchState> statesPath = new ArrayList<>();
            // System.out.println("[INFO] Solved - Generating output path.");
            double cost = 0;

            SearchState currentPacked = domain.unpack(goal.packed);
            SearchState currentParentPacked = null;
            for (Node currentNode = goal;
                 currentNode != null;
                 currentNode = currentNode.parent, currentPacked = currentParentPacked) {
                // If op of current node is not null that means that p has a parent
//                System.out.println(currentPacked.dumpStateShort());
                if (currentNode.op != null) {
                    path.add(currentNode.op);
                    currentParentPacked = domain.unpack(currentNode.parent.packed);
                    cost += currentNode.op.getCost(currentPacked, currentParentPacked);
                }
                statesPath.add(domain.unpack(currentNode.packed));
            }
            // The actual size of the found path can be only lower the G value of the found goal
            assert cost <= goal.g;
            double roundedCost = new BigDecimal(cost).setScale(4, RoundingMode.HALF_DOWN).doubleValue();
            double roundedG = new BigDecimal(goal.g).setScale(4, RoundingMode.HALF_DOWN).doubleValue();
            if (roundedCost - roundedG < 0) {
                this.logger.info("Goal G is higher that the actual cost " +
                        "(G: {} Actual: {})", goal.g, cost);
            }

            Collections.reverse(path);
            solution.addOperators(path);

            Collections.reverse(statesPath);
            solution.addStates(statesPath);

            solution.setCost(cost);
            result.addSolution(solution);
        }

        return result;
    }

    /**
     *
     * @return chosen Node for expansion
     */
    protected Node _selectNode() {
        Node toReturn;
        toReturn = this.open.peek();
//        if(openF.getFminCount() < result.generated){
//            toReturn = this.openF.peek();
//        }
        open.remove(toReturn);
//        openF.remove(toReturn);
        return toReturn;
    }

    /**
     *
     * @param toAdd is the new node that should be added to open
     */
    protected void _addNode(Node toAdd) {
        this.open.add(toAdd);
//        this.openF.add(toAdd);
        // The nodes are ordered in the closed list by their packed values
        this.closed.put(toAdd.packed, toAdd);
    }

    @Override
    public Map<String, Class> getPossibleParameters() {
        return WAStar.WAStarPossibleParameters;
    }

    @Override
    public void setAdditionalParameter(String parameterName, String value) {
        switch (parameterName) {
            case "weight": {
                this.weight = Double.parseDouble(value);
                if (this.weight < 1.0d) {
                    this.logger.error("The weight must be >= 1.0");
                    throw new IllegalArgumentException();
                } else if (this.weight == 1.0d) {
                    this.logger.warn("Weight of 1.0 is equivalent to A*");
                }
                break;
            }
            case "reopen": {
                this.reopen = Boolean.parseBoolean(value);
                break;
            }
            case "bpmx": {
                this.useBPMX = Boolean.parseBoolean(value);
                if (this.useBPMX) {
                    this.logger.info("WAStar will be ran with BPMX");
                }
                break;
            }
            case "max-cost": {
                this.maxCost = Double.parseDouble(value);
                if (this.maxCost <= 0) {
                    this.logger.error("The maximum possible cost must be >= 0");
                    throw new IllegalArgumentException();
                }
                break;
            }
            case "store-best-costs": {
                this.storeBestCosts = Boolean.parseBoolean(value);
                if (this.storeBestCosts) {
                    // Initialize the list if required
                    if (this.bestCosts == null) {
                        this.bestCosts = new TreeMap<>();
                    }
                    this.logger.info("WAStar will store the cost of shortest paths between states");
                }
                break;
            }
            case "use-best-costs": {
                this.useBestCosts = Boolean.parseBoolean(value);
                if (this.useBestCosts) {
                    // Initialize the list if required
                    if (this.bestCosts == null) {
                        this.bestCosts = new TreeMap<>();
                    }
                    this.logger.info("WAStar will try to extract perfect heuristic values from " +
                            "previous searches");
                }
                break;
            }
            case "FR": {
                this.FR = Integer.parseInt(value);
                break;
            }
            default: {
                throw new UnsupportedOperationException();
            }
        }
    }

    // TODO: Implement reset function

    /**
     * The node class
     */
    protected final class Node extends SearchQueueElementImpl {
    	protected double g;
        protected double h;

        protected Operator op;
        protected Operator pop;

        protected Node parent;
        protected PackedElement packed;

//      private int[] secondaryIndex;

        protected Node(SearchState state,
                       Node parent,
                       SearchState parentState,
                       Operator op,
                       Operator pop) {
            // Size of key
            super(2);
            // TODO: Why?
//            this.secondaryIndex = new int[(heapType == HeapType.BUCKET) ? 2 : 1];
            double cost = (op != null) ? op.getCost(state, parentState) : 0;
            this.h = state.getH();
            // If each operation costs something, we should add the cost to the g value of the parent
            this.g = (parent != null) ? parent.g + cost : cost;

            // Parent node
            this.parent = parent;
            this.packed = WAStar.this.domain.pack(state);
            this.pop = pop;
            this.op = op;
        }

        /**
         * @return The value of the weighted evaluation function
         */
        public double getWf() {
            return this.g + (WAStar.this.weight * this.h);
        }

        /**
         * @return The value of the regular evaluation function
         */
        public double getRf() {
            return this.g + this.h;
        }

        /**
         * A constructor of the class that instantiates only the state
         *
         * @param state The state which this node represents
         */
        protected Node(SearchState state) {
            this(state, null, null, null, null);
        }

        @Override
        public double getF() {
            return this.g + this.h;
        }

        @Override
        public double getG() {
            return this.g;
        }

        @Override
        public double getDepth() {
            return 0;
        }

        @Override
        public double getH() {
            return this.h;
        }

        public double setH(double hValue) {
            double previousH = this.getH();
            this.h = hValue;
            return previousH;
        }

        @Override
        public double getD() {
            return 0;
        }

        @Override
        public double getHhat() {
            return 0;
        }

        @Override
        public double getDhat() {
            return 0;
        }

        @Override
        public SearchQueueElement getParent() {return this.parent;}
    }

    /**
     * The nodes comparator class
     */
    protected final class NodeComparator implements Comparator<Node> {

        @Override
        public int compare(final Node a, final Node b) {
            // First compare by wF (smaller is preferred), then by g (bigger is preferred)
            if (a.getWf() < b.getWf()) return -1;
            if (a.getWf() > b.getWf()) return 1;
            if (a.getG() > b.getG()) return -1;
            if (a.getG() < b.getG()) return 1;
            return 0;
        }
    }

}
