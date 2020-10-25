package uk.ac.ed.inf.aqmaps;
import java.net.http.*;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.*;

import com.mapbox.geojson.*;

import com.google.gson.*;
import com.google.gson.reflect.*;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;

public class App 
{
    public static void main( String[] args )
    {
    	final String day = args[0];
    	final String month = args[1];
    	final String year = args[2];
    	final String port = args[6];
    	
    	
    	var sensorNodeList = getSensorList(day,month,year,port);

    	for(SensorNode node : sensorNodeList) {
    		var tempWords = node.getLocation().split("\\.");
    		var response = getDataFromURI("http://localhost:" + port + "/words/" + tempWords[0] + "/" + tempWords[1] + "/" + tempWords[2] + "/details.json");
    		var tempLocation = new Gson().fromJson(response.body(), Location.class);
    		node.setLngLat(tempLocation.getLng(), tempLocation.getLat());
    	}
    	
    	
    	
    	List<Feature> features = getPointFeatures(sensorNodeList);
    	features.add(getLineFeatures(sensorNodeList));
    	System.out.println(FeatureCollection.fromFeatures(features).toJson());  
    	
    	
    }
    
    private static Feature getLineFeatures(ArrayList<SensorNode> sensorNodeList) {
    	
    	RouteFinder finder = new RouteFinder(sensorNodeList);
    	var optOrder = finder.tspInsertion();
    	ArrayList<Path> fullPath = new ArrayList<Path>();
    	
    	
    	int index = optOrder.get(0);
    	int index2 = optOrder.get(1);
    	Point startPoint = Point.fromLngLat(sensorNodeList.get(index).getLng(), sensorNodeList.get(index).getLat());
    	Point aimedEndPoint = Point.fromLngLat(sensorNodeList.get(index2).getLng(), sensorNodeList.get(index2).getLat());
    	fullPath.add(new Path(startPoint, aimedEndPoint));
    	
    	for(int i = 1; i < optOrder.size(); i++) {
    		
    		index = optOrder.get((i + 1) % optOrder.size());
    		
        	startPoint = fullPath.get(i - 1).actualEndLocation;
        	aimedEndPoint = Point.fromLngLat(sensorNodeList.get(index).getLng(), sensorNodeList.get(index).getLat());
        	
    		fullPath.add(new Path(startPoint, aimedEndPoint));
    	}
    	
    	ArrayList<Point> pointList = new ArrayList<Point>();
    	
    	for(Path path : fullPath) {
    		for(Instruction inst : path.instructions) {
    			pointList.add(inst.getPreMove());
    			pointList.add(inst.getPostMove());
    		}
    	}
    	
    	return Feature.fromGeometry(LineString.fromLngLats(pointList));
    }
    
    private static List<Feature> getPointFeatures(List<SensorNode> sensorNodeList) {
    	List<Feature> features = new ArrayList<Feature>();
    	
    	for(SensorNode node : sensorNodeList) {
    		Feature feature;
    		Point point = Point.fromLngLat(node.getLng(), node.getLat());
    		feature = Feature.fromGeometry(point);
    		feature.addStringProperty("rgb-string", node.getColor());
    		feature.addStringProperty("marker-symbol", node.getSymbol());
    		feature.addStringProperty("marker-color", node.getColor());
    		features.add(feature);
    	}
    	return features;   
    }
    
    private static ArrayList<SensorNode> getSensorList(String day, String month, String year, String port)
    {
    	var response = getDataFromURI("http://localhost:" + port + "/maps/" + year + "/" + month + "/" + day + "/air-quality-data.json");
    	// The response object is of class HttpResponse<String>
    	Type listType = new TypeToken<ArrayList<SensorNode>>() {}.getType();
    	// Use the ”fromJson(String, Type)” method
    	return new Gson().fromJson(response.body(), listType);
    }
    
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	return null;
    }
}
