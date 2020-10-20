package kunong.android.library.database;

import java.util.List;

public class DBTableProperty {
    private String mTableName;
    private List<DBField> mPrimaryKeyList;
    private List<DBField> mFieldList;
    private String[] mPrimaryKeysName;
    private String[] mFieldsName;

    public List<DBField> getPrimaryKeyList() {
        return mPrimaryKeyList;
    }

    public void setPrimaryKeyList(List<DBField> primaryKeyList) {
        mPrimaryKeyList = primaryKeyList;
    }

    public List<DBField> getFieldList() {
        return mFieldList;
    }

    public void setFieldList(List<DBField> fieldList) {
        mFieldList = fieldList;
    }

    public String[] getPrimaryKeysName() {
        return mPrimaryKeysName;
    }

    public void setPrimaryKeysName(String[] primaryKeysName) {
        mPrimaryKeysName = primaryKeysName;
    }

    public String[] getFieldsName() {
        return mFieldsName;
    }

    public void setFieldsName(String[] fieldsName) {
        mFieldsName = fieldsName;
    }

    public String getTableName() {
        return mTableName;
    }

    public void setTableName(String tableName) {
        mTableName = tableName;
    }

}
