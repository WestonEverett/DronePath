package uk.ac.ed.inf.aqmaps;

public class Location {
	private String country;
	private String words;
	
	private Coordinates coordinates;
	private static class Coordinates {
		double lng;
		double lat;
	}
	
	public double getLng() {
		return coordinates.lng;
	}
	
	public double getLat() {
		return coordinates.lat;
	}
	
}
