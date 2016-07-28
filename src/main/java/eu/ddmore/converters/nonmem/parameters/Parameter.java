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
package eu.ddmore.converters.nonmem.parameters;

import eu.ddmore.libpharmml.dom.commontypes.Rhs;
import eu.ddmore.libpharmml.dom.modeldefn.PopulationParameter;

/**
 * Creates and adds parameter details.
 *
 */
public class Parameter {

    private final String symbId;
    
    private PopulationParameter popParameter;
    private Rhs initialEstimate;
    private Rhs lowerBound;
    private Rhs upperBound;	
    private Integer index;
    private boolean fixed = false;
    private boolean isStdDev = false;
    private boolean isAssignment = false;

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

    public PopulationParameter getPopParameter() {
        return popParameter;
    }

    public void setPopParameter(PopulationParameter popParameter) {
        this.popParameter = popParameter;
    }

    public boolean isAssignment() {
        return isAssignment;
    }

    public void setAssignment(boolean isAssignment) {
        this.isAssignment = isAssignment;
    }
}
