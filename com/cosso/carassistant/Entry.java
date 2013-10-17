package com.cosso.carassistant;

/**
 * @author Rok Pajk Kosec
 * Class used for sending data to server. Data is recieved in this form via JSON
 */
public class Entry {

	//username
	private String username;
	//password
	private String userpass;
	//starting latitude of monitored segment
	private double latitude;
	//starting longitude of monitored segment
	private double longitude;
	//average heading of monitored segment
	private int heading;
	//average consumption of monitored segment
	private double consumption;
	//average speed of monitored segment
	private double speed;
	//road type of monitored segment
	private String roadType;
	//speed limit of monitored segment
	private int speedLimit;
	//way id from map data used for further data management
	private long wayId;
	//flag for signaling weather update from weather service
	private boolean weatherDataCheck;

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getUserpass() {
		return userpass;
	}

	public void setUserpass(String userpass) {
		this.userpass = userpass;
	}

	public int getHeading() {
		return heading;
	}

	public void setHeading(int heading) {
		this.heading = heading;
	}

	public int getSpeedLimit() {
		return speedLimit;
	}

	public void setSpeedLimit(int speedLimit) {
		this.speedLimit = speedLimit;
	}

	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	public double getConsumption() {
		return consumption;
	}

	public void setConsumption(double consumption) {
		this.consumption = consumption;
	}

	public double getSpeed() {
		return speed;
	}

	public void setSpeed(double speed) {
		this.speed = speed;
	}

	public String getRoadType() {
		return roadType;
	}

	public void setRoadType(String roadType) {
		this.roadType = roadType;
	}

	public long getWayId() {
		return wayId;
	}

	public void setWayId(long wayId) {
		this.wayId = wayId;
	}

	public boolean getWeatherDataCheck() {
		return weatherDataCheck;
	}

	public void setWeatherDataCheck(boolean weatherDataCheck) {
		this.weatherDataCheck = weatherDataCheck;
	}


}