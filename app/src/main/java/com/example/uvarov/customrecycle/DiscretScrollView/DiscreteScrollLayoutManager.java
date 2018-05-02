package com.example.uvarov.customrecycle.DiscretScrollView;

import android.content.Context;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.support.v4.view.accessibility.AccessibilityEventCompat;
import android.support.v4.view.accessibility.AccessibilityRecordCompat;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;


public class DiscreteScrollLayoutManager extends RecyclerView.LayoutManager {

    private static final String EXTRA_POSITION = "extra_position";

    private static final int DIRECTION_START = -1;
    private static final int DIRECTION_END = 1;
    private static final int NO_POSITION = -1;
    private static final int DEFAULT_TIME_FOR_ITEM_SETTLE = 150;

    private int childViewWidth;
    private int childViewHalfWidth;
    private int childViewHeight;
    private int recyclerCenterX;
    private int recyclerCenterY;

    private int currentScrollState;
    private int scrollToChangeTheCurrent;

    private int scrolled;
    private int pendingScroll;
    private int currentPosition;
    private int pendingPosition;

    private int scrolledSum;

    private Context context;

    private int timeForItemSettle;

    private SparseArray<View> detachedCache;

    private List<DiscreteScrollItemTransformer> itemTransformers = new ArrayList<>();
    private ScrollStateListener scrollStateListener;

    int targetOffset;

    public DiscreteScrollLayoutManager(Context c) {
        this.context = c;
        this.timeForItemSettle = DEFAULT_TIME_FOR_ITEM_SETTLE;
        this.pendingPosition = NO_POSITION;
        this.currentPosition = NO_POSITION;
        this.detachedCache = new SparseArray<>();
        setAutoMeasureEnabled(true);
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (state.getItemCount() == 0) {
            removeAndRecycleAllViews(recycler);
            currentPosition = pendingPosition = NO_POSITION;
            scrolled = pendingScroll = 0;
            return;
        }

        boolean isFirstOrEmptyLayout = getChildCount() == 0;
        if (isFirstOrEmptyLayout) {
            initChildDimensions(recycler);
        }

        recyclerCenterX = getWidth() / 2;
        recyclerCenterY = getHeight() / 2;

        detachAndScrapAttachedViews(recycler);

        fill(recycler);

        applyItemTransformToChildren();

        if (isFirstOrEmptyLayout) {
            notifyFirstLayoutCompleted();
        }
    }

    private void initChildDimensions(RecyclerView.Recycler recycler) {
        View viewToMeasure = recycler.getViewForPosition(0);
        addView(viewToMeasure);
        measureChildWithMargins(viewToMeasure, 0, 0);

        childViewWidth = getDecoratedMeasuredWidth(viewToMeasure);
        childViewHalfWidth = childViewWidth / 2;
        childViewHeight = getDecoratedMeasuredHeight(viewToMeasure);

        //This is the distance between adjacent view's x-center coordinates
        scrollToChangeTheCurrent = childViewWidth;

        detachAndScrapView(viewToMeasure, recycler);
    }

