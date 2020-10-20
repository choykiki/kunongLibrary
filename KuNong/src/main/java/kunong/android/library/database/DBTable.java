package kunong.android.library.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.SparseArray;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import java8.util.stream.StreamSupport;
import kunong.android.library.database.Annotation.DBFieldType;
import kunong.android.library.database.Annotation.Field;
import kunong.android.library.database.Annotation.Id;
import kunong.android.library.database.Annotation.Table;

public abstract class DBTable implements Serializable, Cloneable {

    private static final long serialVersionUID = 3816235107361342733L;
    private static final SparseArray<DBTableProperty> mTableProperties = new SparseArray<>();
    private transient DBTableProperty mProperty;

    public static <T extends DBTable> Object[] getPrimaryKeysValue(Class<T> cls, Cursor cursor) {
        DataBundle data = getDataBundle(cursor);
        T object = createObjectFromTable(cls);

        // Update primary keys to object.
        object.mapDataToFields(data, true, true);

        return object.getPrimaryKeysValue();
    }

    private static <T extends DBTable> T getCache(Class<T> cls, Object... values) {
        return cls.cast(DBCacheManager.getInstance().getCache(cls, values));
    }

    public static <T extends DBTable> T findByPrimaryKeys(Class<T> cls, Object... keyValues) {
        List<Object> valueList = new ArrayList<>();

        // Check if values is array.
        if (keyValues.length == 1 && keyValues[0] != null && keyValues[0].getClass().isArray()) {
            Object values = keyValues[0];

            int length = Array.getLength(values);

            for (int i = 0; i < length; i++) {
                valueList.add(Array.get(values, i));
            }
        } else {
            for (Object value : keyValues) {
                valueList.add(value);
            }
        }

        Object[] values = valueList.toArray();

        T object = getCache(cls, values);
        if (object != null)
            return object;

        DBTable table = createObjectFromTable(cls);
        String condition = "";
        String[] primaryKeysName = table.getPrimaryKeysName();

        for (int i = 0; i < primaryKeysName.length; i++) {
            if (i > 0)
                condition += " AND ";
            condition += String.format("`%s` = '%s'", primaryKeysName[i], values[i]);
        }

        List<T> result = newQuery().withCondition(condition).query(cls);

        if (result.size() > 0) {
            object = result.get(0);

            return object;
        }

        return null;
    }

    public static <T extends DBTable> List<T> findAll(Class<T> cls) {
        return newQuery().query(cls);
    }

    public static <T extends DBTable> List<T> findAll(Class<T> cls, Integer limit) {
        return newQuery().limit(limit).query(cls);
    }

    public static <T extends DBTable> Cursor findAllCursor(Class<T> cls) {
        return newQuery().getCursor(cls);
    }

    public static <T extends DBTable> Cursor findAllCursor(Class<T> cls, Integer limit) {
        return newQuery().limit(limit).getCursor(cls);
    }

    @Deprecated
    public static <T extends DBTable> List<T> findWithCondition(Class<T> cls, String condition) {
        return newQuery().withCondition(condition).query(cls);
    }

    public static DBQuery newQuery() {
        return new DBQuery();
    }

    private static <T extends DBTable> Cursor getCursorByQuery(Class<T> cls, DBQuery dbQuery) {
        SQLiteDatabase db = DBHelper.getInstance().getDatabase();
        DBTable table = createObjectFromTable(cls);
        String orderBy = (dbQuery.orderBy == null) ? table.getDefaultOrderBy() : dbQuery.orderBy;
        String limit = (dbQuery.limit == null) ? null : dbQuery.limit.toString();
        Cursor cursor;

        synchronized (db) {
            cursor = db.query(table.getSafeTableName(), null, dbQuery.condition, null, dbQuery.groupBy, dbQuery.having, orderBy, limit);
        }

        return cursor;
    }

    private static <T extends DBTable> List<T> findByQuery(Class<T> cls, DBQuery dbQuery) {
        Cursor cursor = getCursorByQuery(cls, dbQuery);
        List<T> objectList = new ArrayList<>();

        while (cursor.moveToNext()) {
            T object = toObject(cls, cursor);

            objectList.add(object);
        }

        cursor.close();

        return objectList;
    }

    public static <T extends DBTable> T toObject(Class<T> cls, Cursor cursor) {
        return toObject(cls, cursor, null);
    }

