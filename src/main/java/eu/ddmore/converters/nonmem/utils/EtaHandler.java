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
package eu.ddmore.converters.nonmem.utils;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import com.google.common.base.Preconditions;

import crx.converter.engine.ScriptDefinition;
import crx.converter.spi.blocks.ParameterBlock;
import eu.ddmore.converters.nonmem.eta.Eta;
import eu.ddmore.libpharmml.dom.modeldefn.IndividualParameter;
import eu.ddmore.libpharmml.dom.modeldefn.ParameterRandomEffect;
import eu.ddmore.libpharmml.dom.modeldefn.ParameterRandomVariable;

/**
 * This class gets all the etas in the context and makes it available to use.
 * The output is not usable from this class and it will ordered with respect to correlation and IOV analysis.
 */
public class EtaHandler {

    private final Set<Eta> allEtas = new LinkedHashSet<Eta>();

    public EtaHandler(ScriptDefinition scriptDefinition){
        Preconditions.checkNotNull(scriptDefinition, "Script definition cannot be null");
        initialise(scriptDefinition);
    }

    private void initialise(ScriptDefinition scriptDefinition){
        retrieveAllEtas(scriptDefinition);
    }
    /**
     * Creates ordered Etas map which gets all etas list from individual parameters and parameter block.
     * 
     * @param scriptDefinition
     * @return set of etas
     */
    private Set<Eta> retrieveAllEtas(ScriptDefinition scriptDefinition) {
        List<ParameterBlock> blocks = scriptDefinition.getParameterBlocks();
        for(ParameterBlock block : blocks ){

            for(IndividualParameter parameterType: block.getIndividualParameters()){
                if (parameterType.getStructuredModel() != null) {
                    List<ParameterRandomEffect> randomEffects = parameterType.getStructuredModel().getListOfRandomEffects();
                    for (ParameterRandomEffect randomEffect : randomEffects) {
                        if (randomEffect == null) continue;
                        String eta = randomEffect.getSymbRef().get(0).getSymbIdRef();
                        addRandomVarToAllEtas(eta);
                    }
                }
            }

            for(ParameterRandomVariable variable : block.getLinkedRandomVariables()){
                addRandomVarToAllEtas(variable);
            }

            Collection<ParameterRandomVariable> epsilons =  ScriptDefinitionAccessor.getEpsilonRandomVariables(scriptDefinition);
            for(ParameterRandomVariable variable : block.getRandomVariables()){
                if(!epsilons.contains(variable)){
                    addRandomVarToAllEtas(variable);
                }
            }
        }
        return allEtas;
    }

    private void addRandomVarToAllEtas(String variable) {
        Eta eta = new Eta(variable);
        if(!allEtas.contains(eta)){
            allEtas.add(eta);
        }
    }

    private void addRandomVarToAllEtas(ParameterRandomVariable variable) {
        Eta eta = new Eta(variable.getSymbId());
        String omegaName = RandomVariableHelper.getNameFromParamRandomVariable(variable);
        if(StringUtils.isNotEmpty(omegaName)){
            eta.setOmegaName(omegaName);
        }
        if(!allEtas.contains(eta)){
            allEtas.add(eta);
        }else{
            for(Eta existingEta : allEtas){
                if(existingEta.equals(eta)){
                    existingEta.setOmegaName(omegaName);
                }
            }
        }
    }

    public Set<Eta> getAllEtas() {
        return allEtas;
    }
}
