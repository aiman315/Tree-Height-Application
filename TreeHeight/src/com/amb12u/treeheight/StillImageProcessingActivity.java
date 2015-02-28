package com.amb12u.treeheight;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Range;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

public class StillImageProcessingActivity extends Activity {

	private static final int TREETOP_DETECTION = 0;
	private static final int REFERENCE_DETECTION = 1;
	private final String TAG = "StillImageProcessingActivity";
	private final double [] RGB_VAL_BLACK = {0,0,0,0};
	private final double [] RGB_VAL_WHITE = {255,255,255,255};
	private boolean detectedTree, detectedReference;
	private double treeHeight, referenceObjHeight;
	private int treetopRow, treeBottomRow;
	private int referenceObjBottomRow, referenceObjectTopRow;
	private Mat imageMat;
	private int [] offset;

	/**
	 * Calculate tree height by finding the ratio of reference object to  the tree 
	 */
	private void calculateHeight() {
		Log.d(TAG, "onClickCalculateHeight");
		if (detectedTree && detectedReference) {
			treeBottomRow = referenceObjBottomRow;
			double treePixelHeight = treeBottomRow-treetopRow;
			double referenceObjPixelHeight = referenceObjBottomRow-referenceObjectTopRow;

			treeHeight = (treePixelHeight*referenceObjHeight)/referenceObjPixelHeight;

			Toast.makeText(this, String.format("Tree Height = ( %d * %d ) / %d = %d", (int)treePixelHeight, (int)referenceObjHeight, (int)referenceObjPixelHeight, (int)treeHeight), Toast.LENGTH_LONG).show();
		} else {
			Toast.makeText(this, "Error: Run detections first", Toast.LENGTH_LONG).show();
		}
	}
	
	private void detectTreetop() {
		Log.d(TAG, "detectTreetop");
		
		if (!detectedTree && imageMat != null) {
			new TaskDetectTreetop().execute();	
		} else if (detectedTree) {
			ImageView imageView = (ImageView) findViewById(R.id.imageViewCapturedImage);
			imageView.setOnTouchListener(new View.OnTouchListener() {

				@Override
				public boolean onTouch(View v, MotionEvent event) {
					markTouch(v,event, TREETOP_DETECTION);
					return false;
				}
			});
			Toast.makeText(getApplicationContext(), "Touch input enabled", Toast.LENGTH_SHORT).show();
		}
	}

	/**
	 * If the matrix is initialized and the reference object is not yet detected, 
	 * calls the method to detect reference object
	 */
	private void detectReference() {
		Log.d(TAG, "detectReference");
		
		if (!detectedReference && imageMat != null) {
			new TaskDetectReference().execute();
		} else if (detectedReference) {
			ImageView imageView = (ImageView) findViewById(R.id.imageViewCapturedImage);
			imageView.setOnTouchListener(new View.OnTouchListener() {

				@Override
				public boolean onTouch(View v, MotionEvent event) {
					markTouch(v,event, REFERENCE_DETECTION);
					return false;
				}
			});
			Toast.makeText(getApplicationContext(), "Touch input enabled", Toast.LENGTH_SHORT).show();
		}
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
		// White: new Scalar(0, 0, 0), new Scalar(0, 0, 255)
		Imgproc.cvtColor(imageMat, imageMatHSV, Imgproc.COLOR_RGB2HSV, 0);
		Core.inRange(imageMatHSV, new Scalar(0, 0, 0), new Scalar(0, 0, 255), imageMatHSV);
		return imageMatHSV;
	}

	/**
	 * Draw line on a matrix
	 */
	private void drawLine(int row, double [] rgbVal) {
		imageMat.submat(new Range(row, row+1), Range.all()).setTo(new Scalar(255,0,0));		
	}

