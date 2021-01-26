package com.example.myapplication;

import android.content.Context;
import android.graphics.PointF;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.OrientationHelper;
import androidx.recyclerview.widget.RecyclerView;

public class GalleryLayoutManager extends RecyclerView.LayoutManager implements RecyclerView.SmoothScroller.ScrollVectorProvider {
    //item间距
    private int itemSpacing = 20;
    //缩放的的View数量(应该是奇数)
    private int scaleCount = 5;
    //缩放系数
    private float scaleRatio = 0.7f;
    //item原始宽
    private int mCenterItemWidth;

    private static final String TAG = "GalleryLayoutManager";
    final static int LAYOUT_START = -1;

    final static int LAYOUT_END = 1;

    public static final int HORIZONTAL = OrientationHelper.HORIZONTAL;

    public static final int VERTICAL = OrientationHelper.VERTICAL;

    private int mFirstVisiblePosition = 0;
    private int mLastVisiblePos = 0;
    private int mInitialSelectedPosition = 0;

    int mCurSelectedPosition = -1;

    View mCurSelectedView;
    /**
     * Scroll state
     */
    private State mState;

    private LinearSnapHelper mSnapHelper = new LinearSnapHelper();

    private InnerScrollListener mInnerScrollListener = new InnerScrollListener();

    private boolean mCallbackInFling = false;

    /**
     * Current orientation. Either {@link #HORIZONTAL} or {@link #VERTICAL}
     */
    private int mOrientation = HORIZONTAL;

    private OrientationHelper mHorizontalHelper;
    private OrientationHelper mVerticalHelper;

    public GalleryLayoutManager(int orientation) {
        mOrientation = orientation;
    }

    public int getOrientation() {
        return mOrientation;
    }

    public int getCurSelectedPosition() {
        return mCurSelectedPosition;
    }

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        if (mOrientation == VERTICAL) {
            return new LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        } else {
            return new LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
        }
    }

    @Override
    public RecyclerView.LayoutParams generateLayoutParams(Context c, AttributeSet attrs) {
        return new LayoutParams(c, attrs);
    }

    @Override
    public RecyclerView.LayoutParams generateLayoutParams(ViewGroup.LayoutParams lp) {
        if (lp instanceof ViewGroup.MarginLayoutParams) {
            return new LayoutParams((ViewGroup.MarginLayoutParams) lp);
        } else {
            return new LayoutParams(lp);
        }
    }

    @Override
    public boolean checkLayoutParams(RecyclerView.LayoutParams lp) {
        return lp instanceof LayoutParams;
    }


    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onLayoutChildren() called with: state = [" + state + "]");
        }
        if (getItemCount() == 0) {
            reset();
            detachAndScrapAttachedViews(recycler);
            return;
        }
        if (state.isPreLayout()) {
            return;
        }
        if (state.getItemCount() != 0 && !state.didStructureChange()) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "onLayoutChildren: ignore extra layout step");
            }
            return;
        }
        if (getChildCount() == 0 || state.didStructureChange()) {
            reset();
        }
        //设置首次选中item的位置
        mInitialSelectedPosition = Math.min(Math.max(0, mInitialSelectedPosition), getItemCount() - 1);
        //移除所有attach过的Views
        detachAndScrapAttachedViews(recycler);
        //首次填充画面
        firstFillCover(recycler, state, 0);
    }


    private void reset() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "reset: ");
        }
        if (mState != null) {
            mState.mItemsFrames.clear();
        }
        //when data set update keep the last selected position
        if (mCurSelectedPosition != -1) {
            mInitialSelectedPosition = mCurSelectedPosition;
        }
        mInitialSelectedPosition = Math.min(Math.max(0, mInitialSelectedPosition), getItemCount() - 1);
        mFirstVisiblePosition = mInitialSelectedPosition;
        mLastVisiblePos = mInitialSelectedPosition;
        mCurSelectedPosition = -1;
        if (mCurSelectedView != null) {
            mCurSelectedView.setSelected(false);
            mCurSelectedView = null;
        }
    }


    private void firstFillCover(RecyclerView.Recycler recycler, RecyclerView.State state, int scrollDelta) {
        if (mOrientation == HORIZONTAL) {
            firstFillWithHorizontal(recycler, state);
        } else {
            firstFillWithVertical(recycler, state);
        }

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "firstFillCover finish:first: " + mFirstVisiblePosition + ",last:" + mLastVisiblePos);
        }
        //执行每个Item的放大转换逻辑
