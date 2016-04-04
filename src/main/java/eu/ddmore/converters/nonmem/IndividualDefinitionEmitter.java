/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import com.google.common.base.Preconditions;

import eu.ddmore.converters.nonmem.eta.Eta;
import eu.ddmore.converters.nonmem.utils.Formatter;
import eu.ddmore.converters.nonmem.utils.Formatter.NmConstant;
import eu.ddmore.converters.nonmem.utils.Formatter.Symbol;
import eu.ddmore.libpharmml.dom.IndependentVariable;
import eu.ddmore.libpharmml.dom.commontypes.PharmMLRootType;
import eu.ddmore.libpharmml.dom.modeldefn.CovariateDefinition;
import eu.ddmore.libpharmml.dom.modeldefn.CovariateRelation;
import eu.ddmore.libpharmml.dom.modeldefn.CovariateTransformation;
import eu.ddmore.libpharmml.dom.modeldefn.FixedEffectRelation;
import eu.ddmore.libpharmml.dom.modeldefn.IndividualParameter;
import eu.ddmore.libpharmml.dom.modeldefn.LRHSTransformationType;
import eu.ddmore.libpharmml.dom.modeldefn.ParameterRandomEffect;
import eu.ddmore.libpharmml.dom.modeldefn.StructuredModel;
import eu.ddmore.libpharmml.dom.modeldefn.TransformationType;
import eu.ddmore.libpharmml.dom.modeldefn.TransformedCovariate;

/**
 * This class creates individual parameter definition from individual parameter type.
 */
public class IndividualDefinitionEmitter {
    private final ConversionContext context;
    private final Set<String> definedPopSymbols = new HashSet<String>();

    public IndividualDefinitionEmitter(ConversionContext  context) {
        this.context = context;
    }

    /**
     * Get individual parameter definition from individual parameter type.
     * This method is used as part of pred core block.
     * 
     * @param param
     * @return statement
     */
    public String createIndividualDefinition(IndividualParameter param){
        Preconditions.checkNotNull(param, "Individual parameter provided cannot be null.");
        StringBuilder statement = new StringBuilder();

        if (param.getAssign() != null) {
            statement = getIndivDefinitionForAssign(param);
        } else if (param.getStructuredModel() != null) {
            StructuredModel  structuredModel = param.getStructuredModel();
            String popSymbol = context.getOrderedThetasHandler().getPopSymbol(structuredModel);

            //To avoid multiple definitions of the already defined pop symbol.

            statement.append(arrangeEquationStatement(structuredModel, param.getSymbId(), popSymbol));
            definedPopSymbols.add(popSymbol);
        }
        return statement.toString();
    }

    /**
     * This method retrieves details of parameter type passed depending upon 
     * whether it is IDV or Covariate Definition.
     * 
     * @param covRelation
     * @param type
     * @return value to append
     */
    private String getCovariateForIndividualDefinition(CovariateRelation covRelation, PharmMLRootType type) {
        Preconditions.checkNotNull(type, "Covariate type associated with covariate relation cannot be null. ");
        String valueToAppend = new String();

        if(type instanceof IndependentVariable){
            valueToAppend = doIndependentVariable((IndependentVariable)type);
        } else {
            String covariateVar = new String();
            if(type instanceof CovariateTransformation){
                covariateVar = covRelation.getSymbRef().getSymbIdRef();
            } else if(type instanceof CovariateDefinition){
                CovariateDefinition covDefinition = (CovariateDefinition) type;
                covariateVar = covDefinition.getSymbId();
            } else if(type instanceof TransformedCovariate){
                covariateVar = ((TransformedCovariate) type).getSymbId();
            }
            valueToAppend = addFixedEffectsStatementToIndivParamDef(covRelation, covariateVar);
        }

        if(!valueToAppend.isEmpty()){
            return " + "+valueToAppend;
        }
        return valueToAppend;
    }

    /**
     * This method parses and adds fixed effects statements from covariates to individual parameter definition .
     * @param covariate
     * @param covStatement
     * @return cov statement
     */
    private String addFixedEffectsStatementToIndivParamDef(CovariateRelation covariate, String covStatement) {
        List<FixedEffectRelation> fixedEffects = covariate.getListOfFixedEffect();
        for(FixedEffectRelation fixedEffect : fixedEffects){
            if (fixedEffect != null) {
                String  fixedEffectStatement = fixedEffect.getSymbRef().getSymbIdRef();
                if(fixedEffectStatement.isEmpty()){
                    fixedEffectStatement = context.getLocalParserHelper().parse(fixedEffect);
                }
                covStatement = fixedEffectStatement + " * " + covStatement;
            }
        }
        return covStatement;
    }

