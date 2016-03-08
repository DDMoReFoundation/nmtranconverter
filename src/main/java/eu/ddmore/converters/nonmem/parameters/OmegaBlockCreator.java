package eu.ddmore.converters.nonmem.parameters;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.google.common.base.Preconditions;

import eu.ddmore.converters.nonmem.eta.Eta;
import eu.ddmore.converters.nonmem.statements.InterOccVariabilityHandler;
import eu.ddmore.converters.nonmem.statements.OmegaStatement;
import eu.ddmore.converters.nonmem.utils.Formatter;
import eu.ddmore.converters.nonmem.utils.Formatter.NmConstant;
import eu.ddmore.converters.nonmem.utils.RandomVariableHelper;
import eu.ddmore.libpharmml.dom.commontypes.IntValue;
import eu.ddmore.libpharmml.dom.commontypes.Rhs;
import eu.ddmore.libpharmml.dom.commontypes.SymbolRef;
import eu.ddmore.libpharmml.dom.modeldefn.ParameterRandomVariable;

/**
 * this initialises creates omega block and omega title as well as same block title for IOV.
 */
public class OmegaBlockCreator {
    private static final String EMPTY_VARIABLE = "Empty Variable";
    private Set<Eta> orderedEtas = new TreeSet<Eta>();
    ParametersBuilder paramHelper;
    InterOccVariabilityHandler iovHandler;
    ParametersInitialiser parameters;

    public OmegaBlockCreator(ParametersInitialiser parametersInitialiser, InterOccVariabilityHandler iovHandler, OmegaBlock omegaBlock){
        this.parameters = parametersInitialiser;
        this.iovHandler = iovHandler;
        this.orderedEtas = omegaBlock.getOrderedEtas();
    }

    /**
     * Initialise omega blocks maps and also update ordered eta to omegas from correlations map.
     * 
     * @param omegaBlock
     */
    public void initialiseOmegaBlocks(OmegaBlock omegaBlock){

        omegaBlock.getOmegaStatements().clear();
        omegaBlock.setIsCorrelation(false);
        omegaBlock.setIsOmegaBlockFromStdDev(false);

        if(omegaBlock.getCorrelations()!=null && !omegaBlock.getCorrelations().isEmpty()){
            for(CorrelationsWrapper correlation : omegaBlock.getCorrelations()){
                //Need to set SD attribute for whole block if even a single value is from std dev
                setStdDevAttributeForOmegaBlock(correlation, omegaBlock);
                setCorrAttributeForOmegaBlock(correlation, omegaBlock);
            }
        }

        for(Iterator<Eta> it = orderedEtas.iterator();it.hasNext();){
            Eta currentEta = it.next();
            if(!omegaBlock.getEtasToOmegas().keySet().contains(currentEta)){
                it.remove();
            }
        }

        for(Eta eta : orderedEtas){
            ArrayList<OmegaStatement> statements = new ArrayList<OmegaStatement>();
            for(int i=0;i<eta.getOrderInCorr();i++) statements.add(null);
            omegaBlock.addToEtaToOmegaStatement(eta, statements);
        }
    }

    private void setStdDevAttributeForOmegaBlock(CorrelationsWrapper correlation, OmegaBlock omegaBlock) {
        if(!omegaBlock.isOmegaBlockFromStdDev()){
            omegaBlock.setIsOmegaBlockFromStdDev(RandomVariableHelper.isParamFromStdDev(correlation.getFirstParamRandomVariable()) 
                || RandomVariableHelper.isParamFromStdDev(correlation.getFirstParamRandomVariable()));
        }
    }

    private void setCorrAttributeForOmegaBlock(CorrelationsWrapper correlation, OmegaBlock omegaBlock){
        if(!omegaBlock.isCorrelation() && correlation.isCorrelationCoeff()){
            omegaBlock.setIsCorrelation((true));
        }
    }

