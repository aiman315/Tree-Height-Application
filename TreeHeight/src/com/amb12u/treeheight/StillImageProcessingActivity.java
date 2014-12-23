package com.amb12u.treeheight;

import java.io.FileNotFoundException;
import java.io.InputStream;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

public class StillImageProcessingActivity extends Activity {

	private final String TAG = "StillImageProcessingActivity";
	private boolean detectedTree, detectedReference;
	private Mat imageMat;

	private void calculateHeight() {
		Log.d(TAG, "onClickCalculateHeight");
		//TODO:
	}

	/**
	 * If the matrix is initialized and the reference object is not yet detected, 
	 * calls the method to detect reference object
	 */
	private void detectReference() {
		Log.d(TAG, "onClickDetectReference");
		if (imageMat != null && !detectedReference) {
			detectReferenceAlgorithm();
		}
	}

	/**
	 * If the matrix is initialized and a the tree is not yet detected, 
	 * calls the method to detect trees
	 */
	public void detectTree() {
		Log.d(TAG, "onClickDetectTree");
		if (imageMat != null && !detectedTree) {
			//TODO: Activate
			//detectTreeAlgorithm();
			//drawLine();
		}
	}

	/**
	 * Runs the algorithm to detect trees
	 * TODO: Explain how the algorithm works
	 */
	private void detectTreeAlgorithm() {
		//TODO: implementation
	}

	/**
	 * Detect a selected color in the displayed image
	 * @param selectedColorHSV: HSV color value
	 * <br /><b>HSV in OpenCV</b><br />
	 * Hue Range: [0-180]<br />
	 * Saturation Range: [0-255]<br />
	 * Value Range: [0-255]<br />
	 * Normally, the ranges for Hue, Saturation and Value are: [0-360], [0-100] and [0-100] respectively<br />
	 * To convert to OpenCV ranges:<br />
	 * <table>
  <tr>
    <th ></th><th "></th><th >Normal Value</th><th "></th><th >OpenCV Value</th>
  </tr>
  <tr>
    <td >Hue</td>
    <td "></td><td >X</td><td "></td><td >X / 2</td>
  </tr>
  <tr>
    <td >Saturation</td>
    <td "></td><td >X</td><td "></td><td >X * 255 / 100</td>
  </tr>
  <tr>
    <td >Value</td><td "></td><td >X</td><td "></td><td >X * 255 / 100</td>
  </tr>
</table>
	 */
	private Mat detectColor() {
		Mat imageMatHSV = new Mat();
		// Yellow: new Scalar(25, 20, 20), new Scalar(32, 255, 255)
		// Red: new Scalar(0, 190, 190), new Scalar(10, 255, 255)
		Imgproc.cvtColor(imageMat, imageMatHSV, Imgproc.COLOR_RGB2HSV, 0);
		Core.inRange(imageMatHSV, new Scalar(0, 190, 190), new Scalar(10, 255, 255), imageMatHSV);
		return imageMatHSV;
	}

	/**
	 * Runs the algorithm to detect trees
	 * TODO: Explain how the algorithm works
	 */
	private void detectReferenceAlgorithm() {
		Mat outputMat = detectColor();
		//TODO: Further processing


		Bitmap image = mat2bitmap(outputMat);
		ImageView imageView = (ImageView) findViewById(R.id.imageViewCapturedImage);
		imageView.setImageBitmap(image);
	}


	/**
	 * Draw line on a matrix
	 */
	private void drawLine() {
		//Mat outputMat = new Mat(imageMat.rows(), imageMat.cols(), CvType.CV_8UC4);
		Mat outputMat = imageMat.clone();
		Imgproc.cvtColor(outputMat, outputMat, Imgproc.COLOR_BGR2GRAY);

		for (int r = 0 ; r < outputMat.rows(); r++) {
			outputMat.put(r, 13, 255);
			outputMat.put(r, 14, 255);
			outputMat.put(r, 15, 255);
			outputMat.put(r, 16, 255);
		}
		Bitmap image = mat2bitmap(outputMat);

		ImageView imageView = (ImageView) findViewById(R.id.imageViewCapturedImage);
		imageView.setImageBitmap(image);


	}

	private void markTouch(View v, MotionEvent event) {

		ImageView imageView = (ImageView)v;
		int[] viewCoords = new int[2];
		imageView.getLocationOnScreen(viewCoords);

		int touchX = (int) event.getX();
		int touchY = (int) event.getY();

		int imageX = touchX - viewCoords[0]; // viewCoords[0] is the X coordinate
		int imageY = touchY;// - viewCoords[1]; // viewCoords[1] is the y coordinate

		Bitmap image = ((BitmapDrawable)imageView.getDrawable()).getBitmap();
		Mat imageMat = bitmap2mat(image);
		Imgproc.cvtColor(imageMat, imageMat, Imgproc.COLOR_BGR2GRAY);


		imageMat.put(imageY, imageX, 255);


		image = mat2bitmap(imageMat);
		imageView.setImageBitmap(image);
	}


	/**
	 * Converts matrix to bitmap
	 * @param mat
	 * @return bitmap
	 */
	private Bitmap mat2bitmap(Mat mat) {
		Bitmap image = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
		Utils.matToBitmap(mat, image);
		return image;
	}

	/**
	 * Converts bitmap to matrix
	 * @param bitmap
	 * @return matrix
	 */
	private Mat bitmap2mat(Bitmap bitmap) {
		Mat mat = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC1);
		Utils.bitmapToMat(bitmap, mat);
		return mat;
	}


	//	---------------- Activity Methods ---------------- //

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "onCreate");
		super.onCreate(savedInstanceState);
		setTheme(android.R.style.Theme_NoTitleBar_Fullscreen);
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
			//rotate image
			Matrix matrix = new Matrix();
			matrix.postRotate(90);
			loadedImage = Bitmap.createBitmap(loadedImage , 0, 0, loadedImage .getWidth(), loadedImage .getHeight(), matrix, true);

			//load image in image view 
			ImageView imageView = (ImageView) findViewById(R.id.imageViewCapturedImage);
			imageView.setImageBitmap(loadedImage);

			imageView.setOnTouchListener(new View.OnTouchListener() {

				@Override
				public boolean onTouch(View v, MotionEvent event) {
					markTouch(v,event);
					return false;
				}
			});

			//create a matrix of the selected image for processing
			imageMat = bitmap2mat(loadedImage);

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
		getMenuInflater().inflate(R.menu.still_image_processing, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		switch(id){
		case R.id.actionDetectReference:
			detectReference();
			return true;
		case R.id.actionDetectTree:
			detectTree();
			return true;
		case R.id.actionTreeHeight:
			calculateHeight();
			return true;
		case R.id.action_settings:
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
