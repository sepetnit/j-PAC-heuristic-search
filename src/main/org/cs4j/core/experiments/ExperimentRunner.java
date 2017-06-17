package org.cs4j.core.experiments;

import org.cs4j.core.SearchAlgorithm;

import java.util.HashMap;

/**
 * Created by user on 23/02/2017.
 */
public abstract class ExperimentRunner {
   
	
	public abstract double[] run(Class domainClass, SearchAlgorithm alg, String inputPath, String outputPath, int startInstance,
			int stopInstance, HashMap<String, String> domainParams, boolean overwriteFile);

}
