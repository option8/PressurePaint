package com.option8.PressurePaint;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;


public class DebugActivity extends PressurePenActivity implements OnSeekBarChangeListener{

	private int maxWidthPercent;
	private int minWidthPercent;
	private SharedPreferences prefs;
	private SeekBar maxSeekBar;
	private SeekBar minSeekBar;
	private FrameLayout mainLayout;
	public static final String PREFS_NAME = "MyPrefsFile";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		initLayout();
	}

	private void initLayout() {
		mainLayout = (FrameLayout) findViewById(R.id.mainFL);
		mainLayout.addView(new MyView(this));	
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		inflater.inflate(R.layout.layout, mainLayout, true);
		
		maxSeekBar = (SeekBar) findViewById(R.id.maxSeekBar);
		minSeekBar = (SeekBar) findViewById(R.id.minSeekBar);
		maxSeekBar.setOnSeekBarChangeListener(this);
		minSeekBar.setOnSeekBarChangeListener(this);

		getAndSetPrefs();
	}

	private void getAndSetPrefs() {
		prefs = getSharedPreferences(PREFS_NAME, 0);
		maxWidthPercent = prefs.getInt("maxWidthPercent", 50);
		minWidthPercent = prefs.getInt("minWidthPercent", 50);
		
		maxSeekBar.setProgress(maxWidthPercent);
		minSeekBar.setProgress(minWidthPercent);
	}



	@Override
	public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
		if (arg0.getId() == R.id.maxSeekBar && arg2){
			maxWidthPercent = arg1;
		}
		else if (arg0.getId() == R.id.minSeekBar && arg2){
			minWidthPercent = arg1;
		}
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		PressurePenActivity.maxWidthPercent = maxWidthPercent;
		PressurePenActivity.minWidthPercent = minWidthPercent;
        initPressurePen();
	}
	
	@Override
	protected void onPause() {
		Editor editor = prefs.edit();
		editor.putInt("maxWidthPercent", maxWidthPercent);
		editor.putInt("minWidthPercent", minWidthPercent);
		editor.commit();
		super.onPause();
	}

}
