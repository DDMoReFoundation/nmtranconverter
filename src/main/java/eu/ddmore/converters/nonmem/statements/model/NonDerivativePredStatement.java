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
