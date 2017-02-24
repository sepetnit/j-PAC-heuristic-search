package org.cs4j.core.algorithms;


import org.cs4j.core.SearchResult;
import org.cs4j.core.collections.BinHeap;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.*;

/**
 * Created by user on 23/02/2017.
 * Anytime Potential Search
 *
 * @author Roni Stern (based on work by Vitali Sepetnitsky)
 */
public class AnytimePTS extends AbstractAnytimeSearch {

    private static final Map<String, Class> POSSIBLE_PARAMETERS;

    // Declare the parameters that can be tunes before running the search
    static
    {
        POSSIBLE_PARAMETERS = new HashMap<>();
    }

    public AnytimePTS() {
        super();
    }

    @Override
    public String getName() {
        return "AnytimePTS";
    }

    @Override
    public Map<String, Class> getPossibleParameters() {
        return AnytimePTS.POSSIBLE_PARAMETERS;
    }

    @Override
    public void setAdditionalParameter(String parameterName, String value) {
        switch (parameterName) {
            default: {
                System.err.println("No such parameter: " + parameterName + " (value: " + value + ")");
                throw new NotImplementedException();
            }
        }
    }

    /**
     * Continues the search to find better goals
     * @return a better solution, if exists
     */
    @Override
    public SearchResult continueSearch() {
        this._initDataStructures(false,false);

        // Resort open according to the new incumbent @TODO: Study if this actually helps or not?
        List<Node> openNodes = new ArrayList<Node>(this.open.size());
        while(this.open.size()>0)
            openNodes.add(this.open.poll());
        for(Node node : openNodes)
            this.open.add(node);
        openNodes.clear();

        SearchResult results = this._search();
        if(results.hasSolution()) {
            double solutionCost = results.getSolutions().get(0).getCost();
            assert solutionCost<this.incumbentSolution;
            this.incumbentSolution = solutionCost;
        }
        return results;
    }

    /**
     * Create a node comparator used by the open list to prioritize the nodes
     */
    @Override
    protected Comparator<Node> createNodeComparator()
    {
        return new AnytimePTS.NodeComparator();
    }

    /**
     * The node comparator class
     */
    private final class NodeComparator implements Comparator<Node> {
        @Override
        public int compare(final Node a, final Node b) {
            double aCost = (AnytimePTS.this.incumbentSolution - a.g) / a.h;
            double bCost = (AnytimePTS.this.incumbentSolution - b.g) / b.h;

            if (aCost > bCost) {
                return -1;
            }

            if (aCost < bCost) {
                return 1;
            }

            // Here we have a tie @TODO: What about tie-breaking?
            return 0;
        }
    }
}
