package org.cs4j.core.algorithms.pac;

/**
 * This exception is fired when a PAC condition is satisfied
 */
public class PACConditionSatisfied extends RuntimeException{
    public PACCondition conditionSatisfied;

    public PACConditionSatisfied(PACCondition conditionSatisfied){
        super();
        this.conditionSatisfied=conditionSatisfied;
    }
}
