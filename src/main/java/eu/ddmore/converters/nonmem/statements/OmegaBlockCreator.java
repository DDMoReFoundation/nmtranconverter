package eu.ddmore.converters.nonmem.statements;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.bind.JAXBElement;

import com.google.common.base.Preconditions;

import crx.converter.engine.parts.BaseRandomVariableBlock.CorrelationRef;
import eu.ddmore.converters.nonmem.eta.Eta;
import eu.ddmore.converters.nonmem.utils.Formatter;
import eu.ddmore.converters.nonmem.utils.ParametersHelper;
import eu.ddmore.converters.nonmem.utils.RandomVariableHelper;
import eu.ddmore.converters.nonmem.utils.Formatter.NmConstant;
import eu.ddmore.libpharmml.dom.commontypes.IntValue;
import eu.ddmore.libpharmml.dom.commontypes.ObjectFactory;
import eu.ddmore.libpharmml.dom.commontypes.ScalarRhs;
import eu.ddmore.libpharmml.dom.commontypes.SymbolRef;
import eu.ddmore.libpharmml.dom.modeldefn.ParameterRandomVariable;

/**
 * 
 */
public class OmegaBlockCreator {
    private static final String EMPTY_VARIABLE = "Empty Variable";
    private Set<Eta> etaToOmagas = new TreeSet<Eta>();
    ParametersHelper paramHelper;
    InterOccVariabilityHandler iovHandler;

    public OmegaBlockCreator(ParametersHelper parameter, InterOccVariabilityHandler iovHandler, OmegaBlock omegaBlock){
        paramHelper = parameter;
        this.iovHandler = iovHandler;
        setEtaToOmagas(omegaBlock.getOrderedEtas());
    }

