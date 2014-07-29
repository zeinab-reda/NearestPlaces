package com.example.nearbyplaces;

import android.location.Location;

public class FsqVenue {
	public String id;
	public String name;
	public String address;
	public String type;
	public Location location;
	public int direction;
	public int distance;
	public int herenow;
	public String imgURL;
	public String imgPath;

	public void setImgPath(String imgPath) {
		this.imgPath = imgPath;
	}

	public FsqVenue() {

	}

	public FsqVenue(String id, String name, String imgURL, String log,
			String lat) {
		this.id = id;
		this.name = name;
		this.imgURL = imgURL;
		this.location.setLongitude(Double.valueOf(log));
		this.location.setLatitude(Double.valueOf(lat));
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public void setType(String type) {
		this.type = type;
	}

	public void setLocation(Location location) {
		this.location = location;
	}

	public void setDirection(int direction) {
		this.direction = direction;
	}

	public void setDistance(int distance) {
		this.distance = distance;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getAddress() {
		return address;
	}

	public String getType() {
		return type;
	}

	public Location getLocation() {
		return location;
	}

	public String getImgPath() {
		return imgPath;
	}

	public int getDirection() {
		return direction;
	}

	public int getDistance() {
		return distance;
	}

	public int getHerenow() {
		return herenow;
	}

	public String getImgURL() {
		return imgURL;
	}

	public void setHerenow(int herenow) {
		this.herenow = herenow;
	}

	public void setImgURL(String imgURL) {
		this.imgURL = imgURL;
	}

}