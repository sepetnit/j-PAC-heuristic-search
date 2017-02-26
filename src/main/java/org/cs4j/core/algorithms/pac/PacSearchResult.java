package org.cs4j.core.algorithms.pac;

import org.cs4j.core.SearchResult;
import org.cs4j.core.algorithms.SearchResultImpl;

public class PacSearchResult extends SearchResultImpl{

	// The h of the start state
	private double initialH = -1;

	/**
	 * Copy constructor that only adds the initial h over the given results.
	 * @TODO: Verify we copy all fields
	 * @param initialH the h value of the inital state
	 * @param result the SearchResults object to copy values from
	 */
	public PacSearchResult(double initialH, SearchResult result) {
		super();
		this.setInitialH(initialH);
		this.setExpanded(result.getExpanded());
		this.setGenerated(result.getGenerated());
		if(result.hasSolution())
			this.getSolutions().add(result.getSolutions().get(0));
	}

	public void setInitialH(double h){
		// enabling set initial h only once @TODO: Not sure if this will cause more trouble than be helpful?
		if(this.initialH == -1){
			this.initialH = h;
		}
	}
	public double getInitialH(){
		return this.initialH;
	}
}

