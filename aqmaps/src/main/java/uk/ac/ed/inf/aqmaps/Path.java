package uk.ac.ed.inf.aqmaps;
import java.util.*;

import com.mapbox.geojson.Point;

public class Path {
	public ArrayList<Instruction> instructions;
	private Point startLocation;
	private Point endLocation;
	public Point actualEndLocation;
	public int moveCount;
	private double moveLength = .0003;
	private Hashtable<List<Integer>, Node> generatedNodes;
	
	public Path(Point start, Point end) {
		this.startLocation = start;
		this.endLocation = end;
		generatedNodes = new Hashtable<List<Integer>, Node>();
		this.instructions = getPathAStar();
		this.moveCount = instructions.size(); 
	}
	
	private class Node {
		final List<Integer> coordinates;
		final Point location;
		Double gScore;
		Double fScore;
		Node cameFrom;
		
		public Node(List<Integer> coordinates, Point location, Double gScore, Double fScore, Node cameFrom) {
			this.coordinates = coordinates;
			this.location = location;
			this.gScore = gScore;
			this.fScore = fScore;
		}
	}
	
	private ArrayList<Instruction> getPathAStar(){
		
		Node startNode = new Node(Arrays.asList(0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0), startLocation, 0., heuristic(startLocation,endLocation), null);
        generatedNodes.put(startNode.coordinates, startNode);
        
        Comparator<Node> nodeComparator = (p1, p2) -> {return (int) (p1.fScore > p2.fScore ? 1 : -1);};
		var openSet = new PriorityQueue<Node>(nodeComparator);
		openSet.add(startNode);
		
		while(!openSet.isEmpty()) {

			var current = openSet.poll();
			
			/*System.out.println();
			for(int num : current.coordinates) {
				System.out.print(num + " ");
			}*/
			
			if(getEuclid(current.location, endLocation) < .0002) {
				//System.out.println("Reconstructing Path");
				actualEndLocation = current.location;
				return reconstructPath(current);
			}
			
			var neighbors = getNeighbors(current);
			while(!neighbors.isEmpty()) {
				var neighbor = neighbors.poll();
				var tentativeGScore = pointIsValid(neighbor.location) ? current.gScore + moveLength : Double.POSITIVE_INFINITY;
				
				if(tentativeGScore < neighbor.gScore) {
					
					neighbor.cameFrom = current;
					neighbor.gScore = tentativeGScore;
					neighbor.fScore = tentativeGScore + heuristic(neighbor.location,endLocation);
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
		
		Comparator<Node> nodeComparator = (p1, p2) -> {return (int) (p1.fScore > p2.fScore ? 1 : -1);};
        var neighbors = new PriorityQueue<Node>(nodeComparator);
		
		for(int degreeAngle = 0; degreeAngle < 360; degreeAngle += 10) {
			var newCoordinates = new ArrayList<Integer>(current.coordinates);
			
			
			int stepDirection = degreeAngle/10;
			
			if(stepDirection < 18) {
				newCoordinates.set(stepDirection, newCoordinates.get(stepDirection) + 1);
			}
			else {
				newCoordinates.set(stepDirection % 18, newCoordinates.get(stepDirection % 18) - 1);
			}
			
			if(generatedNodes.containsKey(newCoordinates)) {
				neighbors.add(generatedNodes.get(newCoordinates));
			}
			else {
				var angle = degreeAngle * Math.PI/180;
				var point = Point.fromLngLat(current.location.longitude() + (moveLength * Math.cos(angle)), current.location.latitude() + (moveLength * Math.sin(angle)));
				var newNode = new Node(newCoordinates, point, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, current);
				generatedNodes.put(newCoordinates, newNode);
				neighbors.add(newNode);
			}
		}
		
		return neighbors;
	}
	
	private ArrayList<Instruction> reconstructPath(Node current){
		var instructionSet = new ArrayList<Instruction>();
		
		while(current.cameFrom != null) {
			var last = current;
			current = current.cameFrom;
			instructionSet.add(0, new Instruction(current.location, last.location, null));
		}
		
		return instructionSet;
	}
	
	private double heuristic(Point current, Point goal) {
		
		double euclidDist = getEuclid(current, goal);
		return (euclidDist * 100);
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
