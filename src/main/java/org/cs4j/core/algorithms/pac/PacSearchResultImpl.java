package org.cs4j.core.algorithms.pac;

import org.cs4j.core.algorithms.SearchResultImpl;

public class PacSearchResultImpl extends SearchResultImpl{

	
	private double initialH = -1;
	
	public void setInitialH(double h){
		// enabling set initial h only once
		if(this.initialH == -1){
			this.initialH = h;
		}
	}
	public double getInitialH(){
		return this.initialH;
	}
}
