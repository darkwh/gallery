package com.example.myapplication;


import android.util.Log;
import android.view.animation.DecelerateInterpolator;
import android.widget.Scroller;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.gallery.ViewPagerLayoutManager;

/**
 * Class intended to support snapping for a {@link RecyclerView}
 * which use {@link ViewPagerLayoutManager} as its {@link RecyclerView.LayoutManager}.
 * <p>
 * The implementation will snap the center of the target child view to the center of
 * the attached {@link RecyclerView}.
 */
@SuppressWarnings("AliControlFlowStatementWithoutBraces")
public class CenterSnapHelper extends RecyclerView.OnFlingListener {

    RecyclerView mRecyclerView;
    Scroller mGravityScroller;

    private boolean snapToCenter = false;

    // Handles the snap on scroll case.
    private final RecyclerView.OnScrollListener mScrollListener =
            new RecyclerView.OnScrollListener() {

                boolean mScrolled = false;

                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    super.onScrollStateChanged(recyclerView, newState);
//                    Log.d("darkwh", "onScrollStateChanged==" + newState);
//                    final GalleryLayoutManager layoutManager =
//                            (GalleryLayoutManager) recyclerView.getLayoutManager();
//                    final GalleryLayoutManager.OnItemSelectedListener onItemSelectedListener =
//                            layoutManager.getOnItemSelectedListener();
//                    if (onItemSelectedListener != null) {
//                        onItemSelectedListener.onPageScrollStateChanged(newState);
//                    }
//                    Log.d("darkwh", "mScrolled==" + mScrolled);
//                    Log.d("darkwh", "snapToCenter==" + snapToCenter);
//                    if (newState == RecyclerView.SCROLL_STATE_IDLE && mScrolled) {
//                        mScrolled = false;
//                        if (!snapToCenter) {
//                            snapToCenter = true;
//                            snapToCenterView(layoutManager, onItemSelectedListener);
//                        } else {
//                            snapToCenter = false;
//                        }
//                    }
                }

                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    if (dx != 0 || dy != 0) {
                        mScrolled = true;
                        snapToCenter = false;
                    }
                }
            };

    @Override
    public boolean onFling(int velocityX, int velocityY) {
        GalleryLayoutManager layoutManager = (GalleryLayoutManager) mRecyclerView.getLayoutManager();
        if (layoutManager == null) {
            return false;
        }
        RecyclerView.Adapter adapter = mRecyclerView.getAdapter();
        if (adapter == null) {
            return false;
        }
        if ((layoutManager.getState().mScrollDelta == layoutManager.getMaxOffset()
                || layoutManager.getState().mScrollDelta == layoutManager.getMinOffset())) {
            return false;
        }
        final int minFlingVelocity = mRecyclerView.getMinFlingVelocity();
        mGravityScroller.fling(0, 0, velocityX, velocityY,
                Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE);

        if (layoutManager.getOrientation() == ViewPagerLayoutManager.HORIZONTAL
                && Math.abs(velocityX) > minFlingVelocity) {
            final int currentPosition = layoutManager.getCurSelectedPosition();
            Log.d("ALALALALA", "pos==" + currentPosition);
//            final int offsetPosition = (int) (mGravityScroller.getFinalX() /
//                    layoutManager.mInterval / layoutManager.getDistanceRatio());
            ScrollHelper.smoothScrollToPosition(mRecyclerView, layoutManager, currentPosition);
            return true;
        }

        return true;
    }

    /**
     * Please attach after {{@link RecyclerView.LayoutManager} is setting}
     * Attaches the {@link CenterSnapHelper} to the provided RecyclerView, by calling
     * {@link RecyclerView#setOnFlingListener(RecyclerView.OnFlingListener)}.
     * You can call this method with {@code null} to detach it from the current RecyclerView.
     *
     * @param recyclerView The RecyclerView instance to which you want to add this helper or
     *                     {@code null} if you want to remove CenterSnapHelper from the current
     *                     RecyclerView.
     * @throws IllegalArgumentException if there is already a {@link RecyclerView.OnFlingListener}
     *                                  attached to the provided {@link RecyclerView}.
     */
    public void attachToRecyclerView(@Nullable RecyclerView recyclerView)
            throws IllegalStateException {
        if (mRecyclerView == recyclerView) {
            return; // nothing to do
        }
        if (mRecyclerView != null) {
            destroyCallbacks();
        }
        mRecyclerView = recyclerView;
        if (mRecyclerView != null) {
            final RecyclerView.LayoutManager layoutManager = mRecyclerView.getLayoutManager();
            if (!(layoutManager instanceof GalleryLayoutManager)) return;
            setupCallbacks();
            mGravityScroller = new Scroller(mRecyclerView.getContext(),
                    new DecelerateInterpolator());
            snapToCenterView((GalleryLayoutManager) layoutManager,
                    ((GalleryLayoutManager) layoutManager).getOnItemSelectedListener());
        }
    }

    void snapToCenterView(GalleryLayoutManager layoutManager,
                          GalleryLayoutManager.OnItemSelectedListener listener) {
        final int delta = layoutManager.getOffsetToCenter();
        if (delta != 0) {
            if (layoutManager.getOrientation()
                    == RecyclerView.VERTICAL)
                mRecyclerView.smoothScrollBy(0, delta);
            else
                mRecyclerView.smoothScrollBy(delta, 0);
        } else {
            // set it false to make smoothScrollToPosition keep trigger the listener
            snapToCenter = false;
        }

        if (listener != null)
            listener.onItemSelected(layoutManager.getCurSelectedPosition());
    }

    /**
     * Called when an instance of a {@link RecyclerView} is attached.
     */
    void setupCallbacks() throws IllegalStateException {
        if (mRecyclerView.getOnFlingListener() != null) {
            throw new IllegalStateException("An instance of OnFlingListener already set.");
        }
        mRecyclerView.addOnScrollListener(mScrollListener);
        mRecyclerView.setOnFlingListener(this);
    }

    /**
     * Called when the instance of a {@link RecyclerView} is detached.
     */
    void destroyCallbacks() {
        mRecyclerView.removeOnScrollListener(mScrollListener);
        mRecyclerView.setOnFlingListener(null);
    }
}
