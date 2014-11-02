package com.amb12u.treeheight;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

public class DescActivity extends Activity {

	private final String TAG = "DescActivity";
	private TextView desc;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_desc);

		desc = (TextView) findViewById(R.id.textViewDesc);

		Intent intent = getIntent();
		Bundle bundle = intent.getExtras();

		if(bundle!=null)
		{
			int buttonId = (Integer) bundle.get("button_id");
			switch(buttonId) {
			case R.id.buttonMath: 
				setTitle(R.string.button_math);
				desc.setText(R.string.math_approach_desc); 
				break;
			case R.id.buttonRand: 
				setTitle(R.string.button_image_processing);
				desc.setText(R.string.img_processing_approach_desc); 
				break;
			default: 
				setTitle(R.string.button_something_else);
				desc.setText("Error"); 
				break;
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.math, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
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

	public void onClickTry(View v) {
		Log.d(TAG, "onClickTry");
		Intent cameraIntent = new Intent(this, CameraActivity.class);
		startActivity(cameraIntent);
	}
}
