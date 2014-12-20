package com.amb12u.treeheight;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

public class StillImageProcessingActivity extends Activity {
	
	private final String TAG = "StillImageProcessingActivity";
	private Mat imageMat;
	
	public void onClickCalculateHeight(View v) {
		Log.d(TAG, "onClickCalculateHeight");
		//TODO:
	}
	
	public void onClickDetectReference(View v) {
		Log.d(TAG, "onClickDetectReference");
		//TODO:
	}
	
	public void onClickDetectTree(View v) {
		Log.d(TAG, "onClickDetectTree");
		//TODO: Currently detecting edges
		Mat outputMat = new Mat(imageMat.rows(), imageMat.cols(), CvType.CV_8UC4);
		
		Imgproc.Canny(imageMat, outputMat, 300, 600, 5, true);
		Bitmap image = Bitmap.createBitmap(outputMat.cols(), outputMat.rows(), Bitmap.Config.ARGB_8888);
		Utils.matToBitmap(outputMat, image);
		
		ImageView imageView = (ImageView) findViewById(R.id.imageViewCapturedImage);
		imageView.setImageBitmap(image);
	}
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_still_image_processing);
		
		byte[] imageArray = getIntent().getByteArrayExtra("CapturedImage");
        if (imageArray != null) { 
        	Bitmap capturedImage = BitmapFactory.decodeByteArray(imageArray, 0, imageArray.length);
        	
        	ImageView imageView = (ImageView) findViewById(R.id.imageViewCapturedImage);
        	imageView.setImageBitmap(capturedImage);
        
        	imageMat = new Mat(capturedImage.getHeight(), capturedImage.getWidth(), CvType.CV_8UC4);
			Utils.bitmapToMat(capturedImage, imageMat);
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
}
