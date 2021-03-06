/*
 *   Copyright (c) 2009 - 2015 Carmen Alvarez
 *
 *   This file is part of Nounours for Android.
 *
 *   Nounours for Android is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Nounours for Android is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with Nounours for Android.  If not, see <http://www.gnu.org/licenses/>.
 */

package ca.rmen.nounours.android.handheld.nounours;

import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

import ca.rmen.nounours.Nounours;

/**
 * Manages touch events for Nounours on the Android device.
 *
 * @author Carmen Alvarez
 */
public class TouchListener implements OnTouchListener {

    private final GestureDetector mGestureDetector;
    private final Nounours mNounours;

    public TouchListener(Nounours nounours,
                         GestureDetector gestureDetector) {
        mNounours = nounours;
        mGestureDetector = gestureDetector;
    }

    /**
     * The user touched, released, or moved.
     *
     * @see android.view.View.OnTouchListener#onTouch(android.view.View,
     * android.view.MotionEvent)
     */
    @Override
    public boolean onTouch(final View v, final MotionEvent event) {

        if (mGestureDetector != null) {
            mGestureDetector.onTouchEvent(event);
        }
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            mNounours.onPress((int) event.getX(), (int) event.getY());
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            mNounours.onRelease();
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            mNounours.onMove((int) event.getX(), (int) event.getY());
        }
        return true;
    }
}
