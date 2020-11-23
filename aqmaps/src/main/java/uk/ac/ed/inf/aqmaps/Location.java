package uk.ac.ed.inf.aqmaps;

/*
 * Class to contain data parsed from W3W Strings
 */
public class Location {
	private String country; //country, unused
	private String words; //W3W code, unused other than for testing
	
	private Coordinates coordinates;
	private static class Coordinates {
		double lng; //longitude of location
		double lat; //latitude of location
	}
	
	public double getLng() {
		return coordinates.lng;
	}
	
	public double getLat() {
		return coordinates.lat;
	}
	
}
