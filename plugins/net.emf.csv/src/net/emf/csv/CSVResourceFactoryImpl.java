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

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.Resource.Factory;

/**
 * EMF resource factory providing CSV Resources.
 */
public class CSVResourceFactoryImpl implements Factory {

	public CSVResourceFactoryImpl() {
		super();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.emf.ecore.resource.Resource.Factory#createResource(org.eclipse.
	 * emf.common.util.URI)
	 */
	@Override
	public Resource createResource(URI uri) {
		return new CSVResourceImpl(uri);
	}

}
