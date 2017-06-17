package org.cs4j.core.algorithms.familiar;

import org.cs4j.core.*;
import org.cs4j.core.algorithms.auxiliary.SearchQueueElementImpl;
import org.cs4j.core.algorithms.auxiliary.SearchResultImpl;
import org.cs4j.core.collections.PackedElement;
import org.cs4j.core.collections.SearchQueueElement;
import org.cs4j.core.collections.GH_heap;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Created by Daniel on 21/12/2015.
 */
public class DP  extends GenericSearchAlgorithm {

    private boolean debug = false;

    private static final int open_ID = 1;

    private static final Map<String, Class> DPPossibleParameters;

    // Declare the parameters that can be tuned before running the search
    static {
        DPPossibleParameters = new HashMap<>();
        DP.DPPossibleParameters.put("weight", Double.class);
        DP.DPPossibleParameters.put("reopen", Boolean.class);
        DP.DPPossibleParameters.put("FR", Integer.class);
        DP.DPPossibleParameters.put("optimalSolution", Double.class);
    }

    // The domain for the search
    private SearchDomain domain;
    // Open list (frontier)
//    private BinHeapF<Node> open;
    private GH_heap<Node> open;//gh_heap
    //    private BinHeapF<Node> openF;
    // Closed list (seen states)
    private TreeMap<PackedElement, Node> closed;
    //    private Map<PackedElement, Node> closed;
    //the result to return
    private SearchResultImpl result;

    // For Dynamic Potential Bound
    protected double weight;
    // Whether to perform reopening of states
    private boolean reopen;
    //when to empty Focal
    private int FR;
    private boolean useFR;
    private double extractedFmin = 0;
    private NodeComparator NC;

    private double optimalSolution;
    private String name;
    private Node goal = null;
    private Node bestGoalNode = null;

    private boolean useD;
    private boolean isFocalized;
    private boolean useWApriority;

    /**
     * Sets the default values for the relevant fields of the algorithm
     */
    private void _initDefaultValues() {
        // Default values
        this.weight = 1.0;
        this.reopen = true;
        this.FR = Integer.MAX_VALUE;
        this.useFR = false;
    }


    /**
     * A default constructor of the class
     *
     * @param name        the name of the algorithm
     * @param useD        for the algo
     * @param isFocalized if the algoritm is focalized or not
     * @param useWApriority if we want the priority to the one of WA* (works also on D)
     */
    public DP(String name, boolean useD, boolean isFocalized, boolean useWApriority) {
        this.name = name;

        this.useD = useD;
        this.isFocalized = isFocalized;
        this.useWApriority = useWApriority;
        this._initDefaultValues();
    }

    @Override
    public String getName() {
        if(this.useFR) return this.name+"#"+this.FR;
        else return this.name;
    }

    private void _initDataStructures(SearchDomain domain) {
        this.domain = domain;
        this.result = new SearchResultImpl();
        this.NC = new NodeComparator();
//        this.open = new BinHeapF<>(open_ID,domain,this.NC);
        this.open = new GH_heap<>(weight, open_ID, this.domain.initialState().getH(), this.domain.initialState().getD(), result, useFR, useD, isFocalized, useWApriority);//no oracle
        //for cases where we want to set the fmin start
        if (this.optimalSolution != 0) {
            this.open.setOptimal(optimalSolution);
        }
//        this.openF = new BinHeapF<>(openF_ID,domain);
        //this.open = buildHeap(heapType, 100);
        this.closed = new TreeMap<>();
//        this.closed = new HashMap<>();
    }

    @Override
    public SearchResult search(SearchDomain domain) {
        // Initialize all the data structures required for the search
        this._initDataStructures(domain);

        result.startTimer();
        result.setExtras("numOfGoalsFound", 0 + "");

        // Let's instantiate the initial state
        SearchState currentState = domain.initialState();

        // Create a graph node from this state
        Node initNode = new Node(currentState);
        bestGoalNode=null;

        // And add it to the frontier
        _addNode(initNode);

        try {
            while (checkTermination()) {

                // Take the first state (still don't remove it)
                Node currentNode = _selectNode();

                // Extract the state from the packed value of the node
                currentState = unpackDomain(currentNode);

                // Check for goal condition
                if (checkIfGoal(currentState, currentNode))
                    break;

                // Expand the current node
                ++result.expanded;

                // Go over all the possible operators and apply them
                for (int i = 0; i < getNumOperators(currentState); ++i) {
                    Operator op = getOperator(currentState, i);
                    // Try to avoid loops
                    if (op.equals(currentNode.pop)) {
                        continue;
                    }
                    // Here we actually generate a new state
                    SearchState childState = applyOperator(currentState, op);
                    Node childNode = new Node(childState, currentNode, currentState, op, op.reverse(currentState));

                    ++result.generated;
                    // Treat duplicates
                    if (checkClosedContains(childNode)) {
                        _duplicateNode(childNode);
                    } else {// Otherwise, the node is new (hasn't been reached yet)
                        _addNode(childNode);
                    }
                }
                _removeNode(currentNode);
            }
        } catch (OutOfMemoryError e) {
            System.out.println("[INFO] DP OutOfMemory :-( " + e);
            System.out.println("[INFO] OutOfMemory DP on:" + domain.getClass().getSimpleName() + " generated:" + result.getGenerated());
        }

        result.stopTimer();
        // If a goal was found: update the solution
        handleGoal();

        return result;
    }

