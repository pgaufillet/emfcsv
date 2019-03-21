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
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EFactory;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.util.InternalEList;

public class CSVLoader {

	private EObjectIdBiMap eObjectIdBiMap;

	private ResourceSet rs;

	public CSVLoader() {
		rs = null;
	}

	public CSVLoader(ResourceSet rs, EObjectIdBiMap map) {
		this.rs = rs;
		eObjectIdBiMap = map;
	}

	/**
	 * 
	 * @param s
	 */
	protected void createObjects(CSVRecord r, Set<EObject> eObjects) {
		Map<String, String> properties = new HashMap<>();
		Iterator<String> i = r.iterator();
		String eType = i.next();
		String id = i.next();
		while(i.hasNext()) {
			String name = i.next();
			String value = i.next();
			properties.put(name, value);
		}

		// Extract EPackage URI from typeURI
		URI typeURI = URI.createURI(eType);
		String nsURI = typeURI.trimFragment().toString();
		EPackage p = EPackage.Registry.INSTANCE.getEPackage(nsURI);
		Resource pRes = p.eResource();
		EClassifier classifier = null;
		if(pRes != null) {
			classifier = (EClassifier) pRes.getEObject(typeURI.fragment().toString());
		}
		EObject eo = null;
		EClass clazz = null;

		if(classifier instanceof EClass) {
			clazz = (EClass) classifier;
		}

		// Create a new object if not already done
		if(eObjectIdBiMap.containsKey(id)) {
			eo = eObjectIdBiMap.get(id);
		} else if(clazz != null) {
			// Use the direct EPackage containing clazz instead of the root EPackage
			EPackage pc = clazz.getEPackage();
			eo = pc.getEFactoryInstance().create(clazz);
			EAttribute eIDAttribute = clazz.getEIDAttribute();
			if(eIDAttribute != null) {
				EcoreUtil.setID(eo, id);
			}
			eObjectIdBiMap.put(id, eo);
		}

		if(eo != null) {
			eObjects.add(eo);
			for(String name: properties.keySet()) {
				EStructuralFeature esf = clazz.getEStructuralFeature(name);
				EClassifier esfType = esf.getEType();
				if(esfType instanceof EDataType) {
					EDataType dataType = (EDataType) esfType;
					EFactory eFactory = dataType.getEPackage().getEFactoryInstance();
					if(esf.isMany()) {
						@SuppressWarnings("unchecked")
						InternalEList<Object> list = (InternalEList<Object>) eo.eGet(esf);
						List<String> values = Arrays.asList(properties.get(name).split(","));
						for(String value: values) {
							list.addUnique(eFactory.createFromString(dataType, value));
						}
					} else {
						Object value = eFactory.createFromString(dataType, properties.get(name));
						eo.eSet(esf, value);
					}
				}
			}
		}
	}

	/**
	 * 
	 * @param s
	 */
	protected void resolveReferences(CSVRecord r, Set<EObject> eObjects) {
		Map<String, String> properties = new HashMap<>();
		Iterator<String> i = r.iterator();
		i.next();
		String id = i.next();
		while(i.hasNext()) {
			String name = i.next();
			String value = i.next();
			properties.put(name, value);
		}

		// All objects have already been created and indexed
		EObject eo = eObjectIdBiMap.get(id);
		EClass clazz = eo.eClass();

		for(String name: properties.keySet()) {
			EStructuralFeature esf = clazz.getEStructuralFeature(name);
			if(esf instanceof EReference) {
				// esf is a EReference
				List<String> dest = Arrays.asList(properties.get(name).split(","));
				EReference refRef = (EReference) eo.eClass().getEStructuralFeature(name);
				if(refRef.isMany()) {
					for(String refDest: dest) {
						EObject eoDest = getEObject(refDest);
						// the feature is a list of references
						@SuppressWarnings("unchecked")
						EList<EObject> refs = (EList<EObject>) eo.eGet(refRef);
						refs.add(eoDest);
						// A contained EObject shall not be returned as root element
						if(refRef.isContainment()) {
							eObjects.remove(eoDest);
						}
					}
				} else {
					// simple reference
					EObject eoDest;
					eoDest = getEObject(dest.get(0));
					eo.eSet(refRef, eoDest);
					// A contained EObject shall not be returned as root element
					if(refRef.isContainment()) {
						eObjects.remove(eoDest);
					}
				}

			}
		}
	}

	private EObject getEObject(String ref) {
		EObject eoDest;
		if(eObjectIdBiMap.containsKey(ref)) {
			eoDest = eObjectIdBiMap.get(ref);
		} else {
			URI uri = URI.createURI(ref);
			if(rs != null) {
				eoDest = rs.getEObject(uri, true);
			} else {
				eoDest = null;
			}
		}
		return eoDest;
	}

	/**
	 * @param inputStream
	 * @return
	 * @throws IOException
	 */
	public Collection<EObject> load(InputStream inputStream) throws IOException {
		Set<EObject> eObjects = new HashSet<>();
		CSVFormat fmt = CSVFormat.EXCEL.withDelimiter(';');
		Iterable<CSVRecord> records = CSVParser.parse(inputStream, StandardCharsets.UTF_8, fmt).getRecords();

		records.forEach(r -> createObjects(r, eObjects));
		records.forEach(r -> resolveReferences(r, eObjects));

		return eObjects;
	}
}
