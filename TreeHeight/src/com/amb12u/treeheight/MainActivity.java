package com.amb12u.treeheight;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;


public class MainActivity extends Activity {

	private final String TAG = "MainActivity";
	private final int REQUEST_CODE_GALLERY = 999;
	private final int REQUEST_CODE_CAMERA = 1000;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return true;
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

	/**
	 * Handle click on math button, and launches math activity
	 * @param v
	 */
	public void onClickMath(View v) {
		Log.d(TAG, "onClickMath");
		Intent cameraIntent = new Intent(this, MathActivity.class);
		startActivity(cameraIntent);
	}

	/**
	 * Handle click on Image Processing button, displays a dialog to allow user to select photo source (Gallery | Camera) and launches Image Processing activity
	 * @param v
	 */
	public void onClickImageProcessing(View v) {
		Log.d(TAG, "onClickImageProcessing");

		final Dialog dialogPhotoSource = new Dialog(this, R.style.myInstructionDialog);
		dialogPhotoSource.setContentView(R.layout.dialog_custom_ip_photo_source);
		dialogPhotoSource.setTitle("Where is the tree?");

		Button buttonGallery = (Button) dialogPhotoSource.findViewById(R.id.buttonGallery);
		Button buttonCamera = (Button) dialogPhotoSource.findViewById(R.id.buttonCamera);

		buttonGallery.setOnClickListener( new OnClickListener() {	
			@Override
			public void onClick(View v) {
				Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT, null);
				galleryIntent.setType("image/*");
				galleryIntent.putExtra("return-data", true);
				startActivityForResult(galleryIntent,REQUEST_CODE_GALLERY);
				dialogPhotoSource.dismiss();
			}
		});

		buttonCamera.setOnClickListener( new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
				startActivityForResult(cameraIntent,REQUEST_CODE_CAMERA);
				dialogPhotoSource.dismiss();
			}
		});

		dialogPhotoSource.show();
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(TAG, "onActivityResult");

		if (resultCode == RESULT_CANCELED) {
			Toast.makeText(this, "Canceled", Toast.LENGTH_SHORT).show();

			return;
		} else {
			Uri selectedImageUri = data.getData();
			// Pass image uri and start the activity 
			Intent intent = new Intent(this, ImageProcessingActivity.class);
			intent.putExtra("ImgUri", selectedImageUri);
			startActivity(intent);	
		}
	}
}
