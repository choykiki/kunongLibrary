package kunong.android.library.fragment;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.ImageView;

import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;
import com.facebook.rebound.BaseSpringSystem;
import com.facebook.rebound.OrigamiValueConverter;
import com.facebook.rebound.SimpleSpringListener;
import com.facebook.rebound.Spring;
import com.facebook.rebound.SpringConfig;
import com.facebook.rebound.SpringSystem;
import com.facebook.rebound.SpringUtil;
import com.koushikdutta.ion.Ion;
import com.nineoldandroids.view.ViewPropertyAnimator;

import kunong.android.library.R;
import kunong.android.library.helper.PixelHelper;
import kunong.android.library.helper.ViewHelper;
import kunong.android.library.widget.NonSwipeableViewPager;
import kunong.android.library.widget.PagerAdapter;
import uk.co.senab.photoview.PhotoViewAttacher;

public class ImageSliderFragment extends Fragment {

    private NonSwipeableViewPager mViewPager;
    private View mOverlayView;

    private ImageAdapter mAdapter;
    private ImageSliderable mImageSliderable;
    private BaseSpringSystem mSpringSystem;
    private Spring mScaleSpring;
    private int mSelectedPosition = -1;
    private int mAnimatingPosition = -1;
    private Runnable mAnimatingRunnable;
    private boolean mInitialized;
    private boolean mDismissing;
    private boolean mIsPageChangeListenerEnable = true;

    public static ImageSliderFragment newInstance() {
        ImageSliderFragment fm = new ImageSliderFragment();

        return fm;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (getParentFragment() == null) {
            mImageSliderable = (ImageSliderable) activity;
        } else {
            mImageSliderable = (ImageSliderable) getParentFragment();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.image_slider_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mViewPager = (NonSwipeableViewPager) view.findViewById(R.id.viewPager);
        mOverlayView = view.findViewById(R.id.overlay);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Setup spring system.
        mSpringSystem = SpringSystem.create();
        mScaleSpring = mSpringSystem.createSpring();
        mScaleSpring.setSpringConfig(new SpringConfig(OrigamiValueConverter.tensionFromOrigamiValue(50), OrigamiValueConverter.frictionFromOrigamiValue(9)));

        mAdapter = new ImageAdapter();

        mViewPager.setAdapter(mAdapter);
        mViewPager.setPageMargin(PixelHelper.dpToPx(getActivity(), 8));

        setListeners();

        mInitialized = true;

        mImageSliderable.onImageSliderInitialized(this);
    }

    @Override
    public void onDestroy() {
        mScaleSpring.destroy();
        mScaleSpring = null;

        super.onDestroy();
    }

    public boolean isInitialized() {
        return mInitialized;
    }

    private void setListeners() {
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {

            @Override
            public void onPageSelected(int position) {
                if (mSelectedPosition != position) {
                    if (mSelectedPosition != -1) {
                        showThumbnailView(mSelectedPosition);
                    }

                    getController().onPageSelected(position);

                    if (mIsPageChangeListenerEnable) {
                        hideThumbnailView(position);
                    }

                    mSelectedPosition = position;
                }
            }
        });
    }

