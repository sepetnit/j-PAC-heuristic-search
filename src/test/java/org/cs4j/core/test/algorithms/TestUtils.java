package org.cs4j.core.test.algorithms;

import org.cs4j.core.SearchAlgorithm;
import org.cs4j.core.SearchDomain;
import org.cs4j.core.SearchResult;
import org.cs4j.core.domains.FifteenPuzzle;
import org.junit.Assert;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * Created by user on 23/02/2017.
 */
public class TestUtils {
    public static SearchDomain createFifteenPuzzle(String instance) throws FileNotFoundException {
        InputStream is = new FileInputStream(new File("./input/fifteenpuzzle/korf100/"+instance+".in"));
        FifteenPuzzle puzzle = new FifteenPuzzle(is);
        return puzzle;
    }

    public static void testSearchAlgorithm(SearchDomain domain, SearchAlgorithm algo,
                                    long generated, long expanded, double cost) {
        SearchResult result = algo.search(domain);
        SearchResult.Solution sol = result.getSolutions().get(0);
        Assert.assertTrue(result.getWallTimeMillis() > 1);
        Assert.assertTrue("Wall time is " + result.getWallTimeMillis(), result.getWallTimeMillis() < 20000);
        Assert.assertTrue(result.getCpuTimeMillis() > 1);
        Assert.assertTrue(result.getCpuTimeMillis() < 200);
        Assert.assertTrue(result.getGenerated() == generated);
        Assert.assertTrue(result.getExpanded() == expanded);
        Assert.assertTrue(sol.getCost() == cost);
        Assert.assertTrue(sol.getLength() == cost);
    }
}
