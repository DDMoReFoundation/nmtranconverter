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
package eu.ddmore.converters.nonmem.eta;

import static org.junit.Assert.*;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Before;
import org.junit.Test;

/**
 * Test for Eta class.
 */
public class EtaTest {

    private Eta eta1;
    private Eta eta2;
    private Eta eta3;
    @Before
    public void setUp() throws Exception {
        String etaSymbol1 = "firstEta";
        String etaSymbol2 = "secondEta";
        String etaSymbol3 = "thirdEta";
        eta1 = new Eta(etaSymbol1);
        eta2 = new Eta(etaSymbol2);
        eta3 = new Eta(etaSymbol3);

        eta1.setOrder(1);
        eta2.setOrder(2);
        eta3.setOrder(3);
    }

    /**
     * This method verifies compareTo method implementation of Eta for ordered set.
     * This is one of the required features of Eta.
     */
    @Test
    public void shouldVerifyCompareToOfEtaForOrderedSet(){
        Set<Eta> etas = new TreeSet<Eta>();

        Eta eta4 = new Eta("eta");
        eta4.setOrder(4);

        etas.add(eta4);
        etas.add(eta1);
        etas.add(eta2);
        etas.add(eta3);

        assertEquals("should add new eta to set",4,etas.size());

        int index = getIndexFromSet(etas, eta4);
        assertEquals("Should add eta with order "+eta4.getOrder()+" at correct place in set", 3, index);
    }

    @Test
    public void shouldCompareEtasUsingSymbol(){
        Set<Eta> etas = new TreeSet<Eta>();

        Eta eta4 = new Eta(eta3.getEtaSymbol());
        eta4.setOrder(4);

        etas.add(eta4);
        etas.add(eta1);
        etas.add(eta2);
        etas.add(eta3);

        assertTrue("should return success for eta comparison if eta symbols are same", eta4.equals(eta3));
        int index = getIndexFromSet(etas, eta4);
        //Should add eta to "Set" even though the etas are with same eta symbol
        assertEquals("Should add eta with order "+eta4.getOrder()+" at "+3+" in set",3, index);
    }

    @Test
    public void shouldNotAddEtaWithExistingOrder(){
        Set<Eta> etas = new TreeSet<Eta>();

        Eta eta4 = new Eta("new_eta");
        eta4.setOrder(3);

        etas.add(eta1);
        etas.add(eta2);
        etas.add(eta3);
        etas.add(eta4);

        int index = getIndexFromSet(etas, eta4);
        //Should add eta even though the etas are with same eta symbol
        assertTrue("etas contains eta 3", (getIndexFromSet(etas, eta3)!=-1));
        assertEquals("Should not add eta with order "+eta4.getOrder()+" as there is already one with the same order.", -1, index);
    }

    private int getIndexFromSet(Set<Eta> etas, Eta etaToBeConfirmed) {

        int index = 0;
        Iterator<Eta> itr = etas.iterator();
        while(itr.hasNext()){
            Eta eta = itr.next();
            if(eta.getEtaSymbol().equals(etaToBeConfirmed.getEtaSymbol()) 
                    && eta.getOrder() == etaToBeConfirmed.getOrder()){
                break;
            }else{
                index++;
            }
        }
        if(index >= etas.size()){
            return -1;
        }
        return index;
    }
}
