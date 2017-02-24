package org.cs4j.core.mains;

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
public class ExperimentRunner {
    public double[] run(Class domainClass, SearchAlgorithm alg,
                                  String inputPath,
                                  String outputPath,
                                  int startInstance,
                                  int stopInstance,
                                  HashMap<String,String> domainParams,
                                  boolean overwriteFile){
        double retArray[] = {0,0,0};//solved,generated,expanded
        String[] resultColumnNames = {"InstanceID", "Found", "Depth", "Cost" , "Generated", "Expanded", "Cpu Time", "Wall Time"};
        OutputResult output = null;
        double[] resultsData;
        SearchDomain domain;
        SearchResult result;
        int solvableNum = stopInstance-startInstance+1;
        boolean[] solvableInstances = new boolean[solvableNum];
        Arrays.fill(solvableInstances,true);

        Constructor<?> cons = getSearchDomainConstructor(domainClass);
        try {
            // Write headers of output table
            output = new OutputResult(outputPath+alg.getName(), null, -1, -1, null, false, overwriteFile);
            String toPrint = String.join(",", resultColumnNames);
            output.writeln(toPrint);

            System.out.println("Solving "+domainClass.getName() + "\tAlg: " + alg.getName());
            int found = 0;
            //search on this domain and algo and weight the 100 instances
            for (int i = startInstance; i <= stopInstance; ++i) {
                try {
                    // Read domain from file
                    resultsData = new double[resultColumnNames.length];
                    domain = getSearchDomain(inputPath, domainParams, cons, i);

                    System.out.print("\rSolving " + domainClass.getName() + " instance " + (found+1)
                            +"/"+ i +" ("+solvableNum+")\tAlg: " + alg.getName());
                    result = null;
                    if(solvableInstances[i-startInstance])
                        result = alg.search(domain);
                    if (result != null && result.hasSolution()) {
                        found++;
                        setResultsData(resultsData, result);
                    }
                    else if(solvableInstances[i-startInstance]){
                        solvableInstances[i-startInstance] = false;
                        solvableNum--;

                        int sul = 0;
                        for (boolean solvableInstance : solvableInstances) {
                            if (solvableInstance)
                                sul++;
                        }
                        if(sul!= solvableNum)
                            System.out.println("[WARNING] solvable num incorrect i:"+i);
                    }
                    resultsData[0] = i;
                    output.appendNewResult(resultsData);
                    output.newline();
                } catch (OutOfMemoryError e) {
                    System.out.println("[INFO] MainDaniel OutOfMemory :-( "+e);
                    System.out.println("[INFO] OutOfMemory in:"+alg.getName()+" on:"+ domainClass.getName());
                }
                catch (FileNotFoundException e) {
                    System.out.println("[INFO] FileNotFoundException At inputPath:"+inputPath);
                    i=stopInstance; // @TODO: Is this just a replacement for break?
                }
            }
        }  catch (IOException e) {
            System.out.println("[INFO] IOException At outputPath:"+outputPath);
            e.printStackTrace();
        }
        finally {
            output.close();
        }
        return retArray;
    }


    /**
     * Extract from SearchResults the data to output to the file
     * @param resultsData the results data object to update with data from the SearchResults
     * @param result the SearchResults object to extract from
     */
    private void setResultsData(double[] resultsData, SearchResult result) {
        resultsData[1] = 1;
        resultsData[2] = result.getSolutions().get(0).getLength();
        resultsData[3] = result.getSolutions().get(0).getCost();
        resultsData[4] = result.getGenerated();
        resultsData[5] = result.getExpanded();
        resultsData[6] = result.getCpuTimeMillis();
        resultsData[7] = result.getWallTimeMillis();
    }

