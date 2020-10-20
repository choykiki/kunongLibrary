package kunong.android.library.helper;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import kunong.android.library.concurrent.EventLocker;

/**
 * Created by Macmini on 10/3/14 AD.
 */
public class ViewHelper {

    public static void addOnPreDrawListener(View view, OnLayoutUpdateListener listener) {
        view.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {

            @Override
            public boolean onPreDraw() {
                view.getViewTreeObserver().removeOnPreDrawListener(this);

                listener.onLayoutUpdate(view);

                return true;
            }
        });
    }

    public static void addOnPreDrawListener(Runnable listener, View... views) {
        EventLocker eventLocker = new EventLocker();

        for (View view : views) {
            final String key = String.valueOf(view.hashCode());

            eventLocker.lock(key);

            addOnPreDrawListener(view, v -> eventLocker.unlock(key));
        }

        eventLocker.run(listener);
    }

    public static void addOnLayoutUpdateListener(View view, OnLayoutUpdateListener listener) {
        view.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                view.removeOnLayoutChangeListener(this);

                addOnPreDrawListener(view, v1 -> listener.onLayoutUpdate(v));
            }
        });
    }

    public static void addOnLayoutUpdateListener(Runnable listener, View... views) {
        EventLocker eventLocker = new EventLocker();

        for (View view : views) {
            final String key = String.valueOf(view.hashCode());

            eventLocker.lock(key);

            addOnLayoutUpdateListener(view, v -> eventLocker.unlock(key));
        }

        eventLocker.run(listener);
    }

    public static void setTopMargin(View view, int topMargin) {
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        params.topMargin = topMargin;

        view.requestLayout();
    }

    public static Rect getViewRect(View view) {
        int[] location = new int[2];

        view.getLocationOnScreen(location);

        return new Rect(location[0], location[1], location[0] + view.getWidth(), location[1] + view.getHeight());
    }

    public static View getViewByPosition(int position, ListView listView) {
        final int firstListItemPosition = listView.getFirstVisiblePosition();
        final int lastListItemPosition = firstListItemPosition + listView.getChildCount() - 1;

        if (position < firstListItemPosition || position > lastListItemPosition) {
            return listView.getAdapter().getView(position, null, listView);
        } else {
            final int childIndex = position - firstListItemPosition;

            return listView.getChildAt(childIndex);
        }
    }

    /**
     *
     * @param position the position in adapter.
     * @param listView
     */
    public static void refreshViewByPosition(int position, ListView listView) {
        final int firstListItemPosition = listView.getFirstVisiblePosition();
        final int lastListItemPosition = firstListItemPosition + listView.getChildCount() - 1;

        position += listView.getHeaderViewsCount();

        if (position >= firstListItemPosition && position <= lastListItemPosition) {
            final int childIndex = position - firstListItemPosition;

            View view = listView.getChildAt(childIndex);

            listView.getAdapter().getView(position, view, listView);
        }
    }

    public static Rect getRectInView(ViewGroup parent, View view) {
        int[] viewLocation = new int[2];
        view.getLocationInWindow(viewLocation);

        int[] rootLocation = new int[2];
        parent.getLocationInWindow(rootLocation);

        int relativeLeft = viewLocation[0] - rootLocation[0];
        int relativeTop = viewLocation[1] - rootLocation[1];

        return new Rect(relativeLeft, relativeTop, relativeLeft + view.getWidth(), relativeTop + view.getHeight());
    }

    public static List<View> getChildren(ViewGroup parent) {
        return findViews(parent, View.class, false);
    }

    public static <T extends View> List<T> findViews(View root, Class<T> cls) {
        return findViews(root, cls, true);
    }

    public static <T extends View> List<T> findViews(View root, Class<T> cls, boolean recursive) {
        List<T> viewList = new ArrayList<>();

        if (root != null && ViewGroup.class.isInstance(root)) {

            ViewGroup viewGroup = (ViewGroup) root;
            int childCount = viewGroup.getChildCount();

            for (int i = 0; i < childCount; i++) {
                View view = viewGroup.getChildAt(i);

                if (view == null)
                    continue;

                if (cls.isInstance(view)) {
                    viewList.add(cls.cast(view));
                } else if (recursive) {
                    viewList.addAll(findViews(view, cls, true));
                }
            }
        }

        return viewList;
    }


    public static Bitmap getBitmapFromView(View view) {
        return getBitmapFromView(view, true);
    }

    public static Bitmap getBitmapFromView(View view, boolean layoutView) {
        if (layoutView) {
            view.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        }

        Bitmap bitmap = Bitmap.createBitmap(layoutView ? view.getMeasuredWidth() : view.getWidth(), layoutView ? view.getMeasuredHeight() : view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        if (layoutView) {
            view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
        }

        view.draw(canvas);

        return bitmap;
    }

    public interface OnLayoutUpdateListener {
        void onLayoutUpdate(View v);
    }
}
