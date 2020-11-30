package uk.ac.ed.inf.aqmaps;
import java.awt.geom.Line2D;
import java.util.*;
import com.mapbox.geojson.Point;

/*
 * Class for calculating and holding the moves to get between SensorNodes
 */
public class Path {
	private final ArrayList<Instruction> instructions; //List of Instructions for moves between startLocation and actualEndLocation 
	private final ArrayList<double[][]> buildingCoordinates; //List of vertices of buildings
	private final Point startLocation; //Point the path the starts
	private final Point endLocation; //Location the path aims to end close to, ends within .0002
	private final String endWords; //W3W String of the node the path is going to
	private Point actualEndLocation; //Location the path actually ended in
	private int moveCount; //number of moves the path takes
	private final double moveLength = .0003; //distance step each move covers
	private Hashtable<List<Integer>, Node> generatedNodes; //Hashtable tying 18Dim coordinates to corresponding node, helps avoid infinite loops
	
	/*
	 * Path constructor
	 */
	public Path(Point start, Point end, String endWords, ArrayList<double[][]> buildingCoordinates) {
		this.buildingCoordinates = buildingCoordinates;
		this.endWords = endWords;
		this.startLocation = start;
		this.endLocation = end;
		generatedNodes = new Hashtable<List<Integer>, Node>();
		this.instructions = getPathAStar();
		this.moveCount = instructions.size(); 
	}
	
	//instruction getter
	public ArrayList<Instruction> getInstructions(){
		return instructions;
	}
	
	//actualEndLocation getter
	public Point getActualEndPoint() {
		return actualEndLocation;
	}
	
	//moveCount getter
	public int getMoveCount() {
		return moveCount;
	}
	
	/*
	 * private class for packaging Node data
	 * By storing each Node created, can avoid doubling back despite mostly greedy approach
	 */
	private class Node {
		final List<Integer> coordinates; //18Dim coordinates for moves in each angle, used to avoid duplicate nodes from rounding errors
		final Point location; //Actual Cartesian coordinates of the Node
		Double gScore; //distance traveled on most efficient path to get to node
		/*
		 * fScore: Theoretically it is the estimated total travel distance
		 * if it is overestimated, it will prioritize time efficiency of distance efficiency
		 * if it is underestimated, it will do the opposite
		 * will be discussed in depth in the report 
		 */
		Double fScore;
		Node cameFrom; //the most efficient node to come from
		
		/*
		 * constructor for Node
		 */
		public Node(List<Integer> coordinates, Point location, Double gScore, Double fScore, Node cameFrom) {
			this.coordinates = coordinates;
			this.location = location;
			this.gScore = gScore; //traveled distance
			this.fScore = fScore; //total estimated distance
		}
	}
	
	/*
	 * Creates the path between startLocation and endLocation using a variant of the A* algorithm
	 * Builds a list of nodes along with the nodes that they come from
	 * Every iteration, takes the Node that has the smallest estimated total distance (gScore + fScore) and generates/adds it's neighbors to the list 
	 */
	private ArrayList<Instruction> getPathAStar(){
		
		Node startNode = new Node(Arrays.asList(0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0), startLocation, 0., heuristic(startLocation,endLocation), null); //starting Node
        generatedNodes.put(startNode.coordinates, startNode);
        
        Comparator<Node> nodeComparator = (p1, p2) -> {return (int) (p1.fScore > p2.fScore ? 1 : -1);}; //Custom comparator for Node objects
		var openSet = new PriorityQueue<Node>(nodeComparator); //Nodes to be expanded 
		openSet.add(startNode); //adds the first node to the list
		
		while(!openSet.isEmpty()) {

			var current = openSet.poll(); //takes the node with the least estimated total distance
			
			if(getEuclid(current.location, endLocation) < .0002 && current.cameFrom != null) { //if node is within .0002 of target
				//System.out.println("Reconstructing Path");
				actualEndLocation = current.location; //sets final location, later passed to next path as starting point
				return reconstructPath(current); //builds path to get there
			}
			
			var neighbors = getNeighbors(current); //gets all neighbors of current node
			while(!neighbors.isEmpty()) { //while there are some neighbors left
				var neighbor = neighbors.poll(); //get first neighbor and remove from neighbor list
				
				//checks if move to neighbor goes through any banned areas, if not calculates a tentative new traveled distance for that note, otherwise sets it to infinity
				var tentativeGScore = pointIsValid(current.location, neighbor.location) ? current.gScore + moveLength : Double.POSITIVE_INFINITY;
				
				if(tentativeGScore < neighbor.gScore) { //if the tentative traveled distance of the neighbor is less than the stored value (if any)
					
					neighbor.cameFrom = current; //the new previous node
					neighbor.gScore = tentativeGScore; //the new traveled distance
					neighbor.fScore = tentativeGScore + heuristic(neighbor.location,endLocation); //new estimated total
					if(!openSet.contains(neighbor)) {
						openSet.add(neighbor); //adds new neighbors to set
					}
				}
			}
		}
		
		return null;
	}
	
