/*******************************************************************************
 * Copyright (C) 2016 Mango Solutions Ltd - All rights reserved.
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
    public StringBuilder getModelStatementForTTE(DiscreteHandler discreteHandler) {
        StringBuilder tteBlock = new StringBuilder();
        tteBlock.append(Formatter.endline());
        //$SUBS
        tteBlock.append(Formatter.endline(Formatter.subs()+"ADVAN13 TOL=9"));
        //$MODEL
        tteBlock.append(Formatter.endline());
        String modelStatement = Formatter.endline("$MODEL"+ Formatter.endline()+Formatter.addComment("One hardcoded compartment ")) +
                Formatter.endline(Formatter.indent("COMP=COMP1"));
        tteBlock.append(modelStatement);
        //$PK
        tteBlock.append(Formatter.endline()+Formatter.pk()); 
        tteBlock.append(statementHelper.getPredCoreStatement().getStatement());
        tteBlock.append(statementHelper.getAllIndividualParamAssignments());
        //$DES
        DiffEquationStatementBuilder desBuilder = statementHelper.getDiffEquationStatement(tteBlock);
        String HAZARD_FUNC_DES = Formatter.renameVarForDES(discreteHandler.getHazardFunction());
        tteBlock.append(Formatter.endline("HAZARD_FUNC_DES = "+HAZARD_FUNC_DES));
        tteBlock.append(Formatter.endline("DADT(1) = HAZARD_FUNC_DES"));
        //Customised ERROR
        //$ERROR
        tteBlock.append(Formatter.endline()+Formatter.error());
        if(desBuilder!=null){
            tteBlock.append(desBuilder.getVariableDefinitionsStatement(desBuilder.getAllVarDefinitions()));
        }
        tteBlock.append(Formatter.endline("CUMHAZ=A(1)"+
                Formatter.indent(Formatter.indent(""))+Formatter.addComment("CUMHAZ since last event")));
        tteBlock.append(Formatter.endline("HAZARD_FUNC = "+discreteHandler.getHazardFunction()));
        tteBlock.append(discreteHandler.getDiscreteStatement());

        tteBlock.append(statementHelper.getErrorStatementHandler().getErrorStatement());
        return tteBlock;
    }

}
