/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// This project is using one of the Android Open Source files, specifically FingerPaint.java
// Please note that this has been changed from the original versions to incorporate the Pressure Pen technology

package com.option8.PressurePaint;

import java.util.Timer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

public class PressurePenActivity extends Activity
        implements ColorPickerDialog.OnColorChangedListener {
	
	private static MediaRecorder recorder = null;
	protected static int maxWidthPercent;
	protected static int minWidthPercent;
	private SharedPreferences prefs;
	private static Timer timer;
	private int testNumber = 0;
	private ScheduledExecutorService scheduler;
	private BroadcastReceiver receiver;
	public static final String PREFS_NAME = "MyPrefsFile";

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(new MyView(this));

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(0xFF000000);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(5);
        
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_HEADSET_PLUG);

        receiver = new BroadcastReceiver() {
          @Override
          public void onReceive(Context context, Intent intent) {
        	  if (intent.getIntExtra("state", -1) == 0){
        		  //state 0 = unplugged
        			scheduler.shutdown();
        			try {
        				if (scheduler.awaitTermination(500, TimeUnit.MILLISECONDS)){			
        			        if (recorder != null) {
        			        	recorder.stop();
        			        	recorder.release();
        			        	recorder = null;
        			        }
        				}
        			} catch (Exception e) {
        			}
        		  mPaint.setStrokeWidth(5);
        	  }
        	  else{
        		  initPressurePen();
        	  }
          }
        };

        registerReceiver(receiver, filter);

    }   

	@Override
	protected void onResume() {
		super.onResume();
		getPrefs();
        initPressurePen();
	}

	protected void initPressurePen() {
        if (recorder == null) {
//       	 you need to add the following line to the manifest for this to work
//       	 <uses-permission android:name="android.permission.RECORD_AUDIO"/>
       	 recorder = new MediaRecorder();
       	 recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
       	 recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
       	 recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
       	 recorder.setOutputFile("/dev/null"); 
       	 try {
				recorder.prepare();
			} catch (Exception e) {
				Log.e("prepare", "oops");
				e.printStackTrace();
			}
       	 recorder.start();
        }
        scheduler = Executors.newScheduledThreadPool(1);
        final Runnable beeper = new Runnable() {
        	public void run() {
        		float tempVolumeLevel = recorder.getMaxAmplitude();
        		//float tempVolumeLevel = recorder.getMaxAmplitude()/328;
				float maxPenPressurePercent = 65;
				final float multiplier = maxPenPressurePercent / 100;
				final int volumePercent = (int) (tempVolumeLevel * multiplier);
				if (recorder != null){
					getWindow().getDecorView().findViewById(android.R.id.content).
					post(new Runnable() {
						@Override
						public void run() {
							if (volumePercent != 0){
								setStrokeWidthAccordingly(volumePercent);
							}
						}
					});
					if (findViewById(R.id.debugTextView) != null){
						final TextView debugTextView = (TextView) findViewById(R.id.debugTextView);
						debugTextView.post(new Runnable() {
							@Override
							public void run() {
								if (volumePercent != 0){
									debugTextView.setText(String.valueOf(volumePercent));
								}
//									debugTextView.setText(String.valueOf(testNumberMethod()));
							}
						});
					}
				}
        	}
        };     
        
//		you need to add the following line to the manifest for this to work:
//		<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS"/>
//		this only checks if the Pen is plugged in at onCreate time
//		we use a broadcast receiver for the intent: ACTION_HEADSET_PLUG to check if this changes
		AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		boolean penConnected = am.isWiredHeadsetOn();
		if (penConnected){
			int refreshDelay = 100;
			scheduler.scheduleWithFixedDelay(beeper, 0, refreshDelay, TimeUnit.MILLISECONDS);
		}	
	}
	
	@SuppressWarnings("unused")
	private int testNumberMethod() {
		testNumber ++;
		return testNumber;
	}
	
	private void setStrokeWidthAccordingly(int volumePercent) {
		float width = 5;
		float absoluteMaxWidth = 99;
		float relativeMinWidth = minWidthPercent;
		float relativeMaxWidth = relativeMinWidth + ((absoluteMaxWidth - relativeMinWidth) *
				maxWidthPercent / 100);
		width = relativeMinWidth + (relativeMaxWidth - relativeMinWidth) *
				volumePercent / 100;
		mPaint.setStrokeWidth(width);
	}

	private void getPrefs() {
		final String PREFS_NAME = "MyPrefsFile";
		prefs = getSharedPreferences(PREFS_NAME, 0);
		maxWidthPercent = prefs.getInt("maxWidthPercent", 70);
		minWidthPercent = prefs.getInt("minWidthPercent", 10);
		
	}
	
	public Timer getTimer() {
		return timer;
	}
	
	public MediaRecorder getRecorder() {
		return recorder;
	}
	
    private Paint mPaint;
    
    public Paint getmPaint() {
		return mPaint;
	}

	public void colorChanged(int color) {
        mPaint.setColor(color);
    }

    public class MyView extends View {
        
        @SuppressWarnings("unused")
		private static final float MINP = 0.25f;
        @SuppressWarnings("unused")
		private static final float MAXP = 0.75f;
        
        private Bitmap  mBitmap;
        private Canvas  mCanvas;
        private Path    mPath;
        private Paint   mBitmapPaint;
        
        public MyView(Context c) {
            super(c);
            
            DisplayMetrics displaymetrics = new DisplayMetrics();
            
            getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
            
            int height = displaymetrics.heightPixels;
            int width = displaymetrics.widthPixels;

            mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            mCanvas = new Canvas(mBitmap);
            mPath = new Path();
            mBitmapPaint = new Paint(Paint.DITHER_FLAG);
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
        }
        
        @Override
        protected void onDraw(Canvas canvas) {
            canvas.drawColor(0xFFAAAAAA);
            
            canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
            
            canvas.drawPath(mPath, mPaint);
        }
        
        private float mX, mY;
        private static final float TOUCH_TOLERANCE = 2;
        
        private void touch_start(float x, float y) {
            mPath.reset();
            mPath.moveTo(x, y);
            mX = x;
            mY = y;
        }
        private void touch_move(float x, float y) {
            float dx = Math.abs(x - mX);
            float dy = Math.abs(y - mY);
            if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
                mPath.quadTo(mX, mY, (x + mX)/2, (y + mY)/2);
                mX = x;
                mY = y;
            }
        }
        private void touch_up() {
            mPath.lineTo(mX, mY);
            // commit the path to our screen
            mCanvas.drawPath(mPath, mPaint);
            // kill this so we don't double draw
            mPath.reset();
        }
        
        private void touch_all(float x, float y) {
        	touch_move(x, y);
            
            mPath.lineTo(mX, mY);
            // commit the path to our screen
            mCanvas.drawPath(mPath, mPaint);
            // kill this so we don't double draw
            mPath.reset();
            mPath.moveTo(x, y);
            mX = x;
            mY = y;
        }
        
        @Override
        public boolean onTouchEvent(MotionEvent event) {
            float x = event.getX();
            float y = event.getY();
            
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    touch_start(x, y);
                    invalidate();
                    break;
                case MotionEvent.ACTION_MOVE:
                	touch_all(x, y);
//                    touch_move(x, y);
                    invalidate();
                    break;
                case MotionEvent.ACTION_UP:
                    touch_up();
                    invalidate();
                    break;
            }
            return true;
        }
    }
    
    private static final int COLOR_MENU_ID = Menu.FIRST;
    
    private static final int DEBUG_MENU_ID = Menu.FIRST + 2;
    
    private static final int ERASE_MENU_ID = Menu.FIRST + 3;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        
        menu.add(0, COLOR_MENU_ID, 0, "Color").setShortcut('3', 'c');
        
        menu.add(0, DEBUG_MENU_ID, 0, "Debug / Settings");
        
        menu.add(0, ERASE_MENU_ID, 0, "Erase").setShortcut('5', 'z');


        /****   Is this the mechanism to extend with filter effects?
        Intent intent = new Intent(null, getIntent().getData());
        intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        menu.addIntentOptions(
                              Menu.ALTERNATIVE, 0,
                              new ComponentName(this, NotesList.class),
                              null, intent, 0, null);
        *****/
        return true;
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        mPaint.setXfermode(null);
        mPaint.setAlpha(0xFF);

        switch (item.getItemId()) {
            case COLOR_MENU_ID:
                new ColorPickerDialog(this, this, mPaint.getColor()).show();
                return true;

            case ERASE_MENU_ID:
                mPaint.setXfermode(new PorterDuffXfermode(
                                                        PorterDuff.Mode.CLEAR));
                return true;
                
            case DEBUG_MENU_ID:
            	startActivity(new Intent(this, DebugActivity.class));
            	return true;

        }
        return super.onOptionsItemSelected(item);
    }
    
	@Override
	protected void onPause() {
		scheduler.shutdown();
		try {
			if (scheduler.awaitTermination(500, TimeUnit.MILLISECONDS)){			
		        if (recorder != null) {
		        	recorder.stop();
		        	recorder.release();
		        	recorder = null;
		        }
			}
		} catch (Exception e) {
		}
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		unregisterReceiver(receiver);
		super.onDestroy();
	}
}
