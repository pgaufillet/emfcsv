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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.URIConverter;
import org.eclipse.emf.ecore.resource.impl.ResourceImpl;

public class CSVResourceImpl extends ResourceImpl {

	protected EObjectIdBiMap eObjectIdBiMap = new EObjectIdBiMap();

	public CSVResourceImpl() {
		super();
	}

	public CSVResourceImpl(URI uri) {
		super(uri);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.emf.ecore.resource.impl.ResourceImpl#doSave(java.io.OutputStream,
	 * java.util.Map)
	 */
	@Override
	protected void doSave(OutputStream outputStream, Map<?, ?> options) throws IOException {
		if(outputStream instanceof URIConverter.Saveable) {
			((URIConverter.Saveable) outputStream).saveResource(this);
		} else {
			OutputStreamWriter osWriter = new OutputStreamWriter(outputStream, "UTF8");
			SortedBufferedOutputStreamWriter writer = new SortedBufferedOutputStreamWriter(osWriter);

			CSVPrinter printer = new CSVPrinter(writer, eObjectIdBiMap);
			printer.print(new HashSet<>(this.getContents()));
			writer.close();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.emf.ecore.resource.impl.ResourceImpl#doLoad(java.io.InputStream,
	 * java.util.Map)
	 */
	@Override
	protected void doLoad(InputStream inputStream, Map<?, ?> options) throws IOException {
		if(options == null) {
			options = Collections.<String, Object>emptyMap();
		}

		if(inputStream instanceof URIConverter.Loadable) {

			((URIConverter.Loadable) inputStream).loadResource(this);

		} else {
			CSVLoader csvLoader = new CSVLoader(getResourceSet(), eObjectIdBiMap);
			Collection<EObject> eobjects = csvLoader.load(inputStream);
			this.getContents().addAll(eobjects);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.emf.ecore.resource.impl.ResourceImpl#getURIFragment(org.eclipse.
	 * emf.ecore.EObject)
	 */
	@Override
	public String getURIFragment(EObject eObject) {
		return eObjectIdBiMap.get(eObject);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.emf.ecore.resource.impl.ResourceImpl#getIDForEObject(org.eclipse.
	 * emf.ecore.EObject)
	 */
	@Override
	protected String getIDForEObject(EObject eObject) {
		return eObjectIdBiMap.get(eObject);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.emf.ecore.resource.impl.ResourceImpl#getEObjectByID(java.lang.
	 * String)
	 */
	@Override
	protected EObject getEObjectByID(String id) {
		return eObjectIdBiMap.get(id);
	}

	/**
	 * @param eo
	 * @return
	 */
	protected boolean containsKey(EObject eo) {
		return eObjectIdBiMap.containsKey(eo);
	}
}
