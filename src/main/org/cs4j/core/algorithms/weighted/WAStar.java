package org.cs4j.core.algorithms.weighted;

import org.cs4j.core.Operator;
import org.cs4j.core.SearchDomain;
import org.cs4j.core.SearchResultImpl;
import org.cs4j.core.SearchState;
import org.cs4j.core.algorithms.auxiliary.GenericNode;

import java.util.Comparator;

public class WAstar extends GenericWAstar<WAstar.Node, WAstar.NodeComparator> {

    public class Node extends GenericNode<Node> {

        private double getWf() {
            return this.g + WAstar.this.weight * this.h;
        }

        public Node(SearchState state) {
            super(state);
        }

        public Node(SearchState state,
                    Node parent,
                    SearchState parentState,
                    Operator op,
                    Operator pop) {
            super(state, parent, parentState, op, pop);
        }
    }


    @Override
    protected NodeComparator getComparator() {
        return new WAstar.NodeComparator();
    }

    @Override
    protected Node getNode(SearchState state) {
        return new Node(state);
    }

    @Override
    protected Node getNode(SearchState state,
                           Node parent,
                           SearchState parentState,
                           Operator op,
                           Operator pop) {
        return new Node(state, parent, parentState, op, pop);
    }

    protected boolean shouldStop(SearchResultImpl result) {
        return result.solutionsCount() == 1;
    }

    @Override
    // By default, replace duplicate by found node if nodeF < dupF
    protected boolean shouldReplaceNode(SearchDomain domain,
                                        Node existingNode,
                                        Node newFoundNode) {
        // Found a shorter path to the node
        // Check that the f actually decreases
        if (existingNode.getWf() > newFoundNode.getWf()) {
            return true;
        }
        if (domain.isCurrentHeuristicConsistent()) {
            // TODO: log!
            assert false;
        }
        return false;
    }

    /**
     * The nodes comparator class
     */
    protected class NodeComparator implements Comparator<Node> {

        @Override
        public int compare(final Node a, final Node b) {
            // First compare by wF (smaller is preferred), then by g (bigger is preferred)
            if (a.getWf() < b.getWf()) {
                return -1;
            }
            if (a.getWf() > b.getWf()) {
                return 1;
            }
            if (a.getG() > b.getG()) {
                return -1;
            }
            if (a.getG() < b.getG()) {
                return 1;
            }
            return 0;
        }
    }
}
