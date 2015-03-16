package com.amb12u.treeheight;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Range;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.MotionEventCompat;
import android.text.InputType;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class ImageProcessingActivity extends Activity {
	private final String TAG = "StillImageProcessingActivity";

	private final int REQUEST_CODE_SETTING_ACTIVITY = 0;

	private final int STATE_TREETOP = 0;
	private final int STATE_REFERENCE = 1;
	private final int STATE_HEIGHT = 2;

	protected final int INDEX_RATIO_Y = 0;
	protected final int INDEX_RATIO_X = 1;

	private final int INDEX_REF_BOTTOM = 0;
	private final int INDEX_REF_RIGHT = 1;
	private final int INDEX_REF_TOP = 2;
	private final int INDEX_REF_LEFT = 3;

	private final int INDEX_HUE = 0;
	private final int INDEX_SATURATION = 1;
	private final int INDEX_VALUE = 2;

	private final int UNITS_CM = 0;
	private final int UNITS_INCHES = 1;

	private final int LINE_THICKNESS = 5;

	private double heightRatio, widthRatio;
	private double treeHeight, referenceObjHeight;
	private int treePixelHeight, referenceObjPixelHeight;
	private int treetopRow, treeBottomRow;
	private int [] referenceObjBound;
	private float [] colourUpperLimit;
	private float [] colourLowerLimit;
	private int selectedMeasurementsUnit;
	private int currentState;
	private Uri imgUri;
	private ImageView imageView;
	private Button buttonTask;
	private TextView textTreeHeight;
	private Mat displayMat;
	private Mat originalMat;

	private AsyncTask<Void, Void, Integer> taskDetectTreetop;
	private AsyncTask<Void, Void, Integer[]> taskDetectReference;
	private AsyncTask<Void, Void, Void> taskAnimateReference;

	private BaseLoaderCallback mLoaderCallback;

	//TODO:
	//color changing option
	//units changing option
	//Indicate that tree bottom = reference object bottom
	//Indicate treetop must be in top third
	//Code documentation
	//Consider removing Toasts
	//Add option to convert units
	//markTouch at bottom of image has large offset
	//Take photos with A4 paper
	//Make mathematical approach more visual
	//Test for landscape images


	/**
	 * Calculate tree height by finding the ratio of reference object to the tree
	 */
	private void calculateHeight() {
		Log.d(TAG, "calculateHeight");

		//Required computations for tree height calculation
		treeBottomRow = referenceObjBound[INDEX_REF_BOTTOM];
		treePixelHeight = treeBottomRow-treetopRow;
		referenceObjPixelHeight = referenceObjBound[INDEX_REF_BOTTOM]-referenceObjBound[INDEX_REF_TOP];

		//Tree height calculation formula
		treeHeight = (treePixelHeight*referenceObjHeight)/referenceObjPixelHeight;

		Toast.makeText(this, String.format("Tree Height = ( %d * %d ) / %d = %d", treePixelHeight, (int)referenceObjHeight, referenceObjPixelHeight, (int)treeHeight), Toast.LENGTH_LONG).show();
	}

	private void detectTreetop() {
		Log.d(TAG, "detectTreetop");

		if (displayMat != null) {
			new TaskDetectTreetop().execute();	
		}
	}

	/**
	 * If the matrix is initialized, allow user touch input to trigger reference detection
	 */
	private void detectReference() {
		Log.d(TAG, "detectReference");

		if (displayMat != null) {
			imageView.setOnTouchListener(new View.OnTouchListener() {

				@Override
				public boolean onTouch(View v, MotionEvent event) {
					markTouch(event);
					return true;
				}
			});
			Toast.makeText(getApplicationContext(), "Reference detection touch input enabled", Toast.LENGTH_SHORT).show();
		}
	}

	/**
	 * Detect a selected color in the displayed image by setting its upper and lower limit values
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
	private void detectColor(Mat mat) {
		Log.d(TAG, "detectColor");

		Scalar colorUpLimit = null;
		Scalar colorLowLimit = null;

		colorUpLimit = new Scalar(colourUpperLimit[INDEX_HUE],colourUpperLimit[INDEX_SATURATION], colourUpperLimit[INDEX_VALUE]);
		colorLowLimit = new Scalar(colourLowerLimit[INDEX_HUE],colourLowerLimit[INDEX_SATURATION], colourLowerLimit[INDEX_VALUE]);

		Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2HSV, 0);
		Core.inRange(mat, colorLowLimit, colorUpLimit, mat);
	}


	/**
	 * Obtain touch position and execute corresponding task according to current program state
	 * <ul>
	 * <li>State (treetop): handle one finger touch to execute treetop detection task</li>
	 * <li>State (referenceObject):handle two fingers touch to execute reference object detection task</li>
	 * </ul>
	 * @param event
	 */
	private void markTouch(MotionEvent event) {
		Log.d(TAG, "markTouch");

		switch(currentState) {
		case STATE_TREETOP:
			//handle touch of one finger
			int posY = (int) (event.getY() *heightRatio);
			int posX = (int) (event.getX() *widthRatio);
			//start treetop detection at touch position
			taskDetectTreetop = new TaskDetectTreetop(posY, posX).execute();
			break;
		case STATE_REFERENCE:
			//handle touch of two fingers
			if(MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_POINTER_DOWN) {
				if (MotionEventCompat.getPointerCount(event) == 2) {
					//start reference object detection at touch boundaries
					taskDetectReference = new TaskDetectReference(
							(int) (MotionEventCompat.getY(event, 0) *heightRatio), 
							(int) (MotionEventCompat.getX(event, 0) *widthRatio), 
							(int) (MotionEventCompat.getY(event, 1) *heightRatio), 
							(int) (MotionEventCompat.getX(event, 1) *widthRatio)
							).execute();
				}
			}
			break;
		}
	}

	/**
	 * Calculate image pixels to screen pixels ratio (width ratio and height ratio separately).
	 * Normally, the ratios are greater than 1
	 */
	private void calculateImage2ScreenRatio() {
		Log.d(TAG, "calculateImage2ScreenRatio");

		Display display = getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);

		int screenWid = size.x;
		int screenHei = size.y;

		if (displayMat != null) {
			heightRatio = displayMat.rows()/(double)screenHei;
			widthRatio = displayMat.cols()/(double)screenWid;
		}
	}

	/**
	 * AsyncTask (off main UI thread) to detect treetop in image matrix
	 * Detection Algorithm:
	 * <ol>
	 * <li>Limit search region within touch range (default is matrix top third)</li>
	 * <li>Detect edges using Sobel filter</li>
	 * <li>Use few top matrix rows to learn standard deviation value for sky (normally, sky occupies some of the top rows of image)</li>
	 * <li>Use learned standard deviation value as threshold</li>
	 * <li>The row with standard deviation value above threshold is marked as treetop</li>
	 * </ol>   
	 * @author Aiman
	 *
	 */
	private class TaskDetectTreetop extends AsyncTask<Void, Void, Integer> {

		private ProgressDialog progressDialog;
		private Mat processingMat;
		private int minRow;
		private int maxRow;
		private int minCol;
		private int maxCol;
		private boolean isLocalSearch;

		public TaskDetectTreetop() {
			Log.d(TAG, "TaskDetectTreetop");

			//setup search region
			minRow = 0;
			maxRow = originalMat.rows()/3;
			minCol = 0;
			maxCol = originalMat.cols();
			isLocalSearch = false;
		}

		public TaskDetectTreetop(int yPos, int xPos) {
			Log.d(TAG, "TaskDetectTreetop");

			int rowRaduis = 50;
			int colRaduis = 50;
			isLocalSearch = true;

			//setup search region
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

			if (maxRow > originalMat.rows()-1) {
				maxRow = originalMat.rows()-1;
			}

			if (maxCol > originalMat.cols()-1) {
				maxCol = originalMat.cols()-1;
			}
		}

		@Override
		protected Integer doInBackground(Void... params) {

			if(isLocalSearch) {
				//Mark area around touch position
				org.opencv.core.Point pt1 = new org.opencv.core.Point(minCol,minRow);
				org.opencv.core.Point pt2 = new org.opencv.core.Point(maxCol, maxRow);
				Scalar col = new Scalar(0,255,255);
				Core.rectangle(displayMat, pt1, pt2, col);	
			}

			//Sobel filter edge detection
			Mat egdeXmat = new Mat();
			Mat edgeYmat = new Mat();

			Imgproc.cvtColor(processingMat.clone(), egdeXmat, Imgproc.COLOR_RGB2GRAY);
			Imgproc.cvtColor(processingMat.clone(), edgeYmat, Imgproc.COLOR_RGB2GRAY);

			Imgproc.Sobel(egdeXmat, egdeXmat, egdeXmat.depth(), 1, 0); //detect in x direction
			Imgproc.Sobel(edgeYmat, edgeYmat, edgeYmat.depth(), 0, 1); //detect in y direction
			Core.addWeighted(egdeXmat, 0.5, edgeYmat, 0.5, 0, processingMat);

			//convert to binary mat
			Imgproc.threshold(processingMat, processingMat, 100, 255, Imgproc.THRESH_BINARY);

			//compute threshold
			double threshold = calculateThreshold();

			//detect treetop row
			for (int r = 0 ; r < processingMat.rows()-1 ; r ++) {
				if(isCancelled()) {
					return null;
				}

				if (Core.sumElems(processingMat.submat(new Range(r,  r+1), Range.all())).val[0] > threshold) {
					return r;
				}
			} 
			return null;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			displayMat = originalMat.clone();
			processingMat = originalMat.submat(minRow, maxRow, minCol, maxCol).clone();

			progressDialog = new ProgressDialog(ImageProcessingActivity.this);
			progressDialog.setMessage("Detecting Treetop...");
			progressDialog.show(); 
		}

		@Override
		protected void onPostExecute(Integer result) {
			progressDialog.dismiss();
			if (result != null) {
				treetopRow = minRow + result;

				//draw line at treetop position
				displayMat.submat(new Range(treetopRow, treetopRow+LINE_THICKNESS), Range.all()).setTo(new Scalar(255,0,0));
				Toast.makeText(getApplicationContext(), "Treetop Detected (Row: "+treetopRow+")", Toast.LENGTH_SHORT).show();

				//check with user if treetop is correctly detected
				verifyDetection();
			} else {
				Toast.makeText(getApplicationContext(), "No Treetop Detected .. try again or use different image", Toast.LENGTH_SHORT).show();
			}
			updateDisplayImage();
		}

		/**
		 * Calculate threshold used to detect treetop 
		 * It computes mean and standard deviation of top 10% rows and applies the 68–95–99.7 rule (cover 95%)
		 * @return threshold for treetop detection
		 */
		private double calculateThreshold() {
			int rowsCount = processingMat.rows()/10;
			Mat trainingMat = processingMat.submat(new Range(0,  rowsCount), Range.all());
			double calculationArray [] = new double[rowsCount];
			double sum = 0;
			double sumVar = 0;

			//compute sum of matrix row elements and sum of array elements
			for (int r = 0 ; r < rowsCount ; r++) {
				calculationArray[r] = Core.sumElems(trainingMat.submat(new Range(r, r+1), Range.all())).val[0];
				sum += calculationArray[r];
			}

			//compute mean of array elements
			double mean = sum/rowsCount;

			//compute variance
			for (int i = 0 ; i < calculationArray.length ; i++) {
				calculationArray[i] = Math.pow((calculationArray[i]-mean), 2);
			}

			//compute sum of variances 
			for (int i = 0 ; i < calculationArray.length ; i++) {
				sumVar += calculationArray[i];
			}

			//compute standard deviation
			double stDeviation = Math.sqrt(sumVar/calculationArray.length);

			//cover 95% of curve values
			return mean + 2 * stDeviation;
		}
	}

	/**
	 * AsyncTask (off main UI thread) to detect reference object within image matrix
	 * Detection Algorithm:
	 * <ol>
	 * <li>Limit search region within touch range</li>
	 * <li>Use HSV color model to threshold specified color (default is white)</li>
	 * <li>Select largest area of connected points of the specified color and mark it as reference object</li>
	 * </ol>  
	 * @author Aiman
	 *
	 */
	private class TaskDetectReference extends AsyncTask<Void, Void, Integer []> {

		private ProgressDialog progressDialog;
		private Mat processingMat;
		private int minRow;
		private int maxRow;
		private int minCol;
		private int maxCol;

		public TaskDetectReference(int posY0, int posX0, int posY1, int posX1) {
			Log.d(TAG, "TaskDetectReference");

			int rowRaduis = 50;
			int colRaduis = 50;

			//setup search area

			if(posY0 > posY1) {
				int temp = posY0;
				posY0 = posY1;
				posY1 = temp;
			}

			if(posX0 > posX1) {
				int temp = posX0;
				posX0 = posX1;
				posX1 = temp;
			}

			minRow = posY0-rowRaduis;
			maxRow = posY1+rowRaduis;
			minCol = posX0-colRaduis;
			maxCol = posX1+colRaduis;

			if (minRow < 0) {
				minRow = 0;
			}

			if (minCol < 0) {
				minCol = 0;
			}

			if (maxRow > originalMat.rows()-1) {
				maxRow = originalMat.rows()-1;
			}

			if (maxCol > originalMat.cols()-1) {
				maxCol = originalMat.cols()-1;
			}

		}

		@Override
		protected Integer [] doInBackground(Void... params) {

			// Mark area around touch position
			org.opencv.core.Point pt1 = new org.opencv.core.Point(minCol,minRow);
			org.opencv.core.Point pt2 = new org.opencv.core.Point(maxCol, maxRow);
			Scalar col = new Scalar(0,255,0);
			Core.rectangle(displayMat, pt1, pt2, col);	

			// Detect colour
			detectColor(processingMat);

			List<MatOfPoint> contours = new ArrayList<MatOfPoint>();  

			/// Find contours
			Imgproc.findContours(processingMat, contours, new Mat(), Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

			if (!contours.isEmpty()) {
				// Find largest contour
				double largestContourArea = 0;
				int largestContourIndex = 0;
				for( int i = 1; i < contours.size(); i++ ) {
					if(isCancelled()) {
						return null;
					}
					double contourArea = Imgproc.contourArea(contours.get(i),false); 
					if(contourArea > largestContourArea){
						largestContourArea = contourArea;
						largestContourIndex = i;
					}
				}

				//store reference object boundaries locations
				Rect rect = Imgproc.boundingRect(contours.get(largestContourIndex));
				Integer boundaries [] = new Integer[4];

				boundaries[INDEX_REF_TOP] = rect.y;
				boundaries[INDEX_REF_BOTTOM] = rect.y + rect.height;
				boundaries[INDEX_REF_LEFT] = rect.x;
				boundaries[INDEX_REF_RIGHT] = rect.x + rect.width;

				return boundaries;
			}
			return null;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			displayMat = originalMat.clone();
			processingMat = originalMat.submat(minRow, maxRow, minCol, maxCol).clone();

			progressDialog = new ProgressDialog(ImageProcessingActivity.this);
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
				//setup boundary points of reference object rectangle
				referenceObjBound[INDEX_REF_TOP] = minRow + result[INDEX_REF_TOP];
				referenceObjBound[INDEX_REF_BOTTOM] = minRow + result[INDEX_REF_BOTTOM];
				referenceObjBound[INDEX_REF_LEFT] = minCol + result[INDEX_REF_LEFT];
				referenceObjBound[INDEX_REF_RIGHT] = minCol + result[INDEX_REF_RIGHT];

				//draw rectangle around reference object
				org.opencv.core.Point pt1 = new org.opencv.core.Point(referenceObjBound[INDEX_REF_LEFT], referenceObjBound[INDEX_REF_TOP]);
				org.opencv.core.Point pt2 = new org.opencv.core.Point(referenceObjBound[INDEX_REF_RIGHT], referenceObjBound[INDEX_REF_BOTTOM]);
				Core.rectangle(displayMat, pt1, pt2, new Scalar(255,128,100), LINE_THICKNESS);	

				Toast.makeText(getApplicationContext(), "Reference Object Detected (Row: "+referenceObjBound[INDEX_REF_TOP]+"-"+referenceObjBound[INDEX_REF_BOTTOM]+")", Toast.LENGTH_SHORT).show();

				//check with user if reference object is correctly detected
				verifyDetection();
			} else {
				Toast.makeText(getApplicationContext(), "No Reference Object Detected .. try again or use different image", Toast.LENGTH_SHORT).show();
			}
			updateDisplayImage();
		}
	}


	/**
	 * AsyncTask (off main UI thread) to simulate animation of repeating reference object image up to detected line for treetop
	 * @author Aiman
	 *
	 */
	private class TaskAnimateReference extends AsyncTask<Void, Void, Void> {
		Mat processingMat;

		@Override
		protected Void doInBackground(Void... params) {
			duplicateReference();
			handleFinalVisual();
			return null;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			displayMat = originalMat.clone();
			processingMat = displayMat.submat(referenceObjBound[INDEX_REF_TOP], referenceObjBound[INDEX_REF_BOTTOM], referenceObjBound[INDEX_REF_LEFT], referenceObjBound[INDEX_REF_RIGHT]).clone();
		}

		@Override
		protected void onProgressUpdate(Void... values) {
			super.onProgressUpdate(values);
			updateDisplayImage();
		}

		@Override
		protected void onPostExecute(Void result) {
			//set position of textView for tree height calculation formula

			buttonTask.setText(String.format("Tree Height = %.2f cm", treeHeight));

			textTreeHeight.setText(String.format("Tree Height = ( %d * %d ) / %d = %d", treePixelHeight, (int)referenceObjHeight, referenceObjPixelHeight, (int)treeHeight));
			textTreeHeight.setPivotX(0);
			textTreeHeight.setPivotY(0);
			textTreeHeight.setY((int)(treetopRow/heightRatio)-30);

			if (referenceObjBound[INDEX_REF_LEFT] > displayMat.cols() - referenceObjBound[INDEX_REF_RIGHT]) {
				textTreeHeight.setX((int)((referenceObjBound[INDEX_REF_LEFT]-(7*referenceObjBound[INDEX_REF_LEFT]/16))/widthRatio));
			} else {
				textTreeHeight.setX((int)(referenceObjBound[INDEX_REF_RIGHT]/widthRatio));
			}
		}

		/**
		 * Duplicate reference object image up to the treetop
		 */
		private void duplicateReference() {
			Log.d(TAG,"duplicateReference");

			int numDuplicates = treePixelHeight/referenceObjPixelHeight;

			for (int duplicate = 0 ; duplicate < numDuplicates ; duplicate++) {
				if(isCancelled()) {
					return;
				}
				try {
					processingMat.copyTo(displayMat.submat(referenceObjBound[INDEX_REF_TOP]-(duplicate*referenceObjPixelHeight), referenceObjBound[INDEX_REF_BOTTOM]-(duplicate*referenceObjPixelHeight), referenceObjBound[INDEX_REF_LEFT], referenceObjBound[INDEX_REF_RIGHT]));
					publishProgress();
					Thread.sleep((long) (0.25*1000));
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			//Fill remaining area of less than one duplicate
			processingMat = processingMat.submat(new Range(0, treePixelHeight%referenceObjPixelHeight), Range.all());
			processingMat.copyTo(displayMat.submat(treetopRow, treetopRow+processingMat.rows(), referenceObjBound[INDEX_REF_LEFT], referenceObjBound[INDEX_REF_RIGHT]));
			publishProgress();
		}

		/**
		 * Completes screen visuals (draw lines for tree height)
		 */
		private void handleFinalVisual() {
			Log.d(TAG,"handleFinalVisual");

			int offset;

			//draw visuals on the left of reference object
			if (referenceObjBound[INDEX_REF_LEFT] > displayMat.cols() - referenceObjBound[INDEX_REF_RIGHT]) {
				offset = -1*referenceObjBound[INDEX_REF_LEFT]/4;

				// negative offset
				displayMat.submat(treetopRow, treeBottomRow, referenceObjBound[INDEX_REF_LEFT]+offset, referenceObjBound[INDEX_REF_LEFT]+offset+LINE_THICKNESS).setTo(new Scalar(255, 0, 0));
				displayMat.submat(treetopRow, treetopRow+LINE_THICKNESS, referenceObjBound[INDEX_REF_LEFT]+offset, referenceObjBound[INDEX_REF_LEFT]).setTo(new Scalar(255, 0, 0));
				displayMat.submat(treeBottomRow, treeBottomRow+LINE_THICKNESS, referenceObjBound[INDEX_REF_LEFT]+offset, referenceObjBound[INDEX_REF_LEFT]).setTo(new Scalar(255, 0, 0));

				//draw visuals on the right of reference object
			} else {
				offset = (displayMat.cols() - referenceObjBound[INDEX_REF_RIGHT])/4;

				displayMat.submat(treetopRow, treeBottomRow, referenceObjBound[INDEX_REF_RIGHT]+offset, referenceObjBound[INDEX_REF_RIGHT]+offset+LINE_THICKNESS).setTo(new Scalar(255, 0, 0));
				displayMat.submat(treetopRow, treetopRow+LINE_THICKNESS, referenceObjBound[INDEX_REF_RIGHT], referenceObjBound[INDEX_REF_RIGHT]+offset).setTo(new Scalar(255, 0, 0));
				displayMat.submat(treeBottomRow, treeBottomRow+LINE_THICKNESS, referenceObjBound[INDEX_REF_RIGHT], referenceObjBound[INDEX_REF_RIGHT]+offset).setTo(new Scalar(255, 0, 0));
			}
			publishProgress();
		}
	}


	/**
	 * Creates a dialog to input reference object height
	 * The value of reference object height is a positive double, and can't be zero
	 * The unit for reference object height is cm
	 */
	private void setupReferenceObjHeight() {
		//EditText to allow user input
		final EditText input = new EditText(this);
		input.setHint(R.string.ref_height_dialog_text);
		input.setRawInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

		//reference object height input-dialog setup
		final AlertDialog heightDialog = new AlertDialog.Builder(this, R.style.myCustomDialog)
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
								Toast.makeText(ImageProcessingActivity.this, "Input Must be greater than zero", Toast.LENGTH_SHORT).show();
							}
						} catch (NumberFormatException e) {
							Toast.makeText(ImageProcessingActivity.this, "Invalid Input", Toast.LENGTH_SHORT).show();
							Log.e(TAG, "exception", e);
						}
					}
				});
			}
		});
		heightDialog.setCancelable(false);
		heightDialog.show();
	}


	/**
	 * Loads selected image into application, and setup program matrices
	 * @throws FileNotFoundException
	 */
	private void loadDisplayImage() throws FileNotFoundException {
		Log.d(TAG, "loadDisplayImage");

		//retrieve image from storage
		InputStream imageStream = getContentResolver().openInputStream(imgUri);
		Bitmap loadedImage = BitmapFactory.decodeStream(imageStream);

		//rotate image
		Matrix matrix = new Matrix();
		matrix.postRotate(90);

		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
			matrix.postRotate(90);
		}
		loadedImage = Bitmap.createBitmap(loadedImage , 0, 0, loadedImage.getWidth(), loadedImage .getHeight(), matrix, true);

		//load image in image view 
		imageView = (ImageView) findViewById(R.id.imageViewCapturedImage);
		imageView.setImageBitmap(loadedImage);


		//create a matrix of the selected image for processing
		originalMat = new Mat(loadedImage.getHeight(), loadedImage.getWidth(), CvType.CV_8UC1);
		Utils.bitmapToMat(loadedImage, originalMat);
		displayMat = originalMat.clone();
	}


	/**
	 * Update the image displayed on screen
	 * Invoked by methods which process <i>displayMat</i> TaskDetectTreetop, TaskDetectReference, TaskAnimateReference, <b>handleFinalVisual()</b>
	 */
	private synchronized void updateDisplayImage() {
		Log.d(TAG, "updateImage");

		//FIXME: is it correct to keep synchronized?
		//FIXME: is this the correct format to use for Bitmap (RGB_565)?
		Bitmap image = Bitmap.createBitmap(displayMat.cols(), displayMat.rows(), Bitmap.Config.RGB_565);
		Utils.matToBitmap(displayMat, image);
		imageView.setImageBitmap(image);
	}


	/**
	 * Handle click on UI button according to program state
	 * @param v
	 */
	public void onClickButton(View v) {
		Log.d(TAG, "onClickButton");

		switch(currentState) {
		case STATE_TREETOP:
			buttonTask.setEnabled(false);
			detectTreetop();
			break;
		case STATE_REFERENCE:
			buttonTask.setEnabled(false);
			detectReference();
			break;
		case STATE_HEIGHT:
			buttonTask.setClickable(false);
			calculateHeight();
			//Animate reference duplicate
			taskAnimateReference = new TaskAnimateReference().execute();
			break;
		default:
			break;
		}
	}

	/**
	 * Opens a dialog box to verify whether the automatic detection for treetop or reference object is correct
	 * If correct: change program state, and enable UI button
	 * If incorrect: enable touch user-input
	 */
	private void verifyDetection() {
		Log.d(TAG, "verifyDetection");

		AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.myCustomDialog);
		builder.setMessage("Correct Detection?");

		//handle correct detection
		builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface arg0, int arg1) {
				switch(currentState) {
				case STATE_TREETOP:
					currentState = STATE_REFERENCE;
					buttonTask.setEnabled(true);
					buttonTask.setText(R.string.button_detect_reference);
					break;
				case STATE_REFERENCE:
					if (referenceObjBound[INDEX_REF_TOP] < treetopRow) {
						Toast.makeText(getApplicationContext(), "Reference object must be below treetop", Toast.LENGTH_SHORT).show();
					} else {
						currentState = STATE_HEIGHT;
						setupReferenceObjHeight();
						buttonTask.setEnabled(true);
						buttonTask.setText(R.string.button_calc_height);
						imageView.setOnTouchListener(null);
					}
					break;
				default:
					break;
				}
			}
		});

		//handle incorrect detection
		builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface arg0, int arg1) {
				switch(currentState) {
				case STATE_TREETOP:
					Toast.makeText(getApplicationContext(), "Treetop detection touch input enabled", Toast.LENGTH_SHORT).show();
					imageView.setOnTouchListener(new View.OnTouchListener() {

						@Override
						public boolean onTouch(View v, MotionEvent event) {
							markTouch(event);	
							return false;
						}
					});
					break;
				case STATE_REFERENCE:
					Toast.makeText(getApplicationContext(), "Try again or use different image", Toast.LENGTH_SHORT).show();
					break;
				}
			}
		});

		//dialog additional configuration
		AlertDialog verifyDetectionDialog = builder.create();
		verifyDetectionDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		WindowManager.LayoutParams dialogAttrib = verifyDetectionDialog.getWindow().getAttributes();

		Point point = new Point();
		getWindowManager().getDefaultDisplay().getSize(point);

		dialogAttrib.gravity = Gravity.BOTTOM;
		dialogAttrib.y = 100;

		verifyDetectionDialog.setCancelable(false);
		verifyDetectionDialog.show();
	}

	//	---------------- Activity Methods ---------------- //

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_image_processing);

		//initializations
		currentState = STATE_TREETOP;
		selectedMeasurementsUnit = UNITS_CM;

		treeHeight = 0;
		treePixelHeight = 0;
		referenceObjHeight = 0;

		treetopRow = 0;
		treeBottomRow = 0;

		referenceObjBound = new int [4];
		colourLowerLimit = new float[3];
		colourUpperLimit = new float[3];

		//white colour HSV representation
		colourLowerLimit[INDEX_HUE] = 0;
		colourLowerLimit[INDEX_SATURATION] = 0;
		colourLowerLimit[INDEX_VALUE] = 80 * 255 /(float) 100;

		colourUpperLimit[INDEX_HUE] = 180;
		colourUpperLimit[INDEX_SATURATION] = 255;
		colourUpperLimit[INDEX_VALUE] = 255;

		buttonTask = (Button) findViewById(R.id.buttonTask);
		textTreeHeight = (TextView) findViewById(R.id.textTreeHeight);
		imgUri = getIntent().getExtras().getParcelable("ImgUri");

		taskDetectTreetop = null;
		taskDetectReference = null;
		taskAnimateReference = null;

		mLoaderCallback = new BaseLoaderCallback(this) {
			@Override
			public void onManagerConnected(int status) {
				switch (status) {
				case LoaderCallbackInterface.SUCCESS:
					try {
						//load image and setup
						loadDisplayImage();

						//calculate image to screen ratio
						calculateImage2ScreenRatio();
					} catch (FileNotFoundException e) {
						Toast.makeText(getApplicationContext(), "Error locating the file path", Toast.LENGTH_SHORT).show();
						Log.e(TAG, ""+e.getMessage());
						finish();
					} catch (Exception e) {
						Toast.makeText(getApplicationContext(), "Error!!", Toast.LENGTH_SHORT).show();
						Log.e(TAG, ""+e.getMessage());
						finish();
					}
					break;
				default:
					super.onManagerConnected(status);
					break;
				}
			}
		};

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.image_processing, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		switch(id){
		case R.id.action_settings:
			Intent intent = new Intent(this, SettingsActivity.class);
			startActivityForResult(intent, REQUEST_CODE_SETTING_ACTIVITY);
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onDestroy() {
		Log.d(TAG, "onDestroy");
		super.onDestroy();

		if(taskDetectTreetop != null) {
			taskDetectTreetop.cancel(true);
		}

		if(taskDetectReference != null) {
			taskDetectReference.cancel(true);
		}

		if(taskAnimateReference != null) {
			taskAnimateReference.cancel(true);
		}
		//FIXME: when activity starts again after shutting tasks, they don't run again
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
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this, mLoaderCallback);
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

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (requestCode == REQUEST_CODE_SETTING_ACTIVITY) {
			if(resultCode == RESULT_OK){
				colourLowerLimit = data.getFloatArrayExtra("colourLowerLimit");
				colourUpperLimit = data.getFloatArrayExtra("colourUpperLimit");
				selectedMeasurementsUnit = data.getIntExtra("measurementsUnits", UNITS_CM);
			}
			if (resultCode == RESULT_CANCELED) {
				Log.e(TAG, "Error saving settings");
			}
		}
	}

}
