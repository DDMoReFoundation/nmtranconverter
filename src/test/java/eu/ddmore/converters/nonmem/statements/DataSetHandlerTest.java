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
package eu.ddmore.converters.nonmem.statements;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;

import eu.ddmore.libpharmml.dom.trialdesign.ExternalDataSet;

@PrepareForTest(DataSetHandler.class)
public class DataSetHandlerTest extends BasicTestSetup  {

    @Mock BufferedReader reader;
    @Mock InputStreamReader isReader;
    @Mock FileInputStream inputStream;

    private static final Character IGNORE_CHAR = '@';
    private static final Character CUSTOM_IGNORE_CHAR = '&';
    private DataSetHandler dataSetHandler;
    private List<ExternalDataSet> extDataSets = new ArrayList<ExternalDataSet>();
    private String dataLocation;
    private File dataFile;
    private String dataFileName;

    @Before
    public void setUp() throws Exception {
        dataLocation = "temp_location";
        dataFileName = "datafile.csv";
        dataFile = mock(File.class);

        when(dataFile.exists()).thenReturn(true);
        whenNew(File.class).withParameterTypes(String.class).withArguments(dataLocation,dataFileName).thenReturn(dataFile);

        whenNew(FileInputStream.class).withArguments(dataFile).thenReturn(inputStream);
        whenNew(InputStreamReader.class).withArguments(inputStream).thenReturn(isReader);
        whenNew(BufferedReader.class).withArguments(isReader).thenReturn(reader);

        ExternalDataSet extDataSet = mock(ExternalDataSet.class, RETURNS_DEEP_STUBS);
        when(extDataSet.getDataSet().getExternalFile().getPath()).thenReturn(dataFileName);

        extDataSets.add(extDataSet);

    }

    @Test
    public void shouldGetDataFileName() {

        dataSetHandler = new DataSetHandler(extDataSets, dataLocation);

        assertEquals("Should return expected data file name", dataFileName, dataSetHandler.getDataFileName());
        assertEquals("Should return default ignore characher", IGNORE_CHAR, dataSetHandler.getIgnoreChar());
    }

    @Test
    public void shouldGetIgnoreCharacter() throws Exception {
        when(reader.readLine()).thenReturn(CUSTOM_IGNORE_CHAR.toString());

        dataSetHandler = new DataSetHandler(extDataSets, dataLocation);

        Character outputIgnoreChar = dataSetHandler.getIgnoreChar();

        assertEquals("Should return expected data file name", CUSTOM_IGNORE_CHAR, outputIgnoreChar);

    }

    @Test(expected=IllegalStateException.class)
    public void shouldThrowExceptionWhenDataFileDoesNotExist() {
        when(dataFile.exists()).thenReturn(false);

        dataSetHandler = new DataSetHandler(extDataSets, dataLocation);

    }

}
