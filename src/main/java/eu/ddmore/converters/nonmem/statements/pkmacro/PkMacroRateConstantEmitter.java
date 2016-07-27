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
package eu.ddmore.converters.nonmem.statements.pkmacro;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import eu.ddmore.converters.nonmem.ConversionContext;
import eu.ddmore.converters.nonmem.statements.pkmacro.PkMacroAnalyser.PkMacroAttribute;
import eu.ddmore.converters.nonmem.statements.pkmacro.PkMacroAnalyser.PkMacroDetails;
import eu.ddmore.converters.nonmem.utils.Formatter;
import eu.ddmore.converters.nonmem.utils.ScalarValueHandler;
import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.CompartmentMacro;
import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.MacroValue;
import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.PeripheralMacro;
import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.CompartmentMacro.Arg;

/**
 * Analyses rate constant details and creates rate constant definition using these details.
 */
public class PkMacroRateConstantEmitter {

    ConversionContext context;
    PkMacroDetails pkMacroDetails;
    private Integer cmtValue=0;
    private final Set<PkMacroRateConstantsPair> rateConstants = new HashSet<PkMacroRateConstantsPair>();

    enum NmtranRateConstant{
        K12, K21, K13, K31, K23, K32, K24, K42;
    }

    public PkMacroRateConstantEmitter(ConversionContext context, PkMacroDetails pkMacroDetails) {
        this.context = context;
        this.pkMacroDetails = pkMacroDetails;
        initialise();
    }

    private void initialise(){
        cmtValue = getPkMacroCompartmentNumber();
        analyseRateConstants();
    }

    public StringBuilder getRateConstDefinitions(){
        StringBuilder rateConstantBlock = new StringBuilder();

        switch(pkMacroDetails.getMacroAdvanType()){
        case ADVAN3 :
            for(PkMacroRateConstantsPair rateConstPair : rateConstants){
                rateConstantBlock.append(Formatter.endline(NmtranRateConstant.K12 +" = "+rateConstPair.getCompartmentRateConstantSymbol()));
                rateConstantBlock.append(Formatter.endline(NmtranRateConstant.K21 +" = "+rateConstPair.getPeripheralRateConstantSymbol()));
                break;
            }
            break;
        case ADVAN4 :
            for(PkMacroRateConstantsPair rateConstPair : rateConstants){
                rateConstantBlock.append(Formatter.endline(NmtranRateConstant.K23 +" = "+rateConstPair.getCompartmentRateConstantSymbol()));
                rateConstantBlock.append(Formatter.endline(NmtranRateConstant.K32 +" = "+rateConstPair.getPeripheralRateConstantSymbol()));
                break;
            }
            break;
        case ADVAN11 :{
            Iterator<PkMacroRateConstantsPair> itr = rateConstants.iterator();
            for(int i=0; itr.hasNext();i++){
                if(i==0){
                    PkMacroRateConstantsPair firstRateconstant = itr.next();
                    rateConstantBlock.append(Formatter.endline(NmtranRateConstant.K12 +" = "+firstRateconstant.getCompartmentRateConstantSymbol()));
                    rateConstantBlock.append(Formatter.endline(NmtranRateConstant.K21 +" = "+firstRateconstant.getPeripheralRateConstantSymbol()));
                }else{
                    PkMacroRateConstantsPair nextRateconstant = itr.next();
                    rateConstantBlock.append(Formatter.endline(NmtranRateConstant.K13 +" = "+nextRateconstant.getCompartmentRateConstantSymbol()));
                    rateConstantBlock.append(Formatter.endline(NmtranRateConstant.K31 +" = "+nextRateconstant.getPeripheralRateConstantSymbol()));
                }
            }
        }
        break;
        case ADVAN12 :{
            Iterator<PkMacroRateConstantsPair> itr = rateConstants.iterator();
            for(int i=0; itr.hasNext();i++){
                if(i==0){
                    PkMacroRateConstantsPair firstRateconstant = itr.next();
                    rateConstantBlock.append(Formatter.endline(NmtranRateConstant.K23 +" = "+firstRateconstant.getCompartmentRateConstantSymbol()));
                    rateConstantBlock.append(Formatter.endline(NmtranRateConstant.K32 +" = "+firstRateconstant.getPeripheralRateConstantSymbol()));
                }else{
                    PkMacroRateConstantsPair nextRateconstant = itr.next();
                    rateConstantBlock.append(Formatter.endline(NmtranRateConstant.K24 +" = "+nextRateconstant.getCompartmentRateConstantSymbol()));
                    rateConstantBlock.append(Formatter.endline(NmtranRateConstant.K42 +" = "+nextRateconstant.getPeripheralRateConstantSymbol()));
                }
            }
        }
        break;
        default :
            break;
        }

        return rateConstantBlock;
    }

