package kunong.android.library.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificationCenter {

    private static final Map<String, List<OnNotifyListener>> notifyMap = new HashMap<>();

    public static void addListener(String event, OnNotifyListener listener) {
        List<OnNotifyListener> notify = notifyMap.get(event);

        if (notify == null) {
            notify = new ArrayList<>();
            notifyMap.put(event, notify);
        }

        notify.add(listener);
    }

    public static void dispatchEvent(String event) {
        dispatchEvent(event, null);
    }

    public static void dispatchEvent(String event, Map<String, Object> objects) {
        List<OnNotifyListener> notify = notifyMap.get(event);

        if (notify != null) {
            for (OnNotifyListener listner : notify) {
                listner.onNotify(objects);
            }
        }
    }

    public static void removeListener(String event, OnNotifyListener listener) {
        List<OnNotifyListener> notify = notifyMap.get(event);

        if (notify != null) {
            notify.remove(listener);
        }
    }

    public static void removeEvent(String event) {
        notifyMap.remove(event);
    }

    public interface OnNotifyListener {
        void onNotify(Map<String, Object> objects);
    }
}
