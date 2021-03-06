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

import java.util.Map;

import org.apache.commons.lang.StringUtils;

import eu.ddmore.converters.nonmem.utils.DiscreteHandler;
import eu.ddmore.converters.nonmem.utils.Formatter;

/**
 * This class builds estimation statement builder
 */
public class EstimationStatementBuilder {

    public enum EstConstant{
        DEFAULT_COND("COND",""),
        SAEM("SAEM","AUTO=1 PRINT=100 CINTERVAL=30 ATOL=6 SIGL=6"),
        FOCEI ("COND","INTER NSIG=3 SIGL=9 MAXEVALS=9999 PRINT=10 NOABORT"),
        FOCE ("COND","NSIG=3 SIGL=9 MAXEVALS=9999 PRINT=10 NOABORT"),
        FO ("ZERO","NSIG=3 SIGL=9 MAXEVALS=9999 PRINT=10 NOABORT"),
        TTE_DATA_LAPLACE_OPTION(""," LIKELIHOOD LAPLACE NUMERICAL NOINTERACTION"),
        COUNT_DATA_LAPLACE_OPTION(""," -2LL LAPLACE NOINTERACTION");

        private String method;
        private String statement;
        EstConstant(String method, String statement){
            this.method = method;
            this.statement = statement;
        }

        public String getStatement() {
            return statement;
        }

        public String getMethod() {
            return method;
        }
    }

    public enum Method{
        FO, FOCE, FOCEI, SAEM
    }

    private static final String METHOD = "METHOD=";

    private final Map<String, String> estOptions;

    private final DiscreteHandler discreteHandler;
    private boolean isSAEM = false;

    public EstimationStatementBuilder(DiscreteHandler discreteHandler, Map<String, String> estOptions) {
        this.discreteHandler = discreteHandler;
        this.estOptions = estOptions;
    }

    /**
     * Builds estimation statement from algorithm method name and estimation options.
     * 
     * @param methodName algorithm method name
     * @return estimation statement
     */
    public StringBuilder buildEstimationStatementFromAlgorithm(String methodName) {
        StringBuilder statement = new StringBuilder();
        Boolean hasCustomOptions = !estOptions.isEmpty(); 

        statement.append(Formatter.endline()+Formatter.est());
        statement.append(METHOD);
        if (StringUtils.isNotBlank(methodName)) {
            String methodDefinition = methodName.trim().toUpperCase();
            Method method = Method.valueOf(methodDefinition);

            switch(method){
            case FO:
                statement.append(buildEstStatementWithOptions(EstConstant.FO.getMethod(), EstConstant.FO.getStatement(),
                    hasCustomOptions, false));
                break;
            case FOCE:
                statement.append(buildEstStatementWithOptions(EstConstant.FOCE.getMethod(), EstConstant.FOCE.getStatement(),
                    hasCustomOptions, true));
                break;
            case FOCEI:
                statement.append(buildEstStatementWithOptions(EstConstant.FOCEI.getMethod(), EstConstant.FOCEI.getStatement(),
                    hasCustomOptions, true));
                break;
            case SAEM:
                setSAEM(true);
                statement.append(buildEstStatementWithOptions(EstConstant.SAEM.getMethod(), EstConstant.SAEM.getStatement(),
                    hasCustomOptions, true));
                break;
            default:
                statement.append(buildEstStatementWithOptions(methodDefinition+" ","", hasCustomOptions, true));
                break;
            }
        } else {
            statement.append(EstConstant.DEFAULT_COND.getStatement());
        }
        return statement;
    }

    private StringBuilder buildEstStatementWithOptions(String method, String defaultOptions, 
            Boolean hasCustomOptions, Boolean isDiscreteOptions){
        StringBuilder statement = new StringBuilder();
        statement.append(method+" ");
        statement.append((hasCustomOptions)?
            addCustonOptionsToStatement(): defaultOptions);
        if(isDiscreteOptions){
            statement.append(appendDiscreteEstOptions());
        }
        return statement;
    }

    private StringBuilder addCustonOptionsToStatement(){
        StringBuilder options = new StringBuilder();
        for(String option : estOptions.keySet()){
            if(estOptions.get(option).isEmpty()){
                options.append(" "+option);
            }else {
                options.append(" "+option+"="+estOptions.get(option));
            }
        }
        return options;
    }

    private StringBuilder appendDiscreteEstOptions() {
        StringBuilder statement = new StringBuilder();
        if(discreteHandler.isCountData()){
            statement.append(EstConstant.COUNT_DATA_LAPLACE_OPTION.getStatement());
        }else if(discreteHandler.isTimeToEventData()){
            statement.append(EstConstant.TTE_DATA_LAPLACE_OPTION.getStatement());
        }
        return statement;
    }

    public boolean isSAEM() {
        return isSAEM;
    }

    public void setSAEM(boolean isSAEM) {
        this.isSAEM = isSAEM;
    }
}
