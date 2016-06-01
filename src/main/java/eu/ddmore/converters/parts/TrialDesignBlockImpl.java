/* ----------------------------------------------------------------------------
 * This file is part of the CCoPI-Mono Toolkit. 
 *
 * Copyright (C) 2016 jointly by the following organizations:
 * 1. Mango Solutions, England, UK
 * 2. Cyprotex Discovery Limited, Macclesfield, Cheshire, UK
 * 
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation. A copy of the license agreement is provided
 * in the file named "LICENSE.txt" included with this software distribution.
 * ----------------------------------------------------------------------------
 */

package eu.ddmore.converters.parts;

import java.util.ArrayList;
import java.util.List;

import crx.converter.engine.common.DataFiles;
import crx.converter.spi.ILexer;
import crx.converter.spi.blocks.TrialDesignBlock;
import eu.ddmore.libpharmml.dom.commontypes.DerivativeVariable;
import eu.ddmore.libpharmml.dom.commontypes.PharmMLRootType;
import eu.ddmore.libpharmml.dom.trialdesign.ArmDefinition;
import eu.ddmore.libpharmml.dom.trialdesign.TrialDesign;

/**
 * Wrapper class for the PharmML trial design block.
 */
public class TrialDesignBlockImpl extends PartImpl implements TrialDesignBlock {
	private TrialDesign td = null; 
	
	/**
	 * Constructor
	 * @param td_ Trial Design Block
	 * @param c_ Converter Instance
	 */
	public TrialDesignBlockImpl(TrialDesign td_, ILexer c_) {
		if (td_ == null) throw new NullPointerException("The trial design object is NULL.");
		if (c_ == null) throw new NullPointerException("The converter object is NULL.");
		
		td = td_;
		lexer = c_;
		
		DataFiles dfs = lexer.getDataFiles();
		if (dfs != null) dfs.setExternalDataSets(td.getListOfExternalDataSet()); 
	}
	
	@Override
	public void buildTrees() {}
	
	/**
	 * Get the model/source for the trial design block.
	 * @return TrialDesign
	 */
	public TrialDesign getModel() { return td; }
	
	@Override
	public String getName() { return "trial_design"; }
	
	
	
	@Override
	public List<String> getSymbolIds() { return new ArrayList<String>(); }
	
	/**
	 * Flag if a state variable is associated with a dosing event.
	 * @return boolean
	 */
	public boolean hasDosing() {
		return getStateVariablesWithDosing().isEmpty() == false;
	}
	
	@Override
	public boolean hasSymbolId(String name) { return false; }

	@Override
	public List<DerivativeVariable> getStateVariablesWithDosing() { throw new UnsupportedOperationException(); }

    @Override
    public int getArmCount() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public List<ArmDefinition> getArms() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getArmSize(String oid) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getDoseStatement(String administration) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PharmMLRootType getDoseTarget(String administration_oid) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean hasOccassions() {
        // TODO Auto-generated method stub
        return false;
    }
}
