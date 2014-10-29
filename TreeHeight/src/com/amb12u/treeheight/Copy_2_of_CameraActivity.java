package com.amb12u.treeheight;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

public class Copy_2_of_CameraActivity extends Activity {

	private final String TAG = "CameraActivity";
	private final int PHOTO_INTENT = 1000;
	private Uri photoFileUri;

	
	/**
	 * Starts Camera application to capture an image with timeStamp name
	 * @param v: the view invoked the method
	 */
	public void takePicture(View v) {
		Log.d(TAG, "takePicture");
		photoFileUri = generateTimeStampPhotoFileUri();
		if (photoFileUri != null) {
			Intent intent = new Intent (MediaStore.ACTION_IMAGE_CAPTURE);
			intent.putExtra(MediaStore.EXTRA_OUTPUT, photoFileUri);
			startActivityForResult(intent, PHOTO_INTENT);
		}
	}

	protected void onActivityResult (int requestCode, int resultCode, Intent resultIntent) {
		Log.d(TAG, "onActivityResult");
		Log.d(TAG, String.format("requestCode: %d | resultCode: %d", requestCode, resultCode));
		
		if (resultCode == RESULT_CANCELED) {
			Toast.makeText(this, "User Canceled", Toast.LENGTH_LONG).show();
			return;
		}

		switch (requestCode) {
		case PHOTO_INTENT:
			//obtain image from application images directory and display it in activity
			Bitmap imageBitmap = BitmapFactory.decodeFile(photoFileUri.getPath());
			if (imageBitmap != null) {
			//	ImageView imageView = (ImageView) findViewById(R.id.imageView);
				//imageView.setImageBitmap(imageBitmap);
			}
			break;
		}
	}

	
	/** 
	 * Get the photo directory for application, and creates it if necessary
	 * @return File outputDir
	 */
	private File getPhotoDirectory() {
		Log.d(TAG, "getPhotoDirectory");
		
		File outputDir = null;
		String externalStorageState = Environment.getExternalStorageState();
		if (externalStorageState.equals(Environment.MEDIA_MOUNTED)) {
			File pictureDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
			outputDir = new File (pictureDir, "TreeHeightApp");
			if (!outputDir.exists()) {
				if (!outputDir.mkdirs()) {
					Toast.makeText(this,  "failed to create directory: " + outputDir.getAbsolutePath(), Toast.LENGTH_LONG).show();
					outputDir = null;
				}
			}
		}
		return outputDir;
	}
	
	/**
	 * Creates a time stamp for captured image
	 * @return
	 */
	private Uri generateTimeStampPhotoFileUri() {
		Log.d(TAG, "generateTimeStampPhotoFileUri");
		
		Uri photoFileUri = null;
		File outputDir = getPhotoDirectory();
		
		if (outputDir != null) {
			String timeStamp = 	new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss", Locale.UK).format(new Date()); 
			Toast.makeText(this, timeStamp, Toast.LENGTH_LONG).show();
			String photoFileName = "IMG_" + timeStamp + ".jpg";
			
			File photoFile = new File (outputDir, photoFileName);
			photoFileUri = Uri.fromFile(photoFile);	
		}
		
		return photoFileUri;
	}
















	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_camera);
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