    /**
     * Creates omega statements for omega block provided.
     * 
     * @param omegaBlock
     */
    public void createOmegaBlocks(OmegaBlock omegaBlock){

        Map<Eta, List<OmegaStatement>> omegaBlocks = omegaBlock.getOmegaStatements(); 
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

                OmegaStatement omega = parameters.createOmegaFromRandomVarName(eta.getOmegaName());
                List<OmegaStatement> omegas = new ArrayList<OmegaStatement>();
                omegas.add(omega);
                omegaBlocks.put(eta, omegas);
            }
        }
    }

    /**
     * This method will return index from ordered eta map for random var name provided.
     * 
     * @param randomVariable
     * @param orderedEtas
     * @return
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
     * Creates first matrix row which will have only first omega as element.
     * 
     * @param eta
     * @param randomVar1
     * @param omegaBlocks
     */
    private void createFirstCorrMatrixRow(Eta eta, ParameterRandomVariable randomVar1, 
            Map<Eta, List<OmegaStatement>> omegaBlocks) {

        if(eta.getEtaSymbol().equals(randomVar1.getSymbId()) && eta.getOrderInCorr() == 1){

            List<OmegaStatement> matrixRow = new ArrayList<OmegaStatement>();
            String firstVariable = eta.getOmegaName();
            matrixRow.add(parameters.createOmegaFromRandomVarName(firstVariable));
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
    private void addCoefficientToCorrMatrix(CorrelationsWrapper corr, Map<Eta, List<OmegaStatement>> omegaBlocks,
            Eta firstEta, Eta secondEta, int column) {
        List<OmegaStatement> omegas = omegaBlocks.get(secondEta);
        if(omegas.get(column)==null || omegas.get(column).getSymbId().equals(EMPTY_VARIABLE)){
            OmegaStatement omega = null ;
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

    private void addVarToCorrMatrix(Eta eta, int column, Map<Eta, List<OmegaStatement>> omegaBlocks) {
        List<OmegaStatement> omegas = omegaBlocks.get(eta);
        if(omegas.get(column)==null){
            initialiseRowElements(column, omegas);
            String secondVariable = eta.getOmegaName();
            omegas.set(column, parameters.createOmegaFromRandomVarName(secondVariable));
        }
    }

    /**
     * Initialise lower half of the mirrored matrix by empty omega variables.
     * 
     * @param row
     * @param omegas
     */
    private void initialiseRowElements(int row, List<OmegaStatement> omegas) {
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
     * @return
     */
    private OmegaStatement createOmegaWithEmptyScalar(String symbol) {
        OmegaStatement omega;
        omega = new OmegaStatement(symbol);
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
    private Rhs createRhs(String symbol,Integer value) {
        SymbolRef symbRef = new SymbolRef();
        symbRef.setId(symbol);

        Rhs rhs = new Rhs();
        rhs.setScalar(new IntValue(value));
        rhs.setSymbRef(symbRef);
        return rhs;
    }

    /**
     * This method will get existing omega for coefficient 
     * and it will create new one if there no omega ( or coeffiecient specified is only value) 
     * @param correlation
     * @return
     */
    private OmegaStatement getOmegaForCoefficient(Rhs coeff, Eta firstEta, Eta secondEta) {
        Preconditions.checkNotNull(coeff, "coefficient value should not be null");
        String firstVar = firstEta.getEtaSymbol();
        String secondVar = secondEta.getEtaSymbol();

        if(coeff.getSymbRef()!=null){
            return parameters.createOmegaFromRandomVarName(coeff.getSymbRef().getSymbIdRef()); 
        }else if(coeff.getScalar()!=null ){
            OmegaStatement omega = new OmegaStatement(firstVar+"_"+secondVar);
            omega.setInitialEstimate(coeff);
            return omega;
        }else {
            throw new IllegalArgumentException("The Scalarrhs coefficient related to variables "+firstVar+" and "+secondVar
                +" should have either variable, scalar value or equation specified."); 
        }
    }

    /**
     * Creates title for omega blocks.
     * Currently it gets block count for BLOCK(n) and identify in correlations are used.
     * This will be updated for matrix types in future.
     * 
     * @param omegaBlock
     * @return
     */
    public void createOmegaBlockTitle(OmegaBlock omegaBlock) {
        StringBuilder description = new StringBuilder();
        //This will change in case of 0.4 as it will need to deal with matrix types as well.
        description.append((omegaBlock.isCorrelation())?NmConstant.CORRELATION:" ");
        description.append((omegaBlock.isOmegaBlockFromStdDev())?" "+NmConstant.SD:"");
        String title = String.format(Formatter.endline()+"%s %s", Formatter.omegaBlock(omegaBlock.getOrderedEtas().size()),description);
        omegaBlock.setOmegaBlockTitle(title);
    }

    /**
     * creates title for omega "SAME" block
     * @param omegaBlock
     */
    public void createOmegaSameBlockTitle(OmegaBlock omegaBlock) {
        if(omegaBlock.isIOV()){
            Integer uniqueValueCount = iovHandler.getIovColumnUniqueValues().size();
            Integer blockCount = omegaBlock.getOrderedEtas().size();
            String title = String.format(Formatter.endline()+"%s", Formatter.omegaSameBlock(blockCount, uniqueValueCount));
            omegaBlock.setOmegaBlockSameTitle(title);
        }
    }
}
