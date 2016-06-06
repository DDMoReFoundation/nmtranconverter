/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.parameters;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import crx.converter.engine.FixedParameter;
import crx.converter.spi.blocks.ParameterBlock;

import eu.ddmore.converters.nonmem.ConversionContext;
import eu.ddmore.converters.nonmem.eta.Eta;
import eu.ddmore.converters.nonmem.utils.Formatter;
import eu.ddmore.converters.nonmem.utils.RandomVariableHelper;
import eu.ddmore.libpharmml.dom.modeldefn.ParameterRandomVariable;

/**
 * This class helps to build omega statement for nmtran file.
 */
public class OmegaStatementBuilder {

    private final Map<String, OmegaParameter> omegaParameters = new LinkedHashMap<String, OmegaParameter>();
    private OmegaBlockStatement omegaBlockStatement;
    private final Set<ParameterRandomVariable> epsilonVars;
    private final ConversionContext context;

    public OmegaStatementBuilder(ConversionContext context, Set<ParameterRandomVariable> epsilonVars){
        this.context = context;
        this.epsilonVars = epsilonVars;

        initialiseOmegaStatement();
    }

    private void initialiseOmegaStatement(){

        omegaBlockStatement = new OmegaBlockStatement(context.getParameterInitialiser(), context.getCorrelationHandler(), context.getIovHandler());
        omegaBlockStatement.createOmegaBlocks();

        for (ParameterRandomVariable rv : getRandomVarsFromParameterBlock()) {

            if(!context.getIovHandler().isRandomVarIOV(rv)){
                String symbId = RandomVariableHelper.getNameFromParamRandomVariable(rv);
                OmegaParameter omegaStatement = context.getParameterInitialiser().createOmegaFromRandomVarName(symbId);
                if(omegaStatement!=null){
                    for(Iterator<FixedParameter> it= context.getParameterInitialiser().getFixedParameters().iterator();it.hasNext();){
                        String paramName = it.next().pe.getSymbRef().getSymbIdRef();
                        if(paramName.equals(symbId)){
                            omegaStatement.setFixed(true);
                            it.remove();
                        }
                    }
                    if(RandomVariableHelper.isParamFromStdDev(rv)){
                        omegaStatement.setStdDev(true);
                    }
                    omegaParameters.put(symbId, omegaStatement);
                }
            }
        }
    }

    private List<ParameterRandomVariable> getRandomVarsFromParameterBlock() {
        List<ParameterRandomVariable> randomVariables = new ArrayList<ParameterRandomVariable>();

        //Unless parameterBlocks is empty, getting first parameter Block.
        if(!context.getScriptDefinition().getParameterBlocks().isEmpty()){
            List<ParameterBlock> parameterBlocks = context.getScriptDefinition().getParameterBlocks();
            for(ParameterBlock block : parameterBlocks){
                for(ParameterRandomVariable variable : block.getRandomVariables()){
                    if(!isResidualError(variable.getSymbId())){
                        randomVariables.add(variable);
                    }
                }
            }
        } else{
            throw new IllegalStateException("parameterBlocks cannot be empty");
        }
        return randomVariables;
    }

    private boolean isResidualError(String symbId) {
        for(ParameterRandomVariable randomVariable : epsilonVars){
            if(randomVariable.getSymbId().equals(symbId)){
                return true;  
            }
        }
        return false;
    }

    /**
     * Prepares omega statement for IOV omega blocks if present.
     *  
     * @return omega statement
     */
    public String getOmegaStatementBlockForIOV(){
        StringBuilder omegaStatement = new StringBuilder();
        List<OmegaBlock> omegaBlocks = context.getCorrelationHandler().getOmegaBlocksInIOV();
        if(!omegaBlocks.isEmpty()){
            omegaStatement.append(appendOmegaBlocks(omegaBlocks));
        }

        return omegaStatement.toString();
    }

    /**
     * Prepares omega statement using omega block and omegas if present.
     *  
     * @return omega statement
     */
    public String getOmegaStatementBlock() {
        StringBuilder omegaStatement = new StringBuilder();
        List<OmegaBlock> omegaBlocks = context.getCorrelationHandler().getOmegaBlocksInNonIOV();

        if(!omegaBlocks.isEmpty()){
            omegaStatement.append(appendOmegaBlocks(omegaBlocks));
        }

        if (!omegaDoesNotExist()) {
            omegaStatement.append(Formatter.endline());
            omegaStatement.append(Formatter.endline(Formatter.omega()));
            for (final String omegaVar : omegaParameters.keySet()) {
                omegaStatement.append(ParameterStatementHandler.addParameter(omegaParameters.get(omegaVar)));
            }
        }
        return omegaStatement.toString();
    }

    /**
     * Checks if omega statements list has any omega statements added
     */
    public Boolean omegaDoesNotExist(){
        return (omegaParameters == null || omegaParameters.isEmpty());
    }

    private StringBuilder appendOmegaBlocks(List<OmegaBlock> omegaBlocks) {
        StringBuilder omegaStatement = new StringBuilder();
        for(OmegaBlock omegaBlock : omegaBlocks){
            if(omegaBlock.getOrderedEtas().size()>0){
                omegaStatement.append(Formatter.endline(omegaBlock.getOmegaBlockTitle()));
                for(Eta eta : omegaBlock.getOrderedEtas()){
                    for(OmegaParameter omega : eta.getOmegaParameters()){
                        if(omega!=null){
                            omegaStatement.append(ParameterStatementHandler.addParameter(omega));
                        }
                    }
                }
                if(omegaBlock.isIOV()){
                    omegaStatement.append(Formatter.endline(omegaBlock.getOmegaBlockSameTitle()));
                }
            }
        }
        return omegaStatement;
    }

    public Map<String, OmegaParameter> getOmegaStatements() {
        return omegaParameters;
    }
}