    private void fill(RecyclerView.Recycler recycler) {
        cacheAndDetachAttachedViews();

        int startX = recyclerCenterX - scrolledSum;
        //int topPosition = (scrolledSum + (childViewWidth / 2)) / childViewWidth;
        int topPosition = scrolledSum / childViewWidth;
        topPosition = Math.min(Math.max(topPosition, 0), getItemCount() - 1);

//        final int childTop = recyclerCenterY - childHalfHeight;
//        final int childBottom = recyclerCenterY + childHalfHeight;

        //Layout items to the left of the current item
//        int viewLeft = currentViewCenterX - childViewHalfWidth;
//        int position = currentPosition - 1;
//        while (position >= 0 && viewLeft > 0) {
//            layoutView(recycler, position, viewLeft - childViewWidth, 0, viewLeft, childViewHeight);
//            viewLeft -= childViewWidth;
//            position--;
//        }

        for(int leftPosition = 0; leftPosition <= topPosition - 1; leftPosition++) {
            if(topPosition - leftPosition > 3) {
                continue;
            }
            float itemOffsetFromCenter = (leftPosition * childViewWidth) - scrolledSum;
            float itemSelfOffset = calculateItemOffset(itemOffsetFromCenter);
            float leftViewCenter = recyclerCenterX + itemOffsetFromCenter - itemSelfOffset;
            if(leftViewCenter + childViewHalfWidth > 0) {
                layoutView(recycler, leftPosition, (int)(leftViewCenter - childViewHalfWidth), 0, (int)(leftViewCenter + childViewHalfWidth), childViewHeight);
            }
        }

        //Layout items to the right of the current item
//        int viewRight = currentViewCenterX + childViewHalfWidth;
//        position = currentPosition + 1;
//        while (position < getItemCount() && viewRight < getWidth()) {
//            layoutView(recycler, position, viewRight, 0, viewRight + childViewWidth, childViewHeight);
//            viewRight += childViewWidth;
//            position++;
//        }

        for(int rightPosition =  getItemCount() - 1; rightPosition >= topPosition + 1; rightPosition--) {
            if(rightPosition - topPosition > 3) {
                continue;
            }
            float itemOffsetFromCenter = (rightPosition * childViewWidth) - scrolledSum;
            float itemSelfOffset = calculateItemOffset(itemOffsetFromCenter);
            float rightViewCenter = recyclerCenterX + itemOffsetFromCenter - itemSelfOffset;
            if(rightViewCenter - childViewHalfWidth < getWidth()) {
                layoutView(recycler, rightPosition, (int)(rightViewCenter - childViewHalfWidth), 0, (int)(rightViewCenter + childViewHalfWidth), childViewHeight);
            }
        }

        //Layout current
        float itemOffsetFromCenter = (topPosition * childViewWidth) - scrolledSum;
        float itemSelfOffset = calculateItemOffset(itemOffsetFromCenter);
        float topViewCenter = recyclerCenterX + itemOffsetFromCenter - itemSelfOffset;
        layoutView(recycler, topPosition, (int)(topViewCenter - childViewHalfWidth), 0, (int)(topViewCenter + childViewHalfWidth), childViewHeight);

        recycleViewsAndClearCache(recycler);
    }

    private float calculateItemOffset(float x) {
        float sign = Math.signum(x);
        x = Math.abs(x);
        return sign * ((0.0004166f * x * x) + (0.375f * x) + 0);
    }

    private void layoutView(RecyclerView.Recycler recycler, final int position, int l, int t, int r, int b) {
        View v = detachedCache.get(position);
        //if (v == null) {
            v = recycler.getViewForPosition(position);
            addView(v);
            measureChildWithMargins(v, 0, 0);
            layoutDecoratedWithMargins(v, l, t, r, b);
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //scrollToPosition(position);
                    targetOffset = position * scrollToChangeTheCurrent;
                    pendingScroll = -(scrolledSum - targetOffset);
                    startSmoothPendingScroll();
                }
            });
        //} else {
        //    attachView(v);
        //    detachedCache.remove(position);
        //}
    }

    private void cacheAndDetachAttachedViews() {
        detachedCache.clear();
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            detachedCache.put(getPosition(child), child);
        }

        for (int i = 0; i < detachedCache.size(); i++) {
            detachView(detachedCache.valueAt(i));
        }
    }

    private void recycleViewsAndClearCache(RecyclerView.Recycler recycler) {
        for (int i = 0; i < detachedCache.size(); i++) {
            recycler.recycleView(detachedCache.valueAt(i));
        }
        detachedCache.clear();
    }

    @Override
    public void onItemsAdded(RecyclerView recyclerView, int positionStart, int itemCount) {
        if (currentPosition == NO_POSITION) {
            currentPosition = 0;
        } else if (currentPosition >= positionStart) {
            currentPosition += itemCount;
        }
    }

    @Override
    public void onItemsRemoved(RecyclerView recyclerView, int positionStart, int itemCount) {
        if (getItemCount() == 0) {
            currentPosition = NO_POSITION;
        } else if (currentPosition >= positionStart) {
            currentPosition = Math.max(0, currentPosition - itemCount);
        }
    }

    @Override
    public void onItemsChanged(RecyclerView recyclerView) {
        //notifyDataSetChanged() was called. We need to ensure that currentPosition is not out of bounds
        currentPosition = Math.min(Math.max(0, currentPosition), getItemCount() - 1);
    }

    @Override
    public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (getChildCount() == 0) {
            return 0;
        }

        int direction = dxToDirection(dx);
//        int leftToScroll = calculateAllowedScrollIn(direction);
//        if (leftToScroll &lt;= 0) {
//            return 0;
//        }
//
//        int delta = Math.min(leftToScroll, Math.abs(dx)) * direction;
        int delta = Math.abs(dx) * direction;
        scrolledSum += delta;

        if (scrolledSum < -scrollToChangeTheCurrent || (scrolledSum > scrollToChangeTheCurrent * getItemCount())) {
            scrolledSum -= delta;
            return 0;
        }

        scrolled += delta;
