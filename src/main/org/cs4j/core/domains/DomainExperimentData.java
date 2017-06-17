package org.cs4j.core.domains;

import org.cs4j.core.SearchDomain;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Roni Stern on 24/02/2017.
 *
 * A helper class to hold information needed to run experiments on a given domain
 */
public class DomainExperimentData {

    public String inputPath;
    public String outputPath;
    public int fromInstance;
    public int  toInstance;

    /**
     * The constructor of the class
     *
     * @param inputPath The path in which the instances are located
     * @param outputPath The path in which the results should be stored
     * @param fromInstance The minimum index of an instance located in the
     *                     inputPath folder
     * @param toInstance The maximum index of an instance located in the
     *                   inputPath folder
     */
    public DomainExperimentData(String inputPath,String outputPath,
                                int fromInstance, int toInstance){
        this.inputPath = inputPath;
        this.outputPath = outputPath;
        this.fromInstance = fromInstance;
        this.toInstance = toInstance;
    }

    // A map which contains all the possible domains
    public static Map<Class<? extends SearchDomain>, DomainExperimentData>
            domainToExperimentData;

    // A static constructor which adds all the classes to the experiments map
    static {
        domainToExperimentData = new HashMap<>();

        domainToExperimentData.put(
                FifteenPuzzle.class,
                new DomainExperimentData(
                        "./input/FifteenPuzzle/states15",
                        "./results/FifteenPuzzle/",
                        1,
                        100));

        domainToExperimentData.put(
                Pancakes.class,
                new DomainExperimentData(
                        "./input/pancakes/generated-40",
                        "./results/pancakes/",
                        1,
                        100));

        domainToExperimentData.put(
                GridPathFinding.class,
                new DomainExperimentData(
                        "./input/GridPathFinding/brc202d.map",
                        "./results/GridPathFinding/",
                        1,
                        100));

        domainToExperimentData.put(
                VacuumRobot.class,
                new DomainExperimentData(
                        "./input/vacuumrobot/generated-5-dirt",
                        "./results/VacuumRobot/",
                        1,
                        100));

        domainToExperimentData.put(
                DockyardRobot.class,
                new DomainExperimentData(
                        "./input/dockyard-robot-max-edge-2-out-of-place-30",
                        "./results/dockyard-robot-max-edge-2-out-of-place-30/",
                        1,
                        100));
    }

    /**
     * A method which takes data from the domainExperimentsData map
     *
     * @param domainClass The class to take from the map
     *
     * @return The taken data
     */
    public static DomainExperimentData get(Class<? extends SearchDomain> domainClass){
        return DomainExperimentData.domainToExperimentData.get(domainClass);
    }
}
