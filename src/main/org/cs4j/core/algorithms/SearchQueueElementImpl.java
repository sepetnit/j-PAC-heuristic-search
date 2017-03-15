package org.cs4j.core.algorithms;

import org.cs4j.core.collections.SearchQueueElement;

public abstract class SearchQueueElementImpl implements SearchQueueElement {
	
	private int[] indexMap;
	
	protected SearchQueueElementImpl(int keySize) {
		indexMap = new int[keySize];
		for (int i=0; i<keySize; i++) 
			indexMap[i] = -1;
	}

	@Override
	public void setIndex(int key, int index) {
		indexMap[key] = index;
	}

	@Override
	public int getIndex(int key) {
		return indexMap[key];
	}
}
