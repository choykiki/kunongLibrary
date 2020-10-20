package kunong.android.library.database;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import kunong.android.library.utility.ThrowableCallback;

public class DBHelper extends SQLiteOpenHelper {

    private static DBHelper mInstance = null;

    private final String dbPath;
    private final Context mContext;
    private final String mDatabasePath;
    private final int mDatabaseVersion;
    private SQLiteDatabase db;
    private OnDatabaseUpgradeListener mListener;

    public static DBHelper getInstance() {
        return init(null, null, -1, null);
    }

    /**
     * Need to call init() before call getInstance() method.
     *
     * @param context         Application context.
     * @param databasePath    Path of your SQLite database file in the assets folder. Example, demo.sqlite.
     * @param databaseVersion Set your current SQLite database version.
     * @param listener        Callback when the database need to update.
     * @return
     */
    public static DBHelper init(Context context, String databasePath, int databaseVersion, OnDatabaseUpgradeListener listener) {
        if (mInstance == null) {
            mInstance = new DBHelper(context, databasePath, databaseVersion);

            mInstance.mListener = listener;
            mInstance.getDatabase();
        }

        mInstance.mListener = null;

        return mInstance;
    }

    public SQLiteDatabase getDatabase() {
        if (db == null) {
            db = openDatabase();
        }

        return db;
    }

    private DBHelper(Context context, String databasePath, int databaseVersion) {
        super(context, databasePath, null, databaseVersion);

        mContext = context;
        mDatabasePath = databasePath;
        mDatabaseVersion = databaseVersion;

        dbPath = mContext.getDatabasePath(databasePath).getAbsolutePath();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    @Override
    public synchronized void close() {
        if (db != null)
            db.close();
        super.close();
    }

    public void copyDatabase() {
        try {
            InputStream inputFile = mContext.getAssets().open(mDatabasePath);
            OutputStream outputFile = new FileOutputStream(dbPath);
            byte buffer[] = new byte[1024];

            int length;

            while ((length = inputFile.read(buffer)) > 0) {
                outputFile.write(buffer, 0, length);
            }

            outputFile.flush();
            outputFile.close();
            inputFile.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private SQLiteDatabase createDatabase() {
        getWritableDatabase();

        // Copy database asset to device.
        copyDatabase();

        // Lets throw runtime exception if still not open database.
        SQLiteDatabase db = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READWRITE);

        // Set database version.
        db.setVersion(mDatabaseVersion);

        return db;
    }

    public synchronized SQLiteDatabase openDatabase() {
        SQLiteDatabase db = null;
        int oldVersion = 0;
        int newVersion = 0;

        try {
            db = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READWRITE);

            if (db.getVersion() == 0) {
                db.setVersion(1);
            }

            oldVersion = db.getVersion();

            // Set database version.
            getWritableDatabase();

            newVersion = db.getVersion();
        } catch (SQLException e) {
            // Database not found. Try to create a new one.
        }

        // Check database version.
        if (db != null && oldVersion < newVersion) {
            if (mListener != null) {
                mListener.onUpgrade(db, oldVersion, newVersion);
            }
        }

        if (db == null) {
            db = createDatabase();
        }

        return db;
    }

    public SQLiteDatabase recreateDatabase() {
        db = createDatabase();

        return db;
    }

    public void transaction(ThrowableCallback<SQLiteDatabase> action) {
        synchronized (db) {
            db.beginTransaction();

            try {
                action.complete(db);

                db.setTransactionSuccessful();
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                db.endTransaction();
            }
        }
    }

    public interface OnDatabaseUpgradeListener {
        void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion);
    }
}