    public void show(final int position) {
        mDismissing = false;

        getView().setVisibility(View.VISIBLE);

        com.nineoldandroids.view.ViewHelper.setAlpha(mOverlayView, 0);

        // Enable swipe of view pager.
        mViewPager.setMode(NonSwipeableViewPager.Mode.SWIPEABLE);

        mSelectedPosition = position;

        mAdapter.refresh();

        mIsPageChangeListenerEnable = false;
        mViewPager.setCurrentItem(position, false);
        mIsPageChangeListenerEnable = true;

        Runnable runnable = new Runnable() {

            @Override
            public void run() {
                ViewPropertyAnimator.animate(mOverlayView).alpha(1).setDuration(250);

                // Animate full image.
                ViewHolder holder = (ViewHolder) mAdapter.getViewDisplaying(position).getTag();
                final View view = holder.imageView;

                final Rect thumbnailRect = getThumbnailRect(position);
                final Rect fullscreenRect = getFullscreenRect(position);

                final MarginLayoutParams params = (MarginLayoutParams) view.getLayoutParams();
                params.width = thumbnailRect.width();
                params.height = thumbnailRect.height();
                params.leftMargin = thumbnailRect.left;
                params.topMargin = thumbnailRect.top;
                view.requestLayout();

                mScaleSpring.removeAllListeners();

                mScaleSpring.addListener(new SimpleSpringListener() {

                    @Override
                    public void onSpringActivate(Spring spring) {
                        // Hide thumbnail image.
                        hideThumbnailView(position);
                    }

                    @Override
                    public void onSpringUpdate(Spring spring) {
                        float value = (float) spring.getCurrentValue();

                        params.width = (int) SpringUtil.mapValueFromRangeToRange(value, 0, 1, thumbnailRect.width(), fullscreenRect.width());
                        params.height = (int) SpringUtil.mapValueFromRangeToRange(value, 0, 1, thumbnailRect.height(), fullscreenRect.height());
                        params.leftMargin = (int) SpringUtil.mapValueFromRangeToRange(value, 0, 1, thumbnailRect.left, fullscreenRect.left);
                        params.topMargin = (int) SpringUtil.mapValueFromRangeToRange(value, 0, 1, thumbnailRect.top, fullscreenRect.top);

                        view.requestLayout();
                    }

                    @Override
                    public void onSpringAtRest(Spring spring) {
                        mScaleSpring.removeListener(this);

                        // Reset layout params.
                        setViewLayoutToMatchParent(holder.imageView);

                        holder.photoViewAttacher = new PhotoViewAttacher(holder.imageView);
                    }
                });

                mScaleSpring.setEndValue(1);
            }
        };

        View viewDisplaying = mAdapter.getViewDisplaying(position);

        if (viewDisplaying == null) {

            // Should animate after view at the position is displaying.
            mAnimatingPosition = position;
            mAnimatingRunnable = runnable;

        } else {

            runnable.run();

        }
    }

    public void dismiss() {
        if (mDismissing)
            return;

        mDismissing = true;

        final int position = mViewPager.getCurrentItem();

        ViewPropertyAnimator.animate(mOverlayView).alpha(0).setDuration(250);

        // Disable swipe of view pager.
        mViewPager.setMode(NonSwipeableViewPager.Mode.NON_SWIPEABLE_AND_CONSUME_TOUCH);

        // Hide thumbnail image.
        hideThumbnailView(position);

        // Animate full image.
        ViewHolder holder = (ViewHolder) mAdapter.getViewDisplaying(position).getTag();
        final View view = holder.imageView;

        final Rect thumbnailRect = getThumbnailRect(position);
        final Rect fullscreenRect;

        if (holder.photoViewAttacher != null) {
            RectF rectF = holder.photoViewAttacher.getDisplayRect();

            if (rectF != null) {
                fullscreenRect = new Rect((int) rectF.left, (int) rectF.top, (int) rectF.right, (int) rectF.bottom);
            } else {
                fullscreenRect = getFullscreenRect(position);
            }

            holder.photoViewAttacher.cleanup();
        } else {
            fullscreenRect = getFullscreenRect(position);
        }

        holder.imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

        final MarginLayoutParams params = (MarginLayoutParams) view.getLayoutParams();
        params.width = fullscreenRect.width();
        params.height = fullscreenRect.height();
        params.leftMargin = fullscreenRect.left;
        params.topMargin = fullscreenRect.top;
        view.requestLayout();

        mScaleSpring.removeAllListeners();

        mScaleSpring.addListener(new SimpleSpringListener() {
            @Override
            public void onSpringUpdate(Spring spring) {
                float value = (float) spring.getCurrentValue();

                params.width = (int) SpringUtil.mapValueFromRangeToRange(value, 0, 1, thumbnailRect.width(), fullscreenRect.width());
                params.height = (int) SpringUtil.mapValueFromRangeToRange(value, 0, 1, thumbnailRect.height(), fullscreenRect.height());
                params.leftMargin = (int) SpringUtil.mapValueFromRangeToRange(value, 0, 1, thumbnailRect.left, fullscreenRect.left);
                params.topMargin = (int) SpringUtil.mapValueFromRangeToRange(value, 0, 1, thumbnailRect.top, fullscreenRect.top);

                view.requestLayout();
            }

            @Override
            public void onSpringAtRest(Spring spring) {
                mScaleSpring.removeListener(this);

                showThumbnailView(position);

                ViewHelper.addOnPreDrawListener(getView(), v -> {
                    getView().setVisibility(View.INVISIBLE);

                    mImageSliderable.onImageSliderDismiss(ImageSliderFragment.this);
                });

            }
        });

        mScaleSpring.setEndValue(0);
    }

