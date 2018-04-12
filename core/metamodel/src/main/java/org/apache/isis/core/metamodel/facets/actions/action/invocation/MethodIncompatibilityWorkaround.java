/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.isis.core.metamodel.facets.actions.action.invocation;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import org.apache.isis.applib.internal.base._NullSafe;
import org.apache.isis.applib.internal.collections._Collections;

/**
 * Package private utility for method invocation pre-processing. 
 */
class MethodIncompatibilityWorkaround {

	static Object invoke(Method method, Object targetPojo, Object[] executionParameters) 
			throws IllegalAccessException, InvocationTargetException {

		if (_NullSafe.isEmpty(executionParameters)) {
			return method.invoke(targetPojo, executionParameters);
		}
		
		final Class<?>[] parameterTypes = method.getParameterTypes();
		final Object[] adaptedExecutionParameters = new Object[executionParameters.length]; 
		
		int i=0;
		
		for(Object param : executionParameters) {
			adaptedExecutionParameters[i] = adapt(param, parameterTypes[i]);
			++i;
		}
		
		return method.invoke(targetPojo, adaptedExecutionParameters);
	}

	// -- OBJECT ADAPTER
	
	/**
	 * Replaces obj (if required) to be conform with the parameterType
	 * @param obj
	 * @param parameterType
	 * @return
	 */
	private static Object adapt(Object obj, Class<?> parameterType) {

		if(obj==null) {
			return null;
		}
		
		// allow no side effects on Collection arguments
		if(Collection.class.equals(parameterType)) {
			return _Collections.asUnmodifiableCollection((List<?>)obj);
		}
		
		// allow no side effects on List arguments
		if(List.class.equals(parameterType)) {
			return _Collections.asUnmodifiableList((List<?>)obj);
		}

		// adapt as Set (unmodifiable)
		if(Set.class.equals(parameterType)) {
			return _Collections.asUnmodifiableSet((List<?>)obj);
		}
		
		// adapt as SortedSet (unmodifiable)
		if(SortedSet.class.equals(parameterType)) {
			return _Collections.asUnmodifiableSortedSet((List<?>)obj);
		}
		
		return obj;
	}
	

}