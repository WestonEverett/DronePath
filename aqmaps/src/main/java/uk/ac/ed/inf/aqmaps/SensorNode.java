/**
 * 
 */
package uk.ac.ed.inf.aqmaps;

/**
 * @author westo
 *
 */
public class SensorNode {
	private String location;
	private double battery;
	private String reading;
	private String color;
	private String symbol;
	private double lng;
	private double lat;
	
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
	
	private String decideSymbol() {
		if(battery < 10)		return "cross";
		if(reading == null)		return "no symbol";
		double tempReading = Double.parseDouble(reading);
        if(tempReading < 128)  	return "lighthouse";
        if(tempReading < 256) 	return "danger";
        return "no symbol";
	}
	
}
