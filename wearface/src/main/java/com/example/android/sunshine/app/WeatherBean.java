package com.example.android.sunshine.app;

import java.io.Serializable;

public class WeatherBean implements Serializable {

	final int weatherId;
	final String description;
	final String maxTemp;
	final String minTemp;

	public WeatherBean(int weatherId, String description, String maxTemp, String minTemp) {
		this.weatherId = weatherId;
		this.description = description;
		this.maxTemp = maxTemp;
		this.minTemp = minTemp;
	}
}