    private Rect getThumbnailRect(int position) {
        View thumbnailView = getController().getViewDisplaying(position);
        final Rect rect = ViewHelper.getViewRect(thumbnailView);

        Rect containerRect = ViewHelper.getViewRect(mViewPager);
        rect.top -= containerRect.top;
        rect.bottom -= containerRect.top;

        return rect;
    }

    private Rect getFullscreenRect(int position) {
        final Point imageSize = getController().getImageSize(position);
        final float pageRatio = 1f * mViewPager.getWidth() / mViewPager.getHeight();
        final float imageRatio = 1f * imageSize.x / imageSize.y;
        final Point size;

        if (imageRatio > pageRatio) {
            size = new Point(mViewPager.getWidth(), (int) (mViewPager.getWidth() / imageRatio));
        } else {
            size = new Point((int) (mViewPager.getHeight() * imageRatio), mViewPager.getHeight());
        }

        final Point margin = new Point((int) (mViewPager.getWidth() / 2f - size.x / 2f), (int) (mViewPager.getHeight() / 2f - size.y / 2f));

        return new Rect(margin.x, margin.y, margin.x + size.x, margin.y + size.y);
    }

    private void setViewLayoutToMatchParent(View view) {
        MarginLayoutParams params = (MarginLayoutParams) view.getLayoutParams();
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        params.height = ViewGroup.LayoutParams.MATCH_PARENT;
        params.leftMargin = 0;
        params.topMargin = 0;
        view.requestLayout();
    }

    protected void onViewBinded(int position, View view) {
        if (position == mAnimatingPosition) {

            mAnimatingRunnable.run();

            mAnimatingPosition = -1;
            mAnimatingRunnable = null;
        }
    }

    private void showThumbnailView(int position) {
        View view = getController().getViewDisplaying(position);

        view.setVisibility(View.VISIBLE);
    }

    private void hideThumbnailView(int position) {
        View view = getController().getViewDisplaying(position);

        view.setVisibility(View.INVISIBLE);
    }

    public int getCurrentPosition() {
        return mSelectedPosition;
    }

    public ImageSliderController getController() {
        return mImageSliderable.getImageSliderController();
    }

    public interface ImageSliderable {

        public void onImageSliderInitialized(ImageSliderFragment imageSliderFragment);

        public void onImageSliderDismiss(ImageSliderFragment imageSliderFragment);

        public ImageSliderController getImageSliderController();
    }

    public static abstract class ImageSliderController {
        public abstract int getCount();

        public abstract Object getSourceImage(int position);

        public abstract Point getImageSize(int position);

        public abstract View getViewDisplaying(int position);

        public void onPageSelected(int position) {
        }
    }

    protected class ImageAdapter extends PagerAdapter {

        @Override
        public int getCount() {
            return getController().getCount();
        }

        @Override
        public View createView(int position, ViewGroup container) {
            View view = getActivity().getLayoutInflater().inflate(R.layout.image_slider_item, container, false);

            ViewHolder holder = new ViewHolder();

            holder.imageView = (ImageView) view.findViewById(R.id.imageView);

            view.setTag(holder);

            return view;
        }

        @Override
        public void bindingView(int position, View view, ViewGroup container) {
            final ViewHolder holder = (ViewHolder) view.getTag();
            Object sourceImage = getController().getSourceImage(position);

            if (mAnimatingPosition == position) {
                holder.imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            } else {
                // Reset layout params.
                setViewLayoutToMatchParent(holder.imageView);

                if (holder.photoViewAttacher == null) {
                    holder.photoViewAttacher = new PhotoViewAttacher(holder.imageView);
                }
            }

            if (sourceImage instanceof CharSequence) {

                Ion.with(holder.imageView).load(sourceImage.toString()).setCallback((e, imageView) -> {
                    if (mAnimatingPosition != position) {
                        holder.photoViewAttacher.update();
                    }
                });

            } else if (sourceImage instanceof Bitmap) {

                holder.imageView.setImageBitmap((Bitmap) sourceImage);

                if (holder.photoViewAttacher != null) {
                    holder.photoViewAttacher.update();
                }

            } else {
                throw new ClassCastException("getSourceImage() should be CharSequence or Bitmap.");
            }
        }

        @Override
        public void afterContainerAddView(final int position, View view, ViewGroup container) {
            ViewHelper.addOnPreDrawListener(view, v -> onViewBinded(position, v));
        }
    }

    protected class ViewHolder {
        ImageView imageView;
        PhotoViewAttacher photoViewAttacher;
    }
}