//        if (pendingScroll != 0) {
//            pendingScroll -= delta;
//        }

        offsetChildrenHorizontal(-delta);

        View firstChild = getFirstChild(), lastChild = getLastChild();
        boolean isNewVisibleFromLeft = getDecoratedLeft(firstChild) > 0 && getPosition(firstChild) > 0;
        boolean isNewVisibleFromRight = getDecoratedRight(lastChild) < getWidth() && getPosition(lastChild) < getItemCount() - 1;

//        if (isNewVisibleFromLeft || isNewVisibleFromRight) {
        fill(recycler);
//        }

//        notifyScroll();

        applyItemTransformToChildren();

        return delta;
    }

    private void applyItemTransformToChildren() {
        for (DiscreteScrollItemTransformer itemTransformer : itemTransformers) {
            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                float deltaXFromCenter = child.getLeft() + childViewHalfWidth - recyclerCenterX;
                float maxTransformDistance = 225;
                float ratio = deltaXFromCenter / maxTransformDistance;
                float rotateAngle = -60.0f * ratio;
//                child.setPivotX(deltaXFromCenter > 0 ? childViewWidth : 0);
//                child.setRotationY(rotateAngle);
//               child.setTranslationX(-childViewHalfWidth * (Math.signum(ratio) * ratio * ratio));
//                child.setRotationY(10.0f);
//                itemTransformer.transformItem(child, getCenterRelativePositionOf(child));
            }
        }
    }

    @Override
    public void scrollToPosition(int position) {
//        if (currentPosition == position) {
//            return;
//        }

//        scrolled = (currentPosition - position) * scrollToChangeTheCurrent;


        /*
        currentPosition = position;
        scrolledSum = position * scrollToChangeTheCurrent;
        scrolled = 0;
        notifyScrollToPosition(position);
              requestLayout();
*/

//        currentPosition = position;

//        scrolled = 0;
    }

//    @Override
//    public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state, int position) {
//        if (currentPosition == position) {
//            return;
//        }
//
//        pendingScroll = -scrolled;
//        int requiredDx = Math.abs(position - currentPosition)
//                * dxToDirection(position - currentPosition)
//                * scrollToChangeTheCurrent;
//        pendingScroll += requiredDx;
//
//        pendingPosition = position;
//        startSmoothPendingScroll();
//    }

    @Override
    public boolean canScrollHorizontally() {
        return true;
    }

    @Override
    public boolean canScrollVertically() {
        return false;
    }

    @Override
    public void onScrollStateChanged(int state) {
//        if (currentScrollState == RecyclerView.SCROLL_STATE_IDLE &amp;&amp; currentScrollState != state) {
//            notifyScrollStart();
//        }

        if (state == RecyclerView.SCROLL_STATE_IDLE) {
            //Scroll is not finished until current view is centered
            boolean isScrollEnded = onScrollEnd();
            if (isScrollEnded) {
                notifyScrollEnd();
            } else {
                //Scroll continues and we don't want to set currentScrollState to STATE_IDLE,
                //because this will then trigger notifyScrollStart()
                return;
            }
        } else if (state == RecyclerView.SCROLL_STATE_DRAGGING) {
            onDragStart();
        }
        currentScrollState = state;
    }

    /**
     * @return true if scroll is ended and we don't need to settle items
     */
    /*
    private boolean onScrollEnd() {
        if (pendingPosition != NO_POSITION) {
            currentPosition = pendingPosition;
            pendingPosition = NO_POSITION;
            scrolled = 0;
        }

        int scrollDirection = dxToDirection(scrolled);
        if (Math.abs(scrolled) == scrollToChangeTheCurrent) {
            currentPosition += scrollDirection;
            scrolled = 0;
        }

        if (isAnotherItemCloserThanCurrent()) {
            pendingScroll = getHowMuchIsLeftToScroll(scrolled);
        } else {
            pendingScroll = -scrolled;
        }

        if (pendingScroll == 0) {
            return true;
        } else {
            startSmoothPendingScroll();
            return false;
        }
    }
*/
    private boolean onScrollEnd() {

        int scrollDirection = dxToDirection(scrolled);

//        int scrolledPositionCount = Math.abs(scrolled) / scrollToChangeTheCurrent;

        //if (Math.abs(scrolled) == scrollToChangeTheCurrent) {
//            currentPosition += scrollDirection;
        //    currentPosition = scrolledSum / scrollToChangeTheCurrent;
        //    scrolled = 0;
        //}

        if (Math.abs(scrolledSum) == Math.abs(targetOffset)) {
//            currentPosition += scrollDirection;
            //    currentPosition = scrolledSum / scrollToChangeTheCurrent;
            currentPosition = scrolledSum / scrollToChangeTheCurrent;
            scrolled = 0;
            return true;
        }

//        currentPosition = scrolledSum / scrollToChangeTheCurrent;
//        scrolled = 0;

//        if (isAnotherItemCloserThanCurrent()) {
//            pendingScroll = getHowMuchIsLeftToScroll(scrolled);
//        } else {
//            pendingScroll = -scrolled;
//        }

//        int targetPositionOffset = (scrolledSum + (childViewHalfWidth * dxToDirection(scrolled))) / scrollToChangeTheCurrent;
//        int targetPositionOffset = scrolledSum / scrollToChangeTheCurrent;

        int targetPositionOffset = (scrolledSum + childViewHalfWidth) / scrollToChangeTheCurrent;

        if (targetPositionOffset < 0) {
            targetPositionOffset = 0;
        }
        if (targetPositionOffset > getItemCount() - 1) {
            targetPositionOffset = getItemCount() - 1;
        }

        targetOffset = targetPositionOffset * scrollToChangeTheCurrent;

        pendingScroll = -(scrolledSum - targetOffset);

//        Log.d("hello", ""+targetPositionOffset);

//        pendingScroll = (scrolled % scrollToChangeTheCurrent) * -dxToDirection(scrolled);

        if (pendingScroll == 0) {
            return true;
        } else {
            startSmoothPendingScroll();
            return false;
        }
    }

    private void onDragStart() {
        //Here we need to:
        //1. Stop any pending scroll
        //2. Set currentPosition to position of the item that is closest to the center
//        boolean isScrollingThroughMultiplePositions = Math.abs(scrolled) &gt; scrollToChangeTheCurrent;
//        if (isScrollingThroughMultiplePositions) {
//            int scrolledPositions = scrolled / scrollToChangeTheCurrent;
//            currentPosition += scrolledPositions;
//            scrolled -= scrolledPositions * scrollToChangeTheCurrent;
//        }
//        if (isAnotherItemCloserThanCurrent()) {
//            int direction = dxToDirection(scrolled);
//            currentPosition += direction;
//            scrolled = -getHowMuchIsLeftToScroll(scrolled);
//        }
        pendingPosition = NO_POSITION;
        pendingScroll = 0;
    }

