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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import com.google.common.base.Preconditions;

import crx.converter.engine.common.ConditionalDoseEvent;
import crx.converter.engine.common.ParameterEvent;
import crx.converter.engine.common.TemporalDoseEvent;
import crx.converter.spi.blocks.ParameterBlock;
import eu.ddmore.converters.nonmem.ConversionContext;
import eu.ddmore.converters.nonmem.eta.Eta;
import eu.ddmore.converters.nonmem.parameters.Parameter;
import eu.ddmore.converters.nonmem.utils.Formatter;
import eu.ddmore.converters.nonmem.utils.Formatter.Block;
import eu.ddmore.converters.nonmem.utils.ScriptDefinitionAccessor;
import eu.ddmore.libpharmml.dom.modeldefn.ContinuousCovariate;
import eu.ddmore.libpharmml.dom.modeldefn.CovariateDefinition;
import eu.ddmore.libpharmml.dom.modeldefn.CovariateTransformation;
import eu.ddmore.libpharmml.dom.modeldefn.PopulationParameter;

/**
 * creates pred core statement for nmtran.
 * 
 */
public class PredCoreStatement {

    private final ConversionContext context;

    public PredCoreStatement(ConversionContext context){
        Preconditions.checkNotNull(context,"Conversion context cannot be null");
        this.context = context;
    }

    /**
     * Create and returns pred core statment.
     * 
     * @return pred core statement
     */
    public StringBuilder getStatement() {
        StringBuilder predCoreBlock = new StringBuilder();
        predCoreBlock.append(getConditionalDoseDetails());
        predCoreBlock.append(buildThetaAssignments()+Formatter.endline());

        if(!(context.isSigmaPresent() 
                || context.isOmegaForIIVPresent() 
                || context.isOmegaForIOVPresent())){
            predCoreBlock.append(Formatter.getDummyEtaStatement());
        }else{
            predCoreBlock.append(buildEtaAssignments()+Formatter.endline());
        }
        predCoreBlock.append(getTransformedCovStatement());
        predCoreBlock.append(getSimpleParamAssignments()+Formatter.endline());

        return predCoreBlock;
    }

    /**
     * This method will build simple parameter assignment statements
     * @return
     */
    private StringBuilder getSimpleParamAssignments() {
        StringBuilder simpleParamAssignmentBlock = new StringBuilder();
        Map<String, Parameter> params = context.getParameterInitialiser().getParameters();

        for(String simpleParamSymbol : params.keySet()){
            if(params.get(simpleParamSymbol).isAssignment()){
                PopulationParameter simpleParam = params.get(simpleParamSymbol).getPopParameter();
                ParameterEvent event = getEventForParameter(simpleParam);
                String parsedEquation = new String();
                if(event !=null){
                    if(event.getPiecewiseTree()!=null && event.getPiecewiseTree().size()>0){
                        parsedEquation = context.getLocalParserHelper().parse(event.getParameter(), event.getPiecewiseTree());
                    }
                }else{
                    String paramSymbol = Formatter.getReservedParam(simpleParamSymbol);
                    parsedEquation = paramSymbol+" = "+context.getLocalParserHelper().getParsedValueForRhs(simpleParam.getAssign());
                }

                if(!parsedEquation.isEmpty()){
                    simpleParamAssignmentBlock.append(Formatter.endline(parsedEquation));
                }
            }
        }
        return simpleParamAssignmentBlock;
    }

    private ParameterEvent getEventForParameter(PopulationParameter populationParam){
        for(ParameterBlock pb : context.getScriptDefinition().getParameterBlocks()){
            if (pb.hasEvents()) {
                for (ParameterEvent event : pb.getEvents()) {
                    if (event != null) {
                        if (populationParam.getSymbId().equals(event.getParameter().getSymbId())){
                            return event;
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * This method builds theta assignment statements with help of parameter helper.
     * @return theta assignments
     */
    private StringBuilder buildThetaAssignments() {
        StringBuilder thetaAssignmentBlock = new StringBuilder();
        List<String> thetas = new ArrayList<String>(context.getParametersBuilder().getThetasBuilder().getThetaParameters().keySet());

        for(String theta : thetas){
            Integer order = thetas.indexOf(theta)+1;
            String equation = Formatter.buildEffectsDefinitionFor(Block.THETA, theta, order.toString());
            thetaAssignmentBlock.append(equation);
        }
        return thetaAssignmentBlock;
    }

    /**
     * This method will build eta assignment statements to be displayed after theta assignments.
     * @return
     */
    private StringBuilder buildEtaAssignments() {
        StringBuilder etaAssignment = new StringBuilder();
        Set<Eta> orderedThetas = context.retrieveOrderedEtas();
        for(Eta eta : orderedThetas){
            etaAssignment.append(Formatter.endline(eta.getEtaSymbol()+ " = "+Formatter.buildEffectOrderSymbolFor(Block.ETA, eta.getEtaOrderSymbol())));
        }
        return etaAssignment;
    }

    private String getConditionalDoseDetails() {
        List<ConditionalDoseEvent> conditionalDoseEvents = ScriptDefinitionAccessor.getAllConditionalDoseEvents(context.getScriptDefinition());
        List<TemporalDoseEvent> teDoseEvents = ScriptDefinitionAccessor.getAllTemporalDoseEvent(context.getScriptDefinition());

        StringBuilder doseEvents = new StringBuilder();
        for(ConditionalDoseEvent event : conditionalDoseEvents){
            String statement = context.getConditionalEventHandler().parseConditionalDoseEvent(event);
            if(!StringUtils.isEmpty(statement)){
                doseEvents.append(statement);
            }
        }

        if(!teDoseEvents.isEmpty()){
            for(TemporalDoseEvent event : teDoseEvents){
                String statement = context.getConditionalEventHandler().parseTemporalDoseEvent(event);
                if(!StringUtils.isEmpty(statement)){
                    doseEvents.append(statement);
                }
            }
        }
        return doseEvents.toString();
    }

    private StringBuilder getTransformedCovStatement() {
        StringBuilder transformedCovBlock = new StringBuilder();
        //Find and add transformed covariates before indiv parameter definitions 
        List<CovariateDefinition> covDefs = context.getLexer().getCovariates();
        for(CovariateDefinition covDef : covDefs){
            if(covDef.getContinuous()!=null){
                ContinuousCovariate contCov = covDef.getContinuous();
                for(CovariateTransformation transformation : contCov.getListOfTransformation()){
                    String transCovDefinition = context.getParser().getSymbol(transformation);
                    transformedCovBlock.append(Formatter.endline(transCovDefinition));
                }
            }
        }
        return transformedCovBlock;
    }
}
