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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;

/**
 * This bidirectional map intends to be used as a bidirectional index for
 * EObject/id couples. It is loosely inspired by a part of the Map interface,
 * but provides specific behavior. In particular, get(EObject) always returns an
 * id (if the EObject is not yet recorded, a best effort id is returned).
 */
public class EObjectIdBiMap {

	/**
	 * EObject to id map
	 */
	private Map<EObject, String> eObjectToId = new HashMap<>();

	/**
	 * Id to EObject map
	 */
	private Map<String, EObject> idToEObject = new HashMap<>();

	/**
	 * 
	 * @param key the EObject for which the id is searched.
	 * @return a best effort id. If key is already known, the map may be updated if
	 *         the id has changed. If no natural id is available, a generated UUID
	 *         is returned.
	 */
	public String get(EObject key) {
		String id = EcoreUtil.getID(key);
		if(eObjectToId.containsKey(key)) {
			// Check that the intrinsic ID of eObject has not changed
			String internalId = eObjectToId.get(key);
			if(id != null && !id.equals(internalId)) {
				eObjectToId.put(key, id);
				idToEObject.put(id, key);
			}
		} else {
			// Register and create UUID if needed
			if(id == null) {
				id = EcoreUtil.generateUUID();
			}
			eObjectToId.put(key, id);
			idToEObject.put(id, key);
		}
		return eObjectToId.get(key);
	}

	/**
	 * @param key   the EObject to record
	 * @param value the corresponding id. The caller is responsible for checking the
	 *              consistency of the id with the possible intrinsic id of key.
	 * @return the previous value associated with key, or null if there was no
	 *         mapping for key. (A null return can also indicate that the map
	 *         previously associated null with key, if the implementation supports
	 *         null values.)
	 */
	public String put(EObject key, String value) {
		idToEObject.put(value, key);
		return eObjectToId.put(key, value);
	}

	/**
	 * @param key
	 * @return the value to which the specified key is mapped, or null if this map
	 *         contains no mapping for the key.
	 */
	public EObject get(String key) {
		return idToEObject.get(key);
	}

	/**
	 * @param key
	 * @param value
	 * @return the previous value associated with key, or null if there was no
	 *         mapping for key. (A null return can also indicate that the map
	 *         previously associated null with key, if the implementation supports
	 *         null values.)
	 */
	public EObject put(String key, EObject value) {
		eObjectToId.put(value, key);
		return idToEObject.put(key, value);
	}

	/**
	 * @param eo EObject whose presence in this map is to be tested
	 * @return true if this map contains a mapping for the specified EObject
	 */
	public boolean containsKey(EObject eo) {
		return eObjectToId.containsKey(eo);
	}

	/**
	 * @param s id whose presence in this map is to be tested
	 * @return true if this map contains a mapping for the specified id
	 */
	public boolean containsKey(String s) {
		return idToEObject.containsKey(s);
	}
}