    public static <T extends DBTable> T toObject(Class<T> cls, Cursor cursor, String aliasTableName) {
        String prefix = (aliasTableName != null && aliasTableName.length() > 0) ? (aliasTableName + "_") : "";
        DataBundle data = getDataBundle(cursor);
        T object = createObjectFromTable(cls);

        // Update primary keys to object.
        object.mapDataToFields(data, prefix, true, true);

        T cacheObject;
        if ((cacheObject = getCache(cls, object.getPrimaryKeysValue())) != null) {
            object = cacheObject;
        } else {
            // Cache object before update instance.
            object.cache();

            // Update fields to object.
            object.mapDataToFields(data, prefix, false, true);
        }

        return object;
    }

    public static DataBundle getDataBundle(Cursor cursor) {
        DataBundle data = new DataBundle();

        int size = cursor.getColumnCount();
        for (int i = 0; i < size; i++) {
            data.put(cursor.getColumnName(i), cursor.getString(i));
        }

        return data;
    }

    public static void delete(Class<? extends DBTable> cls, Object... values) {
        SQLiteDatabase db = DBHelper.getInstance().getDatabase();
        DBTable table = createObjectFromTable(cls);

        // Remove cache if already existed.
        DBTable cache = DBCacheManager.getInstance().getCache(cls, values);
        if (cache != null)
            cache.unCache();

        String condition = "";
        String[] primaryKeysName = table.getPrimaryKeysName();

        for (int i = 0; i < primaryKeysName.length; i++) {
            if (i > 0)
                condition += " AND ";
            condition += String.format("`%s` = '%s'", primaryKeysName[i], values[i]);
        }

        synchronized (db) {
            db.delete(table.getSafeTableName(), condition, null);
        }
    }

    public static void empty(Class<? extends DBTable> cls) {
        SQLiteDatabase db = DBHelper.getInstance().getDatabase();
        DBTable table = createObjectFromTable(cls);

        synchronized (db) {
            db.delete(table.getSafeTableName(), null, null);
        }

        // Clear cache for this table.
        DBCacheManager.getInstance().removeCacheForTable(cls);
    }

