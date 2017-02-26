package org.cs4j.core.algorithms.pac;

import org.apache.log4j.Logger;
import org.cs4j.core.SearchDomain;
import org.cs4j.core.mains.DomainExperimentData;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by Roni Stern on 26/02/2017.
 *
 * A place for utility functions and constants relevant to PAC Search
 */
public class PACUtils {

    final static Logger logger = Logger.getLogger(TrivialPACCondition.class);

    public static Map<Integer,Double> getOptimalSolutions(SearchDomain domain)
    {
        String inputFile = DomainExperimentData.get(domain.getClass()).inputPath+"/optimalSolutions.in";
        return parseFileWithPairs(inputFile);
    }
    public static Map<Integer,Double> getInitialHValues(SearchDomain domain)
    {
        String inputFile = DomainExperimentData.get(domain.getClass()).inputPath+"/initialHValues.csv";
        return parseFileWithPairs(inputFile);
    }

    /**
     * Internal helper function that parses a file with 2 columsn to a map
     * @param inputFile input file
     */
    private static Map<Integer,Double> parseFileWithPairs(String inputFile){
        Map<Integer,Double> keyToValue=new TreeMap<Integer,Double>();
        try{
            BufferedReader reader = new BufferedReader(new FileReader(inputFile));
            String line = reader.readLine();
            String[] parts;
            while(line!=null){
                parts = line.split(",");
                keyToValue.put(Integer.parseInt(parts[0]), Double.parseDouble(parts[1]));
                line = reader.readLine();
            }
            reader.close();
        }
        catch(IOException e){
            logger.error("Cannot load statistics",e);
            throw new RuntimeException(e);
        }
        return keyToValue;
    }

    public static String getStatisticsFile(SearchDomain domain){
        return null; // TODO: Implement
    }

}
