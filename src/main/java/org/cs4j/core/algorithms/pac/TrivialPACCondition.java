package org.cs4j.core.algorithms.pac;

import org.apache.log4j.Logger;
import org.cs4j.core.SearchDomain;
import org.cs4j.core.SearchResult;
import org.cs4j.core.mains.DomainExperimentData;

import java.io.*;
import java.util.*;



/**
 * Created by user on 26/02/2017.
 *
 * The Trivial PAC condition.
 */
public class TrivialPACCondition extends AbstractPACCondition {

    final static Logger logger = Logger.getLogger(TrivialPACCondition.class);


    private double threshold; // If a solution is equal to or lower than this bound - we can halt


    @Override
    public boolean shouldStop(SearchResult incumbentSolution) {
        // Compute threshold
        return incumbentSolution.getBestSolution().getCost()<=this.threshold;
    }

    @Override
    public void setup(SearchDomain domain, double epsilon, double delta) {
        super.setup(domain,epsilon,delta);

        // Load statistics
        Statistics statistics = new Statistics();
        statistics.load(PACUtils.getStatisticsFile(domain));

        // Compute threshold
        threshold = statistics.computeThreshold(epsilon,delta);

        // Dump statistics (for DEBUG)
        statistics.dumpToFile(DomainExperimentData.domainToExperimentData.get(domain.getClass()).outputPath+"statistics.csv");

    }

    /**
     * This class holds the statistics required for this PAC condition
     */
    public class Statistics
    {
        private SortedMap<Double, Double> costToCDF;
        public void load(String statisticsFile){
            Map<Integer,Double> instanceToOptimal = PACUtils.getOptimalSolutions(domain);

            // Building the PDF (  cost -> prob. that optimal is less than or equal to cost)
            SortedMap<Double, Double> costToPDF = new TreeMap<Double, Double>();
            Collection<Double> optimalSolutions = instanceToOptimal.values();
            for(Double optimalCost : optimalSolutions){
                if(costToPDF.containsKey(optimalCost)==false)
                    costToPDF.put(optimalCost,1.0);
                else
                    costToPDF.put(optimalCost,(costToPDF.get(optimalCost)+1));
            }
            for(Double cost : costToPDF.keySet())
                costToPDF.put(cost,(costToPDF.get(cost)/optimalSolutions.size()));

            // Building the CDF (cumulative)
            costToCDF = new TreeMap<Double, Double>();
            Double oldCDFValue=0.0;
            for(Double cost : costToPDF.keySet()) {
                costToCDF.put(cost, costToPDF.get(cost) + oldCDFValue);
                oldCDFValue = costToCDF.get(cost);
            }

            // Accuracy issues
            if(oldCDFValue!=1.0){
                assert Math.abs(oldCDFValue-1.0)<0.0001; // Verifying this is just an accuracy issue
                costToCDF.put(costToCDF.lastKey(),1.0);
            }
        }

        /**
         * Compute the cost threshold that would satisfy finding a PAC condition
         * @param epsilon desired suboptimality bound
         * @param delta required confidence
         * @return the maximal cost that ensured these PAC parameters
         */
        public double computeThreshold(double epsilon, double delta){
            Double oldCdfValue = 0.0;
            double cdfValue;
            for(Double cost : costToCDF.keySet()) { // Note that costsToCDF is a sorted list!
                cdfValue=costToCDF.get(cost);
                if(cdfValue==delta)
                    return cost*(1+epsilon);
                if (cdfValue>delta)
                    return oldCdfValue*(1+epsilon);
                oldCdfValue=cdfValue;
            }
            throw new IllegalStateException("CDF must sum up to one, so delta value must be met (delta was "+delta+" and last CDF value was "+oldCdfValue+")");
        }

        /**
         * Dump statitics to file
         * @param outputFileName the output file name
         */
        public void dumpToFile(String outputFileName) {
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(outputFileName));
                writer.write("Cost, Pr(OPT<Cost)");
                writer.newLine();
                for (Double cost : costToCDF.keySet()) {
                    writer.write(cost + "," + costToCDF.get(cost));
                    writer.newLine();
                }
                writer.close();
            }catch(IOException exception){
                logger.error("Statistics.dumpToFile failed",exception);
            }
        }
    }
}
