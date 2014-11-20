package eu.ddmore.converters.nonmem.statements;

import java.io.IOException;
import java.io.Writer;

public interface Writeable {

	void write(Writer writer) throws IOException;

}