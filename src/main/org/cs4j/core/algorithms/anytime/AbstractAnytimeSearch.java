package org.cs4j.core.algorithms.anytime;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cs4j.core.*;
import org.cs4j.core.algorithms.auxiliary.SearchQueueElementImpl;
import org.cs4j.core.algorithms.auxiliary.SearchResultImpl;
import org.cs4j.core.collections.*;

import java.util.*;

/**
 * Created by Roni Stern on 23/02/2017.
 */
public abstract class AbstractAnytimeSearch extends GenericSearchAlgorithm implements AnytimeSearchAlgorithm {
    private final static Logger logger = LogManager.getLogger(AbstractAnytimeSearch.class);

    // The domain to which the search problem belongs
    protected SearchDomain domain;

    // OPEN and CLOSED lists
//    protected SearchQueue<Node> open;
    protected SearchQueue<Node> open;//gh_heap

    protected Map<PackedElement, Node> closed;

    // Inconsistent list
    protected Map<PackedElement, Node> incons;

    // The search results encompasses all the iterations run so far
    protected SearchResultImpl totalSearchResults;

    // The search result of the current iteration
    protected SearchResultImpl result;

    // The cost of the best solution found so far
    public double incumbentSolution;

    // The number of anytime iteratoins (~ the number of goals found so far)
    public int iteration;

    // Whether reopening is allowed
    protected boolean reopen;


    // A data structure to maintain minf. @TODO: Allow disabling this for Anytime algorithms that don't care about this
    protected HashMap<Double, Integer> fCounter = new HashMap<Double,Integer>();
    protected double maxFmin; // The maximal fmin observed so far. This is a lower bound on the optimal cost
    protected double fmin; // The minimal f value currently in the open list

    public AbstractAnytimeSearch() {
        // Initial values (afterwards they can be set independently)
        this.reopen = true;
    }

    @Override
    public String getName(){
        return this.getClass().getSimpleName();
    }


    /**
     * Initializes the data structures of the search
     *
     * @param clearOpen   Whether to initialize the open list
     * @param clearClosed Whether to initialize the closed list
     */
    protected void _initDataStructures(boolean clearOpen, boolean clearClosed) {
        if (clearOpen) {
            this.open = new BinHeap<Node>(this.createNodeComparator(), 0);
        }
        if (clearClosed) {
            this.closed = new HashMap<>();
        }
    }


    /**
     * Create a node comparator used by the open list to prioritize the nodes
     */
    abstract protected Comparator<Node> createNodeComparator();


    /**
     * The internal main search procedure
     *
     * @return The search result filled by all the results of the search
     */
    protected SearchResultImpl _search() {
        // The result will be stored here
        Node goal = null;
        this.result = new SearchResultImpl();
        if(this.totalSearchResults==null)
            this.totalSearchResults=this.result;

        result.startTimer();

         // Loop while there is no solution and there are states in the OPEN list
        SearchState childState,currentState;
        Node currentNode, childNode, dupChildNode;
        Operator op;
        double childf,dupChildf;
        while ((goal == null) && !this.open.isEmpty()) {
            // Take a node from the OPEN list (nodes are sorted according to the 'u' function)
            currentNode = this.open.poll();
            this.removeFromfCounter(currentNode.getF());

            // Extract a state from the node
            currentState = domain.unpack(currentNode.packed);
            // expand the node (since, if its g satisfies the goal test - it would be already returned)
            ++result.expanded;
            if (result.expanded % 1000000 == 0)
                logger.info("[INFO] Expanded so far " + result.expanded);

            // Go over all the successors of the state
            for (int i = 0; i < domain.getNumOperators(currentState); ++i) {
                // Get the current operator
                op = domain.getOperator(currentState, i);
                // Don't apply the previous operator on the state - in order not to enter a loop
                if (op.equals(currentNode.pop)) {
                    continue;
                }
                // Otherwise, let's generate the child state
                ++result.generated;
                // Get it by applying the operator on the parent state
                childState = domain.applyOperator(currentState, op);
                // Create a search node for this state
                childNode = new Node(childState, currentNode, currentState, op, op.reverse(currentState));

                // Prune nodes over the bound
                if (childNode.getF() >= this.incumbentSolution) {
                    continue;
                }

                // If the generated node satisfies the goal condition - let' mark the goal and break
                if (domain.isGoal(childState)) {
                    goal = childNode;
                    break;
                }

                // If we got here - the state isn't a goal!

                // Now, merge duplicates - let's check if the state already exists in CLOSE/OPEN:
                // In the node is not in the CLOSED list, then it is also not in the OPEN list
                // In any case it can't be that node is a goal - otherwise, we should return it
                // when we see it at first
                if (this.closed.containsKey(childNode.packed)) {
                    // Count the duplicates
                    ++result.duplicates;
                    // Take the duplicate node
                    dupChildNode = this.closed.get(childNode.packed);
                    childf = childNode.getF();
                    dupChildf = dupChildNode.getF();
                    if (dupChildf > childf) {
                        // Consider only duplicates with higher G value
                        if (dupChildNode.g > childNode.g) {
                            // Make the duplicate to be successor of the current parent node
                            dupChildNode.g = childNode.g;
                            dupChildNode.op = childNode.op;
                            dupChildNode.pop = childNode.pop;
                            dupChildNode.parent = childNode.parent;

                            // In case the node is in the OPEN list - update its key using the new G
                            if (dupChildNode.getIndex(this.open.getKey()) != -1) {
                                ++result.opupdated;
                                this.open.update(dupChildNode);
                                this.closed.put(dupChildNode.packed, dupChildNode);

                                // Update fCounter (and possible minf and maxminf)
                                this.addTofCounter(childf);
                                this.removeFromfCounter(dupChildf);
                            } else {
                                // Return to OPEN list only if reopening is allowed
                                if (this.reopen) {
                                    ++result.reopened;
                                    this.open.add(dupChildNode);
                                    this.addTofCounter(childf);

                                } else {
                                    // Maybe, we will want to expand these states later
                                    this.incons.put(dupChildNode.packed, dupChildNode);
                                }
                                // In any case, update the duplicate node in CLOSED
                                this.closed.put(dupChildNode.packed, dupChildNode);
                            }
                        }
                    }
                    // Consider the new node only if its cost is lower than the maximum cost
                } else {
                    // Otherwise, add the node to the search lists
                    this.open.add(childNode);
                    this.addTofCounter(childNode.getF());
                    this.closed.put(childNode.packed, childNode);
                }


                // Update the fCounter and possible minf and maxminf
                this.updateFmin();
            }
        }
        // Stop the timer and check that a goal was found
        result.stopTimer();

        // If a goal was found: update the solution
        if (goal != null) {
            result.addSolution(constructSolution(goal, this.domain));
        }

        result.setExtras("fmin",this.maxFmin); // Record the lower bound for future analysis @TODO: Not super elegant
        return result;
    }