	/*
	 * takes endpoints of move
	 * checks to see if that line intersects any buildings edges
	 * returns boolean, true if there's no intersection, false if there is
	 */
	private boolean pointIsValid(Point startPoint, Point testPoint) {
		
		//Reads Points into double variables
		var startX = startPoint.longitude();
		var startY = startPoint.latitude();
		var testX = testPoint.longitude();
		var testY = testPoint.latitude();
		
		for(double[][] coord : buildingCoordinates) { //for each set of building vertices
			for(int i = 0, j = coord.length - 1; i < coord.length; j = i++) { //for each edge
				
				//turn each line into Line2D Object
				Line2D line1 = new Line2D.Double(startX, startY, testX, testY); 
				Line2D line2 = new Line2D.Double(coord[i][0], coord[i][1], coord[j][0], coord[j][1]);
				
				if(line2.intersectsLine(line1)) return false; //check if there is an intersection
			}
			
		}
		
		return true;
	}
	
	/*
	 * Gets all neighboring nodes of a node
	 * Returns existing ones if possible
	 */
	private PriorityQueue<Node> getNeighbors(Node current){
		
		Comparator<Node> nodeComparator = (p1, p2) -> {return (int) (p1.fScore > p2.fScore ? 1 : -1);}; //custom comparator
        var neighbors = new PriorityQueue<Node>(nodeComparator); //Queue that holds the sorted neighbors
		
		for(int degreeAngle = 0; degreeAngle < 360; degreeAngle += 10) { //for each allowed angle
			var newCoordinates = new ArrayList<Integer>(current.coordinates); //makes deep copy of 18 dim step coordinates
			
			
			int stepDirection = degreeAngle/10; //index of coordinate that will change
			
			//changes new coordinates to fit neighbor
			if(stepDirection < 18) {
				newCoordinates.set(stepDirection, newCoordinates.get(stepDirection) + 1);
			}
			else {
				newCoordinates.set(stepDirection % 18, newCoordinates.get(stepDirection % 18) - 1);
			}
			
			//checks if Node already exists
			if(generatedNodes.containsKey(newCoordinates)) {
				neighbors.add(generatedNodes.get(newCoordinates));
			}
			else { //makes a new node
				var angle = degreeAngle * Math.PI/180;
				var point = Point.fromLngLat(current.location.longitude() + (moveLength * Math.cos(angle)), current.location.latitude() + (moveLength * Math.sin(angle)));
				var newNode = new Node(newCoordinates, point, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, current);
				generatedNodes.put(newCoordinates, newNode); //adds new node to generated nodes
				neighbors.add(newNode); //adds new node to neighbors
			}
		}
		
		return neighbors;
	}
	/*
	 * Takes in the node that's close enough to the endpoint
	 * uses the cameFrom property almost as a linkedlist to reconstruct the most efficient generated path
	 * returns a list of Instruction objects that describe the path
	 */
	private ArrayList<Instruction> reconstructPath(Node current){
		var instructionSet = new ArrayList<Instruction>(); //List of Instructions
		
		//Adds instruction that gets to end, so that the W3W words are on the end
		var last = current;
		current = current.cameFrom;
		instructionSet.add(0, new Instruction(current.location, last.location, endWords));
		
		//Adds the rest of the instructions, prepending each one
		while(current.cameFrom != null) {
			last = current;
			current = current.cameFrom;
			instructionSet.add(0, new Instruction(current.location, last.location, null));
		}
		
		return instructionSet;
	}
	
	/*
	 * Estimates distance to the end form a point
	 * Currently set very high so the algorithm behaves in a greedy fashion
	 * This saves time at the expense of distance 
	 */
	private double heuristic(Point current, Point goal) {
		
		double euclidDist = getEuclid(current, goal);
		return (euclidDist * 10); //will make no progress for approximately 10 moves before giving up on path
	}
	
	/*
	 * Calculates Euclidean distance between two points
	 */
	private double getEuclid(Point current, Point goal) {
		var latDif = (current.latitude() - goal.latitude());
		var longDif = (current.longitude() - goal.longitude());
		return Math.sqrt((latDif*latDif) + (longDif*longDif));
	}
	
	/*
	 * toString method to make the Instruction set printable, mainly used for testing
	 */
	public String instructionsToString() {
		String string = "";
		for(Instruction inst : instructions) {
			string += inst.toString() + "\n";
		}
		return string;
	}
}