    /**
     * Arrange resulting equation statement in the way we would want to add it to nmtran.
     * TODO: Verify with Henrik : If it is logarithmic then exponential and other cases will have it without exponential.
     * 
     * @param structuredModel
     * @param paramId
     * @param indivDefinitionFromCov
     * @return statement
     */
    private StringBuilder arrangeEquationStatement(StructuredModel structuredModel, String paramId, String popSymbol){
        StringBuilder statement = new StringBuilder();
        String etas = addEtasStatementsToIndivParamDef(structuredModel.getListOfRandomEffects());

        String variableSymbol = paramId;
        String paramSymbol = Formatter.getReservedParam(paramId);
        if(!StringUtils.isEmpty(popSymbol)){
            variableSymbol = context.getMuReferenceHandler().getMUSymbol(popSymbol);
        }

        if(!definedPopSymbols.contains(popSymbol)){
            statement = definePopSymbolEquation(structuredModel, paramSymbol, popSymbol, variableSymbol, etas);
        }else {
            String format = Formatter.endline("%s = %s %s"+Symbol.COMMENT);
            statement.append(String.format(format, paramSymbol, variableSymbol,etas));
        }
        return statement.append(Formatter.endline());
    }

    private StringBuilder definePopSymbolEquation(StructuredModel structuredModel, String paramId, 
            String popSymbol, String variableSymbol, String etas) {
        StringBuilder statement = new StringBuilder();
        String format = "%s = ";
        String indivDefinitionFromCov = getIndivDefinitionFromCov(structuredModel).toString();
        LRHSTransformationType transform = structuredModel.getTransformation();

        if(transform == null || transform.getType() == null){
            //MU_1 = POP_CL
            String muFormat = format+ " %s ";
            String eqFormat = Formatter.endline(format+ " %s %s"+Symbol.COMMENT);

            statement.append(String.format(muFormat, variableSymbol, popSymbol));
            statement.append(Formatter.endline(indivDefinitionFromCov));
            statement.append(String.format(eqFormat, paramId, variableSymbol,etas));
        } else if (TransformationType.LOG.equals(transform.getType())) {
            //MU_1=LOG(POP_CL);
            String muFormat = format+NmConstant.LOG+"(%s)";
            String expFormat = Formatter.endline(format + " EXP(%s %s) "+Symbol.COMMENT);

            statement.append(String.format(muFormat, variableSymbol, popSymbol));
            statement.append(Formatter.endline(indivDefinitionFromCov));
            statement.append(String.format(expFormat, paramId, variableSymbol,etas));
        } else if (TransformationType.LOGIT.equals(transform.getType())) {
            //MU_5=LOG(THETA(5)/(1-THETA(5)))
            String logitEquation = paramId+"_"+NmConstant.LOGIT;
            String muFormat = format + NmConstant.LOG+"( %s/(1-%s) )";
            String logitEqFormat = Formatter.endline(format+" %s %s");
            String expFormat = Formatter.endline(format+" 1/(1 + EXP(-%s)) "+Symbol.COMMENT);

            statement.append(String.format(muFormat, variableSymbol, popSymbol, popSymbol));
            statement.append(Formatter.endline(indivDefinitionFromCov));
            statement.append(String.format(logitEqFormat, logitEquation, variableSymbol, etas));
            statement.append(String.format(expFormat, paramId, logitEquation));
        } else {
            throw new  UnsupportedOperationException("Tranformation type "+transform.getId()+" not yet supported");
        }
        return statement;
    }

    private StringBuilder getIndivDefinitionFromCov(StructuredModel structuredModel) {
        StringBuilder statement = new StringBuilder();
        if(structuredModel.getLinearCovariate()!=null){

            List<CovariateRelation> covariates = structuredModel.getLinearCovariate().getListOfCovariate();
            if (covariates != null && !covariates.isEmpty()) {
                for (CovariateRelation covRelation : covariates) {
                    if (covRelation == null) continue;
                    PharmMLRootType type = context.getLexer().getAccessor().fetchElement(covRelation.getSymbRef());
                    statement.append(getCovariateForIndividualDefinition(covRelation, type));
                }
            }
        }
        return statement;
    }

    /**
     * This method will return individual definition details if assignment is present.
     * 
     * @param ip
     * @return
     */
    private StringBuilder getIndivDefinitionForAssign(IndividualParameter ip) {
        StringBuilder statement = new StringBuilder();
        if (ip.getAssign() != null) {
            String assignment = context.getLocalParserHelper().parse(ip, context.getLexer().getStatement(ip.getAssign()));
            statement.append(Formatter.endline(assignment));
        }
        return statement;
    }

    /**
     * This method adds etas from random effects to individual parameter definitions.
     * @param random_effects
     * @return
     */
    private String addEtasStatementsToIndivParamDef(List<ParameterRandomEffect> random_effects) {
        StringBuilder etas = new StringBuilder();
        if (random_effects != null && !random_effects.isEmpty()) {
            for (ParameterRandomEffect random_effect : random_effects) {
                if (random_effect == null) continue;
                etas.append("+ ");
                for(Eta eta : context.retrieveOrderedEtas()){
                    if(eta.getEtaSymbol().equals(random_effect.getSymbRef().get(0).getSymbIdRef())){
                        etas.append(Formatter.etaFor(eta.getEtaOrderSymbol()));
                        break;
                    }
                }
            }
        }
        return etas.toString();
    }

    /**
     * Gets appropriate individual variable symbol from the individual variable provided.
     * @param variable
     * @return variable symbol
     */
    private String doIndependentVariable(IndependentVariable variable) {
        String symbol = variable.getSymbId().toUpperCase();
        symbol = Formatter.getFormattedSymbol(symbol);
        return symbol;
    }
}