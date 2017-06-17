package org.cs4j.core.experiments;

import org.cs4j.core.SearchDomain;
import org.cs4j.core.domains.DomainExperimentData;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Roni Stern
 *
 * Created by Roni Stern on 26/02/2017.
 *
 * This class holds helper functions that are useful for running experiments
 */
public class ExperimentUtils {

    /**
     * The function creates a constructor for the search domain, to enable
     * creating search domains
     *
     * @param domainClass the class of the domain
     */
    static Constructor<?> getSearchDomainConstructor(Class domainClass) {
        Constructor<?> cons;
        try {
            cons = domainClass.getConstructor(InputStream.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        return cons;
    }

    /**
     * The function reads and setups a search domain
     *
     * @param inputPath Location of the search domain
     * @param domainParams Additional parameters
     * @param cons Constructor of the search domain
     * @param i index of problem instance
     *
     * @return A SearchDomain object
     *
     */
    static SearchDomain getSearchDomain(String inputPath,
                                        Map<String, String> domainParams,
                                        Constructor<?> cons,
                                        int i){
        InputStream is;
        SearchDomain domain;

        try {
            is = new FileInputStream(new File(inputPath + "/" + i + ".in" ));
            // Create the specified instance of the domain
            domain = (SearchDomain) cons.newInstance(is);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        // Set additional parameters to the domain
        for (Map.Entry<String, String> entry : domainParams.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            domain.setAdditionalParameter(key, value);
        }

        return domain;
    }


    /**
     * The function creates and returns the i's instance of the given search domain
     *
     * @param domainClass The class of the search domain to create
     * @param instance The instance to return
     *
     * @return The created instance
     */
    public static SearchDomain getSearchDomain(Class domainClass, int instance) {
        // Get the path in which the instances of the required domain are located
        String inputPath = DomainExperimentData.get(domainClass).inputPath;
        // Get the constructor of the found class
        Constructor constructor = ExperimentUtils.getSearchDomainConstructor(domainClass);
        // Create the i'th instance using the found constructor
        return ExperimentUtils.getSearchDomain(inputPath, new HashMap<>(),
                constructor, instance);
    }
}
