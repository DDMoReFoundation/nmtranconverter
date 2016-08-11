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

import crx.converter.spi.blocks.ParameterBlock;
import crx.converter.spi.blocks.StructuralBlock;
import eu.ddmore.converters.nonmem.ConversionContext;
import eu.ddmore.converters.nonmem.IndividualDefinitionEmitter;
import eu.ddmore.converters.nonmem.LocalParserHelper;
import eu.ddmore.converters.nonmem.statements.DiffEquationStatementBuilder;
import eu.ddmore.converters.nonmem.statements.InitConditionBuilder;
import eu.ddmore.converters.nonmem.statements.error.ErrorStatementHandler;
import eu.ddmore.converters.nonmem.statements.input.InputColumn;
import eu.ddmore.converters.nonmem.utils.Formatter;
import eu.ddmore.libpharmml.dom.commontypes.VariableDefinition;
import eu.ddmore.libpharmml.dom.dataset.ColumnMapping;
import eu.ddmore.libpharmml.dom.dataset.ColumnType;
import eu.ddmore.libpharmml.dom.maths.ExpressionValue;
import eu.ddmore.libpharmml.dom.modeldefn.IndividualParameter;

/**
 * This class contains helper methods for model statement and associated statement blocks which are part of it.
 */
public class ModelStatementHelper {

    private final ConversionContext context;
    private ErrorStatementHandler errorStatementHandler;
    private PredCoreStatement predCoreStatement;
    private StringBuilder differentialInitialConditions;

    public ModelStatementHelper(ConversionContext context){
        Preconditions.checkNotNull(context,"Conversion context cannot be null");
        this.context= context;
        initialise();
    }

    private void initialise(){
        predCoreStatement = new PredCoreStatement(context);
        errorStatementHandler = new ErrorStatementHandler(context);
        differentialInitialConditions = buildDifferentialInitialConditions();
    }

    /**
     * This method will collect all the parsing for Individual parameter assignments.
     *  
     * @param blocks
     * @return
     */
    public StringBuilder getAllIndividualParamAssignments() {
        StringBuilder individualParamAssignmentBlock = new StringBuilder();
        IndividualDefinitionEmitter individualDefEmitter = new IndividualDefinitionEmitter(context);
        for(ParameterBlock parameterBlock : context.getScriptDefinition().getParameterBlocks()){
            for(IndividualParameter parameterType: parameterBlock.getIndividualParameters()){
                individualParamAssignmentBlock.append(individualDefEmitter.createIndividualDefinition(parameterType));
            }
        }
        return individualParamAssignmentBlock;
    }

    /**
     * Creates DES statement block from differential initial conditions.
     * 
     * @return differential initial conditions
     */
    private StringBuilder buildDifferentialInitialConditions(){
        StringBuilder builder = new StringBuilder();
        if(!context.getScriptDefinition().getStructuralBlocks().isEmpty()){
            InitConditionBuilder initBuilder = new InitConditionBuilder();
            builder = initBuilder.getDifferentialInitialConditions(context);
        }
        return builder;
    }

    /**
     * Gets differential equation statement with help of diff equation builder
     * 
     * @param statement
     * @return diff equation statement
     */
    public DiffEquationStatementBuilder getDiffEquationStatement(StringBuilder statement) {
        statement.append(Formatter.des());

        DiffEquationStatementBuilder desBuilder = new DiffEquationStatementBuilder(context);
        Formatter.setInDesBlock(true);
        statement.append(desBuilder.getDifferentialEquationsStatement());
        Formatter.setInDesBlock(false);

        return desBuilder;
    }

    public StringBuilder buildModelStatement() {
        StringBuilder modelBlock = new StringBuilder();
        boolean  isCMTColumn = context.getInputColumnsHandler().getInputColumnsProvider().isCMTColumnPresent();
        InputColumn doseColumn = null;

        modelBlock.append(Formatter.endline());
        modelBlock.append(Formatter.model());

        for(InputColumn column : context.getInputColumnsHandler().getInputColumnsProvider().getInputHeaders()){
            if(column.getColumnType().equals(ColumnType.DOSE)){
                doseColumn = column;
            }
        }

        String doseColumnReatedColumnMapping = "";
        if(doseColumn !=null){
            doseColumnReatedColumnMapping = getDoseColumnRelatedColumnMappingSymbol(doseColumn);
        }

        for(String compartmentSymbol : context.getDerivativeVarCompSequences().keySet()){
            String compartmentNumber = context.getDerivativeVarCompSequences().get(compartmentSymbol);
            String defDoseSymbol = "";
            if(doseColumnReatedColumnMapping.equals(compartmentSymbol) && !isCMTColumn){
                defDoseSymbol = " DEFDOSE";
            }

            modelBlock.append(Formatter.endline("COMP "+"(COMP"+compartmentNumber+defDoseSymbol+") "+Formatter.addComment(compartmentSymbol)));
        }
        return modelBlock;
    }

    private String getDoseColumnRelatedColumnMappingSymbol(InputColumn doseColumn) {
        for(ColumnMapping columnMapping :context.getColumnMappings()){
            if(doseColumn.getColumnId().equals(columnMapping.getColumnRef().getColumnIdRef())){
                if(columnMapping.getSymbRef()!=null && columnMapping.getSymbRef().getSymbIdRef()!=null ){
                    return columnMapping.getSymbRef().getSymbIdRef().trim();
                }else if(columnMapping.getPiecewise()!=null){
                    LocalParserHelper localParser = new LocalParserHelper(context);
                    ExpressionValue value = columnMapping.getPiecewise().getListOfPiece().get(0).getValue();
                    String columnMappingSymbol = localParser.getParsedValueForExpressionValue(value);
                    return columnMappingSymbol.trim();
                }
            }
        }
        return "";
    }

    /**
     * This method gets variable definitions for the non DES variables and adds them to statement.
     * 
     * @return string builder variable definitions types
     */
    public StringBuilder getVarDefinitionTypesForNonDES(){
        StringBuilder varDefinitionsBlock = new StringBuilder();
        for(StructuralBlock block : context.getScriptDefinition().getStructuralBlocks()){

            if(!block.getLocalVariables().isEmpty()){
                for (VariableDefinition definitionType: block.getLocalVariables()){
                    String assignment = context.getLocalParserHelper().parse(definitionType);
                    if(!Formatter.isInDesBlock())
                        varDefinitionsBlock.append(assignment);
                }
            }
        }
        return varDefinitionsBlock;
    }


    public ErrorStatementHandler getErrorStatementHandler() {
        return errorStatementHandler;
    }

    public PredCoreStatement getPredCoreStatement() {
        return predCoreStatement;
    }

    public StringBuilder getDifferentialInitialConditions() {
        return differentialInitialConditions;
    }
}
