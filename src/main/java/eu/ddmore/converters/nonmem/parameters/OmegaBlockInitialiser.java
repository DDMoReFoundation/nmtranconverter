/*******************************************************************************
 * Copyright (C) 2016 Mango Solutions Ltd - All rights reserved.
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
    private final OmegaBlock omegaBlock;

    OmegaBlockInitialiser(OmegaBlock omegaBlock){
        Preconditions.checkNotNull(omegaBlock, "Omega block cannot be null");
        this.omegaBlock = omegaBlock;
    }

    /**
     * Initialise omega block with help of correlation and ordered etas.
     */
    public void initialiseOmegaBlock(){
        if(omegaBlock.getCorrelations()!=null && !omegaBlock.getCorrelations().isEmpty()){
            for(CorrelationsWrapper correlation : omegaBlock.getCorrelations()){
                //Need to set SD attribute for whole block if even a single value is from std dev
                setStdDevAttributeForOmegaBlock(correlation, omegaBlock);
                setCorrAttributeForOmegaBlock(correlation, omegaBlock);
            }
        }

        for(Iterator<Eta> it = omegaBlock.getOrderedEtas().iterator();it.hasNext();){
            Eta currentEta = it.next();
            if(!omegaBlock.getEtasToOmegas().keySet().contains(currentEta)){
                it.remove();
            }
        }

        for(Eta eta : omegaBlock.getOrderedEtas()){
            ArrayList<OmegaParameter> statements = new ArrayList<OmegaParameter>();
            for(int i=0;i<eta.getOrderInCorr();i++) statements.add(null);
            omegaBlock.addToEtaToOmegaParameter(eta, statements);
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
