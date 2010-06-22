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
package com.vodafone.locate.dialog;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.vodafone.locate.R;
import com.vodafone.locate.view.ProgressBar;

public class AlertDialog extends Dialog {

    private static Timer timer;
    
    private TextView messageView;
    private TextView secondaryMessageView;
    private Button btnPos;
    private Button btnNeu;
    private Button btnNeg;
    private LinearLayout contentView;
    private ProgressBar progressbar;
    private ImageView imageTitle;
    private TextView textTitle;
    private LinearLayout layoutButtons;
    private LinearLayout layoutTitle;
    private LinearLayout layoutTimer;
    private TextView textTimer;
    private int dismissTimerTextId = R.string.qtn_andr_368_mess_closes_in_x_sec_txt;
    private TimerTask dismissTask;
    private long stopTime;

    private Runnable dismissRunnable = new Runnable() {
        public void run() {
            int timeLeft = (int) (stopTime - System.currentTimeMillis());
            String text = getContext().getResources().getString(dismissTimerTextId, (timeLeft / 1000));
            textTimer.setText(text);
        }
    };

	private CheckBox dontShowCheckBox;

    public AlertDialog(Context context) {
        super(context, R.style.style_dialog);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND, WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        this.setContentView(R.layout.dialog_alert);

        this.imageTitle = (ImageView) this.findViewById(R.id.image_title);
        this.imageTitle.setImageBitmap(null);
        
        this.textTitle = (TextView) this.findViewById(R.id.text_title);
        this.textTitle.setText("");

        this.progressbar = (ProgressBar) this.findViewById(R.id.progressbar);
        this.progressbar.setVisibility(View.GONE);
        
        this.messageView = (TextView) this.findViewById(R.id.text_message);
        this.secondaryMessageView = (TextView) this.findViewById(R.id.secondary_text_message);
        this.contentView = (LinearLayout) this.findViewById(R.id.layout_content);
        
        this.btnPos = (Button) this.findViewById(R.id.btn_positive);
        this.btnPos.setEnabled(true);
        this.btnPos.setVisibility(View.GONE);
        
        this.btnNeu = (Button) this.findViewById(R.id.btn_neutral);
        this.btnNeu.setVisibility(View.GONE);

        this.btnNeg = (Button) this.findViewById(R.id.btn_negative);
        this.btnNeg.setVisibility(View.GONE);

        this.layoutTitle = (LinearLayout) this.findViewById(R.id.layout_title);
        this.layoutTitle.setVisibility(View.GONE);
        
        this.layoutButtons = (LinearLayout) this.findViewById(R.id.layout_buttons);
        this.layoutButtons.setVisibility(View.GONE);
        
        this.layoutTimer = (LinearLayout) this.findViewById(R.id.layout_timer);
        this.layoutTimer.setVisibility(View.GONE);
        
        this.textTimer = (TextView) this.findViewById(R.id.text_timer);
        this.textTimer.setText("");
        
        this.dontShowCheckBox = (CheckBox) this.findViewById(R.id.cb_dont_show_again);
        this.dontShowCheckBox.setVisibility(View.GONE);
    }

    public void setMessage(int messageId) {
        this.messageView.setText(messageId);
        this.messageView.setVisibility(View.VISIBLE);
        this.secondaryMessageView.setVisibility(View.GONE);
        this.contentView.setVisibility(View.GONE);
    }

    public void setMessage(CharSequence messageId) {
        this.messageView.setText(messageId);
        this.messageView.setVisibility(View.VISIBLE);
        this.secondaryMessageView.setVisibility(View.GONE);
        this.contentView.setVisibility(View.GONE);        
    }
    
    public void setSecondaryMessage(String message) {
    	this.secondaryMessageView.setText(message);
        this.secondaryMessageView.setVisibility(View.VISIBLE);
        this.contentView.setVisibility(View.GONE);
    }
    
