package com.amb12u.treeheight;

import java.io.FileNotFoundException;
import java.io.InputStream;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

public class StillImageProcessingActivity extends Activity {

	private final String TAG = "StillImageProcessingActivity";
	private boolean detectedTree, detectedReference;
	private double referenceObjHeight;
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
			detectedTree = true;
			//TODO: change to AsyncTask
			detectReferenceAlgorithm();
		}
	}

	/**
	 * If the matrix is initialized and a the tree is not yet detected, 
	 * calls the method to detect trees
	 */
	private void detectTree() {
		Log.d(TAG, "onClickDetectTree");
		if (imageMat != null && !detectedTree) {
			//TODO: Activate
			detectedTree = true;
			new TaskDetectTreetop().execute();
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
		// Red: new Scalar(0, 100, 100), new Scalar(10, 255, 255)
		Imgproc.cvtColor(imageMat, imageMatHSV, Imgproc.COLOR_RGB2HSV, 0);
		Core.inRange(imageMatHSV, new Scalar(0, 100, 100), new Scalar(10, 255, 255), imageMatHSV);
		return imageMatHSV;
	}

	/**
	 * Runs the algorithm to detect reference object
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
	private void drawLine(int row) {
		//Mat outputMat = new Mat(imageMat.rows(), imageMat.cols(), CvType.CV_8UC4);
		Mat outputMat = imageMat.clone();
		Imgproc.cvtColor(outputMat, outputMat, Imgproc.COLOR_BGR2GRAY);

		for (int c = 0 ; c < outputMat.cols(); c++) {
			outputMat.put(row-2, c, 0);
			outputMat.put(row-1, c, 0);
			outputMat.put(row, c, 0);
			outputMat.put(row+1, c, 0);
			outputMat.put(row+2, c, 0);
		}
		Bitmap image = mat2bitmap(outputMat);

		ImageView imageView = (ImageView) findViewById(R.id.imageViewCapturedImage);
		imageView.setImageBitmap(image);


	}


	/**
	 * Detects high intensity variation for every row
	 * @return the row number at which the treetop is detected
	 */
	private int detectTopPosition() {
		//TODO: method documentation
		//TODO: what are the limits (minStd, patch size, column increment)
		
		Mat referenceMat = imageMat;
		int minStd = 20;
		Mat patch;

		int windowHeight = 5;
		int windowWidth = 5;

		for (int r = 0 ; r < referenceMat.rows()/4 ; r ++) {
			for (int c = 0 ; c < referenceMat.cols()-windowWidth ; c += windowWidth) {
				patch = referenceMat.submat(r, r+windowHeight, c, c+windowWidth);
				MatOfDouble stdMat = new MatOfDouble();
				Core.meanStdDev(patch, new MatOfDouble(), stdMat);
				int stDeviation = (int) stdMat.toArray()[0];
				if (minStd < stDeviation) {
					return r;
				}
			}
		}
		//TODO: return a false value? -999 <--- add to private final int
		return 0;
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

	private class TaskDetectTreetop extends AsyncTask<Void, Void, Integer> {

		private ProgressDialog pdia;
		
		@Override
		protected Integer doInBackground(Void... params) {
			
			Mat referenceMat = imageMat;
			int minStd = 20;
			Mat patch;

			int windowHeight = 5;
			int windowWidth = 5;
			
			for (int r = 0 ; r < referenceMat.rows()/4 ; r ++) {
				for (int c = 0 ; c < referenceMat.cols()-windowWidth ; c += windowWidth) {
					patch = referenceMat.submat(r, r+windowHeight, c, c+windowWidth);
					MatOfDouble stdMat = new MatOfDouble();
					Core.meanStdDev(patch, new MatOfDouble(), stdMat);
					int stDeviation = (int) stdMat.toArray()[0];
					if (minStd < stDeviation) {
						return r;
					}
				}
			}
			return null;
		}
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
	        pdia = new ProgressDialog(StillImageProcessingActivity.this);
	        pdia.setMessage("Detecting Treetop...");
	        pdia.show(); 
		}
		
		@Override
		protected void onProgressUpdate(Void... values) {
			super.onProgressUpdate(values);
		}

		@Override
		protected void onPostExecute(Integer treetopRow) {
			pdia.dismiss();
			drawLine(treetopRow);
		}
		

	}
	
	
	/**
	 * Creates a dialog to input reference object height
	 * The value of reference object height is positive double, and can't be zero
	 * The unit for reference object height is cm
	 */
	private void setupReferenceObjHeight() {
		// EditText to allow user input
		final EditText input = new EditText(this);
		input.setHint(R.string.height_dialog_text);
		input.setRawInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

		//camera height input-dialog setup
		final AlertDialog heightDialog = new AlertDialog.Builder(this)
		.setView(input)
		.setTitle(R.string.height_dialog_title)
		.setPositiveButton(android.R.string.ok, null)
		.create();

		heightDialog.setOnShowListener(new DialogInterface.OnShowListener() {

			@Override
			public void onShow(DialogInterface dialog) {

				Button buttonOk = heightDialog.getButton(AlertDialog.BUTTON_POSITIVE);
				buttonOk.setOnClickListener(new View.OnClickListener() {

					@Override
					public void onClick(View view) {
						//verify correct input
						try {
							referenceObjHeight = Double.parseDouble(input.getText().toString());
							
							if (referenceObjHeight > 0) {
								heightDialog.dismiss();
							} else {
								Toast.makeText(StillImageProcessingActivity.this, "Input Must be greater than zero", Toast.LENGTH_SHORT).show();
							}
						} catch (NumberFormatException e) {
							Toast.makeText(StillImageProcessingActivity.this, "Invalid Input", Toast.LENGTH_SHORT).show();
							Log.e(TAG, "exception", e);
						}
					}
				});
			}
		});
		heightDialog.setCanceledOnTouchOutside(false);
		heightDialog.show();
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

		Uri imgUri = getIntent().getExtras().getParcelable("ImgUri");
		Bitmap loadedImage;

		try {
			//retrieve image from storage
			InputStream image_stream = getContentResolver().openInputStream(imgUri);
			loadedImage = BitmapFactory.decodeStream(image_stream );

			//rotate image
			Matrix matrix = new Matrix();
			matrix.postRotate(90);
			loadedImage = Bitmap.createBitmap(loadedImage , 0, 0, loadedImage .getWidth(), loadedImage .getHeight(), matrix, true);

			//load image in image view 
			ImageView imageView = (ImageView) findViewById(R.id.imageViewCapturedImage);
			imageView.setImageBitmap(loadedImage);

			//TODO: delete?
			imageView.setOnTouchListener(new View.OnTouchListener() {

				@Override
				public boolean onTouch(View v, MotionEvent event) {
					markTouch(v,event);
					return false;
				}
			});

			//create a matrix of the selected image for processing
			imageMat = bitmap2mat(loadedImage);

			setupReferenceObjHeight();
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