    public static <T extends DBTable> T createObjectFromTable(Class<T> cls) {
        try {
            return cls.getConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static int getTotalCount(Class<? extends DBTable> cls) {
        final SQLiteDatabase db = DBHelper.getInstance().getDatabase();
        DBTable table = createObjectFromTable(cls);
        Cursor cursor;

        synchronized (db) {
            cursor = db.rawQuery(String.format("SELECT COUNT(*) as `total` FROM `%s`", table.getTableName()), null);
        }

        cursor.moveToFirst();

        int totalCount = cursor.getInt(0);

        cursor.close();

        return totalCount;
    }

    public static int getMaxID(Class<? extends DBTable> cls) {
        final SQLiteDatabase db = DBHelper.getInstance().getDatabase();
        DBTable table = createObjectFromTable(cls);
        Cursor cursor;

        synchronized (db) {
            cursor = db.rawQuery(String.format("SELECT MAX(_id) as `total` FROM `%s` WHERE `language` = %s", table.getTableName()), null);
        }

        cursor.moveToFirst();

        int totalCount = cursor.getInt(0);

        cursor.close();

        return totalCount;
    }

    private static int getTotalCount(Class<? extends DBTable> cls, DBQuery dbQuery) {
        Cursor cursor = getCursorByQuery(cls, dbQuery);

        int totalCount = cursor.getCount();

        cursor.close();

        return totalCount;
    }

    public static List<String> getAliasFieldNames(Class<? extends DBTable> cls, String aliasTableName) {
        DBTable table = createObjectFromTable(cls);
        List<String> aliasNameList = new ArrayList<>();

        for (String fieldName : table.getPrimaryKeysName()) {
            String aliasName = String.format("%s.`%s` %s_%s", aliasTableName, fieldName, aliasTableName, fieldName);

            aliasNameList.add(aliasName);
        }

        for (String fieldName : table.getFieldsName()) {
            String aliasName = String.format("%s.`%s` %s_%s", aliasTableName, fieldName, aliasTableName, fieldName);

            aliasNameList.add(aliasName);
        }

        return aliasNameList;
    }

    private String parseTableName() {
        Class<? extends DBTable> cls = getClass();
        Table tableAnnotation = cls.getAnnotation(Table.class);
        if (tableAnnotation == null)
            throw new RuntimeException(String.format("{%s} must be annotated with {@Table}", cls.getName()));

        String defaultValue = "";
        try {
            defaultValue = (String) Table.class.getDeclaredMethod("value").getDefaultValue();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

        return tableAnnotation.value().equals(defaultValue) ? cls.getSimpleName().toLowerCase(Locale.ENGLISH) : tableAnnotation.value();
    }

    private void parseFields() {
        if (mProperty != null)
            return;

        final int tableKey = getClass().hashCode();

        synchronized (mTableProperties) {
            mProperty = mTableProperties.get(tableKey);

            if (mProperty != null)
                return;

            mProperty = new DBTableProperty();
        }

        synchronized (mProperty) {

            List<DBField> dbPrimaryKeyList = new ArrayList<>();
            List<DBField> dbFieldList = new ArrayList<>();
            Class<? extends DBTable> cls = getClass();

            String defaultIdValue = "";
            String defaultFieldValue = "";
            try {
                defaultIdValue = (String) Id.class.getDeclaredMethod("value").getDefaultValue();
                defaultFieldValue = (String) Field.class.getDeclaredMethod("value").getDefaultValue();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }

            // Loop all fields to check database-annotations.
            for (java.lang.reflect.Field field : cls.getDeclaredFields()) {
                field.setAccessible(true);

                Id idAnnotation = field.getAnnotation(Id.class);
                Field fieldAnnotation = field.getAnnotation(Field.class);

                // If it is primary key.
                if (idAnnotation != null) {
                    String idName = idAnnotation.value().equals(defaultIdValue) ? field.getName() : idAnnotation.value();
                    DBFieldType fieldType = idAnnotation.type();
                    int order = idAnnotation.order();

                    // Check if field is DBTable.
                    if (DBTable.class.isAssignableFrom(field.getType())) {
                        fieldType = DBFieldType.TABLE;
                    }

                    DBField dbField = new DBField(idName, field, fieldType, order);
                    dbPrimaryKeyList.add(dbField);
                }
                // Otherwise just normal field.
                else if (fieldAnnotation != null) {
                    String fieldName = fieldAnnotation.value().equals(defaultFieldValue) ? field.getName() : fieldAnnotation.value();
                    DBFieldType fieldType = fieldAnnotation.type();

                    // Check if field is DBTable.
                    if (DBTable.class.isAssignableFrom(field.getType())) {
                        fieldType = DBFieldType.TABLE;
                    }

                    DBField dbField = new DBField(fieldName, field, fieldType, 0);
                    dbFieldList.add(dbField);

                }
            }

            // Sorting primary keys by order.
            Collections.sort(dbPrimaryKeyList, (field1, field2) -> field1.order - field2.order);

            // Generates arrays for faster access.
            List<String> primaryKeyList = new ArrayList<>();
            List<String> fieldList = new ArrayList<>();

            for (DBField dbField : dbPrimaryKeyList) {
                primaryKeyList.add(dbField.name);
            }

            for (DBField dbField : dbFieldList) {
                fieldList.add(dbField.name);
            }

            // Converts array list to normal array.
            String[] primaryKeysName = new String[primaryKeyList.size()];
            primaryKeysName = primaryKeyList.toArray(primaryKeysName);

            String[] fieldsName = new String[fieldList.size()];
            fieldsName = fieldList.toArray(fieldsName);

            // Keep into DBTableProperty class.
            mProperty.setTableName(parseTableName());
            mProperty.setPrimaryKeyList(dbPrimaryKeyList);
            mProperty.setFieldList(dbFieldList);
            mProperty.setPrimaryKeysName(primaryKeysName);
            mProperty.setFieldsName(fieldsName);
        }

        mTableProperties.put(tableKey, mProperty);
    }

    public final String getTableName() {
        requireFieldParsed();

        return mProperty.getTableName();
    }

    private void requireFieldParsed() {
        parseFields();
    }

    public final String[] getPrimaryKeysName() {
        requireFieldParsed();

        return adjustFields(mProperty.getPrimaryKeysName());
    }

    public final String[] getFieldsName() {
        requireFieldParsed();

        return adjustFields(mProperty.getFieldsName());
    }

    private String[] adjustFields(String[] fields) {
        List<String> fieldList = new ArrayList<>();

        for (String field : fields) {
            String[] subFields = field.split(",");

            for (String subField : subFields) {
                fieldList.add(subField.trim());
            }
        }

        String[] adjustedFields = new String[fieldList.size()];

        return fieldList.toArray(adjustedFields);
    }

    private Object[] getBaseFieldsValue(List<DBField> dbFieldList) {
        List<Object> valueList = new ArrayList<>();

        for (DBField dbField : dbFieldList) {
            Object value = getDBFieldValue(dbField);

            if (value != null && value.getClass().isArray()) {
                int length = Array.getLength(value);

                for (int i = 0; i < length; i++) {
                    valueList.add(Array.get(value, i));
                }
            } else {
                valueList.add(value);
            }
        }

        return valueList.toArray();
    }

    private Object getDBFieldValue(DBField dbField) {
        Object value = null;

        switch (dbField.type) {
            case NONE:

                try {
                    value = dbField.field.get(this);

                    // Convert boolean type to 0 or 1.
                    if (value instanceof Boolean)
                        value = (Boolean) value ? 1 : 0;
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
                break;

            case TABLE:
                DBTable object = (DBTable) getFieldValue(dbField);

                if (object != null) {
                    value = object.getPrimaryKeysValue();
                } else {
                    Object[] nullKeys = new Object[dbField.name.split(",").length];

                    for (int i = 0; i < nullKeys.length; i++) {
                        nullKeys[i] = null;
                    }

                    value = nullKeys;
                }

                break;

            case DATE_TIMESTAMP:
                if (dbField.field.getType() == Date.class) {
                    Date date = (Date) getFieldValue(dbField);
                    value = date != null ? date.getTime() : null;
                }
                break;
        }

        return value;
    }

    private Object getFieldValue(DBField dbField) {
        try {
            return dbField.field.get(this);
        } catch (IllegalArgumentException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Deprecated
    public int getPrimaryKey() {
        return (Integer) getPrimaryKeysValue()[0];
    }

    public final Object[] getPrimaryKeysValue() {
        requireFieldParsed();

        return getBaseFieldsValue(mProperty.getPrimaryKeyList());
    }

    public final Object[] getFieldsValue() {
        requireFieldParsed();

        return getBaseFieldsValue(mProperty.getFieldList());
    }

    protected void onPrimaryKeysUpdate(DataBundle data) {
    }

    protected void onFieldsUpdate(DataBundle data) {
    }

    public String getDefaultOrderBy() {
        return "";
    }

    protected String getSafeTableName() {
        return String.format("`%s`", getTableName());
    }

    protected void unCache() {
        DBCacheManager.getInstance().removeCache(this);
    }

    protected void cache() {
        DBCacheManager.getInstance().addCache(this);
    }

    protected void mapDataToFields(DataBundle data, boolean mapPrimaryKey, boolean dispatchEvent) {
        mapDataToFields(data, "", mapPrimaryKey, dispatchEvent);
    }

    @SuppressWarnings("unchecked")
    protected void mapDataToFields(DataBundle data, String prefix, boolean mapPrimaryKey, boolean dispatchEvent) {
        requireFieldParsed();

        List<DBField> dbFieldList;
        if (mapPrimaryKey)
            dbFieldList = mProperty.getPrimaryKeyList();
        else
            dbFieldList = mProperty.getFieldList();

        for (DBField dbField : dbFieldList) {
            java.lang.reflect.Field field = dbField.field;
            Class<?> fieldType = field.getType();
            String fieldName = dbField.name;

            field.setAccessible(true);

            switch (dbField.type) {
                case NONE:
                    Object value;

                    fieldName = prefix + fieldName;

                    if (fieldType == Boolean.class || fieldType == Boolean.TYPE)
                        value = data.getBoolean(fieldName);
                    else if (fieldType == Byte.class || fieldType == Byte.TYPE)
                        value = data.getByte(fieldName);
                    else if (fieldType == Short.class || fieldType == Short.TYPE)
                        value = data.getShort(fieldName);
                    else if (fieldType == Integer.class || fieldType == Integer.TYPE)
                        value = data.getInt(fieldName);
                    else if (fieldType == Long.class || fieldType == Long.TYPE)
                        value = data.getLong(fieldName);
                    else if (fieldType == Float.class || fieldType == Float.TYPE)
                        value = data.getFloat(fieldName);
                    else if (fieldType == Double.class || fieldType == Double.TYPE)
                        value = data.getDouble(fieldName);
                    else
                        value = data.get(fieldName);

                    try {

                        if (fieldType.isPrimitive()) {
                            if (fieldType == Boolean.TYPE)
                                field.setBoolean(this, (Boolean) value);
                            else if (fieldType == Byte.TYPE)
                                field.setByte(this, (Byte) value);
                            else if (fieldType == Short.TYPE)
                                field.setShort(this, (Short) value);
                            else if (fieldType == Integer.TYPE)
                                field.setInt(this, (Integer) value);
                            else if (fieldType == Long.TYPE)
                                field.setLong(this, (Long) value);
                            else if (fieldType == Float.TYPE)
                                field.setFloat(this, (Float) value);
                            else if (fieldType == Double.TYPE)
                                field.setDouble(this, (Double) value);
                        } else {
                            field.set(this, value);
                        }

                    } catch (IllegalArgumentException | IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }

                    break;

                case TABLE:
                    DBTable table = null;

                    fieldName = prefix + fieldName;

                    if (data.get(fieldName) != null) {
                        table = DBTable.findByPrimaryKeys((Class<? extends DBTable>) fieldType, data.get(fieldName));
                    }

                    try {
                        field.set(this, table);
                    } catch (IllegalArgumentException | IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                    break;

                case DATE_TIMESTAMP:
                    if (field.getType() == Date.class) {
                        try {
                            Long time = data.getLong(prefix + fieldName);

                            if (time != null) {
                                field.set(this, new Date(time));
                            }
                        } catch (IllegalArgumentException | IllegalAccessException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    break;
            }

        }

        if (mapPrimaryKey) {
            // onPrimaryKeysUpdate(data);
        } else {
            onFieldsUpdate(data);
        }
    }

    protected void beforeCommit() {
    }

    public void commit() {
        commit(true);
    }

    public void commit(boolean keepCache) {
        beforeCommit();

        SQLiteDatabase db = DBHelper.getInstance().getDatabase();
        String[] primaryKeysName = getPrimaryKeysName();
        Object[] primaryKeysValue = getPrimaryKeysValue();
        String[] fieldsName = getFieldsName();
        Object[] fieldsValue = getFieldsValue();

        ContentValues values = new ContentValues();

        DBTable object = findByPrimaryKeys(getClass(), primaryKeysValue);
        boolean hasPrimaryKeys = object != null || !keyIsEmpty();
        if (hasPrimaryKeys) {
            for (int i = 0; i < primaryKeysName.length; i++)
                values.put(String.format("`%s`", primaryKeysName[i]), primaryKeysValue[i].toString());
        }

        for (int i = 0; i < fieldsName.length; i++) {
            values.put(String.format("`%s`", fieldsName[i]), fieldsValue[i] != null ? fieldsValue[i].toString() : null);
        }

        Integer id;
        synchronized (db) {
            id = (int) db.replace(getSafeTableName(), null, values);
        }

        if (!hasPrimaryKeys) {
            DataBundle data = new DataBundle();
            for (String keyName : primaryKeysName) {
                data.put(keyName, id.toString());
            }

            mapDataToFields(data, true, false);
        }

        // Cache object.
        if (keepCache) {
            cache();
        }
    }

    public void delete() {
        delete(getClass(), getPrimaryKeysValue());
    }

    public synchronized DBTable flush() {
        unCache();

        return findByPrimaryKeys(getClass(), getPrimaryKeysValue());
    }

    public boolean keyIsEmpty() {
        boolean empty = false;

        Object[] primaryKeysValue = getPrimaryKeysValue();
        for (Object value : primaryKeysValue) {
            if (value == null) {
                empty = true;
                break;
            }
        }

        return empty;
    }

    public static class DBQuery {
        private String condition;
        private String orderBy;
        private Integer limit;
        private String having;
        private String groupBy;

        private DBQuery() {
        }

        public DBQuery withCondition(String condition) {
            this.condition = condition;

            return this;
        }

        public DBQuery withCondition(String query, Object... args) {
            return withCondition(String.format(Locale.US, query, args));
        }

        public DBQuery orderBy(String orderBy) {
            this.orderBy = orderBy;

            return this;
        }

        public DBQuery limit(int limit) {
            this.limit = limit;

            return this;
        }

        public DBQuery having(String having) {
            this.having = having;

            return this;
        }

        public DBQuery groupBy(String groupBy) {
            this.groupBy = groupBy;

            return this;
        }

        public <T extends DBTable> int getCount(Class<T> cls) {
            return DBTable.getTotalCount(cls, this);
        }

        public <T extends DBTable> Cursor getCursor(Class<T> cls) {
            return DBTable.getCursorByQuery(cls, this);
        }

        public <T extends DBTable> List<T> query(Class<T> cls) {
            return DBTable.findByQuery(cls, this);
        }

        public <T extends DBTable> T singleQuery(Class<T> cls) {
            List<T> objects = limit(1).query(cls);

            return objects.size() > 0 ? objects.get(0) : null;
        }

        public <T extends DBTable> void delete(Class<T> cls) {
            StreamSupport.stream(query(cls)).forEach(DBTable::delete);
        }
    }
}
