package org.cs4j.core.algorithms.pac;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cs4j.core.algorithms.SearchResultImpl;
import org.cs4j.core.domains.GridPathFinding;
import org.cs4j.core.algorithms.AbstractAnytimeSearch;

/**
 * Created by user on 26/02/2017.
 */
public abstract class AnytimePACSearch extends AbstractAnytimeSearch {
    private final static Logger logger = LogManager.getLogger(GridPathFinding.class);

    protected PACCondition pacCondition;


    public void setPacCondition(PACCondition pacCondition){
        this.pacCondition = pacCondition;
    }


    @Override
    public void setAdditionalParameter(String parameterName, String value) {
        switch (parameterName) {
            default: {
                logger.error("No such parameter: " + parameterName + " (value: " + value + ")");
                throw new UnsupportedOperationException();
            }
        }
    }

    /**
     * The internal main search procedure
     *
     * @return The search result filled by all the results of the search
     */
    protected SearchResultImpl _search() {

        try{
            return super._search();
        }
        catch(PACConditionSatisfied exception){
            // Stop the timer and check that a goal was found
            result.stopTimer();

            // If a goal was found: update the solution
            result.setExtras("fmin",this.maxFmin); // Record the lower bound for future analysis @TODO: Not super elegant
            result.setExtras("pac-condition-statisfied",exception.conditionSatisfied); // Record the lower bound for future analysis @TODO: Not super elegant
            return result;
        }
    }


    /**
     * If there are no more nodes with the old fmin, need to update fmin and maybe also maxfmin accordingly.
     */
    @Override
    protected void updateFmin(){
        // If fmin is no longer fmin, need to search for a new fmin @TODO: May improve efficiency
        if(this.fCounter.containsKey(fmin)==false){
            fmin=Double.MAX_VALUE;
            for(double fInOpen : this.fCounter.keySet()){
                if(fInOpen<fmin)
                    fmin=fInOpen;
            }
            if(maxFmin<fmin) {
                maxFmin = fmin;
                this.totalSearchResults.getExtras().put("fmin", maxFmin);
                if(this.pacCondition.shouldStop(this.totalSearchResults)) {
                    this.totalSearchResults.increase(this.result);
                    this.totalSearchResults.stopTimer();
                    throw new PACConditionSatisfied(this.pacCondition);
                }
            }
        }
    }


}
