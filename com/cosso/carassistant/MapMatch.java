package com.cosso.carassistant;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * @author Rok Pajk Kosec
 * Implementation of topological map matching algorithm.
 */
public class MapMatch {
    private static DatabaseHandler dbHandler;
    private static SQLiteDatabase dbConnection;
    private static int counter = 0;
    //non negative weighting factors
    private double a;
    private double b;
    //perpendicular weighting score
    private double Apd;
    //heading weighting score
    private double Ah;
    //relative postition weighting score
    private double Arp;
    
    //nodes
    private Node node1;
    private Node node2;
    private Node nodeFix;
    private Node nodeMatched;
    
    //matching algorithm variables
    private double headingFix;
    private double currentEdgeHeading;
    private double lastDistanceToEdge;
    private long currentWay;
    private double maxTWS;
    private String currentWayName;
    private String currentWayRef;
    private String currentWayType;
    private int currentSpeedLimit;
		
	public MapMatch(){
		init(4, 2, 10);
	}
		
	private void init(double _a, double _b, double _Apd){
		a = _a;
		b = _b;
		Apd = _Apd;
		Ah = a * Apd;
		Arp = b * Apd;
		node1 = new Node();
		node2 = new Node();
		nodeFix = new Node();
		nodeMatched = new Node();
	}
	
	public void disconnect(){
		dbConnection.close();
		dbHandler.close();
	}
	
	public void reconnect(Context context){
    	dbHandler = new DatabaseHandler(context);
		dbConnection = dbHandler.getReadableDatabase();
	}
	
	private static double square(double expression){
		return expression * expression;
	}
	
	/**
	 * Gets way details from database and sets variables accordingly
	 */
	private void setWayDetails(){
		int wayTypeId = 16;
		
		Cursor cursor = dbConnection.rawQuery("SELECT wayName, wayRef, wayTypeId, wayMaxSpeed FROM way WHERE wayId=" + String.valueOf(currentWay), null);
		if(cursor.moveToFirst()){
			currentWayName = cursor.getString(0);
			currentWayRef = cursor.getString(1);
			currentSpeedLimit = cursor.getInt(3);
			wayTypeId = cursor.getInt(2);
		}
		
		cursor = dbConnection.rawQuery("SELECT wayTypeName, wayTypeDefSpeed FROM wayType WHERE wayTypeId=" + String.valueOf(wayTypeId), null);
		if(cursor.moveToFirst()){
			currentWayType = cursor.getString(0);
			if(currentSpeedLimit == 0){
				currentSpeedLimit = cursor.getInt(1);
			}
		}
		
	}
	
	/**
	 * @param latitude
	 * @param longitude
	 * @param heading
	 * @param location
	 * @return matched latitude and longitude
	 * Main matching method. Does map matching based on current latitude, longitude, heading
	 */
	public double[] match(double latitude, double longitude, double heading, double[] location){
		nodeFix.setLatitude(latitude);
		nodeFix.setLongitude(longitude);
		
		if(stillOnEdge(heading) && counter > 0){
			headingFix = heading;
			getLocationOnEdge();
		}
		else{
			headingFix = heading;
			matchToBestWay(closestNode());
			getLocationOnEdge();
			setWayDetails();
		}
		if(lastDistanceToEdge > 500){
			currentWayName = "Off road";
			currentWayRef = "Off road";
			currentWayType = "Off road";
			currentSpeedLimit = 0;
			location[0] = latitude;
			location[1] = longitude;
		}
		else{
			location[0] = nodeMatched.getLatitude();
			location[1] = nodeMatched.getLongitude();
		}

		counter++;
		
		return location;
	}
	
	/**
	 * @param newHeadingFix
	 * @return true if fix is still on edge
	 * Checks if current position is still on the same edge as it was at previous position
	 */
	private boolean stillOnEdge(double newHeadingFix){
		
		if(Math.cos(Math.toRadians(newHeadingFix - headingFix)) < 0.8660){
			return false;
		}
		if(Math.cos(Math.toRadians(GeoCalculator.initialBearing(nodeFix.getLatitude(), nodeFix.getLongitude(), node2.getLatitude(), node2.getLongitude()) - currentEdgeHeading)) <= 0){
			return false;
		}
		if(Math.abs((nodeFix.getLongitude() * (node1.getLatitude() - node2.getLatitude()) - nodeFix.getLatitude() * (node1.getLongitude() - node2.getLongitude()) + 
				(node1.getLongitude() * node2.getLatitude() - node2.getLongitude() * node1.getLatitude()))/
				(Math.sqrt(square(node1.getLongitude() - node2.getLongitude()) + square(node1.getLatitude() - node2.getLatitude()))))*100000 > 15){
			return false;
		}
		return true;
	}
	
