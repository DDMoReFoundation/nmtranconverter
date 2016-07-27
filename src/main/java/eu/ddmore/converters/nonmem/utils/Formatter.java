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
package eu.ddmore.converters.nonmem.utils;

import java.util.Map;

import eu.ddmore.converters.nonmem.handlers.PropertiesHandler;

/**
 * Formatter for nmtran conversion from pharmML
 * 
 */
public class Formatter {

    public enum Block{
        ABBR, PK, ERROR, DES, MODEL, PRED, DATA, EST, EPS,
        PROBLEM, SIM, TABLE, INPUT, SUBS, SUB, ETA,
        COV, THETA, OMEGA, SIGMA, BLOCK, SAME, SIZE;
    }

    public enum NmConstant{
        LOG, LOGIT, CORRELATION, FIX, SD, T;
    }

    public enum ColumnConstant{
        ID, TIME;
    }

    public enum ReservedColumnConstant{
        EVID, ID;

        public static boolean contains(String val){
            for (ReservedColumnConstant columnType : ReservedColumnConstant.values()) {
                if (columnType.name().equals(val)) {
                    return true;
                }
            }
            return false;
        }
    }

    public enum TableConstant{
        WRES, RES, PRED, NOPRINT, DV, NOAPPEND;
    }

    public enum Operator{
        IF, THEN, ELSE, ENDIF;
    }

    public enum Symbol{
        BLOCK("$"), 
        COMMENT(";");

        private String symbol = new String();
        Symbol(String symbol){
            this.symbol = symbol;
        }

        @Override
        public String toString(){
            return symbol;
        }
    }

    private static final String DES_VAR_SUFFIX = "_"+Block.DES.toString();
    private static final String NEW_LINE = System.getProperty("line.separator");
    private static final String newLineBlockTitle = "%s%s "+NEW_LINE;
    private static final String inLineBlockTitle = "%s%s ";
    private static final String DUMMY_ETA = "DUMMY = ETA(1)";
    private static boolean inDesBlock = false;

    public static final PropertiesHandler propertyHandler = new PropertiesHandler();

    public static boolean isInDesBlock() {
        return inDesBlock;
    }

    public static void setInDesBlock(boolean inDesBlock) {
        Formatter.inDesBlock = inDesBlock;
    }

    public static String renameVarForDES(String variable) {
        return variable+DES_VAR_SUFFIX;
    }

    public static String getTimeSymbol(){
        return (inDesBlock)?NmConstant.T.toString():ColumnConstant.TIME.toString();
    }

    /**
     * Add <code>Table</code> Block title and then continue appending on the same line
     * 
     * @return block title
     */
    public static String table() {
        return String.format(inLineBlockTitle,Symbol.BLOCK,Block.TABLE);
    }
    /**
     * Add <code>Input</code> Block title and then continue appending on the same line
     * 
     * @return block title
     */
    public static String input() {
        return String.format(inLineBlockTitle,Symbol.BLOCK,Block.INPUT);
    }
    /**
     * Add <code>Subs</code> Block title and then continue appending on the same line
     * 
     * @return block title
     */
    public static String subs() {
        return String.format(inLineBlockTitle,Symbol.BLOCK,Block.SUBS);
    }
    /**
     * Add <code>Omega</code> block title and then continue appending on the same line
     * 
     * @return block title
     */
    public static String omega() {
        return String.format(inLineBlockTitle,Symbol.BLOCK,Block.OMEGA);
    }
    /**
     * Add <code>Omega Blocks</code> block title and then continue appending on the same line
     * 
     * @return block title
     */
    public static String omegaBlock(Integer blocksCount) {
        return String.format("%s%s %s(%s)",Symbol.BLOCK, Block.OMEGA, Block.BLOCK, blocksCount);
    }

    /**
     * Add <code>Omega SAME</code> block title and then continue appending on the same line
     * 
     * @return block title
     */
    public static String omegaSameBlock(Integer blockCount, Integer uniqueValueCount) {
        StringBuilder sameBlockStatement = new StringBuilder();
        String sameBlock = String.format("%s%s %s(%s) %s",Symbol.BLOCK, Block.OMEGA, Block.BLOCK, blockCount, Block.SAME);

        for(int i=1;i<uniqueValueCount;i++){
            sameBlockStatement.append(endline(sameBlock));
        }
        return sameBlockStatement.toString();
    }

    public static String getDummyEtaStatement() {
        return Formatter.endline(DUMMY_ETA);
    }

    /**
     * Add <code>Est</code> block title and then continue appending on the same line
     * 
     * @return block title
     */
    public static String est() {
        return String.format(inLineBlockTitle,Symbol.BLOCK,Block.EST);
    }
    /**
     * Add <code>Problem</code> block title and then continue appending on the same line
     * 
     * @return block title
     */
    public static String problem() {
        return String.format(inLineBlockTitle,Symbol.BLOCK,Block.PROBLEM);
    }
    /**
     * Add <code>Sim</code> block title and then continue appending on the same line
     * 
     * @return block title
     */
    public static String sim() {
        return String.format(inLineBlockTitle,Symbol.BLOCK,Block.SIM);
    }
    /**
     * Add <code>Data</code> block title and then continue appending on the same line
     * 
     * @return block title
     */
    public static String data() {
        return String.format(inLineBlockTitle,Symbol.BLOCK,Block.DATA);
    }

