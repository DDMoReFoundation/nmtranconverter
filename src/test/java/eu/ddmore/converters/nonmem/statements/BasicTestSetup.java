package eu.ddmore.converters.nonmem.statements;

import org.mockito.Mock;

import crx.converter.engine.ScriptDefinition;
import crx.converter.engine.parts.EstimationStep;
import eu.ddmore.converters.nonmem.ConversionContext;
/**
 * This class is used as basic common test setup for unit tests around nmtran converter classes
 */
public class BasicTestSetup {

    @Mock ConversionContext context;
    @Mock ScriptDefinition scriptDefinition;

    @Mock EstimationStep estStep;
}
