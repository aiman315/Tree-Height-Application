package com.amb12u.treeheight;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Toast;

public class SettingsActivity extends Activity {

	private final String TAG = "SettingsActivity";
	private final int INDEX_HUE = 0;
	private final int INDEX_SATURATION = 1;
	private final int INDEX_VALUE = 2;

	private final int UNITS_CM = 0;
	private final int UNITS_INCHES = 1;

	private int colourHue;
	private int colourSaturation;
	private int colourValue;

	private float [] colourUpperLimit;
	private float [] colourLowerLimit;

	private int selectedMeasurementsUnit;

	private SeekBar seekBarHue;
	private SeekBar seekBarSaturation;
	private SeekBar seekBarValue;

	private EditText editTextHue;
	private EditText editTextSaturation;
	private EditText editTextValue;

	private ImageView imageViewColourPreview;

	private RadioGroup radioGroupMeasurementsUnits;

	/**
	 * Stores the values for colour (Hue, Saturation and Value) as Lower Limit Threshold 
	 * @param v
	 */
	public void onClickSetColourLowerLimit(View v) {
		Log.d(TAG, "onClickSetColourLowerLimit");

		colourHue = seekBarHue.getProgress();
		colourSaturation = seekBarSaturation.getProgress();
		colourValue = seekBarValue.getProgress();

		colourLowerLimit = new float [3];
		colourLowerLimit[INDEX_HUE] = colourHue;
		colourLowerLimit[INDEX_SATURATION] = colourSaturation;
		colourLowerLimit[INDEX_VALUE] = colourValue;

		((Button) findViewById(R.id.buttonColourLowerLimit)).setTextColor(Color.HSVToColor(colourLowerLimit));
		Toast.makeText(getApplicationContext(), "Colour Lower Limit is set", Toast.LENGTH_SHORT).show();
	}

	/**
	 * Stores the values for colour (Hue, Saturation and Value) as Lower Upper Threshold
	 * @param v
	 */
	public void onClickSetColourUpperLimit(View v) {
		Log.d(TAG, "onClickSetColourUpperLimit");

		colourHue = seekBarHue.getProgress();
		colourSaturation = seekBarSaturation.getProgress();
		colourValue = seekBarValue.getProgress();

		colourUpperLimit = new float [3];
		colourUpperLimit[INDEX_HUE] = colourHue;
		colourUpperLimit[INDEX_SATURATION] = colourSaturation;
		colourUpperLimit[INDEX_VALUE] = colourValue;

		((Button) findViewById(R.id.buttonColourUpperLimit)).setTextColor(Color.HSVToColor(colourUpperLimit));
		Toast.makeText(getApplicationContext(), "Colour Upper Limit is set", Toast.LENGTH_SHORT).show();
	}

	/**
	 * Saves programme settings to be used in ImageProcessingActivity
	 * Settings:
	 * <ul>
	 * <ol>Colour Limits (Upper and Lower)</ol>
	 * <ol>Measurement Units (cm or inches)</ol>
	 * @param v
	 */
	public void onClickSaveSettings(View v) {
		Log.d(TAG, "onClickSaveSettings");

		if (colourLowerLimit == null || colourUpperLimit == null) {
			Toast.makeText(getApplicationContext(), "Set both limits please ..", Toast.LENGTH_SHORT).show();
		} else {
			//convert HSV to OpenCV HSV
			colourLowerLimit[INDEX_HUE] = colourLowerLimit[INDEX_HUE]/(float)2; 
			colourLowerLimit[INDEX_SATURATION] = colourLowerLimit[INDEX_SATURATION] * 255 / (float) 100;
			colourLowerLimit[INDEX_VALUE] = colourLowerLimit[INDEX_VALUE] * 255 / (float) 100;

			colourUpperLimit[INDEX_HUE] = colourUpperLimit[INDEX_HUE]/(float)2; 
			colourUpperLimit[INDEX_SATURATION] = colourUpperLimit[INDEX_SATURATION] * 255 / (float) 100;
			colourUpperLimit[INDEX_VALUE] = colourUpperLimit[INDEX_VALUE] * 255 / (float) 100;

			//get selected units
			switch(radioGroupMeasurementsUnits.getCheckedRadioButtonId()) {
			case R.id.radioCM:
				selectedMeasurementsUnit = UNITS_CM;
				break;
			case R.id.radioInches:
				selectedMeasurementsUnit = UNITS_INCHES;
				break;
			default:
				selectedMeasurementsUnit = -1;
				break;
			}

			//send back settings
			Intent intent = new Intent();
			intent.putExtra("colourLowerLimit", colourLowerLimit);
			intent.putExtra("colourUpperLimit", colourUpperLimit);
			intent.putExtra("measurementsUnits", selectedMeasurementsUnit);
			setResult(RESULT_OK,intent);
			finish();

		}
	}


