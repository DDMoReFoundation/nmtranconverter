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
package eu.ddmore.converters.nonmem.statements.model;

import com.google.common.base.Preconditions;

import eu.ddmore.converters.nonmem.ConversionContext;

/**
 * This class creates model statement. It adds associated statement blocks for continuous as well as discrete statements. 
 */
public class ModelStatement {

    private final ConversionContext context;
    private ContinuousStatement continuousStatement;
    private DiscreteStatement discreteStatement;
    private NonDerivativePredStatement nonDerivativePredStatement;

    public ModelStatement(ConversionContext context){
        Preconditions.checkNotNull(context,"Conversion context cannot be null");
        this.context= context;
        initialise();
    }

    private void initialise(){
        continuousStatement = new ContinuousStatement(context);
        discreteStatement = new DiscreteStatement(context.getModelStatementHelper());
        nonDerivativePredStatement = new NonDerivativePredStatement(context.getModelStatementHelper());
    }

    /**
     * Gets model statement using conversion context for nmtran
     * @return model statement
     */
    public StringBuilder getModelStatement(){

        StringBuilder predStatement = new StringBuilder();

        if(context.getDiscreteHandler().isDiscrete()){
            //Discrete
            if(context.getDiscreteHandler().isCountData()){
                predStatement.append(discreteStatement.getModelStatementForCountData(context.getDiscreteHandler()));
            }
            else if(context.getDiscreteHandler().isTimeToEventData()){
                predStatement.append(discreteStatement.buildModelStatementForTTE(context.getDiscreteHandler()));
            }
            else if(context.getDiscreteHandler().isCategoricalData()){
                //TODO: add support for categorical data
            }
        }else if(!context.getDerivativeVars().isEmpty()){
            //DES
            predStatement.append(continuousStatement.buildContinuousStatement());
        }else{
            //PRED
            predStatement.append(nonDerivativePredStatement.getPredStatement());
        }
        return new StringBuilder(predStatement.toString().toUpperCase());

    }
}