	/**
	 * Gets position along found edge
	 */
	private void getLocationOnEdge(){	
		
		double temp1 = nodeFix.getLongitude() * (node2.getLongitude() - node1.getLongitude()) + nodeFix.getLatitude() * (node2.getLatitude() - node1.getLatitude());
		double temp2 = (node1.getLongitude() * node2.getLatitude()) - (node2.getLongitude() * node1.getLatitude());
		double temp3 = square(node2.getLongitude() - node1.getLongitude()) + square(node2.getLatitude() - node1.getLatitude());
		
		nodeMatched.setLatitude(((node2.getLatitude() - node1.getLatitude()) * temp1 - (node2.getLongitude() - node1.getLongitude()) * temp2)/temp3);
		
		nodeMatched.setLongitude(((node2.getLongitude() - node1.getLongitude()) * temp1 + (node2.getLatitude() - node1.getLatitude()) * temp2)/temp3);
	}
	
	/**
	 * @param node
	 * If no other information is already set (at the beginning of matching), find best way to match current position to
	 */
	private void matchToBestWay(Node node){
		maxTWS = -1000;
		if(node2.getId() != -1){
			matchToWay(new Node(node2.getLatitude(), node2.getLongitude(), node2.getId()));
		}
		if(node.getId() != -1){
			matchToWay(node);
		}
	}
	
