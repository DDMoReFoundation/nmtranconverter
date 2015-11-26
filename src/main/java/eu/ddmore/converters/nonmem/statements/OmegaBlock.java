package eu.ddmore.converters.nonmem.statements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import crx.converter.engine.parts.BaseRandomVariableBlock.CorrelationRef;

import eu.ddmore.converters.nonmem.eta.Eta;

/**
 * Omega block which stores information related to omega statement block.
 * 
 */
public class OmegaBlock {

    private List<CorrelationRef> correlations = new ArrayList<>();
    private final Set<Eta> orderedEtas = new TreeSet<Eta>();
    private Map<Eta, String> etasToOmegas = new LinkedHashMap<Eta, String>();
    private Map<Eta, List<OmegaStatement>> omegaStatements = new HashMap<Eta, List<OmegaStatement>>();
    private Boolean isOmegaBlockFromStdDev = false;
    private Boolean isCorrelation = false;
    private Boolean isIOV = false;
    private String omegaBlockTitle;
    private String omegaBlockSameTitle;

    public void addToOrderedEtas(Eta eta){
        orderedEtas.add(eta);
    }

    public void addToCorrelations(CorrelationRef correlation){
        correlations.add(correlation);
    }

    public void addToEtaToOmegas(Eta eta, String omega) {
        etasToOmegas.put(eta, omega);
    }

    public void addToEtaToOmegaStatement(Eta eta, List<OmegaStatement> omega) {
        omegaStatements.put(eta, omega);
    }

    public Set<Eta> getOrderedEtas() {
        return orderedEtas;
    }

    public Map<Eta, String> getEtasToOmegas() {
        return etasToOmegas;
    }

    public void setEtasToOmegas(Map<Eta, String> etasToOmegas) {
        this.etasToOmegas = etasToOmegas;
    }

    public String getOmegaBlockTitle() {
        return omegaBlockTitle;
    }

    public void setOmegaBlockTitle(String omegaBlockTitle) {
        this.omegaBlockTitle = omegaBlockTitle;
    }

    public Boolean isCorrelation() {
        return isCorrelation;
    }

    public void setIsCorrelation(Boolean isCorrelation) {
        this.isCorrelation = isCorrelation;
    }

    public Boolean isOmegaBlockFromStdDev() {
        return isOmegaBlockFromStdDev;
    }

    public void setIsOmegaBlockFromStdDev(Boolean isOmegaBlockFromStdDev) {
        this.isOmegaBlockFromStdDev = isOmegaBlockFromStdDev;
    }

    public Map<Eta, List<OmegaStatement>> getOmegaStatements() {
        return omegaStatements;
    }

    public void setOmegaStatements(Map<Eta, List<OmegaStatement>> omegaStatements) {
        this.omegaStatements = omegaStatements;
    }

    public List<CorrelationRef> getCorrelations() {
        return correlations;
    }

    public void setCorrelations(List<CorrelationRef> correlations) {
        this.correlations = correlations;
    }

    public String getOmegaBlockSameTitle() {
        return omegaBlockSameTitle;
    }

    public void setOmegaBlockSameTitle(String omegaBlockSameTitle) {
        this.omegaBlockSameTitle = omegaBlockSameTitle;
    }

    public Boolean isIOV() {
        return isIOV;
    }

    public void setIsIOV(Boolean isIOV) {
        this.isIOV = isIOV;
    }
}
