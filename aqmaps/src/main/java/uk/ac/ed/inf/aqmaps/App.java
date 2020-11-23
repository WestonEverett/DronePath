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
	final static double minLat = 55.942617;
	final static double maxLat = 55.946233;
	final static double minLon = -3.192473;
	final static double maxLon = -3.184319;
	
    public static void main( String[] args )
    {
    	final String day = args[0]; //DD
    	final String month = args[1]; //MM
    	final String year = args[2]; //YYYY
    	final double startLat = Double.parseDouble(args[3]);  //Drone start position
    	final double startLng = Double.parseDouble(args[4]);
    	final int seed = Integer.parseInt(args[5]); //seed for randomness (unused)
    	final String port = args[6]; //Server port, usually 80
    	
    	var sensorNodeList = getSensorList(day,month,year,port); //gets list of SensorNode objects and gets their coordinates from the WebServer
    	sensorNodeList.add(0, new SensorNode(null, 0, null, "#ffffff", "cross", startLng, startLat)); //adds the starting position, marked by a white x
   	
    	var buildingFeatures = getBuildingFeatures(port); //reads the buildings from WebServer into a feature list and adds the drone containment area
		
    	var buildingCoordinates = new ArrayList<double[][]>(); //ArrayList of all the vertices sets of the buildings
    	
    	for(Feature feat : buildingFeatures) {
    		buildingCoordinates.add(new Gson().fromJson(feat.toJson(), Geometry.class).geometry.coordinates[0]); //adds the double[][] vertices coordinates from each building feature
    	}
    	
    	var fullPath = getFullPath(sensorNodeList, buildingCoordinates); //Gets the List of Path objects each holding a list of Instruction objects with the drones moves between nodes
    	
    	var pointList = new ArrayList<Point>(); //List of points for a json file in order to display the moves
    	
    	int counter = 1; //which number instruction in flightpath file
    	
    	String concat = ""; //String to be saved into flightpath file
    	
    	pointList.add(Point.fromLngLat(startLng, startLat)); //adds starting point
    	for(Path path : fullPath) { //for each Path (moves to get from one SensorNode to another) in fullPath
    		for(Instruction inst : path.instructions) { //for each of the individual instructions in the path
    			pointList.add(inst.getPostMove()); //adds end of each move to pointList
    			
    			concat = concat + counter + "," + inst.toString() + "\n"; //adds the instruction to the string with the number instruction it is
    			counter++;
    		}
    	}
    	saveFile(concat,"flightpath-" + day + "-" + month + "-" + year + ".txt"); //saves the prepared string as flightpath-DD-MM-YYYY.txt
    	
    	var features = getPointFeatures(sensorNodeList); //gets all the points from SensorNodeList
    	features.addAll(buildingFeatures); //adds all the buildings
    	features.add(Feature.fromGeometry(LineString.fromLngLats(pointList))); //adds the drone containment area
    	var jsonStr = FeatureCollection.fromFeatures(features).toJson(); //converts the FeatureCollection to a Json String
    	saveFile(jsonStr,"readings-" + day + "-" + month + "-" + year + ".geojson"); //saves the Json String as readings-DD-MM-YYYY.geojson
    	System.out.println("Done");
    	
    }
    
    class Geometry { //class for parsing building geometry jsons using Gson
        GeometryData geometry; //holds coordinates
    }   
    class GeometryData { //class for parsing building geometry jsons using Gson
        String type; //unused
        double[][][] coordinates; //List of building vertices in coordinates[0]
    }
    
    /*
     * takes in a str to save and a filename
     * saves the file 
     */
    private static void saveFile(String fileStr, String filename) {
    	
    	try {
             FileWriter myWriter = new FileWriter(filename); //opens file
             myWriter.write(fileStr); //writes to file
             myWriter.close(); //closes file
        }
    	catch (IOException e) {
             System.out.println("An error occurred.");
             e.printStackTrace();
        }   
    }
    
    /*
     * takes in the port where the server is
     * returns all the building features
     */
    private static List<Feature> getBuildingFeatures(String port) {
    	
    	//gets all buildings from server
    	var response = getDataFromURI("http://localhost:" + port + "/buildings/no-fly-zones.geojson");
    	var buildingCollection = FeatureCollection.fromJson(response.body());
    	var buildingFeatures = buildingCollection.features();
    	
    	//creates the drone containement area feature
    	List<Point> boxVertices = Arrays.asList(Point.fromLngLat(minLon, minLat), Point.fromLngLat(maxLon, minLat), Point.fromLngLat(maxLon, maxLat), Point.fromLngLat(minLon, maxLat), Point.fromLngLat(minLon, minLat));
		List<List<Point>> boxVerticesList = Arrays.asList(boxVertices);
		Feature outerBox = Feature.fromGeometry(Polygon.fromLngLats(boxVerticesList));
		outerBox.addNumberProperty("fill-opacity", 0);
		
		//adds drone containment area to building features
		buildingFeatures.add(outerBox);
		
		return buildingFeatures;
    }
    
    /*
     * Takes in the list of SensorNodes and the list of buildings
     * outputs a list of Path objects that visits every node and returns to the start point
     */
    private static ArrayList<Path> getFullPath(ArrayList<SensorNode> sensorNodeList, ArrayList<double[][]> buildingCoordinates) {
    	
    	var finder = new RouteFinder(sensorNodeList); //creates a RouteFinder object, which takes the list of SensorNodes and decides what order to visit them in
    	var optOrder = finder.tspInsertion(); //runs an insertion algorithm to choose the order
    	
    	var fullPath = new ArrayList<Path>();
    	
    	var index = optOrder.get(0); //index of first node
    	var index2 = optOrder.get(1); //index of second node
    	var startPoint = Point.fromLngLat(sensorNodeList.get(index).getLng(), sensorNodeList.get(index).getLat()); //first node coordinates
    	var aimedEndPoint = Point.fromLngLat(sensorNodeList.get(index2).getLng(), sensorNodeList.get(index2).getLat()); //second node coordinates
    	fullPath.add(new Path(startPoint, aimedEndPoint, sensorNodeList.get(index2).getLocation(), buildingCoordinates)); //generates a path between those coordinates
    	
    	for(int i = 1; i < optOrder.size(); i++) {
    		
    		index = optOrder.get((i + 1) % optOrder.size()); //gets the index of the next node
    		
        	startPoint = fullPath.get(i - 1).actualEndLocation; //uses the endpoint of the last path as the start point of the new one
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
    		feature.addStringProperty("rgb-string", node.getColor());//adds color
    		feature.addStringProperty("marker-symbol", node.getSymbol());//adds symbol
    		feature.addStringProperty("marker-color", node.getColor());//adds color again
    		features.add(feature);//adds feature to list
    	}
    	return features;//returns list of features
    }
    
    /*
     * takes in the day/month/year to operate on and the port where the webserver is
     * returns a list of all the SensorNode objects with including their coordinates (instead of W3W format)
     */
    private static ArrayList<SensorNode> getSensorList(String day, String month, String year, String port)
    {
    	var response = getDataFromURI("http://localhost:" + port + "/maps/" + year + "/" + month + "/" + day + "/air-quality-data.json"); //gets SensorNode data and converts it into SensorNode objects
    	// The response object is of class HttpResponse<String>
    	Type listType = new TypeToken<ArrayList<SensorNode>>() {}.getType();
    	// Use the ”fromJson(String, Type)” method
    	ArrayList<SensorNode> sensorNodeList = new Gson().fromJson(response.body(), listType);
    	
    	for(SensorNode node : sensorNodeList) { //for each SensorNode created
    		if(node.getLocation() != null) { //As long as it has a What3Words component
	    		var tempWords = node.getLocation().split("\\."); //extracts the words
	    		var response2 = getDataFromURI("http://localhost:" + port + "/words/" + tempWords[0] + "/" + tempWords[1] + "/" + tempWords[2] + "/details.json"); //reads the information from the webserver
	    		var tempLocation = new Gson().fromJson(response2.body(), Location.class); //creates Location object from that information
	    		node.setLngLat(tempLocation.getLng(), tempLocation.getLat()); //pulls longitude and latitude of location and adds them to SensorNode
    		}
    	}
    	
    	return sensorNodeList;
    }
    
    /*
     * Takes a String URL
     * returns the corresponding Data
     */
    private static HttpResponse<String> getDataFromURI(String uri)
    {
    	try {
    		// Create a new HttpClient with default settings.
    		var client = HttpClient.newHttpClient();
    		// HttpClient assumes that it is a GET request by default.
    		var request = HttpRequest.newBuilder()
    				.uri(URI.create(uri))
    				.build();
			return client.send(request, BodyHandlers.ofString());
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
    	
    	return null;
    }
}