	/**
	 * @param node
	 * Does actual map matching using current location fix in Node class form
	 */
	private void matchToWay(Node node){
		long nodeId2;
				
		double edgeHeading;
		double connectionHeading;		

		double lat2temp = 0;
		double long2temp = 0;
		
		//perpendicular distance scale between 0 and 1
		double w;
		
		//weights that are later used for best match now initialized to lower than minimum values
		double headingW = -1;
		double proximityW = -1;
		double relativeW = 0;
		double tempTWS = -1000;
		
		double twsFactor = 1;
		double deltaBeta;
		double d;
		
		Cursor cursor;
		Cursor cursor1;
		
		//get heading of connection between last and current position fix
		connectionHeading = GeoCalculator.initialBearing(node.getLatitude(), node.getLongitude(), nodeFix.getLatitude(), nodeFix.getLongitude());

//START NODE----------------------------------------------------------------------------------------------------------------------------------------------
		
		//get all the edges going out of current node and all end node ids
		cursor = dbConnection.rawQuery("SELECT edgeId, edgeHeading, endId, wayId FROM edge WHERE startId=" + String.valueOf(node.getId()), null);
		
		while(cursor.moveToNext()){
			
			nodeId2 = cursor.getLong(2);
			edgeHeading = cursor.getDouble(1);
			
			//get node latitude and longitude at end of observing edge
			cursor1 = dbConnection.rawQuery("SELECT nodeLat, nodeLong FROM node WHERE nodeId=" + String.valueOf(nodeId2), null);
			
			if(cursor1.moveToFirst()){
				lat2temp = cursor1.getDouble(0);
				long2temp = cursor1.getDouble(1);
			}
			
			//difference between fix heading and edge heading
			deltaBeta = edgeHeading-headingFix;
			//heading weight
			headingW = Ah * Math.cos(Math.toRadians(deltaBeta));
			
			/*
			 * if angle between fix, start point of edge and end point of edge is more than 90 degrees, distance between location fix
			 * and edge is distance between location fix and start point of edge
			 */
			if(Math.cos(Math.toRadians(GeoCalculator.initialBearing(node.getLatitude(), node.getLongitude(), nodeFix.getLatitude(), nodeFix.getLongitude()) - 
					edgeHeading)) <= 0){
				d = GeoCalculator.haversine(node.getLatitude(), node.getLongitude(), nodeFix.getLatitude(), nodeFix.getLongitude());
				twsFactor = 0.9;
			}
			/*
			 * if angle between fix, end point of edge and start point of edge is mode than 90 degrees, distance between location fix
			 * and edge is distance between location fix and end point of edge
			 */
			else if(Math.cos(Math.toRadians(GeoCalculator.initialBearing(lat2temp, long2temp, nodeFix.getLatitude(), nodeFix.getLongitude()) - 
					(edgeHeading+180))%360) <= 0){
				d = GeoCalculator.haversine(lat2temp, long2temp, nodeFix.getLatitude(), nodeFix.getLongitude());
				twsFactor = 0.9;
			}
			/*
			 * If above conditions are not met, than distance between location fix and edge is perpendicular distance between them
			 */
			else{
				d = Math.abs((nodeFix.getLongitude() * (node.getLatitude() - lat2temp) - nodeFix.getLatitude() * (node.getLongitude() - long2temp) + 
						(node.getLongitude() * lat2temp - long2temp * node.getLatitude()))/(Math.sqrt(square(node.getLongitude() - long2temp) + square(node.getLatitude() - lat2temp))))*100000;
				twsFactor = 1;
			}

			//if distance from edge to location fix is less than 5 meters, distance scale factor is 1, ...
			if(d < 5){
				w = 1;
			}
			else if(5 <= d && d <= 100){
				w = 1.0 - (0.01 * d);
			}
			else{
				w = -1;
			}
			
			//proximity weight
			proximityW = Apd * w;
			
			//relative position weight
			relativeW = Arp * Math.cos(Math.toRadians(edgeHeading - connectionHeading));
			
			//calculation of total weighting score for this edge
			tempTWS = headingW + proximityW + relativeW;
			tempTWS = tempTWS * twsFactor;
			
			//if it is greater than previous max, assign this score as max
			if(tempTWS > maxTWS){
				maxTWS = tempTWS;
				node1.setId(node.getId());
				node2.setId(nodeId2);
				currentWay = cursor.getInt(3);
				node1.setLatitude(node.getLatitude());
				node1.setLongitude(node.getLongitude());
				node2.setLatitude(lat2temp);
				node2.setLongitude(long2temp);
				currentEdgeHeading = edgeHeading;
				lastDistanceToEdge = d;
			}
		}
		
//END NODE----------------------------------------------------------------------------------------------------------------------------------------------
		
		//get all the edges going in to current node and all end node ids
		cursor = dbConnection.rawQuery("SELECT edgeId, edgeHeading, startId, wayId FROM edge WHERE endId=" + String.valueOf(node.getId()), null);
		
		while(cursor.moveToNext()){
			
			nodeId2 = cursor.getLong(2);
			edgeHeading = cursor.getDouble(1);
			
			//get node latitude and longitude at start of observing edge
			cursor1 = dbConnection.rawQuery("SELECT nodeLat, nodeLong FROM node WHERE nodeId=" + String.valueOf(nodeId2), null);
			
			if(cursor1.moveToFirst()){
				lat2temp = cursor1.getDouble(0);
				long2temp = cursor1.getDouble(1);
			}
			
			//correction due to opposite way of traveling relative to edge
			edgeHeading = (edgeHeading+180)%360;
			
			//check if edge we found is one way. In this case ignore this edge
			cursor1 = dbConnection.rawQuery("SELECT wayOneWay FROM way WHERE wayId=" + String.valueOf(cursor.getInt(3)), null);
			
			if(cursor1.moveToFirst()){
				if(cursor1.getShort(0) == 1){
					continue;
				}	
			}
			
			//difference between fix heading and edge heading
			deltaBeta = edgeHeading-headingFix;
			//heading weight
			headingW = Ah * Math.cos(Math.toRadians(deltaBeta));
			
			/*
			 * if angle between fix, start point of edge and end point of edge is more than 90 degrees, distance between location fix
			 * and edge is distance between location fix and start point of edge
			 */
			if(Math.cos(Math.toRadians(GeoCalculator.initialBearing(node.getLatitude(), node.getLongitude(), nodeFix.getLatitude(), nodeFix.getLongitude()) - 
					edgeHeading)) <= 0){
				d = GeoCalculator.haversine(node.getLatitude(), node.getLongitude(), nodeFix.getLatitude(), nodeFix.getLongitude());
				twsFactor = 0.9;
			}
			/*
			 * if angle between fix, end point of edge and start point of edge is mode than 90 degrees, distance between location fix
			 * and edge is distance between location fix and end point of edge
			 */
			else if(Math.cos(Math.toRadians(GeoCalculator.initialBearing(lat2temp, long2temp, nodeFix.getLatitude(), nodeFix.getLongitude()) - 
					(edgeHeading+180))%360) <= 0){
				d = GeoCalculator.haversine(lat2temp, long2temp, nodeFix.getLatitude(), nodeFix.getLongitude());
				twsFactor = 0.9;
			}
			/*
			 * If above conditions are not met, than distance between location fix and edge is perpendicular distance between them
			 */
			else{
				d = Math.abs((nodeFix.getLongitude() * (node.getLatitude() - lat2temp) - nodeFix.getLatitude() * (node.getLongitude() - long2temp) + 
						(node.getLongitude() * lat2temp - long2temp * node.getLatitude()))/(Math.sqrt(square(node.getLongitude() - long2temp) + square(node.getLatitude() - lat2temp))))*100000;
				twsFactor = 1;
			}
			
			//if distance from edge to location fix is less than 5 meters, distance scale factor is 1, ...
			if(d < 5){
				w = 1;
			}
			else if(5 <= d && d <= 100){
				w = 1.0 - (0.01 * d);
			}
			else{
				w = -1;
			}
			
			//proximity weight
			proximityW = Apd * w;
			
			//relative position weight
			relativeW = Arp * Math.cos(Math.toRadians(edgeHeading - connectionHeading));
			
			//calculation of total weighting score for this edge
			tempTWS = headingW + proximityW + relativeW;
			tempTWS = tempTWS * twsFactor;
			
			//if it is greater than previous max, assign this score as max
			if(tempTWS > maxTWS){
				maxTWS = tempTWS;
				node1.setId(node.getId());
				node2.setId(nodeId2);
				currentWay = cursor.getInt(3);
				node1.setLatitude(node.getLatitude());
				node1.setLongitude(node.getLongitude());
				node2.setLatitude(lat2temp);
				node2.setLongitude(long2temp);
				currentEdgeHeading = edgeHeading;
				lastDistanceToEdge = d;
			}
		}
	}
	
