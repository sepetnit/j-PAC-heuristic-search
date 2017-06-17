package org.cs4j.core.collections;

/**
 * Created by user on 2017-04-24.
 *
 */
public class NonOrderedPair<E> {

    private E first;
    private E second;

    public NonOrderedPair(E first, E second) {
        this.first = first;
        this.second = second;
    }

    public E getFirst() {
        return this.first;
    }

    public E getSecond() {
        return this.second;
    }

    public void setFirst(E first) {
        this.first = first;
    }

    public void setSecond(E second) {
        this.second = second;
    }

    @Override
    public String toString() {
        return "(" + this.first + ", " + this.second + ") " +
                "/" +
                (" (" + this.second + ", " + this.first + ")");
    }

    @Override
    // We check the class of the object
    @SuppressWarnings("unchecked")
    public boolean equals(Object other) {
        if (other.getClass() == this.getClass()) {
            NonOrderedPair<E> otherAsNonOrderedPair = (NonOrderedPair<E>) other;
            return ((this.first == otherAsNonOrderedPair.first &&
                    this.second == otherAsNonOrderedPair.getSecond())
                    ||
                    (this.second == otherAsNonOrderedPair.first &&
                            this.first == otherAsNonOrderedPair.second));
        }
        return false;
    }
}