    public void setView(View view) {
        this.contentView.removeAllViews();
        this.contentView.addView(view, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
        this.contentView.setVisibility(View.VISIBLE);        
        this.messageView.setVisibility(View.GONE);
        this.secondaryMessageView.setVisibility(View.GONE);
    }

    public void setPositiveButton(int textId, final OnClickListener onClickListener) {
        this.layoutButtons.setVisibility(View.VISIBLE);
        this.btnPos.setVisibility(View.VISIBLE);
        this.btnPos.setText(textId);
        this.btnPos.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(onClickListener != null) {
                    onClickListener.onClick(AlertDialog.this, DialogInterface.BUTTON_POSITIVE);
                }
            }
        });
    }

    public void setNeutralButton(int textId, final OnClickListener onClickListener) {
        this.layoutButtons.setVisibility(View.VISIBLE);
        this.btnNeu.setVisibility(View.VISIBLE);

        //If negative button is not visible it means that neutral button�s background should be the right image
        if(this.btnNeg.getVisibility() != View.VISIBLE) {
        	this.btnNeu.setBackgroundResource(R.drawable.floating_button_right);
        }
        else {
        	this.btnNeu.setBackgroundResource(R.drawable.floating_button_middle);
        }
        
        this.btnNeu.setText(textId);
        this.btnNeu.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(onClickListener != null) {
                    onClickListener.onClick(AlertDialog.this, DialogInterface.BUTTON_NEUTRAL);
                }
            }
        });
    }

    public void setNegativeButton(int textId, final OnClickListener onClickListener) {
        this.layoutButtons.setVisibility(View.VISIBLE);
        this.btnNeg.setVisibility(View.VISIBLE);

        //reset neutral button�s background to the middle image
       	this.btnNeu.setBackgroundResource(R.drawable.floating_button_middle);

        this.btnNeg.setText(textId);
        this.btnNeg.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(onClickListener != null) {
                    onClickListener.onClick(AlertDialog.this, DialogInterface.BUTTON_NEGATIVE);
                }
            }
        });
    }

    public void setIcon(int iconId) {
        this.layoutTitle.setVisibility(View.VISIBLE);
        this.imageTitle.setImageResource(iconId);
    }
    
    @Override
    public void setTitle(CharSequence title) {
        this.layoutTitle.setVisibility(View.VISIBLE);
        this.textTitle.setText(title);
    }
    
    @Override
    public void setTitle(int titleId) {
        this.layoutTitle.setVisibility(View.VISIBLE);
        this.textTitle.setText(titleId);
    }
    
    public void setProgressbarVisible(boolean visible) {
        if(visible) {
            this.progressbar.setVisibility(View.VISIBLE);
        }
        else {
            this.progressbar.setVisibility(View.GONE);
        }
    }

    /**
     * @param autoDismissTime time in seconds before the dialog is dismissed. If time <= 0 the timer is removed from the dialog and the auto dismiss is cancelled
     */
    public void setAutoDismissTime(int autoDismissTimeInSeconds) {
        this.setAutoDismissTime(autoDismissTimeInSeconds, R.string.qtn_andr_368_mess_closes_in_x_sec_txt);
    }
    
    /**
     * @param autoDismissTime time in seconds before the dialog is dismissed. If time <= 0 the timer is removed from the dialog and the auto dismiss is cancelled
     */
    public void setAutoDismissTime(int autoDismissTimeInSeconds, int dismissTimerTextId) {
        if(autoDismissTimeInSeconds > 0) {
            this.dismissTimerTextId = dismissTimerTextId;
            this.stopTime = System.currentTimeMillis() + (autoDismissTimeInSeconds * 1000);
            layoutTimer.setVisibility(View.VISIBLE);
            
            Handler handler = layoutTimer.getHandler();
            if(handler != null){
	            handler.post(new Runnable() {
					public void run() {
			            layoutTimer.invalidate();
			            layoutTimer.requestLayout();
					}
				});
            }

            if(timer == null) {
                timer = new Timer("AlertDialog-Timer");
            }

            if(this.dismissTask != null) {
                this.dismissTask.cancel();
                this.dismissTask = null;
            }
            
            this.dismissTask = new TimerTask() {
                public void run() {
                    if(System.currentTimeMillis() > stopTime) {
                        AlertDialog.this.cancel();
                    }
                    else {
                        layoutTimer.getHandler().post(dismissRunnable);
                    }
                }
            };
            timer.schedule(this.dismissTask, 0, 1000);
        }
        else {
            if(this.dismissTask != null) {
                this.dismissTask.cancel();
                this.dismissTask = null;
            }
            this.layoutTimer.setVisibility(View.GONE);
            layoutTimer.invalidate();
            layoutTimer.requestLayout();
        }
    }
}