//    public void onFling(int velocity) {
//        int direction = dxToDirection(velocity);
//        int newPosition = currentPosition + direction;
//        boolean canFling = newPosition &gt;= 0 &amp;&amp; newPosition &lt; getItemCount();
//        if (canFling) {
//            pendingScroll = getHowMuchIsLeftToScroll(velocity);
//            if (pendingScroll != 0) {
//                startSmoothPendingScroll();
//            }
//        } else {
//            returnToCurrentPosition();
//        }
//    }

//    public void returnToCurrentPosition() {
//        pendingScroll = -scrolled;
//        if (pendingScroll != 0) {
//            startSmoothPendingScroll();
//        }
//    }

    /*
    private int calculateAllowedScrollIn(@Direction int direction) {
        if (pendingScroll != 0) {
            return Math.abs(pendingScroll);
        }
        int allowedScroll;
        boolean isBoundReached;
        boolean isScrollDirectionAsBefore = direction * scrolled &gt; 0;
        if (direction == DIRECTION_START &amp;&amp; currentPosition == 0) {
            //We can scroll to the left when currentPosition == 0 only if we scrolled to the right before
            isBoundReached = scrolled == 0;
            allowedScroll = isBoundReached ? 0 : Math.abs(scrolled);
        } else if (direction == DIRECTION_END &amp;&amp; currentPosition == getItemCount() - 1) {
            //We can scroll to the right when currentPosition == last only if we scrolled to the left before
            isBoundReached = scrolled == 0;
            allowedScroll = isBoundReached ? 0 : Math.abs(scrolled);
        } else {
            isBoundReached = false;
            allowedScroll = isScrollDirectionAsBefore ?
                    scrollToChangeTheCurrent - Math.abs(scrolled) :
                    scrollToChangeTheCurrent + Math.abs(scrolled);
        }
        notifyBoundReached(isBoundReached);
        return allowedScroll;
    }
*/

    private void startSmoothPendingScroll() {
        LinearSmoothScroller scroller = new DiscreteLinearSmoothScroller(context);
        scroller.setTargetPosition(currentPosition);
        startSmoothScroll(scroller);
    }

    @Override
    public void onAdapterChanged(RecyclerView.Adapter oldAdapter, RecyclerView.Adapter newAdapter) {
        if (newAdapter.getItemCount() > 0) {
            pendingPosition = NO_POSITION;
            scrolled = pendingScroll = 0;
            currentPosition = 0;
        }
        removeAllViews();
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        if (pendingPosition != NO_POSITION) {
            currentPosition = pendingPosition;
        }
        bundle.putInt(EXTRA_POSITION, currentPosition);
        return bundle;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        Bundle bundle = (Bundle) state;
        currentPosition = bundle.getInt(EXTRA_POSITION);
    }

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    public void addItemTransformer(DiscreteScrollItemTransformer itemTransformer) {
        this.itemTransformers.add(itemTransformer);
    }

    public void setScrollStateListener(ScrollStateListener boundReachedListener) {
        this.scrollStateListener = boundReachedListener;
    }

    public void setTimeForItemSettle(int timeForItemSettle) {
        this.timeForItemSettle = timeForItemSettle;
    }

    public int getCurrentPosition() {
        return currentPosition;
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        if (getChildCount() > 0) {
            final AccessibilityRecordCompat record = AccessibilityEventCompat.asRecord(event);
            record.setFromIndex(getPosition(getFirstChild()));
            record.setToIndex(getPosition(getLastChild()));
        }
    }

    private float getCenterRelativePositionOf(View v) {
        int viewCenterX = getDecoratedLeft(v) + childViewHalfWidth;
        float distanceFromCenter = viewCenterX - recyclerCenterX;
        return Math.min(Math.max(-1f, distanceFromCenter / scrollToChangeTheCurrent), 1f);
    }

    private int getHowMuchIsLeftToScroll(int dx) {
        return (scrollToChangeTheCurrent - Math.abs(scrolled)) * dxToDirection(dx);
    }

    private boolean isAnotherItemCloserThanCurrent() {
        return Math.abs(scrolled) >= scrollToChangeTheCurrent * 0.6f;
    }

    @Direction
    private int dxToDirection(int dx) {
        return dx > 0 ? DIRECTION_END : DIRECTION_START;
    }

    private View getFirstChild() {
        return getChildAt(0);
    }

    private View getLastChild() {
        return getChildAt(getChildCount() - 1);
    }

    private void notifyScrollEnd() {
        if (scrollStateListener != null) {
            scrollStateListener.onScrollEnd();
        }
    }

    private void notifyScrollToPosition(int position) {
        if (scrollStateListener != null) {
            scrollStateListener.onScrollToPosition(position);
        }
    }

