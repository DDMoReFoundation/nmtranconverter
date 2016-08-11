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

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.python.google.common.base.Preconditions;

import eu.ddmore.converters.nonmem.ConversionContext;
import eu.ddmore.converters.nonmem.LocalParserHelper;
import eu.ddmore.converters.nonmem.statements.pkmacro.PkMacroAnalyser.PkMacroAttribute;
import eu.ddmore.converters.nonmem.statements.pkmacro.PkMacroAnalyser.PkMacroDetails;
import eu.ddmore.converters.nonmem.utils.Formatter;
import eu.ddmore.libpharmml.dom.commontypes.DerivativeVariable;
import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.AbsorptionOralMacro;
import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.CompartmentMacro;
import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.CompartmentMacro.Arg;
import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.AbsorptionMacro;
import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.DepotMacro;
import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.EliminationMacro;
import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.IVMacro;
import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.MacroValue;

/**
 * PK macro analyser collects pk macro information and perform required analysis to add pk macro related changes to nmtran. 
 */
public class PkMacrosEmitter {

    private final ConversionContext context;
    PkMacroDetails pkMacroDetails;
    private String macroEquation = "";
    private StringBuilder pkMacroStatement = new StringBuilder();

    public PkMacrosEmitter(ConversionContext context, PkMacroDetails pkMacroDetails) {
        Preconditions.checkNotNull(context, "Conversion context cannot be null");
        Preconditions.checkNotNull(pkMacroDetails, "Pk macro details cannot be null");
        this.context = context;
        this.pkMacroDetails = pkMacroDetails;
        initialise();
    }

    private void initialise(){
        analyseCompAmountEquation();
        PkMacroRateConstantEmitter rateConstantEmitter = new PkMacroRateConstantEmitter(context, pkMacroDetails);
        pkMacroStatement.append(getTransRelatedDefinitions());
        pkMacroStatement.append(rateConstantEmitter.getRateConstDefinitions());
        pkMacroStatement.append(getPkMacroEquations());
    }

    private StringBuilder getTransRelatedDefinitions(){
        StringBuilder transDefinitionBlock = new StringBuilder();
        String volumeSymbol = PkMacroAttribute.V.getValue();
        String clSymbol = PkMacroAttribute.CL.getValue();
        String kSymbol = Formatter.getReservedParam(PkMacroAttribute.K.getValue());
        boolean isKSymbolExist = false;
        for(EliminationMacro eliminationMacros :pkMacroDetails.getEliminations()){
            if(eliminationMacros != null ){
                for(MacroValue value :eliminationMacros.getListOfValue()){
                    String valueArgument = value.getArgument().toUpperCase().trim();

                    if(StringUtils.isNotEmpty(valueArgument)){
                        if(valueArgument.equals(PkMacroAttribute.CL.name())){
                            clSymbol = getSpecificSymbol(PkMacroAttribute.CL, value);
                        }else if(valueArgument.equals(PkMacroAttribute.V.name())){
                            volumeSymbol = getSpecificSymbol(PkMacroAttribute.V, value);
                        }else if(valueArgument.equals(PkMacroAttribute.K.name())){
                            isKSymbolExist = true;
                            kSymbol = getSpecificSymbol(PkMacroAttribute.K, value);
                        }
                    }
                }
            }
        }
        if(!isKSymbolExist){
            kSymbol = clSymbol+"/"+volumeSymbol;
        }
        transDefinitionBlock.append(Formatter.endline(PkMacroAttribute.K.name() +" = "+ kSymbol));
        return transDefinitionBlock;
    }

    private String getSpecificSymbol(PkMacroAttribute attribute, MacroValue value){
        LocalParserHelper pa = new LocalParserHelper(context);
        if(value.getAssign()!=null 
                && !value.getAssign().getSymbRef().equals(attribute.name())){
            return pa.getParsedValueForRhs(value.getAssign());
        }
        return attribute.getValue();
    }

    private void analyseCompAmountEquation(){
        if(!pkMacroDetails.getCompartments().isEmpty()){
            for(CompartmentMacro compMacro : pkMacroDetails.getCompartments()){
                for(MacroValue value : compMacro.getListOfValue()){
                    if(value.getArgument().equals(Arg.AMOUNT.toString())){
                        String equation = value.getAssign().getSymbRef().getSymbIdRef()+" = F";
                        if(StringUtils.isEmpty(macroEquation)){
                            macroEquation = Formatter.endline(equation);
                        }
                    }
                }
            }
        }
    }

