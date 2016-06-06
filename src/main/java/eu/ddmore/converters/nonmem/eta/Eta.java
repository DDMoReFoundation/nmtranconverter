package eu.ddmore.converters.nonmem.eta;

import java.util.List;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import eu.ddmore.converters.nonmem.parameters.OmegaParameter;

/**
 * This class stores information about Etas in nmtran
 */
public class Eta implements Comparable<Eta> {

    private final String etaSymbol;
    private VariabilityLevel variabilityLevel;
    private Integer order=0;
    private Integer orderInCorrelation=0;
    private String etaSymbolForIOV;
    private String omegaName;
    private boolean isCorrelationRelated = false;
    private List<OmegaParameter> omegaParameters;

    public Eta(String etaSymbol){
        this.etaSymbol = etaSymbol;
        //Currently this is default variability level.
        variabilityLevel = VariabilityLevel.IIV;
    }

    public String getEtaSymbol() {
        return etaSymbol;
    }

    public boolean isIOV() {
        return (variabilityLevel.equals(VariabilityLevel.IOV));
    }

    public String getEtaOrderSymbol(){
        return (isIOV())?getEtaSymbolForIOV():getOrder().toString();
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public String getEtaSymbolForIOV() {
        return etaSymbolForIOV;
    }

    public void setEtaSymbolForIOV(String etaSymbolForIOV) {
        this.etaSymbolForIOV = etaSymbolForIOV;
    }

    public Integer getOrderInCorr() {
        return orderInCorrelation;
    }

    public void setOrderInCorr(Integer orderInCorr) {
        this.orderInCorrelation = orderInCorr;
    }

    public VariabilityLevel getVariabilityLevel() {
        return variabilityLevel;
    }

    public void setVariabilityLevel(VariabilityLevel variabilityLevel) {
        this.variabilityLevel = variabilityLevel;
    }

    public String getOmegaName() {
        return omegaName;
    }

    public void setOmegaName(String omegaName) {
        this.omegaName = omegaName;
    }

    public boolean isCorrelationRelated() {
        return isCorrelationRelated;
    }

    public void setCorrelationRelated(boolean isCorrelationRelated) {
        this.isCorrelationRelated = isCorrelationRelated;
    }

    public List<OmegaParameter> getOmegaParameters() {
        return omegaParameters;
    }

    public void setOmegaParameters(List<OmegaParameter> omegaParameters) {
        this.omegaParameters = omegaParameters;
    }

    @Override
    public int compareTo(Eta secondEta) {
        return Integer.compare(this.getOrder(), secondEta.getOrder());
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 31).
                append(etaSymbol).
                toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Eta))
            return false;
        if (obj == this)
            return true;

        Eta rhs = (Eta) obj;
        return new EqualsBuilder().
                append(etaSymbol, rhs.etaSymbol).
                isEquals();
    }
}
