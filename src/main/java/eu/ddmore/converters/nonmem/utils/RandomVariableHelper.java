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
package eu.ddmore.converters.nonmem.utils;

import java.util.regex.Pattern;

import com.google.common.base.Preconditions;

import eu.ddmore.libpharmml.dom.modeldefn.ParameterRandomVariable;
import eu.ddmore.libpharmml.dom.probonto.DistributionParameter;
import eu.ddmore.libpharmml.dom.probonto.ParameterName;
import eu.ddmore.libpharmml.dom.probonto.ProbOnto;
import eu.ddmore.libpharmml.dom.uncertml.AbstractContinuousUnivariateDistributionType;
import eu.ddmore.libpharmml.dom.uncertml.NormalDistribution;
import eu.ddmore.libpharmml.dom.uncertml.PositiveRealValueType;

/**
 * Helps to get required information from random variables.
 *  
 */
public class RandomVariableHelper {

    private static UncertMLHelper uncertMLHelper = new UncertMLHelper();
    private static ProbOntoHelper probOntoHelperHelper = new ProbOntoHelper();

    /**
     * Returns parameter name from parameter random variable. 
     * The name can be obtained from either standard deviation or variance.
     * This method will return null if name is not found in random variable provided.
     *  
     * @param rv
     * @return
     */
    public static String getNameFromParamRandomVariable(ParameterRandomVariable rv) {
        Preconditions.checkNotNull(rv, "Parameter random variable cannot be null.");
        if (rv.getDistribution().getUncertML() != null) {
            return uncertMLHelper.getReferencedVariableName(rv);
        } else if (rv.getDistribution().getProbOnto() != null) {
            return probOntoHelperHelper.getReferencedVariableName(rv);
        } else {
            return null;
        }
    }

    /**
     * Identifies if current parameter is from Standard Deviation.
     * 
     * @param rv
     * @return
     */
    public static Boolean isParamFromStdDev(ParameterRandomVariable rv) {
        Preconditions.checkNotNull(rv, "Parameter random variable cannot be null.");
        if (getDistributionTypeStdDev(rv) != null) {
            return true;
        } else if (getDistributionTypeVariance(rv) != null) {
            return false;
        } else {
            throw new IllegalStateException("Distribution type for variable " + rv.getSymbId() + " is unknown");
        }
    }

    /**
     * Get distribution type from standard deviation.
     * @param rv
     * @return
     */
    public static String getDistributionTypeStdDev(ParameterRandomVariable rv) {
        Preconditions.checkNotNull(rv, "Parameter random variable cannot be null.");
        if (rv.getDistribution().getUncertML() != null) {
            return uncertMLHelper.getVarianceFromStdDevValue(rv);
        } else if (rv.getDistribution().getProbOnto() != null) {
            return probOntoHelperHelper.getVarianceFromStdDevValue(rv);
        } else {
            throw new IllegalStateException(String.format("%s missing distribution details", rv));
        }
    }

    /**
     * Get distribution type from standard deviation.
     * @param rv
     * @return
     */
    public static String getDistributionTypeVariance(ParameterRandomVariable rv) {
        Preconditions.checkNotNull(rv, "Parameter random variable cannot be null.");
        if (rv.getDistribution().getUncertML() != null) {
            return uncertMLHelper.getVarianceValue(rv);
        } else if (rv.getDistribution().getProbOnto() != null) {
            return probOntoHelperHelper.getVarianceValue(rv);
        } else {
            throw new IllegalStateException(String.format("%s missing distribution details", rv));
        }
    }

    /**
     * Retrieves information from UncertML distributions.
     */
    private static class UncertMLHelper {

