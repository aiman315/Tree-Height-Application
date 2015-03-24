package com.amb12u.treeheight;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
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
		getMenuInflater().inflate(R.menu.main, menu);
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

	public void onClickMath(View v) {
		Log.d(TAG, "onClickMath");
		Intent cameraIntent = new Intent(this, CameraActivity.class);
		startActivity(cameraIntent);
	}

	public void onClickImageProcessing(View v) {
		Log.d(TAG, "onClickImageProcessing");

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
