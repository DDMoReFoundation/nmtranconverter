/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.statements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import crx.converter.engine.parts.ParameterBlock;
import crx.converter.engine.parts.BaseRandomVariableBlock.Correlation;
import eu.ddmore.converters.nonmem.utils.Formatter;
import eu.ddmore.converters.nonmem.utils.Formatter.Param;
import eu.ddmore.converters.nonmem.utils.ParametersHelper;
import eu.ddmore.converters.nonmem.utils.Formatter.Constant;
import eu.ddmore.libpharmml.dom.modeldefn.ParameterRandomVariableType;

public class OmegaBlockStatement {
	
	final Map<String, List<OmegaStatement>> OmegaBlocks = new HashMap<String, List<OmegaStatement>>();
	LinkedHashMap<String, String> etasToOmegasInCorrelation = new LinkedHashMap<String, String>();
	
	String omegaBlockTitle;
	Boolean isOmegaBlockFromStdDev = false;
	Map<String, Integer> etaToOmagaMap = new HashMap<String, Integer>();
	Map<Integer, String> orderedEtasToOmegaMap = new TreeMap<Integer, String>();
	ParametersHelper paramHelper;
	
	public OmegaBlockStatement(ParametersHelper parameters) {
		paramHelper = parameters;
	}
	
	
	/**
	 * This method will create omega blocks if there are any.
	 * We will need ordered etas and eta to omega map to determine order of the omega block elements.
	 * Currently only correlations are supported.
	 *  
	 * @return
	 */
	public void createOmegaBlocks(){
		List<Correlation> correlations = getAllCorrelations();
		
		if(!correlations.isEmpty()){
			initialiseOmegaBlocks(correlations);
			orderedEtasToOmegaMap = paramHelper.reverseMap(etaToOmagaMap);
			omegaBlockTitle = createOmegaBlockTitle(correlations);

			for(String eta : orderedEtasToOmegaMap.values()){

				for(Correlation correlation :  correlations){
					String firstRandomVar = correlation.rnd1.getSymbId();
					String secondRandomVar = correlation.rnd2.getSymbId();
					int column = getOrderedEtaIndex(firstRandomVar);
					int row = getOrderedEtaIndex(secondRandomVar);

					createFirstMatrixRow(eta, correlation.rnd1);
						
					List<OmegaStatement> omegas = OmegaBlocks.get(secondRandomVar);
					if(omegas.get(row)==null){
						omegas.remove(row);
						String symbId = paramHelper.getNameFromParamRandomVariable(correlation.rnd2);
						omegas.add(row, paramHelper.getOmegaFromRandomVarName(symbId));
					}
					
					if(omegas.get(column)==null){
						String symbId = correlation.correlationCoefficient.getSymbRef().getSymbIdRef();
						omegas.remove(column);
						omegas.add(column, paramHelper.getOmegaFromRandomVarName(symbId));	
					}
				}
			}
		}
	}

	/**
	 * Creates title for omega blocks.
	 * Currently it gets block count for BLOCK(n) and identify in correlations are used.
	 * This will be updated for matrix types in future.
	 * 
	 * @param correlations
	 * @return
	 */
	private String createOmegaBlockTitle(List<Correlation> correlations) {
		Integer blocksCount = etaToOmagaMap.size();
		StringBuilder description = new StringBuilder();
		//This will change in case of 0.4 as it will need to deal with matrix types as well.
		description.append((!correlations.isEmpty())?Constant.CORRELATION:" ");
		description.append((isOmegaBlockFromStdDev)?" "+Constant.SD:"");
		String title = Formatter.endline()+"$"+Param.OMEGA+" "+Param.BLOCK+"("+blocksCount+") "+description;
		return title;
	}

	/**
	 * This method will return index from ordered eta map for random var name provided. 
	 * 
	 * @param randomVariable
	 * @return
	 */
	private int getOrderedEtaIndex(String randomVariable) {
		return (etaToOmagaMap.get(randomVariable)>0)?etaToOmagaMap.get(randomVariable)-1:0;
	}

	/**
	 * Collects correlations from all the prameter blocks. 
	 * 
	 * @return
	 */
	private List<Correlation> getAllCorrelations() {
		List<Correlation> correlations = new ArrayList<Correlation>();
		List<ParameterBlock> parameterBlocks = paramHelper.getScriptDefinition().getParameterBlocks();
		if(!parameterBlocks.isEmpty()){
			for(ParameterBlock block : parameterBlocks){
				correlations.addAll(block.getCorrelations());				
			}
		}
		return correlations;
	}

	/**
	 * Creates first matrix row which will have only first omega as element.
	 * 
	 * @param eta
	 * @param randomVar1
	 */
	private void createFirstMatrixRow(String eta, ParameterRandomVariableType randomVar1) {
		if(etaToOmagaMap.get(randomVar1.getSymbId())== 1 && eta.equals(randomVar1.getSymbId())){
			List<OmegaStatement> matrixRow = new ArrayList<OmegaStatement>();
			String symbId = paramHelper.getNameFromParamRandomVariable(randomVar1);
			matrixRow.add(paramHelper.getOmegaFromRandomVarName(symbId));
			OmegaBlocks.put(randomVar1.getSymbId(), matrixRow);
		}
	}
	
	/**
	 * Initialise omega blocks maps and also update ordered eta to omegas from correlations map.
	 * 
	 * @param correlations
	 */
	private void initialiseOmegaBlocks(List<Correlation> correlations){
		OmegaBlocks.clear();

		for(Correlation correlation : correlations){
			//Need to set SD attribute for whole block if even a single value is from std dev
			setStdDevAttributeForOmegaBlock(correlation);
			paramHelper.addCorrelationToMap(etasToOmegasInCorrelation,correlation);
		}
		
		for(Iterator<Entry<String, Integer>> it = etaToOmagaMap.entrySet().iterator(); it.hasNext();) {
			if(!etasToOmegasInCorrelation.keySet().contains(it.next().getKey())){
		    	  it.remove();
		      }
		}

		for(String eta : etaToOmagaMap.keySet()){
			ArrayList<OmegaStatement> statements = new ArrayList<OmegaStatement>();
			for(int i=0;i<etaToOmagaMap.keySet().size();i++) statements.add(null);
			OmegaBlocks.put(eta, statements);
		}
	}

	private void setStdDevAttributeForOmegaBlock(Correlation correlation) {
		if(!isOmegaBlockFromStdDev){
			isOmegaBlockFromStdDev = paramHelper.isParamFromStdDev(correlation.rnd1) || paramHelper.isParamFromStdDev(correlation.rnd1);
		}
	}
	
	//Getters and Setters
	public Map<String, Integer> getEtaToOmagaMap() {
		return etaToOmagaMap;
	}

	public void setEtaToOmagaMap(Map<String, Integer> etaToOmagaMap) {
		this.etaToOmagaMap = etaToOmagaMap;
	}

	public Map<String, List<OmegaStatement>> getOmegaBlocks() {
		return OmegaBlocks;
	}

	public Map<String, String> getEtasToOmegasInCorrelation() {
		return etasToOmegasInCorrelation;
	}
	public Map<Integer, String> getOrderedEtasToOmegaMap() {
		return orderedEtasToOmegaMap;
	}


	public String getOmegaBlockTitle() {
		return omegaBlockTitle;
	}

}
