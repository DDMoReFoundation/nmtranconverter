/*******************************************************************************
 * Copyright (C) 2016 Mango Business Solutions Ltd, [http://www.mango-solutions.com]
*
* This program is free software: you can redistribute it and/or modify it under
* the terms of the GNU Affero General Public License as published by the
* Free Software Foundation, version 3.
*
* This program is distributed in the hope that it will be useful, 
* but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
* or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License 
* for more details.
*
* You should have received a copy of the GNU Affero General Public License along 
* with this program. If not, see <http://www.gnu.org/licenses/agpl-3.0.html>.
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