	private void markTouch(View v, MotionEvent event, int currentDetection) {
		int touchX = (int) event.getX();
		int touchY = (int) event.getY();

		Log.i("XXXXXX", "Location of touch:\ty-"+touchY+"-x-"+touchX);

		int offsetH = offset[0];
		int offsetW = offset[1];

		//		Log.i("XXXXXX", "Offset Location of touch:\ty-"+(touchY-offsetH)+"-x-"+(touchX-offsetW));
		//		imageMat.put(touchY, touchX-offsetW, RGB_VAL_BLACK);

		//		touchY = touchX-offsetH;
		//		touchX = touchX-offsetW;
		switch(currentDetection) {
		case TREETOP_DETECTION:
			new TaskDetectTreetop(touchY, touchX).execute();
			break;
		case REFERENCE_DETECTION:
			new TaskDetectReference(touchY, touchX).execute();
			break;
		}
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

	private int [] calculateImageOffset() {
		int screenWid, screenHei;
		int imgWid, imgHei;
		int diffWid, diffHei;
		int offsetW, offsetH;

		//Calculate offset
		Display display = getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);

		screenWid = size.x;
		screenHei = size.y;

		imgWid = imageMat.cols();
		imgHei = imageMat.rows();

		Log.i("XXXX", "img hei: "+imgHei+"\timg wid: "+imgWid);
		Log.i("XXXX", "screen hei: "+screenHei+"\tscreen wid: "+screenWid);

		imgWid = screenHei * imgWid / imgHei;
		imgHei = screenHei;

		diffWid = screenWid - imgWid;
		diffHei = screenHei - imgHei;

		offsetW = diffWid/2;	
		offsetH = diffHei/2;
		int [] imgOffset = {offsetH, offsetW};

		Imgproc.resize(imageMat, imageMat, new Size(screenWid, screenHei));
		updateImage();

		Log.i("XXXX", "NEW img hei: "+imgHei+"\timg wid: "+imgWid);
		Log.i("XXXX", "screen hei: "+screenHei+"\tscreen wid: "+screenWid);

		return imgOffset;
	}

	private class TaskDetectTreetop extends AsyncTask<Void, Void, Integer> {

		private ProgressDialog progressDialog;
		private int minRow;
		private int maxRow;
		private int minCol;
		private int maxCol;

		public TaskDetectTreetop() {
			minRow = 0;
			maxRow = imageMat.rows()/3;
			minCol = 0;
			maxCol = imageMat.cols();
		}

		public TaskDetectTreetop(int yPos, int xPos) {
			int rowRaduis = 50;
			int colRaduis = 50;
			minRow = yPos-rowRaduis;
			maxRow = yPos+rowRaduis;
			minCol = xPos-colRaduis;
			maxCol = xPos+colRaduis;

			if (minRow < 0) {
				minRow = 0;
			}

			if (minCol < 0) {
				minCol = 0;
			}

			if (maxRow > imageMat.rows()-1) {
				maxRow = imageMat.rows()-1;
			}

			if (maxCol > imageMat.cols()-1) {
				maxCol = imageMat.cols()-1;
			}
		}

		@Override
		protected Integer doInBackground(Void... params) {

			//Detection using Sobel Filter
			Mat temp1 = imageMat.clone();
			Mat temp2 = imageMat.clone();

			Imgproc.Sobel(temp1, temp1, temp1.depth(), 1, 0); //detect in x direction
			Imgproc.Sobel(temp2, temp2, temp2.depth(), 0, 1); //detect in y direction
			Core.addWeighted(temp1, 0.5, temp2, 0.5, 0, temp1);

			Imgproc.threshold(temp1, temp1, 50, 255, Imgproc.THRESH_BINARY);

			for (int r = minRow ; r < maxRow-1 ; r ++) {
				double sum = Core.sumElems(temp1.submat(r,  r+1, minCol, maxCol)).val[0];
				if (sum > 1) {
					return r;
				}
			}


			/*List<MatOfPoint> contours = new Vector<MatOfPoint>();
			Mat hierarchy = new Mat();

			Imgproc.findContours(temp1, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
			 */

			/*if (!contours.isEmpty()) {
				MatOfPoint points = new MatOfPoint(contours.get(0));
				List<org.opencv.core.Point> list = points.toList();
				return (int) list.get(0).y;
			}*/


			/*
			//Detection using Standard Deviation
			double tempStd = 0;

			for (int r = minRow ; r < maxRow-1 ; r ++) {
				MatOfDouble stdMat = new MatOfDouble();
				Core.meanStdDev(imageMat.submat(r,  r+1, minCol, maxCol), new MatOfDouble(), stdMat);
				double stDeviation = stdMat.toArray()[0];
				if (stDeviation-tempStd > 0) {
					return r;
				}
			}

			 */

			return null;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			progressDialog = new ProgressDialog(StillImageProcessingActivity.this);
			progressDialog.setMessage("Detecting Treetop...");
			progressDialog.show(); 
		}

		@Override
		protected void onPostExecute(Integer result) {
			progressDialog.dismiss();
			if (result != null) {
				Toast.makeText(getApplicationContext(), "Treetop Detected (Row: "+result, Toast.LENGTH_SHORT).show();
				detectedTree = true;
				treetopRow = result;
				drawLine(result, RGB_VAL_BLACK);
			} else {
				Toast.makeText(getApplicationContext(), "No Tree Detected", Toast.LENGTH_SHORT).show();
			}
			updateImage();
		}
	}
	
