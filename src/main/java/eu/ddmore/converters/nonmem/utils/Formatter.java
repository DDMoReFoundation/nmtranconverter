/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.utils;

import java.util.Map;
import java.util.TreeMap;

/**
 * Formatter for nmtran conversion from pharmML
 * 
 */
public class Formatter {

    public enum Block{
        PK, ERROR, DES, MODEL, PRED, DATA, EST, 
        PROBLEM, SIM, TABLE, INPUT, SUBS, SUB, 
        COV, THETA, OMEGA, SIGMA, BLOCK;
    }

    public enum NmConstant{
        LOG, LOGIT, CORRELATION, FIX, SD, T;
    }

    public enum ColumnConstant{
        ID, TIME;
    }

    public enum TableConstant{
        WRES, RES, PRED, NOPRINT, DV, NOAPPEND;
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

    private static boolean inDesBlock = false;

    public static boolean isInDesBlock() {
        return inDesBlock;
    }

    public static void setInDesBlock(boolean inDesBlock) {
        Formatter.inDesBlock = inDesBlock;
    }

    public static String getTimeSymbol(){
        return (inDesBlock)?NmConstant.T.toString():ColumnConstant.TIME.toString();
    }

    private static final String PREFIX = "";//"NM_";
    private static final String NEW_LINE = System.getProperty("line.separator");

    private static final String newLineBlockTitle = "%s%s "+NEW_LINE;
    private static final String inLineBlockTitle = "%s%s ";

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
        return String.format(newLineBlockTitle,Symbol.BLOCK,Block.COV);
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
     * Add specified prefix to parameter name provided.
     * 
     * @return parameter name with prefix
     */
    public static String addPrefix(String paramName){
        return (!paramName.contains(PREFIX))?PREFIX + paramName.toUpperCase():paramName.toUpperCase();
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
     * Return the formatted symbol and also gets appropriate time symbol if its TIME symbol.
     * 
     * @param symbol 
     * @return String formatted symbol
     */
    public static String getFormattedSymbol(String symbol) {
        if (symbol.equals(ColumnConstant.TIME.toString()) || symbol.equals(NmConstant.T.toString())){
            symbol = Formatter.getTimeSymbol();
        } else{
            symbol = Formatter.addPrefix(symbol);
        }
        return symbol;
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