    private void analyseRateConstants(){
        for(PeripheralMacro peripheralMacro :pkMacroDetails.getPeripherals()){
            for(MacroValue macroValue : peripheralMacro.getListOfValue()){
                if(macroValue.getArgument().toUpperCase().startsWith(PkMacroAttribute.K.name()+"_")){
                    getRateConstant(macroValue);
                }
            }
        }
    }

    private Integer getPkMacroCompartmentNumber(){
        if(!pkMacroDetails.getCompartments().isEmpty()){
            for(CompartmentMacro compMacro : pkMacroDetails.getCompartments()){
                for(MacroValue value : compMacro.getListOfValue()){
                    if(value.getArgument().equals(Arg.CMT.toString())){
                        return ScalarValueHandler.getValueFromScalarRhs(value.getAssign()).intValue();
                    }
                }
            }
        }
        return 0;
    }

    private void getRateConstant(MacroValue macroValue) {
        String rateConstantArgument = macroValue.getArgument();

        String[] rateConstDetails = rateConstantArgument.split("_");
        if(rateConstDetails.length!=3){
            throw new IllegalArgumentException("The rate constant "+rateConstantArgument+ " is not valid.");
        }

        final String rateConstantSymbol = context.getLocalParserHelper().getParsedValueForRhs(macroValue.getAssign());
        Integer fromCmtNumber = Integer.parseInt(rateConstDetails[1]);
        Integer toCmtNumber = Integer.parseInt(rateConstDetails[2]);

        addRateConstantSymbol(rateConstantSymbol, fromCmtNumber, toCmtNumber);
    }

    private void addRateConstantSymbol(String rateConstantSymbol, Integer fromCmtNumber, Integer toCmtNumber) {

        Integer compartmentNum = fromCmtNumber;
        Integer peripheralNum = toCmtNumber;

        if(!(cmtValue == fromCmtNumber)){
            compartmentNum = toCmtNumber;
            peripheralNum = fromCmtNumber;
        }

        for(PkMacroRateConstantsPair nextPair : rateConstants){
            if(nextPair.getCompartmentCmtNumber()!=0 
                    && nextPair.getCompartmentCmtNumber() == compartmentNum){

                if(nextPair.getCompartmentCmtNumber()!=0 
                        && nextPair.getPeripheralCmtNumber() == peripheralNum){
                    nextPair.setPeripheralCmtNumber(peripheralNum);
                    if(StringUtils.isEmpty(nextPair.getPeripheralRateConstantSymbol())){
                        nextPair.setPeripheralRateConstantSymbol(rateConstantSymbol);
                    }
                    return;
                }
            }
        }
        //no element found with this compartment number
        PkMacroRateConstantsPair newPair = new PkMacroRateConstantsPair();
        newPair.setCompartmentCmtNumber(compartmentNum);
        newPair.setPeripheralCmtNumber(peripheralNum);
        newPair.setCompartmentRateConstantSymbol(rateConstantSymbol);
        rateConstants.add(newPair);
    }
}
