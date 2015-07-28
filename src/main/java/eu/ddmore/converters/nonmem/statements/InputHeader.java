/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.statements;

/**
 * This class stores input header details.
 */
public class InputHeader {
    private String columnId;
    private boolean dropped;
    private Long columnSequence;
    
    public InputHeader(String columnId, boolean isDropped, Long columnSeq){
        this.columnId = columnId;
        this.dropped = isDropped;
        this.columnSequence = columnSeq;
    }

    public String getColumnId() {
        return columnId;
    }

    public void setColumnId(String columnId) {
        this.columnId = columnId;
    }

    public boolean isDropped() {
        return dropped;
    }

    public void setDropped(boolean dropped) {
        this.dropped = dropped;
    }

    public Long getColumnSequence() {
        return columnSequence;
    }

    public void setColumnSequence(Long columnSequence) {
        this.columnSequence = columnSequence;
    }
}
