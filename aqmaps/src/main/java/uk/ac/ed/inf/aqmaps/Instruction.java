package uk.ac.ed.inf.aqmaps;

import com.mapbox.geojson.Point;

public class Instruction {
	private Point preMove;
	private int angle;
	private Point postMove;
	private String sensorLocation;
	
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
	
	private int getAngle() {
		double angle = Math.atan2(postMove.latitude() - preMove.latitude(),postMove.longitude() - preMove.longitude());
		return (int) Math.round((angle * (180 / Math.PI)) / 10.0) * 10;
	}
	
	public String toString() {
		return preMove.longitude() + "," + preMove.latitude() + " " + angle + " " + postMove.longitude() + "," + postMove.latitude();
	}
}
