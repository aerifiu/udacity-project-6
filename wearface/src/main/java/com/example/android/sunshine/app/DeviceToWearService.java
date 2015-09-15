package com.example.android.sunshine.app;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.List;

public class DeviceToWearService extends WearableListenerService {

	public static final String INTENT_WEATHER_DATA = "com.example.android.sunshine.app.intentWeatherData";
	public static final String EXTRA_WEATHER_DATA = "com.example.android.sunshine.app.extraWeatherData";
	final String WEATHER_ID = "weatherId";
	final String DESCRIPTION = "description";
	final String MAX_TEMP = "maxtemp";
	final String MIN_TEMP = "mintemp";

	private static final String TAG = "DataLayerSample";
	public static final String DATA_ITEM_WEATHER_PATH = "/sunshine";

	@Override
	public void onDataChanged(DataEventBuffer dataEvents) {
		final List<DataEvent> events = FreezableUtils.freezeIterable(dataEvents);
		for (DataEvent event : events) {
			if (event.getType() == DataEvent.TYPE_CHANGED) {
				String path = event.getDataItem().getUri().getPath();
				if (path.equals(DATA_ITEM_WEATHER_PATH)) {
					DataMap dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();

					try {
						int res = dataMap.get(WEATHER_ID);
						String description = dataMap.get(DESCRIPTION);
						String maxTemp = dataMap.get(MAX_TEMP);
						String minTemp = dataMap.get(MIN_TEMP);

						WeatherBean bean = new WeatherBean(res, description, maxTemp, minTemp);
						Intent intent = new Intent(INTENT_WEATHER_DATA);
						intent.putExtra(EXTRA_WEATHER_DATA, bean);
						LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
						manager.sendBroadcast(intent);

					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
}