    /**
     * After adding a node to OPEN, we update the f-counter
     * to keep track of minf
     * @param f the (admissible) f value of the node that was just added to OPEN
     */
    private void addTofCounter(double f){
        if(this.fCounter.containsKey(f))
            this.fCounter.put(f, this.fCounter.get(f)+1);
        else
            this.fCounter.put(f,1);

        // Update fmin if needed
        if(f<this.fmin)
            this.fmin=f;
    }
    /**
     * After removing from OPEN a node with a given f-value,
     */
    protected void removeFromfCounter(double f) {
        int newfCount = this.fCounter.get(f)-1;
        this.fCounter.put(f,newfCount);

        if(newfCount==0){
            this.fCounter.remove(f);
        }
    }

    /**
     * If there are no more nodes with the old fmin, need to update fmin and maybe also maxfmin accordingly.
     */
    protected void updateFmin(){
        // If fmin is no longer fmin, need to search for a new fmin @TODO: May improve efficiency
        if(this.fCounter.containsKey(fmin)==false){
            fmin=Double.MAX_VALUE;
            for(double fInOpen : this.fCounter.keySet()){
                if(fInOpen<fmin)
                    fmin=fInOpen;
            }
            if(maxFmin<fmin)
                maxFmin=fmin;
        }
    }



