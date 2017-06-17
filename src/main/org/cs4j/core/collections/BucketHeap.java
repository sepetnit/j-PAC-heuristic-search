/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.cs4j.core.collections;

import java.util.ArrayList;
import java.util.List;

import org.cs4j.core.collections.BucketHeap.BucketHeapElement;

/**
 * An implementation of a bucket heap where elements are aware of their
 * location (index) in the heap.
 *
 * @author Matthew Hatem & Vitali Sepetnitsky
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public final class BucketHeap<E extends BucketHeapElement> implements SearchQueue<E> {
    
    // Number of elements in the heap
    private int fill;
    // Number of buckets in the heap
    private int size;
    private int min = Integer.MAX_VALUE;
    private Bucket[] buckets;
    private int key;
    
    /**
     * Constructor of the heap
     *
     * @param size Number of buckets to create
     * @param key The index at the single heap element, that allows accessing a single element
     */
    public BucketHeap(int size, int key) {
        this.size = size;
        // Create an array of buckets
        this.buckets = new Bucket[size];
        this.key = key;
    }
  
    @Override
    public int getKey() {
        return this.key;
    }
  
    /**
     * The function inserts the given element to the heap and assures the heap property is still
     * true after the insertion
     *
     * @param e The element to insert
    */
    @Override
    public void add(E e) {
        int p0 = (int)e.getRank(0);
        // Update the minimal element of the heap
        if (p0 < this.min) {
            this.min = p0;
        }
        // Get the relevant bucket, according to the rank of the element
        Bucket<E> bucket = this.buckets[p0];
        // Create a new bucket if required
        if (bucket == null) {
            // The size of each bucket is equal to the number of elements in the bucket
            bucket = new Bucket<>(this.buckets.length, this.key);
            // Insert the bucket to the array
            this.buckets[p0] = bucket;
        }
        // Update the index of the element to correspond to its rank
        e.setIndex(this.key, p0);

        // Get the rank of the element, at level 1
        int p1 = (int)e.getRank(1);
        // Insert the element to the bucket, according to the relevant rank
        bucket.push(e, p1);
        // Update the secondary index of the element according to its rank at the first level
        e.setSecondaryIndex(this.key, p1);
        // Increase the number of elements
        ++this.fill;
    }

    /**
     * The function removes the first element of the heap and returns it
     *
     * @return The first element of the heap
     */
    @Override
    public E poll() {
        // Find the first non-empty bucket
        for (; this.min < this.buckets.length; ++this.min) {
            // Find the first non-empty bucket
            Bucket minBucket = this.buckets[this.min];
            // Break if the bucket was found
            if (minBucket != null && !minBucket.isEmpty()) {
                break;
            }
        }
        // Decrease the number of elements in the heap
        --fill;

        // Get the found bucket
        Bucket<E> minBucket = this.buckets[this.min];
        // Take the first element of the bucket
        E e = minBucket.pop();
        // Clean the indexes of the taken element (make the element un-aware of its
        // place in the heap)
        e.setIndex(key, -1);
        e.setSecondaryIndex(key, -1);
        return e;
    }

    /**
     * The function takes the first element of the heap and returns it
     * (without removing) the element
     *
     * @return The first element of the heap
     */
    @Override
    public E peek() {
  	    int min = this.min;
  	    // Find the first non-empty bucket
        for (; min < this.buckets.length; ++min) {
            Bucket minBucket = this.buckets[min];
            if (minBucket != null && !minBucket.isEmpty()) {
                break;
            }
        }
        // Take the bucket
        Bucket<E> minBucket = this.buckets[min];
        // Return the fist element of the bucket
        return minBucket.peek();
    }

    /**
     * The function updates the given element in the heap (replaces it)
     *
     * @param e The element to replace
     */
	@Override
    // Update h
	public void update(E e) {
	    // Get the location of the element (the relevant bucket)
		int p0 = e.getIndex(this.key);
		// Assure the returned bucket is valid
		if (p0 > this.buckets.length - 1 || p0 < 0) {
            throw new IllegalArgumentException();
        }
        // Take the bucket from the buckets list
		Bucket<E> b = this.buckets[p0];
		// Remove the element and add the updated one
		b.remove(e);
		this.add(e);
	}

    /**
     * The function removes the given element from the heap and returns it
     *
     * @param e The element to remove
     *
     * @return The removed element
     */
	@Override
	public E remove(E e) {
	    // Get the bucket in which the given element is located
		int p0 = e.getIndex(this.key);
		// Assure the bucket index is valid
		if (p0 > this.buckets.length - 1 || p0 < 0) {
            throw new IllegalArgumentException();
        }
        // Take the bucket
		Bucket<E> b = this.buckets[p0];
		// Remove the element from the bucket
		b.remove(e);
		// Return the element
		return e;
	}

    /**
     * The function clears the heap
     */
	@Override
	public void clear() {
		this.fill = 0;
		this.min = Integer.MAX_VALUE;
		// Re-create the buckets list
		this.buckets = new Bucket[this.size];
	}

    /**
     * @return Whether the heap is empty
     */
    @Override
    public boolean isEmpty() {
        return fill == 0;
    }

    /**
     * @return The number of elements in the heap
     */
    @Override
    public int size() {
      	return this.fill;
    }

    /**
     * This class represents a single bucket of the heap, which stores the
     * elements
     *
     * @param <E> The type of elements stored in the heap
     */
    private static final class Bucket<E extends BucketHeapElement> {
        private int fill;
        private int max;
        // A single bucket contains an array of arrays
        private ArrayList[] bins;
        private int key;

        /**
         * The constructor of the class - creates a single bucket
         *
         * @param size The size of the bucket (corresponds to the size of the heap)
         * @param key The key which allows accessing the rank of each element
         */
        private Bucket(int size, int key) {
            this.bins = new ArrayList[size];
            this.key = key;
        }

        /**
         * Adds a single element to the bucket
         *
         * @param e The element to add
         * @param er The rank of the element
         */
        private void push(E e, int er) {
            // Update the maximum rank of elements stored in the heap
            if (er > this.max) {
                this.max = er;
            }
            // Get the relevant bin (according to the rank)
            ArrayList<E> binEr = this.bins[er];
            // If there is no list corresponding to the given rank, create it
            if (binEr == null) {
                binEr = new ArrayList<>(10000);
                this.bins[er] = binEr;
            }
            // Add the element to the created list
            binEr.add(e);
            // Update the number of elements stored in the bucket
            ++this.fill;
        }

        /**
         * Remove the element with the maximum rank from the current bucket and
         * return it
         *
         * @return The removed element
         */
        private E pop() {
            // Find the bin the maximum rank
            for ( ; max > 0; max--) {
                ArrayList<E> maxBin = this.bins[this.max];
                if (maxBin != null && !maxBin.isEmpty()) {
                    break;
                }
            }
            // Get the found bin
            ArrayList<E> maxBin = this.bins[this.max];
            // Find the last non-empty index of the bin
            int last = maxBin.size() - 1;
            // Take the last element and remove it from the bin
            E n = maxBin.get(last);
            maxBin.remove(last);
            // Update the total number of elements in the bin
            --fill;
            // Return the found element
            return n;
        }

        /**
         * Return the element with the highest rank (without removing it)
         *
         * @return The found element
         */
        private E peek() {
            int max = this.max;
            // Take the maximum index of a bin, which contains at least one element
            for ( ; max > 0; max--) {
                ArrayList<E> maxBin = this.bins[max];
                if (maxBin != null && !maxBin.isEmpty()) {
                    break;
                }
            }
            // Get the relevant bin
            ArrayList<E> maxBin = this.bins[max];
            // Get the last non-empty index of the bin
            int last = maxBin.size() - 1;
            // Return the element stored at the found index (without removing it!)
            return maxBin.get(last);
        }

        /**
         * Removes the given element from the heap
         *
         * @param e The element to remove
         */
        private void remove(E e) {
            // Find the bin in which the element is stored
            int p1 = e.getSecondaryIndex(key);
            if (p1 > this.bins.length - 1 || p1 < 0) {
                throw new IllegalArgumentException();
            }
            // Take the relevant list
            List<E> list = this.bins[p1];
            // Try to remove the element from the found list
            if (!list.remove(e)) {
                throw new IllegalArgumentException();
            }
            // Update the total number of elements in the bucket
            --fill;
        }

        /**
         * @return Whether the bucket contains at least a single element
         */
        private boolean isEmpty() {
            return fill == 0;
        }
    }

    /**
     * The interface represents an element which can be added to the heap
     */
    public interface BucketHeapElement extends SearchQueueElement {

        /**
         * Set the secondary index of the element, which represents the bucket
         * in which the element will be stored
         *
         * @param key The key in the map, in which the given index will be stored
         * @param index The value to store
         */
        void setSecondaryIndex(int key, int index);

        /**
         * @param key The key in the map, in which the index is stored
         *
         * @return The found index
         */
        int getSecondaryIndex(int key);

        /**
         * @param level The level to return
         *
         * @return The found rank which is stored at the given level
         */
        double getRank(int level);
    }
}
