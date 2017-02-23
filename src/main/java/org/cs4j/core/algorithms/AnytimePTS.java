package org.cs4j.core.algorithms;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

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
