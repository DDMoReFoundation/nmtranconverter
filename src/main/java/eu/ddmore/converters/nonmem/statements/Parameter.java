/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.statements;

import eu.ddmore.libpharmml.dom.commontypes.ScalarRhs;
import eu.ddmore.libpharmml.dom.modellingsteps.InitialEstimateType;

/**
 * Creates and adds parameter details.
 * 
 * @author sdeshmukh
 *
 */
public class Parameter {
	
	String symbId;
	InitialEstimateType initialEstimate;
	ScalarRhs lowerBound;
	ScalarRhs upperBound;	
	Integer index;
	boolean fixed = false;
	boolean isStdDev = false;

	public Parameter(String symbId){
		this.symbId = symbId;
	}
	
	public void setParameterBounds(InitialEstimateType initialEstimate,ScalarRhs lowerBound, ScalarRhs upperBound){
		this.initialEstimate = initialEstimate;
		this.lowerBound = lowerBound;
		this.upperBound = upperBound;
	}
	

	public boolean isFixed() {
		return fixed;
	}

	public void setFixed(boolean isFixed) {
		this.fixed = isFixed;
	}
	
	public String getSymbId() {
		return symbId;
	}

	public void setSymbId(String symbId) {
		this.symbId = symbId;
	}


	public InitialEstimateType getInitialEstimate() {
		return initialEstimate;
	}


	public void setInitialEstimate(InitialEstimateType initialEstimate) {
		this.initialEstimate = initialEstimate;
	}


	public ScalarRhs getLowerBound() {
		return lowerBound;
	}


	public void setLowerBound(ScalarRhs lowerBound) {
		this.lowerBound = lowerBound;
	}


	public ScalarRhs getUpperBound() {
		return upperBound;
	}


	public void setUpperBound(ScalarRhs upperBound) {
		this.upperBound = upperBound;
	}


	public Integer getIndex() {
		return index;
	}


	public void setIndex(Integer index) {
		this.index = index;
	}
	
	public boolean isStdDev() {
		return isStdDev;
	}

	public void setStdDev(boolean isStdDev) {
		this.isStdDev = isStdDev;
	}
}
