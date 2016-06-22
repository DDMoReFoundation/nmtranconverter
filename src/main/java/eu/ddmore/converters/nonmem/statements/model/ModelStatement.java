/*******************************************************************************
 * Copyright (C) 2016 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.statements.model;

import com.google.common.base.Preconditions;

import eu.ddmore.converters.nonmem.ConversionContext;

/**
 * This class creates model statement. It adds associated statement blocks for continuous as well as discrete statements. 
 */
public class ModelStatement {

    private final ConversionContext context;
    private ModelStatementHelper statementHelper;
    private ContinuousStatement continuousStatement;
    private DiscreteStatement discreteStatement;
    private NonDerivativePredStatement nonDerivativePredStatement;

    public ModelStatement(ConversionContext context){
        Preconditions.checkNotNull(context,"Conversion context cannot be null");
        this.context= context;
        initialise();
    }

    private void initialise(){
        statementHelper = new ModelStatementHelper(context);
        continuousStatement = new ContinuousStatement(statementHelper);
        discreteStatement = new DiscreteStatement(statementHelper);
        nonDerivativePredStatement = new NonDerivativePredStatement(statementHelper);
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
