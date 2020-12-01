package uk.ac.ed.inf.aqmaps;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;

public class FileManager {
	
	final static double minLat = 55.942617;
	final static double maxLat = 55.946233;
	final static double minLon = -3.192473;
	final static double maxLon = -3.184319;

	/*
     * takes in the day/month/year to operate on and the port where the webserver is
     * returns a list of all the SensorNode objects with including their coordinates (instead of W3W format)
     */
    public static ArrayList<SensorNode> getSensorList(String day, String month, String year, String port)
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
     * takes in the port where the server is
     * returns all the building features
     */
    public static List<Feature> getBuildingFeatures(String port) {
    	
    	//gets all buildings from server
    	var response = getDataFromURI("http://localhost:" + port + "/buildings/no-fly-zones.geojson");
    	var buildingCollection = FeatureCollection.fromJson(response.body());
    	var buildingFeatures = buildingCollection.features();
    	
    	//creates the drone containment area feature
    	List<Point> boxVertices = Arrays.asList(Point.fromLngLat(minLon, minLat), Point.fromLngLat(maxLon, minLat), Point.fromLngLat(maxLon, maxLat), Point.fromLngLat(minLon, maxLat), Point.fromLngLat(minLon, minLat));
		List<List<Point>> boxVerticesList = Arrays.asList(boxVertices);
		Feature outerBox = Feature.fromGeometry(Polygon.fromLngLats(boxVerticesList));
		outerBox.addNumberProperty("fill-opacity", 0);
		
		//adds drone containment area to building features
		buildingFeatures.add(outerBox);
		
		return buildingFeatures;
    }
    
    /*
     * Takes a String URL
     * returns the corresponding Data
     */
    public static HttpResponse<String> getDataFromURI(String uri)
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
    
    /*
     * takes in a str to save and a filename
     * saves the file 
     */
    public static void saveFile(String fileStr, String filename) {
    	
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
}
