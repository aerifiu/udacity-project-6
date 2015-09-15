package com.example.android.sunshine.app;

import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.example.android.sunshine.app.data.WeatherContract;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

public class WearToDeviceService extends WearableListenerService implements GoogleApiClient.OnConnectionFailedListener {

	private static final String TAG = "WearToDeviceService";
	public static final String DATA_ITEM_WEATHER_PATH = "/sunshine";
	private GoogleApiClient googleApiClient;

	private static final String[] FORECAST_COLUMNS = {
			WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
			WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
			WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
			WeatherContract.WeatherEntry.COLUMN_MIN_TEMP
	};

	// these indices must match the projection
	private static final int INDEX_WEATHER_ID = 0;
	private static final int INDEX_SHORT_DESC = 1;
	private static final int INDEX_MAX_TEMP = 2;
	private static final int INDEX_MIN_TEMP = 3;

	// message data set
	final String WEATHER_ID = "weatherId";
	final String DESCRIPTION = "description";
	final String MAX_TEMP = "maxtemp";
	final String MIN_TEMP = "mintemp";

	@Override
	public void onCreate() {
		super.onCreate();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (googleApiClient != null) {
			googleApiClient.disconnect();
		}
	}

	@Override
	public void onMessageReceived(MessageEvent messageEvent) {
		GetWeatherTask task = new GetWeatherTask();
		task.execute();
	}

	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		Log.e(TAG, "failed connecting " + connectionResult);
	}

	private class GetWeatherTask extends AsyncTask {

		@Override
		protected Object doInBackground(Object[] params) {

			googleApiClient = new GoogleApiClient.Builder(WearToDeviceService.this)
					.addOnConnectionFailedListener(WearToDeviceService.this)
					.addApi(Wearable.API)
					.build();
			googleApiClient.blockingConnect();

			if (googleApiClient.isConnected()) {
				// Get today's data from the ContentProvider
				String location = Utility.getPreferredLocation(WearToDeviceService.this);
				Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(
						location, System.currentTimeMillis());
				Cursor data = getContentResolver().query(weatherForLocationUri, FORECAST_COLUMNS, null,
						null, WeatherContract.WeatherEntry.COLUMN_DATE + " ASC");

				if (data == null) {
					return null;
				}
				if (!data.moveToFirst()) {
					data.close();
					return null;
				}

				// Extract the weather data from the Cursor
				int weatherId = data.getInt(INDEX_WEATHER_ID);
				String description = data.getString(INDEX_SHORT_DESC);
				double maxTemp = data.getDouble(INDEX_MAX_TEMP);
				double minTemp = data.getDouble(INDEX_MIN_TEMP);
				String formattedMaxTemperature = Utility.formatTemperature(WearToDeviceService.this, maxTemp);
				String formattedMinTemperature = Utility.formatTemperature(WearToDeviceService.this, minTemp);
				data.close();

				PutDataMapRequest putDataMapReq = PutDataMapRequest.create(DATA_ITEM_WEATHER_PATH);
				putDataMapReq.getDataMap().putInt(WEATHER_ID, weatherId);
				putDataMapReq.getDataMap().putString(DESCRIPTION, description);
				putDataMapReq.getDataMap().putString(MAX_TEMP, formattedMaxTemperature);
				putDataMapReq.getDataMap().putString(MIN_TEMP, formattedMinTemperature);
				putDataMapReq.getDataMap().putLong("timestamp", System.currentTimeMillis());

				PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
				PendingResult<DataApi.DataItemResult> pendingResult =
						Wearable.DataApi.putDataItem(googleApiClient, putDataReq);

				pendingResult.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
					@Override
					public void onResult(final DataApi.DataItemResult result) {
						googleApiClient.disconnect();
					}
				});
			}
			return null;
		}
	}
}
