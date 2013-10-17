package com.cosso.carassistant;

/**
 * @author Rok Pajk Kosec
 * Class used for representation of node in map data for map matching algorithm
 */
public class Node {
	private double latitude;
	private double longitude;
	private long id;
	
	/**
	 * Empty constructor
	 */
	public Node(){
		latitude = 0;
		longitude = 0;
		id = -1;
	}
	
	/**
	 * @param _lat
	 * @param _long
	 * @param _id
	 * Constructor that sets all attributes
	 */
	public Node(double _lat, double _long, long _id){
		latitude = _lat;
		longitude = _long;
		id = _id;
	}
	
	public void setLatitude(double _lat){
		latitude = _lat;
	}
	
	public void setLongitude(double _long){
		longitude = _long;
	}
	
	public void setId(long _id){
		id = _id;
	}
	
	public double getLatitude(){
		return latitude;
	}
	
	public double getLongitude(){
		return longitude;
	}
	
	public long getId(){
		return id;
	}
}
