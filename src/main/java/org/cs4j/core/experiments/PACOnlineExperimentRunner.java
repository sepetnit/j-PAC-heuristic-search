package org.cs4j.core.experiments;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.HashMap;

import org.cs4j.core.OutputResult;
import org.cs4j.core.SearchAlgorithm;
import org.cs4j.core.SearchDomain;
import org.cs4j.core.SearchResult;
import org.cs4j.core.algorithms.AnytimePTS;
import org.cs4j.core.domains.FifteenPuzzle;

/**
 * This class runs PAC search experiments. It assumes the pre-process is given.
 */
public class PACOnlineExperimentRunner extends ExperimentRunner{

	/**
	 * Extract from SearchResults the data to output to the file
	 * @param resultsData the results data object to update with data from the SearchResults
	 * @param result the SearchResults object to extract from
	 */
	protected void setResultsData(double[] resultsData, SearchResult result) {
		resultsData[1] = 1;
		resultsData[2] = result.getSolutions().get(0).getLength();
		resultsData[3] = result.getSolutions().get(0).getCost();
		resultsData[4] = result.getGenerated();
		resultsData[5] = result.getExpanded();
		resultsData[6] = result.getCpuTimeMillis();
		resultsData[7] = result.getWallTimeMillis();
	}

	@Override
	public double[] run(Class domainClass, SearchAlgorithm alg, String inputPath, String outputPath, int startInstance,
			int stopInstance, HashMap<String, String> domainParams, boolean overwriteFile) {
		double retArray[] = { 0, 0, 0 };// solved,generated,expanded
		String[] resultColumnNames = { "InstanceID", "Found", "Depth", "Cost", "Generated", "Expanded", "Cpu Time",
				"Wall Time" };
		OutputResult output = null;
		double[] resultsData;
		SearchDomain domain;
		SearchResult result;
		int solvableNum = stopInstance - startInstance + 1;
		boolean[] solvableInstances = new boolean[solvableNum];
		Arrays.fill(solvableInstances, true);

		Constructor<?> cons = ExperimentUtils.getSearchDomainConstructor(domainClass);
		try {
			// Write headers of output table
			output = new OutputResult(outputPath + alg.getName(), null, -1, -1, null, false, overwriteFile);
			String toPrint = String.join(",", resultColumnNames);
			output.writeln(toPrint);

			System.out.println("Solving " + domainClass.getName() + "\tAlg: " + alg.getName());
			int found = 0;
			// search on this domain and algo and weight the 100 instances
			for (int i = startInstance; i <= stopInstance; ++i) {
				try {
					// Read domain from file
					resultsData = new double[resultColumnNames.length];
					domain = ExperimentUtils.getSearchDomain(inputPath, domainParams, cons, i);

					System.out.print("\rSolving " + domainClass.getName() + " instance " + (found + 1) + "/" + i + " ("
							+ solvableNum + ")\tAlg: " + alg.getName());
					result = null;
					if (solvableInstances[i - startInstance])
						result = alg.search(domain);
					if (result != null && result.hasSolution()) {
						found++;
						setResultsData(resultsData, result);
					} else if (solvableInstances[i - startInstance]) {
						solvableInstances[i - startInstance] = false;
						solvableNum--;

						int sul = 0;
						for (boolean solvableInstance : solvableInstances) {
							if (solvableInstance)
								sul++;
						}
						if (sul != solvableNum)
							System.out.println("[WARNING] solvable num incorrect i:" + i);
					}
					resultsData[0] = i;
					output.appendNewResult(resultsData);
					output.newline();
				} catch (OutOfMemoryError e) {
					System.out.println("[INFO] MainDaniel OutOfMemory :-( " + e);
					System.out.println("[INFO] OutOfMemory in:" + alg.getName() + " on:" + domainClass.getName());
				} catch (FileNotFoundException e) {
					System.out.println("[INFO] FileNotFoundException At inputPath:" + inputPath);
					i = stopInstance; // @TODO: Is this just a replacement for
										// break?
				}
			}
		} catch (IOException e) {
			System.out.println("[INFO] IOException At outputPath:" + outputPath);
			e.printStackTrace();
		} finally {
			output.close();
		}
		return retArray;
	}


    /**
     * An example of using ExperimentRunner
     * @param args
     */
    public static void main(String[] args)
    {
        ExperimentRunner runner = new PACOnlineExperimentRunner();
        SearchAlgorithm algorithm = new AnytimePTS();
        HashMap domainParams = new HashMap<>();

        runner.run(FifteenPuzzle.class,algorithm,
                "./input/FifteenPuzzle/states15",
                "./results/FifteenPuzzle/anytime",
				1,5,
//                1,100,
                domainParams,true);
    }

}
