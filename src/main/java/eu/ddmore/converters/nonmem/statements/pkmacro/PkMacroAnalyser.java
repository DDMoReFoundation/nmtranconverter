/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.statements.pkmacro;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

import crx.converter.engine.ScriptDefinition;
import crx.converter.spi.blocks.StructuralBlock;
import eu.ddmore.converters.nonmem.ConversionContext;
import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.AbsorptionMacro;
import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.AbsorptionOralMacro;
import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.CompartmentMacro;
import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.DepotMacro;
import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.EliminationMacro;
import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.IVMacro;
import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.MacroValue;
import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.PKMacro;
import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.PeripheralMacro;

/**
 * PK macro analyser will collect pk macro information 
 * and perform required analysis to add pk macro related changes to nmtran. 
 */
public class PkMacroAnalyser {

    /**
     * Enum with pk macro attributes and respective values for these attributes.
     */
    enum PkMacroAttribute{
        P("F"), TLAG("ALAG"), KA("KA"), V("V"), CL("CL"), K("K"), TARGET("TARGET");

        private String value;

        private PkMacroAttribute(String value) {
            this.value = value;
        }

        public String getValue(){
            return value;
        }

    }

    public enum AdvanType{
        ADVAN1,ADVAN2,ADVAN3,ADVAN4,ADVAN10,ADVAN11,ADVAN12,ADVAN13, NONE;
    };

    /**
     * This method will process pk macros to collect information and set macro advan type and other details. 
     * 
     * @param context
     * @return pk macro details
     */
    public PkMacroDetails analyse(ConversionContext context) {
        Preconditions.checkNotNull(context, "Conversion Context cannot be null");
        PkMacroDetails details = processPkMacros(context);
        if(!details.isEmpty() ){
            details.setMacroAdvanType(captureAdvanType(details));
        }
        return details;
    }

    /**
     * This method processes compartments, eliminations, iv/orals and peripherals from all the pk macros.
     * Also it will determine and set pk macro advan type.
     */
    private PkMacroDetails processPkMacros(ConversionContext context){
        ScriptDefinition scriptDefinition = context.getScriptDefinition();
        Preconditions.checkNotNull(scriptDefinition, "Script definition cannot be null");

        PkMacroDetails details = new PkMacroDetails();
        List<PKMacro> allPkMacros = new ArrayList<PKMacro>();
        for(StructuralBlock block : scriptDefinition.getStructuralBlocks()){
            if(block.getPKMacros()!=null){
                allPkMacros.addAll(block.getPKMacros());
            }
        }

        int compCount=0;
        for(PKMacro pkMacro : allPkMacros){
            if(pkMacro instanceof CompartmentMacro){
                details.getCompartments().add((CompartmentMacro) pkMacro);
                //add compartment number depending upon order of occurrence.
                details.setCompartmentCompNumber(++compCount);
            }else if(pkMacro instanceof EliminationMacro){
                details.getEliminations().add((EliminationMacro) pkMacro);
            }else if(pkMacro instanceof DepotMacro){
                boolean isOral = false;
                for(MacroValue value : pkMacro.getListOfValue()){
                    String valueArgument = value.getArgument().toUpperCase().trim();
                    if(StringUtils.isNotEmpty(valueArgument)){
                        if(valueArgument.equals(PkMacroAttribute.KA.name())){
                            isOral = true;
                        }
                    }
                }
                if(isOral){
                    AbsorptionOralMacro absMacro = new AbsorptionMacro();
                    absMacro.getListOfValue().addAll(pkMacro.getListOfValue());
                    details.getAbsorptionOrals().add(absMacro);
                }else {
                    for(MacroValue value : pkMacro.getListOfValue()){
                        if(value.getArgument().equalsIgnoreCase(PkMacroAttribute.TARGET.name())){
                        }
                    }
                    IVMacro ivMacro = new IVMacro();
                    ivMacro.getListOfValue().addAll(pkMacro.getListOfValue());
                    details.getIvs().add(ivMacro);
                }
            }else if(pkMacro instanceof IVMacro){
                details.getIvs().add((IVMacro) pkMacro);
            }else if(pkMacro instanceof AbsorptionOralMacro){
                details.getAbsorptionOrals().add((AbsorptionOralMacro) pkMacro);
                //add absorption compartment number depending upon order of occurrence.
                details.setAbsOralCompNumber(++compCount);
            }else if(pkMacro instanceof PeripheralMacro){
                details.getPeripherals().add((PeripheralMacro) pkMacro);
            }
        }
        return details;
    }

    /*
     *             CMT     IV      ORAL    PERIF   ELIM
     * ADVAN-1      1       1       0       0       1   k
     * ADVAN-2      1       0       1       0       1   k
     * ADVAN-3      1       1       0       1       1   k
     * ADVAN-4      1       0       1       1       1   k
     * ADVAN-10     1       1       0       0       1   km & vm
     * ADVAN-11     1       1       0       2       1   k
     * ADVAN-12     1       0       1       2       1   k
     */