    /**
     * Add <code>PK</code> title and then continue appending on the new line
     * 
     * @return block title
     */
    public static String pk() {
        return String.format(newLineBlockTitle,Symbol.BLOCK,Block.PK);
    }
    /**
     * Add <code>Error</code> title and then continue appending on the new line
     * 
     * @return block title
     */
    public static String error() {
        return String.format(newLineBlockTitle,Symbol.BLOCK,Block.ERROR);
    }
    /**
     * Add <code>DES</code> title and then continue appending on the new line
     * 
     * @return block title
     */
    public static String des() {
        return String.format(newLineBlockTitle,Symbol.BLOCK,Block.DES);
    }
    /**
     * Add <code>Cov</code> title and then continue appending on the new line
     * 
     * @return block title
     */
    public static String cov() {
        return String.format(inLineBlockTitle,Symbol.BLOCK,Block.COV);
    }
    /**
     * Add <code>sub</code> title and then continue appending on the new line
     * 
     * @return block title
     */
    public static String sub() {
        return String.format(newLineBlockTitle,Symbol.BLOCK,Block.SUB);
    }
    /**
     * Add <code>Model</code> title and then continue appending on the new line
     * 
     * @return block title
     */
    public static String model() {
        return String.format(newLineBlockTitle,Symbol.BLOCK,Block.MODEL);
    }
    /**
     * Add <code>Pred</code> title and then continue appending on the new line
     * 
     * @return block title
     */
    public static String pred() {
        return String.format(newLineBlockTitle,Symbol.BLOCK,Block.PRED);
    }

    /**
     * Add <code>Abbr</code> title and then continue appending on the new line
     * 
     * @return block title
     */
    public static String abbr() {
        return String.format(inLineBlockTitle,Symbol.BLOCK,Block.ABBR);
    }

    /**
     * Add <code>Theta</code> title and then continue appending on the new line
     * 
     * @return block title
     */
    public static String theta() {
        return String.format(newLineBlockTitle,Symbol.BLOCK,Block.THETA);
    }

    /**
     * Add <code>Sigma</code> title and then continue appending on the new line
     * 
     * @return block title
     */
    public static String sigma() {
        return String.format(newLineBlockTitle,Symbol.BLOCK,Block.SIGMA);
    }

    /**
     * gets parameter name with prefix as replacement for the parameter name if its reserved word.
     * 
     * @return parameter name with prefix
     */
    public static String getReservedParam(String paramName){
        return propertyHandler.getReservedWordFor(paramName).toUpperCase();
    }

    /**
     * Add indentation before the text provided. 
     * 
     * @return text with indentation
     */
    public static String indent(String text) {
        return "\t" + text;
    }
    /**
     * Add endline after the text provided 
     * 
     * @return text with endline added
     */
    public static String endline(String text) {
        return text + endline();
    }
    /**
     * Return new line specific to system
     * 
     * @return "new line" string
     */    
    public static String endline() {
        return NEW_LINE;
    }

    /**
     * Build variable definitions for fixed and random effects.
     * 
     * @param effect enum with name of effect
     * @param varSymbol variable for the effect
     * @param order order of the variable with other effect variables.
     * @return equation with definition of variable
     */
    public static String buildEffectsDefinitionFor(Block effect, String varSymbol, String order) {
        String effectVariableSymbol = Formatter.getReservedParam(varSymbol);
        String equation = Formatter.endline(effectVariableSymbol+ " = "+buildEffectOrderSymbolFor(effect, order));
        return equation;
    }

    /**
     * Build effect order symbol for effect with order specified.
     * @param effect enum with name of effect
     * @param order order of the variable with other effect variables.
     * @return effect order symbol
     */
    public static String buildEffectOrderSymbolFor(Block effect, String order){
        return String.format(effect+"(%s)",order);
    }

    /**
     * Return the formatted symbol and also gets appropriate time symbol if its TIME symbol.
     * 
     * @param symbol 
     * @return String formatted symbol
     */
    public static String getFormattedSymbol(String symbol) {
        if (symbol.equals(ColumnConstant.TIME.toString()) || symbol.equals(NmConstant.T.toString())){
            symbol = Formatter.getTimeSymbol();
        }
        return symbol.toUpperCase();
    }

    public static String addComment(String comment) {
        return Formatter.indent(Symbol.COMMENT.toString()+comment);
    }

    /**
     * This method gets variable amount from compartment and returns it.
     * 
     * @param variable
     * @return
     */
    public static String getVarAmountFromCompartment(String variable, Map<String,String> derivativeVariableMap) {
        String varAmount = new String(); 
        varAmount = derivativeVariableMap.get(variable);
        if(!varAmount.isEmpty()){
            varAmount = "A("+varAmount+")";
        }
        return varAmount;
    }
}
