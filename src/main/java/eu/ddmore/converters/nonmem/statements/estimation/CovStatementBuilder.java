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