        public String getVarianceFromStdDevValue(ParameterRandomVariable rv) {
            final AbstractContinuousUnivariateDistributionType distributionType = rv.getDistribution().getUncertML()
                    .getAbstractContinuousUnivariateDistribution().getValue(); //rv.getAbstractContinuousUnivariateDistribution().getValue();
            if (distributionType instanceof NormalDistribution) {
                PositiveRealValueType stdDev = ((NormalDistribution) distributionType).getStddev();
                if(stdDev!=null) {
                    if (stdDev.getVar() != null) {
                        return stdDev.getVar().getVarId();
                    } else if (stdDev.getPrVal() != null) {
                        Double idVal = (stdDev.getPrVal() * stdDev.getPrVal());
                        return idVal.toString();
                    }
                }
            }
            return null;
        }

        public String getReferencedVariableName(ParameterRandomVariable rv) {
            final AbstractContinuousUnivariateDistributionType distributionType = rv.getDistribution().getUncertML()
                    .getAbstractContinuousUnivariateDistribution().getValue();
            
            if (distributionType instanceof NormalDistribution) {
                NormalDistribution distribution = ((NormalDistribution) distributionType);
                PositiveRealValueType val = (distribution.getStddev()!=null)?distribution.getStddev():distribution.getVariance();
                if (val != null && val.getVar()!=null) {
                    return val.getVar().getVarId();
                }
            }
            return null;
        }

        public String getVarianceValue(ParameterRandomVariable rv) {
            final AbstractContinuousUnivariateDistributionType distributionType = rv.getDistribution().getUncertML()
                    .getAbstractContinuousUnivariateDistribution().getValue();
            if (distributionType instanceof NormalDistribution) {
                PositiveRealValueType variance = ((NormalDistribution) distributionType).getVariance();
                if(variance!=null) {
                    if (variance.getVar() != null) {
                        return variance.getVar().getVarId();
                    } else if (variance.getPrVal() != null) {
                        Double idVal = (variance.getPrVal());
                        return idVal.toString();
                    }
                }
            }
            return null;
        }
    }

    /**
     * Retrieves information from ProbOnto distributions.
     */
    private static class ProbOntoHelper {

        private static final Pattern NORMAL_DISTR_NAME = Pattern.compile("Normal.*");

        public String getVarianceFromStdDevValue(ParameterRandomVariable rv) {
            ProbOnto probOnto = rv.getDistribution().getProbOnto();

            if (!NORMAL_DISTR_NAME.matcher(probOnto.getName().value()).matches()) {
                return null;
            }

            DistributionParameter param = probOnto.getParameter(ParameterName.STDEV);
            
            if(param==null || param.getAssign()==null) {
                return null;
            }
            
            if(param.getAssign().getSymbRef()!=null) {
                return param.getAssign().getSymbRef().getSymbIdRef();
            }
            
            Double val = Double.parseDouble(param.getAssign().getScalar().valueToString());
            return Double.toString(val * val);
        }

        public String getReferencedVariableName(ParameterRandomVariable rv) {
            ProbOnto probOnto = rv.getDistribution().getProbOnto();

            if (!NORMAL_DISTR_NAME.matcher(probOnto.getName().value()).matches()) {
                return null;
            }
            DistributionParameter param = (probOnto.getParameter(ParameterName.STDEV)!=null)?probOnto.getParameter(ParameterName.STDEV):probOnto.getParameter(ParameterName.VAR);

            if(param==null || param.getAssign()==null || param.getAssign().getSymbRef()==null) {
                return null;
            }
            
            return param.getAssign().getSymbRef().getSymbIdRef();
            
        }

        public String getVarianceValue(ParameterRandomVariable rv) {
            ProbOnto probOnto = rv.getDistribution().getProbOnto();

            if (!NORMAL_DISTR_NAME.matcher(probOnto.getName().value()).matches()) {
                return null;
            }

            DistributionParameter param = probOnto.getParameter(ParameterName.VAR);

            if(param==null || param.getAssign()==null) {
                return null;
            }
            
            if(param.getAssign().getSymbRef()!=null) {
                return param.getAssign().getSymbRef().getSymbIdRef();
            }

            return param.getAssign().getScalar().valueToString();
        }
    }
}
