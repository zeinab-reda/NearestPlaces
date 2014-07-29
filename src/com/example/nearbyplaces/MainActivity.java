package com.example.nearbyplaces;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.net.URL;
import java.util.ArrayList;
import com.example.nearbyplaces.FoursquareApp.FsqAuthListener;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

public class MainActivity extends Activity implements OnInfoWindowClickListener {

	// client_id && secret_id of application on foursquare API
	public static final String CLIENT_ID = "BBEIJCQKSZE5X54R2OBFGT3FN2CIUKMJO0XSK3T1RX43W0JA";
	public static final String CLIENT_SECRET = "5OKO5AIPAHMCPJ2C11SZ2VY5WIIALU1OKUSVWV0X25L1RYML";

	// map for show nearest places on it
	private GoogleMap map;

	// refreshing progressbar when gps location is getted
	private ProgressDialog mProgress;

	// load the nearest 7 places
	private ArrayList<FsqVenue> mNearbyList;

	private FoursquareApp mFsqApp;

	// update and listen to location changes
	private LocationManager locManager;
	private LocationListener locListener;
	private Location mobileLocation;

	// check gps aviability
	private Boolean gps_Enabled = false;
	private Boolean network_Enabled = false;

	// latest location of user
	double lng, lat;

	// list for images & their location & their name for nearest places
	private ArrayList<Marker> myPins;

	private Bitmap bitmap;

	// to caching latest venues
	DatabaseHandler db;
	// directory for saving image into sdcard
	File imagesfolder;

	// return to confirm checkin for clicked venue
	boolean checkinFalg;

	int pinIndex = 0;

	// to save latest position of user
	SharedPreferences latestPosition;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		// initialization for member variables
		imagesfolder = new File(Environment.getExternalStorageDirectory()
				+ "/NearbyPlaces/Pins/");

		try {
			mFsqApp = new FoursquareApp(this, CLIENT_ID, CLIENT_SECRET);
			mNearbyList = new ArrayList<FsqVenue>();
			mProgress = new ProgressDialog(this);
			myPins = new ArrayList<Marker>();
			checkinFalg = false;
			mProgress.setMessage("Refreshing ...");
			db = new DatabaseHandler(getApplicationContext());
			initilizeMap();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void initilizeMap() {
		if (map == null) {

			map = ((MapFragment) getFragmentManager()
					.findFragmentById(R.id.map)).getMap();
			map.setOnInfoWindowClickListener(this);
			// check if map is created successfully or not
			if (map == null) {
				Toast.makeText(getApplicationContext(),
						"Sorry! unable to create maps", Toast.LENGTH_SHORT)
						.show();
			} else {

			}
		}
	}

	private void getCurrentLocation() {
		locManager = (LocationManager) this
				.getSystemService(Context.LOCATION_SERVICE);
		locListener = new LocationListener() {
			@Override
			public void onStatusChanged(String provider, int status,
					Bundle extras) {
				// TODO Auto-generated method stub
			}

			@Override
			public void onProviderEnabled(String provider) {
				// TODO Auto-generated method stub
			}

			@Override
			public void onProviderDisabled(String provider) {
				// TODO Auto-generated method stub
			}

			@Override
			public void onLocationChanged(Location location) {
				// TODO Auto-generated method stub

				mobileLocation = location;
				if (mobileLocation != null) {
					locManager.removeUpdates(locListener); // This needs to stop
															// getting the
															// location data and
															// save the battery
															// power.

					lng = mobileLocation.getLongitude();
					lat = mobileLocation.getLatitude();
					saveLatestPosition();
					loadNearbyPlaces(lat, lng);

				} else {

				}

			}
		};
		gps_Enabled = locManager
				.isProviderEnabled(LocationManager.GPS_PROVIDER);
		network_Enabled = locManager
				.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

		latestPosition = getSharedPreferences("latestPostion", MODE_PRIVATE);

		if (latestPosition.getString("lng", null) != null) {
			lng = Double.parseDouble(latestPosition.getString("lng", null));
			lat = Double.parseDouble(latestPosition.getString("lat", null));

			centerMapOnMyLocation();

		}

		if (!gps_Enabled) {
			showToast(getApplicationContext(), "GPS is Not Enabled");
			mNearbyList=(ArrayList<FsqVenue>) db.getAllVenues();
			loadCachedVenues();

		} else {
			mNearbyList=(ArrayList<FsqVenue>) db.getAllVenues();
			loadCachedVenues();

			locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5,
					10, locListener);
		}
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		initilizeMap();
		// to get authentication if user is not authenticated else get his
		// position
		if (!mFsqApp.hasAccessToken()) {
			authorizeUser();
		} else {
			getCurrentLocation();
		}

	}

