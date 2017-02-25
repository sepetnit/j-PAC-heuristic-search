package og.cs4j.core.experiments;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.cs4j.core.OutputResult;
import org.cs4j.core.SearchAlgorithm;
import org.cs4j.core.SearchDomain;
import org.cs4j.core.SearchResult;
import org.cs4j.core.algorithms.AStar;
import org.cs4j.core.algorithms.AnytimePTS;
import org.cs4j.core.algorithms.pac.FMinCondition;
import org.cs4j.core.algorithms.pac.PACSearchFramework;
import org.cs4j.core.domains.GridPathFinding;
import org.cs4j.core.mains.DomainExperimentData;

public class PreprocessRunner extends ExperimentRunner {

	final static Logger logger = Logger.getLogger(PreprocessRunner.class);

	@Override
	public double[] run(Class domainClass, SearchAlgorithm alg, String inputPath, String outputPath, int startInstance,
			int stopInstance, HashMap<String, String> domainParams, boolean overwriteFile) {

		logger.info("init experiment");
		OutputResult output = null;
		try {
			SearchDomain domain;
			SearchResult result;

			String[] resultColumnNames = { "InstanceID", "h*(s)", "h(s)", "h*/h" };
			try {
				output = new OutputResult(outputPath + alg.getName(), "Preprocess_", -1, -1, null, false,
						overwriteFile);
			} catch (IOException e1) {
				logger.error("FAILED to instantiate output result", e1);
			}
			double[] resultsData;

			Constructor<?> cons = getSearchDomainConstructor(domainClass);
			String toPrint = String.join(",", resultColumnNames);

			output.writeln(toPrint);

			logger.info("Start running search for " + (stopInstance - startInstance + 1) +" instances:");
			for (int i = startInstance; i <= stopInstance; ++i) {
				try {
					logger.info("Running the " + i +"'th instance");
					// Read domain from file
					resultsData = new double[resultColumnNames.length];
					domain = getSearchDomain(inputPath, domainParams, cons, i);
					result = alg.search(domain);
					logger.info("Solution found? " + result.hasSolution());

					setResultsData(result, resultsData, i);
					output.appendNewResult(resultsData);
					output.newline();

				} catch (OutOfMemoryError e) {
					logger.error("PreprocessRunner OutOfMemory :-( ", e);
					logger.error("OutOfMemory in:" + alg.getName() + " on:" + domainClass.getName());
				}
			}
		} catch (IOException e1) {
		} finally {
			output.close();
		}
		return null;
	}

	private void setResultsData(SearchResult result, double[] resultsData, int i) {
		int instanceId = i;
		double cost = result.getSolutions() != null && !result.getSolutions().isEmpty()? result.getSolutions().get(0).getCost() : -1;
		double initialH = result.getInitialH();
		double suboptimality = cost / initialH;
		
		resultsData[0] = instanceId;
		resultsData[1] = cost;
		resultsData[2] = initialH;
		resultsData[3] = suboptimality;
	}

	/**
	 * An example of using ExperimentRunner
	 * 
	 * @param args
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void main(String[] args) {

		ExperimentRunner runner = new PreprocessRunner();
		SearchAlgorithm algorithm = new AStar(); // default constructor -->
													// weight=1 (meaning its
													// like A*)
		HashMap domainParams = new HashMap<>();


		double epsilon = 0.0;
//		SearchAlgorithm pacSearch = new PACSearchFramework();
//        pacSearch.setAdditionalParameter("delta","1");
//        pacSearch.setAdditionalParameter("pacCondition",FMinCondition.class.getName());
		Class<GridPathFinding> domainClass = GridPathFinding.class;
		runner.run(domainClass, algorithm,
                DomainExperimentData.get(domainClass).inputPath,
                DomainExperimentData.get(domainClass).outputPath+"eps="+epsilon+"-",
                DomainExperimentData.get(domainClass).fromInstance,
                DomainExperimentData.get(domainClass).toInstance,
                domainParams, true);
	}

}
