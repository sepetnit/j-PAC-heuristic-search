package java.org.cs4j.core.test.algorithms;

import org.cs4j.core.SearchAlgorithm;
import org.cs4j.core.SearchDomain;
import org.cs4j.core.SearchResult;
import org.cs4j.core.algorithms.familiar.DP;
import org.junit.Assert;
import org.junit.Test;

import java.io.FileNotFoundException;

/**
 * Created by user on 27/02/2017.
 */
public class TestDPS {
    /**
     * Bug fix. Running instances 1-4 before 5 caused 5 to halt immediately with a goal.
     * @throws FileNotFoundException
     */
    @Test
    public void testDPS() throws FileNotFoundException {
        SearchDomain domain;
        SearchAlgorithm dps = new DP("DPS",false,false,false);

        for(int i=1;i<6;i++) {
            domain = TestUtils.createGridPathFinding("brc202d.map",i+"");
            SearchResult result = dps.search(domain);
            Assert.assertTrue(result.hasSolution());
            Assert.assertTrue(result.getExpanded() > 0);
            Assert.assertTrue(result.getGenerated() > 0);
        }
    }

}