//        if (mItemTransformer != null) {
//            View child;
//            for (int i = 0; i < getChildCount(); i++) {
//                child = getChildAt(i);
//                mItemTransformer.transformItem(this, child, calculateToCenterFraction(child, scrollDelta));
//            }
//        }
        //首次填充后主动调用一次Scrolled
        mCurSelectedPosition = mInitialSelectedPosition;
        mInnerScrollListener.onScrolled(mRecyclerView, 0, 0);
    }

    /**
     * Layout the item view witch position specified by {@link GalleryLayoutManager#mInitialSelectedPosition} first and then layout the other
     *
     * @param recycler
     * @param state
     */
    private void firstFillWithHorizontal(RecyclerView.Recycler recycler, RecyclerView.State state) {
        //来自英文渣的翻译: scrap-->碎片,这里理解为每个itemView
        detachAndScrapAttachedViews(recycler);
        //RecyclerView内容区域起始位置(即去除padding后的位置)
        int leftEdge = getOrientationHelper().getStartAfterPadding();
        //RecyclerView内容区域结束位置(即去除padding后的位置)
        int rightEdge = getOrientationHelper().getEndAfterPadding();
        //起始选中位置
        int startPosition = mInitialSelectedPosition;
        //选中item的宽和高
        int scrapWidth, scrapHeight;
        Rect scrapRect = new Rect();
        //垂直方向可用空间高度(view高度减去padding距离)
        int height = getVerticalSpace();
        //item顶部距离RecyclerView顶部的距离
        int topOffset;
        //layout the init position view
        View scrap = recycler.getViewForPosition(mInitialSelectedPosition);
        //添加初始化时设置的选中item(即中间的item)
        addView(scrap, 0);
        //测量子view
        measureChildWithMargins(scrap, 0, 0);
        //获取测量后的宽
        scrapWidth = getDecoratedMeasuredWidth(scrap);
        mCenterItemWidth = scrapWidth;
        //获取测量后的高
        scrapHeight = getDecoratedMeasuredHeight(scrap);
        topOffset = (int) (getPaddingTop() + (height - scrapHeight) / 2.0f);
        int left = (int) (getPaddingLeft() + (getHorizontalSpace() - scrapWidth) / 2.f);
        scrapRect.set(left, topOffset, left + scrapWidth, topOffset + scrapHeight);
        //绘制选中的item
        layoutDecorated(scrap, scrapRect.left, scrapRect.top, scrapRect.right, scrapRect.bottom);
        //设置或更新item frame缓存内容
        if (getState().mItemsFrames.get(startPosition) == null) {
            getState().mItemsFrames.put(startPosition, scrapRect);
        } else {
            getState().mItemsFrames.get(startPosition).set(scrapRect);
        }
        //对第一个和最后一个可见item的position进行记录
        mFirstVisiblePosition = mLastVisiblePos = startPosition;
        //选中item左边缘到RecyclerView左边缘的距离(包含Decoration)
        int leftStartOffset = getDecoratedLeft(scrap);
        //选中item右边缘到RecyclerView左边缘的距离(包含Decoration)
        int rightStartOffset = getDecoratedRight(scrap);
        //fill left of center
        //从选中item向左填充绘制
        fillLeft(recycler, mInitialSelectedPosition - 1, leftStartOffset, leftEdge);
        //fill right of center
        //从选中item向右填充绘制
        fillRight(recycler, mInitialSelectedPosition + 1, rightStartOffset, rightEdge);
    }

    @Override
    public void onItemsRemoved(RecyclerView recyclerView, int positionStart, int itemCount) {
        super.onItemsRemoved(recyclerView, positionStart, itemCount);
    }

    /**
     * Layout the item view witch position special by {@link GalleryLayoutManager#mInitialSelectedPosition} first and then layout the other
     *
     * @param recycler
     * @param state
     */
    private void firstFillWithVertical(RecyclerView.Recycler recycler, RecyclerView.State state) {
        detachAndScrapAttachedViews(recycler);
        int topEdge = getOrientationHelper().getStartAfterPadding();
        int bottomEdge = getOrientationHelper().getEndAfterPadding();
        int startPosition = mInitialSelectedPosition;
        int scrapWidth, scrapHeight;
        Rect scrapRect = new Rect();
        int width = getHorizontalSpace();
        int leftOffset;
        //layout the init position view
        View scrap = recycler.getViewForPosition(mInitialSelectedPosition);
        addView(scrap, 0);
        measureChildWithMargins(scrap, 0, 0);
        scrapWidth = getDecoratedMeasuredWidth(scrap);
        scrapHeight = getDecoratedMeasuredHeight(scrap);
        leftOffset = (int) (getPaddingLeft() + (width - scrapWidth) / 2.0f);
        int top = (int) (getPaddingTop() + (getVerticalSpace() - scrapHeight) / 2.f);
        scrapRect.set(leftOffset, top, leftOffset + scrapWidth, top + scrapHeight);
        layoutDecorated(scrap, scrapRect.left, scrapRect.top, scrapRect.right, scrapRect.bottom);
        if (getState().mItemsFrames.get(startPosition) == null) {
            getState().mItemsFrames.put(startPosition, scrapRect);
        } else {
            getState().mItemsFrames.get(startPosition).set(scrapRect);
        }
        mFirstVisiblePosition = mLastVisiblePos = startPosition;
        int topStartOffset = getDecoratedTop(scrap);
        int bottomStartOffset = getDecoratedBottom(scrap);
        //fill left of center
        fillTop(recycler, mInitialSelectedPosition - 1, topStartOffset, topEdge);
        //fill right of center
        fillBottom(recycler, mInitialSelectedPosition + 1, bottomStartOffset, bottomEdge);
    }

    /**
     * Fill left of the center view
     *
     * @param recycler
     * @param startPosition start position to fill left
     * @param startOffset   layout start offset
     * @param leftEdge
     */
    private void fillLeft(RecyclerView.Recycler recycler, int startPosition, int startOffset, int leftEdge) {
        View scrap;
        int topOffset;
        int scrapWidth, scrapHeight;
        Rect scrapRect = new Rect();
        int height = getVerticalSpace();
        for (int i = startPosition; i >= 0 && startOffset > leftEdge; i--) {
            scrap = recycler.getViewForPosition(i);
            addView(scrap, 0);
            measureChildWithMargins(scrap, 0, 0);
            scrapWidth = getDecoratedMeasuredWidth(scrap);
            scrapHeight = getDecoratedMeasuredHeight(scrap);
            int gamma = Math.min((startPosition - i + 1), (scaleCount - 1) / 2);
            float spacing = (float) Math.pow(scaleRatio, gamma);
            scrap.setScaleX(spacing);
            scrap.setScaleY(spacing);
            topOffset = (int) (getPaddingTop() + (height - scrapHeight * spacing) / 2.0f);
            int topPosition = (int) (topOffset - scrapHeight * (1 - spacing) / 2.0f);
            int rightPosition = (int) (startOffset - (itemSpacing - scrapWidth * (1 - spacing) / 2));
            scrapRect.set(rightPosition - scrapWidth, topPosition, rightPosition, topPosition + scrapHeight);
            startOffset = (int) (startOffset - scrapWidth * spacing - itemSpacing);
            layoutDecorated(scrap, scrapRect.left, scrapRect.top, scrapRect.right, scrapRect.bottom);
            mFirstVisiblePosition = i;
            if (getState().mItemsFrames.get(i) == null) {
                getState().mItemsFrames.put(i, scrapRect);
            } else {
                getState().mItemsFrames.get(i).set(scrapRect);
            }
        }
    }

    /**
     * Fill right of the center view
     *
     * @param recycler
     * @param startPosition start position to fill right
     * @param startOffset   layout start offset
     * @param rightEdge
     */
    private void fillRight(RecyclerView.Recycler recycler, int startPosition, int startOffset, int rightEdge) {
        View scrap;
        int topOffset;
        int scrapWidth, scrapHeight;
        Rect scrapRect = new Rect();
        int height = getVerticalSpace();
        for (int i = startPosition; i < getItemCount() && startOffset < rightEdge; i++) {
            scrap = recycler.getViewForPosition(i);
            addView(scrap);
            measureChildWithMargins(scrap, 0, 0);
            scrapWidth = getDecoratedMeasuredWidth(scrap);
            scrapHeight = getDecoratedMeasuredHeight(scrap);

            int gamma = Math.min((i - startPosition + 1), (scaleCount - 1) / 2);
            float spacing = (float) Math.pow(scaleRatio, gamma);
            scrap.setScaleX(spacing);
            scrap.setScaleY(spacing);
            topOffset = (int) (getPaddingTop() + (height - scrapHeight * spacing) / 2.0f);
            int topPosition = (int) (topOffset - scrapHeight * (1 - spacing) / 2.0f);
            int leftPosition = (int) (startOffset + (itemSpacing - scrapWidth * (1 - spacing) / 2));
            scrapRect.set(leftPosition, topPosition, leftPosition + scrapWidth, topPosition + scrapHeight);
            startOffset = (int) (startOffset + scrapWidth * spacing + itemSpacing);
            layoutDecorated(scrap, scrapRect.left, scrapRect.top, scrapRect.right, scrapRect.bottom);
            mLastVisiblePos = i;
            if (getState().mItemsFrames.get(i) == null) {
                getState().mItemsFrames.put(i, scrapRect);
            } else {
                getState().mItemsFrames.get(i).set(scrapRect);
            }
        }
    }

    /**
     * Fill top of the center view
     *
     * @param recycler
     * @param startPosition start position to fill top
     * @param startOffset   layout start offset
     * @param topEdge       top edge of the RecycleView
     */
    private void fillTop(RecyclerView.Recycler recycler, int startPosition, int startOffset, int topEdge) {
        View scrap;
        int leftOffset;
        int scrapWidth, scrapHeight;
        Rect scrapRect = new Rect();
        int width = getHorizontalSpace();
        for (int i = startPosition; i >= 0 && startOffset > topEdge; i--) {
            scrap = recycler.getViewForPosition(i);
            addView(scrap, 0);
            measureChildWithMargins(scrap, 0, 0);
            scrapWidth = getDecoratedMeasuredWidth(scrap);
            scrapHeight = getDecoratedMeasuredHeight(scrap);
            leftOffset = (int) (getPaddingLeft() + (width - scrapWidth) / 2.0f);
            scrapRect.set(leftOffset, startOffset - scrapHeight, leftOffset + scrapWidth, startOffset);
            layoutDecorated(scrap, scrapRect.left, scrapRect.top, scrapRect.right, scrapRect.bottom);
            startOffset = scrapRect.top;
            mFirstVisiblePosition = i;
            if (getState().mItemsFrames.get(i) == null) {
                getState().mItemsFrames.put(i, scrapRect);
            } else {
                getState().mItemsFrames.get(i).set(scrapRect);
            }
        }
    }

    /**
     * Fill bottom of the center view
     *
     * @param recycler
     * @param startPosition start position to fill bottom
     * @param startOffset   layout start offset
     * @param bottomEdge    bottom edge of the RecycleView
     */
    private void fillBottom(RecyclerView.Recycler recycler, int startPosition, int startOffset, int bottomEdge) {
        View scrap;
        int leftOffset;
        int scrapWidth, scrapHeight;
        Rect scrapRect = new Rect();
        int width = getHorizontalSpace();
        for (int i = startPosition; i < getItemCount() && startOffset < bottomEdge; i++) {
            scrap = recycler.getViewForPosition(i);
            addView(scrap);
            measureChildWithMargins(scrap, 0, 0);
            scrapWidth = getDecoratedMeasuredWidth(scrap);
            scrapHeight = getDecoratedMeasuredHeight(scrap);
            leftOffset = (int) (getPaddingLeft() + (width - scrapWidth) / 2.0f);
            scrapRect.set(leftOffset, startOffset, leftOffset + scrapWidth, startOffset + scrapHeight);
            layoutDecorated(scrap, scrapRect.left, scrapRect.top, scrapRect.right, scrapRect.bottom);
            startOffset = scrapRect.bottom;
            mLastVisiblePos = i;
            if (getState().mItemsFrames.get(i) == null) {
                getState().mItemsFrames.put(i, scrapRect);
            } else {
                getState().mItemsFrames.get(i).set(scrapRect);
            }
        }
    }


    private void fillCover(RecyclerView.Recycler recycler, RecyclerView.State state, int scrollDelta) {
        if (getItemCount() == 0) {
            return;
        }

        if (mOrientation == HORIZONTAL) {
//            fillWithHorizontal(recycler, state, scrollDelta);
            fillHorizontalTest(recycler, state, getState().mScrollDelta);
        } else {
            fillWithVertical(recycler, state, scrollDelta);
        }


//        if (mItemTransformer != null) {
//            View child;
//            for (int i = 0; i < getChildCount(); i++) {
//                child = getChildAt(i);
//                mItemTransformer.transformItem(this, child, calculateToCenterFraction(child, scrollDelta));
//            }
//        }
    }

    private float calculateToCenterFraction(View child, float pendingOffset) {
        int distance = calculateDistanceCenter(child, pendingOffset);
        int childLength = mOrientation == GalleryLayoutManager.HORIZONTAL ? child.getWidth() : child.getHeight();

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "calculateToCenterFraction: distance:" + distance + ",childLength:" + childLength);
        }
        float scrollDelta = Math.max(-1.f, Math.min(1.f, distance * 1.f / childLength));
        Log.d(TAG, "scrollDelta: " + scrollDelta);
        return scrollDelta;
    }

    /**
     * @param child
     * @param pendingOffset child view will scroll by
     * @return
     */
    private int calculateDistanceCenter(View child, float pendingOffset) {
        OrientationHelper orientationHelper = getOrientationHelper();
        int parentCenter = (orientationHelper.getEndAfterPadding() - orientationHelper.getStartAfterPadding()) / 2 + orientationHelper.getStartAfterPadding();
        if (mOrientation == GalleryLayoutManager.HORIZONTAL) {
            return (int) (child.getWidth() / 2 - pendingOffset + child.getLeft() - parentCenter);
        } else {
            return (int) (child.getHeight() / 2 - pendingOffset + child.getTop() - parentCenter);
        }

    }

    /**
     * @param recycler
     * @param state
     * @param dy
     */
    private void fillWithVertical(RecyclerView.Recycler recycler, RecyclerView.State state, int dy) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "fillWithVertical: dy:" + dy);
        }
        int topEdge = getOrientationHelper().getStartAfterPadding();
        int bottomEdge = getOrientationHelper().getEndAfterPadding();

        //1.remove and recycle the view that disappear in screen
        View child;
        if (getChildCount() > 0) {
            if (dy >= 0) {
                //remove and recycle the top off screen view
                int fixIndex = 0;
                for (int i = 0; i < getChildCount(); i++) {
                    child = getChildAt(i + fixIndex);
                    if (getDecoratedBottom(child) - dy < topEdge) {
                        if (BuildConfig.DEBUG) {
                            Log.v(TAG, "fillWithVertical: removeAndRecycleView:" + getPosition(child) + ",bottom:" + getDecoratedBottom(child));
                        }
                        removeAndRecycleView(child, recycler);
                        mFirstVisiblePosition++;
                        fixIndex--;
                    } else {
                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "fillWithVertical: break:" + getPosition(child) + ",bottom:" + getDecoratedBottom(child));
                        }
                        break;
                    }
                }
            } else { //dy<0
                //remove and recycle the bottom off screen view
                for (int i = getChildCount() - 1; i >= 0; i--) {
                    child = getChildAt(i);
                    if (getDecoratedTop(child) - dy > bottomEdge) {
                        if (BuildConfig.DEBUG) {
                            Log.v(TAG, "fillWithVertical: removeAndRecycleView:" + getPosition(child));
                        }
                        removeAndRecycleView(child, recycler);
                        mLastVisiblePos--;
                    } else {
                        break;
                    }
                }
            }

        }

        int startPosition = mFirstVisiblePosition;
        int startOffset = -1;
        int scrapWidth, scrapHeight;
        Rect scrapRect;
        int width = getHorizontalSpace();
        int leftOffset;
        View scrap;
        //2.Add or reattach item view to fill screen
        if (dy >= 0) {
            if (getChildCount() != 0) {
                View lastView = getChildAt(getChildCount() - 1);
                startPosition = getPosition(lastView) + 1;
                startOffset = getDecoratedBottom(lastView);
            }
            for (int i = startPosition; i < getItemCount() && startOffset < bottomEdge + dy; i++) {
                scrapRect = getState().mItemsFrames.get(i);
                scrap = recycler.getViewForPosition(i);
                addView(scrap);
                if (scrapRect == null) {
                    scrapRect = new Rect();
                    getState().mItemsFrames.put(i, scrapRect);
                }
                measureChildWithMargins(scrap, 0, 0);
                scrapWidth = getDecoratedMeasuredWidth(scrap);
                scrapHeight = getDecoratedMeasuredHeight(scrap);
                leftOffset = (int) (getPaddingLeft() + (width - scrapWidth) / 2.0f);
                if (startOffset == -1 && startPosition == 0) {
                    //layout the first position item in center
                    int top = (int) (getPaddingTop() + (getVerticalSpace() - scrapHeight) / 2.f);
                    scrapRect.set(leftOffset, top, leftOffset + scrapWidth, top + scrapHeight);
                } else {
                    scrapRect.set(leftOffset, startOffset, leftOffset + scrapWidth, startOffset + scrapHeight);
                }
                layoutDecorated(scrap, scrapRect.left, scrapRect.top, scrapRect.right, scrapRect.bottom);
                startOffset = scrapRect.bottom;
                mLastVisiblePos = i;
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "fillWithVertical: add view:" + i + ",startOffset:" + startOffset + ",mLastVisiblePos:" + mLastVisiblePos + ",bottomEdge" + bottomEdge);
                }
            }
        } else {
            //dy<0
            if (getChildCount() > 0) {
                View firstView = getChildAt(0);
                startPosition = getPosition(firstView) - 1; //前一个View的position
                startOffset = getDecoratedTop(firstView);
            }
            for (int i = startPosition; i >= 0 && startOffset > topEdge + dy; i--) {
                scrapRect = getState().mItemsFrames.get(i);
                scrap = recycler.getViewForPosition(i);
                addView(scrap, 0);
                if (scrapRect == null) {
                    scrapRect = new Rect();
                    getState().mItemsFrames.put(i, scrapRect);
                }
                measureChildWithMargins(scrap, 0, 0);
                scrapWidth = getDecoratedMeasuredWidth(scrap);
                scrapHeight = getDecoratedMeasuredHeight(scrap);
                leftOffset = (int) (getPaddingLeft() + (width - scrapWidth) / 2.0f);
                scrapRect.set(leftOffset, startOffset - scrapHeight, leftOffset + scrapWidth, startOffset);
                layoutDecorated(scrap, scrapRect.left, scrapRect.top, scrapRect.right, scrapRect.bottom);
                startOffset = scrapRect.top;
                mFirstVisiblePosition = i;
            }
        }
    }

    private void fillHorizontalTest(RecyclerView.Recycler recycler, RecyclerView.State state, int dx) {
        detachAndScrapAttachedViews(recycler);
        //recyclerview中心点x坐标位置
        int parentCenter = (getOrientationHelper().getEndAfterPadding() - getOrientationHelper().getStartAfterPadding()) / 2 + getOrientationHelper().getStartAfterPadding();
        //确定recyclerview左边缘的位置
        int leftEdge = getOrientationHelper().getStartAfterPadding();
        //确定recyclerview右边缘的位置
        int rightEdge = getOrientationHelper().getEndAfterPadding();
        //中心item到两边最近的item的偏移量
        int offetOneFromCenter = mCenterItemWidth + itemSpacing;
        //根据mCurSelectedPosition先绘制中间item(中间item此时可能也处于偏移状态)
        int offsetDx = Math.abs(dx) % offetOneFromCenter;


        int topOffset;
        int scrapWidth, scrapHeight;
        int height = getVerticalSpace();
        Rect scrapRect;
        float spacing;
        int beishu = Math.abs(dx) / offetOneFromCenter;
        if (dx > 0) {
            if ((mInitialSelectedPosition + beishu) < getItemCount()) {
                mCurSelectedPosition = mInitialSelectedPosition + beishu;
            } else {
                mCurSelectedPosition = getItemCount() - 1;
            }
            //从右向左滑
            spacing = 1f - (1f - scaleRatio) * offsetDx / (float) offetOneFromCenter;
            scrapRect = getState().mItemsFrames.get(mCurSelectedPosition);
            View scrap = recycler.getViewForPosition(mCurSelectedPosition);
            addView(scrap);
            measureChildWithMargins(scrap, 0, 0);
            scrapWidth = getDecoratedMeasuredWidth(scrap);
            scrapHeight = getDecoratedMeasuredHeight(scrap);
            scrap.setScaleX(spacing);
            scrap.setScaleY(spacing);
            if (scrapRect == null) {
                scrapRect = new Rect();
            }
            topOffset = (int) (getPaddingTop() + (height - scrapHeight * spacing) / 2.0f);
            int topPosition = (int) (topOffset - scrapHeight * (1 - spacing) / 2.0f);
            int rightPosition = (int) (parentCenter + mCenterItemWidth / 2 - offsetDx +
                    (scrapWidth * (1 - spacing) / 2));
            scrapRect.set(rightPosition - scrapWidth, topPosition, rightPosition, topPosition + scrapHeight);
            layoutDecorated(scrap, scrapRect.left, scrapRect.top, scrapRect.right, scrapRect.bottom);
            //画左面
            fillLeftTest(recycler, mCurSelectedPosition - 1,
                    (int) (rightPosition - scrapWidth + scrapWidth * (1f - spacing) / 2),
                    leftEdge, dx);
            //画右面
            fillRightTest(recycler, mCurSelectedPosition + 1,
                    (int) (rightPosition - scrapWidth * (1f - spacing) / 2),
                    rightEdge, dx);
        } else {
            //从左向右滑
            if ((mInitialSelectedPosition - beishu) > 0) {
                mCurSelectedPosition = mInitialSelectedPosition - beishu;
            } else {
                mCurSelectedPosition = 0;
            }
            spacing = 1f - (1f - scaleRatio) * offsetDx / (float) offetOneFromCenter;
            scrapRect = getState().mItemsFrames.get(mCurSelectedPosition);
            View scrap = recycler.getViewForPosition(mCurSelectedPosition);
            addView(scrap);
            measureChildWithMargins(scrap, 0, 0);
            scrapWidth = getDecoratedMeasuredWidth(scrap);
            scrapHeight = getDecoratedMeasuredHeight(scrap);
            scrap.setScaleX(spacing);
            scrap.setScaleY(spacing);
            if (scrapRect == null) {
                scrapRect = new Rect();
            }
            topOffset = (int) (getPaddingTop() + (height - scrapHeight * spacing) / 2.0f);
            int topPosition = (int) (topOffset - scrapHeight * (1 - spacing) / 2.0f);
            int leftPosition = (int) (parentCenter - mCenterItemWidth / 2 + offsetDx -
                    (scrapWidth * (1 - spacing) / 2));
            scrapRect.set(leftPosition, topPosition, leftPosition + scrapWidth, topPosition + scrapHeight);
            layoutDecorated(scrap, scrapRect.left, scrapRect.top, scrapRect.right, scrapRect.bottom);

            //画左面
            fillLeftTest1(recycler, mCurSelectedPosition - 1,
                    (int) (leftPosition + scrapWidth * (1f - spacing) / 2),
                    leftEdge, dx);
            //画右面
            fillRightTest1(recycler, mCurSelectedPosition + 1,
                    (int) (leftPosition + scrapWidth - scrapWidth * (1f - spacing) / 2),
                    rightEdge, dx);
        }
    }

    private void fillLeftTest1(RecyclerView.Recycler recycler, int startPosition, int startOffset, int leftEdge, int dx) {
        View scrap;
        int topOffset;
        int scrapWidth, scrapHeight;
        Rect scrapRect = new Rect();
        int height = getVerticalSpace();
        int offetOneFromCenter = mCenterItemWidth + itemSpacing;
        for (int i = startPosition; i >= 0 && startOffset > leftEdge + itemSpacing; i--) {
            int gamma = startPosition - i + 1;
            float tempScale = (float) Math.max(Math.pow(scaleRatio, (scaleCount - 1) / 2f),
                    Math.pow(scaleRatio, (float) gamma - 1));
            float preSpacing = (float) Math.max(Math.pow(scaleRatio, (scaleCount - 1) / 2f),
                    Math.pow(scaleRatio, gamma));
            scrap = recycler.getViewForPosition(i);
            addView(scrap, 0);
            measureChildWithMargins(scrap, 0, 0);
            scrapWidth = getDecoratedMeasuredWidth(scrap);
            scrapHeight = getDecoratedMeasuredHeight(scrap);
            int offsetDx = Math.abs(dx) % offetOneFromCenter;
            float spacing = preSpacing + (tempScale - preSpacing) * offsetDx / (float) offetOneFromCenter;
            scrap.setScaleX(spacing);
            scrap.setScaleY(spacing);
            topOffset = (int) (getPaddingTop() + (height - scrapHeight * spacing) / 2.0f);
            int topPosition = (int) (topOffset - scrapHeight * (1 - spacing) / 2.0f);
            int rightPosition = (int) (startOffset - (itemSpacing - scrapWidth * (1 - spacing) / 2));
            scrapRect.set(rightPosition - scrapWidth, topPosition, rightPosition, topPosition + scrapHeight);
            startOffset = (int) (startOffset - scrapWidth * spacing - itemSpacing);
            layoutDecorated(scrap, scrapRect.left, scrapRect.top, scrapRect.right, scrapRect.bottom);
            mFirstVisiblePosition = i;
            if (getState().mItemsFrames.get(i) == null) {
                getState().mItemsFrames.put(i, scrapRect);
            } else {
                getState().mItemsFrames.get(i).set(scrapRect);
            }
        }
    }

    private void fillRightTest1(RecyclerView.Recycler recycler, int startPosition, int startOffset, int rightEdge, int dx) {
        View scrap;
        int topOffset;
        int scrapWidth, scrapHeight;
        Rect scrapRect = new Rect();
        int height = getVerticalSpace();
        int offetOneFromCenter = mCenterItemWidth + itemSpacing;
        for (int i = startPosition; i < getItemCount() && startOffset < rightEdge - itemSpacing; i++) {
            int gamma = i - startPosition + 1;
            float tempScale = (float) Math.max(Math.pow(scaleRatio, (scaleCount - 1) / 2f),
                    Math.pow(scaleRatio, (float) gamma));
            float nextSpacing = (float) Math.max(Math.pow(scaleRatio, (scaleCount - 1) / 2f),
                    Math.pow(scaleRatio, gamma + 1));
            scrap = recycler.getViewForPosition(i);
            addView(scrap);
            measureChildWithMargins(scrap, 0, 0);
            scrapWidth = getDecoratedMeasuredWidth(scrap);
            scrapHeight = getDecoratedMeasuredHeight(scrap);
            int offsetDx = Math.abs(dx) % offetOneFromCenter;
            float spacing = tempScale - (tempScale - nextSpacing) * offsetDx / (float) offetOneFromCenter;
            scrap.setScaleX(spacing);
            scrap.setScaleY(spacing);
            topOffset = (int) (getPaddingTop() + (height - scrapHeight * spacing) / 2.0f);
            int topPosition = (int) (topOffset - scrapHeight * (1 - spacing) / 2.0f);
            int leftPosition = (int) (startOffset + (itemSpacing - scrapWidth * (1 - spacing) / 2));
            scrapRect.set(leftPosition, topPosition, leftPosition + scrapWidth, topPosition + scrapHeight);
            startOffset = (int) (startOffset + scrapWidth * spacing + itemSpacing);
            layoutDecorated(scrap, scrapRect.left, scrapRect.top, scrapRect.right, scrapRect.bottom);
            mLastVisiblePos = i;
            if (getState().mItemsFrames.get(i) == null) {
                getState().mItemsFrames.put(i, scrapRect);
            } else {
                getState().mItemsFrames.get(i).set(scrapRect);
            }
        }
    }

    /**
     * 动态绘制左侧view
     */
    private void fillLeftTest(RecyclerView.Recycler recycler, int startPosition, int startOffset, int leftEdge, int dx) {
        View scrap;
        int topOffset;
        int scrapWidth, scrapHeight;
        Rect scrapRect = new Rect();
        int height = getVerticalSpace();
        int offetOneFromCenter = mCenterItemWidth + itemSpacing;
        for (int i = startPosition; i >= 0 && startOffset > leftEdge + itemSpacing; i--) {
            int gamma = Math.min((startPosition - i + 1), (scaleCount - 1) / 2);
            float tempScale = (float) Math.max(Math.pow(scaleRatio, (scaleCount - 1) / 2f),
                    Math.pow(scaleRatio, (float) gamma));
            float preSpacing = (float) Math.max(Math.pow(scaleRatio, (scaleCount - 1) / 2f),
                    Math.pow(scaleRatio, gamma + 1));
            scrap = recycler.getViewForPosition(i);
            addView(scrap, 0);
            measureChildWithMargins(scrap, 0, 0);
            scrapWidth = getDecoratedMeasuredWidth(scrap);
            scrapHeight = getDecoratedMeasuredHeight(scrap);
            int offsetDx = Math.abs(dx) % offetOneFromCenter;
            float spacing = tempScale - (tempScale - preSpacing) * offsetDx / (float) offetOneFromCenter;
            scrap.setScaleX(spacing);
            scrap.setScaleY(spacing);
            topOffset = (int) (getPaddingTop() + (height - scrapHeight * spacing) / 2.0f);
            int topPosition = (int) (topOffset - scrapHeight * (1 - spacing) / 2.0f);
            int rightPosition = (int) (startOffset - (itemSpacing - scrapWidth * (1 - spacing) / 2));
            scrapRect.set(rightPosition - scrapWidth, topPosition, rightPosition, topPosition + scrapHeight);
            startOffset = (int) (startOffset - scrapWidth * spacing - itemSpacing);
            layoutDecorated(scrap, scrapRect.left, scrapRect.top, scrapRect.right, scrapRect.bottom);
            mFirstVisiblePosition = i;
            if (getState().mItemsFrames.get(i) == null) {
                getState().mItemsFrames.put(i, scrapRect);
            } else {
                getState().mItemsFrames.get(i).set(scrapRect);
            }
        }
    }

    /**
     * 动态绘制右侧view
     */
    private void fillRightTest(RecyclerView.Recycler recycler, int startPosition, int startOffset, int rightEdge, int dx) {
        View scrap;
        int topOffset;
        int scrapWidth, scrapHeight;
        Rect scrapRect = new Rect();
        int height = getVerticalSpace();
        int offetOneFromCenter = mCenterItemWidth + itemSpacing;
        for (int i = startPosition; i < getItemCount() && startOffset < rightEdge - itemSpacing; i++) {
            int gamma = i - startPosition + 1;
            float tempScale = (float) Math.max(Math.pow(scaleRatio, (scaleCount - 1) / 2f),
                    Math.pow(scaleRatio, (float) gamma));
            float preSpacing = (float) Math.max(Math.pow(scaleRatio, (scaleCount - 1) / 2f),
                    Math.pow(scaleRatio, gamma - 1));
            scrap = recycler.getViewForPosition(i);
            addView(scrap);
            measureChildWithMargins(scrap, 0, 0);
            scrapWidth = getDecoratedMeasuredWidth(scrap);
            scrapHeight = getDecoratedMeasuredHeight(scrap);
            int offsetDx = Math.abs(dx) % offetOneFromCenter;
            float spacing = tempScale + (preSpacing - tempScale) * offsetDx / (float) offetOneFromCenter;
            scrap.setScaleX(spacing);
            scrap.setScaleY(spacing);
            topOffset = (int) (getPaddingTop() + (height - scrapHeight * spacing) / 2.0f);
            int topPosition = (int) (topOffset - scrapHeight * (1 - spacing) / 2.0f);
            int leftPosition = (int) (startOffset + (itemSpacing - scrapWidth * (1 - spacing) / 2));
            scrapRect.set(leftPosition, topPosition, leftPosition + scrapWidth, topPosition + scrapHeight);
            startOffset = (int) (startOffset + scrapWidth * spacing + itemSpacing);
            layoutDecorated(scrap, scrapRect.left, scrapRect.top, scrapRect.right, scrapRect.bottom);
            mLastVisiblePos = i;
            if (getState().mItemsFrames.get(i) == null) {
                getState().mItemsFrames.put(i, scrapRect);
            } else {
                getState().mItemsFrames.get(i).set(scrapRect);
            }
        }
    }

    /**
     * @param recycler
     * @param state
     */
    private void fillWithHorizontal(RecyclerView.Recycler recycler, RecyclerView.State state, int dx) {
        //确定recyclerview左边缘的位置
        int leftEdge = getOrientationHelper().getStartAfterPadding();
        //确定recyclerview右边缘的位置
        int rightEdge = getOrientationHelper().getEndAfterPadding();
        if (BuildConfig.DEBUG) {
            Log.v(TAG, "fillWithHorizontal() called with: dx = [" + dx + "],leftEdge:" + leftEdge + ",rightEdge:" + rightEdge);
        }
        //1.remove and recycle the view that disappear in screen
        View child;
        if (getChildCount() > 0) {
            if (dx >= 0) {
                //remove and recycle the left off screen view
                int fixIndex = 0;
                for (int i = 0; i < getChildCount(); i++) {
                    child = getChildAt(i + fixIndex);
                    if (child != null) {
                        int scrapWidth = getDecoratedMeasuredWidth(child);
                        if (getDecoratedRight(child) - (scrapWidth * (1 - child.getScaleX()) / 2) - dx < leftEdge) {
                            removeAndRecycleView(child, recycler);
                            mFirstVisiblePosition++;
                            fixIndex--;
                            if (BuildConfig.DEBUG) {
                                Log.v(TAG, "fillWithHorizontal:removeAndRecycleView:" + getPosition(child) + " mFirstVisiblePosition change to:" + mFirstVisiblePosition);
                            }
                        }
                    }
                }
            } else { //dx<0
                //remove and recycle the right off screen view
                for (int i = getChildCount() - 1; i >= 0; i--) {
                    child = getChildAt(i);
                    if (child != null) {
                        int scrapWidth = getDecoratedMeasuredWidth(child);
                        if (getDecoratedLeft(child) + (scrapWidth * (1 - child.getScaleX()) / 2) - dx > rightEdge) {
                            removeAndRecycleView(child, recycler);
                            mLastVisiblePos--;
                            if (BuildConfig.DEBUG) {
                                Log.v(TAG, "fillWithHorizontal:removeAndRecycleView:" + getPosition(child) + "mLastVisiblePos change to:" + mLastVisiblePos);
                            }
                        }
                    }
                }
            }

        }


        //2.Add or reattach item view to fill screen
        int startPosition = mFirstVisiblePosition;
        int startOffset = -1;
        int scrapWidth, scrapHeight;
        Rect scrapRect;
        int height = getVerticalSpace();
        int topOffset;
        View scrap;
        if (dx >= 0) {
            if (getChildCount() != 0) {
                View lastView = getChildAt(getChildCount() - 1);
                startPosition = getPosition(lastView) + 1; //start layout from next position item
                startOffset = getDecoratedRight(lastView);
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "fillWithHorizontal:to right startPosition:" + startPosition + ",startOffset:" + startOffset + ",rightEdge:" + rightEdge);
                }
            }
            for (int i = startPosition; i < getItemCount() && startOffset < rightEdge + dx; i++) {
                scrapRect = getState().mItemsFrames.get(i);
                scrap = recycler.getViewForPosition(i);
                addView(scrap);
                if (scrapRect == null) {
                    scrapRect = new Rect();
                    getState().mItemsFrames.put(i, scrapRect);
                }
                measureChildWithMargins(scrap, 0, 0);
                scrapWidth = getDecoratedMeasuredWidth(scrap);
                scrapHeight = getDecoratedMeasuredHeight(scrap);
                topOffset = (int) (getPaddingTop() + (height - scrapHeight) / 2.0f);
                if (startOffset == -1 && startPosition == 0) {
                    // layout the first position item in center
                    int left = (int) (getPaddingLeft() + (getHorizontalSpace() - scrapWidth) / 2.f);
                    scrapRect.set(left, topOffset, left + scrapWidth, topOffset + scrapHeight);
                } else {
                    scrapRect.set(startOffset, topOffset, startOffset + scrapWidth, topOffset + scrapHeight);
                }
                layoutDecorated(scrap, scrapRect.left, scrapRect.top, scrapRect.right, scrapRect.bottom);
                startOffset = scrapRect.right;
                mLastVisiblePos = i;
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "fillWithHorizontal,layout:mLastVisiblePos: " + mLastVisiblePos);
                }
            }
        } else {
            //dx<0
            if (getChildCount() > 0) {
                View firstView = getChildAt(0);
                startPosition = getPosition(firstView) - 1; //start layout from previous position item
                startOffset = getDecoratedLeft(firstView);
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "fillWithHorizontal:to left startPosition:" + startPosition + ",startOffset:" + startOffset + ",leftEdge:" + leftEdge + ",child count:" + getChildCount());
                }
            }
            for (int i = startPosition; i >= 0 && startOffset > leftEdge + dx; i--) {
                scrapRect = getState().mItemsFrames.get(i);
                scrap = recycler.getViewForPosition(i);
                addView(scrap, 0);
                if (scrapRect == null) {
                    scrapRect = new Rect();
                    getState().mItemsFrames.put(i, scrapRect);
                }
                measureChildWithMargins(scrap, 0, 0);
                scrapWidth = getDecoratedMeasuredWidth(scrap);
                scrapHeight = getDecoratedMeasuredHeight(scrap);
                topOffset = (int) (getPaddingTop() + (height - scrapHeight) / 2.0f);
                scrapRect.set(startOffset - scrapWidth, topOffset, startOffset, topOffset + scrapHeight);
                layoutDecorated(scrap, scrapRect.left, scrapRect.top, scrapRect.right, scrapRect.bottom);
                startOffset = scrapRect.left;
                mFirstVisiblePosition = i;
            }
        }
    }

    /**
     * 获取水平方向可用距离
     *
     * @return
     */
    private int getHorizontalSpace() {
        return getWidth() - getPaddingRight() - getPaddingLeft();
    }

    /**
     * 获取垂直方向可用距离
     *
     * @return
     */
    private int getVerticalSpace() {
        return getHeight() - getPaddingBottom() - getPaddingTop();
    }

    public State getState() {
        if (mState == null) {
            mState = new State();
        }
        return mState;
    }

    private int calculateScrollDirectionForPosition(int position) {
        if (getChildCount() == 0) {
            return LAYOUT_START;
        }
        final int firstChildPos = mFirstVisiblePosition;
        return position < firstChildPos ? LAYOUT_START : LAYOUT_END;
    }

    @Override
    public PointF computeScrollVectorForPosition(int targetPosition) {
        final int direction = calculateScrollDirectionForPosition(targetPosition);
        PointF outVector = new PointF();
        if (direction == 0) {
            return null;
        }
        if (mOrientation == HORIZONTAL) {
            outVector.x = direction;
            outVector.y = 0;
        } else {
            outVector.x = 0;
            outVector.y = direction;
        }
        return outVector;
    }

    /**
     * @author chensuilun
     */
    class State {
        /**
         * Record all item view 's last position after last layout
         */
        SparseArray<Rect> mItemsFrames;

        /**
         * RecycleView 's current scroll distance since first layout
         */
        int mScrollDelta;

        public State() {
            mItemsFrames = new SparseArray<Rect>();
            mScrollDelta = 0;
        }
    }


    @Override
    public boolean canScrollHorizontally() {
        return mOrientation == HORIZONTAL;
    }


    @Override
    public boolean canScrollVertically() {
        return mOrientation == VERTICAL;
    }


    //横向滑动触发
    @Override
    public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler, RecyclerView.State state) {
        // When dx is positive，finger fling from right to left(←)，scrollX+
        //当dx是正数时，代表手指从右向左惯性滑动
        if (getChildCount() == 0 || dx == 0) {
            return 0;
        }
        int delta = -dx;
        //寻找中心点位置
        int parentCenter = (getOrientationHelper().getEndAfterPadding() - getOrientationHelper().getStartAfterPadding()) / 2 + getOrientationHelper().getStartAfterPadding();
        View child;
        if (dx > 0) {
            //If we've reached the last item, enforce limits
            //如果到达最后一个，强行限制delta
            if (getPosition(getChildAt(getChildCount() - 1)) == getItemCount() - 1) {
                child = getChildAt(getChildCount() - 1);
                delta = -Math.max(0, Math.min(dx, (child.getRight() - child.getLeft()) / 2 + child.getLeft() - parentCenter));
            }
            if (mLastVisiblePos == getItemCount() - 1) {
                Log.e("darkwh", "mLastVisiblePos");
            }
        } else {
            //If we've reached the first item, enforce limits
            //如果到达第一个，强行限制delta
            if (mFirstVisiblePosition == 0) {
                child = getChildAt(0);
                delta = -Math.min(0, Math.max(dx, ((child.getRight() - child.getLeft()) / 2 + child.getLeft()) - parentCenter));
            }
            if (mFirstVisiblePosition == 0) {
                Log.e("darkwh", "mFirstVisiblePosition");
            }
        }
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "scrollHorizontallyBy: dx:" + dx + ",fixed:" + delta);
        }
        //记录从第一次layout之后的滑动距离
        getState().mScrollDelta += -delta;
