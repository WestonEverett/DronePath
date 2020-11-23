/**
 * 
 */
package uk.ac.ed.inf.aqmaps;

/*
 *Class for containing Sensor-related information
 *Mainly for parsing json files, with some helper functions
 *lng and lat do not come with parse, have to be taken from W3W file
 */
public class SensorNode {
	private String location; //W3W String, used to select the proper location for lng and lat
	private double battery; //sensor current battery
	private String reading; //sensor reading
	private String color; //color to be displayed
	private String symbol; //symbol to be displayed
	//coordinates of Sensor
	private double lng;
	private double lat;
	
	public SensorNode(String location, double battery, String reading, String color, String symbol, double lng, double lat) {
		this.location = location;
		this.battery = battery;
		this.reading = reading;
		this.color = color;
		this.symbol = symbol;
		this.lng = lng;
		this.lat = lat;
	}
	
	public String getLocation() {
		return location;
	}
	
	
	public String getColor() {
		if(color == null) color = decideColor();
		return color;
	}
	
	public String getSymbol() {
		if(symbol == null) symbol = decideSymbol();
		return symbol;
	}
	
	
	public double getBattery() {
		return battery;
	}
	
	public String getReading() {
		return reading;
	}
	
	
	public double getLng() {
		return lng;
	}
	
	public double getLat() {
		return lat;
	}
	
	public void setLngLat(double lng,double lat) {
		this.lng = lng;
		this.lat = lat;
	}
	
	/*
	 * Chooses color, first ensuring reading is not null or NaN based on battery
	 */
	private String decideColor() {
		if(battery < 10)		return "#000000";
		if(reading == null)		return "#aaaaaa";
		double tempReading = Double.parseDouble(reading);
        if(tempReading <= 32)	return "#00ff00";
        if(tempReading < 64)	return "#40ff00";
        if(tempReading < 96)  	return "#80ff00";
        if(tempReading < 128)  	return "#c0ff00";
        if(tempReading < 160) 	return "#ffc000";
        if(tempReading < 192) 	return "#ff8000";
        if(tempReading < 224) 	return "#ff4000";
        if(tempReading < 256) 	return "#ff0000";
        return "#aaaaaa";
	}
	
	/*
	 * Chooses symbol, again checking in order so valid input will not throw errors
	 */
	private String decideSymbol() {
		if(battery < 10)		return "cross";
		if(reading == null)		return "no symbol";
		double tempReading = Double.parseDouble(reading);
        if(tempReading < 128)  	return "lighthouse";
        if(tempReading < 256) 	return "danger";
        return "no symbol";
	}
	
}
