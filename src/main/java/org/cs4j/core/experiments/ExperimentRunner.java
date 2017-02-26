package org.cs4j.core.experiments;

import org.cs4j.core.OutputResult;
import org.cs4j.core.SearchAlgorithm;
import org.cs4j.core.SearchDomain;
import org.cs4j.core.SearchResult;
import org.cs4j.core.algorithms.AnytimePTS;
import org.cs4j.core.domains.FifteenPuzzle;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by user on 23/02/2017.
 */
public abstract class ExperimentRunner {
   
	
	public abstract double[] run(Class domainClass, SearchAlgorithm alg, String inputPath, String outputPath, int startInstance,
			int stopInstance, HashMap<String, String> domainParams, boolean overwriteFile);






}