	/**
	 * @return closest node to current position fix
	 * Gets closest node from database. Search is due to speed optimization limited to 500m radius
	 */
	private Node closestNode(){
		long id = -1;
		double minDistance = 100000000; //100000 km which is more than equator
		double distance = 0;
		double latitudeMap;
		double longitudeMap;
		Node returnNode = new Node();
		Cursor cursor;
		
		cursor = dbConnection.rawQuery("SELECT nodeId, nodeLat, nodeLong FROM node WHERE nodeLat<=" + String.valueOf(nodeFix.getLatitude()+0.0005) + " AND nodeLat>=" +
				String.valueOf(nodeFix.getLatitude()-0.0005) + " AND nodeLong<=" + String.valueOf(nodeFix.getLongitude()+(0.0005*90/nodeFix.getLatitude())) +
				" AND nodeLong>=" + String.valueOf(nodeFix.getLongitude()-(0.0005*90/nodeFix.getLatitude())), null);
		
		while(cursor.moveToNext()){
			latitudeMap = cursor.getDouble(1);
			longitudeMap = cursor.getDouble(2);
			if((distance = GeoCalculator.haversine(nodeFix.getLatitude(), nodeFix.getLongitude(), latitudeMap, longitudeMap)) < minDistance){
				id = cursor.getLong(0);
				minDistance = distance;
				returnNode.setId(id);
				returnNode.setLatitude(latitudeMap);
				returnNode.setLongitude(longitudeMap);
			}
		}
		return returnNode;
	}
	
	public String getCurrentWayName(){
		return currentWayName;
	}
	
	public String getCurrentWayRef(){
		return currentWayRef;
	}
	
	public String getCurrentWayType(){
		return currentWayType;
	}
	
	public long getCurrentWayId(){
		return currentWay;
	}
	
	public int getSpeedLimit(){
		return currentSpeedLimit;
	}
}
