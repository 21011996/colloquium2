package ru.ifmo.md.lesson7;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

public class FeedContentProvider extends ContentProvider {
    private static final int FEEDS = 0;
    private static final int FEED_ID = 1;
    private static final int POSTS = 2;

    private static final String AUTHORITY = "ru.ifmo.md.lesson7";
    private static final String PATH_FEEDS = "feeds";
    public static final Uri CONTENT_URI_FEEDS = Uri.parse("content://" + AUTHORITY + "/" + PATH_FEEDS);
    private static final String PATH_POSTS = "posts";
    public static final Uri CONTENT_URI_POSTS = Uri.parse("content://" + AUTHORITY + "/" + PATH_POSTS);
    private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        URI_MATCHER.addURI(AUTHORITY, PATH_FEEDS, FEEDS);
        URI_MATCHER.addURI(AUTHORITY, PATH_FEEDS + "/#", FEED_ID);
        URI_MATCHER.addURI(AUTHORITY, PATH_POSTS + "/#", POSTS);
    }

    static String DB_NAME = "feeds.db";
    static int DB_VERSION = 1;
    private static String FEEDS_TABLE = "feeds";
    private static String POSTS_TABLE = "posts";
    private FeedDatabaseHelper dbHelper;

    @Override
    public boolean onCreate() {
        dbHelper = new FeedDatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        int uriType = URI_MATCHER.match(uri);
        switch (uriType) {
            case FEEDS:
                queryBuilder.setTables(FEEDS_TABLE);
                break;
            case FEED_ID:
                queryBuilder.setTables(FEEDS_TABLE);
                queryBuilder.appendWhere("_id=" + uri.getLastPathSegment());
                break;
            case POSTS:
                queryBuilder.setTables(POSTS_TABLE);
                queryBuilder.appendWhere("feed_id=" + uri.getLastPathSegment());
                break;
            default:
                throw new IllegalArgumentException("Bad URI: " + uri);
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Cursor cursor = queryBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder);
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        int uriType = URI_MATCHER.match(uri);
        SQLiteDatabase sqlDB = dbHelper.getWritableDatabase();
        long id;
        switch (uriType) {
            case FEEDS:
                id = sqlDB.insert(FEEDS_TABLE, null, values);
                break;
            case POSTS:
                values.put("feed_id", uri.getLastPathSegment());
                id = sqlDB.insert(POSTS_TABLE, null, values);
                break;
            default:
                throw new IllegalArgumentException("Bad URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return Uri.withAppendedPath(uri, "" + id);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {

        int uriType = URI_MATCHER.match(uri);
        SQLiteDatabase sqlDB = dbHelper.getWritableDatabase();
        int rowsDeleted = 0;
        switch (uriType) {
            case FEEDS:
                rowsDeleted = sqlDB.delete(FEEDS_TABLE, selection, selectionArgs);
                break;
            case FEED_ID:
                String fid = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsDeleted = sqlDB.delete(FEEDS_TABLE, "_id=" + fid, null);
                } else {
                    rowsDeleted = sqlDB.delete(FEEDS_TABLE, "_id=" + fid + " and " + selection, selectionArgs);
                }
                break;
            case POSTS:
                String id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsDeleted = sqlDB.delete(POSTS_TABLE, "feed_id=" + id, null);
                } else {
                    rowsDeleted = sqlDB.delete(POSTS_TABLE, "feed_id=" + id + " and " + selection, selectionArgs);
                }
                break;
            default:
                throw new IllegalArgumentException("Bad URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return rowsDeleted;
    }


    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int uriType = URI_MATCHER.match(uri);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rowsUpdated;
        switch (uriType) {
            case FEEDS:
                rowsUpdated = db.update(FEEDS_TABLE, values, selection, selectionArgs);
                break;
            case FEED_ID:
                String id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsUpdated = db.update(FEEDS_TABLE, values, "_id=" + id, null);
                } else {
                    rowsUpdated = db.update(FEEDS_TABLE, values, "_id=" + id + " and " + selection, selectionArgs);
                }
                break;
            case POSTS:
                String feedId = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsUpdated = db.update(POSTS_TABLE, values, "_id=" + feedId, null);
                } else {
                    rowsUpdated = db.update(POSTS_TABLE, values, "_id=" + feedId + " and " + selection, selectionArgs);
                }
                break;
            default:
                throw new IllegalArgumentException("Bad URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return rowsUpdated;
    }

    private final class FeedDatabaseHelper extends SQLiteOpenHelper {
        public FeedDatabaseHelper(Context context) {
            super(context, DB_NAME, null, DB_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("PRAGMA foreign_keys=ON;");
            db.execSQL("CREATE TABLE " + FEEDS_TABLE +
                            " (_id INTEGER PRIMARY KEY AUTOINCREMENT" +
                            ", title TEXT NOT NULL" +
                            ", url TEXT NOT NULL UNIQUE);"
            );
            db.execSQL("CREATE TABLE " + POSTS_TABLE +
                            " (_id INTEGER PRIMARY KEY AUTOINCREMENT" +
                            ", title TEXT NOT NULL" +
                            ", url TEXT NOT NULL" +
                            ", feed_id INTEGER REFERENCES feeds(_id) ON DELETE CASCADE);"
            );
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int i, int i2) {
            db.execSQL("DROP TABLE IF EXISTS feeds");
            db.execSQL("DROP TABLE IF EXISTS posts");
            onCreate(db);
        }

        @Override
        public void onDowngrade(SQLiteDatabase db, int i, int i2) {
            db.execSQL("DROP TABLE IF EXISTS feeds");
            db.execSQL("DROP TABLE IF EXISTS posts");
            onCreate(db);
        }
    }
}
