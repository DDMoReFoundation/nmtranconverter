/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.statements;

import eu.ddmore.libpharmml.dom.dataset.ColumnType;

/**
 * This class stores input header details.
 */
public class InputHeader {
    private String columnId;
    private boolean dropped;
    private Long columnSequence;
    private ColumnType columnType;
    
    public InputHeader(String columnId, boolean isDropped, Long columnSeq, ColumnType columnType){
        this.columnId = columnId;
        this.dropped = isDropped;
        this.columnSequence = columnSeq;
        this.columnType = columnType;
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

    public ColumnType getColumnType() {
        return columnType;
    }

    public void setColumnType(ColumnType columnType) {
        this.columnType = columnType;
    }
}