//        if (getState().mScrollDelta <= -500) {
//            getState().mScrollDelta = -500;
//            return 0;
//        }
        //填充画面(重新layout子view)
        fillCover(recycler, state, -delta);
        //将所有view整体进行水平平移
        offsetChildrenHorizontal(delta);
        return -delta;
    }

    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (getChildCount() == 0 || dy == 0) {
            return 0;
        }
        int delta = -dy;
        int parentCenter = (getOrientationHelper().getEndAfterPadding() - getOrientationHelper().getStartAfterPadding()) / 2 + getOrientationHelper().getStartAfterPadding();
        View child;
        if (dy > 0) {
            //If we've reached the last item, enforce limits
            if (getPosition(getChildAt(getChildCount() - 1)) == getItemCount() - 1) {
                child = getChildAt(getChildCount() - 1);
                delta = -Math.max(0, Math.min(dy, (getDecoratedBottom(child) - getDecoratedTop(child)) / 2 + getDecoratedTop(child) - parentCenter));
            }
        } else {
            //If we've reached the first item, enforce limits
            if (mFirstVisiblePosition == 0) {
                child = getChildAt(0);
                delta = -Math.min(0, Math.max(dy, (getDecoratedBottom(child) - getDecoratedTop(child)) / 2 + getDecoratedTop(child) - parentCenter));
            }
        }
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "scrollVerticallyBy: dy:" + dy + ",fixed:" + delta);
        }
        getState().mScrollDelta += -delta;
        fillCover(recycler, state, -delta);
        offsetChildrenVertical(delta);
        return -delta;
    }

    public OrientationHelper getOrientationHelper() {
        if (mOrientation == HORIZONTAL) {
            if (mHorizontalHelper == null) {
                mHorizontalHelper = OrientationHelper.createHorizontalHelper(this);
            }
            return mHorizontalHelper;
        } else {
            if (mVerticalHelper == null) {
                mVerticalHelper = OrientationHelper.createVerticalHelper(this);
            }
            return mVerticalHelper;
        }
    }

    /**
     * @author chensuilun
     */
    public static class LayoutParams extends RecyclerView.LayoutParams {

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(ViewGroup.MarginLayoutParams source) {
            super(source);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }

        public LayoutParams(RecyclerView.LayoutParams source) {
            super(source);
        }
    }


    private ItemTransformer mItemTransformer;


    public void setItemTransformer(ItemTransformer itemTransformer) {
        mItemTransformer = itemTransformer;
    }

    /**
     * A ItemTransformer is invoked whenever a attached item is scrolled.
     * This offers an opportunity for the application to apply a custom transformation
     * to the item views using animation properties.
     */
    public interface ItemTransformer {

        /**
         * Apply a property transformation to the given item.
         *
         * @param layoutManager Current LayoutManager
         * @param item          Apply the transformation to this item
         * @param fraction      of page relative to the current front-and-center position of the pager.
         *                      0 is front and center. 1 is one full
         *                      page position to the right, and -1 is one page position to the left.
         */
        void transformItem(GalleryLayoutManager layoutManager, View item, float fraction);
    }

    /**
     * Listen for changes to the selected item
     *
     * @author chensuilun
     */
    public interface OnItemSelectedListener {
        /**
         * @param recyclerView The RecyclerView which item view belong to.
         * @param item         The current selected view
         * @param position     The current selected view's position
         */
        void onItemSelected(RecyclerView recyclerView, View item, int position);
    }

    private OnItemSelectedListener mOnItemSelectedListener;

    public void setOnItemSelectedListener(OnItemSelectedListener onItemSelectedListener) {
        mOnItemSelectedListener = onItemSelectedListener;
    }

    public void attach(RecyclerView recyclerView) {
        this.attach(recyclerView, -1);
    }

    /**
     * @param recyclerView
     * @param selectedPosition
     */
    public void attach(RecyclerView recyclerView, int selectedPosition) {
        if (recyclerView == null) {
            throw new IllegalArgumentException("The attach RecycleView must not null!!");
        }
        mRecyclerView = recyclerView;
        mInitialSelectedPosition = Math.max(0, selectedPosition);
        recyclerView.setLayoutManager(this);
//        mSnapHelper.attachToRecyclerView(recyclerView);
        recyclerView.addOnScrollListener(mInnerScrollListener);
    }

    RecyclerView mRecyclerView;


    public void setCallbackInFling(boolean callbackInFling) {
        mCallbackInFling = callbackInFling;
    }

    /**
     * Inner Listener to listen for changes to the selected item
     *
     * @author chensuilun
     */
    private class InnerScrollListener extends RecyclerView.OnScrollListener {
        int mState;
        boolean mCallbackOnIdle;

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
//            View snap = mSnapHelper.findSnapView(recyclerView.getLayoutManager());
//            if (snap != null) {
//                int selectedPosition = recyclerView.getLayoutManager().getPosition(snap);
//                Log.e("darkwh", "onScrolled  selectedPosition is ----->" + selectedPosition);
//                if (selectedPosition != mCurSelectedPosition) {
//                    if (mCurSelectedView != null) {
//                        mCurSelectedView.setSelected(false);
//                    }
//                    mCurSelectedView = snap;
//                    mCurSelectedView.setSelected(true);
//                    mCurSelectedPosition = selectedPosition;
//                    Log.e("darkwh", "onScrolled   mCurSelectedPosition is" + mCurSelectedPosition);
//                    if (!mCallbackInFling && mState != SCROLL_STATE_IDLE) {
//                        if (BuildConfig.DEBUG) {
//                            Log.v(TAG, "ignore selection change callback when fling ");
//                        }
//                        mCallbackOnIdle = true;
//                        return;
//                    }
//                    if (mOnItemSelectedListener != null) {
//                        mOnItemSelectedListener.onItemSelected(recyclerView, snap, mCurSelectedPosition);
//                    }
//                }
//            }
//            if (BuildConfig.DEBUG) {
//                Log.v(TAG, "onScrolled: dx:" + dx + ",dy:" + dy);
//            }
        }

        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
