package kunong.android.library.database;

class DBField {
    String name;
    java.lang.reflect.Field field;
    Annotation.DBFieldType type = Annotation.DBFieldType.NONE;
    int order;

    protected DBField(String name, java.lang.reflect.Field field, Annotation.DBFieldType type, int order) {
        this.name = name;
        this.field = field;
        this.type = type;
        this.order = order;
    }

}
