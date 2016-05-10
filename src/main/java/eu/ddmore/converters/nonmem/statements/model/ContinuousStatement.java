/*******************************************************************************
 * Copyright (C) 2016 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.statements.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Preconditions;

import crx.converter.engine.parts.StructuralBlock;
import eu.ddmore.converters.nonmem.eta.Eta;
import eu.ddmore.converters.nonmem.parameters.OmegaBlock;
import eu.ddmore.converters.nonmem.statements.DiffEquationStatementBuilder;
import eu.ddmore.converters.nonmem.statements.InitConditionBuilder;
import eu.ddmore.converters.nonmem.statements.pkmacro.PkMacroAnalyser;
import eu.ddmore.converters.nonmem.statements.pkmacro.PkMacrosEmitter;
import eu.ddmore.converters.nonmem.statements.pkmacro.PkMacroAnalyser.AdvanType;
import eu.ddmore.converters.nonmem.statements.pkmacro.PkMacroAnalyser.PkMacroDetails;
import eu.ddmore.converters.nonmem.utils.Formatter;
import eu.ddmore.libpharmml.dom.commontypes.DerivativeVariable;
import eu.ddmore.libpharmml.dom.commontypes.VariableDefinition;

/**
 * This class generates continuous statement block
 */
public class ContinuousStatement {

    private static final int TOL_VALUE_IF_NOT_SAEM = 9;
    private static final int TOL_VALUE_IF_SAEM = 6;
    private final ModelStatementHelper statementHelper;
    private PkMacrosEmitter pkMacroEquationsEmitter;
    private PkMacroDetails pkMacroDetails;

    public ContinuousStatement(ModelStatementHelper statementHelper){
        Preconditions.checkNotNull(statementHelper,"model statement helper cannot be null");
        this.statementHelper = statementHelper;
        initialise();
    }

    private void initialise(){
        PkMacroAnalyser analyser = new PkMacroAnalyser();
        pkMacroDetails = analyser.analyse(statementHelper.getContext());
        pkMacroEquationsEmitter = new PkMacrosEmitter(statementHelper.getContext(), pkMacroDetails);
    }

    /**
     * Creates and returns continuous statement
     * 
     * @return continuous statement
     */
    public StringBuilder getContinuousStatement(){
        StringBuilder continuousStatement = new StringBuilder();
        if(pkMacroDetails.getMacroAdvanType().equals(AdvanType.NONE)){
            int tolValue = (statementHelper.getContext().getEstimationEmitter().isSAEM())? TOL_VALUE_IF_SAEM:TOL_VALUE_IF_NOT_SAEM;
            continuousStatement.append(getSubsStatement(AdvanType.ADVAN13, " TOL="+tolValue));
            continuousStatement.append(getDerivativePredStatement());
        }else{
            AdvanType advanType = pkMacroDetails.getMacroAdvanType();
            continuousStatement.append(getSubsStatement(advanType, " TRANS=1"));
            continuousStatement.append(getAdvanMacroStatement());
        }
        return continuousStatement;
    }

    private String getSubsStatement(AdvanType advanType, String additionalParams){
        return Formatter.endline()+Formatter.endline(Formatter.subs()+advanType+additionalParams);
    }

    private StringBuilder getAdvanMacroStatement(){
        StringBuilder advanblock = new StringBuilder();
        advanblock.append(getPKStatement());
        advanblock.append(pkMacroEquationsEmitter.getPkMacroStatement());

        advanblock.append(Formatter.error());
        advanblock.append(pkMacroEquationsEmitter.getMacroEquation());
        advanblock.append(getStructuralModelVarDefinitions());
        advanblock.append(statementHelper.getErrorStatementHandler().getErrorStatement());
        return advanblock;
    }

    private StringBuilder getStructuralModelVarDefinitions() {
        StringBuilder builder = new StringBuilder();
        if(!statementHelper.getContext().getScriptDefinition().getStructuralBlocks().isEmpty()){
            for(StructuralBlock block : statementHelper.getContext().getScriptDefinition().getStructuralBlocks()){
                Map<String, String> allVarDefinitions = new HashMap<String, String>();
                for (VariableDefinition definitionType: block.getLocalVariables()){
                    String variable = definitionType.getSymbId().toUpperCase();
                    String rhs = statementHelper.getContext().getLocalParserHelper().parse(definitionType);
                    allVarDefinitions.put(variable, rhs);
                    builder.append(rhs);
                }
            }
        }
        return builder;
    }

