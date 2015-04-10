/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.statements;

import eu.ddmore.libpharmml.dom.commontypes.ScalarRhs;

/**
 * Creates and adds parameter details.
 *
 */
public class Parameter {

    private final String symbId;
    private ScalarRhs initialEstimate;
    private ScalarRhs lowerBound;
    private ScalarRhs upperBound;	
    private Integer index;
    private boolean fixed = false;
    private boolean isStdDev = false;

    public Parameter(String symbId){
        this.symbId = symbId;
    }

    public void setParameterBounds(ScalarRhs initialEstimate,ScalarRhs lowerBound, ScalarRhs upperBound){
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

    public ScalarRhs getInitialEstimate() {
        return initialEstimate;
    }

    public void setInitialEstimate(ScalarRhs initialEstimate) {
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
