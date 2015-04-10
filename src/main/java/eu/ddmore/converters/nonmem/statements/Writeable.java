/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem.statements;

import java.io.IOException;
import java.io.Writer;

public interface Writeable {

    void write(Writer writer) throws IOException;

}