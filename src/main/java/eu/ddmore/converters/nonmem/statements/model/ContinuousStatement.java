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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import com.google.common.base.Preconditions;

import crx.converter.spi.blocks.StructuralBlock;

import eu.ddmore.converters.nonmem.ConversionContext;
import eu.ddmore.converters.nonmem.LocalParserHelper;
import eu.ddmore.converters.nonmem.eta.Eta;
import eu.ddmore.converters.nonmem.parameters.OmegaBlock;
import eu.ddmore.converters.nonmem.statements.DiffEquationStatementBuilder;
import eu.ddmore.converters.nonmem.statements.estimation.EstimationDetailsEmitter;
import eu.ddmore.converters.nonmem.statements.input.InputColumn;
import eu.ddmore.converters.nonmem.statements.pkmacro.PkMacroAnalyser;
import eu.ddmore.converters.nonmem.statements.pkmacro.PkMacrosEmitter;
import eu.ddmore.converters.nonmem.statements.pkmacro.PkMacroAnalyser.AdvanType;
import eu.ddmore.converters.nonmem.statements.pkmacro.PkMacroAnalyser.PkMacroDetails;
import eu.ddmore.converters.nonmem.utils.Formatter;
import eu.ddmore.converters.nonmem.utils.Formatter.Block;
import eu.ddmore.libpharmml.dom.commontypes.VariableDefinition;
import eu.ddmore.libpharmml.dom.dataset.ColumnMapping;
import eu.ddmore.libpharmml.dom.dataset.ColumnType;
import eu.ddmore.libpharmml.dom.maths.ExpressionValue;

/**
 * This class generates continuous statement block
 */
public class ContinuousStatement {

    private static final int TOL_VALUE_IF_NOT_SAEM = 9;
    private static final int TOL_VALUE_IF_SAEM = 6;
    private static final String DEFDOSE = "DEFDOSE";

    private final ConversionContext context;
    private PkMacrosEmitter pkMacroEquationsEmitter;
    private PkMacroDetails pkMacroDetails;

    public ContinuousStatement(ConversionContext context){
        Preconditions.checkNotNull(context,"model statement helper cannot be null");
        this.context = context;
        initialise();
    }

    private void initialise(){
        PkMacroAnalyser analyser = new PkMacroAnalyser();
        pkMacroDetails = analyser.analyse(context);
        pkMacroEquationsEmitter = new PkMacrosEmitter(context, pkMacroDetails);
    }

    /**
     * Creates and returns continuous statement
     * 
     * @return continuous statement
     */
    public StringBuilder buildContinuousStatement(){
        StringBuilder continuousStatement = new StringBuilder();
        if(pkMacroDetails.getMacroAdvanType().equals(AdvanType.NONE)){
            EstimationDetailsEmitter emitter = context.getEstimationEmitter();
            int tolValue = (emitter.isSAEM())? TOL_VALUE_IF_SAEM:TOL_VALUE_IF_NOT_SAEM;
            if(StringUtils.isNotEmpty(emitter.getTolValue().trim())){
                tolValue = Integer.parseInt(emitter.getTolValue());
            }
            continuousStatement.append(buildSubsStatement(AdvanType.ADVAN13, " TOL="+tolValue));
            continuousStatement.append(buildDerivativePredStatement());
        }else{
            AdvanType advanType = pkMacroDetails.getMacroAdvanType();
            continuousStatement.append(buildSubsStatement(advanType, " TRANS=1"));
            continuousStatement.append(buildAdvanMacroStatement());
        }
        return continuousStatement;
    }

    private String buildSubsStatement(AdvanType advanType, String additionalParams){
        return Formatter.endline()+Formatter.endline(Formatter.subs()+advanType+additionalParams);
    }

    private StringBuilder buildAdvanMacroStatement(){
        StringBuilder advanblock = new StringBuilder();
        advanblock.append(buildAbbreviatedStatement());
        advanblock.append(buildPKStatement());
        advanblock.append(pkMacroEquationsEmitter.getPkMacroStatement());

        advanblock.append(Formatter.error());
        advanblock.append(pkMacroEquationsEmitter.getMacroEquation());
        advanblock.append(buildStructuralModelVarDefinitions());
        advanblock.append(context.getModelStatementHelper().getErrorStatementHandler().getErrorStatement());
        return advanblock;
    }

