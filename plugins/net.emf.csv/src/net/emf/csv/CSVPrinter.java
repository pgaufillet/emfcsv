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
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EFactory;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.util.EcoreUtil;

/**
 *
 */
public class CSVPrinter {

	private EObjectIdBiMap eObjectIdBiMap;

	private org.apache.commons.csv.CSVPrinter printer = null;

	private String listDelimiter = ",";

	/**
	 * 
	 */
	public CSVPrinter(Writer w, EObjectIdBiMap map) throws IOException {
		printer = CSVFormat.EXCEL.withDelimiter(';').print(w);
		eObjectIdBiMap = map;
	}

	// eos shall be contained in the same model/resource/resourceset
	public void print(Set<? extends EObject> eos) throws IOException {
		for(EObject eo: eos) {
			print(eo);
		}
	}

	private void print(EObject eo) throws IOException {
		// Build a CSV properties line representing the object
		List<String> fields = new ArrayList<>();
		String id = EcoreUtil.getID(eo);
		if(id == null) {
			id = eObjectIdBiMap.get(eo);
		} else {
			eObjectIdBiMap.put(id, eo);
		}
		fields.add(EcoreUtil.getURI(eo.eClass()).toString());
		fields.add(id);
		List<EStructuralFeature> esList = new ArrayList<EStructuralFeature>(eo.eClass().getEAllStructuralFeatures());
		// Sort structural features to limit CSV file variability
		// It makes file comparison easier
		Collections.sort(esList, new Comparator<EStructuralFeature>() {

			@Override
			public int compare(EStructuralFeature o1, EStructuralFeature o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});

		for(EStructuralFeature es: esList) {
			if(!es.isTransient() && eo.eIsSet(es)) {
				if(es instanceof EAttribute) {
					EAttribute ea = (EAttribute) es;
					if(!ea.isID()) {
						fields.addAll(printEAttribute(eo, ea));
					}
				} else {
					EReference er = (EReference) es;
					String eReferenceStr = printEReferenceValue(eo, er);

					if(!eReferenceStr.equals("")) {
						fields.add(er.getName());
						fields.add(eReferenceStr);
					}
				}
			}
		}
		printer.printRecord(fields);

		// Last, explore children.
		for(EObject c: eo.eContents()) {
			// This hack solves an issue with eGenericSupertypes, (probably) wrongly
			// considered as a container feature by eContents() but filtered by the eIsSet()
			// test above.
			if(eObjectIdBiMap.containsKey(c)) {
				print(c);
			}
		}
	}

	/**
	 * @param eo
	 * @param ea
	 * @return
	 */
	protected List<String> printEAttribute(EObject eo, EAttribute ea) {
		List<String> retVal = new ArrayList<>();
		Object val = eo.eGet(ea);
		EFactory eFactory = ea.getEType().getEPackage().getEFactoryInstance();
		if(val != null) {
			EDataType eaType = (EDataType) (ea.getEType());
			retVal.add(ea.getName());
			if(val instanceof EList<?>) {
				EList<?> valList = (EList<?>) val;
				for(Object o: valList) {
					retVal.add(eFactory.convertToString(eaType, o));
				}
			} else {
				retVal.add(eFactory.convertToString(eaType, val));
			}
		}
		return retVal;
	}

	/**
	 * @param eo
	 * @param eObjectToId
	 * @param processedObjects
	 * @param sb
	 * @param er
	 * @param dstEReferenceStr
	 * @return
	 * @throws IOException
	 */
	protected String printEReferenceValue(EObject eo, EReference er) throws IOException {
		String retVal = "";
		Object val = eo.eGet(er);
		if(val instanceof EObject) {
			EObject eoVal = (EObject) val;
			retVal = getReference(eo, eoVal);
		} else if(val instanceof EList<?>) {
			EList<?> valList = (EList<?>) val;
			List<String> refList = new ArrayList<>();
			for(Object o: valList) {
				if(o instanceof EObject) {
					EObject eoVal = (EObject) o;
					String id = getReference(eo, eoVal);
					refList.add(id);
				}
			}
			if(refList.size() > 0) {
				Collections.sort(refList);
				retVal = String.join(listDelimiter, refList);
			}
		}
		return retVal;
	}

	private String getReference(EObject eo, EObject eoVal) {
		String id;

		if(eoVal.eResource().equals(eo.eResource())) {
			id = EcoreUtil.getID(eoVal);
			if(id == null) {
				id = eObjectIdBiMap.get(eoVal);
			} else {
				eObjectIdBiMap.put(id, eoVal);
			}
		} else {
			id = EcoreUtil.getURI(eoVal).toString();
		}
		return id;
	}

}
