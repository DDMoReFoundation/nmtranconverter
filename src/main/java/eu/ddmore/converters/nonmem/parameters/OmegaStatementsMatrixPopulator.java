/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.parameters;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Preconditions;

import eu.ddmore.converters.nonmem.eta.Eta;
import eu.ddmore.libpharmml.dom.commontypes.IntValue;
import eu.ddmore.libpharmml.dom.commontypes.Rhs;
import eu.ddmore.libpharmml.dom.commontypes.SymbolRef;
import eu.ddmore.libpharmml.dom.modeldefn.ParameterRandomVariable;

/**
 * This class creates omega statements matrix using omega statements and correlations in omega block specified.
 */
public class OmegaStatementsMatrixPopulator {

    private static final String EMPTY_VARIABLE = "Empty Variable";
    private final OmegaBlock omegaBlock;
    private final ParametersInitialiser parametersInitialiser;

    OmegaStatementsMatrixPopulator(OmegaBlock omegaBlock, ParametersInitialiser parametersInitialiser){
        this.omegaBlock = omegaBlock;
        this.parametersInitialiser = parametersInitialiser;
    }

    /**
     * Creates omega statements matrix using omega statements in omega block.
     */
    public void createOmegaStatementMatrixForOmegaBlock(){
        Map<Eta, List<OmegaParameter>> omegaBlocks = omegaBlock.getOmegaParameters(); 
        Set<Eta> etaToOmagas = omegaBlock.getEtasToOmegas().keySet(); 

        if(omegaBlock.getCorrelations()!=null && !omegaBlock.getCorrelations().isEmpty()){
            for(Eta eta : etaToOmagas){
                Iterator<CorrelationsWrapper> iterator = omegaBlock.getCorrelations().iterator();

                while(iterator.hasNext()){
                    CorrelationsWrapper correlation = iterator.next();
                    ParameterRandomVariable firstRandomVar = correlation.getFirstParamRandomVariable();
                    ParameterRandomVariable secondRandomVar = correlation.getSecondParamRandomVariable();
                    // row = i column = j
                    int column = getOrderedEtaIndex(firstRandomVar.getSymbId(), etaToOmagas);
                    int row = getOrderedEtaIndex(secondRandomVar.getSymbId(), etaToOmagas);
                    if(column > row){
                        firstRandomVar = correlation.getSecondParamRandomVariable();
                        secondRandomVar = correlation.getFirstParamRandomVariable();
                        int swap = column;
                        column = row;
                        row = swap;
                    }

                    Eta firstEta = getEtaForRandomVar(firstRandomVar.getSymbId(),etaToOmagas);
                    Eta secondEta = getEtaForRandomVar(secondRandomVar.getSymbId(),etaToOmagas);
                    createFirstCorrMatrixRow(eta, firstRandomVar, omegaBlocks);

                    addVarToCorrMatrix(firstEta, column, omegaBlocks);
                    addVarToCorrMatrix(secondEta, row, omegaBlocks);

                    addCoefficientToCorrMatrix(correlation, omegaBlocks, firstEta, secondEta, column);

                    iterator.remove();
                }
            }
        }else if(omegaBlocks!=null && !omegaBlocks.isEmpty()){
            for(Eta eta : omegaBlocks.keySet()){

                OmegaParameter omega = parametersInitialiser.createOmegaFromRandomVarName(eta.getOmegaName());
                List<OmegaParameter> omegas = new ArrayList<OmegaParameter>();
                omegas.add(omega);
                omegaBlocks.put(eta, omegas);
            }
        }
    }

    /**
     * This method will return index from ordered eta to omegas map for random var name provided.
     * 
     * @param randomVariable
     * @param etaToOmagas
     * @return ordered eta index
     */
    private int getOrderedEtaIndex(String randomVariable, Set<Eta> etaToOmagas) {
        Eta eta = getEtaForRandomVar(randomVariable, etaToOmagas);
        if(eta!=null){
            return (eta.getOrderInCorr()>0)?eta.getOrderInCorr()-1:0;
        }
        return 0;
    }

    private Eta getEtaForRandomVar(String randomVariable, Set<Eta> etaToOmagas){
        for(Eta eta : etaToOmagas){
            if(eta.getEtaSymbol().equals(randomVariable)){
                return eta;
            }
        }
        return null;
    }

