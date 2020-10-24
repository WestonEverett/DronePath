package uk.ac.ed.inf.aqmaps;
import java.util.*;

import com.mapbox.geojson.Point;

public class Path {
	private ArrayList<Instruction> instructions;
	private Point startLocation;
	private Point endLocation;
	public int moveCount;
	private double moveLength = .0003;
	private Hashtable<List<Integer>, Node> generatedNodes;
	
	public Path(Point start, Point end) {
		this.startLocation = start;
		this.endLocation = end;
		this.instructions = getPathAStar();
		this.moveCount = instructions.size(); 
	}
	
	private class Node {
		final List<Integer> coordinates;
		final Point location;
		
		public Node(List<Integer> coordinates, Point location) {
			this.coordinates = coordinates;
			this.location = location;
		}
	}
	
	private ArrayList<Instruction> getPathAStar(){
		
		Node startNode = new Node(Arrays.asList(0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0), startLocation);
        generatedNodes.put(startNode.coordinates, startNode);
        
        Comparator<Node> nodeComparator = (p1, p2) -> {return (int) (heuristic(p1.location,endLocation) > heuristic(p2.location,endLocation) ? 1 : -1);};
		var openSet = new PriorityQueue<Node>(nodeComparator);
		openSet.add(startNode);
		
		var cameFrom = new Hashtable<Node,Node>();
		
		var gScore = new Hashtable<Node, Double>();
		gScore.put(startNode, 0.);
		
		var fScore = new Hashtable<Node, Double>();
		fScore.put(startNode, heuristic(startLocation, endLocation));
		
		while(!openSet.isEmpty()) {
			var current = openSet.peek();
			for(Node node : openSet) {
				if(fScore.getOrDefault(node, Double.POSITIVE_INFINITY) < fScore.get(current)) {
					current = node;
				}
			}
			
			current = openSet.poll();
			
			if(getEuclid(current.location, endLocation) < .0002) {
				//System.out.println("Reconstructing Path");
				return reconstructPath(cameFrom, current);
			}
			
			var neighbors = getNeighbors(current);
			while(!neighbors.isEmpty()) {
				var neighbor = neighbors.poll();
				var tentativeGScore = pointIsValid(neighbor.location) ? gScore.get(current) + moveLength : Double.POSITIVE_INFINITY;
				
				if(tentativeGScore < gScore.getOrDefault(neighbor, Double.POSITIVE_INFINITY)) {
					
					cameFrom.put(neighbor, current);
					gScore.put(neighbor, tentativeGScore);
					fScore.put(neighbor, tentativeGScore + heuristic(neighbor.location,endLocation));
					//System.out.println(fScore.get(neighbor) + "," + gScore.get(neighbor));
					if(!openSet.contains(neighbor)) {
						openSet.add(neighbor);
					}
				}
			}
		}
		
		return null;
	}
	
	private boolean pointIsValid(Point testPoint) {
		//ToDo check if point is in no-fly space or outside of box
		if(testPoint.latitude() > testPoint.longitude() && testPoint.longitude() > .001) return false;
		return true;
	}
	
	private PriorityQueue<Node> getNeighbors(Node current){
		
		Comparator<Node> nodeComparator = (p1, p2) -> {return (int) (heuristic(p1.location,endLocation) > heuristic(p2.location,endLocation) ? 1 : -1);};
        var neighbors = new PriorityQueue<Node>(nodeComparator);
		
		for(int degreeAngle = 0; degreeAngle < 360; degreeAngle += 10) {
			var newCoordinates = new ArrayList<Integer>(current.coordinates);
			
			int stepDirection = degreeAngle/10;
			
			if(stepDirection < 18) {
				newCoordinates.set(stepDirection, newCoordinates.get(stepDirection) + 1);
			}
			else {
				newCoordinates.set(stepDirection % 18, newCoordinates.get(stepDirection) - 1);
			}
			
			if(generatedNodes.containsKey(newCoordinates)) {
				neighbors.add(generatedNodes.get(newCoordinates));
			}
			else {
				var angle = degreeAngle * Math.PI/180;
				var point = Point.fromLngLat(current.location.longitude() + (moveLength * Math.cos(angle)), current.location.latitude() + (moveLength * Math.sin(angle)));
				var newNode = new Node(newCoordinates, point);
				generatedNodes.put(newCoordinates, newNode);
				neighbors.add(newNode);
			}
		}
		
		return neighbors;
	}
	
	private ArrayList<Instruction> reconstructPath(Hashtable<Node, Node> cameFrom, Node current){
		var instructionSet = new ArrayList<Instruction>();
		
		while(cameFrom.containsKey(current)) {
			var last = current;
			current = cameFrom.get(current);
			instructionSet.add(0, new Instruction(current.location, last.location, null));
		}
		
		return instructionSet;
	}
	
	private double heuristic(Point current, Point goal) {
		
		var latDif = (current.latitude() - goal.latitude());
		var longDif = (current.longitude() - goal.longitude());
		return Math.sqrt((latDif*latDif) + (longDif*longDif));
		
		//double euclidDist = getEuclid(current, goal);
		//return (int) (euclidDist/moveLength);
	}
	
	private double getEuclid(Point current, Point goal) {
		var latDif = (current.latitude() - goal.latitude());
		var longDif = (current.longitude() - goal.longitude());
		return Math.sqrt((latDif*latDif) + (longDif*longDif));
	}
	
	public String instructionsToString() {
		String string = "";
		for(Instruction inst : instructions) {
			string += inst.toString() + "\n";
		}
		return string;
	}
}
