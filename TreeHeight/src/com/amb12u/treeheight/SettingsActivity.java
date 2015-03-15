package com.amb12u.treeheight;
import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;

public class SettingsActivity extends Activity {

	private final String TAG = "SettingsActivity";
	private int colourLowerHue;
	private int colourLowerSaturation;
	private int colourLowerValue;
	private int colourUpperHue;
	private int colourUpperSaturation;
	private int colourUpperValue;

	private SeekBar seekBarHue;
	private SeekBar seekBarSaturation;
	private SeekBar seekBarValue;

	private EditText editTextHue;
	private EditText editTextSaturation;
	private EditText editTextValue;


	public void onClickSetColourLowerLimit(View v) {
		colourLowerHue = Integer.parseInt(editTextHue.getText().toString());
		colourLowerSaturation = Integer.parseInt(editTextSaturation.getText().toString());
		colourLowerValue = Integer.parseInt(editTextValue.getText().toString());
	}

	public void onClickSetColourUpperLimit(View v) {
		colourUpperHue = Integer.parseInt(editTextHue.getText().toString());
		colourUpperSaturation = Integer.parseInt(editTextSaturation.getText().toString());
		colourUpperValue = Integer.parseInt(editTextValue.getText().toString());
	}

	public void onClickSaveSettings(View v) {

	}


	private class MySeekBarListener implements SeekBar.OnSeekBarChangeListener {
		EditText editText;
		int seekBarProgress;

		@Override
		public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
			seekBarProgress = progress;
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

			if (editText != null) {
				editText.setText(""+seekBarProgress);	
			}
		}

	}

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
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);

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

