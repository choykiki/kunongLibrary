package kunong.android.library.database;

import java.util.HashMap;

public class DBCacheManager {

	private static DBCacheManager instance;
	private final HashMap<String, DBCacheTable> tableMap = new HashMap<>();

	private DBCacheManager() {
	}

	public static DBCacheManager getInstance() {
		if (instance == null)
			instance = new DBCacheManager();

		return instance;
	}

	public void addCache(DBTable object) {
		String tableName = object.getClass().getName();

		if (tableMap.get(tableName) == null)
			tableMap.put(tableName, new DBCacheTable(tableName));

		DBCacheTable cacheTable = tableMap.get(tableName);
		cacheTable.addCache(object);
	}

	public void removeCache(DBTable object) {
		String tableName = object.getClass().getName();

		DBCacheTable cacheTable = tableMap.get(tableName);
		if (cacheTable != null) {
			cacheTable.removeCache(object);
		}
	}

	public DBTable getCache(Class<? extends DBTable> cls, Object[] keysValue) {
		String tableName = cls.getName();
		DBCacheTable cacheTable = tableMap.get(tableName);

		if (cacheTable != null) {
			return cacheTable.getCache(keysValue);
		}

		return null;
	}

	public void removeCacheForTable(Class<? extends DBTable> cls) {
		String tableName = cls.getName();

		tableMap.remove(tableName);
	}

	public void reset() {

	}

	protected class DBCacheTable {
		private String name;
		private HashMap<String, DBTable> rowMap = new HashMap<String, DBTable>();

		public DBCacheTable(String name) {
			this.name = name;
		}

		public void addCache(DBTable object) {
			String key = getKey(object);

			if (key.length() == 0)
				return;

			rowMap.put(key, object);
		}

		public void removeCache(DBTable object) {
			String key = getKey(object);

			rowMap.remove(key);
		}

		public DBTable getCache(Object[] keysValue) {
			String key = getKey(keysValue);

			return rowMap.get(key);
		}

		private String getKey(Object[] keysValue) {
			String key = "";

			for (int i = 0; i < keysValue.length; i++) {
				if (i > 0)
					key += "@";
				key += keysValue[i];
			}

			return key;
		}

		private String getKey(DBTable object) {
			return getKey(object.getPrimaryKeysValue());
		}

		public String getName() {
			return name;
		}

	}

}
