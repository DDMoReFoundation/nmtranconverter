package eu.ddmore.converters.nonmem.parameters;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import eu.ddmore.converters.nonmem.ConversionContext;
import eu.ddmore.converters.nonmem.eta.Eta;
import eu.ddmore.converters.nonmem.eta.VarLevel;
import eu.ddmore.converters.nonmem.statements.InterOccVariabilityHandler;
import eu.ddmore.converters.nonmem.utils.EtaHandler;

/**
 * Initialises ordered Etas and omega  
 */
public class EtaAndOmegaBlocksInitialiser {

    private final Set<Eta> allOrderedEtas = new TreeSet<Eta>();
    private final List<OmegaBlock> omegaBlocksInNonIOV;
    private final List<OmegaBlock> omegaBlocksInIOV;
    private final ConversionContext context;
    private final InterOccVariabilityHandler iovhandler;

    public EtaAndOmegaBlocksInitialiser(ConversionContext context, List<OmegaBlock> omegaBlocksInIOV, List<OmegaBlock> omegaBlocksInNonIOV){
        this.context = context;
        this.omegaBlocksInIOV = omegaBlocksInIOV;
        this.omegaBlocksInNonIOV = omegaBlocksInNonIOV;
        this.iovhandler = context.getIovHandler();
    }

    /**
     * Populate ordered etas and omega blocks from eta handler.
     * @return all etas ordered
     */
    public Set<Eta> popupateOrderedEtaAndOmegaBlocks(){

        EtaHandler etaHandler = new EtaHandler(context.getScriptDefinition());
        Set<Eta> allEtas = etaHandler.getAllEtas();

        if(!allEtas.isEmpty()){
            int etaCount = 0;
            List<Eta> nonCorrIOVEtas = new ArrayList<Eta>();

            for(Eta eta : allEtas) {
                etaCount = addEtaToOmegaBlock(etaCount, nonCorrIOVEtas, eta);
            }
            etaCount = addIOVEtasToOmegaBlock(etaCount);
            etaCount = addNonCorrIOVEtasToOmegaBlock(etaCount, nonCorrIOVEtas);
        }
        return allOrderedEtas;
    }

    private int addIOVEtasToOmegaBlock(int etaCount) {

        for(OmegaBlock omegaBlock : omegaBlocksInIOV){
            int iovEtaCount = 0;
            for(Eta iovEta : omegaBlock.getEtasToOmegas().keySet()){
                if(iovEta.getVarLevel().equals(VarLevel.IOV) && iovEta.getOrder()==0){
                    iovEta.setOrder(++etaCount);
                    iovEta.setOrderInCorr(++iovEtaCount);
                    iovEta.setVarLevel(VarLevel.IOV);
                    String etaSymbolForIOV = iovhandler.getColumnWithOcc().getColumnId()+"_"+iovEta.getOmegaName();
                    iovEta.setEtaSymbolForIOV(etaSymbolForIOV);
                    allOrderedEtas.add(iovEta);
                    omegaBlock.addToOrderedEtas(iovEta);
                    omegaBlock.setIsIOV(true);
                }
            }
        }
        return etaCount;
    }

    private int addNonCorrIOVEtasToOmegaBlock(int etaCount, List<Eta> nonCorrIOVEtas) {

        for(Eta nonCorrEta : nonCorrIOVEtas){
            nonCorrEta.setOrder(++etaCount);
            allOrderedEtas.add(nonCorrEta);

            OmegaBlock nonCorrOmegaBlock = new OmegaBlock();
            nonCorrOmegaBlock.addToOrderedEtas(nonCorrEta);
            nonCorrOmegaBlock.addToEtaToOmegas(nonCorrEta, nonCorrEta.getOmegaName());
            nonCorrOmegaBlock.setIsIOV(true);
            omegaBlocksInIOV.add(nonCorrOmegaBlock);
        }
        return etaCount;
    }

    private int addEtaToOmegaBlock(int etaCount, List<Eta> nonCorrIOVEtas, Eta eta) {
        boolean isEtaSet = false;

        for(OmegaBlock omegaBlock : omegaBlocksInNonIOV){
            if(omegaBlock.getEtasToOmegas().keySet().contains(eta)){
                for(Eta nonIovEta : omegaBlock.getEtasToOmegas().keySet()){
                    if(eta.getEtaSymbol().equals(nonIovEta.getEtaSymbol()) ){
                        nonIovEta.setOrder(++etaCount);
                        nonIovEta.setOrderInCorr(omegaBlock.getOrderedEtas().size()+1);
                        nonIovEta.setVarLevel(VarLevel.IIV);
                        allOrderedEtas.add(nonIovEta);
                        omegaBlock.addToOrderedEtas(nonIovEta);
                        isEtaSet = true;
                    }
                }
            }
        }

        if(!isEtaSet){
            etaCount = addEtaToNonCorrIOVEtas(etaCount, nonCorrIOVEtas, eta);
        }

        return etaCount;
    }

    private int addEtaToNonCorrIOVEtas(int etaCount, List<Eta> nonCorrIOVEtas, Eta eta) {

        for(OmegaBlock omegaBlock : omegaBlocksInIOV){
            if(!omegaBlock.getEtasToOmegas().keySet().contains(eta)){
                if(iovhandler.getOccasionRandomVariables().contains(eta.getEtaSymbol())){
                    eta.setVarLevel(VarLevel.IOV);
                    eta.setOrderInCorr(1);
                    String etaSymbolForIOV = iovhandler.getColumnWithOcc().getColumnId()+"_"+eta.getOmegaName();
                    eta.setEtaSymbolForIOV(etaSymbolForIOV);

                    nonCorrIOVEtas.add(eta);
                }else {
                    eta.setVarLevel(VarLevel.IIV);
                    eta.setOrder(++etaCount);
                    allOrderedEtas.add(eta);
                }
            }
        }
        return etaCount;
    }
}
