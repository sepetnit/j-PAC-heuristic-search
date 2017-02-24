package org.cs4j.core.test.algorithms;

import junit.framework.Assert;
import org.cs4j.core.AnytimeSearchAlgorithm;
import org.cs4j.core.SearchAlgorithm;
import org.cs4j.core.SearchDomain;
import org.cs4j.core.SearchResult;
import org.cs4j.core.algorithms.AnytimePTS;
import org.cs4j.core.algorithms.AnytimeWAStar;
import org.cs4j.core.algorithms.WAStar;
import org.junit.Test;

import java.io.FileNotFoundException;

/**
 * Created by Roni Stern on 23/02/2017.
 */
public class TestAnytimeSearchAlgorithms {

    /**
     * Verify that the given anytime search algorithm improves the solution quality every time it is run
     * and that it ends only when it has the optimal solution cost.
     */
    public void testAnytimeSearch(AnytimeSearchAlgorithm algorithm) throws FileNotFoundException {
        SearchDomain[] domains = {
                TestUtils.createFifteenPuzzle("12"),
                TestUtils.createPancakePuzzle(12,"12"),
                TestUtils.createGridPathFinding("brc202d.map","12"),
                TestUtils.createVacuumRobot(5,"12"),
                TestUtils.createDockyardRobot("12")
        };
        for(SearchDomain domain : domains){
            SearchResult results = algorithm.search(domain);
            Assert.assertTrue(results.hasSolution());

            double solutionCost=Double.MAX_VALUE;
            double newSolutionCost;
            do {
                System.out.println("Found solution:"+solutionCost);
                newSolutionCost = results.getSolutions().get(0).getCost();
                Assert.assertTrue("New solution ("+newSolutionCost+") not better than old solution("+solutionCost+")",
                        newSolutionCost<solutionCost);
                results = algorithm.continueSearch();
                solutionCost=newSolutionCost;
            }while(results.hasSolution());


            // Verify that ended up with the optimal solution (the same as A*)
            results = TestUtils.findOptimalSolution(domain);
            Assert.assertTrue(results.hasSolution());
            Assert.assertEquals(solutionCost,results.getSolutions().get(0).getCost());
        }
    }

    @Test
    public void testAnytimePTS() throws FileNotFoundException
    {
        testAnytimeSearch(new AnytimePTS());
    }
    @Test
    public void testAnytimeWAstar() throws FileNotFoundException {

        AnytimeWAStar algorithm = new AnytimeWAStar();
        algorithm.setAdditionalParameter("weight","3");

        testAnytimeSearch(algorithm);
    }

    /**
     * Run WA* and AWA* with the same weight and verify that they behave exactly the same
     */
    @Test
    public void testAnytimeWAstarLikeWAstar() throws FileNotFoundException {
        for(double w=1;w<4;w=w+0.25) {
            WAStar wastar = new WAStar();
            wastar.setAdditionalParameter("weight", ""+w);
            SearchDomain domain = TestUtils.createFifteenPuzzle("12");

            SearchResult resultsWAstar = wastar.search(domain);
            Assert.assertTrue(resultsWAstar.hasSolution());

            AnytimeWAStar awastar = new AnytimeWAStar();
            awastar.setAdditionalParameter("weight", ""+w);
            SearchResult resultsAWAstar = awastar.search(domain);
            Assert.assertTrue(resultsAWAstar.hasSolution());

            SearchResult.Solution AWAstarSolution = resultsAWAstar.getSolutions().get(0);
            SearchResult.Solution WAstarSolution = resultsWAstar.getSolutions().get(0);
            Assert.assertEquals(WAstarSolution.getCost(), AWAstarSolution.getCost());
        }
    }


}