    private StringBuilder getDerivativePredStatement(){

        StringBuilder derivativePredblock = new StringBuilder();
        derivativePredblock.append(getModelStatement());
        derivativePredblock.append(getAbbreviatedStatement());
        derivativePredblock.append(getPKStatement());

        if(pkMacroDetails!=null && !pkMacroDetails.isEmpty()){
            derivativePredblock.append(pkMacroEquationsEmitter.getPkMacroEquations());
        }
        derivativePredblock.append(getDifferentialInitialConditions());
        DiffEquationStatementBuilder desBuilder = statementHelper.getDiffEquationStatement(derivativePredblock);
        //TODO: getAESStatement();
        derivativePredblock.append(Formatter.endline()+Formatter.error());
        derivativePredblock.append(statementHelper.getErrorStatementHandler().getErrorStatement(desBuilder));

        return derivativePredblock;
    }

    /**
     * Creates DES statement block from differential initial conditions.
     * 
     * @return differential initial conditions
     */
    private StringBuilder getDifferentialInitialConditions(){
        StringBuilder builder = new StringBuilder();
        if(!statementHelper.getContext().getScriptDefinition().getStructuralBlocks().isEmpty()){
            InitConditionBuilder initBuilder = new InitConditionBuilder();
            builder = initBuilder.getDifferentialInitialConditions(statementHelper.getContext().getScriptDefinition().getStructuralBlocks());
        }
        return builder;
    }

    /**
     * get model statement block for pred statement of nonmem file.
     * 
     */
    private StringBuilder getModelStatement() {
        StringBuilder modelBlock = new StringBuilder();
        modelBlock.append(Formatter.endline());
        modelBlock.append(Formatter.model());
        for(DerivativeVariable stateVariable :statementHelper.getContext().getDerivativeVars()){
            String compartment = stateVariable.getSymbId().toUpperCase();
            modelBlock.append(Formatter.endline("COMP "+"(COMP"+statementHelper.getContext().getDerivativeVarCompSequences().get(compartment)+") "+Formatter.addComment(compartment)));
        }
        return modelBlock;
    }

    private StringBuilder getAbbreviatedStatement() {
        StringBuilder abbrStatement = new StringBuilder();
        abbrStatement.append(Formatter.endline());
        int prevBlockValue = 0;

        for(OmegaBlock omegaBlock : statementHelper.getContext().getCorrelationHandler().getOmegaBlocksInIOV()){
            abbrStatement.append(Formatter.endline());
            Set<Eta> etas =  omegaBlock.getOrderedEtas();
            int iovEtasCount = etas.size();
            boolean isFirstBlock = (prevBlockValue == 0);

            for(Eta iovEta :etas){
                int etaOrder = (isFirstBlock)?iovEta.getOrder():++prevBlockValue;
                StringBuilder etaValues = new StringBuilder();
                prevBlockValue = getIovEtaValueForAbbr(iovEtasCount, etaOrder, etaValues);
                String nextAbbr = Formatter.abbr()+"REPLACE "+Formatter.etaFor(iovEta.getEtaOrderSymbol());
                abbrStatement.append(Formatter.endline(nextAbbr+"="+Formatter.etaFor(etaValues.toString())));
            }
        }
        return abbrStatement;
    }

    //(<eta_order>, 1*<no_of_etas>+<eta_order>, 2*<no_of_etas>+<eta_order>, ... , <no_of_unique_occ_values>*<no_of_etas>+<eta_order>);
    private int getIovEtaValueForAbbr(int iovEtasCount, int etaOrder, StringBuilder etaValues) {
        int etaVal = 0;
        int uniqueOccValuesCount = statementHelper.getContext().getIovHandler().getIovColumnUniqueValues().size();
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
    private StringBuilder getPKStatement() {
        StringBuilder pkStatementBlock = new StringBuilder();
        pkStatementBlock.append(Formatter.endline());
        pkStatementBlock.append(Formatter.pk());
        pkStatementBlock.append(statementHelper.getPredCoreStatement().getStatement());
        pkStatementBlock.append(statementHelper.getAllIndividualParamAssignments());
        return new StringBuilder(pkStatementBlock.toString().toUpperCase());
    }
}
