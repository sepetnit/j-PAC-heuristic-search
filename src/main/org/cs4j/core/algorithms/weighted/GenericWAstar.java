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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cs4j.core.*;
import org.cs4j.core.algorithms.auxiliary.GenericNode;
import org.cs4j.core.collections.BinHeap;
import org.cs4j.core.collections.PackedElement;
import org.cs4j.core.collections.Pair;
import org.cs4j.core.collections.SearchQueue;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * A* Search and Weighted A* Search
 *
 * @author Matthew Hatem
 *         <p>
 *         (Edited by Vitali Sepetnitsky)
 */

public abstract class GenericWAstar<N extends GenericNode<N>, C extends Comparator<N>> extends
        GenericSearchAlgorithm {

    protected abstract C getComparator();

    protected abstract N getNode(SearchState s);

    protected abstract N getNode(SearchState state,
                                 N parent,
                                 SearchState parentState,
                                 Operator op,
                                 Operator pop);

    private final Logger logger = LogManager.getLogger(GenericWAstar.class);


    protected static final Map<String, Class> WAStarPossibleParameters;

    // Declare the parameters that can be tuned before running the search
    static {
        WAStarPossibleParameters = new HashMap<>();
        GenericWAstar.WAStarPossibleParameters.put("weight", Double.class);
        GenericWAstar.WAStarPossibleParameters.put("reopen", Boolean.class);
        GenericWAstar.WAStarPossibleParameters.put("max-cost", Double.class);
        GenericWAstar.WAStarPossibleParameters.put("bpmx", Boolean.class);
        GenericWAstar.WAStarPossibleParameters.put("store-best-costs", Boolean.class);
        GenericWAstar.WAStarPossibleParameters.put("use-best-costs", Boolean.class);
    }

    // Open list (frontier)
    protected SearchQueue<N> open;
    // private BinHeapF<LazyAstarNode> openF;
    // Closed list (seen states)
    protected Map<PackedElement, N> closed;

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

    //protected SearchResultImpl result;

    public Map<PackedElement, N> getClosed() {
        return this.closed;
    }

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
     */
    protected GenericWAstar(HeapType heapType) {
        this.heapType = heapType;
        this._initDefaultValues();
    }

    /**
     * A default constructor of the class (weight of 1.0, binary heap and AR)
     */
    public GenericWAstar() {
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
     * @param size     Initial size of the heap
     *                 <p>
     *                 NOTE: In case of unknown type, null is returned (no exception is thrown)
     * @return The created heap
     */
    protected SearchQueue<N> buildHeap(HeapType heapType, int size) {
        SearchQueue<N> heap = null;
        switch (heapType) {
            case BUCKET:
                // heap = new BucketHeap<>(size, QID);
                break;
            case BIN:
                heap = new BinHeap<>(this.getComparator(), 0);
                break;
        }
        return heap;
    }

    protected void _initDataStructures(SearchDomain domain) {
        this.open = new BinHeap<>(this.getComparator(), 0);
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

            Map<PackedElement, Double> currentElementCosts = this.bestCosts.get(s.pack());

            if (currentElementCosts != null) {
                Double perfectH = currentElementCosts.get(goalElement);
                if (perfectH != null) {

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
    public SearchResultImpl concreteSearch(MultipleGoalsSearchDomain domain) {
        return this.search(domain);
    }

    // By default, reverse the paths
    protected SearchResultImpl.SolutionImpl getSolution(SearchDomain domain, N goal) {
        System.out.println("ddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd");
        return this.getSolution(domain, goal, true);
    }

    protected SearchResultImpl.SolutionImpl getSolution(SearchDomain domain, N goal, boolean reverse) {
        SearchResultImpl.SolutionImpl solution =
                new SearchResultImpl.SolutionImpl();
        double cost = 0;
        List<SearchState> statesPath = new ArrayList<>();
        List<Operator> operatorsPath = new ArrayList<>();
        SearchState currentPacked = domain.unpackLite(goal.getPacked());
        SearchState currentParentPacked = null;
        for (N currentNode = goal;
             currentNode != null;
             currentNode = currentNode.getParent(), currentPacked = currentParentPacked) {
            // If op of current node is not null that means that p has a parent
            if (currentNode.getOp() != null) {
                operatorsPath.add(currentNode.getOp());
                currentParentPacked = domain.unpackLite(currentNode.getParent().getPacked());
                cost += currentNode.getOp().getCost(currentPacked, currentParentPacked);
            }
            statesPath.add(domain.unpackLite(currentNode.getPacked()));
        }

        ///////////////////////////////////////////////////////////////////////////////////////////
        // The actual size of the found path can be only lower the G value of the found goal
        assert cost <= goal.getG();
        double roundedCost = new BigDecimal(cost).setScale(4, RoundingMode.HALF_DOWN).doubleValue();
        double roundedG = new BigDecimal(goal.getG()).setScale(4, RoundingMode.HALF_DOWN)
                .doubleValue();
        if (roundedCost - roundedG < 0) {
            this.logger.info("Goal G is higher that the actual cost " +
                    "(G: {} Actual: {})", goal.getG(), cost);
        }
        ///////////////////////////////////////////////////////////////////////////////////////////

        solution.addStates(statesPath);
        solution.addOperators(operatorsPath);
        solution.setCost(cost);

        // Reverse the paths (make them : start -> ... -> goal)
        if (reverse) {
            solution.reverseAll();
        }

        return solution;
    }

    // By default, replace duplicate by found node if nodeF < dupF
    protected boolean shouldReplaceNode(SearchDomain domain,
                                        N existingNode,
                                        N newFoundNode) {
        return newFoundNode.getF() < existingNode.getF();
    }

    protected abstract boolean shouldStop(SearchResultImpl result);

    /**
     * The function generates the initial state, creates the initial node and
     * adds it to the open list
     *
     * @return The generated initial state
     */
    protected SearchState generateInitialNode(SearchDomain domain) {
        SearchState toReturn = domain.initialState();
        System.out.println("Initial state : " + toReturn.dumpStateShort());
        // Create a graph node from this state
        N initNode = this.getNode(toReturn);
        // And add it to the frontier
        this._addNode(initNode);
        return toReturn;
    }

    protected boolean isGoal(SearchDomain domain,
                             N node,
                             SearchState state) {
        return domain.isGoal(state);
    }

    @Override
    public SearchResultImpl search(SearchDomain domain) {
        // Initialize all the data structures required for the search
        this._initDataStructures(domain);
        this.assureCorrectInitialization();
        System.out.println("store: " + this.storeBestCosts + "; use: " + this.useBestCosts);

        SearchResultImpl result = new SearchResultImpl();

        result.startTimer();

        // Let's instantiate the initial state and add its node to the open list
        SearchState currentState = this.generateInitialNode(domain);
        PackedElement initialPacked = currentState.pack();

        try {
            // Loop over the frontier
            while (!this.open.isEmpty() &&
                    result.getGenerated() < domain.maxGeneratedSize() &&
                    result.checkMinTimeOut()) {
                // Take the first state (still don't remove it)
                // LazyAstarNode currentNode = this.open.poll();
                N currentNode = _selectNode();

                ///////////////////////////////////////////////////////////////

                if (this.storeBestCosts) {
                    // Store best cost if required
                    Map<PackedElement, Double> found =
                            this.bestCosts.computeIfAbsent(currentNode.getPacked(),
                                    k -> new TreeMap<>());
                    // Store the perfect heuristic (even if a previous value is
                    // stored)
                    //System.out.println("Stored: " +
                    //        domain.unpack(currentNode.packed).dumpStateShort() + " " +
                    //        domain.unpack(initialPacked).dumpStateShort());
                    found.put(initialPacked, currentNode.getG());
                }

                ///////////////////////////////////////////////////////////////

                // Prune
                if (currentNode.getRf() >= this.maxCost) {
                    continue;
                }

                // Extract the state from the packed value of the node
                currentState = domain.unpack(currentNode.getPacked());

                //System.out.println(currentState.dumpStateShort());
                // Check for goal condition
                if (this.isGoal(domain, currentNode, currentState)) {
                    // If a goal was found: update the solution
                    SearchResultImpl.Solution sol = this.getSolution(domain, currentNode);
                    result.addSolution(sol);
                    logger.error("EXPANDEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEED: " + result
                            .getExpanded());
                    logger.error("LENNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNN: " + sol.getLength());
                    if (this.shouldStop(result)) {
                        break;
                    }
                }

                List<Pair<SearchState, N>> children = new ArrayList<>();

                // Expand the current node
                ++result.expanded;
                // Stores parent h-cost (from path-max)
                double bestHValue = 0.0d;
                // First, let's generate all the children
                // Go over all the possible operators and apply them
                for (int i = 0; i < domain.getNumOperators(currentState); ++i) {
                    Operator op = domain.getOperator(currentState, i);
                    // Try to avoid loops - no need to generate parent
                    if (op.equals(currentNode.getPop())) {
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
                    N childNode = this.getNode(childState, currentNode,
                            currentState, op, op.reverse(currentState));

                    if (this.useBestCosts) {
                        if (this.bestCosts.get(childNode.getPacked()) != null) {
                            //double previous = childNode.getH();
                            if (domain.improveStateHValue(childState,
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
//                        System.out.println("[INFO] WA Generated:" + formatter.format(result
.getGenerated()) + "\tTime:"+passed);
                        System.out.print("\r[INFO] WA Generated:" + formatter.format(result
                        .getGenerated()));

                    }*/
                    // Perform only if BPMX is required
                    if (this.useBPMX) {
                        bestHValue = Math.max(bestHValue, childNode.getH() - op.getCost(childState,
                                currentState));
                    }
                    children.add(new Pair<>(childState, childNode));
                }

                // Update the H Value of the parent in case of BPMX
                if (this.useBPMX) {
                    currentNode.setH(Math.max(currentNode.getH(), bestHValue));
                    // Prune
                    if (currentNode.getRf() >= this.maxCost) {
                        continue;
                    }
                }

                // Go over all the possible operators and apply them
                for (Pair<SearchState, N> currentChild : children) {
                    SearchState childState = currentChild.getKey();
                    N childNode = currentChild.getValue();
                    double edgeCost = childNode.getOp().getCost(childState, currentState);

                    // Prune
                    if (childNode.getRf() >= this.maxCost) {
                        continue;
                    }
                    // Treat duplicates
                    boolean contains = true;
                    // Get the previous copy of this node (and extract it)
                    N dupChildNode = this.closed.get(childNode.getPacked());
                    if (dupChildNode == null) {
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
                            dupChildNode.setH(Math.max(dupChildNode.getH(),
                                    currentNode.getH() - edgeCost));
                        }

                        // Found a shorter path to the node
                        if (dupChildNode.getG() > childNode.getG()) {
                            // In case the node should be actually replaced in the closed list
                            if (shouldReplaceNode(domain, dupChildNode, childNode)) {
                                // In any case update the duplicate with the new values - we reached
                                // it via a shorter path
                                dupChildNode.copyFromDuplicateNode(childNode);

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
                                this.updateClosed(dupChildNode.getPacked(), dupChildNode);
                            }
                        } else {
                            // A shorter path has not been found, but let's update the node in
                            // open if its h increased
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
                            childNode.setH(Math.max(childNode.getH(),
                                    currentNode.getH() - edgeCost));
                        }
                        this._addNode(childNode);
                    }
                }
            }
        } catch (OutOfMemoryError e) {
            this.logger.error("WAstar OutOfMemory :-( {}", e);
            this.logger.error("OutOfMemory WAstar on: {}, generated: {}",
                    domain.getClass().getSimpleName(), result.getGenerated());
        }

        result.stopTimer();
//        System.out.println("Generated:\t"+result.getGenerated());
//        System.out.println("closed Size:\t"+this.closed.size());
//        result.printArrCpuTimeMillis();

        return result;
    }

    /**
     * @return chosen LazyAstarNode for expansion
     */
    protected N _selectNode() {
        N toReturn;
        toReturn = this.open.peek();
//        if(openF.getFminCount() < result.generated){
//            toReturn = this.openF.peek();
//        }
        open.remove(toReturn);
//        openF.remove(toReturn);
        return toReturn;
    }

    /**
     * @param toAdd is the new node that should be added to open
     */
    protected void _addNode(N toAdd) {
        this.open.add(toAdd);
//        this.openF.add(toAdd);
        // The nodes are ordered in the closed list by their packed values
        this.updateClosed(toAdd.getPacked(), toAdd);
    }


    protected void updateClosed(PackedElement packed, N node) {
        this.closed.put(packed, node);
    }

    @Override
    public Map<String, Class> getPossibleParameters() {
        return GenericWAstar.WAStarPossibleParameters;
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
                    this.logger.info("GenericWAstar will be ran with BPMX");
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
                    this.logger.info("GenericWAstar will store the cost of shortest paths between " +
                            "states");
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
                    this.logger.info("GenericWAstar will try to extract perfect heuristic values " +
                            "from " +
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

}