    /**
     * 
     * @param correlations
     * @param omegaBlocks
     * @param etaToOmagas
     * @param etas
     */
    public void createOmegaBlocks(OmegaBlock omegaBlock){

        Map<Eta, List<OmegaStatement>> omegaBlocks = omegaBlock.getOmegaStatements(); 
        Set<Eta> etaToOmagas = omegaBlock.getEtasToOmegas().keySet(); 

        if(omegaBlock.getCorrelations()!=null && !omegaBlock.getCorrelations().isEmpty()){
            for(Eta eta : etaToOmagas){
                Iterator<CorrelationRef> iterator = omegaBlock.getCorrelations().iterator();

                while(iterator.hasNext()){
                    CorrelationRef correlation = iterator.next();
                    ParameterRandomVariable firstRandomVar = correlation.rnd1;
                    ParameterRandomVariable secondRandomVar = correlation.rnd2;
                    // row = i column = j
                    int column = getOrderedEtaIndex(firstRandomVar.getSymbId(), etaToOmagas);
                    int row = getOrderedEtaIndex(secondRandomVar.getSymbId(), etaToOmagas);
                    if(column > row){
                        firstRandomVar = correlation.rnd2;
                        secondRandomVar = correlation.rnd1;
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

                OmegaStatement omega = paramHelper.getOmegaFromRandomVarName(eta.getOmegaName());
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
     * @param etaToOmagas
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
     * @param etaToOmagas
     */
    private void createFirstCorrMatrixRow(Eta eta, ParameterRandomVariable randomVar1, 
            Map<Eta, List<OmegaStatement>> omegaBlocks) {

        if(eta.getEtaSymbol().equals(randomVar1.getSymbId()) && eta.getOrderInCorr() == 1){

            List<OmegaStatement> matrixRow = new ArrayList<OmegaStatement>();
            String firstVariable = eta.getOmegaName();
            matrixRow.add(paramHelper.getOmegaFromRandomVarName(firstVariable));
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
    private void addCoefficientToCorrMatrix(CorrelationRef corr, Map<Eta, List<OmegaStatement>> omegaBlocks,
            Eta firstEta, Eta secondEta, int column) {
        List<OmegaStatement> omegas = omegaBlocks.get(secondEta);
        if(omegas.get(column)==null || omegas.get(column).getSymbId().equals(EMPTY_VARIABLE)){
            OmegaStatement omega = null ;
            if(corr.isCorrelation()){
                omega = getOmegaForCoefficient(corr.correlationCoefficient, firstEta, secondEta);
            }else if(corr.isCovariance()){
                omega = getOmegaForCoefficient(corr.covariance, firstEta, secondEta);
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
            omegas.set(column, paramHelper.getOmegaFromRandomVarName(secondVariable));
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
        ScalarRhs scalar = createScalarRhs(symbol, createScalar(0));  
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
    private ScalarRhs createScalarRhs(String symbol,JAXBElement<?> scalar) {
        ScalarRhs scalarRhs = new ScalarRhs();
        scalarRhs.setScalar(scalar);
        SymbolRef symbRef = new SymbolRef();
        symbRef.setId(symbol);
        scalarRhs.setSymbRef(symbRef);
        return scalarRhs;
    }

    /**
     * Creates scalar for the value provided.
     * This method will be helpful when only value or no value is provided in case of correlation coefficients.
     *  
     * @param value
     * @return
     */
    private static JAXBElement<IntValue> createScalar(Integer value) {
        IntValue intValue = new IntValue();
        intValue.setValue(BigInteger.valueOf(value));
        ObjectFactory factory = new ObjectFactory();
        return factory.createInt(intValue);
    }

    /**
     * This method will get existing omega for coefficient 
     * and it will create new one if there no omega ( or coeffiecient specified is only value) 
     * @param correlation
     * @return
     */
    private OmegaStatement getOmegaForCoefficient(ScalarRhs coeff, Eta firstEta, Eta secondEta) {
        Preconditions.checkNotNull(coeff, "coefficient value should not be null");
        String firstVar = firstEta.getEtaSymbol();
        String secondVar = secondEta.getEtaSymbol();

        if(coeff.getSymbRef()!=null){
            return paramHelper.getOmegaFromRandomVarName(coeff.getSymbRef().getSymbIdRef()); 
        }else if(coeff.getScalar()!=null || coeff.getEquation()!=null){
            OmegaStatement omega = new OmegaStatement(firstVar+"_"+secondVar);
            omega.setInitialEstimate(coeff);
            return omega;
        }else {
            throw new IllegalArgumentException("The Scalarrhs coefficient related to variables "+firstVar+" and "+secondVar
                +" should have either variable, scalar value or equation specified."); 
        }
    }


    /**
     * Initialise omega blocks maps and also update ordered eta to omegas from correlations map.
     * 
     * @param correlations
     * @param omegaBlock
     */
    public void initialiseOmegaBlocks(OmegaBlock omegaBlock){

        omegaBlock.getOmegaStatements().clear();
        omegaBlock.setIsCorrelation(false);
        omegaBlock.setIsOmegaBlockFromStdDev(false);

        if(omegaBlock.getCorrelations()!=null && !omegaBlock.getCorrelations().isEmpty()){
            for(CorrelationRef correlation : omegaBlock.getCorrelations()){
                //Need to set SD attribute for whole block if even a single value is from std dev
                setStdDevAttributeForOmegaBlock(correlation, omegaBlock);
                setCorrAttributeForOmegaBlock(correlation, omegaBlock);
            }
        }

        for(Iterator<Eta> it = etaToOmagas.iterator();it.hasNext();){
            Eta currentEta = it.next();
            if(!omegaBlock.getEtasToOmegas().keySet().contains(currentEta)){
                it.remove();
            }
        }

        for(Eta eta : etaToOmagas){
            ArrayList<OmegaStatement> statements = new ArrayList<OmegaStatement>();
            for(int i=0;i<eta.getOrderInCorr();i++) statements.add(null);
            omegaBlock.addToEtaToOmegaStatement(eta, statements);
        }
    }

    private void setStdDevAttributeForOmegaBlock(CorrelationRef correlation, OmegaBlock omegaBlock) {
        if(!omegaBlock.isOmegaBlockFromStdDev()){
            omegaBlock.setIsOmegaBlockFromStdDev(RandomVariableHelper.isParamFromStdDev(correlation.rnd1) 
                || RandomVariableHelper.isParamFromStdDev(correlation.rnd1));
        }
    }

    private void setCorrAttributeForOmegaBlock(CorrelationRef correlation, OmegaBlock omegaBlock){
        if(!omegaBlock.isCorrelation() && correlation.isCorrelation()){
            omegaBlock.setIsCorrelation((true));
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

    public void createOmegaSameBlockTitle(OmegaBlock omegaBlock) {
        if(omegaBlock.isIOV()){
            Integer uniqueValueCount = iovHandler.getIovColumnUniqueValues().size();
            Integer blockCount = omegaBlock.getOrderedEtas().size();
            String title = String.format(Formatter.endline()+"%s", Formatter.omegaSameBlock(blockCount, uniqueValueCount));
            omegaBlock.setOmegaBlockSameTitle(title);
        }
    }

    public Set<Eta> getEtaToOmagas() {
        return etaToOmagas;
    }


    public void setEtaToOmagas(Set<Eta> etaToOmagas) {
        this.etaToOmagas = new TreeSet<Eta>(etaToOmagas);
    }
}
