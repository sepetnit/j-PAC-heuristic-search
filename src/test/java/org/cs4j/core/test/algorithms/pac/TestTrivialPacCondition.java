package org.cs4j.core.test.algorithms.pac;

import junit.framework.Assert;
import org.apache.log4j.Logger;
import org.cs4j.core.SearchDomain;
import org.cs4j.core.SearchResult;
import org.cs4j.core.algorithms.pac.TrivialPACCondition;
import org.cs4j.core.domains.*;
import org.cs4j.core.experiments.ExperimentUtils;
import org.cs4j.core.test.algorithms.TestUtils;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

/**
 * Created by user on 26/02/2017.
 *
 * Test case for the Trivial PAC condition
 */
public class TestTrivialPacCondition {

    final static Logger logger = Logger.getLogger(TestTrivialPacCondition.class);


    @Test
    public void testSetup() throws FileNotFoundException {

        Class[] domains = {
                FifteenPuzzle.class,
                Pancakes.class,
                VacuumRobot.class,
                DockyardRobot.class,
                GridPathFinding.class};

        for(Class domainClass :domains){
            logger.info("Testing domain "+domainClass.getName());
            SearchDomain instance = ExperimentUtils.getSearchDomain(domainClass,12);
            SearchResult resultZero = new SearchResultsStub(0);
            SearchResult resultMax = new SearchResultsStub(Double.MAX_VALUE);
            TrivialPACCondition condition = new TrivialPACCondition();

            condition.setup(instance,0,0);
            Assert.assertFalse(condition.shouldStop(resultMax));
            Assert.assertTrue(condition.shouldStop(resultZero));

            condition.setup(instance,1,0);
            Assert.assertFalse(condition.shouldStop(resultMax));
            Assert.assertTrue(condition.shouldStop(resultZero));

            condition.setup(instance,0,1);
            Assert.assertFalse(condition.shouldStop(resultMax));
            Assert.assertTrue(condition.shouldStop(resultZero));

            condition.setup(instance,1,1);
            Assert.assertFalse(condition.shouldStop(resultMax));
            Assert.assertTrue(condition.shouldStop(resultZero));
        }
    }


    // ----------------- STUBS ------------------------
    private class SolutionStub implements SearchResult.Solution{

        private double cost;

        public SolutionStub(double cost){this.cost=cost;}

        @Override
        public double getCost() {
            return this.cost;
        }

        @Override
        public List<SearchDomain.Operator> getOperators() {
            return null;
        }

        @Override
        public List<SearchDomain.State> getStates() {
            return null;
        }

        @Override
        public String dumpSolution() {
            return null;
        }


        @Override
        public int getLength() {
            return 0;
        }
    }



    private class SearchResultsStub implements SearchResult
    {
        private Solution solution;

        public SearchResultsStub(double cost){
            this.solution = new SolutionStub(cost);
        }
        @Override
        public boolean hasSolution() {
            return true;
        }
        @Override
        public Solution getBestSolution() {
            return this.solution;
        }


        @Override
        public List<Solution> getSolutions() {
            return null;
        }



        @Override
        public long getFirstIterationExpanded() {
            return 0;
        }

        @Override
        public long getExpanded() {
            return 0;
        }

        @Override
        public long getGenerated() {
            return 0;
        }

        @Override
        public long getDuplicates() {
            return 0;
        }

        @Override
        public long getUpdatedInOpen() {
            return 0;
        }

        @Override
        public long getReopened() {
            return 0;
        }

        @Override
        public TreeMap<String, Object> getExtras() {
            return null;
        }

        @Override
        public long getWallTimeMillis() {
            return 0;
        }

        @Override
        public long getCpuTimeMillis() {
            return 0;
        }

        @Override
        public void increase(SearchResult previous) {

        }
    }

}