    private boolean checkTermination() {
//        return !this.open.isEmpty() && result.getGenerated() < domain.maxGeneratedSize() && result.checkMinTimeOut();
        return result.getGenerated() < domain.maxGeneratedSize();
    }

    private SearchState applyOperator(SearchState currentState, Operator op){
        return domain.applyOperator(currentState, op);
    }
    private Operator getOperator(SearchState currentState, int i){
        return domain.getOperator(currentState, i);
    }

    private int getNumOperators(SearchState currentState) {
        return domain.getNumOperators(currentState);
    }

    private SearchState unpackDomain(Node currentNode){
        return domain.unpack(currentNode.packed);
    }

    private boolean checkIfGoal(SearchState currentState, Node currentNode){
        double fmin = open.getFmin();
        if(this.domain.isGoal(currentState)){
            if (currentNode.g < fmin * weight) {
                goal = currentNode;
                return true;
            } else {
                double currentCost = 0;
                //DO NOT set the current cost to currentNode.g! it causes floating point errors in GH_heap
                //here we trace back the node, maybe the because the cost might be better than currentNode.g in case we have found a shortcut
                SearchState currentPacked = domain.unpack(currentNode.packed);
                SearchState currentParentPacked = null;
                for (Node node = currentNode;
                     node != null;
                     node = node.parent, currentPacked = currentParentPacked) {
                    // If op of current node is not null that means that p has a parent
                    if (node.op != null) {
                        currentParentPacked = domain.unpack(node.parent.packed);
                        currentCost += node.op.getCost(currentPacked, currentParentPacked);
                    }
                }

                if (bestGoalNode == null || bestGoalNode.g > currentCost) {
                    bestGoalNode = currentNode;
                }
                TreeMap<String, Object> extras = result.getExtras();
                if (extras.get("generatedFirst") == null) {
                    result.setExtras("generatedFirst", result.generated + "");
                }
                Double numOfGoalsFound = Double.parseDouble((String)(extras.get("numOfGoalsFound")));
                result.setExtras("numOfGoalsFound", numOfGoalsFound + 1 + "");
                //                        System.out.print("\n[INFO] A goal was found but not under the bound:"+fmin*weight+" f:"+currentNode.f+", W:"+weight+", fmin:"+fmin);
            }
        }
        else if(bestGoalNode != null && bestGoalNode.g < fmin * weight) {
            goal = bestGoalNode;
            return true;
        }
        return false;
    }

    private boolean checkClosedContains(Node childNode){
        return this.closed.containsKey(childNode.packed);
    }

