package com.amb12u.treeheight;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class DescActivity extends Activity {

	private final String TAG = "DescActivity";

	private final int REQUEST_CODE_GALLERY = 999;
	private final int REQUEST_CODE_CAMERA = 1000;
	private final int INVALID_METHOD_SELECTION = -999;
	private final int MATH_METHOD_SELECTION = 1;
	private final int IMAGE_PROCESSING_METHOD_SELECTION = 2;

	private TextView desc;
	private int selectedMethod;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_desc);

		desc = (TextView) findViewById(R.id.textViewDesc);
		selectedMethod = INVALID_METHOD_SELECTION;

		Intent intent = getIntent();
		Bundle bundle = intent.getExtras();

		if(bundle!=null)
		{
			int buttonId = (Integer) bundle.get("button_id");
			switch(buttonId) {
			case R.id.buttonMath: 
				setTitle(R.string.button_math);
				desc.setText(R.string.math_approach_desc); 
				selectedMethod = MATH_METHOD_SELECTION;
				break;
			case R.id.buttonIP: 
				setTitle(R.string.button_image_processing);
				desc.setText(R.string.img_processing_approach_desc);
				selectedMethod = IMAGE_PROCESSING_METHOD_SELECTION;
				break;
			default:
				//TODO: what to do here?
				setTitle(R.string.button_something_else);
				desc.setText("Error");
				selectedMethod = INVALID_METHOD_SELECTION;
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
		switch(selectedMethod) {
		case MATH_METHOD_SELECTION: {
			Intent cameraIntent = new Intent(this, CameraActivity.class);
			startActivity(cameraIntent);
			break;
		}
		case IMAGE_PROCESSING_METHOD_SELECTION: {

			AlertDialog.Builder photoSourceDialog = new AlertDialog.Builder(this);
			photoSourceDialog.setMessage("Select Photo source");
			photoSourceDialog.setPositiveButton("Gallery", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface arg0, int arg1) {
					Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT, null);
					galleryIntent.setType("image/*");
					galleryIntent.putExtra("return-data", true);
					startActivityForResult(galleryIntent,REQUEST_CODE_GALLERY);
				}
			});

			photoSourceDialog.setNegativeButton("Camera", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface arg0, int arg1) {
					Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
					startActivityForResult(cameraIntent,REQUEST_CODE_CAMERA);
				}
			});
			photoSourceDialog.show();
		}	
		default:
			Toast.makeText(this, "Invalid method selection", Toast.LENGTH_SHORT).show();
			break;
		}
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(TAG, "onActivityResult");
		Log.d(TAG, String.format("requestCode: %d | resultCode: %d", requestCode, resultCode));

		switch (requestCode) {
		case REQUEST_CODE_CAMERA:
		case REQUEST_CODE_GALLERY:

			if (resultCode == RESULT_CANCELED) {
				Toast.makeText(this, "User Canceled", Toast.LENGTH_LONG).show();
				return;
			} else {

				Uri selectedImageUri = data.getData();
				// Pass image uri and start the activity 
				Intent intent = new Intent(this, ImageProcessingActivity.class);
				intent.putExtra("ImgUri", selectedImageUri);
				startActivity(intent);	
			}
			break;
		}

	}
}
