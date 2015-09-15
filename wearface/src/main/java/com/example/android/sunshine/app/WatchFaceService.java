package com.example.android.sunshine.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
import android.view.SurfaceHolder;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Wearable;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class WatchFaceService extends CanvasWatchFaceService implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient
		.OnConnectionFailedListener {

	private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equals(DeviceToWearService.INTENT_WEATHER_DATA)) {
				WeatherBean bean = (WeatherBean) intent.getSerializableExtra(DeviceToWearService.EXTRA_WEATHER_DATA);
				if (bean != null) {
					currentWeatherData = bean;
					engine.invalidate();
				}
			}
		}
	};

	private static final String TAG = "WatchFaceService";
	private static final long REFRESH_INTERVAL = 10000;
	private GoogleApiClient googleApiClient;
	private Engine engine;
	private volatile WeatherBean currentWeatherData = new WeatherBean(R.drawable.art_clear, "unknown", "?", "?");

	private Handler refreshWeatherHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			requestWeather();
			sendEmptyMessageDelayed(0, REFRESH_INTERVAL);
		}
	};

	@Override
	public void onCreate() {
		super.onCreate();

		IntentFilter intentFilter = new IntentFilter(DeviceToWearService.INTENT_WEATHER_DATA);
		LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, intentFilter);

		googleApiClient = new GoogleApiClient.Builder(this)
				.addConnectionCallbacks(this)
				.addOnConnectionFailedListener(this)
				.addApi(Wearable.API)
				.build();
		googleApiClient.connect();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		googleApiClient.disconnect();
		refreshWeatherHandler.removeCallbacksAndMessages(null);

		LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
	}

	private void requestWeather() {
		if (googleApiClient.isConnected()) {
			Wearable.MessageApi.sendMessage(googleApiClient, "", "", null);
		}
	}

	@Override
	public void onConnected(Bundle bundle) {
		requestWeather();
		refreshWeatherHandler.sendEmptyMessageDelayed(0, REFRESH_INTERVAL);
	}

	@Override
	public void onConnectionSuspended(int i) {
	}

	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		Log.e(TAG, "failed connecting " + connectionResult);
	}

	@Override
	public Engine onCreateEngine() {
		this.engine = new Engine();
		return engine;
	}

	private class Engine extends CanvasWatchFaceService.Engine {

		Paint textPaint;
		Paint tempPaint;
		Paint datePaint;
		Paint linePaint;
		Paint backgroundPaint;

		@Override
		public void onCreate(SurfaceHolder holder) {
			super.onCreate(holder);

			setWatchFaceStyle(new WatchFaceStyle.Builder(WatchFaceService.this)
							.setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
							.setCardPeekMode(WatchFaceStyle.PEEK_MODE_VARIABLE)
							.setShowSystemUiTime(false)
							.build()
			);

			backgroundPaint = new Paint();
			backgroundPaint.setColor(getBackgroundColor(false));

			textPaint = new Paint();
			textPaint.setTextSize(56);
			textPaint.setColor(getPrimaryColor(false));
			textPaint.setAntiAlias(true);

			tempPaint = new Paint();
			tempPaint.setTextSize(36);
			tempPaint.setColor(getPrimaryColor(false));
			tempPaint.setAntiAlias(true);

			datePaint = new Paint();
			datePaint.setTextSize(22);
			datePaint.setColor(getSecondaryColor());
			datePaint.setAntiAlias(true);

			linePaint = new Paint();
			linePaint.setColor(getSecondaryColor());
			linePaint.setStrokeWidth(1);
			linePaint.setAntiAlias(true);
		}

		@Override
		public void onTimeTick() {
			super.onTimeTick();
			invalidate();
		}

		@Override
		public void onDraw(Canvas canvas, Rect bounds) {
			canvas.drawRect(bounds, backgroundPaint);

			Calendar cal = Calendar.getInstance();

			final String date = new SimpleDateFormat("ccc, dd MMM", Locale.getDefault()).format(cal.getTime());
			final String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(cal.getTime());
			final String temp = String.format(getResources().getString(R.string.temp),
					currentWeatherData.maxTemp, currentWeatherData.minTemp);
			float textTimeXOffset = textPaint.measureText(time) / 2;

			canvas.drawText(time,
					bounds.centerX() - textTimeXOffset,
					bounds.height() * 0.35f,
					textPaint);


			if (!isInAmbientMode()) {
				float textDateXOffset = datePaint.measureText(date) / 2;
				canvas.drawText(date,
						bounds.centerX() - textDateXOffset,
						bounds.height() * 0.45f,
						datePaint);

				float tempXOffset = tempPaint.measureText(temp) / 2;
				canvas.drawText(temp,
						bounds.centerX() - tempXOffset,
						bounds.height() * 0.9f,
						tempPaint);

				final int resId = Util.getIconResourceForWeatherCondition(currentWeatherData.weatherId);
				if (resId != -1) {
					Bitmap bitmap = BitmapFactory.decodeResource(getResources(), resId);
					int size = (int) (bounds.width() * 0.20f);
					bitmap = Bitmap.createScaledBitmap(bitmap, size, size, true);
					int width = bitmap.getScaledWidth(canvas);

					canvas.drawBitmap(bitmap,
							bounds.centerX() - width / 2,
							bounds.height() * 0.58f,
							null);

					bitmap.recycle();
				}

				canvas.drawLine(bounds.width() * 0.7f, bounds.centerY(), bounds.width() * 0.3f, bounds.centerY(), linePaint);
			}

		}

		@Override
		public void onAmbientModeChanged(boolean inAmbientMode) {
			super.onAmbientModeChanged(inAmbientMode);
			backgroundPaint.setColor(getBackgroundColor(inAmbientMode));
			textPaint.setColor(getPrimaryColor(inAmbientMode));
			textPaint.setAntiAlias(!inAmbientMode);
		}

		private int getBackgroundColor(boolean isInAmbientMode) {
			return isInAmbientMode ? getResources().getColor(R.color.md_teal_900) : getResources().getColor(R.color.md_teal_500);
		}

		private int getPrimaryColor(boolean isInAmbientMode) {
			return isInAmbientMode ? getResources().getColor(R.color.md_teal_500) : getResources().getColor(R.color.md_teal_50);
		}

		private int getSecondaryColor() {
			return getResources().getColor(R.color.md_teal_200);
		}
	}
}