    /**
     * Create a constructor for the search domain, to enable creating search domains for a 
     * @param domainClass the class of the domain
     */
    private Constructor<?> getSearchDomainConstructor(Class domainClass) {
        Constructor<?> cons = null;
        try {
            cons = domainClass.getConstructor(InputStream.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        return cons;
    }

    /**
     * Read and setup a search domain
     * @param inputPath Location of the search domain
     * @param domainParams Additional parameters
     * @param cons Constructor of the search domain
     * @param i index of problem instance
     * @return A SearchDomain object
     *
     */
    private SearchDomain getSearchDomain(String inputPath, HashMap<String, String> domainParams, Constructor<?> cons, int i){
        InputStream is = null;
        SearchDomain domain=null;
        try {
            is = new FileInputStream(new File(inputPath + "/" + i + ".in" ));
            domain = (SearchDomain) cons.newInstance(is);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        // Set additional parameters to the domain
        for(Map.Entry<String, String> entry : domainParams.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            domain.setAdditionalParameter(key,value);
        }
        return domain;
    }

    public double[] runWithAppend(Class domainClass, SearchAlgorithm alg,
                                    String inputPath,
                                    String outputPath,
                                    int startInstance,
                                    int stopInstance,
                                    HashMap<String,String> domainParams,
                                    boolean overwriteFile,
                                    boolean appendToFile){
        double retArray[] = {0,0,0};//solved,generated,expanded
        String[] resultColumnNames = {"InstanceID", "Found", "Depth", "Cost" , "Generated", "Expanded", "Cpu Time", "Wall Time"};
        OutputResult output = null;
        int solvableNum = stopInstance-startInstance+1;
        boolean[] solvableInstances = new boolean[solvableNum];
        Arrays.fill(solvableInstances,true);

        Constructor<?> cons = getSearchDomainConstructor(domainClass);
        try {
            String str;
            String[] lines = new String[0];
            String fname = outputPath+alg.getName()+".csv";
            File file = new File(fname);
            if(appendToFile && file.exists()){
                FileInputStream fis = new FileInputStream(file);
                byte[] data = new byte[(int) file.length()];
                fis.read(data);
                fis.close();
                str = new String(data, "UTF-8");
                lines = str.split("\n");
            }
            // Write headers of output table
            output = new OutputResult(outputPath+alg.getName(), null, -1, -1, null, false, overwriteFile);
            String toPrint = String.join(",", resultColumnNames);
            output.writeln(toPrint);

            System.out.println("Solving "+domainClass.getName() + "\tAlg: " + alg.getName());
            BufferedReader optimalReader = null;

            int found = 0;
            //search on this domain and algo and weight the 100 instances
            for (int i = startInstance; i <= stopInstance; ++i) {
                try {
                    // Read domain from file
                    double resultsData[] = new double[resultColumnNames.length];
                    SearchDomain domain = getSearchDomain(inputPath, domainParams, cons, i);
                    if(appendToFile && lines.length > i && lines[i].split(",").length == resultsData.length){
                        output.writeln(lines[i]);
                        String[] lineSplit = lines[i].split(",");
                        String f = lineSplit[1];
                        if(Double.parseDouble(f) == 1.0){
                            found++;
                        }
                        else if(solvableInstances[i-startInstance]){
                            solvableNum--;
                            solvableInstances[i-startInstance] = false;
                        }
                    }
                    else {
                        System.out.print("\rSolving " + domainClass.getName() + " instance " + (found+1)
                                +"/"+ i +" ("+solvableNum+")\tAlg: " + alg.getName());
                        SearchResult result = null;
                        if(solvableInstances[i-startInstance])
                            result = alg.search(domain);
                        if (result != null && result.hasSolution()) {
                            found++;
                            setResultsData(resultsData, result);
                        }
                        else if(solvableInstances[i-startInstance]){
                            solvableInstances[i-startInstance] = false;
                            solvableNum--;

                            int sul = 0;
                            for (boolean solvableInstance : solvableInstances) {
                                if (solvableInstance)
                                    sul++;
                            }
                            if(sul!= solvableNum)
                                System.out.println("[WARNING] solvable num incorrect i:"+i);
                        }
                        resultsData[0] = i;
                        output.appendNewResult(resultsData);
                        output.newline();
                    }
                } catch (OutOfMemoryError e) {
                    System.out.println("[INFO] MainDaniel OutOfMemory :-( "+e);
                    System.out.println("[INFO] OutOfMemory in:"+alg.getName()+" on:"+ domainClass.getName());
                }
                catch (FileNotFoundException e) {
                    System.out.println("[INFO] FileNotFoundException At inputPath:"+inputPath);
                    i=stopInstance;
//                    e.printStackTrace();
                }
            }
        }  catch (IOException e) {
            System.out.println("[INFO] IOException At outputPath:"+outputPath);
            e.printStackTrace();
        }
        finally {
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
        ExperimentRunner runner = new ExperimentRunner();
        SearchAlgorithm algorithm = new AnytimePTS();
        HashMap domainParams = new HashMap<>();

        runner.run(FifteenPuzzle.class,algorithm,
                "./input/FifteenPuzzle/states15",
                "./results/FifteenPuzzle/anytime",
                1,100,
                domainParams,true);
    }
}
