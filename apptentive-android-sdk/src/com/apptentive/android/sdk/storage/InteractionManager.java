package com.apptentive.android.sdk.storage;

import android.content.Context;
import android.content.SharedPreferences;
import com.apptentive.android.sdk.Log;
import com.apptentive.android.sdk.comm.ApptentiveClient;
import com.apptentive.android.sdk.comm.ApptentiveHttpResponse;
import com.apptentive.android.sdk.model.Interaction;
import com.apptentive.android.sdk.model.Interactions;
import com.apptentive.android.sdk.module.metric.MetricModule;
import com.apptentive.android.sdk.util.Constants;
import com.apptentive.android.sdk.util.Util;
import org.json.JSONException;

/**
 * @author Sky Kelsey
 */
public class InteractionManager {


	public static void asyncFetchAndStoreInteractions(final Context context) {

		if (hasCacheExpired(context)) {
			Log.d("Interaction cache has expired. Fetching new interactions.");
			Thread thread = new Thread() {
				public void run() {
					fetchAndStoreInteractions(context);
				}
			};
			Thread.UncaughtExceptionHandler handler = new Thread.UncaughtExceptionHandler() {
				@Override
				public void uncaughtException(Thread thread, Throwable throwable) {
					Log.w("UncaughtException in InteractionManager.", throwable);
					MetricModule.sendError(context.getApplicationContext(), throwable, null, null);
				}
			};
			thread.setUncaughtExceptionHandler(handler);
			thread.setName("Apptentive-FetchInteractions");
			thread.start();
		} else {
			Log.d("Interaction cache has not expired. Using existing interactions.");
		}
	}

	public static void fetchAndStoreInteractions(Context context) {
		ApptentiveHttpResponse response = ApptentiveClient.getInteractions();

		if (response != null && response.isSuccessful()) {
			String interactionsString = response.getContent();

			// Store new integration cache expiration.
			String cacheControl = response.getHeaders().get("Cache-Control");
			Integer cacheSeconds = Util.parseCacheControlHeader(cacheControl);
			if (cacheSeconds == null) {
				cacheSeconds = Constants.CONFIG_DEFAULT_INTERACTION_CACHE_EXPIRATION_DURATION_SECONDS;
			}
			updateCacheExpiration(context, cacheSeconds);
			storeInteractions(context, interactionsString);		}
	}

	public static Interaction loadInteraction(Context context, String codePoint) {
		return null;
	}

	public static void recordInteraction(String interaction) {

	}

	private static Interactions loadInteractions(Context context) {
		Log.d("Loading interactions.");
		SharedPreferences prefs = context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE);
		String interactionsString = prefs.getString(Constants.PREF_KEY_INTERACTIONS, null);
		if (interactionsString != null) {
			try {
				return new Interactions(interactionsString);
			} catch (JSONException e) {
				Log.w("Exception creating Interactions object.", e);
			}
		}
		return null;
	}

	private static void storeInteractions(Context context, String interactionsString) {
		Log.v("Storing interactions: " + interactionsString);
		SharedPreferences prefs = context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE);
		prefs.edit().putString(Constants.PREF_KEY_INTERACTIONS, interactionsString).commit();
	}

	private static boolean hasCacheExpired(Context context) {
		SharedPreferences prefs = context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE);
		long expiration = prefs.getLong(Constants.PREF_KEY_INTERACTIONS_CACHE_EXPIRATION, 0);
		return expiration < System.currentTimeMillis();
	}

	private static void updateCacheExpiration(Context context, long duration) {
		long expiration = System.currentTimeMillis() + (duration * 1000);
		SharedPreferences prefs = context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE);
		prefs.edit().putLong(Constants.PREF_KEY_INTERACTIONS_CACHE_EXPIRATION, expiration).commit();
	}
}