package kunong.android.library.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.ListView;

/**
 * Created by Macmini on 10/1/14 AD.
 */
public class ExpandableListView extends ListView {

    private boolean mExpanded = true;
    private boolean mLimitHeight = false;

    public ExpandableListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // HACK! TAKE THAT ANDROID!
        if (mExpanded) {
            int maxHeight = Integer.MAX_VALUE;

            if (mLimitHeight) {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);

                maxHeight = getMeasuredHeight();
            }

            // Calculate entire height by providing a very large height hint.
            // But do not use the highest 2 bits of this integer; those are
            // reserved for the MeasureSpec mode.
            int expandSpec = MeasureSpec.makeMeasureSpec(Integer.MAX_VALUE >> 2, MeasureSpec.AT_MOST);
            super.onMeasure(widthMeasureSpec, expandSpec);

            ViewGroup.LayoutParams params = getLayoutParams();
            params.height = Math.min(maxHeight, getMeasuredHeight());
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    public boolean isExpanded() {
        return mExpanded;
    }

    public void setExpanded(boolean expanded) {
        this.mExpanded = expanded;
    }

    public boolean isLimitHeight() {
        return mLimitHeight;
    }

    public void setLimitHeight(boolean limitHeight) {
        mLimitHeight = limitHeight;
    }
}