//    private void notifyScrollStart() {
//        if (scrollStateListener != null) {
//            scrollStateListener.onScrollStart();
//        }
//    }

//    private void notifyBoundReached(boolean isReached) {
//        if (scrollStateListener != null) {
//            scrollStateListener.onIsBoundReachedFlagChange(isReached);
//        }
//    }

//    private void notifyScroll() {
//        if (scrollStateListener != null) {
//            float position = -Math.min(Math.max(-1f,
//                    scrolled / (float) scrollToChangeTheCurrent),
//                    1f);
//            scrollStateListener.onScroll(position);
//        }
//    }

    private void notifyFirstLayoutCompleted() {
        if (scrollStateListener != null) {
            scrollStateListener.onCurrentViewFirstLayout();
        }
    }

    private class DiscreteLinearSmoothScroller extends LinearSmoothScroller {

        public DiscreteLinearSmoothScroller(Context context) {
            super(context);
        }

        @Override
        public int calculateDxToMakeVisible(View view, int snapPreference) {
            return -pendingScroll;
        }

        @Override
        protected int calculateTimeForScrolling(int dx) {
            float dist = Math.min(Math.abs(dx), scrollToChangeTheCurrent);
            return (int) (Math.max(0.01f, dist / scrollToChangeTheCurrent) * timeForItemSettle);
        }

        @Nullable
        @Override
        public PointF computeScrollVectorForPosition(int targetPosition) {
            return new PointF(-pendingScroll, 0);
        }
    }

    public interface ScrollStateListener {
        void onIsBoundReachedFlagChange(boolean isBoundReached);

        void onScrollEnd();

        void onCurrentViewFirstLayout();

        void onScrollToPosition(int position);
    }

    @IntDef({DIRECTION_START, DIRECTION_END})
    @Retention(RetentionPolicy.SOURCE)
    private @interface Direction {
    }
}