/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.utils;

import java.util.Map;
import java.util.TreeMap;

import com.google.common.base.Preconditions;

import eu.ddmore.converters.nonmem.ConversionContext;


public class MuReferenceHandler {
    
    private final ConversionContext context;
    private static final String MU = "MU_";
    private Integer MU_ORDER = 0;
    private final Map<Integer,String> muReferences = new TreeMap<Integer,String>();
    
    public MuReferenceHandler(ConversionContext context){
        this.context = context;
    }
    
    /**
     * Get MU symbol for population parameter symbol provided
     * @param popSymbol
     * @return respective MU symbol
     */
    public String getMUSymbol(String popSymbol){
        Map<Integer, String> orderedThetas = context.getOrderedThetasHandler().getOrderedThetas();
        Preconditions.checkArgument(!orderedThetas.isEmpty(), "Ordered thetas do not exist or not arranged yet.");
        if(orderedThetas.containsValue(popSymbol)){
            for(Integer thetaOrder: orderedThetas.keySet()){
                if(popSymbol.equals(orderedThetas.get(thetaOrder)) && !muReferences.containsValue(popSymbol)){
                    muReferences.put(++MU_ORDER, popSymbol);
                }
            }
        }else if(!muReferences.containsValue(popSymbol)) {
            muReferences.put(++MU_ORDER, popSymbol);
        }
        return new String(MU + MU_ORDER);
    }

}
