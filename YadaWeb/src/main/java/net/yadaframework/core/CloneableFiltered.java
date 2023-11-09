package net.yadaframework.core;

import java.lang.reflect.Field;

/**
 * Marker interface to allow an object to be cloned with YadaUtil.copyEntity().
 * It also has two more functions:
 * - During a clone operation, a source object implementing this interface can specify which fields should not be cloned
 * - When the source object is inside a collection, it will not be cloned when inserted into the cloned collection. Use ClonableDeep
 *   if the cloned collection must contain a cloned instance of the source object
 * Please note: any field declared in getExcludedFields() is left uninitialized (e.g. null).
 * In order to shallow copy the field instead, see {@link net.yadaframework.components.YadaCopyShallow}
 */
// TODO this interface is not useful anymore and YadaUtil could be refactored without it so that any object could be cloned  
public interface CloneableFiltered {
	/**
	 * Ritorna la lista dei campi da non copiare. Tornare null per non filtrare niente.
	 * Il campo "id" è escluso per default.
	 * Vedere come è implementato in Prodotto. Marchiarla @Transient
	 * Esempio: java.lang.reflect.Field[] result = new java.lang.reflect.Field[] {
	 *				Prodotto.class.getDeclaredField("codice"),
	 *				Prodotto.class.getDeclaredField("stato")
	 *			};
	 * @deprecated use {@link net.yadaframework.components.YadaCopyNot} on single fields instead
	 */
	@Deprecated
	Field[] getExcludedFields();
}