package org.cs4j.core.test.algorithms;

import junit.framework.Assert;
import org.cs4j.core.SearchAlgorithm;
import org.cs4j.core.SearchDomain;
import org.cs4j.core.SearchResult;
import org.cs4j.core.algorithms.AnytimePTS;
import org.cs4j.core.algorithms.WAStar;
import org.junit.Test;

import java.io.FileNotFoundException;

/**
 * Created by Roni Stern on 23/02/2017.
 */
public class TestAnytimePTS {

    /**
     * Verify that APTS improves the solution quality every time it is run
     * and that it ends only when it has the optimal solution cost.
     */
    @Test
    public void testAnytimePTS() throws FileNotFoundException {
        SearchDomain domain = TestUtils.createFifteenPuzzle("12");
        AnytimePTS apts = new AnytimePTS();
        SearchResult results = apts.search(domain);
        Assert.assertTrue(results.hasSolution());

        double solutionCost=Double.MAX_VALUE;
        double newSolutionCost;
        do {
            System.out.println("Found solution:"+solutionCost);
            newSolutionCost = results.getSolutions().get(0).getCost();
            Assert.assertTrue("New solution ("+newSolutionCost+") not better than old solution("+solutionCost+")",
                    newSolutionCost<solutionCost);
            results = apts.continueSearch();
            solutionCost=newSolutionCost;
        }while(results.hasSolution());


        // Verify that ended up with the optimal solution (the same as A*)
        WAStar wastar = new WAStar();
        wastar.setAdditionalParameter("weight","1");
        results = wastar.search(domain);
        Assert.assertTrue(results.hasSolution());
        Assert.assertEquals(solutionCost,results.getSolutions().get(0).getCost());
    }
}
