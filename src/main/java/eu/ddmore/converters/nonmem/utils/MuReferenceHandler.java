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

import java.util.Map;
import java.util.TreeMap;

import com.google.common.base.Preconditions;

import eu.ddmore.converters.nonmem.ConversionContext;


public class MuReferenceHandler {
    
    private final ConversionContext context;
    private static final String MU = "MU_";
    private Integer muOrder = 0;
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
                    muReferences.put(++muOrder, popSymbol);
                }
            }
        }else if(!muReferences.containsValue(popSymbol)) {
            muReferences.put(++muOrder, popSymbol);
        }
        return new String(MU + muOrder);
    }

}
