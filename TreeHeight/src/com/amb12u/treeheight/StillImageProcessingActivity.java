package com.amb12u.treeheight;

import java.io.FileNotFoundException;
import java.io.InputStream;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

public class StillImageProcessingActivity extends Activity {

	private final String TAG = "StillImageProcessingActivity";
	private boolean detectedTree, detectedReference;
	private Mat imageMat;

	public void onClickCalculateHeight(View v) {
		Log.d(TAG, "onClickCalculateHeight");
		//TODO:
	}

	/**
	 * If the matrix is initialized and the reference object is not yet detected, 
	 * calls the method to detect reference object
	 * @param v: The view that invoked the method
	 */
	public void onClickDetectReference(View v) {
		Log.d(TAG, "onClickDetectReference");
		if (imageMat != null && !detectedReference) {
			detectReference();
			
			//disable button once pressed
			Button button = (Button)v;
			button.setEnabled(false);
		}
	}

	/**
	 * If the matrix is initialized and a the tree is not yet detected, 
	 * calls the method to detect trees
	 * @param v: The view that invoked the method
	 */
	public void onClickDetectTree(View v) {
		Log.d(TAG, "onClickDetectTree");
		//TODO: Currently detecting edges
		if (imageMat != null && !detectedTree) {
			detectTrees();
			
			//disable button once pressed			
			Button button = (Button)v;
			button.setEnabled(false);
		}
	}

	/**
	 * Runs the algorithm to detect trees
	 * TODO: Explain how the algorithm works
	 */
	private void detectTrees() {
		Mat outputMat = new Mat(imageMat.rows(), imageMat.cols(), CvType.CV_8UC4);

		//FIXME: currently detects edges
		Imgproc.Canny(imageMat, outputMat, 300, 600, 5, true);
		Bitmap image = Bitmap.createBitmap(outputMat.cols(), outputMat.rows(), Bitmap.Config.ARGB_8888);
		Utils.matToBitmap(outputMat, image);

		ImageView imageView = (ImageView) findViewById(R.id.imageViewCapturedImage);
		imageView.setImageBitmap(image);
	}
	
	/**
	 * Runs the algorithm to detect trees
	 * TODO: Explain how the algorithm works
	 */
	private void detectReference() {
		Mat outputMat = new Mat(imageMat.rows(), imageMat.cols(), CvType.CV_8UC4);
	}
	
	
	//	---------------- Activity Methods ---------------- //

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_still_image_processing);
		
		//initializations
		detectedReference = false;
		detectedTree = false;

		byte[] imageArray = getIntent().getByteArrayExtra("CapturedImage");
		Uri imgUri = getIntent().getExtras().getParcelable("ImgUri");
		Bitmap loadedImage;

		try {
			//Case1: Activity is launched via frame capture 
			if (imageArray != null) { 		
				loadedImage = BitmapFactory.decodeByteArray(imageArray, 0, imageArray.length);

			//Case2: Activity is launched via gallery image selection
			} else {
				InputStream image_stream = getContentResolver().openInputStream(imgUri);
				loadedImage = BitmapFactory.decodeStream(image_stream );
			}
			//load image in image view 
			ImageView imageView = (ImageView) findViewById(R.id.imageViewCapturedImage);
			imageView.setImageBitmap(loadedImage);

			//create a matrix of the selected image for processing
			imageMat = new Mat(loadedImage.getHeight(), loadedImage.getWidth(), CvType.CV_8UC4);
			Utils.bitmapToMat(loadedImage, imageMat);
			
		// Handle exceptions
		} catch (FileNotFoundException e) {
			Toast.makeText(this, "Error locating the file path", Toast.LENGTH_SHORT).show();
			Log.e(TAG, e.getMessage());
			finish();
		} catch (Exception e) {
			Toast.makeText(this, "Error!!", Toast.LENGTH_SHORT).show();
			Log.e(TAG, e.getMessage());
			finish();
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
