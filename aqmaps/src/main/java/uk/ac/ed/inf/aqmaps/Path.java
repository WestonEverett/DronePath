package uk.ac.ed.inf.aqmaps;
import java.util.*;

import com.mapbox.geojson.Point;

public class Path {
	private ArrayList<Instruction> instructions;
	private Point start;
	private Point end;
	public int moveCount;
	private double moveLength = .0003;
	
	public Path(Point start, Point end) {
		this.start = start;
		this.end = end;
		this.instructions = getPathAStar();
		this.moveCount = instructions.size(); 
	}
	
	private ArrayList<Instruction> getPathAStar(){
		
		Comparator<Point> pointComparator = (p1, p2) -> {return (int) (heuristic(p1,end) > heuristic(p2,end) ? 1 : -1);};
        
		var openSet = new PriorityQueue<Point>(pointComparator);
		openSet.add(start);
		
		var cameFrom = new Hashtable<Point,Point>();
		
		var gScore = new Hashtable<Point, Double>();
		gScore.put(start, 0.);
		
		var fScore = new Hashtable<Point, Double>();
		fScore.put(start, heuristic(start, end));
		
		while(!openSet.isEmpty()) {
			var current = openSet.peek();
			for(Point node : openSet) {
				if(fScore.getOrDefault(node, Double.POSITIVE_INFINITY) < fScore.get(current)) {
					current = node;
				}
			}
			
			current = openSet.poll();
			
			if(getEuclid(current, end) < .0002) {
				//System.out.println("Reconstructing Path");
				return reconstructPath(cameFrom, current);
			}
			
			var neighbors = getNeighbors(current);
			while(!neighbors.isEmpty()) {
				var neighbor = neighbors.poll();
				var tentativeGScore = pointIsValid(neighbor) ? gScore.get(current) + moveLength : Double.POSITIVE_INFINITY;
				
				if(tentativeGScore < gScore.getOrDefault(neighbor, Double.POSITIVE_INFINITY)) {
					
					cameFrom.put(neighbor, current);
					gScore.put(neighbor, tentativeGScore);
					fScore.put(neighbor, tentativeGScore + heuristic(neighbor,end));
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
	
	private PriorityQueue<Point> getNeighbors(Point current){
		
		Comparator<Point> pointComparator = (p1, p2) -> {return (int) (heuristic(p1,end) > heuristic(p2,end) ? 1 : -1);};
        var neighbors = new PriorityQueue<Point>(pointComparator);
		
		for(int degreeAngle = 0; degreeAngle < 360; degreeAngle += 10) {
			var angle = degreeAngle * Math.PI/180;
			neighbors.add(Point.fromLngLat(current.longitude() + (moveLength * Math.cos(angle)), current.latitude() + (moveLength * Math.sin(angle))));
		}
		
		return neighbors;
	}
	
	private ArrayList<Instruction> reconstructPath(Hashtable<Point, Point> cameFrom, Point current){
		var instructionSet = new ArrayList<Instruction>();
		
		while(cameFrom.containsKey(current)) {
			var last = current;
			current = cameFrom.get(current);
			instructionSet.add(0, new Instruction(current, last, null));
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
