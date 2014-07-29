package com.example.nearbyplaces;

import java.io.BufferedReader;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.example.nearbyplaces.FoursquareDialog.FsqDialogListener;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;
import android.app.ProgressDialog;

import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Message;

/**
 * 
 * @author Lorensius W. L. T <lorenz@londatiga.net>
 * 
 */
public class FoursquareApp {
	private FoursquareSession mSession;
	private FoursquareDialog mDialog;
	private FsqAuthListener mListener;
	private ProgressDialog mProgress;
	private String mTokenUrl;
	private String mAccessToken;

	/**
	 * Callback url, as set in 'Manage OAuth Costumers' page
	 * (https://developer.foursquare.com/)
	 */
	public static final String CALLBACK_URL = "http://www.nearestplaces.com/";
	private static final String AUTH_URL = "https://foursquare.com/oauth2/authenticate?response_type=code";
	private static final String TOKEN_URL = "https://foursquare.com/oauth2/access_token?grant_type=authorization_code";
	private static final String API_URL = "https://api.foursquare.com/v2";
	private static final String TAG = "FoursquareApi";

	public FoursquareApp(Context context, String clientId, String clientSecret) {
		mSession = new FoursquareSession(context);

		mAccessToken = mSession.getAccessToken();

		mTokenUrl = TOKEN_URL + "&client_id=" + clientId + "&client_secret="
				+ clientSecret + "&redirect_uri=" + CALLBACK_URL;

		String url = AUTH_URL + "&client_id=" + clientId + "&redirect_uri="
				+ CALLBACK_URL;

		FsqDialogListener listener = new FsqDialogListener() {
			@Override
			public void onComplete(String code) {
				getAccessToken(code);
			}

			@Override
			public void onError(String error) {
				mListener.onFail("Authorization failed");
			}
		};

		mDialog = new FoursquareDialog(context, url, listener);
		mProgress = new ProgressDialog(context);

		mProgress.setCancelable(false);
	}

	private void getAccessToken(final String code) {
		mProgress.setMessage("Getting access token ...");
		mProgress.show();

		new Thread() {
			@Override
			public void run() {
				Log.i(TAG, "Getting access token");

				int what = 0;

				try {
					URL url = new URL(mTokenUrl + "&code=" + code);

					Log.i(TAG, "Opening URL " + url.toString());

					HttpURLConnection urlConnection = (HttpURLConnection) url
							.openConnection();

					urlConnection.setRequestMethod("GET");
					urlConnection.setDoInput(true);
					// urlConnection.setDoOutput(true);

					urlConnection.connect();

					JSONObject jsonObj = (JSONObject) new JSONTokener(
							streamToString(urlConnection.getInputStream()))
							.nextValue();
					mAccessToken = jsonObj.getString("access_token");

					Log.i(TAG, "Got access token: " + mAccessToken);
				} catch (Exception ex) {
					what = 1;

					ex.printStackTrace();
				}

				mHandler.sendMessage(mHandler.obtainMessage(what, 1, 0));
			}
		}.start();
	}

