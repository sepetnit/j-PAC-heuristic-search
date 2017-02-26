package org.cs4j.core.mains;

import org.cs4j.core.SearchAlgorithm;
import org.cs4j.core.algorithms.AnytimePTS;
import org.cs4j.core.algorithms.pac.FMinCondition;
import org.cs4j.core.algorithms.pac.PACSearchFramework;
import org.cs4j.core.domains.*;

import org.cs4j.core.experiments.ExperimentRunner;
import org.cs4j.core.experiments.PACOnlineExperimentRunner;

import java.io.*;
import java.util.HashMap;


/**
 * Created by user on 23/02/2017.
 */
public class PACMain {


    @SuppressWarnings({ "unchecked", "rawtypes" })
	public static void main(String[] args) throws IOException
    {
        ExperimentRunner runner = new PACOnlineExperimentRunner();
        HashMap domainParams = new HashMap<>();
        double[] epsilons = {2,1.5, 1, 0.75, 0.5, 0.25, 0.1, 0.01, 0}; // Remember these are epsilons, not the 1+eps
        Class[] domains = {
                Pancakes.class,
                FifteenPuzzle.class,
                VacuumRobot.class,
                DockyardRobot.class,
                GridPathFinding.class
        };

        // Setup the search parameters
        SearchAlgorithm pacSearch = new PACSearchFramework();
        pacSearch.setAdditionalParameter("delta","1");
        pacSearch.setAdditionalParameter("anytimeSearch",AnytimePTS.class.getName());
        pacSearch.setAdditionalParameter("pacCondition",FMinCondition.class.getName());

        for(double epsilon : epsilons)
        {
            for(Class domainClass: domains) {
                pacSearch.setAdditionalParameter("epsilon", "" + epsilon);
                System.out.println("Running "+ domainClass.getSimpleName() + " with eps="+epsilon);
                runner.run(domainClass, pacSearch,
                        DomainExperimentData.get(domainClass).inputPath,
                        DomainExperimentData.get(domainClass).outputPath+"eps="+epsilon+"-",
                        DomainExperimentData.get(domainClass).fromInstance,
                        DomainExperimentData.get(domainClass).toInstance,
                        domainParams, true);
            }
        }

        collectResults(domains);
    }



    /**
     * Create a single CSV file for each domain with the different epsilon values
     * @param domains the domains to collect data from
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
	public static void collectResults(Class[] domains) throws IOException
    {
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
}