    /**
     * Retrieves advan type from compartments, eliminations and peripherals available for pk macro.
     * 
     * @return advan type
     */
    @VisibleForTesting
    AdvanType captureAdvanType(PkMacroDetails details) {

        /*
         * TODO: We need to get next version of libpharmml-pkmacro (0.2.2), 
         * where pkmacro-derivative vars and non-pkmacro derivative vars are differentiated. 
         * Then this can be enabled to throw exception when non-pkmacro derivative vars are not present.
        if(details.getCompartments().isEmpty() || details.getEliminations().isEmpty()){
            throw new IllegalArgumentException("The compartment missing from pk macro specified");
        }*/

        AdvanType advanType = AdvanType.NONE;
        if(details.getCompartments().size()>1){
            return advanType;
        }

        switch(details.getPeripherals().size()){
        case 0:
            if(isIV(details)){
                if(isKmAndVm(details)){
                    advanType = AdvanType.ADVAN10;
                }else{
                    advanType = AdvanType.ADVAN1;
                }
            }else if(isOral(details)){
                advanType = AdvanType.ADVAN2;
            }
            break;
        case 1:
            if(isIV(details)){
                advanType = AdvanType.ADVAN3;
            }else if(isOral(details)){
                advanType = AdvanType.ADVAN4;
            }
            break;
        case 2:
            if(isIV(details)){
                advanType = AdvanType.ADVAN11;
            }else if(isOral(details)){
                advanType = AdvanType.ADVAN12;
            }
            break;
        default:
            advanType = AdvanType.NONE;
        }
        return advanType;
    }

    /**
     * Determines if current elimination has Km as well as Vm.
     * If either or them is not present then it will return as false.  
     */
    private boolean isKmAndVm(PkMacroDetails details){
        boolean isKm = false, isVm = false ;
        final List<MacroValue> eliminationMacroValues = details.getEliminations().get(0).getListOfValue();
        if(eliminationMacroValues != null && !eliminationMacroValues.isEmpty()){
            for(MacroValue vals : eliminationMacroValues){

                if(vals.getSymbRef()!=null){
                    String variable = vals.getSymbRef().getSymbIdRef();
                    if(variable.equalsIgnoreCase("Km")){
                        isKm = true;
                    }
                    if(variable.equalsIgnoreCase("Vm")){
                        isVm = true;
                    }
                }
            }
        }
        return (isKm && isVm);
    }

    /**
     * Check if injection type is IV
     */
    private boolean isIV(PkMacroDetails details){
        if(!details.getIvs().isEmpty() && details.getAbsorptionOrals().isEmpty()){
            if(details.getIvs().size()==1){
                return true;
            }
        }
        return false;
    }

    /**
     * Check if injection type is Oral
     */
    private boolean isOral(PkMacroDetails details){
        if(details.getIvs().isEmpty() && !details.getAbsorptionOrals().isEmpty()){
            if(details.getAbsorptionOrals().size()==1){
                return true;
            }
        }
        return false;
    }

    /**
     * This class will store pk macro details from pharmML
     */
    public static class PkMacroDetails {

        private final List<CompartmentMacro> compartments = new ArrayList<CompartmentMacro>();
        private final List<EliminationMacro> eliminations = new ArrayList<EliminationMacro>();
        private final List<IVMacro> ivs = new ArrayList<IVMacro>();
        private final List<AbsorptionOralMacro> orals = new ArrayList<AbsorptionOralMacro>();
        private final List<PeripheralMacro> peripherals = new ArrayList<PeripheralMacro>();
        private int absOralCompNumber=0;
        private int cmtCompNumber=0;

        private AdvanType macroAdvanType = AdvanType.NONE;

        public AdvanType getMacroAdvanType() {
            return macroAdvanType;
        }

        private AdvanType setMacroAdvanType(AdvanType advanType) {
            return macroAdvanType = advanType;
        }

        public List<CompartmentMacro> getCompartments() {
            return compartments;
        }

        public List<EliminationMacro> getEliminations() {
            return eliminations;
        }

        public List<IVMacro> getIvs() {
            return ivs;
        }

        public List<AbsorptionOralMacro> getAbsorptionOrals() {
            return orals;
        }

        public List<PeripheralMacro> getPeripherals() {
            return peripherals;
        }

        public int getAbsOralCompNumber() {
            return absOralCompNumber;
        }

        public void setAbsOralCompNumber(int absOralCompNumber) {
            this.absOralCompNumber = absOralCompNumber;
        }

        public int getCompartmentCompNumber() {
            return cmtCompNumber;
        }

        public void setCompartmentCompNumber(int cmtCompNumber) {
            this.cmtCompNumber = cmtCompNumber;
        }

        public boolean isEmpty(){

            return (getCompartments().isEmpty() 
                    && getEliminations().isEmpty() 
                    && getIvs().isEmpty() 
                    && getAbsorptionOrals().isEmpty() 
                    && getPeripherals().isEmpty());
        }
    }
}
