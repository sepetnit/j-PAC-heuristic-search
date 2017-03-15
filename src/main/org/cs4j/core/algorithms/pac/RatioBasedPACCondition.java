package org.cs4j.core.algorithms.pac;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cs4j.core.SearchDomain;
import org.cs4j.core.domains.GridPathFinding;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;


/**
 * Created by user on 26/02/2017.
 *
 * The Trivial PAC condition.
 */
public class RatioBasedPACCondition extends ThresholdPACCondition {
    
    private final static Logger logger = LogManager.getLogger(GridPathFinding.class);

    // The h-value of the initial state
    private double initialH;

    @Override
    public void setup(SearchDomain domain, double epsilon, double delta) {
        this.initialH = domain.initialState().getH();
        super.setup(domain,epsilon,delta);
    }

    /**
     * Computes a cumulative distribution function (CDF) of the heuristic error,
     * that is, the ratio between opt and the initial h (h(start))
     *
     * @param statistics a map of instance to relevant statistics
     * @return a map of solution cost value to the corresponding CDF.
     */
    @Override
    protected SortedMap<Double, Double> computeCDF(PACStatistics statistics){
        Map<Integer,Double> instanceToOptimal = PACUtils.getOptimalSolutions(domain.getClass());
        Map<Integer,Double> instanceToInitialH = PACUtils.getInitialHValues(domain.getClass());

        // Building the PDF (  cost -> prob. that optimal is less than or equal to cost)
        SortedMap<Double, Double> ratioToPDF = new TreeMap<Double, Double>();
        double ratio;
        for(Integer instance : instanceToOptimal.keySet()){
            ratio = instanceToOptimal.get(instance)/instanceToInitialH.get(instance);
            if (!ratioToPDF.containsKey(ratio))
                ratioToPDF.put(ratio,1.0);
            else
                ratioToPDF.put(ratio,(ratioToPDF.get(ratio)+1));
        }
        for(Double cost : ratioToPDF.keySet())
            ratioToPDF.put(cost,(ratioToPDF.get(cost)/instanceToOptimal.size()));

        // Building the CDF (cumulative)
        SortedMap<Double, Double>ratioToCDF = new TreeMap<Double, Double>();
        Double oldCDFValue=0.0;
        for(Double cost : ratioToPDF.keySet()) {
            ratioToCDF.put(cost*this.initialH, ratioToPDF.get(cost) + oldCDFValue);
            oldCDFValue = ratioToCDF.get(cost*this.initialH);
        }

        // Accuracy issues
        if(oldCDFValue!=1.0){
            assert Math.abs(oldCDFValue-1.0)<0.0001; // Verifying this is just an accuracy issue
            ratioToCDF.put(ratioToCDF.lastKey(),1.0);
        }

        return ratioToCDF;
    }

}
