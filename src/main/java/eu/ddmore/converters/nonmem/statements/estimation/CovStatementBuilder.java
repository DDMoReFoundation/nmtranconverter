/*******************************************************************************
 * Copyright (C) 2016 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.statements.estimation;

import java.util.LinkedHashMap;
import java.util.Map;

import eu.ddmore.converters.nonmem.utils.Formatter;

/**
 * This class builds covariate statement builder
 */
public class CovStatementBuilder {

    private final Map<String, String> covOptions = new LinkedHashMap<String, String>();
    private final StringBuilder covStatement = new StringBuilder();
    private boolean isCovFound = false;

    /**
     * Builds covariate statement using cov options.
     */
    public void buildCovStatement(){
        if(isCovFound ){
            covStatement.append(Formatter.endline()+Formatter.cov());
            if(!covOptions.isEmpty()){
                for(String option : covOptions.keySet()){
                    if(covOptions.get(option).isEmpty()){
                        covStatement.append(" "+option);
                    }else {
                        covStatement.append(" "+option+"="+covOptions.get(option));
                    }
                }
            }
            covStatement.append(Formatter.endline());
        }
    }

    public StringBuilder getCovStatement() {
        return covStatement;
    }

    public Map<String, String> getCovOptions() {
        return covOptions;
    }

    public boolean isCovFound() {
        return isCovFound;
    }

    public void setCovFound(boolean isCovFound) {
        this.isCovFound = isCovFound;
    }

}
