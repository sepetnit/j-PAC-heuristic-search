package org.cs4j.core.collections;

import java.util.ArrayList;
import java.util.Comparator;

/**
 * An implementation of a binary heap where elements are aware of their 
 * location (index) in the heap.
 *
 * @author Matthew Hatem & Vitali Sepetnitsky
 */
public class BinHeap<E extends SearchQueueElement> implements SearchQueue<E> {

	private final ArrayList<E> heap;
	private final Comparator<E> cmp;
	private final int key;
    
    /**
     * Constructor of the heap
     *
     * @param cmp Comparator that allows comparison of the heap elements
     * @param key The index at the single heap element, that allows accessing a single element
     */
	public BinHeap(Comparator<E> cmp, int key) {
		this.heap = new ArrayList<>();
		this.cmp = cmp;
		this.key = key;
	}

	@Override
	public int getKey() {
		return this.key;
	}

	@Override
	public boolean isEmpty() {
		return this.heap.isEmpty();
	}

	@Override
	public int size() {
		return this.heap.size();
	}

	@Override
	public E poll() {
		if (this.heap.isEmpty()) {
			return null;
		}
		// Get the top (smallest) element
		E elementToReturn = this.heap.get(0);
		setIndex(elementToReturn, -1);
		if (this.heap.size() > 1) {
		    // Remove the element with the biggest index
			E biggestElement = this.heap.remove(this.heap.size() - 1);
			// Make this element to be the root (instead of elementToReturn)
			this.heap.set(0, biggestElement);
			this.setIndex(biggestElement, 0);
			// Move the root down until the heap property stops to be broken
			this.pushDown(0);
		}
		else {
		    // Just remove the single element which should be returned
			this.heap.remove(0);
		}
		return elementToReturn;
	}
    
    /**
     * The function peeks the element located at the root of the heap
     *
     * @return The element at root (or null if the heap is empty)
     */
	@Override
	public E peek() {
		if (this.heap.isEmpty()) {
            return null;
        }
		return this.heap.get(0);
	}
    
    /**
     * The function inserts the given element to the heap and assures the heap property is still
     * true after the insertion
     *
     * @param e The element to insert
     */
    @Override
	public void add(E e) {
	    // Add the element to the end of the heap
		this.heap.add(e);
		// Update the index of the element
		this.setIndex(e, this.heap.size() - 1);
		// Move the added element up, until the heap property stops to be broken
		this.pullUp(this.heap.size() - 1);
	}

	@Override
	public void clear() {
		this.heap.clear();
	}
    
    /**
     * The function updates the given element (according to its index)
     *
     * @param e The element to update
     */
	@Override
	public void update(E e) {
	    // Get the index of the element that should be updated
		int i = e.getIndex(this.key);
		// Check the index is valid
        // TODO: Added here -1, looks it was buggy
		if (i < 0 || i > this.heap.size() - 1) {
            throw new IllegalArgumentException();
        }
		// First, move the element up, until its place is valid
		i = this.pullUp(i);
		// Now, move down the element according to its updated index (since its new
        // location may break the heap property)
		this.pushDown(i);
	}
    
    /**
     * The function removes the given element from the heap
     *
     * @param e The element to remove
     */
	@Override
	public E remove(E e) {
	    // Get the index to remove
		int elementIndex = e.getIndex(this.key);
		// Remove according to the extracted index
		return removeElementAt(elementIndex);
	}
    
    /**
     * The function removes the element located at index i
     *
     * @param i The index of the element to remove
     *
     * @return The removed element
     */
	private E removeElementAt(int i) {
	    // Get the element at index i
		E toReturn = this.heap.get(i);
		// Update its index to be 'illegal'
		this.setIndex(toReturn, -1);
		// If the element wasn't the last one we need to update the heap
		if (this.heap.size() - 1 != i) {
		    // Take the new last element of the heap and move it to be at i
			this.heap.set(i, this.heap.get(this.heap.size() - 1));
			// Update the index of the new element located now at i
			this.setIndex(this.heap.get(i), i);
		}
		// Now, remove the last element of the heap (its copy is stored at index i)
		this.heap.remove(this.heap.size() - 1);
		// In case element at i wan't the last element, we need ensure that the heap property
        // wasn't broken
		if (i < this.heap.size()) {
		    // First, move the element at i up through the heap
			this.pullUp(i);
			// Now, ensure that the element located now at i shouldn't be moved down
			this.pushDown(i);
		}
		// Return the removed element
		return toReturn;
	}

