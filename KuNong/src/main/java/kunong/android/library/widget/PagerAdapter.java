package kunong.android.library.widget;

import java.util.ArrayList;
import java.util.List;

import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;

public abstract class PagerAdapter extends androidx.viewpager.widget.PagerAdapter {

	private SparseArray<List<View>> mCacheViewLists = new SparseArray<>();
	private SparseArray<View> mViews = new SparseArray<>();
	private ViewGroup mContainer;

	@Override
	public void startUpdate(ViewGroup container) {
		super.startUpdate(container);

		mContainer = container;
	}

	@Override
	public final View instantiateItem(ViewGroup container, int position) {
		View view = getView(position, container);

		mViews.put(position, view);

		// Update view before add view to container.
		bindingView(position, view, container);

		container.addView(view);

		afterContainerAddView(position, view, container);

		return view;
	}

	@Override
	public void destroyItem(ViewGroup container, int position, Object object) {
		View view = (View) object;

		mViews.remove(position);

		// Cache view before remove view.
		cacheView(getItemViewType(position), view);

		container.removeView(view);
	}

	@Override
	public boolean isViewFromObject(View view, Object object) {
		return view == object;
	}

	private View getView(int position, ViewGroup container) {
		List<View> cacheViewList = getCacheViewList(getItemViewType(position));

		if (cacheViewList.size() == 0) {
			return createView(position, container);
		} else {
			return cacheViewList.remove(0);
		}
	}

	private void cacheView(int type, View view) {
		List<View> cacheViewList = getCacheViewList(type);

		cacheViewList.add(view);
	}

	public int getViewTypeCount() {
		return 1;
	}

	public int getItemViewType(int position) {
		return 0;
	}

	public void refresh() {
		if (mContainer != null) {
			for (int i = 0, n = mViews.size(); i < n; i++) {
				int position = mViews.keyAt(i);
				View view = mViews.valueAt(i);

				bindingView(position, view, mContainer);
			}
		}
	}

	private List<View> getCacheViewList(int type) {
		List<View> cacheViewList = mCacheViewLists.get(type);

		if (cacheViewList == null) {
			cacheViewList = new ArrayList<>();

			mCacheViewLists.put(type, cacheViewList);
		}

		return cacheViewList;
	}

	public View getViewDisplaying(int position) {
		return mViews.get(position);
	}

	public void afterContainerAddView(int position, View view, ViewGroup container) {
	}

	public abstract View createView(int position, ViewGroup container);

	public abstract void bindingView(int position, View view, ViewGroup container);
}
