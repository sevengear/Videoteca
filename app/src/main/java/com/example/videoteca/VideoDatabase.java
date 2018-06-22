package com.example.videoteca;

import android.app.SearchManager;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.provider.BaseColumns;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Miguel Á. Núñez on 23/06/2018.
 */
public class VideoDatabase {
    public static final String KEY_NAME = SearchManager.SUGGEST_COLUMN_TEXT_1;
    public static final String KEY_DATA_TYPE = SearchManager.SUGGEST_COLUMN_CONTENT_TYPE;
    public static final String KEY_PRODUCTION_YEAR = SearchManager.SUGGEST_COLUMN_PRODUCTION_YEAR;
    public static final String KEY_ICON = SearchManager.SUGGEST_COLUMN_RESULT_CARD_IMAGE;
    private static final String DATABASE_NAME = "Videoteca_database";
    private static final String FTS_VIRTUAL_TABLE = "Videoteca_table";
    private static final int DATABASE_VERSION = 8;
    private static final HashMap<String, String> COLUMN_MAP = buildColumnMap();
    private static int CARD_WIDTH = 313;
    private static int CARD_HEIGHT = 176;
    private VideoDatabaseOpenHelper mDatabaseOpenHelper;
    private SQLiteDatabase mDatabase;

    public VideoDatabase(Context context) {
        mDatabaseOpenHelper = new VideoDatabaseOpenHelper(context);
        mDatabase = mDatabaseOpenHelper.getWritableDatabase();
        mDatabaseOpenHelper.refrescarTabla(mDatabase);
    }

    private static HashMap<String, String> buildColumnMap() {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put(KEY_NAME, KEY_NAME);
        map.put(KEY_DATA_TYPE, KEY_DATA_TYPE);
        map.put(KEY_PRODUCTION_YEAR, KEY_PRODUCTION_YEAR);
        map.put(KEY_ICON, KEY_ICON);
        map.put(BaseColumns._ID, "rowid AS " + BaseColumns._ID);
        map.put(SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID, "rowid AS " + SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID);
        map.put(SearchManager.SUGGEST_COLUMN_SHORTCUT_ID, "rowid AS " + SearchManager.SUGGEST_COLUMN_SHORTCUT_ID);
        return map;
    }

    public Cursor getWord(String rowId, String[] columns) {
        String selection = "rowid = ?";
        String[] selectionArgs = new String[]{rowId};
        return query(selection, selectionArgs, columns);
    }

    public Cursor getWordMatch(String query, String[] columns) {
        String selection = KEY_NAME + " MATCH ?";
        String[] selectionArgs = new String[]{query + "*"};
        return query(selection, selectionArgs, columns);
    }

    private Cursor query(String selection, String[] selectionArgs, String[] columns) {
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables(FTS_VIRTUAL_TABLE);
        builder.setProjectionMap(COLUMN_MAP);
        Cursor cursor = new PaginatedCursor(builder.query(mDatabaseOpenHelper.getReadableDatabase(), columns, selection, selectionArgs, null, null, null));
        if (cursor == null) {
            return null;
        } else if (!cursor.moveToFirst()) {
            cursor.close();
            return null;
        }
        return cursor;
    }

    private static class VideoDatabaseOpenHelper extends SQLiteOpenHelper {
        private final Context mHelperContext;
        private SQLiteDatabase dbase;

        VideoDatabaseOpenHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
            mHelperContext = context;
        }

        private static final String FTS_TABLE_CREATE = "CREATE VIRTUAL TABLE " + FTS_VIRTUAL_TABLE + " USING fts3 (" + KEY_NAME + ", " + KEY_DATA_TYPE + "," + KEY_ICON + "," + KEY_PRODUCTION_YEAR + ");";

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(FTS_TABLE_CREATE);
            loadDatabase();
            dbase = db;
        }

        private void loadDatabase() {
            new Thread(new Runnable() {
                public void run() {
                    try {
                        loadMovies();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }).start();
        }

        private void loadMovies() throws IOException {
            if (MovieList.list == null) {
                MovieList.list = new ArrayList<Movie>();
                String json = Utils.loadJSONFromResource(mHelperContext, R.raw.movies);
                Gson gson = new Gson();
                Type collection = new TypeToken<ArrayList<Movie>>() {
                }.getType();
                MovieList.list = gson.fromJson(json, collection);
            }
            for (Movie movie : MovieList.list) {
                long id = addMovie(movie);
            }
        }

        public long addMovie(Movie movie) {
            ContentValues initialValues = new ContentValues();
            initialValues.put(KEY_NAME, movie.getTitle());
            initialValues.put(KEY_DATA_TYPE, "video/mp4");
            initialValues.put(KEY_PRODUCTION_YEAR, 2014);
            initialValues.put(KEY_ICON, movie.getCardImageUrl());
            return dbase.insert(FTS_VIRTUAL_TABLE, null, initialValues);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + FTS_VIRTUAL_TABLE);
            onCreate(db);
        }

        public void refrescarTabla(SQLiteDatabase db) {
            db.execSQL("DROP TABLE IF EXISTS " + FTS_VIRTUAL_TABLE);
            onCreate(db);
        }
    }
}