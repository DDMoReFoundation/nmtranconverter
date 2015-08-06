/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.statements;

import java.util.ArrayList;
import java.util.List;

/**
 * This class stores associated information retrieved from dataset and to be used by other statement blocks. 
 */
public class InputColumnsProvider {

    private List<String> catCovTableColumns = new ArrayList<String>();
    private List<String> contCovTableColumns = new ArrayList<String>();
    private List<InputHeader> inputHeaders = new ArrayList<InputHeader>();
    
    public List<String> getCatCovTableColumns() {
        return catCovTableColumns;
    }
    
    public void setCatCovTableColumns(List<String> catCovTableColumns) {
        this.catCovTableColumns = catCovTableColumns;
    }
    
    public List<String> getContCovTableColumns() {
        return contCovTableColumns;
    }
    
    public void setContCovTableColumns(List<String> contCovTableColumns) {
        this.contCovTableColumns = contCovTableColumns;
    }
    
    public List<InputHeader> getInputHeaders() {
        return inputHeaders;
    }
    
    public void setInputHeaders(List<InputHeader> inputHeaders) {
        this.inputHeaders = inputHeaders;
    }
}