	private class TaskDetectReference extends AsyncTask<Void, Void, Integer []> {

		private ProgressDialog progressDialog;
		private int minRow;
		private int maxRow;
		private int minCol;
		private int maxCol;


		public TaskDetectReference() {
			minRow = 0;
			maxRow = imageMat.rows();
			minCol = 0;
			maxCol = imageMat.cols();
		}

		public TaskDetectReference(int yPos, int xPos) {
			int rowRaduis = 50;
			int colRaduis = 50;
			minRow = yPos-rowRaduis;
			maxRow = yPos+rowRaduis;
			minCol = xPos-colRaduis;
			maxCol = xPos+colRaduis;

			if (minRow < 0) {
				minRow = 0;
			}

			if (minCol < 0) {
				minCol = 0;
			}

			if (maxRow > imageMat.rows()-1) {
				maxRow = imageMat.rows()-1;
			}

			if (maxCol > imageMat.cols()-1) {
				maxCol = imageMat.cols()-1;
			}
		}

		@Override
		protected Integer [] doInBackground(Void... params) {
			Integer boundaries [] = new Integer[2];
			
			//TODO: add options for color detection
			Mat outputMat = detectColor();

			List<MatOfPoint> contours = new ArrayList<MatOfPoint>();  
			Mat hierarchy = new Mat();
			
			/// Find contours
			Imgproc.findContours(outputMat, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

			/// Draw contours
			Mat drawing = Mat.zeros( outputMat.size(), CvType.CV_8UC3 );
			Scalar color = new Scalar(255,255,255);
			for( int i = 0; i< contours.size(); i++ ) {
				Imgproc.drawContours(imageMat, contours, i, color, 10);
			}


	/*

			for (int r = 50 ; r < outputMat.rows()-50 ; r++) {
				if (referenceObjectTopRow == 0 && Core.sumElems(outputMat.row(r)).val[0] > 5000) { 
					referenceObjectTopRow = r;
				}
				if (Core.sumElems(outputMat.row(r)).val[0] > 5000) {
					referenceObjBottomRow = r;
				}
			}*/

			//TODO: Further processing

			//drawLine(referenceObjBottomRow, RGB_VAL_WHITE);
			//drawLine(referenceObjectTopRow,RGB_VAL_WHITE);
			
			if (false) {
				
				return boundaries;
			}
			
			return null;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			progressDialog = new ProgressDialog(StillImageProcessingActivity.this);
			progressDialog.setMessage("Detecting Reference Object...");
			progressDialog.show(); 
		}

		@Override
		protected void onProgressUpdate(Void... values) {
			super.onProgressUpdate(values);
		}

		@Override
		protected void onPostExecute(Integer [] result) {
			progressDialog.dismiss();
			if (result != null) {
				Toast.makeText(getApplicationContext(), "Reference Object Detected (Row: "+result, Toast.LENGTH_SHORT).show();
				detectedReference = true;
				referenceObjectTopRow = result[0];
				referenceObjBottomRow = result[1];
				drawLine(result[0], RGB_VAL_BLACK);
				drawLine(result[1], RGB_VAL_BLACK);
			} else {
				Toast.makeText(getApplicationContext(), "No Reference Object Detected", Toast.LENGTH_SHORT).show();
			}
			updateImage();
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
		input.setHint(R.string.ref_height_dialog_text);
		input.setRawInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

		//reference object height input-dialog setup
		final AlertDialog heightDialog = new AlertDialog.Builder(this)
		.setView(input)
		.setTitle(R.string.ref_height_dialog_title)
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

	private void updateImage() {
		ImageView image = (ImageView) findViewById(R.id.imageViewCapturedImage);
		image.setImageBitmap(mat2bitmap(imageMat));
	}








	//	---------------- Activity Methods ---------------- //

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "onCreate");
		super.onCreate(savedInstanceState);
		//setTheme(android.R.style.Theme_NoTitleBar_Fullscreen);
		setContentView(R.layout.activity_still_image_processing);

		//initializations
		detectedReference = false;
		detectedTree = false;
		treeHeight = 0;
		treetopRow = 0;
		treeBottomRow = 0;
		referenceObjectTopRow = 0;
		referenceObjBottomRow = 0;

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

			//create a matrix of the selected image for processing
			imageMat = bitmap2mat(loadedImage);

			//setup image offset
			offset = calculateImageOffset();

			//setup for reference object
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
			item.setTitle("Incorrect Treetop Detection?");
			return true;
		case R.id.actionDetectTree:
			detectTreetop();
			item.setTitle("Incorrect Treetop Detection?");
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
