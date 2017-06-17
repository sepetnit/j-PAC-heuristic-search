package org.cs4j.core.mains;

import jxl.Workbook;
import jxl.read.biff.BiffException;
import jxl.write.*;
import jxl.write.Number;
import jxl.write.biff.RowsExceededException;
import org.cs4j.core.SearchAlgorithm;
import org.cs4j.core.SearchDomain;
import org.cs4j.core.SearchResult;
import org.cs4j.core.algorithms.auxiliary.SearchResultImpl;
import org.cs4j.core.algorithms.kgoal.KBidirectionalAstar;
import org.cs4j.core.algorithms.kgoal.KxAstar;
import org.cs4j.core.domains.GridPathFinding;

import java.io.*;
import java.util.*;

/**
 * Created by user on 2017-04-03.
 *
 */
public class MainKGoalSearch {
    private SearchDomain gp;
    private SearchAlgorithm alg;

    // Constructor
    public void MainKxAstarSearch() {
        try {
            InputStream is = new FileInputStream(new File(
                    "input/gridpathfinding/k-goal/ost003d.map/1.in"));
            this.gp = new GridPathFinding(is);
            this.alg = new KxAstar();
            this.alg.search(this.gp);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    // Constructor
    public void MainKBidirectionalAstarSearch() {
        try {
            InputStream is = new FileInputStream(new File(
                    "input/gridpathfinding/k-goal/brc202d.map/1.in"));
            this.gp = new GridPathFinding(is);
            this.alg = new KBidirectionalAstar();
            SearchResult result = this.alg.search(this.gp);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }


    // The function returns the following map:
    // MapName+GoalsCount : <Alg1 -> AverageExpanded>, <Alg2 -> AverageExpanded> ...
    public Map<String, Double> runGridPathFindingExperiment(String mapName,
                                                            int goalsCount,
                                                            int instancesCount) throws
            FileNotFoundException {
        // AlgName, InstanceNum,
        Map<String, Double> toReturn = new TreeMap<>();

        // Algorithms
        List<Class<? extends SearchAlgorithm>> algs = new ArrayList<>();
        algs.add(KBidirectionalAstar.class);
        algs.add(KxAstar.class);

        String basicPathName =
                "input/gridpathfinding/k-goal/" + mapName + ".map/" +
                        goalsCount + "/{instance-index}.in";

        for (Class<? extends SearchAlgorithm> algCls: algs) {
            try {
                SearchAlgorithm alg = algCls.newInstance();
                System.out.println("Solving for alg : " + alg.getName());
                SearchResultImpl res = new SearchResultImpl();
                for (int instanceNum = 1; instanceNum < instancesCount; ++instanceNum) {
                    try {
                        InputStream is = new FileInputStream(
                                new File(basicPathName.replace("{instance-index}",
                                        instanceNum + "")
                                ));
                        GridPathFinding instance = new GridPathFinding(is);
                        SearchResult singleResult = alg.search(instance);
                        if (singleResult != null) {
                            res.addConcreteResult(singleResult);
                            System.out.println(singleResult.getExpanded());
                            res.setExpanded(res.getExpanded() + singleResult.getExpanded());
                        }
                    } catch (IOException e) {
                        throw e;
                    }
                }
                toReturn.put(alg.getName(), res.getAverageExpanded());
            }
            catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
               // TODO
               // e.printStackTrace();
            }
        }
        return toReturn;
    }

    private Map<Integer, Map<String, Double>> runGridPathFindingExperimentForGoalsCounts(
            String mapName, int[] goalsCounts, int instancesCount) throws FileNotFoundException {
                Map<Integer, Map<String, Double>> toReturn = new TreeMap<>();
        for (int goalsCount : goalsCounts) {
            toReturn.put(goalsCount, runGridPathFindingExperiment(mapName,
                    goalsCount, instancesCount));
        }
        return toReturn;
    }

    private Map<String, Map<Integer, Map<String, Double>>> runGridPathFindingExperimentOverMaps() throws FileNotFoundException {
        String[] mapsNames = new String[] {"brc202d", "ost003d", "den400d"};
        int[] goalsCounts = new int[] {2, 3};//, 4, 5, 10, 15, 20, 30, 50, 100};

        Map<String, Map<Integer, Map<String, Double>>> toReturn = new TreeMap<>();
        for (String mapName: mapsNames) {
            toReturn.put(mapName,
                    this.runGridPathFindingExperimentForGoalsCounts(mapName,
                            goalsCounts, 4));
        }
        return toReturn;
    }


    private static WritableCellFormat cellFormat1000 =
            new WritableCellFormat(new NumberFormat("#,###"));
    private static WritableCellFormat cellFormatDecimal =
            new WritableCellFormat(new NumberFormat("#,###.00"));

    private static void createSummary(Map<String, Map<Integer, Map<String, Double>>> summary)
            throws IOException {
        try {
            String path = "C:\\Users\\user\\Documents\\Work\\Git\\j-heuristic-search-galdreiman" +
                    "-branch\\input\\GridPathFinding\\k-goal\\result.xls";
            File summaryFile = new File(path);

            int sheetsCount = 0;
            WritableWorkbook writableWorkbook;
            if (summaryFile.exists()) {
                Workbook existingWorkbook = Workbook.getWorkbook(summaryFile);
                sheetsCount = existingWorkbook.getNumberOfSheets();
                writableWorkbook = Workbook.createWorkbook(summaryFile, existingWorkbook);
            } else {
                writableWorkbook = Workbook.createWorkbook(summaryFile);
            }

            WritableSheet writableSheet;
            Label label;

            // Final average of the following format: <goals-count => <alg-name, expanded>>
            Map<Integer, Map<String, Double>> averageExpandedStates = new TreeMap<>();

            for (String mapName : summary.keySet()) {
                // TODO: Sheet name
                WritableSheet existingSheet = writableWorkbook.getSheet(mapName);

                if (existingSheet != null) {
                    writableSheet = existingSheet;
                } else {
                    writableSheet = writableWorkbook.createSheet(mapName, sheetsCount++);
                }

                int currentRow = 1;

                // Get sorted goalsCounts list
                Set<Integer> goalsCountsSet = summary.get(mapName).keySet();
                List<Integer> goalsCounts =
                        Arrays.asList(goalsCountsSet.toArray(new Integer[goalsCountsSet.size()]));
                Collections.sort(goalsCounts);

                // Get all names of algorithms
                Set<String> algsNames = summary.get(mapName).get(goalsCounts.get(0)).keySet();
                for (String algName : algsNames) {
                    writableSheet.addCell(new Label(0, currentRow++, algName + ""));
                }

                int currentCol = 1;

                for (int goalsCount : goalsCounts) {
                    currentRow = 0;
                    // Add domain name
                    writableSheet.addCell(new Label(currentCol, currentRow++, goalsCount + ""));
                    for (String algName : algsNames) {
                        Map<String, Double> results = summary.get(mapName).get(goalsCount);
                        // Add the result
                        writableSheet.addCell(
                                new Number(currentCol, currentRow++,
                                        results.get(algName), cellFormatDecimal));

                        // Update average
                        Map<String, Double> currentGoalsCountData =
                                averageExpandedStates.getOrDefault(goalsCount, new TreeMap<>());
                        double currentExpanded =
                                currentGoalsCountData.getOrDefault(algName, 0.0);
                        currentExpanded += results.get(algName);
                        currentGoalsCountData.put(algName, currentExpanded);

                    }
                    currentCol++;
                }
            }

            WritableSheet existingSheet = writableWorkbook.getSheet("average");
            if (existingSheet != null) {
                writableSheet = existingSheet;
            } else {
                writableSheet = writableWorkbook.createSheet("average", sheetsCount++);
            }

            //for ()



            writableWorkbook.write();
            writableWorkbook.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (WriteException e) {
            e.printStackTrace();
        } catch (BiffException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        System.out.println("Test Started");

        MainKGoalSearch MKGS = new MainKGoalSearch();

        MKGS.createSummary(MKGS.runGridPathFindingExperimentOverMaps());
        //MKGS.MainKBidirectionalAstarSearch();

        System.out.println("Test Ended");
    }
}
