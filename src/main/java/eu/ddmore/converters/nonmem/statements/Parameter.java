/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.statements;

import eu.ddmore.libpharmml.dom.commontypes.Rhs;

/**
 * Creates and adds parameter details.
 *
 */
public class Parameter {

    private final String symbId;
    private Rhs initialEstimate;
    private Rhs lowerBound;
    private Rhs upperBound;	
    private Integer index;
    private boolean fixed = false;
    private boolean isStdDev = false;

    public Parameter(String symbId){
        this.symbId = symbId;
    }

    public void setParameterBounds(Rhs initialEstimate,Rhs lowerBound, Rhs upperBound){
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

    public Rhs getInitialEstimate() {
        return initialEstimate;
    }

    public void setInitialEstimate(Rhs initialEstimate) {
        this.initialEstimate = initialEstimate;
    }

    public Rhs getLowerBound() {
        return lowerBound;
    }

    public void setLowerBound(Rhs lowerBound) {
        this.lowerBound = lowerBound;
    }

    public Rhs getUpperBound() {
        return upperBound;
    }

    public void setUpperBound(Rhs upperBound) {
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
