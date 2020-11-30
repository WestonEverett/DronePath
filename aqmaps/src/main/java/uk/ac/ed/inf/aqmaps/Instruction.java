package uk.ac.ed.inf.aqmaps;

import com.mapbox.geojson.Point;

/*
 * Class for holding the data on individual moves of the drone
 */
public class Instruction {
	private Point preMove; //Point before move
	private int angle; //angle move was in
	private Point postMove; //Point after move
	private String sensorLocation; //W3W String if the move comes close to a sensor, usually null
	
	public Instruction(Point preMove, Point postMove, String sensorLocation) {
		this.preMove = preMove;
		this.postMove = postMove;
		this.sensorLocation = sensorLocation;
		this.angle = getAngle();
	}
	
	public Point getPreMove() {
		return preMove;
	}
	
	public Point getPostMove() {
		return postMove;
	}
	
	public String getSensorLocation() {
		return sensorLocation;
	}
	
	/*
	 * Calculates angle from points provided, rounds to nearest 10 degrees
	 */
	private int getAngle() {
		double angle = Math.atan2(postMove.latitude() - preMove.latitude(),postMove.longitude() - preMove.longitude()); //calculates angle
		int processed = (int) (Math.round((angle * (180 / Math.PI)) / 10.0) * 10); //changes angle from degrees to radians, then rounds to nearest 10
		if(processed < 0) return processed + 360; //shifts angle so it is between 0 and 360 rather than -180 and 180
		else return processed;
	}
	
	/*
	 * Puts Instruction into format for easy printing/saving
	 */
	public String toString() {
		return preMove.longitude() + "," + preMove.latitude() + "," + angle + "," + postMove.longitude() + "," + postMove.latitude() + "," + sensorLocation;
	}
}
