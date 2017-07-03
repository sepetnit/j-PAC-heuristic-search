package org.cs4j.core.algorithms.auxiliary;

/**
 * Created by Vitali Sepetnitsky on 2017-06-26.
 *
 */

import org.cs4j.core.Operator;
import org.cs4j.core.SearchState;
import org.cs4j.core.collections.PackedElement;

/**
 * The node class
 */
public class GenericNode<T extends GenericNode<T>> extends SearchQueueElementImpl {

    protected double g;
    protected double h;

    protected Operator op;
    protected Operator pop;

    protected T parent;

    protected PackedElement packed;

//      private int[] secondaryIndex;

    public GenericNode getNode(SearchState state) {
        return new GenericNode(state);
    }


    public GenericNode(SearchState state,
                       T parent,
                       SearchState parentState,
                       Operator op,
                       Operator pop) {
        // Size of key
        super(2);
        // Parent node
        this.parent = parent;

        // TODO: Why?
//            this.secondaryIndex = new int[(heapType == HeapType.BUCKET) ? 2 : 1];
        double cost = (op != null) ? op.getCost(state, parentState) : 0;
        this.h = this.computeH(state);
        // If each operation costs something, we should add the cost to the g value of the parent
        this.g = (parent != null) ? parent.g + cost : cost;

        this.packed = state.pack();
        this.pop = pop;
        this.op = op;
    }

    /**
     * A constructor of the class that instantiates only the state
     *
     * @param state The state which this node represents
     */
    protected GenericNode(SearchState state) {
        this(state, null, null, null, null);
    }

    protected double computeH(SearchState state) {
        return state.getH();
    }

    public void copyFromDuplicateNode(GenericNode<T> other) {
        this.g = other.getG();
        this.op = other.getOp();
        this.pop = other.getPop();
        this.parent = other.getParent();
    }

    /**
     * @return The value of the regular evaluation function
     */
    public double getRf() {
        return this.g + this.h;
    }

    @Override
    public double getF() {
        return this.g + this.h;
    }


    @Override
    public double getG() {
        return this.g;
    }

    @Override
    public double getDepth() {
        return 0;
    }

    @Override
    public double getH() {
        return this.h;
    }

    public double setH(double hValue) {
        double previousH = this.getH();
        this.h = hValue;
        return previousH;
    }

    @Override
    public double getD() {
        return 0;
    }

    @Override
    public double getHhat() {
        return 0;
    }

    @Override
    public double getDhat() {
        return 0;
    }

    public T getParent() {
        return this.parent;
    }

    public PackedElement getPacked() {
        return this.packed;
    }

    public Operator getOp() {
        return this.op;
    }

    public Operator getPop() {
        return this.pop;
    }
}
