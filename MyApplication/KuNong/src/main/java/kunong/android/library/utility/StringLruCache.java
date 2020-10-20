package kunong.android.library.utility;

import androidx.collection.LruCache;

import java.io.UnsupportedEncodingException;

public class StringLruCache extends LruCache<String, String> {

    public StringLruCache(int maxSize) {
        super(maxSize);
    }

    @Override
    protected int sizeOf(String key, String value) {
        try {
            return value.getBytes("UTF-8").length;
        } catch (UnsupportedEncodingException e) {
            return super.sizeOf(key, value);
        }
    }
}
