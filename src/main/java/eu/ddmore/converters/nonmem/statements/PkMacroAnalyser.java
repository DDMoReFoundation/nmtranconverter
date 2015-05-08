package eu.ddmore.converters.nonmem.statements;

import java.util.ArrayList;
import java.util.List;

import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.CompartmentMacro;
import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.EliminationMacro;
import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.IVMacro;
import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.MacroValue;
import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.OralMacro;
import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.PKMacro;
import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.PKMacroList;
import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.PeripheralMacro;


public class PkMacroAnalyser {

    enum AdvanType{
        ADVAN1,ADVAN2,ADVAN3,ADVAN4,ADVAN10,ADVAN11,ADVAN12;
    };

    List<PKMacroList> pkMacros = new ArrayList<PKMacroList>();

    List<CompartmentMacro> compartments = new ArrayList<CompartmentMacro>();
    List<IVMacro> ivs = new ArrayList<IVMacro>();
    List<EliminationMacro> eliminations = new ArrayList<EliminationMacro>();
    List<OralMacro> orals = new ArrayList<OralMacro>();
    List<PeripheralMacro> peripherals = new ArrayList<PeripheralMacro>();

    String macroAdvanType = new String();

    public PkMacroAnalyser(List<PKMacroList> pkMacroLists) {
        this.pkMacros = pkMacroLists;
        initialiseMacros();
        macroAdvanType = retrieveAdvanType();
    }

    public void initialiseMacros(){
        for(PKMacroList pkMacro : pkMacros){

            for(PKMacro macro : pkMacro.getListOfMacro()){
                if(macro instanceof CompartmentMacro){
                    compartments.add((CompartmentMacro) macro);
                }else if(macro instanceof EliminationMacro){
                    eliminations.add((EliminationMacro) macro);
                }else if(macro instanceof IVMacro){
                    ivs.add((IVMacro) macro);
                }else if(macro instanceof OralMacro){
                    orals.add((OralMacro) macro);
                }else if(macro instanceof PeripheralMacro){
                    peripherals.add((PeripheralMacro) macro);
                }
            }
        }
    }

    /**
     *              CMT     IV      ORAL    PERIF   ELIM
     * ADVAN-1      1       1       0       0       1   k
     * ADVAN-2      1       0       1       0       1   k
     * ADVAN-3      1       1       0       1       1   k
     * ADVAN-4      1       0       1       1       1   k
     * ADVAN-10     1       1       0       0       1   km & vm
     * ADVAN-11     1       1       0       2       1   k
     * ADVAN-12     1       0       1       2       1   k
     * 
     * @return advan type
     */
    public String retrieveAdvanType(){

        if(compartments.isEmpty() || eliminations.isEmpty()){
            throw new IllegalArgumentException("The compartment missing from pk macro specified");
        }
        if(peripherals.size()==0){
            if(isIV()){
                if(isKmAndVm()){
                    return AdvanType.ADVAN10.toString();                           
                }
                return AdvanType.ADVAN1.toString();

            }else if(isOral()){
                return AdvanType.ADVAN2.toString();
            }
        }else if(peripherals.size()>1){
            if(peripherals.size() == 1){
                if(isIV()){
                    return AdvanType.ADVAN3.toString();
                }else if(isOral()){
                    return AdvanType.ADVAN4.toString();
                }
            }else if(peripherals.size() ==2){
                if(isIV()){
                    return AdvanType.ADVAN11.toString();
                }else if(isOral()){
                    return AdvanType.ADVAN12.toString();
                }
            }
        }
        return macroAdvanType;
    }

    private boolean isKmAndVm(){
        boolean isKm = false, isVm = false ;
        if(eliminations.get(0).getListOfValue() != null){
            for(MacroValue vals : eliminations.get(0).getListOfValue()){

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

    private boolean isIV(){
        if(!ivs.isEmpty() && orals.isEmpty()){
            if(ivs.size()==1){
                return true;
            }
        }
        return false;
    }

    private boolean isOral(){
        if(ivs.isEmpty() && !orals.isEmpty()){
            if(orals.size()==1){
                return true;
            }
        }
        return false;
    }

    public String getMacroAdvanType() {
        return macroAdvanType;
    }

    public void setMacroAdvanType(String macroAdvanType) {
        this.macroAdvanType = macroAdvanType;
    }
}
