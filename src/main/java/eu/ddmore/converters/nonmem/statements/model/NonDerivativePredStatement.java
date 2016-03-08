/*******************************************************************************
 * Copyright (C) 2016 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.statements.model;

import com.google.common.base.Preconditions;

import eu.ddmore.converters.nonmem.utils.Formatter;

/**
 * Creates non derivative pred statement for nmtran.
 */
public class NonDerivativePredStatement {
    
    ModelStatementHelper statementHelper;
    
    public NonDerivativePredStatement(ModelStatementHelper statementHelper){
        Preconditions.checkNotNull(statementHelper,"model statement helper cannot be null");
        this.statementHelper = statementHelper;
    }

    /**
     * Returns non derivative pred statement.
     * @return 
     */
    protected StringBuilder getPredStatement() {
        StringBuilder nonDerivativePredBlock = new StringBuilder();

        nonDerivativePredBlock.append(Formatter.endline()+Formatter.endline()+Formatter.pred());
        nonDerivativePredBlock.append(statementHelper.getPredCoreStatement().getStatement());
        nonDerivativePredBlock.append(statementHelper.getAllIndividualParamAssignments());
        nonDerivativePredBlock.append(statementHelper.getVarDefinitionTypesForNonDES()+Formatter.endline());
        nonDerivativePredBlock.append(statementHelper.getErrorStatementHandler().getErrorStatement());

        return nonDerivativePredBlock;
    }

}