	private void fetchUserName() {
		mProgress.setMessage("Finishing ...");

		new Thread() {
			@Override
			public void run() {
				Log.i(TAG, "Fetching user name");
				int what = 0;

				try {
					String v = timeMilisToString(System.currentTimeMillis());
					URL url = new URL(API_URL + "/users/self?oauth_token="
							+ mAccessToken + "&v=" + v);

					Log.d(TAG, "Opening URL " + url.toString());

					HttpURLConnection urlConnection = (HttpURLConnection) url
							.openConnection();

					urlConnection.setRequestMethod("GET");
					urlConnection.setDoInput(true);
					// urlConnection.setDoOutput(true);

					urlConnection.connect();

					String response = streamToString(urlConnection
							.getInputStream());
					JSONObject jsonObj = (JSONObject) new JSONTokener(response)
							.nextValue();

					JSONObject resp = (JSONObject) jsonObj.get("response");
					JSONObject user = (JSONObject) resp.get("user");

					String firstName = user.getString("firstName");
					String lastName = user.getString("lastName");

					Log.i(TAG, "Got user name: " + firstName + " " + lastName);

					mSession.storeAccessToken(mAccessToken, firstName + " "
							+ lastName);
				} catch (Exception ex) {
					what = 1;

					ex.printStackTrace();
				}

				mHandler.sendMessage(mHandler.obtainMessage(what, 2, 0));
			}
		}.start();
	}

	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (msg.arg1 == 1) {
				if (msg.what == 0) {
					fetchUserName();
				} else {
					mProgress.dismiss();

					mListener.onFail("Failed to get access token");
				}
			} else {
				mProgress.dismiss();

				mListener.onSuccess();
			}
		}
	};

	public boolean hasAccessToken() {
		return (mAccessToken == null) ? false : true;
	}

	public void setListener(FsqAuthListener listener) {
		mListener = listener;
	}

	public String getUserName() {
		return mSession.getUsername();
	}

	public void authorize() {
		mDialog.show();
	}

	public ArrayList<FsqVenue> getNearby(double latitude, double longitude)
			throws Exception {
		ArrayList<FsqVenue> venueList = new ArrayList<FsqVenue>();

		try {
			String v = timeMilisToString(System.currentTimeMillis());
			String ll = String.valueOf(latitude) + ","
					+ String.valueOf(longitude);
			URL url;
			url = new URL(API_URL + "/venues/search?ll=" + ll + "&oauth_token="
					+ mSession.getAccessToken() + "&v=" + v + "&limit=10");

			Log.d(TAG, "Opening URL " + url.toString());

			HttpURLConnection urlConnection = (HttpURLConnection) url
					.openConnection();

			urlConnection.setRequestMethod("GET");
			urlConnection.setDoInput(true);

			urlConnection.connect();

			String response = streamToString(urlConnection.getInputStream());
			JSONObject jsonObj = (JSONObject) new JSONTokener(response)
					.nextValue();

			JSONArray groups = (JSONArray) jsonObj.getJSONObject("response")
					.getJSONArray("venues");

			int length = groups.length();
			Log.v("response_length_places", Integer.toString(length));
			if (length > 0) {
				int ilength = length;

				for (int j = 0; j < ilength; j++) {
					JSONObject item = (JSONObject) groups.get(j);

					FsqVenue venue = new FsqVenue();

					venue.id = item.getString("id");
					JSONObject location = (JSONObject) item
							.getJSONObject("location");
					Location loc = new Location(LocationManager.GPS_PROVIDER);

					loc.setLatitude(Double.valueOf(location.getString("lat")));
					loc.setLongitude(Double.valueOf(location.getString("lng")));
					venue.location = loc;
					venue.distance = location.getInt("distance");
					venue.herenow = item.getJSONObject("hereNow").getInt(
							"count");
					venue.name = item.getString("name");
					venue.imgURL = getImageURL(item.getString("id"));
					venueList.add(venue);
				}
			}
		} catch (Exception ex) {
			throw ex;
		}
		return venueList;
	}

	public String getImageURL(String venue_id) throws Exception {
		// ArrayList<FsqVenue> venueList = new ArrayList<FsqVenue>();
		String img_URL = "";
		int length = 0;
		try {

			String v = timeMilisToString(System.currentTimeMillis());
			URL url = new URL(API_URL + "/venues/" + venue_id
					+ "/photos?oauth_token=" + mSession.getAccessToken()
					+ "&v=" + v);
			Log.d(TAG, "Opening URL " + url.toString());

			HttpURLConnection urlConnection = (HttpURLConnection) url
					.openConnection();

			urlConnection.setRequestMethod("GET");
			urlConnection.setDoInput(true);

			urlConnection.connect();

			String response = streamToString(urlConnection.getInputStream());
			JSONObject jsonObj = (JSONObject) new JSONTokener(response)
					.nextValue();

			JSONArray items = (JSONArray) jsonObj.getJSONObject("response")
					.getJSONObject("photos").getJSONArray("items");

			length = items.length();
			if (length > 0) {

				JSONObject item = (JSONObject) items.get(0);
				img_URL = item.getString("prefix") + item.getString("width")
						+ "x" + item.getString("height")
						+ item.getString("suffix");

			} else {
				img_URL = "NotFound";
			}
		} catch (Exception ex) {
			img_URL = "NotFound";

			System.out.println(ex.toString());
			throw ex;
		}
		Log.v("response_length_images", Integer.toString(length));

		return img_URL;
	}

	public Boolean CheckinVenue(String venue_id) throws Exception {
		boolean flag = false;
		try {

			String v = timeMilisToString(System.currentTimeMillis());
			URL url = new URL(API_URL + "/checkins/add?oauth_token="
					+ mSession.getAccessToken() + "&v=" + v);

			HttpURLConnection urlConnection = (HttpURLConnection) url
					.openConnection();

			urlConnection.setRequestMethod("GET");
			urlConnection.setDoInput(true);
			urlConnection.setDoOutput(true);
			List<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair("venueId", venue_id));
			OutputStream os = urlConnection.getOutputStream();
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
					os, "UTF-8"));
			writer.write(getQuery(params));
			writer.flush();
			writer.close();
			os.close();
			urlConnection.connect();

			String response = streamToString(urlConnection.getInputStream());
			JSONObject jsonObj = (JSONObject) new JSONTokener(response)
					.nextValue();

			int code = Integer.parseInt(jsonObj.getJSONObject("meta")
					.getString("code"));

			if (code == 200) {

				flag = true;
			} else {
				flag = false;
			}
		} catch (Exception ex) {
			flag = false;

			System.out.println(ex.toString());
			throw ex;
		}

		return flag;
	}

	private String getQuery(List<NameValuePair> params)
			throws UnsupportedEncodingException {
		StringBuilder result = new StringBuilder();
		boolean first = true;

		for (NameValuePair pair : params) {
			if (first)
				first = false;
			else
				result.append("&");

			result.append(URLEncoder.encode(pair.getName(), "UTF-8"));
			result.append("=");
			result.append(URLEncoder.encode(pair.getValue(), "UTF-8"));
		}

		return result.toString();
	}

	private String streamToString(InputStream is) throws IOException {
		String str = "";

		if (is != null) {
			StringBuilder sb = new StringBuilder();
			String line;

			try {
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(is));

				while ((line = reader.readLine()) != null) {
					sb.append(line);
				}

				reader.close();
			} finally {
				is.close();
			}

			str = sb.toString();
		}

		return str;
	}

	private String timeMilisToString(long milis) {
		SimpleDateFormat sd = new SimpleDateFormat("yyyyMMdd");
		Calendar calendar = Calendar.getInstance();

		calendar.setTimeInMillis(milis);

		return sd.format(calendar.getTime());
	}

	public interface FsqAuthListener {
		public abstract void onSuccess();

		public abstract void onFail(String error);
	}

}