    private void handleGoal(){
        if (goal != null) {
            System.out.print("\r");
            SearchResultImpl.SolutionImpl solution = new SearchResultImpl.SolutionImpl(this.domain);
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
                System.out.println("[INFO] Goal G is higher than the actual cost " +
                        "(G: " + goal.g +  ", Actual: " + cost + ")");
            }

            Collections.reverse(path);
            solution.addOperators(path);

            Collections.reverse(statesPath);
            solution.addStates(statesPath);

            solution.setCost(cost);
            result.addSolution(solution);

            int smallerThanFmin = 0;
            int smallerThanWcost = 0;
            double fmin = open.getFmin();
            for(Iterator<Map.Entry<PackedElement,Node>> it = closed.entrySet().iterator(); it.hasNext();){
                Map.Entry<PackedElement,Node> entry = it.next();
                Node node = entry.getValue();
                if(node.f < fmin) smallerThanFmin++;
                if(weight * node.f < cost) smallerThanWcost++;
//                it.remove();
            }
            result.setExtras("fmin",fmin+"");
/*            System.out.println("closed size:"+closed.size());
            System.out.println("smallerThanFmin:"+smallerThanFmin);
            System.out.println("smallerThanWcost:"+smallerThanWcost);*/
        }
        else{
            if(this.open.isEmpty()){
                System.out.println("[INFO] no goal found: open.isEmpty");
            }
            if(result.checkMinTimeOut()){
                System.out.println("[INFO] no goal found: checkMinTimeOut:"+result.printWallTime());
            }
            if(result.getGenerated() >= domain.maxGeneratedSize()){
                System.out.println("[INFO] no goal found: maxGeneratedSize");
            }
        }
    }

    @Override
    public Map<String, Class> getPossibleParameters() {
        return DP.DPPossibleParameters;
    }

    @Override
    public void setAdditionalParameter(String parameterName, String value) {
        switch (parameterName) {
            case "weight": {
                this.weight = Double.parseDouble(value);
                if (this.weight < 1.0d) {
                    System.out.println("[ERROR] The weight must be >= 1.0");
                    throw new IllegalArgumentException();
                } else if (this.weight == 1.0d) {
                    System.out.println("[WARNING] Weight of 1.0 is equivalent to A*");
                }
                break;
            }
            case "reopen": {
                this.reopen = Boolean.parseBoolean(value);
                break;
            }
            case "FR": {
                this.FR = Integer.parseInt(value);
                this.useFR = true;
                break;
            }
            case "optimalSolution": {
                this.optimalSolution = Double.parseDouble(value);
                break;
            }
            default: {
                throw new UnsupportedOperationException();
            }
        }
    }

    /**
     *
     * @return chosen Node for expansion
     */
    private Node _selectNode() {
        Node toReturn;
        toReturn = this.open.peek();

        if(useFR) {
            double fmin = open.getFmin();
            if(extractedFmin == fmin){
                toReturn = this.open.peekF();
            }
            else {
                double fminCount = open.getFminCount();
                double lowerLimit = fminCount * FR;
                if (lowerLimit < result.generated) {
                    toReturn = this.open.peekF();
                    extractedFmin = fmin;
                }
            }
        }
        return toReturn;
    }

    /**
     *
     * @param toAdd is the new node toAdd to open
     */
    private void _addNode(Node toAdd) {
        this.open.add(toAdd);
//        this.openF.add(toAdd);
        // The nodes are ordered in the closed list by their packed values
        this.closed.put(toAdd.packed, toAdd);
    }

    /**
     *
     * @param childNode is the new duplicate detected
     */
    private void _duplicateNode(Node childNode) {
        // Count the duplicates
        ++result.duplicates;
        // Get the previous copy of this node (and extract it)
        Node dupChildNode = this.closed.get(childNode.packed);
        // check if the potential has changed
//        dupChildNode.reCalcValue();
        // Take the h value from the previous version of the node (for case of randomization of h values)
        childNode.computeNodeValue(dupChildNode.h,dupChildNode.d);
        // check which child is better
        int compared = NC.compare(childNode, dupChildNode);
        // childNode is better, need to update dupChildNode
        if (compared < 0) {
            // In any case update the duplicate with the new values - we reached it via a shorter path
            _updateNode(dupChildNode,childNode);
        }
    }

    /**
     *
     * @param oldNode is the old Node, last time visited
     * @param newNode is the new Node, with better path
     */
    private void _updateNode(Node oldNode,Node newNode) {
//        double oldF = oldNode.getF();
/*        double oldG = oldNode.getG();
        double oldH = oldNode.getH();
        oldNode.f = newNode.f;
        oldNode.g = newNode.g;
        oldNode.op = newNode.op;
        oldNode.pop = newNode.pop;
        oldNode.parent = newNode.parent;
        oldNode.potential = newNode.potential;*/
        // if dupChildNode is in open, update it there too
        if (oldNode.getIndex(this.open.getKey()) != -1) {
            ++result.opupdated;
            this.open.remove(oldNode);
            _copyNodeValues(oldNode,newNode);
            this.open.add(newNode);
//            this.open.updateF(oldNode,oldG,oldH);//gh_heap
//            this.open.updateF(oldNode, oldF);
//            this.openF.updateF(oldNode, oldF);
        }
        // Otherwise, consider to reopen dupChildNode
        else {
            if (this.reopen) {
                ++result.reopened;
                _copyNodeValues(oldNode,newNode);
                this.open.add(oldNode);//gh_heap
//                this.open.add(oldNode);
//                this.openF.add(oldNode);
            }
        }
        // in any case, update closed to be bestChild
        this.closed.put(oldNode.packed, oldNode);
    }

    /**
     *
     * @param oldNode get values from newNode
     * @param newNode
     */
    private void _copyNodeValues(Node oldNode,Node newNode) {
        oldNode.f = newNode.f;
        oldNode.g = newNode.g;
        oldNode.h = newNode.h;
        oldNode.op = newNode.op;
        oldNode.pop = newNode.pop;
        oldNode.parent = newNode.parent;

        oldNode.d = newNode.d;
        oldNode.sseH = newNode.sseH;
        oldNode.sseD = newNode.sseD;
        oldNode.fHat = newNode.fHat;
        oldNode.hHat = newNode.hHat;
        oldNode.dHat = newNode.dHat;
        oldNode.depth = newNode.depth;
//        oldNode.potential = newNode.potential;
    }

    /**
     *
     * @param toRemove is the node to be removed from open
     */
    private void _removeNode(Node toRemove) {
//        double prevFmin = open.getFmin();
        this.open.remove(toRemove);
//        this.openF.remove(toRemove);
/*        if(prevFmin < open.getFmin()){//fmin changed, need to reorder priority Queue
            _reorder();
        }*/
    }

    /**
     * The node class
     */
    protected final class Node extends SearchQueueElementImpl {
        private double f;
        private double g;
        private double h;
        private double d;
        private double sseH;
        private double sseD;
        private double fHat;
        private double hHat;
        private double dHat;
        private int depth;
//        private double potential;

        private Operator op;
        private Operator pop;

        private Node parent;
        private PackedElement packed;
        private int[] secondaryIndex;
        private double fcounterFmin;

        private Node(SearchState state, Node parent, SearchState parentState, Operator op, Operator pop) {
            // Size of key
            super(2);
            // TODO: Why?
            this.secondaryIndex = new int[1];
            this.g = (op != null) ? op.getCost(state, parentState) : 0;
            this.depth = (op != null) ? 1 : 0;
            // If each operation costs something, we should add the cost to the g value of the parent
            // Our g equals to the cost + g value of the parent
            if (parent != null) {
                this.g += parent.g;
                this.depth += parent.depth;
            }

            // Update potential, h and f values
            this.computeNodeValue(state.getH(),state.getD());

            // Parent node
            this.parent = parent;
            this.packed = DP.this.domain.pack(state);
            this.pop = pop;
            this.op = op;

            // Compute the actual values of sseH and sseD
//            this._computePathHats(parent, cost);

        }

        /**
         * Use the Path Based Error Model by calculating the mean one-step error only along the current search
         * path: The cumulative single-step error experienced by a parent node is passed down to all of its children

         * @return The calculated sseHMean
         */
        private double __calculateSSEMean(double totalSSE) {
            return (this.g == 0) ? totalSSE : totalSSE / this.depth;
        }

        /**
         * @return The mean value of sseH
         */
        private double _calculateSSEHMean() {
            return this.__calculateSSEMean(this.sseH);
        }

        /**
         * @return The mean value of sseD
         */
        private double _calculateSSEDMean() {
            return this.__calculateSSEMean(this.sseD);
        }

        /**
         * @return The calculated hHat value
         *
         * NOTE: if our estimate of sseDMean is ever as large as one, we assume we have infinite cost-to-go.
         */
        private double _computeHHat() {
//            double hHat = Double.MAX_VALUE;
            double hHat = this.h;
            double sseDMean = this._calculateSSEDMean();
            if (sseDMean < 1) {
                double sseHMean = this._calculateSSEHMean();
                hHat = this.h + ( (this.d / (1 - sseDMean)) * sseHMean );
            }
/*            else{
                System.out.println("sseDMean: "+sseDMean);
                hHat = this.h;
            }*/
            return hHat;
        }

        /**
         * @return The calculated dHat value
         *
         * NOTE: if our estimate of sseDMean is ever as large as one, we assume we have infinite distance-to-go
         */
        private double _computeDHat() {
            double dHat = Double.MAX_VALUE;
            double sseDMean = this._calculateSSEDMean();
            if (sseDMean < 1) {
                dHat = this.d / (1 - sseDMean);
            }
            return dHat;
        }

        /**
         * The function computes the values of dHat and hHat of this node, based on that values of the parent node
         * and the cost of the operator that generated this node
         *
         * @param parent The parent node
         * @param edgeCost The cost of the operation which generated this node
         */
        public void _computePathHats(Node parent, double edgeCost) {
            if (parent != null) {
                // Calculate the single step error caused when calculating h and d
                this.sseH = parent.sseH + ((edgeCost + this.h) - parent.h);
                this.sseD = parent.sseD + ((1 + this.d) - parent.d);
/*                if(sseD < 0){
                    System.out.println("sseD: "+sseD);
                    System.out.println("this:"+this);
                    System.out.println("parent:"+parent);
                    SearchDomain.State State = domain.unpack(this.packed);
                    SearchDomain.State parentState = domain.unpack(parent.packed);
                    State.getD();
                    System.out.println("sseD: "+sseD);
                }*/
            }

            this.hHat = this._computeHHat();
            this.dHat = this._computeDHat();
            this.fHat = this.g + this.hHat;

            // This must be true assuming the heuristic is consistent (fHat may only overestimate the cost to the goal)
            if (domain.isCurrentHeuristicConsistent()) {
                assert this.fHat >= this.f;
            }
            assert this.dHat >= 0;
        }

        @Override
        public String toString() {
            SearchState state = domain.unpack(this.packed);
            StringBuilder sb = new StringBuilder();
            sb.append("State:"+state.dumpStateShort());
            sb.append(", h: "+this.h);//h
            sb.append(", g: "+this.g);//g
            sb.append(", f: "+this.f);//f
            sb.append(", d: "+this.d);//d
//            sb.append(", potential: "+this.potential);//potential
//            sb.append(", fcounterFmin: "+this.fcounterFmin);//fcounterFmin
            return sb.toString();
        }

/*        void printNode(Node node){
            SearchDomain.State state = domain.unpack(node.packed);
            StringBuilder sb = new StringBuilder();
            sb.append("State:"+state.dumpStateShort());
            sb.append(", h: "+this.h);//h
            sb.append(", g: "+this.g);//g
            sb.append(", f: "+this.f);//f
            sb.append(", potential: "+this.potential);//potential
//            sb.append(", fcounterFmin: "+this.fcounterFmin);//fcounterFmin
            System.out.println(sb.toString());
            System.out.println("State:"+state.dumpStateShort());
//            System.out.println("F:" +this.f + "\tG:" + this.g + "\tH:" + this.h+ "\tPotential:" + this.potential+ "\tfcounterFmin:" + this.fcounterFmin);
        }*/

        /**
         * The function computes the F values according to the given heuristic value (which is computed externally)
         *
         * Also, all other values that depend on h are updated
         *
         * @param updatedHValue The updated heuristic value
         */
        public void computeNodeValue(double updatedHValue, double updatedDValue) {

            if(debug && this.h != 0 && this.h != updatedHValue){
                // can only happen when updating nodes
                System.out.println("[INFO] GH_heap should update");
            }
            this.h = updatedHValue;
            this.d = updatedDValue;
            this.f = this.g + this.h;
            this.fcounterFmin = open.getFmin();
//            this.potential =  (this.fcounterFmin*DP.this.weight -this.g)/this.h;
        }

/*        public void reCalcValue() {
            if(open.getFmin() != this.fcounterFmin) {
                this.fcounterFmin = open.getFmin();
                this.potential =  (this.fcounterFmin*DP.this.weight -this.g)/this.h;
            }
        }*/


        /**
         * A constructor of the class that instantiates only the state
         *
         * @param state The state which this node represents
         */
        private Node(SearchState state) {
            this(state, null, null, null, null);
        }

/*
        @Override
        public void setSecondaryIndex(int key, int index) {
            this.secondaryIndex[key] = index;
        }

        @Override
        public int getSecondaryIndex(int key) {
            return this.secondaryIndex[key];
        }

        @Override
        public double getRank(int level) {
            return (level == 0) ? this.f : this.g;
        }*/

        @Override
        public double getF() {
            return this.f;
        }

        @Override
        public double getG() {
            return this.g;
        }

        @Override
        public double getDepth() {
            return this.depth;
        }

        @Override
        public double getH() {
            return this.h;
        }

        @Override
        public double getD() {
            return this.d;
        }

        @Override
        public double getHhat() {return this.hHat;}

        @Override
        public double getDhat() {return this.dHat;}

        @Override
        public SearchQueueElement getParent() {
            return this.parent;
        }

    }

    /**
     * The nodes comparator class
     */
    protected final class NodeComparator implements Comparator<Node> {

        @Override
        public int compare(final Node a, final Node b) {
            // First compare by f (smaller is preferred), then by h (smaller is preferred)

            if (a.f < b.f) return -1;
            if (a.f > b.f) return 1;

            if (a.h < b.h) return -1;
            if (a.h > b.h) return 1;

            if (a.d < b.d) return -1;
            if (a.d > b.d) return 1;

            return 0;
        }
    }
}
