package uk.ac.ed.inf.aqmaps;

import java.util.ArrayList;
import java.util.List;

public class RouteFinder {
	
	List<SensorNode> nodes;
	double[][] distances;
	
	public RouteFinder(List<SensorNode> nodes) {
		this.nodes = nodes;
		
		this.distances = new double[nodes.size()][nodes.size()];
		
		for(int i = 0; i < distances.length; i++) {
			for(int j = 0; j < distances.length; j++) {
				double[] p1 = new double[] {nodes.get(i).getLat(), nodes.get(i).getLng()};
				double[] p2 = new double[] {nodes.get(j).getLat(), nodes.get(j).getLng()};
				distances[i][j] = getEuclid(p1, p2);
			}
		}
	}
	
	public List<Integer> tspInsertion(){
		
		var optionList = new ArrayList<Integer>();
		for(int i = 0; i < distances.length; i++) {
			optionList.add(i);
		}
		
		var finalList = new ArrayList<Integer>();
		
		finalList.add(optionList.get(0));
		optionList.remove(0);
		finalList.add(optionList.get(0));
		optionList.remove(0);
		
		var minOpt = new int[] {0,0};
		
		while(!optionList.isEmpty()) {
			var minOptVal = Double.POSITIVE_INFINITY;
			
			for(int i = 0; i < optionList.size(); i++) {
				for(int j = 0; j < finalList.size(); j++) {
					var newVal = distances[optionList.get(i)][finalList.get(j)]
							   + distances[optionList.get(i)][finalList.get((j + 1) % finalList.size())]
							   - distances[finalList.get(j)][finalList.get((j + 1) % finalList.size())];
					if(newVal < minOptVal) {
						minOpt[0] = i;
						minOpt[1] = j;
						minOptVal = newVal;
					}
				}
			}
			
			finalList.add(minOpt[1] + 1, optionList.get(minOpt[0]));
			optionList.remove(minOpt[0]);
		}
		
		return finalList;
	}
	
	private double getEuclid(double[] p1, double[] p2) {
		var xDif = (p1[0] - p2[0]) * (p1[0] - p2[0]);
		var yDif = (p1[1] - p2[1]) * (p1[1] - p2[1]);
		return Math.sqrt(xDif + yDif);
	}
}
