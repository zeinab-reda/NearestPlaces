package com.example.nearbyplaces;

import java.util.ArrayList;
import java.util.List;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

public class DatabaseHandler extends SQLiteOpenHelper {

	// All Static variables
	// Venues table name
	private static final String TABLE_VENUES = "venues";

	// Database Version
	private static final int DATABASE_VERSION = 1;
	// Venues Table Columns names

	private static final String KEY_VENUE_ID = "venue_id";
	private static final String KEY_VENUE_IMG_PATH = "venue_img_path";
	private static final String KEY_VENUE_Longtitude = "venue_longtitude";
	private static final String KEY_VENUE_Latitude = "venue_latitude";
	private static final String KEY_VENUE_NAME = "venue_name";

	// Database Name
	private static final String DATABASE_NAME = "mydb";

	public DatabaseHandler(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		// TODO Auto-generated method stub
		String CREATE_VENUE_TABLE = "CREATE TABLE " + TABLE_VENUES + "("
				+ KEY_VENUE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
				+ KEY_VENUE_NAME + " TEXT," + KEY_VENUE_IMG_PATH + " TEXT,"
				+ KEY_VENUE_Longtitude + " TEXT," + KEY_VENUE_Latitude
				+ " TEXT )";

		db.execSQL(CREATE_VENUE_TABLE);

	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

		db.execSQL("DROP TABLE IF EXISTS " + TABLE_VENUES);

		// Create tables again
		onCreate(db);
	}

	// Adding new model
	public void addVenue(FsqVenue venue) {

		SQLiteDatabase db = this.getWritableDatabase();

		ContentValues values = new ContentValues();
		values.put(KEY_VENUE_NAME, venue.getName());
		values.put(KEY_VENUE_IMG_PATH, venue.getImgPath());
		System.out.println("imgPath " + venue.getImgPath());
		values.put(KEY_VENUE_Longtitude,
				Double.toString(venue.getLocation().getLongitude()));
		values.put(KEY_VENUE_Latitude,
				Double.toString(venue.getLocation().getLatitude()));

		// Inserting Row
		db.insert(TABLE_VENUES, null, values);
		db.close(); // Closing database connection
	}

	public FsqVenue getVenue(int id) {
		FsqVenue venue = null;
		SQLiteDatabase db = this.getReadableDatabase();

		Cursor cursor = db.query(TABLE_VENUES, new String[] { KEY_VENUE_ID,
				KEY_VENUE_NAME, KEY_VENUE_Latitude, KEY_VENUE_Longtitude,
				KEY_VENUE_Latitude }, KEY_VENUE_ID + "=?",
				new String[] { String.valueOf(id) }, null, null, null, null);
		if (cursor != null && !cursor.isClosed()) {
			cursor.moveToFirst();
			if (cursor.getCount() > 0) {
				venue = new FsqVenue(cursor.getString(0), cursor.getString(1),
						cursor.getString(2), cursor.getString(3),
						cursor.getString(4));
			}
			cursor.close();
		}
		db.close();
		return venue;
	}

	// Getting All Models
	public List<FsqVenue> getAllVenues() {
		List<FsqVenue> modelsList = new ArrayList<FsqVenue>();
		// Select All Query
		String selectQuery = "SELECT  * FROM " + TABLE_VENUES;

		SQLiteDatabase db = this.getWritableDatabase();
		Cursor cursor = db.rawQuery(selectQuery, null);

		// looping through all rows and adding to list
		if (cursor.moveToFirst()) {
			do {
				FsqVenue venue = new FsqVenue();
				venue.setId(cursor.getString(0));
				venue.setName(cursor.getString(1));
				venue.setImgPath(cursor.getString(2));
				Location loc = new Location(LocationManager.GPS_PROVIDER);
				loc.setLongitude(Double.valueOf(cursor.getString(3)));
				loc.setLatitude(Double.valueOf(cursor.getString(4)));
				venue.setLocation(loc);

				// Adding model to list
				modelsList.add(venue);
			} while (cursor.moveToNext());
		}
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}
		db.close();
		return modelsList;
	}

	// Getting models Count
	public int getVenuesCount() {
		String countQuery = "SELECT * FROM " + TABLE_VENUES;
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.rawQuery(countQuery, null);
		int count = 0;
		try {
			if (cursor.moveToFirst()) {
				count = cursor.getCount();
			}
			return count;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	public void deleteAllVenues() {

		SQLiteDatabase db = this.getWritableDatabase();
		db.execSQL("delete from " + TABLE_VENUES);
		db.close();
	}

}
