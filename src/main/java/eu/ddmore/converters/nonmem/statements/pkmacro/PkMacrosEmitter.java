/*******************************************************************************
 * Copyright (C) 2016 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.statements.pkmacro;

import org.apache.commons.lang.StringUtils;
import org.python.google.common.base.Preconditions;

import eu.ddmore.converters.nonmem.ConversionContext;
import eu.ddmore.converters.nonmem.LocalParserHelper;
import eu.ddmore.converters.nonmem.statements.pkmacro.PkMacroAnalyser.PkMacroAttribute;
import eu.ddmore.converters.nonmem.statements.pkmacro.PkMacroAnalyser.PkMacroDetails;
import eu.ddmore.converters.nonmem.utils.Formatter;
import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.AbsorptionOralMacro;
import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.CompartmentMacro;
import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.CompartmentMacro.Arg;
import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.EliminationMacro;
import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.IVMacro;
import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.MacroValue;

/**
 * PK macro analyser collects pk macro information and perform required analysis to add pk macro related changes to nmtran. 
 */
public class PkMacrosEmitter {

    private final ConversionContext context;
    PkMacroDetails pkMacroDetails;
    //    private Integer cmtValue=0;
    private String macroEquation = "";
    private StringBuilder pkMacroStatement = new StringBuilder();

    //    private final Set<PkMacroRateConstantsPair> rateConstants = new HashSet<PkMacroRateConstantsPair>();

    public PkMacrosEmitter(ConversionContext context, PkMacroDetails pkMacroDetails) {
        Preconditions.checkNotNull(context, "Conversion context cannot be null");
        Preconditions.checkNotNull(pkMacroDetails, "Pk macro details cannot be null");
        this.context = context;
        this.pkMacroDetails = pkMacroDetails;
        initialise();
    }

    private void initialise(){
        analyseCompAmountEquation();
        //analyseRateConstants();
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
                        if(StringUtils.isNotEmpty(macroEquation)){
                            macroEquation = Formatter.endline(equation);
                        }
                    }
                }
            }
        }
    }

    public StringBuilder getPkMacroEquations() {
        StringBuilder builder = new StringBuilder();

        if(!pkMacroDetails.getAbsorptionOrals().isEmpty()){
            for(AbsorptionOralMacro oralMacro : pkMacroDetails.getAbsorptionOrals()){
                for(MacroValue value : oralMacro.getListOfValue()){
                    String macroEquation = getMacroEquation(value, pkMacroDetails.getAbsOralCompNumber());
                    if(StringUtils.isNotEmpty(macroEquation)){
                        builder.append(Formatter.endline(macroEquation));
                    }
                }
            }
            builder.append(Formatter.endline());
        }

        if(!pkMacroDetails.getIvs().isEmpty()){
            for(IVMacro oralMacro : pkMacroDetails.getIvs()){
                for(MacroValue value : oralMacro.getListOfValue()){
                    String macroEquation = getMacroEquation(value, 1);
                    if(StringUtils.isNotEmpty(macroEquation)){
                        builder.append(Formatter.endline(macroEquation));
                    }
                }
            }
            builder.append(Formatter.endline());
        }
        return builder;
    }

    private String getMacroEquation(MacroValue value, Integer compartmentNumber) {
        String valueArgument = value.getArgument().toUpperCase().trim();
        if(StringUtils.isNotEmpty(valueArgument) 
                && !valueArgument.equals(PkMacroAttribute.KA.name()) 
                && value.getAssign().getSymbRef()!=null) {

            PkMacroAttribute attribute= PkMacroAttribute.valueOf(valueArgument);
            String variable = Formatter.getReservedParam(value.getAssign().getSymbRef().getSymbIdRef());
            return attribute.getValue()+ compartmentNumber+ " = "+ variable;
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
