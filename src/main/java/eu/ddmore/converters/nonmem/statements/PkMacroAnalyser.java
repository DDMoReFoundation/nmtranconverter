package eu.ddmore.converters.nonmem.statements;

import java.util.ArrayList;
import java.util.List;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

import crx.converter.engine.ScriptDefinition;
import crx.converter.engine.parts.StructuralBlock;
import eu.ddmore.converters.nonmem.ConversionContext;
import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.CompartmentMacro;
import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.EliminationMacro;
import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.IVMacro;
import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.MacroValue;
import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.OralMacro;
import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.PKMacro;
import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.PeripheralMacro;

/**
 * PK macro analyser will collect pk macro information 
 * and perform required analysis to add pk macro related changes to nmtran. 
 */
public class PkMacroAnalyser {

    enum AdvanType{
        ADVAN1,ADVAN2,ADVAN3,ADVAN4,ADVAN10,ADVAN11,ADVAN12;
    };

    /**
     * This method will process pk macros to collect information and set macro advan type and other details. 
     * 
     * @param context
     * @return
     */
    public PkMacroDetails analyse(ConversionContext context) {
        Preconditions.checkNotNull(context, "Conversion Context cannot be null");
        
        PkMacroDetails details = processPkMacros(context.getScriptDefinition());        
        if(!details.isEmpty()){
            details.setMacroAdvanType(captureAdvanType(details));
        }

        return details;
    }

    /**
     * This method will process compartments, eliminations, iv/orals and peripherals from all the pk macros.
     * Also it will determine and set pk macro advan type.
     * 
     */
    private PkMacroDetails processPkMacros(ScriptDefinition scriptDefinition){
        Preconditions.checkNotNull(scriptDefinition, "Script definition cannot be null");
        
        PkMacroDetails details = new PkMacroDetails();
        List<PKMacro> allPkMacros = new ArrayList<PKMacro>();
        for(StructuralBlock block : scriptDefinition.getStructuralBlocks()){
            if(block.getPKMacros()!=null){
                allPkMacros.addAll(block.getPKMacros());
            }
        }

        for(PKMacro pkMacro : allPkMacros){
            if(pkMacro instanceof CompartmentMacro){
                details.getCompartments().add((CompartmentMacro) pkMacro);
            }else if(pkMacro instanceof EliminationMacro){
                details.getEliminations().add((EliminationMacro) pkMacro);
            }else if(pkMacro instanceof IVMacro){
                details.getIvs().add((IVMacro) pkMacro);
            }else if(pkMacro instanceof OralMacro){
                details.getOrals().add((OralMacro) pkMacro);
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
    String captureAdvanType(PkMacroDetails details) {

        if(details.getCompartments().isEmpty() || details.getEliminations().isEmpty()){
            throw new IllegalArgumentException("The compartment missing from pk macro specified");
        }

        String advanType = new String();
        switch(details.getPeripherals().size()){
        case 0:
            if(isIV(details)){
                if(isKmAndVm(details)){
                    advanType = AdvanType.ADVAN10.toString();
                }else{
                    advanType = AdvanType.ADVAN1.toString();
                }
            }else if(isOral(details)){
                advanType = AdvanType.ADVAN2.toString();
            }
            break;
        case 1:
            if(isIV(details)){
                advanType = AdvanType.ADVAN3.toString();
            }else if(isOral(details)){
                advanType = AdvanType.ADVAN4.toString();
            }
            break;
        case 2:
            if(isIV(details)){
                advanType = AdvanType.ADVAN11.toString();
            }else if(isOral(details)){
                advanType = AdvanType.ADVAN12.toString();
            }
            break;
        default:
            advanType = new String();
        }
        return advanType;
    }

    /**
     * Determines if current elimination has Km as well as Vm.
     * If either or them is not present then it will return as false.  
     * 
     * @return
     */
    private boolean isKmAndVm(PkMacroDetails details){
        boolean isKm = false, isVm = false ;
        if(details.getEliminations().get(0).getListOfValue() != null){
            for(MacroValue vals : details.getEliminations().get(0).getListOfValue()){

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
     * @return
     */
    private boolean isIV(PkMacroDetails details){
        if(!details.getIvs().isEmpty() && details.getOrals().isEmpty()){
            if(details.getIvs().size()==1){
                return true;
            }
        }
        return false;
    }

    /**
     * Check if injection type is Oral
     * @return
     */
    private boolean isOral(PkMacroDetails details){
        if(details.getIvs().isEmpty() && !details.getOrals().isEmpty()){
            if(details.getOrals().size()==1){
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
        private final List<OralMacro> orals = new ArrayList<OralMacro>();
        private final List<PeripheralMacro> peripherals = new ArrayList<PeripheralMacro>();

        private String macroAdvanType = new String();

        public String getMacroAdvanType() {
            return macroAdvanType;
        }

        private String setMacroAdvanType(String advanType) {
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

        public List<OralMacro> getOrals() {
            return orals;
        }

        public List<PeripheralMacro> getPeripherals() {
            return peripherals;
        }

        public boolean isEmpty(){

            return (getCompartments().isEmpty() 
                    && getEliminations().isEmpty() 
                    && getIvs().isEmpty() 
                    && getOrals().isEmpty() 
                    && getPeripherals().isEmpty());
        }
    }
}
