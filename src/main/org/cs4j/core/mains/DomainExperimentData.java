package org.cs4j.core.mains;

import org.cs4j.core.SearchDomain;
import org.cs4j.core.domains.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Roni Stern on 24/02/2017.
 *
 * A helper class to hold information needed to run experiments on a given domain
 */
public class DomainExperimentData
{
    public static Map<Class<? extends SearchDomain>,DomainExperimentData> domainToExperimentData;
    static{
        domainToExperimentData = new HashMap<Class<? extends SearchDomain>,DomainExperimentData>();

        domainToExperimentData.put(FifteenPuzzle.class,
                new DomainExperimentData("./input/FifteenPuzzle/states15",
                        "./results/FifteenPuzzle/",1,100));
        domainToExperimentData.put(Pancakes.class,
                new DomainExperimentData("./input/pancakes/generated-40",
                        "./results/pancakes/",1,100));
        domainToExperimentData.put(GridPathFinding.class,
                new DomainExperimentData("./input/GridPathFinding/brc202d.map",
                        "./results/GridPathFinding/",1,100));
        domainToExperimentData.put(VacuumRobot.class,
                new DomainExperimentData("./input/vacuumrobot/generated-5-dirt",
                        "./results/VacuumRobot/",1,100));
        domainToExperimentData.put(DockyardRobot.class,
                new DomainExperimentData("./input/dockyard-robot-max-edge-2-out-of-place-30",
                        "./results/dockyard-robot-max-edge-2-out-of-place-30/",1,100));

    }

    public static DomainExperimentData get(Class<? extends SearchDomain> domainClass){
        return domainToExperimentData.get(domainClass);
    }

    public String inputPath;
    public String outputPath;
    public int fromInstance;
    public int  toInstance;
    public DomainExperimentData(String inputPath,String outputPath,
                          int fromInstance, int toInstance){
        this.inputPath=inputPath; this.outputPath=outputPath;
        this.fromInstance=fromInstance; this.toInstance=toInstance;
    }
}
