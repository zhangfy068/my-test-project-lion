/*
 * CopyRight (C) 2013 NewTech CORP LTD.
 * ProcessInfo.java
 */
package com.newtech.taskmanager;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Checkable;
import android.widget.LinearLayout;

/**
 * This is a view group for check list item.
 * This class wrapping actual checkable view (e.g. CheckedTextView)
 * and contains additional image or label.
 */
public final class ChecklistItem extends LinearLayout implements Checkable {
    /**
     * actual checkable object.
     */
    protected Checkable mCheckable;

    /**
     * factory method.
     * @param aContext activity context
     * @param aInflater layout inflater
     * @param aLayout layout id
     * @param aVg parent view group
     * @return new instance
     */
    public static ChecklistItem create(final Context aContext,
            final LayoutInflater aInflater, final int aLayout,
            final ViewGroup aVg) {
        ChecklistItem self = new ChecklistItem(aContext);
        self.setOrientation(LinearLayout.HORIZONTAL);
        aInflater.inflate(aLayout, self);
        return self;
    }

    /**
     * constructor.
     * @param aContext activity context
     */
    protected ChecklistItem(final Context aContext) {
        super(aContext);
    }

    /**
     * set actual checkable object.
     * @param aCheckable checkable that actually handle checking event.
     */
    public void setCheckable(final Checkable aCheckable) {
        mCheckable = aCheckable;
    }

    /**
     * getter of checking state.
     * forwards to mCheckable.
     * @return checked or not
     */
    public boolean isChecked() {
        if (mCheckable != null) {
            return mCheckable.isChecked();
        } else {
            return false;
        }
    }

    /**
     * setter of checking state.
     * forwards to mCheckable.
     * @param aChecked check or uncheck
     */
    public void setChecked(final boolean aChecked) {
        if (mCheckable != null) {
            mCheckable.setChecked(aChecked);
        }
    }

    /**
     * toggle checking state.
     * forwards to mCheckable.
     */
    public void toggle() {
        if (mCheckable != null) {
            mCheckable.toggle();
        }
    }
}