    private StringBuilder buildStructuralModelVarDefinitions() {
        StringBuilder builder = new StringBuilder();
        if(!context.getScriptDefinition().getStructuralBlocks().isEmpty()){
            for(StructuralBlock block : context.getScriptDefinition().getStructuralBlocks()){
                Map<String, String> allVarDefinitions = new HashMap<String, String>();
                for (VariableDefinition definitionType: block.getLocalVariables()){
                    String variable = definitionType.getSymbId().toUpperCase();
                    String rhs = context.getLocalParserHelper().parse(definitionType);
                    allVarDefinitions.put(variable, rhs);
                    builder.append(rhs);
                }
            }
        }
        return builder;
    }

    private StringBuilder buildDerivativePredStatement() {
        StringBuilder derivativePredblock = new StringBuilder();
        derivativePredblock.append(buildModelStatement());
        derivativePredblock.append(buildAbbreviatedStatement());
        derivativePredblock.append(buildPKStatement());

        if(pkMacroDetails!=null && !pkMacroDetails.isEmpty()){
            derivativePredblock.append(pkMacroEquationsEmitter.getPkMacroEquations());
        }
        derivativePredblock.append(context.getModelStatementHelper().getDifferentialInitialConditions());
        DiffEquationStatementBuilder desBuilder = context.getModelStatementHelper().getDiffEquationStatement(derivativePredblock);
        //TODO: getAESStatement();
        derivativePredblock.append(Formatter.endline()+Formatter.error());
        derivativePredblock.append(context.getModelStatementHelper().getErrorStatementHandler().getErrorStatement(desBuilder));

        return derivativePredblock;
    }

    /**
     * get model statement block for pred statement of nonmem file.
     * 
     */
    private StringBuilder buildModelStatement() {
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
                defDoseSymbol = " "+DEFDOSE;
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

    private StringBuilder buildAbbreviatedStatement() {
        StringBuilder abbrStatement = new StringBuilder();
        abbrStatement.append(Formatter.endline());
        int prevBlockValue = 0;

        for(OmegaBlock omegaBlock : context.getCorrelationHandler().getOmegaBlocksInIOV()){
            abbrStatement.append(Formatter.endline());
            Set<Eta> etas =  omegaBlock.getOrderedEtas();
            int iovEtasCount = etas.size();
            boolean isFirstBlock = (prevBlockValue == 0);

            for(Eta iovEta :etas){
                int etaOrder = (isFirstBlock)?iovEta.getOrder():++prevBlockValue;
                StringBuilder etaValues = new StringBuilder();
                prevBlockValue = getIovEtaValueForAbbr(iovEtasCount, etaOrder, etaValues);
                String nextAbbr = Formatter.abbr()+"REPLACE "+Formatter.buildEffectOrderSymbolFor(Block.ETA, iovEta.getEtaOrderSymbol());
                abbrStatement.append(Formatter.endline(nextAbbr+"="+Formatter.buildEffectOrderSymbolFor(Block.ETA, etaValues.toString())));
            }
        }
        return abbrStatement;
    }

    /**
     * The ABBR statement should be,
     * $ABBR REPLACE  ETA(<symbol>)= ETA(r, (1*n+r),(2*n+r),... ..., (u*n+r))
     *      where 'r' is eta order number, 
     *      'n' is total number of etas 
     *      and 'u' us total number of unique values in occasion column in dataset provided.
     * 
     * This method calculates RHS part of equation for ABBR statement.
     * It calculates iov eta value and also creates iov eta statement for current count using following formula.
     * 
     * @param iovEtasCount iov etas count number
     * @param etaOrder eta order number
     * @param etaValues iov eta values statement in string format
     * @return iov Eta value in integer format
     */
    private int getIovEtaValueForAbbr(int iovEtasCount, int etaOrder, StringBuilder etaValues) {
        int etaVal = 0;
        int uniqueOccValuesCount = context.getIovHandler().getIovColumnUniqueValues().size();
        for(int i=0;i<uniqueOccValuesCount;i++){
            etaVal = i*iovEtasCount+etaOrder;
            etaValues.append(etaVal);
            if(i != uniqueOccValuesCount-1){
                etaValues.append(", ");
            }
        }
        return etaVal;
    }

    /**
     * gets pk block for pred statement
     */
    private StringBuilder buildPKStatement() {
        StringBuilder pkStatementBlock = new StringBuilder();
        pkStatementBlock.append(Formatter.endline());
        pkStatementBlock.append(Formatter.pk());
        pkStatementBlock.append(context.getModelStatementHelper().getPredCoreStatement().getStatement());
        pkStatementBlock.append(context.getModelStatementHelper().getAllIndividualParamAssignments());
        return new StringBuilder(pkStatementBlock.toString().toUpperCase());
    }
}
