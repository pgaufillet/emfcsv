/*******************************************************************************
 * Copyright (c) 2019 Pierre Gaufillet.
 *  This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Pierre Gaufillet - initial API and implementation
 *******************************************************************************/

package net.emf.csv;

import java.io.FilterWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * This class provides a StreamWriter that sort the lines it contains by String
 * order. It accumulates passively the appended content until it is closed. It
 * then sorts it before sending it to the next chained writer.
 */
public class SortedBufferedOutputStreamWriter extends FilterWriter {

	private StringBuilder sBuilder = new StringBuilder();

	public SortedBufferedOutputStreamWriter(Writer out) {
		super(out);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.FilterOutputStream#flush()
	 */
	@Override
	public void flush() throws IOException {
		// Do nothing. This stream shall not be flushed before all lines have been
		// appended.
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.FilterOutputStream#close()
	 */
	@Override
	public void close() throws IOException {
		// Sort, write and close
		List<String> lines = Arrays.asList(sBuilder.toString().split("\n"));

		Collections.sort(lines);
		for(String s: lines) {
			out.write(s + "\n");
		}
		super.close();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.FilterOutputStream#write(int)
	 */
	@Override
	public void write(String s) throws IOException {
		sBuilder.append(s);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.FilterWriter#write(int)
	 */
	@Override
	public void write(int c) throws IOException {
		sBuilder.append((char) c);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.FilterWriter#write(char[], int, int)
	 */
	@Override
	public void write(char[] cbuf, int off, int len) throws IOException {
		sBuilder.append(cbuf, off, len);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.FilterWriter#write(java.lang.String, int, int)
	 */
	@Override
	public void write(String str, int off, int len) throws IOException {
		sBuilder.append(str, off, len);
	}

}
