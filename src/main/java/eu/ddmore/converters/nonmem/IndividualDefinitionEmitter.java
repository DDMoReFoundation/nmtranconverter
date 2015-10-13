/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import com.google.common.base.Preconditions;

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
import eu.ddmore.libpharmml.dom.modeldefn.IndividualParameter.GaussianModel;
import eu.ddmore.libpharmml.dom.modeldefn.IndividualParameter.GaussianModel.GeneralCovariate;
import eu.ddmore.libpharmml.dom.modeldefn.LhsTransformation;
import eu.ddmore.libpharmml.dom.modeldefn.ParameterRandomEffect;

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
     * @param muReferences 
     * @return individual definition statement
     */
    public String createIndividualDefinition(IndividualParameter param){
        StringBuilder statement = new StringBuilder();

        String variableSymbol = param.getSymbId();     
        if (param.getAssign() != null) {
            statement = getIndivDefinitionForAssign(param);
        }else if (param.getGaussianModel() != null) {

            GaussianModel gaussianModel = param.getGaussianModel();

            String pop_param_symbol = context.getOrderedThetasHandler().getPopSymbol(gaussianModel);

            if(!StringUtils.isEmpty(pop_param_symbol)){
                variableSymbol = context.getMuReferenceHandler().getMUSymbol(pop_param_symbol);    
            }

            String logType = identifyLogType(gaussianModel.getTransformation());

            //To avoid multiple definitions of the already defined pop symbol.
            if(!definedPopSymbols.contains(pop_param_symbol)){
                statement.append(String.format("%s = ", variableSymbol));

                if(gaussianModel.getLinearCovariate()!=null){
                    if (!StringUtils.isEmpty(pop_param_symbol)) {
                        String format = ((logType.equals(NmConstant.LOG.toString()))?(logType+"(%s)"):("%s"));
                        statement.append(String.format(format, pop_param_symbol));
                        definedPopSymbols.add(pop_param_symbol);
                    }

                    List<CovariateRelation> covariates = gaussianModel.getLinearCovariate().getCovariate();
                    if (covariates != null) {
                        for (CovariateRelation covRelation : covariates) {
                            if (covRelation == null) continue;
                            PharmMLRootType type = context.getLexer().getAccessor().fetchElement(covRelation.getSymbRef());
                            statement.append(getCovariateForIndividualDefinition(covRelation, type));
                        }
                    }
                } else if (gaussianModel.getGeneralCovariate() != null) {
                    GeneralCovariate generalCov = gaussianModel.getGeneralCovariate();
                    String assignment = context.parse(generalCov);
                    statement.append(assignment);
                }
                statement.append(Formatter.endline(Symbol.COMMENT.toString()));
            }
            statement.append(Formatter.endline(arrangeEquationStatement(param.getSymbId(), variableSymbol, gaussianModel, logType)));
        }

        return statement.toString();
    }

    /**
     * This method will retrieve details of parameter type passed depending upon 
     * whether it is IDV or Covariate Definition.
     * 
     * @param covRelation
     * @param type
     * @return
     */
    private StringBuilder getCovariateForIndividualDefinition(CovariateRelation covRelation, PharmMLRootType type) {
        Preconditions.checkNotNull(type, "Covariate type associated with covariate relation cannot be null. ");
        StringBuilder statement = new StringBuilder();
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
            }
            valueToAppend = addFixedEffectsStatementToIndivParamDef(covRelation, covariateVar);
        }

        if(!valueToAppend.isEmpty()){
            statement.append(" + "+valueToAppend);
        }
        return statement;
    }

    /**
     * This method parses and adds fixed effects statements from covariates to individual parameter definition .
     * @param covariate
     * @param covStatement
     * @return
     */
    private String addFixedEffectsStatementToIndivParamDef(CovariateRelation covariate, String covStatement) {
        FixedEffectRelation fixedEffect = covariate.getFixedEffect();
        if (fixedEffect != null) {
            String  fixedEffectStatement = fixedEffect.getSymbRef().getSymbIdRef();
            if(fixedEffectStatement.isEmpty())
                fixedEffectStatement = context.parse(fixedEffect);
            covStatement = fixedEffectStatement + " * " + covStatement;
        }
        return covStatement;
    }

    /**
     * Arrange resulting equation statement in the way we would want to add it to nmtran.
     * TODO: Verify with Henrik : If it is logarithmic then exponential and other cases will have it without exponential.
     * 
     * @param paramId
     * @param variableSymbol
     * @param gaussianModel
     * @param logType 
     * @return
     */
    private String arrangeEquationStatement(String paramId, String variableSymbol, GaussianModel gaussianModel, String logType) {
        StringBuilder statement = new StringBuilder();
        String etas = addEtasStatementsToIndivParamDef(gaussianModel.getRandomEffects());

        if(!logType.isEmpty()){
            if (logType.equals(NmConstant.LOG.toString())) {
                String format = Formatter.endline("%s = EXP(%s %s);");
                statement.append(String.format(format, paramId, variableSymbol,etas));
            } else if (logType.equals(NmConstant.LOGIT.toString())) {
                String format = Formatter.endline("%s = 1./(1 + exp(-%s));");
                statement.append(String.format(format, paramId, variableSymbol));
            }
        }else{
            String format = Formatter.endline("%s = %s %s"+Symbol.COMMENT);
            statement.append(String.format(format, paramId, variableSymbol,etas));
        }
        return statement.toString().toUpperCase();
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
            statement.append(String.format("%s = ", ip.getSymbId()));
            String assignment = context.parse(new Object(), context.getLexer().getStatement(ip.getAssign()));
            statement.append(Formatter.endline(assignment+Symbol.COMMENT));
        }
        return statement;
    }

    /**
     * Identifies and sets log type in constant format depending on transformation type
     * This method is mainly used to help creation of individual parameter definitions.
     * 
     * @param transform
     * @return
     */
    private String identifyLogType(LhsTransformation transform) {
        if(transform == null){
            return new String();
        }else if (transform == LhsTransformation.LOG){
            return NmConstant.LOG.toString();
        }else if (transform == LhsTransformation.LOGIT){
            return NmConstant.LOGIT.toString();
        }else{
            throw new  UnsupportedOperationException("Tranformation type "+transform.name()+" not yet supported");
        }
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
                etas.append("ETA("+context.retrieveOrderedEtas().get(random_effect.getSymbRef().get(0).getSymbIdRef())+")");
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