package kunong.android.library.helper;


import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Created by kunong on 10/3/14 AD.
 */
public class FragmentHelper {

    public static void clearChildFragmentStack(FragmentManager fragmentManager) {
        for (int i = fragmentManager.getBackStackEntryCount() - 1; i >= 0; --i) {
            FragmentManager.BackStackEntry backStackEntry = fragmentManager.getBackStackEntryAt(i);
            fragmentManager.popBackStackImmediate(backStackEntry.getName(), FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
    }

    public static boolean returnBackStackImmediate(FragmentManager fm) {
        if (fm.getBackStackEntryCount() > 0) {
            List<Fragment> fragments = fm.getFragments();

            if (fragments != null && fragments.size() > 0) {
                for (Fragment fragment : fragments) {
                    boolean hasBackStackEntry = fragment != null && returnBackStackImmediate(fragment.getChildFragmentManager());

                    if (hasBackStackEntry) {
                        return true;
                    }
                }
            }

            fm.popBackStackImmediate();

            return true;
        }

        return false;
    }

    public static List<Fragment> getChildren(Fragment parent, boolean recursive) {
        Queue<FragmentManager> fmQueue = new LinkedList<>();
        List<Fragment> children = new LinkedList<>();
        FragmentManager fm;

        fmQueue.add(parent.getChildFragmentManager());

        while ((fm = fmQueue.poll()) != null) {
            List<Fragment> fragments = fm.getFragments();

            if (fragments == null || fragments.size() == 0)
                continue;

            for (Fragment fragment : fragments) {
                children.add(fragment);

                if (recursive) {
                    fmQueue.add(fragment.getChildFragmentManager());
                }
            }
        }

        return children;
    }

}
