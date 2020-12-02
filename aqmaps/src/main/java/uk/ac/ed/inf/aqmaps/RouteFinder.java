package uk.ac.ed.inf.aqmaps;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/*
 * Class that chooses the order sensors will be visited in 
 * runs a Traveling Salesman Problem optimization
 * requires a list of nodes, then a TSP function to be called
 */
public class RouteFinder {
	
	ArrayList<SensorNode> nodes; //all nodes that need to be visited
	SensorNode startnode;
	ArrayList<Integer> order;
	double[][] distances; //approximate distance lookup table for nodes (ignoring buildings)
	
	public RouteFinder(ArrayList<SensorNode> nodes) {
		this.nodes = nodes;
		this.startnode = nodes.get(0);
		
		this.distances = new double[nodes.size()][nodes.size()]; //chooses size of distances[][]
		
		//populates distances[][] with the Euclidean distance between points
		for(int i = 0; i < distances.length; i++) {
			for(int j = 0; j < distances.length; j++) {
				double[] p1 = new double[] {nodes.get(i).getLat(), nodes.get(i).getLng()};
				double[] p2 = new double[] {nodes.get(j).getLat(), nodes.get(j).getLng()};
				distances[i][j] = getEuclid(p1, p2);
			}
		}
		
		this.order = new ArrayList<Integer>();
		for(int i = 0; i < distances.length; i++) {
			order.add(i);
		}
	}
	
	public ArrayList<Integer> getOrder(){
		return order;
	}
	
	public ArrayList<SensorNode> getNodeList(){
		return nodes;
	}
	
	public void setStartNodeFirst() {
		while(nodes.get(order.get(0)) != startnode) {
			order.add(order.size(), order.get(0));
			order.remove(0);
		}
	}
	
	/*
	 * Runs a variant on insertion sort for the route, inserting each new node at the cheapest place for it until no nodes remain
	 * 
	 * Returns a List of the indexes, so that the index of the first node to visit in the provided list of nodes is at 
	 * index 0 in the returned list 
	 */
	public void tspInsertion(){
		
		//Creates list of all indexes of nodes that need to be added
		var optionList = order;
		
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
		
		System.out.println("Insertion Sort Done");
		order = finalList;
	}
	
	/*
	 * Heuristic to attempt to improve path
	 * keeps swapping nodes until it can't be improved by swapping two nodes
	 */
	public void swapHeuristic(){
		boolean better = true;
		while(better) {
			better = false;
			for(int i = 0; i < order.size(); i++) {
				if(trySwap(i)) {
					better = true; //variable to swap with next on list
				}
			}
		}
		System.out.println("Swap Heuristic Done");
	}
	
	/*
	 * checks to see if swapping the entered index of order and the next index of order is a good idea
	 * if so, swaps and returns true
	 * otherwise returns false
	 */
	private boolean trySwap(int orderIndex) { 
		int preindex = order.get((order.size() + orderIndex - 1) % order.size()); // node connecting to original
		int index = order.get(orderIndex); //original node
		int postindex = order.get((order.size() + orderIndex + 1) % order.size()); //node to swap with
		int postpostindex = order.get((order.size() + orderIndex + 2) % order.size()); //node connecting to node to swap with
		double swap = distances[preindex][postindex] + distances[postpostindex][index]; // value of the new connections formed
		double noSwap = distances[preindex][index] + distances[postindex][postpostindex]; //value of old connections lost
		
		if(swap < noSwap) { //if the swap is good, swaps them
			int temp = order.get(orderIndex);
			order.set(orderIndex, order.get((orderIndex + 1) % order.size()));
			order.set((orderIndex + 1) % order.size(), temp);
			
			return true;
		}
		else return false;
	}
	
	/*
	 * Heuristic that reverses subsections of the list until no more reverses will improve the list
	 */
	public void twoOptHeuristic() {
		boolean better = true;
		while(better) {
			better = false;
			for(int j = 0; j < order.size() - 1; j++) {
				for(int i = 0; i < j; i++) {
					if(tryReverse(i,j)) { //i and j hold the endpoints of the sublist that is considering being reversed
						better = true;
					}
				}
			}
		}
		System.out.println("Two-Opt Heuristic Done");
	}
	
	/*
	 * Tries reversing the orders between orderi and orderj
	 * Checks if it is more efficient to reverse them
	 * If so, reverses them and returns true
	 * otherwise, returns false
	 */
	private boolean tryReverse(int orderi, int orderj) {
		int preI = order.get((order.size() + orderi - 1) % order.size()); //the node that attaches to i and will be changed
		int i = order.get(orderi); 
		int j = order.get(orderj);
		int postj = order.get((order.size() + orderj + 1) % order.size()); //the node that attaches to j and will be changed
		
		double swap = distances[preI][j] + distances[i][postj]; //the cost of the two connections if swapped
		double noSwap = distances[preI][i] + distances[j][postj]; //the cost of the two changed connections if not swapped
		
		if(swap < noSwap) {
			var tempList = order.subList(orderi, orderj + 1); //temporary list holding the reversed sublist
			Collections.reverse(tempList);

			for(int k = orderi; k < orderj + 1; k++) { //copies reversed sublist into appropriate location
				order.set(k, tempList.get(k - orderi));
			}
			return true;
		}
		return false;
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
