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

	final static double minLat = 55.942617;
	final static double maxLat = 55.946233;
	final static double minLon = -3.192473;
	final static double maxLon = -3.184319;
	
    public static void main( String[] args )
    {
    	final String day = args[0];
    	final String month = args[1];
    	final String year = args[2];
    	final double startLat = Double.parseDouble(args[3]);
    	final double startLng = Double.parseDouble(args[4]);
    	final int seed = Integer.parseInt(args[5]);
    	final String port = args[6];
    	
    	var sensorNodeList = getSensorList(day,month,year,port);
    	
    	var startNode = new SensorNode(null, 0, null, "#ffffff", "cross", startLng, startLat);
    	sensorNodeList.add(0, startNode);

    	for(SensorNode node : sensorNodeList) {
    		if(node.getLocation() != null) {
	    		var tempWords = node.getLocation().split("\\.");
	    		var response = getDataFromURI("http://localhost:" + port + "/words/" + tempWords[0] + "/" + tempWords[1] + "/" + tempWords[2] + "/details.json");
	    		var tempLocation = new Gson().fromJson(response.body(), Location.class);
	    		node.setLngLat(tempLocation.getLng(), tempLocation.getLat());
    		}
    	}
    	
    	var response = getDataFromURI("http://localhost:" + port + "/buildings/no-fly-zones.geojson");
    	var buildingCollection = FeatureCollection.fromJson(response.body());
    	var buildingFeatures = buildingCollection.features();
    	
    	List<Point> boxVertices = Arrays.asList(Point.fromLngLat(minLon, minLat), Point.fromLngLat(maxLon, minLat), Point.fromLngLat(maxLon, maxLat), Point.fromLngLat(minLon, maxLat), Point.fromLngLat(minLon, minLat));
		List<List<Point>> boxVerticesList = Arrays.asList(boxVertices);
		
		//Creates a feature from the box and adds fill/color, then adds that feature to a feature list
		Feature outerBox = Feature.fromGeometry(Polygon.fromLngLats(boxVerticesList));
		outerBox.addNumberProperty("fill-opacity", 0);
		buildingFeatures.add(outerBox);
		
    	var buildingCoordinates = new ArrayList<double[][]>();
    	
    	for(Feature feat : buildingFeatures) {
    		buildingCoordinates.add(new Gson().fromJson(feat.toJson(), Geometry.class).geometry.coordinates[0]);
    	}
    	
    	var fullPath = getFullPath(sensorNodeList, buildingCoordinates);
    	
    	var pointList = new ArrayList<Point>();
    	
    	int counter = 1;
    	
    	String concat = "";
    	
    	for(Path path : fullPath) {
    		for(Instruction instruct : path.instructions) {
    			concat = concat + counter + "," + instruct.toString() + "\n";
    			counter++;
    		}
    		for(Instruction inst : path.instructions) {
    			pointList.add(inst.getPreMove());
    			pointList.add(inst.getPostMove());
    		}
    	}
    	saveFile(concat,"flightpath-" + day + "-" + month + "-" + year + ".txt");
    	
    	var features = getPointFeatures(sensorNodeList);
    	features.addAll(buildingFeatures);
    	features.add(Feature.fromGeometry(LineString.fromLngLats(pointList)));
    	var jsonStr = FeatureCollection.fromFeatures(features).toJson();
    	saveFile(jsonStr,"readings-" + day + "-" + month + "-" + year + ".geojson");
    	System.out.println("Done");
    	
    }
    
    class Geometry {
        GeometryData geometry;
    }   
    class GeometryData {
        String type;
        double[][][] coordinates;
    }
    
    private static void saveFile(String fileStr, String filename) {
    	
    	try {
             FileWriter myWriter = new FileWriter(filename);
             myWriter.write(fileStr);
             myWriter.close();
        }
    	catch (IOException e) {
             System.out.println("An error occurred.");
             e.printStackTrace();
        }   
    }
    
    private static ArrayList<Path> getFullPath(ArrayList<SensorNode> sensorNodeList, ArrayList<double[][]> buildingCoordinates) {
    	
    	RouteFinder finder = new RouteFinder(sensorNodeList);
    	var optOrder = finder.tspInsertion();
    	ArrayList<Path> fullPath = new ArrayList<Path>();
    	
    	
    	int index = optOrder.get(0);
    	int index2 = optOrder.get(1);
    	Point startPoint = Point.fromLngLat(sensorNodeList.get(index).getLng(), sensorNodeList.get(index).getLat());
    	Point aimedEndPoint = Point.fromLngLat(sensorNodeList.get(index2).getLng(), sensorNodeList.get(index2).getLat());
    	fullPath.add(new Path(startPoint, aimedEndPoint, sensorNodeList.get(index2).getLocation(), buildingCoordinates));
    	
    	for(int i = 1; i < optOrder.size(); i++) {
    		
    		index = optOrder.get((i + 1) % optOrder.size());
    		
        	startPoint = fullPath.get(i - 1).actualEndLocation;
        	aimedEndPoint = Point.fromLngLat(sensorNodeList.get(index).getLng(), sensorNodeList.get(index).getLat());
        	
    		fullPath.add(new Path(startPoint, aimedEndPoint, sensorNodeList.get(index).getLocation(), buildingCoordinates));
    	}
    	   	
    	return fullPath;
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