	private void loadNearbyPlaces(final double latitude, final double longitude) {
		mProgress.show();
		myPins.clear();
		mNearbyList.clear();
		new Thread() {
			@Override
			public void run() {
				int what = 0;

				try {

					mNearbyList = mFsqApp.getNearby(latitude, longitude);

				} catch (Exception e) {
					what = 1;
					e.printStackTrace();
				}

				nplacesHandler.sendMessage(nplacesHandler.obtainMessage(what));
			}
		}.start();
	}

	private Handler nplacesHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			mProgress.dismiss();

			centerMapOnMyLocation();
			if (msg.what == 0) {
				if (mNearbyList.size() == 0) {
					Toast.makeText(MainActivity.this,
							"No nearby places available", Toast.LENGTH_SHORT)
							.show();
					return;
				}

				showToast(getApplicationContext(), mNearbyList.size()+"Venues Loaded" );
				for (int i = 0; i < mNearbyList.size(); i++) {
				new LoadImage(i).execute(mNearbyList.get(i).getImgURL());

			}

				if (db.getVenuesCount() > 0) {
					db.deleteAllVenues();
					deleteFiles(imagesfolder.toString());
					deleteDatabase(imagesfolder.toString());
					map.clear();
				}
				addCachedVenues();
			} else {
				Toast.makeText(MainActivity.this,
						"Failed to load nearby places", Toast.LENGTH_SHORT)
						.show();
			}
		}
	};

	private void VenueCheckin(final String venue_id) {

		new Thread() {
			@Override
			public void run() {
				int flag = 0;

				try {

					checkinFalg = mFsqApp.CheckinVenue(venue_id);

				} catch (Exception e) {
					flag = 1;
					e.printStackTrace();
				}

				checkinHandler.sendMessage(checkinHandler.obtainMessage(flag));
			}
		}.start();
	}

	private Handler checkinHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {

			if (msg.what == 0) {

				if (checkinFalg)
					showToast(getApplicationContext(),
							"Venue Checkin Sucessfully");
				else
					showToast(getApplicationContext(), "Checkin Failed");

			} else {
				Toast.makeText(MainActivity.this, "Failed to check in venue",
						Toast.LENGTH_SHORT).show();
			}
		}
	};

	@Override
	public void onInfoWindowClick(Marker marker) {
		// TODO Auto-generated method stub

		for (pinIndex = 0; pinIndex < myPins.size(); pinIndex++) {

			if (marker.getPosition().equals(myPins.get(pinIndex).getPosition())) {
				final int index = pinIndex;
				Toast.makeText(getApplicationContext(),
						myPins.get(pinIndex).getTitle(), Toast.LENGTH_LONG)
						.show();
				AlertDialog.Builder alertDialog = new AlertDialog.Builder(
						MainActivity.this);

				// Setting Dialog Title
				alertDialog.setTitle("Check In");

				// Setting Dialog Message
				alertDialog.setMessage("Do you Want Check In Place?");

				// Setting Icon to Dialog
				alertDialog.setIcon(R.drawable.checkin);

				// Setting Positive "Yes" Button
				alertDialog.setPositiveButton("YES",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								// check for network access if not hint user
								// that there no network to checkin
								ConnectivityManager cm;
								cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
								NetworkInfo ni = cm.getActiveNetworkInfo();
								if (ni == null) {
									Toast.makeText(
											getApplicationContext(),
											"Sorry Your Internet Access is not Available!",
											Toast.LENGTH_SHORT).show();
								} else {
									VenueCheckin(mNearbyList.get(index).getId());
								}
							}
						});

				// Setting Negative "NO" Button
				alertDialog.setNegativeButton("NO",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								Toast.makeText(getApplicationContext(),
										"Places is Not Checkedin",
										Toast.LENGTH_SHORT).show();
								dialog.cancel();
							}
						});

				// Showing Alert Message
				alertDialog.show();
			}
		}
	}

	private class LoadImage extends AsyncTask<String, String, Bitmap> {
		private int index;
		Bitmap resized;
		BitmapDescriptor fImage;

		public LoadImage(int venueIndex) {
			index = venueIndex;
		}

		protected Bitmap doInBackground(String... args) {
			try {
				// check if there no image supported for this venue then set
				// default pin image for it
				if (!args[0].equals("NotFound"))
					bitmap = BitmapFactory.decodeStream((InputStream) new URL(
							args[0]).getContent());

				else
					bitmap = BitmapFactory.decodeResource(getResources(),
							R.drawable.notimage);

				resized = Bitmap.createScaledBitmap(bitmap, (int) (50),
						(int) (50), true);
				fImage = BitmapDescriptorFactory.fromBitmap(resized);

			} catch (Exception e) {
				e.printStackTrace();
			}
			return resized;
		}

		protected void onPostExecute(Bitmap image) {
			if (image != null) {
				storeImage(image, mNearbyList.get(index).id + ".png", "png");
				LatLng latlng = new LatLng(
						mNearbyList.get(index).location.getLatitude(),
						mNearbyList.get(index).location.getLongitude());
				Marker myMarker = map.addMarker(new MarkerOptions()
						.title(mNearbyList.get(index).name).position(latlng)
						.icon(fImage));
				myPins.add(myMarker);

			} else {
				Toast.makeText(MainActivity.this,
						"Image Does Not exist or Network Error",
						Toast.LENGTH_SHORT).show();
			}
		}
	}

	void showToast(Context context, String msg) {

		Toast.makeText(context, msg, Toast.LENGTH_LONG).show();

	}

	public void authorizeUser() {
		mFsqApp.authorize();
		FsqAuthListener listener = new FsqAuthListener() {
			@Override
			public void onSuccess() {
				Toast.makeText(MainActivity.this,
						"Hello , " + mFsqApp.getUserName(), Toast.LENGTH_SHORT)
						.show();
				getCurrentLocation();
			}

			@Override
			public void onFail(String error) {
				Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT)
						.show();
			}
		};
		mFsqApp.setListener(listener);

	}

	// store images in sdCard
	private boolean storeImage(Bitmap imageData, String filename,
			String extension) {

		boolean success = true;
		if (!imagesfolder.exists()) {
			success = imagesfolder.mkdirs();
		}
		try {
			String filePath = imagesfolder.toString() + "/" + filename;
			FileOutputStream fileOutputStream = new FileOutputStream(filePath);

			BufferedOutputStream bos = new BufferedOutputStream(
					fileOutputStream);
			if (extension.equals("jpg")) {
				imageData.compress(CompressFormat.JPEG, 100, bos);
			} else if (extension.equals("png")) {
				imageData.compress(CompressFormat.PNG, 100, bos);
			}

			bos.flush();
			bos.close();

		} catch (FileNotFoundException e) {
			Log.w("TAG", "Error saving image file: " + e.getMessage());
			return false;
		} catch (IOException e) {
			Log.w("TAG", "Error saving image file: " + e.getMessage());
			return false;
		}

		return true;
	}

	// Caching latest venues retrieved from server

	private void addCachedVenues() {
		// TODO Auto-generated method stub

		if (mNearbyList.size() == 0) {
			showToast(getApplicationContext(), "No Venues to Cache");
		} else {
			for (int i = 0; i < mNearbyList.size(); i++) {

				FsqVenue venue = new FsqVenue();
				venue.setId(mNearbyList.get(i).getId());
				venue.setName(mNearbyList.get(i).getName());
				venue.setLocation(mNearbyList.get(i).getLocation());
				venue.setImgPath(mNearbyList.get(i).getId());
				db.addVenue(venue);

			}
		}

	}

	// Retrieving latest cached venues
	private void loadCachedVenues() {
		// TODO Auto-generated method stub
		ArrayList<FsqVenue> cachedVenueList = (ArrayList<FsqVenue>) db
				.getAllVenues();
		if (cachedVenueList.size() == 0) {
			showToast(getApplicationContext(), "No Cached Venues");
		} else {
			for (int i = 0; i < cachedVenueList.size(); i++) {

				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inPreferredConfig = Bitmap.Config.ARGB_8888;
				Bitmap bitmap = BitmapFactory
						.decodeFile(imagesfolder + "/"
								+ cachedVenueList.get(i).getImgPath() + ".png",
								options);

				BitmapDescriptor dImage = BitmapDescriptorFactory
						.fromBitmap(bitmap);

				LatLng latlng = new LatLng(
						cachedVenueList.get(i).location.getLatitude(),
						cachedVenueList.get(i).location.getLongitude());
				Marker marker = map.addMarker(new MarkerOptions()
						.title(cachedVenueList.get(i).name).position(latlng)
						.icon(dImage));
				myPins.add(marker);
			}
		}

	}

	public void deleteFiles(String path) {
		File file = new File(path);

		if (file.exists()) {
			String deleteCmd = "rm -r " + path;
			Runtime runtime = Runtime.getRuntime();
			try {
				runtime.exec(deleteCmd);
			} catch (IOException e) {

			}
		}

	}

	// zoom in to latest or current position
	private void centerMapOnMyLocation() {

		LatLng ll = new LatLng(lat, lng);
		map.moveCamera(CameraUpdateFactory.newLatLngZoom(ll, 20));
	}

	// save latest position of user
	private void saveLatestPosition() {

		latestPosition = getSharedPreferences("latestPostion", MODE_PRIVATE);
		SharedPreferences.Editor editor = latestPosition.edit();
		editor.putString("lng", Double.toString(lng));
		editor.putString("lat", Double.toString(lat));
		editor.commit();

	}

}
