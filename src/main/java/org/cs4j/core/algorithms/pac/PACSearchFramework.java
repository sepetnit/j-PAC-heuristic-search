package org.cs4j.core.algorithms.pac;

import org.cs4j.core.AnytimeSearchAlgorithm;
import org.cs4j.core.SearchAlgorithm;
import org.cs4j.core.SearchDomain;
import org.cs4j.core.SearchResult;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

/**
 * A simple framework for finding PAC solutions.
 * Requires an anytime search algorith and a PAC condition.
 * Created by Roni Stern on 23/02/2017.
 */
public class PACSearchFramework implements SearchAlgorithm {
    private static final Map<String, Class> POSSIBLE_PARAMETERS;
    private double epsilon; // Desired suboptimality
    private double delta; // Required confidence
    private Class pacConditionClass; // The stopping conditions used to verify a PAC solution
    private Class anytimeSearchClass; // The class of the anytime search algorithm to use


    // Declare the parameters that can be tunes before running the search
    static
    {
        POSSIBLE_PARAMETERS = new HashMap<>();
        POSSIBLE_PARAMETERS .put("epsilon", Double.class);
        POSSIBLE_PARAMETERS .put("delta", Double.class);
        POSSIBLE_PARAMETERS .put("anytimeSearch", Class.class);
        POSSIBLE_PARAMETERS .put("pacCondition", Class.class);
    }

    @Override
    public Map<String, Class> getPossibleParameters() {
        return POSSIBLE_PARAMETERS;
    }

    @Override
    public void setAdditionalParameter(String parameterName, String value) {
        switch (parameterName) {
            case "epsilon": {
                this.epsilon = Double.parseDouble(value);
                if (this.epsilon < 0.0d) {
                    System.out.println("[ERROR] The epsilon must be >= 0.0");
                    throw new IllegalArgumentException();
                }
                break;
            }
            case "delta": {
                this.delta = Double.parseDouble(value);
                if (this.delta < 0.0d) {
                    System.out.println("[ERROR] The delta must be >= 0.0");
                    throw new IllegalArgumentException();
                }
                break;
            }
            case "anytimeSearch":
                try {
                    this.anytimeSearchClass = Class.forName(value);
                } catch (ClassNotFoundException e) {
                    throw new IllegalArgumentException("Anytime Search Algorithm "+value+" is not a valid class name");
                }
                if (AnytimeSearchAlgorithm.class.isAssignableFrom(anytimeSearchClass)==false)
                    throw new IllegalArgumentException("Anytime Search Algorithm "+value+
                            " does not implement "+AnytimeSearchAlgorithm.class.getName());
                break;
            case "pacCondition":
                try {
                    this.pacConditionClass = Class.forName(value);
                } catch (ClassNotFoundException e) {
                    throw new IllegalArgumentException("PAC condition "+value+" is not a valid class name");
                }
                if (PACCondition.class.isAssignableFrom(this.pacConditionClass)==false)
                    throw new IllegalArgumentException("PAC condition  "+value+
                            " does not implement "+PACCondition.class.getName());
                break;
            default: {
                System.err.println("No such parameter: " + parameterName + " (value: " + value + ")");
                throw new NotImplementedException();
            }
        }
    }

    @Override
    public String getName() {
        return "PACSF("+this.anytimeSearchClass.getSimpleName()+","+this.pacConditionClass.getSimpleName()+")";
    }

    /**
     * This simple PAC Search framework, as described in the original PAC Search paper
     * @param domain The domain to apply the search on
     */
    @Override
    public SearchResult search(SearchDomain domain) {
        AnytimeSearchAlgorithm anytimeSearchAlgorithm=this.createAnytimeSearchAlgorithm();
        PACCondition pacCondition = this.createPacCondition(domain,this.epsilon,this.delta);
        if(anytimeSearchAlgorithm instanceof AnytimePACSearch)
            ((AnytimePACSearch)anytimeSearchAlgorithm).setPacCondition(pacCondition);

        // Run an anytime search
        SearchResult result = anytimeSearchAlgorithm.search(domain);
        if(result.hasSolution()==false) return result;

        // Check solution after it is found to see if we should halt
        SearchResult incumbent=result;
        try {
            while (pacCondition.shouldStop(result) == false) {
                result = anytimeSearchAlgorithm.continueSearch();
                if (result.hasSolution() == false) return incumbent;
                incumbent = result;
            }
        }catch(PACConditionSatisfied conditionSatisfied){
            // This part is designed for the search-aware PAC conditions
            incumbent = anytimeSearchAlgorithm.getTotalSearchResults();
            if(incumbent.hasSolution())
                return incumbent;
        }
        return incumbent;
    }


    /**
     * Create an anytimeSearchAlgorithm instance
     */
    private AnytimeSearchAlgorithm createAnytimeSearchAlgorithm()
    {
        // Create the anytime search algorithm
        try {
            Constructor constructor = this.anytimeSearchClass.getConstructor();
            return (AnytimeSearchAlgorithm) (constructor.newInstance());
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Create an pac condition instance
     */
    private PACCondition createPacCondition(SearchDomain domain, double epsilon, double delta)
    {
        // Create the anytime search algorithm
        try {
            Constructor constructor = this.pacConditionClass.getConstructor();
            PACCondition pacCondition = (PACCondition) (constructor.newInstance());
            pacCondition.setup(domain, epsilon,delta);
            return pacCondition;
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
