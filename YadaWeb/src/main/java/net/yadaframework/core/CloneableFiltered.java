package net.yadaframework.core;

import java.lang.reflect.Field;


/**
 * Questa interfaccia indica che l'oggetto deve essere clonato in modo che la nuova istanza condivida i riferimenti agli stessi oggetti dell'originale, ma in collection/mappe differenti.
 * Se il membro di una collection/mappa la implementa a sua volta, esso non viene condiviso tra le copie ma clonato ricorsivamente.
 * Permette inoltre di specificare i campi da non copiare.
 * Vedere come è usato in Prodotto e Sellability.
 */
public interface CloneableFiltered {
	/**
	 * Ritorna la lista dei campi da non copiare. Tornare null per non filtrare niente.
	 * Il campo "id" è escluso per default.
	 * Vedere come è implementato in Prodotto. Marchiarla @Transient
	 * Esempio: java.lang.reflect.Field[] result = new java.lang.reflect.Field[] {
	 *				Prodotto.class.getDeclaredField("codice"),
	 *				Prodotto.class.getDeclaredField("stato")
	 *			};
	 */
	Field[] getExcludedFields();
}