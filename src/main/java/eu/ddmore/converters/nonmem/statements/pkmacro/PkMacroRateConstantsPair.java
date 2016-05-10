/*******************************************************************************
 * Copyright (C) 2016 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.statements.pkmacro;

/**
 * This class repesents pk macro rate constants pair which represents rate constants by respective compartments.
 */
public class PkMacroRateConstantsPair implements Comparable<PkMacroRateConstantsPair>  {

    private Integer peripheralCmtNumber=0;
    private Integer compartmentCmtNumber=0;
    private String peripheralRateConstantSymbol = new String();
    private String compartmentRateConstantSymbol = new String();

    public String getPeripheralRateConstantSymbol() {
        return peripheralRateConstantSymbol;
    }

    public String getCompartmentRateConstantSymbol() {
        return compartmentRateConstantSymbol;
    }

    public void setPeripheralRateConstantSymbol(String peripheralRateConstant) {
        this.peripheralRateConstantSymbol = peripheralRateConstant;
    }

    public void setCompartmentRateConstantSymbol(String compartmentRateConstant) {
        this.compartmentRateConstantSymbol = compartmentRateConstant;
    }

    public Integer getCompartmentCmtNumber() {
        return compartmentCmtNumber;
    }

    public void setCompartmentCmtNumber(Integer compartmentCmtNumber) {
        this.compartmentCmtNumber = compartmentCmtNumber;
    }

    public Integer getPeripheralCmtNumber() {
        return peripheralCmtNumber;
    }

    public void setPeripheralCmtNumber(Integer peripheralCmtNumber) {
        this.peripheralCmtNumber = peripheralCmtNumber;
    }

    /**
     * Peripheral compartment number determines order of these rate constants.
     */
    @Override
    public int compareTo(PkMacroRateConstantsPair rateConstantToCompare) {
        return Integer.compare(this.getPeripheralCmtNumber(), rateConstantToCompare.getPeripheralCmtNumber());
    }

}
