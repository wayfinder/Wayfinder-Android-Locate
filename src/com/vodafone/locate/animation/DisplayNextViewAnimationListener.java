/*******************************************************************************
 * Copyright (c) 1999-2010, Vodafone Group Services
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions 
 * are met:
 * 
 *     * Redistributions of source code must retain the above copyright 
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above 
 *       copyright notice, this list of conditions and the following 
 *       disclaimer in the documentation and/or other materials provided 
 *       with the distribution.
 *     * Neither the name of Vodafone Group Services nor the names of its 
 *       contributors may be used to endorse or promote products derived 
 *       from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE 
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING 
 * IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY 
 * OF SUCH DAMAGE.
 ******************************************************************************/
package com.vodafone.locate.animation;

import android.os.Handler;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;

public final class DisplayNextViewAnimationListener implements Animation.AnimationListener {
	private Handler handler;
	private SwapViews swapViews;
    private View view1;
    private View view2;
    private boolean flipGridIn;

	public DisplayNextViewAnimationListener(Handler handler, boolean flipGridIn, View parent, View view1, View view2) {
		this.handler = handler;
		this.swapViews = new SwapViews(flipGridIn, parent, view1, view2);
		this.flipGridIn = flipGridIn;
		this.view1 = view1;
		this.view2 = view2;
	}

	public void onAnimationStart(Animation animation) {
		if(this.flipGridIn) {
		    view1.setVisibility(View.VISIBLE);
		    view2.setVisibility(View.GONE);
		}
		else {
            view1.setVisibility(View.GONE);
            view2.setVisibility(View.VISIBLE);
		}
	}

	public void onAnimationEnd(Animation animation) {
		if(this.handler != null) {
			this.handler.postAtFrontOfQueue(this.swapViews);
		}
	}

	public void onAnimationRepeat(Animation animation) {
	}

	private static class SwapViews implements Runnable {
		private boolean flipGridIn;
		private View parent;
		private View view1;
		private View view2;
		private Flip3dAnimation rotationClock;
		private Flip3dAnimation rotationCounterClock;

		public SwapViews(boolean flipGridIn, View parent, View image1, View image2) {
			this.flipGridIn = flipGridIn;
			this.parent = parent;
			this.view1 = image1;
			this.view2 = image2;
			final float centerX = parent.getWidth() / 2.0f;
			final float centerY = parent.getHeight() / 2.0f;

			rotationClock = new Flip3dAnimation(90, 0, centerX, centerY);
			rotationClock.setDuration(300);
			rotationClock.setFillAfter(true);
			rotationClock.setInterpolator(new DecelerateInterpolator());

			rotationCounterClock = new Flip3dAnimation(-90, 0, centerX, centerY);
			rotationCounterClock.setDuration(300);
			rotationCounterClock.setFillAfter(true);
			rotationCounterClock.setInterpolator(new DecelerateInterpolator());
		}

		public void run() {
			if (flipGridIn) {
				parent.startAnimation(rotationClock);
				view1.setVisibility(View.GONE);
				view1.setEnabled(false);
				view2.setVisibility(View.VISIBLE);
				view2.setEnabled(true);
				//view2.requestFocus();
				
			} else {
				parent.startAnimation(rotationCounterClock);
				view2.setVisibility(View.GONE);
				view2.setEnabled(false);
				view1.setVisibility(View.VISIBLE);
			//	view1.requestFocus();
				view1.setEnabled(true);
			}
		}
	}
}
