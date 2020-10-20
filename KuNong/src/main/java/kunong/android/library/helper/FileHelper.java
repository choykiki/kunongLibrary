package kunong.android.library.helper;

import android.net.Uri;
import android.webkit.MimeTypeMap;

import java.io.File;

public class FileHelper {

    public static String getMimeType(File file) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(file).toString());

        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }

        return type;
    }

}
