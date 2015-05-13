package eu.ddmore.converters.nonmem;

import java.util.List;

import eu.ddmore.converters.nonmem.utils.Formatter;
import eu.ddmore.converters.nonmem.utils.Formatter.NmConstant;
import eu.ddmore.converters.nonmem.utils.Formatter.Symbol;
import eu.ddmore.libpharmml.dom.IndependentVariable;
import eu.ddmore.libpharmml.dom.commontypes.PharmMLRootType;
import eu.ddmore.libpharmml.dom.modeldefn.ContinuousCovariate;
import eu.ddmore.libpharmml.dom.modeldefn.CovariateDefinition;
import eu.ddmore.libpharmml.dom.modeldefn.CovariateRelation;
import eu.ddmore.libpharmml.dom.modeldefn.CovariateTransformation;
import eu.ddmore.libpharmml.dom.modeldefn.FixedEffectRelation;
import eu.ddmore.libpharmml.dom.modeldefn.IndividualParameter;
import eu.ddmore.libpharmml.dom.modeldefn.IndividualParameter.GaussianModel;
import eu.ddmore.libpharmml.dom.modeldefn.IndividualParameter.GaussianModel.GeneralCovariate;
import eu.ddmore.libpharmml.dom.modeldefn.LhsTransformation;
import eu.ddmore.libpharmml.dom.modeldefn.ParameterRandomEffect;

public class IndividualDefinitionEmitter {
    private static final String comment_char = ";";
    ConversionContext context;

    public IndividualDefinitionEmitter(ConversionContext  context) {
        this.context = context;
    }

    /**
     * Get individual parameter definition from individual parameter type.
     * This method is used as part of pred core block.
     * 
     * @param param
     * @return
     */
    public String createIndividualDefinition(IndividualParameter param){
        StringBuilder statement = new StringBuilder();

        String variableSymbol = param.getSymbId();     
        if (param.getAssign() != null) {
            statement = getIndivDefinitionForAssign(param);
        } 
        else if (param.getGaussianModel() != null) {

            GaussianModel gaussianModel = param.getGaussianModel();
            String logType = getLogType(gaussianModel.getTransformation());
            String pop_param_symbol = context.getParameterHelper().getPopSymbol(gaussianModel);
            variableSymbol = (pop_param_symbol.isEmpty())?variableSymbol:context.getParameterHelper().getMUSymbol(pop_param_symbol);

            statement.append(String.format("%s = ", variableSymbol));

            if(gaussianModel.getLinearCovariate()!=null){
                if (!pop_param_symbol.isEmpty()) {
                    statement.append(String.format(logType+"(%s)", Formatter.addPrefix(pop_param_symbol)));
                }

                List<CovariateRelation> covariates = gaussianModel.getLinearCovariate().getCovariate();
                if (covariates != null) {
                    for (CovariateRelation covariate : covariates) {
                        if (covariate == null) continue;
                        PharmMLRootType type = context.getLexer().getAccessor().fetchElement(covariate.getSymbRef());
                        statement.append(getCovariateForIndividualDefinition(covariate, type));
                    }
                }
            }
            else if (gaussianModel.getGeneralCovariate() != null) {
                GeneralCovariate generalCov = gaussianModel.getGeneralCovariate(); 
                String assignment = context.parse(generalCov);
                statement.append(assignment);
            }
            statement.append(Formatter.endline(comment_char));

            StringBuilder etas = addEtasStatementsToIndivParamDef(gaussianModel.getRandomEffects());
            if (logType.equals(NmConstant.LOG.toString())) {
                String format = Formatter.endline("%s = EXP(%s %s);");
                statement.append(String.format(format, Formatter.addPrefix(param.getSymbId()), variableSymbol,etas));
            } else if (logType.equals(NmConstant.LOGIT.toString())) {
                String format = Formatter.endline("%s = 1./(1 + exp(-%s));");
                statement.append(String.format(format, Formatter.addPrefix(param.getSymbId()), variableSymbol));
            }
        }
        statement.append(Formatter.endline());

        return statement.toString();
    }

    /**
     * This method will retrieve details of parameter type passed depending upon 
     * whether it is IDV or Covariate Definition.
     * 
     * @param covariate
     * @param type
     * @return
     */
    private StringBuilder getCovariateForIndividualDefinition(CovariateRelation covariate, PharmMLRootType type) {
        StringBuilder statement = new StringBuilder();
        if(type instanceof IndependentVariable){
            String idvName = doIndependentVariable((IndependentVariable)type);
            statement.append("+"+idvName);
        }
        else if(type instanceof CovariateDefinition){
            CovariateDefinition covariateDef = (CovariateDefinition) type;
            if (covariateDef != null) {
                if (covariateDef.getContinuous() != null) {
                    String covStatement = "";
                    ContinuousCovariate continuous = covariateDef.getContinuous();
                    List<CovariateTransformation> transformations = continuous.getListOfTransformation();
                    if (transformations != null && !transformations.isEmpty()){
                        covStatement = context.getParser().getSymbol(continuous.getListOfTransformation().get(0));
                    }
                    else covStatement = covariateDef.getSymbId();

                    covStatement = addFixedEffectsStatementToIndivParamDef(covariate, covStatement);
                    if(!covStatement.isEmpty())
                        statement.append("+"+covStatement);
                } else if (covariateDef.getCategorical() != null) {
                    throw new UnsupportedOperationException("No categorical yet");
                }
            }
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
            String  fixedEffectStatement = Formatter.addPrefix(fixedEffect.getSymbRef().getSymbIdRef());
            if(fixedEffectStatement.isEmpty())
                fixedEffectStatement = context.parse(fixedEffect);
            covStatement = fixedEffectStatement + " * " + covStatement;
        }
        return covStatement;
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
            statement.append(Formatter.endline());
            statement.append(String.format("%s = ", ip.getSymbId()));
            String assignment = context.parse(new Object(), context.getLexer().getStatement(ip.getAssign()));
            statement.append(Formatter.endline(assignment+Symbol.COMMENT));
        }
        return statement;
    }

    /**
     * Return log type in constant format depending on transformation type
     * This method is mainly used to help creation of individual parameter definitions.
     * 
     * @param transform
     * @return
     */
    private String getLogType(LhsTransformation transform) {
        if (transform == LhsTransformation.LOG){
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
    private StringBuilder addEtasStatementsToIndivParamDef(List<ParameterRandomEffect> random_effects) {
        StringBuilder etas = new StringBuilder();
        if (random_effects != null && !random_effects.isEmpty()) {
            for (ParameterRandomEffect random_effect : random_effects) {
                if (random_effect == null) continue;
                etas.append("+ ");
                etas.append("ETA("+context.getOrderedEtas().get(random_effect.getSymbRef().get(0).getSymbIdRef())+")");
            }
        }
        return etas;
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