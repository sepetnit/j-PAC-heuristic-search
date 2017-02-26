package org.cs4j.core.experiments;

import java.io.*;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.cs4j.core.*;
import org.cs4j.core.algorithms.AnytimePTS;
import org.cs4j.core.algorithms.SearchResultImpl;
import org.cs4j.core.algorithms.pac.*;
import org.cs4j.core.domains.*;
import org.cs4j.core.mains.DomainExperimentData;

/**
 * This class runs PAC search experiments. It assumes the pre-process is given.
 */
public class PACOnlineExperimentRunner{
	final static Logger logger = Logger.getLogger(PACOnlineExperimentRunner.class);

	/**
	 * Extract from SearchResults the data to output to the file
	 * @param resultsData the results data object to update with data from the SearchResults
	 * @param result the SearchResults object to extract from
	 */
	protected void setResultsData(double[] resultsData, SearchResult result) {
		resultsData[1] = 1;
		resultsData[2] = result.getBestSolution().getLength();
		resultsData[3] = result.getBestSolution().getCost();
		resultsData[4] = result.getGenerated();
		resultsData[5] = result.getExpanded();
		resultsData[6] = result.getCpuTimeMillis();
		resultsData[7] = result.getWallTimeMillis();
	}


	public void run(Class domainClass, SearchAlgorithm alg, String inputPath, String outputPath, int startInstance,
					int stopInstance, HashMap<String, String> domainParams) {
		String[] resultColumnNames = {"InstanceID", "Found",
				"Depth", "Cost", "Generated", "Expanded", "Cpu Time", "Wall Time"};
		OutputResult output = null;
		SearchDomain domain;
		SearchResult result;
		double[] resultsData;

		Constructor<?> cons = ExperimentUtils.getSearchDomainConstructor(domainClass);
		try {
			// Write headers of output table
			output = new OutputResult(outputPath + alg.getName(), null, -1, -1, null, false, true);
			String toPrint = String.join(",", resultColumnNames);
			output.writeln(toPrint);

			logger.info("Solving " + domainClass.getName() + "\tAlg: " + alg.getName());
			// search on this domain and algo and weight the 100 instances
			for (int i = startInstance; i <= stopInstance; ++i) {
				try {
					// Read domain from file
					resultsData = new double[resultColumnNames.length];
					resultsData[0] = i;

					logger.info("\rSolving " + domainClass.getName() + "\t instance " + i + "\t Alg: " + alg.getName());
					domain = ExperimentUtils.getSearchDomain(inputPath, domainParams, cons, i);
					result = alg.search(domain);
					logger.info("Solution found? " + result.hasSolution());

					resultsData = new double[resultColumnNames.length];
					setResultsData(resultsData, result);
					output.appendNewResult(resultsData);
					output.newline();
				} catch (OutOfMemoryError e) {
					logger.info("OutOfMemory in:" + alg.getName() + " on:" + domainClass.getName());
				} catch (FileNotFoundException e) {
					logger.info("[INFO] FileNotFoundException At inputPath:" + inputPath);
					i = stopInstance; // @TODO: Is this just a replacement for a break?
				}
			}
		} catch (IOException e) {
			logger.error("[INFO] IOException At outputPath:" + outputPath);
			e.printStackTrace();
		} finally {
			output.close();
		}
	}

	/**
	 * Load PAC statistics for the given domain
	 * @param domainClass the class of domains for which to load statistics
	 */
	public void loadPACStatistics(Class domainClass)
	{
		PACStatistics pacStatistics = new PACStatistics();
		pacStatistics.instanceToInitialH = PACUtils.getInitialHValues(domainClass);
		pacStatistics.instanceToOptimal= PACUtils.getOptimalSolutions(domainClass);
		PACUtils.setStatisticFile(null,domainClass,pacStatistics);
	}

	/**
	 * Create a single CSV file for each domain with the different epsilon values
	 * @param domains the domains to collect data from
	 */
	public void collectResults(Class[] domains, Class[] pacConditions) throws IOException
	{
		//@TODO: GAL: CODE BELOW IS NOT WORKING AND IS MAINLY COPY PASTE. YOU CAN JUST IGNORE IT
		for(Class domainClass : domains){
			File domainDir = new File(DomainExperimentData.get(domainClass).outputPath);
			// Returns files starting with e=
			File[] foundFiles = domainDir.listFiles((dir1, name) -> name.startsWith("eps="));

			// Create domain output file
			File outputFile = new File(DomainExperimentData.get(domainClass).outputPath+"summary.csv");
			BufferedReader reader;
			BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
			String line;
			boolean firstEpsilon=true;
			String[] parts;
			String epsilon;
			for (File file : foundFiles) {
				// Extract epsilon
				parts = file.getName().split("=");
				parts = parts[parts.length-1].split("-");
				epsilon = parts[0];

				// Read file
				reader = new BufferedReader(new FileReader(file));

				// Handle the headers line
				line = reader.readLine();
				if(firstEpsilon){ // If first epsilon for this domain, write the header line
					writer.write(line+",epsilon");
					writer.newLine();
					firstEpsilon=false;
				}

				line = reader.readLine();
				while(line!=null){
					writer.write(line+epsilon); // @TODO: Why we don't need a comma here?
					writer.newLine();
					line = reader.readLine();
				}
				reader.close();
			}
			writer.close();
		}
	}
		/**
     * An example of using ExperimentRunner
     * @param args
     */
    public static void main(String[] args) throws IOException {
		//Class[] domains = {GridPathFinding.class, DockyardRobot.class, FifteenPuzzle.class,Pancakes.class, VacuumRobot.class};
		Class[] domains = {GridPathFinding.class};//, DockyardRobot.class, FifteenPuzzle.class,Pancakes.class, VacuumRobot.class};
		Class[] pacConditions = {TrivialPACCondition.class,RatioBasedPACCondition.class,FMinCondition.class};
		double[] epsilons = {0, 0.1, 0.5};
		double[] deltas = {0,0.1, 0.5};
		HashMap domainParams = new HashMap<>();

		SearchAlgorithm pacSearch = new PACSearchFramework();
		pacSearch.setAdditionalParameter("delta","1");
		pacSearch.setAdditionalParameter("anytimeSearch",AnytimePTS4PAC.class.getName());
		pacSearch.setAdditionalParameter("pacCondition",TrivialPACCondition.class.getName());


		PACOnlineExperimentRunner runner = new PACOnlineExperimentRunner();
		for(Class domainClass : domains) {
			logger.info("Running anytime for class " + domainClass.getName());
			runner.loadPACStatistics(domainClass);
			for (Class pacConditionClass : pacConditions){
				for (double epsilon : epsilons) {
					for (double delta : deltas) {
						pacSearch.setAdditionalParameter("epsilon", "" + epsilon);
						pacSearch.setAdditionalParameter("delta", "" + delta);
						runner.run(domainClass, pacSearch,
								DomainExperimentData.get(domainClass).inputPath,
								DomainExperimentData.get(domainClass).outputPath + pacConditionClass.getSimpleName()+"/e=" + epsilon + ",d=" + delta,
								DomainExperimentData.get(domainClass).fromInstance,
								DomainExperimentData.get(domainClass).toInstance,
								domainParams);
					}
				}
			}
		}

		//@TODO: GAL, IF YOU CAN IMPLEMENT THIS SO IT RETURNS A SINGLE CSV FILE WITH ALL THE RESULTS, THAT'D BE GREAT
		// (OF COURSE, ADDING COLUMNS FOR EPSILON, DELTA, AND PAC-CONDITION-NAME
		runner.collectResults(domains,pacConditions);
    }
}
