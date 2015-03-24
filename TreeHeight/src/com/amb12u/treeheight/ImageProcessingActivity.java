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
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.MotionEventCompat;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class ImageProcessingActivity extends Activity {
	private final String TAG = "StillImageProcessingActivity";

	private final int STATE_TREETOP = 0;
	private final int STATE_REFERENCE = 1;
	private final int STATE_HEIGHT = 2;

	protected final int INDEX_RATIO_Y = 0;
	protected final int INDEX_RATIO_X = 1;

	private final int INDEX_REF_BOTTOM = 0;
	private final int INDEX_REF_RIGHT = 1;
	private final int INDEX_REF_TOP = 2;
	private final int INDEX_REF_LEFT = 3;

	private final double HEIGHT_A4_PAPER = 29.7;

	private final int LINE_THICKNESS = 5;

	private double heightRatio, widthRatio;
	private double treeHeight, referenceObjHeight;
	private int treePixelHeight, referenceObjPixelHeight;
	private int treetopRow, treeBottomRow;
	private int [] referenceObjBound;
	private int currentState;
	
	private boolean isInstructionEnabled;
	private boolean isPortraitImg;
	
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
	//Test for landscape images (Added isPortraitImg variable . use it)
	//Indicate that tree bottom = reference object bottom
	//Code documentation
	//markTouch at bottom of image has large offset

	//FIXME:
	//offset for clicks at bottom causing app to crash


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
			
			//show instruction
			showIncsturctions();
			
			imageView.setOnTouchListener(new View.OnTouchListener() {

				@Override
				public boolean onTouch(View v, MotionEvent event) {
					markTouch(event);
					return true;
				}
			});
			Toast.makeText(getApplicationContext(), "A4 paper detection touch input enabled", Toast.LENGTH_SHORT).show();
		}
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
			//start treetop detection at touch position
			taskDetectTreetop = new TaskDetectTreetop((int) (event.getY() *heightRatio), (int) (event.getX() *widthRatio)).execute();
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
			heightRatio = (double) displayMat.rows()/screenHei;
			widthRatio = (double) displayMat.cols()/screenWid;
		}
	}

	/**
	 * AsyncTask (off main UI thread) to detect treetop in image matrix
	 * Detection Algorithm:
	 * <ol>
	 * <li>Limit search region within touch range (default is entire image)</li>
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
			maxRow = originalMat.rows();
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

			if (minRow > originalMat.rows()-1) {
				minRow = originalMat.rows()-1;
			}

			if (minCol > originalMat.cols()-1) {
				minCol = originalMat.cols()-1;
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
				Toast.makeText(getApplicationContext(), "No Treetop Detected .. try touch input or use a different image", Toast.LENGTH_SHORT).show();
				imageView.setOnTouchListener(new View.OnTouchListener() {

					@Override
					public boolean onTouch(View v, MotionEvent event) {
						markTouch(event);	
						return false;
					}
				});
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
	 * <li>Use HSV color model to threshold white colour</li>
	 * <li>Select largest area of connected points of white colour and mark it as reference object</li>
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

			// Detect white colour
			Imgproc.cvtColor(processingMat, processingMat, Imgproc.COLOR_BGR2GRAY, 0);
			Imgproc.threshold(processingMat, processingMat, (float) 60 * 255 / 100, 255, Imgproc.THRESH_BINARY);


			/// Find contours
			List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
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
			progressDialog.setMessage("Detecting A4 paper...");
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

				Toast.makeText(getApplicationContext(), "A4 Paper Detected (Row: "+referenceObjBound[INDEX_REF_TOP]+"-"+referenceObjBound[INDEX_REF_BOTTOM]+")", Toast.LENGTH_SHORT).show();

				//check with user if reference object is correctly detected
				verifyDetection();
			} else {
				Toast.makeText(getApplicationContext(), "No A4 Paper Detected .. try again or use different image", Toast.LENGTH_SHORT).show();
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

			textTreeHeight.setText(String.format("Tree Height = ( %d * %.2f ) / %d = %.2f cm", treePixelHeight, referenceObjHeight, referenceObjPixelHeight, treeHeight));
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

		Log.i("XXXXX", "height "+loadedImage.getHeight());
		Log.i("XXXXX", "width "+loadedImage.getWidth());

		if (loadedImage.getHeight() < loadedImage.getWidth()) {
			isPortraitImg = false;
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
			buttonTask.setVisibility(View.INVISIBLE);
			detectTreetop();
			break;
		case STATE_REFERENCE:
			buttonTask.setVisibility(View.INVISIBLE);
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
					buttonTask.setVisibility(View.VISIBLE);
					buttonTask.setText(R.string.button_detect_reference);
					break;
				case STATE_REFERENCE:
					if (referenceObjBound[INDEX_REF_TOP] < treetopRow) {
						Toast.makeText(getApplicationContext(), "A4 paper must be below treetop", Toast.LENGTH_SHORT).show();
					} else {
						currentState = STATE_HEIGHT;
						buttonTask.setVisibility(View.VISIBLE);
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
				default:
					return;
				}
				//show instruction dialog
				showIncsturctions();
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

	/**
	 * Display instruction dialog depending of programme state
	 */
	private void showIncsturctions() {
		Log.d(TAG, "showIncsturctions");
		
		if (!isInstructionEnabled) {
			return;
		}
		
		final Dialog dialogInstruction = new Dialog(ImageProcessingActivity.this, R.style.myInstructionDialog);
		String dialogTitle;
		
		switch(currentState) {
		case STATE_TREETOP:
			dialogTitle = "Treetop!";
			dialogInstruction.setContentView(R.layout.dialog_custom_ip_treetop_touch);
			((ImageView) dialogInstruction.findViewById(R.id.imageViewIpTouch)).startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.animation_touch));
			break;
		case STATE_REFERENCE:
			dialogTitle = "Paper!";
			dialogInstruction.setContentView(R.layout.dialog_custom_ip_tree_bottom_touch);
			((ImageView) dialogInstruction.findViewById(R.id.imageViewIpTouch1)).startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.animation_touch));
			((ImageView) dialogInstruction.findViewById(R.id.imageViewIpTouch2)).startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.animation_touch));
			break;
		case STATE_HEIGHT:
			default:
				return;
		}
		
		dialogInstruction.setTitle(dialogTitle);
		Button button = (Button) dialogInstruction.findViewById(R.id.buttonOkay);
		button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				dialogInstruction.dismiss();
			}
		});
		dialogInstruction.show();
		
	}
	
	//	---------------- Activity Methods ---------------- //
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.image_processing, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();

		switch(id) {
		case R.id.action_instruction:
			isInstructionEnabled = !isInstructionEnabled;
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_image_processing);
		//hide navigation bar
		getWindow().getDecorView().setSystemUiVisibility(
				View.SYSTEM_UI_FLAG_LAYOUT_STABLE
				| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
				| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
				| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
				| View.SYSTEM_UI_FLAG_FULLSCREEN
				| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

		//initializations
		currentState = STATE_TREETOP;
		isPortraitImg = true;
		isInstructionEnabled = true;

		treeHeight = 0;
		treePixelHeight = 0;
		referenceObjHeight = HEIGHT_A4_PAPER;

		treetopRow = 0;
		treeBottomRow = 0;

		referenceObjBound = new int [4];

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

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		if (hasFocus) {
			getWindow().getDecorView().setSystemUiVisibility(
					View.SYSTEM_UI_FLAG_LAYOUT_STABLE
					| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
					| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
					| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
					| View.SYSTEM_UI_FLAG_FULLSCREEN
					| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
		}
	}
}