    /**
     * Creates first matrix row which should have only first omega as element.
     * @param eta
     * @param randomVar
     * @param omegaBlocks
     */
    private void createFirstCorrMatrixRow(Eta eta, ParameterRandomVariable randomVar, 
            Map<Eta, List<OmegaParameter>> omegaBlocks) {

        if(eta.getEtaSymbol().equals(randomVar.getSymbId()) && eta.getOrderInCorr() == 1){
            List<OmegaParameter> matrixRow = new ArrayList<OmegaParameter>();
            String firstVariable = eta.getOmegaName();
            matrixRow.add(parametersInitialiser.createOmegaFromRandomVarName(firstVariable));
            omegaBlocks.put(eta, matrixRow);
        }
    }

    /**
     * This method adds coefficient associated with random var1 and random var2 at [i,j] 
     * which is mirrored with [j,i] ([j,i] can be empty as its not required) from correlation or covariance provided.
     * If nothing is specified then default value will be specified. 
     * 
     * @param corr
     * @param omegaBlocks 
     * @param firstEta
     * @param secondEta
     * @param column
     */
    private void addCoefficientToCorrMatrix(CorrelationsWrapper corr, Map<Eta, List<OmegaParameter>> omegaBlocks,
            Eta firstEta, Eta secondEta, int column) {
        List<OmegaParameter> omegas = omegaBlocks.get(secondEta);
        if(omegas.get(column)==null || omegas.get(column).getSymbId().equals(EMPTY_VARIABLE)){
            OmegaParameter omega = null ;
            if(corr.isCorrelationCoeff()){
                omega = getOmegaForCoefficient(corr.getCorrelationCoefficient().getAssign(), firstEta, secondEta);
            }else if(corr.isCovariance()){
                omega = getOmegaForCoefficient(corr.getCovariance().getAssign(), firstEta, secondEta);
            }
            if(omega != null){
                omegas.set(column,omega);
            }
        }
    }

    private void addVarToCorrMatrix(Eta eta, int column, Map<Eta, List<OmegaParameter>> omegaBlocks) {
        List<OmegaParameter> omegas = omegaBlocks.get(eta);
        if(omegas.get(column)==null){
            initialiseRowElements(column, omegas);
            String secondVariable = eta.getOmegaName();
            omegas.set(column, parametersInitialiser.createOmegaFromRandomVarName(secondVariable));
        }
    }

    /**
     * Initialise lower half of the mirrored matrix by empty omega variables.
     * 
     * @param row
     * @param omegas
     */
    private void initialiseRowElements(int row, List<OmegaParameter> omegas) {
        for(int i=0; i <=row ; i++){
            if(omegas.get(i)==null){
                omegas.set(i, createOmegaWithEmptyScalar(EMPTY_VARIABLE));
            }
        }
    }

    /**
     * We set omega blocks matrix using omegas with empty scalar (i.e. value '0') so that 
     * if there is no coefficient value set for a random value pair in matrix, it will use this default value. 
     *  
     * @param symbol
     * @return omega statement
     */
    private OmegaParameter createOmegaWithEmptyScalar(String symbol) {
        OmegaParameter omega;
        omega = new OmegaParameter(symbol);
        Rhs scalar = createRhs(symbol, 0);
        omega.setInitialEstimate(scalar);
        return omega;
    }

    /**
     * This method will create scalar Rhs object for a symbol from the scalar value provided.
     *  
     * @param symbol
     * @param scalar
     * @return ScalarRhs object
     */
    private Rhs createRhs(String symbol, Integer value) {
        SymbolRef symbRef = new SymbolRef();
        symbRef.setId(symbol);

        Rhs rhs = new Rhs();
        rhs.setScalar(new IntValue(value));
        rhs.setSymbRef(symbRef);
        return rhs;
    }

    /**
     * This method will get existing omega for coefficient and it will create new one 
     * if there is no omega ( or coeffiecient specified is only value) 
     * @param correlation
     * @return omega statement
     */
    private OmegaParameter getOmegaForCoefficient(Rhs coeff, Eta firstEta, Eta secondEta) {
        Preconditions.checkNotNull(coeff, "coefficient value should not be null");
        String firstVar = firstEta.getEtaSymbol();
        String secondVar = secondEta.getEtaSymbol();

        if(coeff.getSymbRef()!=null){
            return parametersInitialiser.createOmegaFromRandomVarName(coeff.getSymbRef().getSymbIdRef()); 
        }else if(coeff.getScalar()!=null ){
            OmegaParameter omega = new OmegaParameter(firstVar+"_"+secondVar);
            omega.setInitialEstimate(coeff);
            return omega;
        }else {
            throw new IllegalArgumentException("The Scalarrhs coefficient related to variables "+firstVar+" and "+secondVar
                +" should have either variable, scalar value or equation specified."); 
        }
    }
}