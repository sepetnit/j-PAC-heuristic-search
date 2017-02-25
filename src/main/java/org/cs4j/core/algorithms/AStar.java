package org.cs4j.core.algorithms;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.cs4j.core.SearchDomain;
import org.cs4j.core.SearchDomain.Operator;
import org.cs4j.core.SearchDomain.State;
import org.cs4j.core.SearchResult;
import org.cs4j.core.algorithms.SearchResultImpl.SolutionImpl;
import org.cs4j.core.algorithms.pac.PacSearchResultImpl;
import org.cs4j.core.collections.PackedElement;
import org.cs4j.core.collections.Pair;

public class AStar extends WAStar {

	public AStar() {
		super(); // call to default constractor --> set weight = 1
	}

	@Override
	public SearchResult search(SearchDomain domain) {
		// Initialize all the data structures required for the search
		this._initDataStructures(domain);
		Node goal = null;

		result = new PacSearchResultImpl();

		result.startTimer();

		// Let's instantiate the initial state
		State currentState = domain.initialState();
		// Create a graph node from this state
		Node initNode = new Node(currentState);
		result.setInitialH(initNode.getH());

		// And add it to the frontier
		_addNode(initNode);
		try {
			// Loop over the frontier
			while (!this.open.isEmpty() && result.getGenerated() < this.domain.maxGeneratedSize()
					&& result.checkMinTimeOut()) {
				// Take the first state (still don't remove it)
				// Node currentNode = this.open.poll();
				Node currentNode = _selectNode();
				// Prune
				if (currentNode.getRf() >= this.maxCost) {
					continue;
				}

				// Extract the state from the packed value of the node
				currentState = domain.unpack(currentNode.packed);

				// System.out.println(currentState.dumpStateShort());
				// Check for goal condition
				if (domain.isGoal(currentState)) {
					goal = currentNode;
					break;
				}

				List<Pair<State, Node>> children = new ArrayList<>();

				// Expand the current node
				++result.expanded;
				// Stores parent h-cost (from path-max)
				double bestHValue = 0.0d;
				// First, let's generate all the children
				// Go over all the possible operators and apply them
				for (int i = 0; i < domain.getNumOperators(currentState); ++i) {
					Operator op = domain.getOperator(currentState, i);
					// Try to avoid loops
					if (op.equals(currentNode.pop)) {
						continue;
					}
					State childState = domain.applyOperator(currentState, op);
					Node childNode = new Node(childState, currentNode, currentState, op, op.reverse(currentState));
					// Here we actually generated a new state
					++result.generated;
					/*
					 * if(result.getGenerated() % 1000 == 0){ DecimalFormat
					 * formatter = new DecimalFormat("#,###"); //
					 * System.out.println("[INFO] WA Generated:" +
					 * formatter.format(result.getGenerated()) +
					 * "\tTime:"+passed);
					 * System.out.print("\r[INFO] WA Generated:" +
					 * formatter.format(result.getGenerated()));
					 * 
					 * }
					 */
					// Perform only if BPMX is required
					if (this.useBPMX) {
						bestHValue = Math.max(bestHValue, childNode.h - op.getCost(childState, currentState));
					}
					children.add(new Pair<>(childState, childNode));
				}

				// Update the H Value of the parent in case of BPMX
				if (this.useBPMX) {
					currentNode.h = Math.max(currentNode.h, bestHValue);
					// Prune
					if (currentNode.getRf() >= this.maxCost) {
						continue;
					}
				}

				// Go over all the possible operators and apply them
				for (Pair<State, Node> currentChild : children) {
					State childState = currentChild.getKey();
					Node childNode = currentChild.getValue();
					double edgeCost = childNode.op.getCost(childState, currentState);

					// Prune
					if (childNode.getRf() >= this.maxCost) {
						continue;
					}
					// Treat duplicates
					boolean contains = true;
					PackedElement p = childNode.packed;
					Node testNode = this.closed.get(p);
					if (testNode == null) {
						contains = false;
					}
					// contains = this.closed.containsKey(childNode.packed);
					if (contains) {
						// Count the duplicates
						++result.duplicates;
						// Get the previous copy of this node (and extract it)
						Node dupChildNode = this.closed.get(childNode.packed);

						// Propagate the H value to child (in case of BPMX)
						if (this.useBPMX) {
							dupChildNode.h = Math.max(dupChildNode.h, currentNode.h - edgeCost);
						}

						// Found a shorter path to the node
						if (dupChildNode.g > childNode.g) {
							// Check that the f actually decreases
							if (dupChildNode.getWf() > childNode.getWf()) {
								// Do nothing
							} else {
								if (this.domain.isCurrentHeuristicConsistent()) {
									assert false;
								}
								continue;
							}
							// In any case update the duplicate with the new
							// values - we reached it via a shorter path
							double dupF = dupChildNode.getF();
							dupChildNode.g = childNode.g;
							dupChildNode.op = childNode.op;
							dupChildNode.pop = childNode.pop;
							dupChildNode.parent = childNode.parent;

							// if dupChildNode is in open, update it there too
							if (dupChildNode.getIndex(this.open.getKey()) != -1) {
								++result.opupdated;
								this.open.update(dupChildNode);
								// this.openF.updateF(dupChildNode, dupF);
							}
							// Otherwise, consider to reopen dupChildNode
							else {
								// Return to OPEN list only if reopening is
								// allowed
								if (this.reopen) {
									++result.reopened;
									_addNode(dupChildNode);
								}
							}
							// in any case, update closed to be bestChild
							this.closed.put(dupChildNode.packed, dupChildNode);

						} else {
							// A shorter path has not been found, but let's
							// update the node in open if its h increased
							if (this.useBPMX) {
								if (dupChildNode.getIndex(this.open.getKey()) != -1) {
									this.open.update(dupChildNode);
								}
							}
						}
						// Otherwise, the node is new (hasn't been reached yet)
					} else {
						// Propagate the H value to child (in case of BPMX)
						if (this.useBPMX) {
							childNode.h = Math.max(childNode.h, currentNode.h - edgeCost);
						}
						_addNode(childNode);
					}
				}
			}
		} catch (OutOfMemoryError e) {
			System.out.println("[INFO] WAstar OutOfMemory :-( " + e);
			System.out.println("[INFO] OutOfMemory WAstar on:" + this.domain.getClass().getSimpleName() + " generated:"
					+ result.getGenerated());
		}

		result.stopTimer();
		// System.out.println("Generated:\t"+result.getGenerated());
		// System.out.println("closed Size:\t"+this.closed.size());
		// result.printArrCpuTimeMillis();

		// If a goal was found: update the solution
		if (goal != null) {
			System.out.print("\r");
			SolutionImpl solution = new SolutionImpl(this.domain);
			List<Operator> path = new ArrayList<>();
			List<State> statesPath = new ArrayList<>();
			// System.out.println("[INFO] Solved - Generating output path.");
			double cost = 0;

			State currentPacked = domain.unpack(goal.packed);
			State currentParentPacked = null;
			for (Node currentNode = goal; currentNode != null; currentNode = currentNode.parent, currentPacked = currentParentPacked) {
				// If op of current node is not null that means that p has a
				// parent
				// System.out.println(currentPacked.dumpStateShort());
				if (currentNode.op != null) {
					path.add(currentNode.op);
					currentParentPacked = domain.unpack(currentNode.parent.packed);
					cost += currentNode.op.getCost(currentPacked, currentParentPacked);
				}
				statesPath.add(domain.unpack(currentNode.packed));
			}
			// The actual size of the found path can be only lower the G value
			// of the found goal
			assert cost <= goal.g;
			double roundedCost = new BigDecimal(cost).setScale(4, RoundingMode.HALF_DOWN).doubleValue();
			double roundedG = new BigDecimal(goal.g).setScale(4, RoundingMode.HALF_DOWN).doubleValue();
			if (roundedCost - roundedG < 0) {
				System.out.println(
						"[INFO] Goal G is higher that the actual cost " + "(G: " + goal.g + ", Actual: " + cost + ")");
			}

			Collections.reverse(path);
			solution.addOperators(path);

			Collections.reverse(statesPath);
			solution.addStates(statesPath);

			solution.setCost(cost);
			result.addSolution(solution);
		}

		return result;
	}
}
