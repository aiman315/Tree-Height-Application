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
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
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

	private static final int STATE_TREETOP = 0;
	private static final int STATE_REFERENCE = 1;
	private static final int STATE_HEIGHT = 2;

	private static final int COLOR_RED = 0;
	private static final int COLOR_YELLOW = 1;
	private static final int COLOR_WHITE = 2;
	private static final int COLOR_BLACK = 3;

	private static final int DETECT_TYPE_TREETOP = 0;
	private static final int DETECT_TYPE_REFERENCE = 1;

	protected static final int RATIO_INDEX_Y = 0;
	protected static final int RATIO_INDEX_X = 1;

	private static final int INDEX_REF_BOTTOM = 0;
	private static final int INDEX_REF_RIGHT = 1;
	private static final int INDEX_REF_TOP = 2;
	private static final int INDEX_REF_LEFT = 3;

	private static final int LINE_THICKNESS = 5;

	private final String TAG = "StillImageProcessingActivity";
	private double heightRatio, widthRatio;
	private double treeHeight, referenceObjHeight;
	private int treePixelHeight, referenceObjPixelHeight;
	private int treetopRow, treeBottomRow;
	private int [] referenceObjBound;
	private int selectedColor;
	private int currentState;
	private Uri imgUri;
	private ImageView imageView;
	private Button buttonTask;
	private TextView textTreeHeight;
	private Mat displayMat;
	private Mat originalMat;

	private BaseLoaderCallback mLoaderCallback;

	//TODO:
	//Add remaining of reference duplication animation
	//Take photos with A4 paper
	//Make mathematical approach more visual
	//Test for landscape images
	//Indicate that tree bottom = reference object bottom
	//Code documentation


	/**
	 * Calculate tree height by finding the ratio of reference object to  the tree 
	 */
	private void calculateHeight() {
		Log.d(TAG, "onClickCalculateHeight");

		treeBottomRow = referenceObjBound[INDEX_REF_BOTTOM];
		treePixelHeight = treeBottomRow-treetopRow;
		referenceObjPixelHeight = referenceObjBound[INDEX_REF_BOTTOM]-referenceObjBound[INDEX_REF_TOP];


		new TaskAnimateRef().execute();

		//calculate tree height
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
	 * If the matrix is initialized and the reference object is not yet detected, 
	 * calls the method to detect reference object
	 */
	private void detectReference() {
		Log.d(TAG, "detectReference");

		if (displayMat != null) {
			imageView.setOnTouchListener(new View.OnTouchListener() {

				@Override
				public boolean onTouch(View v, MotionEvent event) {
					markTouch(v,event, DETECT_TYPE_REFERENCE);
					return true;
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
	private void detectColor(Mat mat) {

		Scalar colorUpLimit = null;
		Scalar colorLowLimit = null;

		switch(selectedColor) {
		case COLOR_RED:
			colorUpLimit = new Scalar(10, 255, 255);
			colorLowLimit = new Scalar(0, 100, 100);
			break;
		case COLOR_YELLOW:
			colorUpLimit = new Scalar(32, 255, 255);
			colorLowLimit = new Scalar(0, 100, 100);
			break;
		case COLOR_WHITE:
			colorUpLimit = new Scalar(180, 50, 255);
			colorLowLimit = new Scalar(0, 0, 230);
			break;
		case COLOR_BLACK:
			colorUpLimit = new Scalar(80, 80, 80);
			colorLowLimit = new Scalar(0, 0, 0);
			break;
		default:
			Log.e(TAG, "Error in colour selection");
			colorUpLimit = new Scalar(0, 0, 0);
			colorLowLimit = new Scalar(0, 0, 0);
			break;

		}

		Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2HSV, 0);
		Core.inRange(mat, colorLowLimit, colorUpLimit, mat);
	}

	private void markTouch(View v, MotionEvent event, int detectionType) {

		switch(detectionType) {
		case DETECT_TYPE_TREETOP:
			int posY = (int) (event.getY() *heightRatio);
			int posX = (int) (event.getX() *widthRatio);			
			new TaskDetectTreetop(posY, posX).execute();
			break;
		case DETECT_TYPE_REFERENCE:
			if(MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_POINTER_DOWN) {
				if (MotionEventCompat.getPointerCount(event) == 2) {
					//Log.i("XXXXX", "["+(event.getY()*heightRatio)+", "+(event.getX()*widthRatio)+"]");
					new TaskDetectReference(
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

		imgWid = displayMat.cols();
		imgHei = displayMat.rows();

		Log.i("XXXX", "img hei: "+imgHei+"\timg wid: "+imgWid);
		Log.i("XXXX", "screen hei: "+screenHei+"\tscreen wid: "+screenWid);

		imgWid = screenHei * imgWid / imgHei;
		imgHei = screenHei;

		diffWid = screenWid - imgWid;
		diffHei = screenHei - imgHei;

		offsetW = diffWid/2;	
		offsetH = diffHei/2;
		int [] imgOffset = {offsetH, offsetW};

		Imgproc.resize(displayMat, displayMat, new Size(screenWid, screenHei));
		updateImage();

		Log.i("XXXX", "NEW img hei: "+imgHei+"\timg wid: "+imgWid);
		Log.i("XXXX", "screen hei: "+screenHei+"\tscreen wid: "+screenWid);

		return imgOffset;
	}

	private double [] calculateImage2ScreenRatio() {
		double [] ratio = null;
		Display display = getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);

		int screenWid = size.x;
		int screenHei = size.y;

		if (displayMat != null) {
			ratio = new double[2];
			Log.i("XXXX", "screen hei: "+screenHei+"\tscreen wid: "+screenWid);
			Log.i("XXXX", "img hei: "+displayMat.rows()+"\timg wid: "+displayMat.cols());
			ratio[0] = displayMat.rows()/(double)screenHei;
			ratio[1] = displayMat.cols()/(double)screenWid;
		}
		return ratio; 
	}

	private class TaskDetectTreetop extends AsyncTask<Void, Void, Integer> {

		private ProgressDialog progressDialog;
		private Mat processingMat;
		private int minRow;
		private int maxRow;
		private int minCol;
		private int maxCol;

		public TaskDetectTreetop() {
			displayMat = originalMat.clone();
			minRow = 0;
			maxRow = originalMat.rows()/3;
			minCol = 0;
			maxCol = originalMat.cols();
			processingMat = originalMat.submat(minRow, maxRow, minCol, maxCol).clone();
		}

		public TaskDetectTreetop(int yPos, int xPos) {
			displayMat = originalMat.clone();

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

			if (maxRow > originalMat.rows()-1) {
				maxRow = originalMat.rows()-1;
			}

			if (maxCol > originalMat.cols()-1) {
				maxCol = originalMat.cols()-1;
			}

			processingMat = originalMat.submat(minRow, maxRow, minCol, maxCol).clone();

			//Mark area around touch position
			org.opencv.core.Point pt1 = new org.opencv.core.Point(minCol,minRow);
			org.opencv.core.Point pt2 = new org.opencv.core.Point(maxCol, maxRow);
			Scalar col = new Scalar(0,255,255);
			Core.rectangle(displayMat, pt1, pt2, col);
		}

		@Override
		protected Integer doInBackground(Void... params) {



			//Detection using Sobel Filter
			Mat egdeXmat = new Mat();
			Mat edgeYmat = new Mat();

			Imgproc.cvtColor(processingMat.clone(), egdeXmat, Imgproc.COLOR_RGB2GRAY);
			Imgproc.cvtColor(processingMat.clone(), edgeYmat, Imgproc.COLOR_RGB2GRAY);

			Imgproc.Sobel(egdeXmat, egdeXmat, egdeXmat.depth(), 1, 0); //detect in x direction
			Imgproc.Sobel(edgeYmat, edgeYmat, edgeYmat.depth(), 0, 1); //detect in y direction
			Core.addWeighted(egdeXmat, 0.5, edgeYmat, 0.5, 0, processingMat);

			/*			
			Mat learnMat = processingMat.submat(0,  100, minCol, maxCol);
			double sum = Core.sumElems(learnMat).val[0];
			double mean = sum/100;
			MatOfDouble stdMat = new MatOfDouble();
			Core.meanStdDev(learnMat, new MatOfDouble(), stdMat);
			double stDeviation = stdMat.toArray()[0];
			Log.i("XXXXX", "Sum = "+sum);
			Log.i("XXXXX", "Mean = "+mean);
			Log.i("XXXXX", "std = "+stDeviation);
			Log.i("XXXXX", "===================================");

			for (int r = minRow ; r < maxRow-1 ; r ++) {
				Log.i("XXXXXXXX", ""+Core.sumElems(processingMat.submat(r,  r+1, minCol, maxCol)).val[0]);
				if (Core.sumElems(processingMat.submat(r,  r+1, minCol, maxCol)).val[0] > (mean + 2 * stDeviation)) {
					return r;
				}
			}
			 */


			Imgproc.threshold(processingMat, processingMat, 100, 255, Imgproc.THRESH_BINARY);

			for (int r = 0 ; r < processingMat.cols() ; r++) {
				double sum = Core.sumElems(processingMat.submat(r,  r+1, 0, processingMat.cols()-1)).val[0];
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
			progressDialog = new ProgressDialog(ImageProcessingActivity.this);
			progressDialog.setMessage("Detecting Treetop...");
			progressDialog.show(); 
		}

		@Override
		protected void onPostExecute(Integer result) {
			progressDialog.dismiss();
			if (result != null) {
				treetopRow = minRow + result;
				Core.rectangle(displayMat, new org.opencv.core.Point(0,treetopRow), new org.opencv.core.Point(displayMat.cols(), treetopRow), new Scalar(255,0,0), LINE_THICKNESS);

				displayMat.submat(new Range(treetopRow, treetopRow+1), Range.all()).setTo(new Scalar(255,0,0));
				Toast.makeText(getApplicationContext(), "Treetop Detected (Row: "+treetopRow+")", Toast.LENGTH_SHORT).show();

				verifyDetection();
			} else {
				Toast.makeText(getApplicationContext(), "No Treetop Detected .. try again or use different image", Toast.LENGTH_SHORT).show();
			}
			updateImage();
		}
	}

	private class TaskDetectReference extends AsyncTask<Void, Void, Integer []> {

		private ProgressDialog progressDialog;
		private Mat processingMat;
		private int minRow;
		private int maxRow;
		private int minCol;
		private int maxCol;

		public TaskDetectReference(int posY0, int posX0, int posY1, int posX1) {

			int rowRaduis = 50;
			int colRaduis = 50;
			displayMat = originalMat.clone();

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

			processingMat = originalMat.submat(minRow, maxRow, minCol, maxCol).clone();
		}

		@Override
		protected Integer [] doInBackground(Void... params) {

			// Mark area around touch position
			org.opencv.core.Point pt1 = new org.opencv.core.Point(minCol,minRow);
			org.opencv.core.Point pt2 = new org.opencv.core.Point(maxCol, maxRow);
			Scalar col = new Scalar(0,255,0);
			Core.rectangle(displayMat, pt1, pt2, col);	

			//TODO: add options for color detection
			detectColor(processingMat);

			List<MatOfPoint> contours = new ArrayList<MatOfPoint>();  

			/// Find contours
			Imgproc.findContours(processingMat, contours, new Mat(), Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

			if (!contours.isEmpty()) {
				// Find largest contour
				double largestContourArea = 0;
				int largestContourIndex = 0;
				for( int i = 1; i < contours.size(); i++ ) {
					//Imgproc.drawContours(processingMat, contours, i, new Scalar(255,255,255, LINE_THICKNESS);
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
			progressDialog = new ProgressDialog(ImageProcessingActivity.this);
			progressDialog.setMessage("Detecting Reference Object...");
			progressDialog.show(); 
		}

		@Override
		protected void onProgressUpdate(Void... values) {
			super.onProgressUpdate(values);
			updateImage();
		}

		@Override
		protected void onPostExecute(Integer [] result) {
			progressDialog.dismiss();
			if (result != null) {
				referenceObjBound[INDEX_REF_TOP] = minRow + result[INDEX_REF_TOP];
				referenceObjBound[INDEX_REF_BOTTOM] = minRow + result[INDEX_REF_BOTTOM];
				referenceObjBound[INDEX_REF_LEFT] = minCol + result[INDEX_REF_LEFT];
				referenceObjBound[INDEX_REF_RIGHT] = minCol + result[INDEX_REF_RIGHT];

				org.opencv.core.Point pt1 = new org.opencv.core.Point(referenceObjBound[INDEX_REF_LEFT], referenceObjBound[INDEX_REF_TOP]);
				org.opencv.core.Point pt2 = new org.opencv.core.Point(referenceObjBound[INDEX_REF_RIGHT], referenceObjBound[INDEX_REF_BOTTOM]);
				Core.rectangle(displayMat, pt1, pt2, new Scalar(255,128,100), LINE_THICKNESS);	
				Toast.makeText(getApplicationContext(), "Reference Object Detected (Row: "+referenceObjBound[INDEX_REF_TOP]+"-"+referenceObjBound[INDEX_REF_BOTTOM]+")", Toast.LENGTH_SHORT).show();

				verifyDetection();
			} else {
				Toast.makeText(getApplicationContext(), "No Reference Object Detected .. try again or use different image", Toast.LENGTH_SHORT).show();
			}
			updateImage();
		}
	}

	private class TaskAnimateRef extends AsyncTask<Void, Void, Integer []> {
		Mat processingMat;

		@Override
		protected Integer [] doInBackground(Void... params) {
			displayMat = originalMat.clone();
			int numDuplicates = treePixelHeight/referenceObjPixelHeight;

			for (int duplicate = 0 ; duplicate < numDuplicates ; duplicate++) {
				try {
					processingMat.copyTo(displayMat.submat(referenceObjBound[INDEX_REF_TOP]-(duplicate*referenceObjPixelHeight), referenceObjBound[INDEX_REF_BOTTOM]-(duplicate*referenceObjPixelHeight), referenceObjBound[INDEX_REF_LEFT], referenceObjBound[INDEX_REF_RIGHT]));
					publishProgress();
					Thread.sleep((long) (0.25*1000));
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
			//sub reference to fill duplicate remains
			processingMat = processingMat.submat(new Range(0, treePixelHeight%referenceObjPixelHeight), Range.all());
			processingMat.copyTo(displayMat.submat(treetopRow, treetopRow+processingMat.rows(), referenceObjBound[INDEX_REF_LEFT], referenceObjBound[INDEX_REF_RIGHT]));
			publishProgress();

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
			updateImage();
		}

		@Override
		protected void onPostExecute(Integer [] result) {
			handleFinalVisual();
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

	private void handleFinalVisual() {

		buttonTask.setText(String.format("Tree Height = %.2f cm", treeHeight));

		textTreeHeight.setText(String.format("Tree Height = ( %d * %d ) / %d = %d", treePixelHeight, (int)referenceObjHeight, referenceObjPixelHeight, (int)treeHeight));
		textTreeHeight.setPivotX(0);
		textTreeHeight.setPivotY(0);
		textTreeHeight.setY((int)(treetopRow/heightRatio)-30);

		//draw line next to reference object 
		//and set tree height calculation text next to it
		int offset;
		if (referenceObjBound[INDEX_REF_LEFT] > displayMat.cols() - referenceObjBound[INDEX_REF_RIGHT]) {
			offset = -1*referenceObjBound[INDEX_REF_LEFT]/4;
			textTreeHeight.setX((int)((referenceObjBound[INDEX_REF_LEFT]+(7/4)*offset)/widthRatio));

			// negative offset
			displayMat.submat(treetopRow, treeBottomRow, referenceObjBound[INDEX_REF_LEFT]+offset, referenceObjBound[INDEX_REF_LEFT]+offset+LINE_THICKNESS).setTo(new Scalar(255, 0, 0));
			displayMat.submat(treetopRow, treetopRow+LINE_THICKNESS, referenceObjBound[INDEX_REF_LEFT]+offset, referenceObjBound[INDEX_REF_LEFT]).setTo(new Scalar(255, 0, 0));
			displayMat.submat(treeBottomRow, treeBottomRow+LINE_THICKNESS, referenceObjBound[INDEX_REF_LEFT]+offset, referenceObjBound[INDEX_REF_LEFT]).setTo(new Scalar(255, 0, 0));
		} else {
			offset = (displayMat.cols() - referenceObjBound[INDEX_REF_RIGHT])/4;
			textTreeHeight.setX((int)(referenceObjBound[INDEX_REF_RIGHT]/widthRatio));

			displayMat.submat(treetopRow, treeBottomRow, referenceObjBound[INDEX_REF_RIGHT]+offset, referenceObjBound[INDEX_REF_RIGHT]+offset+LINE_THICKNESS).setTo(new Scalar(255, 0, 0));
			displayMat.submat(treetopRow, treetopRow+LINE_THICKNESS, referenceObjBound[INDEX_REF_RIGHT], referenceObjBound[INDEX_REF_RIGHT]+offset).setTo(new Scalar(255, 0, 0));
			displayMat.submat(treeBottomRow, treeBottomRow+LINE_THICKNESS, referenceObjBound[INDEX_REF_RIGHT], referenceObjBound[INDEX_REF_RIGHT]+offset).setTo(new Scalar(255, 0, 0));
		}

		updateImage();
	}

	private void loadImage() throws FileNotFoundException {

		Log.i("XXXXXXXXX", "imgUri : "+imgUri);
		Log.i("XXXXXXXXX", "imgUri.getPath() : "+imgUri.getEncodedPath());


		//retrieve image from storage
		InputStream imageStream = getContentResolver().openInputStream(imgUri);
		Bitmap loadedImage = BitmapFactory.decodeStream(imageStream);

		/*

	    // First decode with inJustDecodeBounds=true to check dimensions
	    final BitmapFactory.Options options = new BitmapFactory.Options();
	    options.inJustDecodeBounds = true;
	    Bitmap loadedImage = BitmapFactory.decodeFile(imgUri.getPath(), options);
	    if (loadedImage == null) {
	    	Log.i("XXXXXXXXX", "loaded Image == null");
	    } else {
	    	Log.i("XXXXXXXXX", "loaded Image != null");
	    }

	    // Calculate inSampleSize
	    options.inSampleSize = calculateInSampleSize(options, 100, 100);

	    // Decode bitmap with inSampleSize set
	    options.inJustDecodeBounds = false;
	    loadedImage = BitmapFactory.decodeFile(imgUri.getPath(), options);

		 */





		//rotate image
		Matrix matrix = new Matrix();
		matrix.postRotate(90);

		getResources().getConfiguration();
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

	public static int calculateInSampleSize(
			BitmapFactory.Options options, int reqWidth, int reqHeight) {
		// Raw height and width of image
		final int height = options.outHeight;
		final int width = options.outWidth;

		Log.i("XXXXXXXXX", "height = "+height+"\twidth = "+width);
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth) {

			final int halfHeight = height / 2;
			final int halfWidth = width / 2;

			// Calculate the largest inSampleSize value that is a power of 2 and keeps both
			// height and width larger than the requested height and width.
			while ((halfHeight / inSampleSize) > reqHeight
					&& (halfWidth / inSampleSize) > reqWidth) {
				inSampleSize *= 2;
			}
		}
		return inSampleSize;
	}


	//FIXME: is it correct to keep synchronized?
	private synchronized void updateImage() {
		Bitmap image = Bitmap.createBitmap(displayMat.cols(), displayMat.rows(), Bitmap.Config.RGB_565);
		Utils.matToBitmap(displayMat, image);
		imageView.setImageBitmap(image);
	}

	public void onClickButton(View v) {
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
			break;
		default:
			break;
		}
	}

	private void verifyDetection() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.myCustomDialog);
		builder.setMessage("Correct Detection?");
		builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface arg0, int arg1) {
				switch(currentState) {
				case STATE_TREETOP:
					currentState = STATE_REFERENCE;
					buttonTask.setEnabled(true);
					buttonTask.setText(R.string.button_detect_reference);
					break;
				case STATE_REFERENCE:
					currentState = STATE_HEIGHT;
					setupReferenceObjHeight();
					buttonTask.setEnabled(true);
					buttonTask.setText(R.string.button_calc_height);
					imageView.setOnTouchListener(null);
					break;
				default:
					break;
				}
				return;
			}
		});

		builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface arg0, int arg1) {
				imageView.setOnTouchListener(new View.OnTouchListener() {

					@Override
					public boolean onTouch(View v, MotionEvent event) {
						switch(currentState) {
						case STATE_TREETOP:
							markTouch(v,event, DETECT_TYPE_TREETOP);
							return false;
						case STATE_REFERENCE:
							markTouch(v,event, DETECT_TYPE_REFERENCE);
							return true;
						}
						return false;
					}
				});
				Toast.makeText(getApplicationContext(), "Touch input enabled", Toast.LENGTH_SHORT).show();
			}
		});

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
		treeHeight = 0;
		treePixelHeight = 0;
		referenceObjHeight = 0;
		treetopRow = 0;
		treeBottomRow = 0;
		referenceObjBound = new int [4];
		selectedColor = COLOR_WHITE;
		buttonTask = (Button) findViewById(R.id.buttonTask);
		textTreeHeight = (TextView) findViewById(R.id.textTreeHeight);
		imgUri = getIntent().getExtras().getParcelable("ImgUri");

		mLoaderCallback = new BaseLoaderCallback(this) {
			@Override
			public void onManagerConnected(int status) {
				switch (status) {
				case LoaderCallbackInterface.SUCCESS:
					Log.i(TAG, "OpenCV loaded successfully");

					try {
						//load image and setup
						loadImage();

						//calculate image to screen ratio
						double ratio [] = calculateImage2ScreenRatio();
						heightRatio = ratio[RATIO_INDEX_Y];
						widthRatio = ratio[RATIO_INDEX_X];
						Log.i("XXXX", "Ratios ["+ratio[0]+", "+ratio[1]+"]");

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
			//FIXME: delete later
			switch(selectedColor) {
			case COLOR_RED:
				Toast.makeText(getApplicationContext(), "Detecting Color: WHITE", Toast.LENGTH_SHORT).show();
				selectedColor = COLOR_WHITE;
				break;
			case COLOR_WHITE:
				Toast.makeText(getApplicationContext(), "Detecting Color: BLACK", Toast.LENGTH_SHORT).show();
				selectedColor = COLOR_BLACK;
				break;
			case COLOR_BLACK:
				Toast.makeText(getApplicationContext(), "Detecting Color: RED", Toast.LENGTH_SHORT).show();
				selectedColor = COLOR_RED;
				break;
			}
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
}
