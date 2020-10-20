package kunong.android.library.fragment;

import android.content.Intent;
import android.content.res.Resources;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import java.lang.reflect.Field;
import java.util.List;

import androidx.fragment.app.Fragment;
import java8.util.stream.StreamSupport;

/**
 * Created by kunong on 12/13/14 AD.
 */
public class NestedFragment extends Fragment {

    private static final int DEFAULT_CHILD_ANIMATION_DURATION = 250;

    private static long getNextAnimationDuration(Fragment fragment, long defValue) {
        try {
            // Attempt to get the resource ID of the next animation that will be applied to the given fragment.
            Field nextAnimField = Fragment.class.getDeclaredField("mNextAnim");
            nextAnimField.setAccessible(true);
            int nextAnimResource = nextAnimField.getInt(fragment);
            Animation nextAnim = AnimationUtils.loadAnimation(fragment.getActivity(), nextAnimResource);

            // ...and if it can be loaded, return that animation's duration
            return (nextAnim == null) ? defValue : nextAnim.getDuration();
        } catch (NoSuchFieldException | IllegalAccessException | Resources.NotFoundException ex) {
            return defValue;
        }
    }

    private static Fragment getRemovingParent(Fragment fragment) {
        fragment = fragment.getParentFragment();

        while (fragment != null && NestedFragment.class.isInstance(fragment) && !fragment.isRemoving()) {
            fragment = fragment.getParentFragment();
        }

        return fragment;
    }

    @Override
    public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
        // Apply the workaround only if this is a child fragment, and the parent is being removed.
        if (!enter) {
            final Fragment parent = getRemovingParent(this);

            if (parent != null && parent.isRemoving() && isVisible()) {
                // This is a workaround for the bug where child fragments disappear when the parent is removed (as all children are first removed from the parent)
                // See https://code.google.com/p/android/issues/detail?id=55228
                Animation doNothingAnim = new AlphaAnimation(1, 1);
                doNothingAnim.setDuration(getNextAnimationDuration(parent, DEFAULT_CHILD_ANIMATION_DURATION));

                return doNothingAnim;
            }
        }

        return super.onCreateAnimation(transit, enter, nextAnim);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        List<Fragment> fragments = getChildFragmentManager().getFragments();

        if (fragments != null) {
            StreamSupport.stream(fragments).forEach(fragment -> {
                if (fragment != null) {
                    fragment.onActivityResult(requestCode, resultCode, data);
                }
            });
        }
    }
}
