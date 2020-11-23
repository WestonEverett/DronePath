package uk.ac.ed.inf.aqmaps;

import java.util.ArrayList;
import java.util.List;

/*
 * Class that chooses the order sensors will be visited in 
 * runs a Traveling Salesman Problem optimization
 * requires a list of nodes, then a TSP function to be called
 */
public class RouteFinder {
	
	List<SensorNode> nodes; //all nodes that need to be visited
	double[][] distances; //approximate distance lookup table for nodes (ignoring buildings)
	
	public RouteFinder(List<SensorNode> nodes) {
		this.nodes = nodes;
		
		this.distances = new double[nodes.size()][nodes.size()]; //chooses size of distances[][]
		
		//populates distances[][] with the Euclidean distance between points
		for(int i = 0; i < distances.length; i++) {
			for(int j = 0; j < distances.length; j++) {
				double[] p1 = new double[] {nodes.get(i).getLat(), nodes.get(i).getLng()};
				double[] p2 = new double[] {nodes.get(j).getLat(), nodes.get(j).getLng()};
				distances[i][j] = getEuclid(p1, p2);
			}
		}
	}
	
	/*
	 * Runs a variant on insertion sort for the route, inserting each new node at the cheapest place for it until no nodes remain
	 * 
	 * Returns a List of the indexes, so that the index of the first node to visit in the provided list of nodes is at 
	 * index 0 in the returned list 
	 */
	public List<Integer> tspInsertion(){
		
		//Creates list of all indexes of nodes that need to be added
		var optionList = new ArrayList<Integer>(); 
		for(int i = 0; i < distances.length; i++) {
			optionList.add(i);
		}
		
		//initializes list of indexes for final list
		var finalList = new ArrayList<Integer>();
		
		//adds the first two indexes of the list as a base, removes them from options 
		finalList.add(optionList.get(0));
		optionList.remove(0);
		finalList.add(optionList.get(0));
		optionList.remove(0);
		
		var minOpt = new int[] {0,0}; //the index to be inserted, the location it should be inserted into on the finallist
		
		while(!optionList.isEmpty()) { //while there are indexes left to insert
			var minOptVal = Double.POSITIVE_INFINITY;
			
			//finds the cheapest node to insert and the cheapest place to insert it
			for(int i = 0; i < optionList.size(); i++) {
				for(int j = 0; j < finalList.size(); j++) {
					var newVal = distances[optionList.get(i)][finalList.get(j)]
							   + distances[optionList.get(i)][finalList.get((j + 1) % finalList.size())]
							   - distances[finalList.get(j)][finalList.get((j + 1) % finalList.size())];//calculates cost of insertion
					if(newVal < minOptVal) {
						//saves the cheaper option
						minOpt[0] = i;
						minOpt[1] = j;
						minOptVal = newVal;
					}
				}
			}
			
			//adds index to appropriate place and removes it from the options
			finalList.add(minOpt[1] + 1, optionList.get(minOpt[0]));
			optionList.remove(minOpt[0]);
		}
		
		return finalList;
	}
	
	/*
	 * Calculates Euclidean distance between two points
	 */
	private double getEuclid(double[] p1, double[] p2) {
		var xDif = (p1[0] - p2[0]) * (p1[0] - p2[0]);
		var yDif = (p1[1] - p2[1]) * (p1[1] - p2[1]);
		return Math.sqrt(xDif + yDif);
	}
}
