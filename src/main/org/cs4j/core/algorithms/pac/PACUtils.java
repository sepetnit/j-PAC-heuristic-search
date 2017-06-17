package org.cs4j.core.algorithms.pac;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cs4j.core.domains.DomainExperimentData;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by Roni Stern on 26/02/2017.
 *
 * A place for utility functions and constants relevant to PAC Search
 */
public class PACUtils {

    private final static Logger logger = LogManager.getLogger(TrivialPACCondition.class);

    // Maps a domain to a PAC statistics object, used later by the PAC conditions
    private static Map<Class, PACStatistics> domainToPACStatistics
            = new HashMap<>();

    public static Map<Integer,Double> getOptimalSolutions(Class domainClass) {
        String inputFile = DomainExperimentData.get(domainClass).inputPath+"/optimalSolutions.in";
        return parseFileWithPairs(inputFile);
    }
    
    public static Map<Integer,Double> getInitialHValues(Class domainClass) {
        String inputFile = DomainExperimentData.get(domainClass).inputPath+"/initialHValues.csv";
        return parseFileWithPairs(inputFile);
    }

    /**
     * Internal helper function that parses a file with 2 columsn to a map
     * @param inputFile input file
     */
    private static Map<Integer,Double> parseFileWithPairs(String inputFile){
        Map<Integer,Double> keyToValue= new TreeMap<>();
        try{
            BufferedReader reader = new BufferedReader(new FileReader(inputFile));
            String line = reader.readLine();
            String[] parts;

            // Handle first line if headers, if such exists
            parts = line.split(",");
            if(!isDouble(parts[1]))// Then this is a header row and should ignore it
                line = reader.readLine();

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

    public static PACStatistics getStatisticsFile(PACCondition condition, Class domainClass){
        return domainToPACStatistics.get(domainClass);
    }
    
    @SuppressWarnings("unused")
    private static void setStatisticFile(PACCondition condition, Class domainClass,PACStatistics statistics){
        domainToPACStatistics.put(domainClass,statistics);
    }
    
    public static void setStatisticFile(Class domainClass,PACStatistics statistics){
        setStatisticFile(null, domainClass, statistics);
    }
    
    
    /**
     * Check if a string is convertable to double
     */
    private static boolean isDouble(String text){
        try {
            Double.parseDouble(text);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    /**
     * Load PAC statistics for the given domain
     * @param domainClass the class of domains for which to load statistics
     */
    public static void loadPACStatistics(Class domainClass)
    {
        PACStatistics pacStatistics = new PACStatistics();
        pacStatistics.instanceToInitialH = getInitialHValues(domainClass);
        pacStatistics.instanceToOptimal= getOptimalSolutions(domainClass);
        setStatisticFile(null,domainClass,pacStatistics);
    }
}
