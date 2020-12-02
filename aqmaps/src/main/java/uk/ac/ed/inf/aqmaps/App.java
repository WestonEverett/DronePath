package uk.ac.ed.inf.aqmaps;
import java.net.http.*;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.*;

import com.mapbox.geojson.*;

import com.google.gson.*;
import com.google.gson.reflect.*;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;

public class App 
{
	//edges of the drone containment area

	final static int maxMoves = 150;
	
    public static void main( String[] args )
    {
    	final String day = args[0]; //DD
    	final String month = args[1]; //MM
    	final String year = args[2]; //YYYY
    	final double startLat = Double.parseDouble(args[3]);  //Drone start position
    	final double startLng = Double.parseDouble(args[4]);
    	final int seed = Integer.parseInt(args[5]); //seed for randomness (unused)
    	final String port = args[6]; //Server port, usually 80
    	
    	var sensorNodeList = FileManager.getSensorList(day,month,year,port); //gets list of SensorNode objects and gets their coordinates from the WebServer
    	sensorNodeList.add(0, new SensorNode(null, 0, null, "#ffffff", "cross", startLng, startLat)); //adds the starting position, marked by a white x, should be removed before it is displayed
   	
    	var buildingFeatures = FileManager.getBuildingFeatures(port); //reads the buildings from WebServer into a feature list and adds the drone containment area
    	
    	var buildingCoordinates = new ArrayList<double[][]>(); //ArrayList of all the vertices sets of the buildings
    	
    	for(Feature feat : buildingFeatures) {
    		buildingCoordinates.add(new Gson().fromJson(feat.toJson(), Geometry.class).geometry.coordinates[0]); //adds the double[][] vertices coordinates from each building feature
    	}
    	
    	var fullPath = getFullPath(sensorNodeList, buildingCoordinates); //Gets the List of Path objects each holding a list of Instruction objects with the drones moves between nodes
    	
    	var pointList = new ArrayList<Point>(); //List of points for a json file in order to display the moves
    	
    	int counter = 1; //which number instruction in flightpath file
    	
    	String concat = ""; //String to be saved into flightpath file
    	
    	pointList.add(Point.fromLngLat(startLng, startLat)); //adds starting point
    	var visitedSensorLocations = new HashSet<String>();
    	
    	for(Path path : fullPath) { //for each Path (moves to get from one SensorNode to another) in fullPath
    		
    		for(Instruction inst : path.getInstructions()) { //for each of the individual instructions in the path
    			pointList.add(inst.getPostMove()); //adds end of each move to pointList
    			
    			concat = concat + counter + "," + inst.toString() + "\n"; //adds the instruction to the string with the number instruction it is
    			
    			if(inst.getSensorLocation() != null) {
    				visitedSensorLocations.add(inst.getSensorLocation());
    			}
    				
    			
    			if(counter > maxMoves) { //breaks early if out of moves
    				System.out.println("Out of Moves");
    				break;
    			}
    			counter++;
    		}
    		if(counter > maxMoves) break; //breaks early if out of moves
    	}
    	
    	FileManager.saveFile(concat,"flightpath-" + day + "-" + month + "-" + year + ".txt"); //saves the prepared string as flightpath-DD-MM-YYYY.txt
    	
    	sensorNodeList.remove(0); //removes the start point from sensorNodeList
    	
    	for(SensorNode node : sensorNodeList) { //marks all nodes not visited so they will not show readings
    		if(!visitedSensorLocations.contains(node.getLocation())) node.setToUnvisited();
    	}
    	
    	var features = getPointFeatures(sensorNodeList); //gets all the points from SensorNodeList
    	features.add(Feature.fromGeometry(LineString.fromLngLats(pointList))); 
    	var jsonStr = FeatureCollection.fromFeatures(features).toJson(); //converts the FeatureCollection to a Json String
    	FileManager.saveFile(jsonStr,"readings-" + day + "-" + month + "-" + year + ".geojson"); //saves the Json String as readings-DD-MM-YYYY.geojson
    	
    	System.out.println("Done in " + (counter - 1) + " moves");
    	
    }
    
    class Geometry { //class for parsing building geometry jsons using Gson
        GeometryData geometry; //holds coordinates
    }   
    class GeometryData { //class for parsing building geometry jsons using Gson
        String type; //unused
        double[][][] coordinates; //List of building vertices in coordinates[0]
    }
   
    /*
     * Takes in the list of SensorNodes and the list of buildings
     * outputs a list of Path objects that visits every node and returns to the start point
     */
    private static ArrayList<Path> getFullPath(ArrayList<SensorNode> sensorNodeList, ArrayList<double[][]> buildingCoordinates) {
    	
    	var finder = new RouteFinder(sensorNodeList); //creates a RouteFinder object, which takes the list of SensorNodes and decides what order to visit them in
    	finder.tspInsertion(); //runs an insertion algorithm to choose the order
    	finder.twoOptHeuristic();
    	finder.swapHeuristic();
    	finder.setStartNodeFirst();
    	var optOrder = finder.getOrder();
    	
    	var fullPath = new ArrayList<Path>();
    	
    	var index = optOrder.get(0); //index of first node
    	var index2 = optOrder.get(1); //index of second node
    	var startPoint = Point.fromLngLat(sensorNodeList.get(index).getLng(), sensorNodeList.get(index).getLat()); //first node coordinates
    	var aimedEndPoint = Point.fromLngLat(sensorNodeList.get(index2).getLng(), sensorNodeList.get(index2).getLat()); //second node coordinates
    	fullPath.add(new Path(startPoint, aimedEndPoint, sensorNodeList.get(index2).getLocation(), buildingCoordinates)); //generates a path between those coordinates
    	
    	for(int i = 1; i < optOrder.size(); i++) {
    		
    		index = optOrder.get((i + 1) % optOrder.size()); //gets the index of the next node
    		
        	startPoint = fullPath.get(i - 1).getActualEndPoint(); //uses the endpoint of the last path as the start point of the new one
        	aimedEndPoint = Point.fromLngLat(sensorNodeList.get(index).getLng(), sensorNodeList.get(index).getLat());  //gets next node location to aim for
        	
    		fullPath.add(new Path(startPoint, aimedEndPoint, sensorNodeList.get(index).getLocation(), buildingCoordinates)); //generates Path between the two points and adds it to the list
    	}
    	   	
    	return fullPath; //return list of Paths
    }
    
    /*
     * Takes in list of SensorNode objects
     * returns all Point features from the SensorNode objects, built according to the information in them
     */
    private static List<Feature> getPointFeatures(List<SensorNode> sensorNodeList) { 
    	var features = new ArrayList<Feature>();
    	
    	for(SensorNode node : sensorNodeList) {
    		Feature feature; //creates Feature
    		
    		var point = Point.fromLngLat(node.getLng(), node.getLat()); //gets point from node
    		feature = Feature.fromGeometry(point); //turns point into geometry
    		
    		if(node.getSymbol() != "no symbol") feature.addStringProperty("marker-symbol", node.getSymbol());//adds symbol
    		feature.addStringProperty("rgb-string", node.getColor());//adds color
    		feature.addStringProperty("marker-color", node.getColor());//adds color again
    		
    		features.add(feature);//adds feature to list
    	}
    	return features;//returns list of features
    }
    
    
}
