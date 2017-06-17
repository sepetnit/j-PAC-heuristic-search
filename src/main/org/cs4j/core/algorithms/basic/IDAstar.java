package org.cs4j.core.algorithms.basic;
import org.cs4j.core.*;
import org.cs4j.core.algorithms.auxiliary.SearchResultImpl;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Iterative Deepening A* Search
 *
 * @author Matthew Hatem
 */
public class IDAstar extends GenericSearchAlgorithm {
    // The domain for the search
    private SearchDomain domain;

    private SearchResultImpl result;
    private SearchResultImpl.SolutionImpl solution;

    private double weight;
    private double bound;
    private double minNextF;

    /**
     * The default constructor of the class
     */
    public IDAstar() {
  	    this(1.0);
    }

    protected IDAstar(double weight) {
        this.weight = weight;
    }

    @Override
    public String getName() {
        return "idastar";
    }

    @Override
    public Map<String, Class> getPossibleParameters() {
        return null;
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
            default:{
                throw new UnsupportedOperationException();
            }
        }
    }

    @Override
    public SearchResult search(SearchDomain domain) {
        this.result = new SearchResultImpl();
        this.solution = new SearchResultImpl.SolutionImpl();
        SearchState root = domain.initialState();
        this.result.startTimer();
        this.bound = this.weight * root.getH();
        int i = 0;
        do {
            this.minNextF = -1;
            boolean goalWasFound = this.dfs(domain, root, 0, null);
/*            System.out.println("min next f: " + minNextF ) ;
            System.out.println("next");*/
            this.result.addIteration(i, this.bound, this.result.expanded, this.result.generated);
            this.bound = this.minNextF;
            if (goalWasFound) {
                break;
            }
        } while (true);
        this.result.stopTimer();

        SearchResultImpl.SolutionImpl solution = new SearchResultImpl.SolutionImpl(this.domain);
        List<Operator> path = this.solution.getOperators();
        List<SearchState> statesPath = this.solution.getStates();

        path.remove(0);
        Collections.reverse(path);
        solution.addOperators(path);

        statesPath.remove(0);
        Collections.reverse(statesPath);
        solution.addStates(statesPath);

        solution.setCost(this.solution.getCost());
        result.addSolution(solution);

        return this.result;
    }

    /**
     * A single iteration of the IDA*
     *
     * @param domain The domain on which the search is performed
     * @param parent The parent state
     * @param cost The cost to reach the parent state
     * @param pop The reverse operator?
     *
     * @return Whether a solution was found
     */
    private boolean dfs(SearchDomain domain, SearchState parent, double cost, Operator pop) {
        double f = cost + this.weight * parent.getH();
    
        if (f <= this.bound && domain.isGoal(parent)) {
            this.solution.setCost(f);
            this.solution.addOperator(pop);
            return true;
        }

        if (f > this.bound) {
            // Let's record the lowest value of f that is greater than the bound
            if (this.minNextF < 0 || f < this.minNextF)
                this.minNextF = f;
            return false;
        }

        // Expand the current node
        ++result.expanded;
        int numOps = domain.getNumOperators(parent);
        for (int i = 0; i < numOps; ++i) {
    	    Operator op = domain.getOperator(parent, i);
            // Bypass reverse operators
            if (op.equals(pop)) {
                continue;
            }
            ++result.generated;
            SearchState child = domain.applyOperator(parent, op);
            boolean goal = this.dfs(domain, child, op.getCost(child, parent) + cost, op.reverse(parent));
            if (goal) {
                this.solution.addOperator(op);
                this.solution.addState(parent);
                return true;
            }
        }

        // No solution was found
        return false;
    }
}
