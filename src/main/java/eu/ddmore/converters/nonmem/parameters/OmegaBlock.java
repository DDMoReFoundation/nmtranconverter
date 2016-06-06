/*******************************************************************************
 * Copyright (C) 2016 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.parameters;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import eu.ddmore.converters.nonmem.eta.Eta;

/**
 * Omega block which stores information related to omega statement block.
 */
public class OmegaBlock {

    private final Set<Eta> orderedEtas = new TreeSet<Eta>();
    private final List<Eta> omegaBlockEtas = new LinkedList<Eta>();
    private List<CorrelationsWrapper> correlations = new ArrayList<>();
    private Boolean isOmegaBlockFromStdDev = false;
    private Boolean isCorrelation = false;
    private Boolean isIOV = false;
    private String omegaBlockTitle;
    private String omegaBlockSameTitle;

    public void addToOrderedEtas(Eta eta){
        orderedEtas.add(eta);
    }

    public void addToCorrelations(CorrelationsWrapper correlation){
        correlations.add(correlation);
    }

    public void addToOmegaBlockEtas(Eta eta) {
        omegaBlockEtas.add(eta);
    }

    public Set<Eta> getOrderedEtas() {
        return orderedEtas;
    }

    public List<Eta> getOmegaBlockEtas() {
        return omegaBlockEtas;
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

    public List<CorrelationsWrapper> getCorrelations() {
        return correlations;
    }

    public void setCorrelations(List<CorrelationsWrapper> correlations) {
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
