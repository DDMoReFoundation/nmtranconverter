/*******************************************************************************
 * Copyright (C) 2016 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.statements.model;

import java.util.Set;

import org.apache.commons.lang.StringUtils;

import com.google.common.base.Preconditions;

import eu.ddmore.converters.nonmem.eta.Eta;
import eu.ddmore.converters.nonmem.statements.DiffEquationStatementBuilder;
import eu.ddmore.converters.nonmem.statements.InitConditionBuilder;
import eu.ddmore.converters.nonmem.parameters.OmegaBlock;
import eu.ddmore.converters.nonmem.statements.model.PkMacroAnalyser.PkMacroAttribute;
import eu.ddmore.converters.nonmem.statements.model.PkMacroAnalyser.PkMacroDetails;
import eu.ddmore.converters.nonmem.utils.Formatter;
import eu.ddmore.libpharmml.dom.commontypes.DerivativeVariable;
import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.AbsorptionOralMacro;
import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.MacroValue;

/**
 * This class generates continuous statement block
 */
public class ContinuousStatement {

    private final ModelStatementHelper statementHelper;

    public ContinuousStatement(ModelStatementHelper statementHelper){
        Preconditions.checkNotNull(statementHelper,"model statement helper cannot be null");
        this.statementHelper = statementHelper;
    }

    /**
     * Creates and returns continuous statement
     * 
     * @return continuous statement
     */
    public StringBuilder getContinuousStatement(){
        PkMacroAnalyser analyser = new PkMacroAnalyser();
        PkMacroDetails pkMacroDetails = analyser.analyse(statementHelper.getContext());

        StringBuilder continuousStatement = new StringBuilder();
        //TODO: Handle specific types of advans. Currently everything goes through default advan type.
        if(pkMacroDetails.getMacroAdvanType().isEmpty()){

        int tolValue = (statementHelper.getContext().getEstimationEmitter().isSAEM())? 6:9;
        continuousStatement.append(getSubsStatement("ADVAN13", " TOL="+tolValue));
        continuousStatement.append(getDerivativePredStatement(pkMacroDetails));

        }else{
            String advanType = pkMacroDetails.getMacroAdvanType();
            continuousStatement.append(getSubsStatement(advanType, " TRANS=1"));
            continuousStatement.append(getAdvanMacroStatement(advanType, pkMacroDetails));
        }

        return continuousStatement;
    }

    private String getSubsStatement(String advanType, String additionalParams){
        return Formatter.endline()+Formatter.endline(Formatter.subs()+advanType+additionalParams);
    }

    private StringBuilder getAdvanMacroStatement(String advanType, PkMacroDetails pkMacroDetails){
        StringBuilder advanblock = new StringBuilder();
        advanblock.append(getPKStatement());
        advanblock.append(getPkMacroEquations(pkMacroDetails));
        advanblock.append(Formatter.error());
        advanblock.append(statementHelper.getErrorStatementHandler().getErrorStatement());
        return advanblock;
    }

    private StringBuilder getDerivativePredStatement(PkMacroDetails pkMacroDetails){

        StringBuilder derivativePredblock = new StringBuilder();
        derivativePredblock.append(getModelStatement());
        derivativePredblock.append(getAbbreviatedStatement());
        derivativePredblock.append(getPKStatement());

        if(pkMacroDetails!=null && !pkMacroDetails.isEmpty()){
            derivativePredblock.append(getPkMacroEquations(pkMacroDetails));
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
     * @return
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

    private StringBuilder getPkMacroEquations(PkMacroDetails pkMacroDetails) {
        // TODO Auto-generated method stub
        StringBuilder builder = new StringBuilder();

        if(!pkMacroDetails.getAbsorptionOrals().isEmpty()){
            for(AbsorptionOralMacro oralMacro : pkMacroDetails.getAbsorptionOrals()){
                for(MacroValue value : oralMacro.getListOfValue()){
                    String macroEquation = getAbsOralMacroEquation(pkMacroDetails, value);
                    if(StringUtils.isNotEmpty(macroEquation)){
                        builder.append(Formatter.endline(macroEquation));
                    }
                }
            }
            builder.append(Formatter.endline());
        }

        return builder;
    }

    private String getAbsOralMacroEquation(PkMacroDetails pkMacroDetails, MacroValue value) {
        String valueArgument = value.getArgument().toUpperCase().trim();
        if(StringUtils.isNotEmpty(valueArgument) 
                && !valueArgument.equals(PkMacroAttribute.KA.name()) 
                && value.getAssign().getSymbRef()!=null) {

            PkMacroAttribute attribute= PkMacroAttribute.valueOf(valueArgument);
            //TODO : check for reserved word and add NM_ prefix
            String variable = Formatter.getReservedParam(value.getAssign().getSymbRef().getSymbIdRef());
            return attribute.getValue()+ pkMacroDetails.getAbsOralCompNumber()+ " = "+ variable;
        }
        return "";
    }

}