	/**
	 * A recursive function that moves the element located at index i, up of the heap, until
     * the heap property stops to be broken
     *
     * @return The value of the last element pushed up (the function is recursive)
	 */
	private int pullUp(int i) {
	    // In case element at i is located at the root of the tree - stop and return it
		if (i == 0) {
            return i;
        }
        // Get the parent element of the given one
		int p = this.parent(i);
		// If element at i has smaller value, swap between it and its parent
		if (this.compare(i, p) < 0) {
		    // Swap between the element at i and element at p (move i up)
			this.swap(i, p);
			// Continue moving up the element at index p (now it is smaller that at start)
			return this.pullUp(p);
		}
		return i;
	}
    
    /**
     * A recursive function that moves the element located at index i, down of the heap, until
     * the heap property stops being broken
     *
     * @param i The index of the element to push down
     */
	private void pushDown(int i) {
	    // Extract the index of the left successor of the given element
		int l = this.left(i);
		// Extract the index of the right successor of the given element
		int r = this.right(i);
		// Find the smallest element among i and its left and right successors
		int sml = i;
		// If left successor is smaller than its ancestor
		if (l < heap.size() && this.compare(l, i) < 0) {
            sml = l;
        }
        // If left successor is smaller than the current smallest element (i or its left successor)
        if (r < heap.size() && this.compare(r, sml) < 0) {
            sml = r;
        }
        // In case and update of the heap is required
		if (sml != i) {
		    // Swap i and the smallest element
			this.swap(i, sml);
			// Continue to push down the smallest element (until no change is required)
			this.pushDown(sml);
		}
	}
    
    /**
     * The function compares between two elements in the heap
     *
     * @param i The index of the first element
     * @param j The index of the second element
     *
     * @return Negative number if element at i is smaller than element at j and positive
     *         number otherwise
     */
	private int compare(int i, int j) {
		E a = this.heap.get(i);
		E b = this.heap.get(j);
		return this.cmp.compare(a, b);
	}
    
    /**
     * The functions updates the index stored at the given element to be equal to i
     * @param e The element to update
     * @param i The index to set
     */
	private void setIndex(E e, int i) {
		e.setIndex(this.key, i);
	}
    
    /**
     * The function swaps between element located at index i and the element located at index j
     *
     * @param i The index of the first element
     * @param j The index of the second element
     */
	private void swap(int i, int j) {
		E iE = this.heap.get(i);
		E jE = this.heap.get(j);

		// Move jE element to be at index i
		this.heap.set(i, jE);
		// Update index of jE element
		this.setIndex(jE, i);
		// Move iE element to be at index j
		this.heap.set(j, iE);
		// Update index of jE element
		this.setIndex(iE, j);
	}
    
    /**
     *
     * @param i The index of the element whose parent is required
     *
     * @return The index of the parent element of the given one
     */
	private int parent(int i) {
		return (i - 1) / 2;
	}
    
    /**
     *
     * @param i The index of the element whose left child is required
     *
     * @return The index of the left successor element of the given one
     */
	private int left (int i) {
		return 2 * i + 1;
	}
    
    /**
     *
     * @param i The index of the element whose right child is required
     *
     * @return The index of the right successor element of the given one
     */
    private int right (int i) {
		return 2 * i + 2;
	}
    
    /**
     *
     * @param i The function returns element at index i
     *
     * @return Element at the given index
     */
	public E getElementAt(int i) {
        // TODO: Added here -1, looks it was buggy
        if (i < 0 || i > this.heap.size() - 1) {
            throw new IllegalArgumentException();
        }
		return this.heap.get(i);
	}
}