//            mState = newState;
//            if (BuildConfig.DEBUG) {
//                Log.v(TAG, "onScrollStateChanged: " + newState);
//            }
//            if (mState == SCROLL_STATE_IDLE) {
//                View snap = mSnapHelper.findSnapView(recyclerView.getLayoutManager());
//                if (snap != null) {
//                    int selectedPosition = recyclerView.getLayoutManager().getPosition(snap);
//                    if (selectedPosition != mCurSelectedPosition) {
//                        if (mCurSelectedView != null) {
//                            mCurSelectedView.setSelected(false);
//                        }
//                        mCurSelectedView = snap;
//                        mCurSelectedView.setSelected(true);
//                        mCurSelectedPosition = selectedPosition;
//                        Log.e("darkwh", "onScrollStateChanged   mCurSelectedPosition is" + mCurSelectedPosition);
//                        if (mOnItemSelectedListener != null) {
//                            mOnItemSelectedListener.onItemSelected(recyclerView, snap, mCurSelectedPosition);
//                        }
//                    } else if (!mCallbackInFling && mOnItemSelectedListener != null && mCallbackOnIdle) {
//                        mCallbackOnIdle = false;
//                        mOnItemSelectedListener.onItemSelected(recyclerView, snap, mCurSelectedPosition);
//                    }
//                } else {
//                    Log.e(TAG, "onScrollStateChanged: snap null");
//                }
//            }
        }
    }


    @Override
    public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state, int position) {
        GallerySmoothScroller linearSmoothScroller = new GallerySmoothScroller(recyclerView.getContext());
        linearSmoothScroller.setTargetPosition(position);
        startSmoothScroll(linearSmoothScroller);
    }

    /**
     * Implement to support {@link GalleryLayoutManager#smoothScrollToPosition(RecyclerView, RecyclerView.State, int)}
     */
    private class GallerySmoothScroller extends LinearSmoothScroller {

        public GallerySmoothScroller(Context context) {
            super(context);
        }

        /**
         * Calculates the horizontal scroll amount necessary to make the given view in center of the RecycleView
         *
         * @param view The view which we want to make in center of the RecycleView
         * @return The horizontal scroll amount necessary to make the view in center of the RecycleView
         */
        public int calculateDxToMakeCentral(View view) {
            final RecyclerView.LayoutManager layoutManager = getLayoutManager();
            if (layoutManager == null || !layoutManager.canScrollHorizontally()) {
                return 0;
            }
            final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) view.getLayoutParams();
            final int left = layoutManager.getDecoratedLeft(view) - params.leftMargin;
            final int right = layoutManager.getDecoratedRight(view) + params.rightMargin;
            final int start = layoutManager.getPaddingLeft();
            final int end = layoutManager.getWidth() - layoutManager.getPaddingRight();
            final int childCenter = left + (int) ((right - left) / 2.0f);
            final int containerCenter = (int) ((end - start) / 2.f);
            return containerCenter - childCenter;
        }

        /**
         * Calculates the vertical scroll amount necessary to make the given view in center of the RecycleView
         *
         * @param view The view which we want to make in center of the RecycleView
         * @return The vertical scroll amount necessary to make the view in center of the RecycleView
         */
        public int calculateDyToMakeCentral(View view) {
            final RecyclerView.LayoutManager layoutManager = getLayoutManager();
            if (layoutManager == null || !layoutManager.canScrollVertically()) {
                return 0;
            }
            final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams)
                    view.getLayoutParams();
            final int top = layoutManager.getDecoratedTop(view) - params.topMargin;
            final int bottom = layoutManager.getDecoratedBottom(view) + params.bottomMargin;
            final int start = layoutManager.getPaddingTop();
            final int end = layoutManager.getHeight() - layoutManager.getPaddingBottom();
            final int childCenter = top + (int) ((bottom - top) / 2.0f);
            final int containerCenter = (int) ((end - start) / 2.f);
            return containerCenter - childCenter;
        }


        @Override
        protected void onTargetFound(View targetView, RecyclerView.State state, Action action) {
            final int dx = calculateDxToMakeCentral(targetView);
            final int dy = calculateDyToMakeCentral(targetView);
            final int distance = (int) Math.sqrt(dx * dx + dy * dy);
            final int time = calculateTimeForDeceleration(distance);
            if (time > 0) {
                action.update(-dx, -dy, time, mDecelerateInterpolator);
            }
        }
    }
}