	/**
	 * Handles changes to SeekBars.
	 * Updates EditText corresponding to the SeekBar.
	 * Updates imageViewColourPreview colour to the values of SeekBars
	 * @author Aiman
	 *
	 */
	private class MySeekBarListener implements SeekBar.OnSeekBarChangeListener {
		EditText editText;

		@Override
		public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
			if (editText != null) {
				editText.setText(""+progress);	
			}
			updateColourPreview();
		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
			switch (seekBar.getId()) {
			case R.id.seekBarHue:
				editText = editTextHue;
				break;
			case R.id.seekBarSaturation:
				editText = editTextSaturation;
				break;
			case R.id.seekBarValue:
				editText = editTextValue;
				break;
			default:
				editText = null;
				break;
			}

		}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {

		}

		/**
		 * Changes the colour of colour preview View to using values from progress bars.
		 * Dependent on SeekBars values, NOT EditTexts.
		 * Colour of imageViewColourPreview is RGB, hence HSV values are converted
		 */
		private void updateColourPreview() {
			float hsv [] = new float[3];

			hsv[INDEX_HUE] = seekBarHue.getProgress();
			hsv[INDEX_SATURATION] = seekBarSaturation.getProgress()/(float)100;
			hsv[INDEX_VALUE] = seekBarValue.getProgress()/(float)100;
			imageViewColourPreview.setBackgroundColor(Color.HSVToColor(hsv));
		}

	}

	/**
	 * Handles changes to EditTexts.
	 * Updates SeekBar corresponding to the EditText
	 * Exceeding the maximum value will change the colour of text
	 * @author Aiman
	 *
	 */
	private class MyEditTextWatcher implements TextWatcher {

		int editTextID;
		SeekBar seekBar;
		int seekBarProgress;

		public MyEditTextWatcher (int editTextID) {
			this.editTextID = editTextID;
		}

		@Override
		public void afterTextChanged(Editable text) {
			if (text.length() > 0) {
				seekBarProgress = Integer.parseInt(text.toString());
				if (seekBarProgress <= seekBar.getMax()) {
					((EditText) findViewById(editTextID)).setTextColor(Color.BLACK);
					seekBar.setProgress(seekBarProgress);	
				} else {
					((EditText) findViewById(editTextID)).setTextColor(Color.RED);
					seekBar.setProgress(0);	
				}
			} else {
				seekBar.setProgress(0);
			}
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			switch (editTextID) {
			case R.id.editTextHue:
				seekBar = seekBarHue;
				break;
			case R.id.editTextSaturation:
				seekBar = seekBarSaturation;
				break;
			case R.id.editTextValue:
				seekBar = seekBarValue;
				break;
			default:
				seekBar = null;
				break;
			}
		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {

		}

	}

	//	---------------- Activity Methods ---------------- //

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "onCreate");

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);

		//Initializations
		colourLowerLimit = null;
		colourUpperLimit = null;

		MySeekBarListener seekBarListener = new MySeekBarListener();
		MyEditTextWatcher editTextWatcher;

		seekBarHue = (SeekBar) findViewById(R.id.seekBarHue);
		seekBarSaturation = (SeekBar) findViewById(R.id.seekBarSaturation);
		seekBarValue = (SeekBar) findViewById(R.id.seekBarValue);

		seekBarHue.setMax(360);
		seekBarSaturation.setMax(100);
		seekBarValue.setMax(100);

		seekBarHue.setOnSeekBarChangeListener(seekBarListener);
		seekBarSaturation.setOnSeekBarChangeListener(seekBarListener);
		seekBarValue.setOnSeekBarChangeListener(seekBarListener);

		editTextHue = (EditText) findViewById(R.id.editTextHue);
		editTextWatcher = new MyEditTextWatcher(R.id.editTextHue);
		editTextHue.addTextChangedListener(editTextWatcher);

		editTextSaturation = (EditText) findViewById(R.id.editTextSaturation);
		editTextWatcher = new MyEditTextWatcher(R.id.editTextSaturation);
		editTextSaturation.addTextChangedListener(editTextWatcher);

		editTextValue = (EditText) findViewById(R.id.editTextValue);
		editTextWatcher = new MyEditTextWatcher(R.id.editTextValue);
		editTextValue.addTextChangedListener(editTextWatcher);

		imageViewColourPreview = (ImageView) findViewById(R.id.imageViewColourPreview);
		imageViewColourPreview.setBackgroundColor(Color.BLACK);

		radioGroupMeasurementsUnits = (RadioGroup) findViewById(R.id.radioGroupMeasurementsUnits);
	}

	@Override
	protected void onDestroy() {
		Log.d(TAG, "onDestroy");
		super.onDestroy();
	}


	@Override
	protected void onPause() {
		Log.d(TAG, "onPause");
		super.onPause();
	}


	@Override
	protected void onResume() {
		Log.d(TAG, "onResume");
		super.onResume();
	}


	@Override
	protected void onStart() {
		Log.d(TAG, "onStart");
		super.onStart();
	}


	@Override
	protected void onStop() {
		Log.d(TAG, "onStop");
		super.onStop();
	}
}

