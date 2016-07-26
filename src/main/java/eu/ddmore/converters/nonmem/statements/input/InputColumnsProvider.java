/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.statements.input;

import java.util.ArrayList;
import java.util.List;

import eu.ddmore.libpharmml.dom.dataset.ColumnType;

/**
 * This class stores associated information retrieved for input headers, associated categorical or continuous cov table colums 
 * and to be used by other statement blocks.
 * TODO: suggested change: moving the methods that populate instances of InputColumnsProvider from InputStatement to here. 
 */
public class InputColumnsProvider {

    private final List<String> catCovTableColumns = new ArrayList<String>();
    private final List<String> contCovTableColumns = new ArrayList<String>();
    private final List<InputColumn> inputHeaders = new ArrayList<InputColumn>();
    private boolean isCMTColumnPresent = false;

    /**
     * This method is to add continuous covariate table column to cotab table columns list.
     * @param contCov continuous cov table column
     */
    public void addContCovTableColumn(String contCov){
        contCovTableColumns.add(contCov);
    }

    /**
     * This method is to add categorical covariate table column to catab table columns list.
     * @param contCov categorical cov table column
     */
    public void addCatCovTableColumn(String catCov){
        catCovTableColumns.add(catCov);
    }

    /**
     * This method adds input header column to input header columns list.
     * @param inputHeader input header column
     */
    public void addInputHeaders(InputColumn inputHeader){
        isCMTColumnPresent = (!isCMTColumnPresent)?inputHeader.getColumnType().equals(ColumnType.CMT):isCMTColumnPresent;
        inputHeaders.add(inputHeader);
    }

    public boolean isCMTColumnPresent() {
        return isCMTColumnPresent;
    }

    public void setCMTColumnPresent(boolean isCMTColumnPresent) {
        this.isCMTColumnPresent = isCMTColumnPresent;
    }

    public List<String> getCatCovTableColumns() {
        return catCovTableColumns;
    }

    public List<String> getContCovTableColumns() {
        return contCovTableColumns;
    }

    public List<InputColumn> getInputHeaders() {
        return inputHeaders;
    }
}