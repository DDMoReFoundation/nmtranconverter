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

import java.util.ArrayList;
import java.util.Iterator;

import com.google.common.base.Preconditions;

import eu.ddmore.converters.nonmem.eta.Eta;
import eu.ddmore.converters.nonmem.utils.RandomVariableHelper;

/**
 * This class initialises omega block provided as parameter.
 */
public class OmegaBlockInitialiser {

    /**
     * Initialise omega block with help of correlation and ordered etas.
     */
    public void initialiseOmegaBlock(OmegaBlock omegaBlock){
        Preconditions.checkNotNull(omegaBlock, "Omega block cannot be null");
        if(omegaBlock.getCorrelations()!=null && !omegaBlock.getCorrelations().isEmpty()){
            for(CorrelationsWrapper correlation : omegaBlock.getCorrelations()){
                //Need to set SD attribute for whole block if even a single value is from std dev
                setStdDevAttributeForOmegaBlock(correlation, omegaBlock);
                setCorrAttributeForOmegaBlock(correlation, omegaBlock);
            }
        }

        for(Iterator<Eta> it = omegaBlock.getOrderedEtas().iterator();it.hasNext();){
            Eta currentEta = it.next();
            if(!omegaBlock.getOmegaBlockEtas().contains(currentEta)){
                it.remove();
            }
        }

        for(Eta eta : omegaBlock.getOrderedEtas()){
            ArrayList<OmegaParameter> statements = new ArrayList<OmegaParameter>();
            for(int i=0;i<eta.getOrderInCorr();i++) {
                statements.add(null);
            }
            eta.setOmegaParameters(statements);
        }
    }

    private void setStdDevAttributeForOmegaBlock(CorrelationsWrapper correlation, OmegaBlock omegaBlock) {
        if(!omegaBlock.isOmegaBlockFromStdDev()){
            omegaBlock.setIsOmegaBlockFromStdDev(RandomVariableHelper.isParamFromStdDev(correlation.getFirstParamRandomVariable()) 
                || RandomVariableHelper.isParamFromStdDev(correlation.getFirstParamRandomVariable()));
        }
    }

    private void setCorrAttributeForOmegaBlock(CorrelationsWrapper correlation, OmegaBlock omegaBlock){
        if(!omegaBlock.isCorrelation() && correlation.isCorrelationCoeff()){
            omegaBlock.setIsCorrelation((true));
        }
    }
}
