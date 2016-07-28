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

import eu.ddmore.converters.nonmem.statements.DiffEquationStatementBuilder;
import eu.ddmore.converters.nonmem.utils.DiscreteHandler;
import eu.ddmore.converters.nonmem.utils.Formatter;

/**
 * This class generates discrete statement block
 */
public class DiscreteStatement {

    private final ModelStatementHelper statementHelper;

    public DiscreteStatement(ModelStatementHelper statementHelper){
        Preconditions.checkNotNull(statementHelper,"model statement helper cannot be null");
        this.statementHelper = statementHelper;
    }

    /**
     * Gets model statement with discrete statement for count data 
     * 
     * @param discreteHandler
     * @return model statement
     */
    public StringBuilder getModelStatementForCountData(DiscreteHandler discreteHandler) {
        StringBuilder countDataBlock = new StringBuilder();
        //PRED
        countDataBlock.append(Formatter.endline()+Formatter.endline()+Formatter.pred());
        countDataBlock.append(statementHelper.getPredCoreStatement().getStatement());
        countDataBlock.append(statementHelper.getAllIndividualParamAssignments());
        countDataBlock.append(statementHelper.getVarDefinitionTypesForNonDES()+Formatter.endline());
        countDataBlock.append(discreteHandler.getDiscreteStatement());
        countDataBlock.append(statementHelper.getErrorStatementHandler().getErrorStatement());

        return countDataBlock;
    }

    /**
     * Gets model statement with discrete statement for Time to Event
     * 
     * @param discreteHandler
     * @return model statement
     */
    public StringBuilder buildModelStatementForTTE(DiscreteHandler discreteHandler) {
        StringBuilder tteBlock = new StringBuilder();

        buildSubStatementForTTE(tteBlock);
        buildModelBlockForTTE(tteBlock);
        buildPkStatementForTTE(tteBlock);
        DiffEquationStatementBuilder desBuilder = statementHelper.getDiffEquationStatement(tteBlock);
        Integer dadtEquationIndex = buildDiffEquationStatementForTTE(discreteHandler, tteBlock, desBuilder);
        buildErrorStatementForTTE(discreteHandler, tteBlock, desBuilder, dadtEquationIndex);
        return tteBlock;
    }

    private void buildErrorStatementForTTE(DiscreteHandler discreteHandler, StringBuilder tteBlock,
            DiffEquationStatementBuilder desBuilder, Integer dadtEquationIndex) {
        tteBlock.append(Formatter.endline()+Formatter.error());
        tteBlock.append(desBuilder.getVariableDefinitionsStatement(desBuilder.getAllVarDefinitions()));
        //TODO : change A(1) to A(n+1)
        tteBlock.append(Formatter.endline("CUMHAZ=A("+dadtEquationIndex+")"+
                Formatter.indent(Formatter.indent(""))+Formatter.addComment("CUMHAZ since last event")));
        tteBlock.append(Formatter.endline("HAZARD_FUNC = "+discreteHandler.getHazardFunction()));
        tteBlock.append(discreteHandler.getDiscreteStatement());

        tteBlock.append(statementHelper.getErrorStatementHandler().getErrorStatement());
    }

    private Integer buildDiffEquationStatementForTTE(DiscreteHandler discreteHandler, StringBuilder tteBlock,
            DiffEquationStatementBuilder desBuilder) {
        Integer dadtEquationIndex = desBuilder.getDadtDefinitionsInDES().size()+1;

        String hazardFunction = Formatter.renameVarForDES(discreteHandler.getHazardFunction());
        tteBlock.append(Formatter.endline("HAZARD_FUNC_DES = "+hazardFunction));
        tteBlock.append(Formatter.endline("DADT("+dadtEquationIndex+") = HAZARD_FUNC_DES"));
        return dadtEquationIndex;
    }

    private void buildPkStatementForTTE(StringBuilder tteBlock) {
        tteBlock.append(Formatter.endline()+Formatter.pk()); 
        tteBlock.append(statementHelper.getPredCoreStatement().getStatement());
        tteBlock.append(statementHelper.getAllIndividualParamAssignments());
    }

    private void buildModelBlockForTTE(StringBuilder tteBlock) {
        tteBlock.append(Formatter.endline());
        String modelStatement = Formatter.endline("$MODEL"+ Formatter.endline()+Formatter.addComment("One hardcoded compartment ")) +
                Formatter.endline(Formatter.indent("COMP=COMP1"));
        tteBlock.append(modelStatement);
    }

    private void buildSubStatementForTTE(StringBuilder tteBlock) {
        tteBlock.append(Formatter.endline());
        tteBlock.append(Formatter.endline(Formatter.subs()+"ADVAN13 TOL=9"));
    }

}