    /**
     * This method looks into absorption oral and IVs to get pk macro equations.
     * @return pk macro equations
     */
    public StringBuilder getPkMacroEquations() {
        StringBuilder builder = new StringBuilder();

        if(!pkMacroDetails.getAbsorptionOrals().isEmpty()){
            for(AbsorptionOralMacro oralMacro : pkMacroDetails.getAbsorptionOrals()){
                String compartmentId = getCompartmentNameForAbsOral(oralMacro);
                if(StringUtils.isNotEmpty(compartmentId)){
                    String compartmentNumber = context.getDerivativeVarCompSequences().get(compartmentId.toUpperCase());
                    for(MacroValue value : oralMacro.getListOfValue()){
                        String macroEquation = getMacroEquation(value, Integer.parseInt(compartmentNumber));
                        if(StringUtils.isNotEmpty(macroEquation)){
                            builder.append(Formatter.endline(macroEquation));
                        }
                    }
                }
            }
            builder.append(Formatter.endline());
        }

        if(!pkMacroDetails.getIvs().isEmpty()){
            for(IVMacro ivMacro : pkMacroDetails.getIvs()){
                int compartmentNumber = getCompartmentNumberForMacro(ivMacro.getListOfValue());
                for(MacroValue value : ivMacro.getListOfValue()){
                    String macroEquation = getMacroEquation(value, compartmentNumber);
                    if(StringUtils.isNotEmpty(macroEquation)){
                        builder.append(Formatter.endline(macroEquation));
                    }
                }
            }
            builder.append(Formatter.endline());
        }
        return builder;
    }

    private String getCompartmentNameForAbsOral(AbsorptionOralMacro oralMacro) {
        for(DerivativeVariable var : context.getPkMacroDerivativeVars()){
            if(var.getOriginMacro() instanceof AbsorptionOralMacro){
                if(var.getOriginMacro().equals(oralMacro)){
                    return var.getSymbId();
                }
            }
            if(var.getOriginMacro() instanceof DepotMacro){
                AbsorptionOralMacro absMacro = new AbsorptionMacro();
                absMacro.getListOfValue().addAll(var.getOriginMacro().getListOfValue());
                if(absMacro.toString().equals(oralMacro.toString())){
                    return var.getSymbId();
                }
            }
        }
        return "";
    }

    private int getCompartmentNumberForMacro(List<MacroValue> macroValues){
        int compartmentNumber = 0;
        for (MacroValue value : macroValues){
            if(value.getArgument().equals(DepotMacro.Arg.TARGET.toString())){
                // the value will have compartment name so get order number and use it as cmt number
                String compartmentId = value.getAssign().getSymbRef().getSymbIdRef();
                compartmentNumber = Integer.parseInt(context.getDerivativeVarCompSequences().get(compartmentId));
            }else if(value.getArgument().equals(IVMacro.Arg.CMT.toString())){
                int currentMacroCmtNumber = Integer.parseInt(value.getAssign().getScalar().valueToString());
                compartmentNumber = getRelatedModelCompartmentNumber(currentMacroCmtNumber);
            }
        }
        return compartmentNumber;
    }

    private int getRelatedModelCompartmentNumber(int currentMacroCmtNumber) {
        for(CompartmentMacro macro :pkMacroDetails.getCompartments()){
            String compartmentId = "";
            int cmtNum = 0;
            for(MacroValue macroValue : macro.getListOfValue()){
                if(macroValue.getArgument().equals(CompartmentMacro.Arg.AMOUNT.toString())){
                    compartmentId = macroValue.getAssign().getSymbRef().getSymbIdRef(); 
                }
                if(macroValue.getArgument().equals(CompartmentMacro.Arg.CMT.toString())){
                    cmtNum = Integer.parseInt(macroValue.getAssign().getScalar().valueToString());
                }
            }
            if(cmtNum == currentMacroCmtNumber){
                return Integer.parseInt(context.getDerivativeVarCompSequences().get(compartmentId));
                //look for model compartment number
            }
        }
        return 0;
    }

    private String getMacroEquation(MacroValue value, Integer compartmentNumber) {
        String valueArgument = value.getArgument().toUpperCase().trim();
        if(StringUtils.isNotEmpty(valueArgument) 
                && !valueArgument.equals(PkMacroAttribute.KA.name())
                && !valueArgument.equals(PkMacroAttribute.TARGET.name())
                && value.getAssign().getSymbRef()!=null) {

            valueArgument = (PkMacroAttribute.containsMacroWithValue(valueArgument))?
                PkMacroAttribute.valueOf(valueArgument).getValue():valueArgument;
            String variable = Formatter.getReservedParam(value.getAssign().getSymbRef().getSymbIdRef());
            return valueArgument+ compartmentNumber+ " = "+ variable;
        }
        return "";
    }

    public String getMacroEquation() {
        return macroEquation;
    }

    public StringBuilder getPkMacroStatement(){
        return pkMacroStatement;
    }
}
