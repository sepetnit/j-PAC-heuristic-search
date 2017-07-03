package org.cs4j.core.algorithms.pac;

import org.cs4j.core.SearchResultImpl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Created by Roni Stern on 26/02/2017.
 *
 * An anytime PAC search that is based on APTS
 */
public class AnytimePTS4PAC extends AnytimePACSearch {
    @Override
    protected Comparator<Node> createNodeComparator() {
        return new Comparator<Node>(){
            public int compare(final Node a, final Node b) {
                double aCost = (AnytimePTS4PAC.this.incumbentSolution - a.g) / a.h;
                double bCost = (AnytimePTS4PAC.this.incumbentSolution - b.g) / b.h;

                if (aCost > bCost) {
                    return -1;
                }

                if (aCost < bCost) {
                    return 1;
                }

                // Here we have a tie @TODO: What about tie-breaking?
                return 0;
            }
        };
    }

    @Override
    // Resort OPEN before continuing the search, because the PTS evaluation function considers the incumbent solution
    public SearchResultImpl continueSearch() {
        // Resort open according to the new incumbent @TODO: Study if this actually helps or not?
        List<Node> openNodes = new ArrayList<Node>(this.open.size());
        while(this.open.size()>0)
            openNodes.add(this.open.poll());
        for(Node node : openNodes)
            this.open.add(node);
        openNodes.clear();

        return super.continueSearch();
    }
}
