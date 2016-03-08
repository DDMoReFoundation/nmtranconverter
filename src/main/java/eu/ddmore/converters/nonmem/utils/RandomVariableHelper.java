/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.utils;

import com.google.common.base.Preconditions;

import eu.ddmore.libpharmml.dom.modeldefn.ParameterRandomVariable;
import eu.ddmore.libpharmml.dom.uncertml.AbstractContinuousUnivariateDistributionType;
import eu.ddmore.libpharmml.dom.uncertml.NormalDistribution;
import eu.ddmore.libpharmml.dom.uncertml.PositiveRealValueType;

/**
 * Helps to get required information from random variables.
 *  
 */
public class RandomVariableHelper {

    /**
     * Returns parameter name from parameter random variable. 
     * The name can be obtained from either standard deviation or variance.
     * This method will return null if name is not found in random variable provided.
     *  
     * @param rv
     * @return
     */
    public static String getNameFromParamRandomVariable(ParameterRandomVariable rv) {
        Preconditions.checkNotNull(rv, "Parameter random variable cannot be null.");
        String symbId = null;
        if (getDistributionTypeStdDev(rv) != null) {
            symbId = getDistributionTypeStdDev(rv).getVar().getVarId();
        } else if (getDistributionTypeVariance(rv) != null) {
            if(getDistributionTypeVariance(rv).getVar()!=null){
                symbId = getDistributionTypeVariance(rv).getVar().getVarId();
            }
        }
        return symbId;
    }

    /**
     * Identifies if current parameter is from Standard Deviation.
     * 
     * @param rv
     * @return
     */
    public static Boolean isParamFromStdDev(ParameterRandomVariable rv) {
        Preconditions.checkNotNull(rv, "Parameter random variable cannot be null.");
        if (getDistributionTypeStdDev(rv) != null) {
            return true;
        } else if (getDistributionTypeVariance(rv) != null) {
            return false;
        }else{
            throw new IllegalStateException("Distribution type for variable "+rv.getSymbId()+" is unknown");
        }
    }

    /**
     * Get distribution type from standard deviation.
     * @param rv
     * @return
     */
    public static PositiveRealValueType getDistributionTypeStdDev(ParameterRandomVariable rv){
        Preconditions.checkNotNull(rv, "Parameter random variable cannot be null.");
        final AbstractContinuousUnivariateDistributionType distributionType = rv.getDistribution().getUncertML().getAbstractContinuousUnivariateDistribution().getValue();  //rv.getAbstractContinuousUnivariateDistribution().getValue();
        
        if (distributionType instanceof NormalDistribution) {
            return ((NormalDistribution) distributionType).getStddev();
        }
        return null;
    }

    /**
     * Get distribution type from standard deviation.
     * @param rv
     * @return
     */
    public static PositiveRealValueType getDistributionTypeVariance(ParameterRandomVariable rv){
        Preconditions.checkNotNull(rv, "Parameter random variable cannot be null.");
        final AbstractContinuousUnivariateDistributionType distributionType = rv.getDistribution().getUncertML().getAbstractContinuousUnivariateDistribution().getValue();
        if (distributionType instanceof NormalDistribution) {
            return ((NormalDistribution) distributionType).getVariance();
        }
        return null;
    }

}
