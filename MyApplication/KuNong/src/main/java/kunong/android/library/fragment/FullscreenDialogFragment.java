package kunong.android.library.fragment;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import kunong.android.library.concurrent.Async;

public abstract class FullscreenDialogFragment extends DialogFragment implements DialogInterface.OnKeyListener {

    private boolean mIsClosing;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = new FrameLayout(getActivity());

        view.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // Creating the fullscreen dialog.
        final Dialog dialog = new Dialog(getActivity());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(view);
        dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        dialog.getWindow().setWindowAnimations(0);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        dialog.setOnKeyListener(this);

        return dialog;
    }

    @Override
    public void onViewCreated(@Nullable View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        int animationId = getEnterAnimation();

        if (animationId != 0) {
            Animation animation = AnimationUtils.loadAnimation(view.getContext(), animationId);
            view.startAnimation(animation);
        }
    }

    public void close() {
        if (mIsClosing)
            return;

        View view = getView();
        int animationId = getExitAnimation();

        if (animationId != 0 && view != null) {

            mIsClosing = true;

            Animation animation = AnimationUtils.loadAnimation(view.getContext(), animationId);
            animation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    Async.main(FullscreenDialogFragment.this::dismiss);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });

            view.startAnimation(animation);

        } else {

            dismiss();

        }
    }

    @Override
    public void dismiss() {
        dismissAllowingStateLoss();
    }

    public int getEnterAnimation() {
        return 0;
    }

    public int getExitAnimation() {
        return 0;
    }

    @Override
    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
            close();

            return true;
        }

        return false;
    }
}