    /**
     * Construct a solution for the given domain after a goal has been found,
     * and update the given SearchResults object accordingly.
     * @param goal The goal node that was found
     * @param domain The domain
     * @return The new solution found
     */
    private static SearchResult.Solution constructSolution(Node goal, SearchDomain domain) {
        Node currentNode;
        SearchResultImpl.SolutionImpl solution = new SearchResultImpl.SolutionImpl(domain);
        List<Operator> path = new ArrayList<>();
        List<SearchState> statesPath = new ArrayList<>();
        System.out.print("[INFO] Solved - Generating output path. Cost=");
        double cost = 0;

        SearchState currentPacked = domain.unpack(goal.packed);
        SearchState currentParentPacked = null;
        for (currentNode = goal;
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
        System.out.println("Cost="+cost);
        // The actual size of the found path can be only lower the G value of the found goal
        assert cost <= goal.g;
        if (cost - goal.g < 0) {
            System.out.println("[INFO] Goal G is higher that the actual cost " +
                    "(G: " + goal.g +  ", Actual: " + cost + ")");
        }

        Collections.reverse(path);
        solution.addOperators(path);

        Collections.reverse(statesPath);
        solution.addStates(statesPath);

        solution.setCost(cost);
        return solution;
    }

    /**
     * Search from a given start node until finding the first goal
     * @param domain The domain to apply the search on
     */
    @Override
    public SearchResult search(SearchDomain domain) {
        // Initially all the data structures are cleaned
        this.domain = domain;
        this.incumbentSolution=Double.MAX_VALUE;
        this.iteration=0;
        // The result will be stored here
        // Initialize all the data structures )
        this._initDataStructures(true, true);

        // Extract the initial state from the domain
        SearchState currentState = domain.initialState();
        // Initialize a search node using the state (contains data according to the current
        // algorithm)
        Node initialNode = new Node(currentState);

        // Start the search: Add the node to the OPEN and CLOSED lists
        this.open.add(initialNode);
        double startFmin = initialNode.getF();
        this.fCounter.put(startFmin,1);
        this.maxFmin = startFmin;
        this.fmin = startFmin;

        // n in OPEN ==> n in CLOSED -Thus- ~(n in CLOSED) ==> ~(n in OPEN)
        this.closed.put(initialNode.packed, initialNode);

        SearchResult results = this._search();
        if(results.hasSolution())
            this.incumbentSolution=results.getSolutions().get(0).getCost();

        // Store these results if we continue the search
        this.totalSearchResults=(SearchResultImpl)results;
        return results;
    }

    /**
     * Continues the search to find better goals
     * @return a better solution, if exists
     */
    @Override
    public SearchResult continueSearch() {
        this.iteration++;
        this._initDataStructures(false,false);
        SearchResult results = this._search();

        // Update total search results, which contains the effort over all the iterations
        this.totalSearchResults.addIteration(this.iteration,this.incumbentSolution,results.getExpanded(), results.getGenerated());
        this.totalSearchResults.increase(results);

        if(results.hasSolution()) {
            SearchResult.Solution bestSolution = results.getBestSolution();
            double solutionCost = bestSolution.getCost();
            assert solutionCost<this.incumbentSolution;
            this.incumbentSolution = solutionCost;
            this.totalSearchResults.getSolutions().add(bestSolution);
        }
        return results;
    }


    @Override
    public Map<String, Class> getPossibleParameters() {
        return new HashMap<>();
    }

    @Override
    public void setAdditionalParameter(String parameterName, String value) {
        switch (parameterName) {
            default: {
                System.err.println("No such parameter: " + parameterName + " (value: " + value + ")");
                throw new UnsupportedOperationException();
            }
        }
    }
    /**
     * Returns a SearchResults object that contains all the search results so
     */
    @Override
    public SearchResult getTotalSearchResults() { return this.totalSearchResults; }

    /**
     * The Node is the basic data structure which is used by the algorithm during the search -
     * OPEN and CLOSED lists contain nodes which are created from the domain states
     */
    public final class Node extends SearchQueueElementImpl implements BucketHeap.BucketHeapElement {
        public double g;
        public double h;
        public double d;

        private Operator op;
        private Operator pop;

        private Node parent;

        private PackedElement packed;

        private int[] secondaryIndex;

        /**
         * An extended constructor which receives the initial state, but also the parent of the node
         * and operators (last and previous)
         *
         * @param state The state from which the node should be created
         * @param parent The parent node
         * @param parentState The state of the parent
         * @param op The operator which was applied to the parent state in order to get the current
         *           one
         * @param pop The operator which will reverse the last applied operation which revealed the
         *            current state
         */
        private Node(SearchState state, Node parent, SearchState parentState, Operator op, Operator pop) {
            // The size of the key (for SearchQueueElementImpl) is 1
            super(1);
            this.secondaryIndex = new int[1];
            // WHY THE COST IS OF APPLYING THE OPERATOR ON THAT NODE????
            // SHOULDN'T IT BE ON THE PARENT???
            // OR EVEN MAYBE WE WANT EITHER PARENT **AND** THE CHILD STATES TO PASS TO THE getCost
            // FUNCTION IN ORDER TO GET THE OPERATOR VALUE ...
            double cost = (op != null) ? op.getCost(state, parentState) : 0;
            this.h = state.getH();
            this.d = state.getD();
            this.g = (parent != null)? parent.g + cost : cost;
            this.parent = parent;
            this.packed = domain.pack(state);
            this.pop = pop;
            this.op = op;
        }

        /**
         * @return The computed (on the fly) value of f
         */
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

        @Override
        public double getD() {return this.d;}

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

        /**
         * Default constructor which creates the node from some given state
         *
         * {see Node(State, Node, State, Operator, Operator)}
         *
         * @param state The state from which the node should be created
         */
        private Node(SearchState state) {
            this(state, null, null, null, null);
        }

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
            return (level == 0) ? this.getF() : this.g;
        }
    }
}
