/*******************************************************************************
 * Copyright (C) 2016 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.parameters;

import crx.converter.engine.common.CorrelationRef;
import eu.ddmore.libpharmml.dom.commontypes.Matrix;
import eu.ddmore.libpharmml.dom.commontypes.StandardAssignable;
import eu.ddmore.libpharmml.dom.modeldefn.ParameterRandomVariable;
/**
 * Wrapper for Correlation Reference from common converter. This gives access to correlationRef fields via getters and helps testing.
 */
public class CorrelationsWrapper {

    private final CorrelationRef correlation;

    CorrelationsWrapper(CorrelationRef correlation){
        this.correlation = correlation;
    }

    public ParameterRandomVariable getFirstParamRandomVariable(){
        return correlation.rnd1;
    }

    public ParameterRandomVariable getSecondParamRandomVariable(){
        return correlation.rnd2;
    }

    public StandardAssignable getCorrelationCoefficient(){
        return correlation.correlationCoefficient;
    }

    public StandardAssignable getCovariance(){
        return correlation.covariance;
    }

    public Matrix getMatrix(){
        return correlation.matrix;
    }

    public boolean isMatrix(){
        return correlation.is_matrix;
    }

    public boolean isPairwise(){
        return correlation.is_pairwise;
    }

    public boolean isCorrelationCoeff(){
        return correlation.isCorrelation();
    }

    public boolean isCovariance(){
        return correlation.isCovariance();
    }

    public CorrelationRef getCorrelation(){
        return correlation;
    }
}
