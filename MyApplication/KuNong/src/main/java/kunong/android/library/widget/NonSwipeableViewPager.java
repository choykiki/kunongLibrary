package kunong.android.library.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import androidx.viewpager.widget.ViewPager;

public class NonSwipeableViewPager extends ViewPager {

	private Mode mMode = Mode.NON_SWIPEABLE;

	public NonSwipeableViewPager(Context context) {
		super(context);
	}

	public NonSwipeableViewPager(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent event) {
		if (mMode == Mode.SWIPEABLE) {
			try {
				return super.onInterceptTouchEvent(event);
			} catch (IllegalArgumentException e) {
				return false;
			}
		}

		// Never allow swiping to switch between pages
		return mMode == Mode.NON_SWIPEABLE_AND_CONSUME_TOUCH;
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (mMode == Mode.SWIPEABLE)
			return super.onTouchEvent(event);

		// Never allow swiping to switch between pages
		return mMode == Mode.NON_SWIPEABLE_AND_CONSUME_TOUCH;
	}

	public Mode getMode() {
		return mMode;
	}

	public void setMode(Mode mode) {
		mMode = mode;
	}

	public enum Mode {
		SWIPEABLE, NON_SWIPEABLE, NON_SWIPEABLE_AND_CONSUME_TOUCH
	}
}
