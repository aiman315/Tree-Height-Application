package com.amb12u.treeheight;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Point;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class MathActivity extends Activity implements SensorEventListener, IntStageListener{


	//TODO: Move all text to strings.xml
	//complete instruction (How we calculated tree height)
	//delete Desc Activity
	//detect shake
	private final String TAG = "MathActivity";
	private final int INVALID_ANGLE = -999;

	private final int STAGE_HEIGHT_INPUT = 0;
	private final int STAGE_TREETOP_ANGLE = 1;
	private final int STAGE_TREE_BOTTOM_ANGLE = 2;
	private final int STAGE_CALCULATE_TREE_HEIGHT = 3;
	private final int STAGE_END = 4;

	private final String SELETED_CAMERA_ID_KEY = "selectedCameraId";
	private final int CAMERA_ID_NOT_SET = -1;

	// Determine existence of cameras
	private boolean hasCamera;
	private boolean hasFrontCamera;

	private Camera selectedCamera;
	private ProgramStage currentStage;
	private boolean isInstructionEnabled;
	private boolean isDebuggingEnabled;

	// Holds ID values for cameras
	private int backFacingCameraId;
	private int selectedCameraId;

	// Accelerometer variables
	private SensorManager sensorManager;
	private Sensor accelerometer;

	// Camera height
	private double heightCamera;

	// Angle readings
	private double accelerometerAngle;
	private double angleTreetop;
	private double angleTreeBottom;

	//Tree height
	private double heightTree;

	private SeekBar seekBarZoom;

	TextView textViewAngleNum;

	//Debugging textViews
	TextView textViewCameraHeight;
	TextView textViewX;
	TextView textViewY;
	TextView textViewZ;
	TextView textViewAngle;
	TextView textViewFirstAngle;
	TextView textViewSecondAngle;
	TextView textViewFormula;
	TextView textViewTreeHeight;

	/**
	 * Read and store current angle reading
	 * @param v: The view that invoked the method
	 */
	public void onClickReadAngle(View v) {
		Log.d(TAG, "onClickReadAngle");
		if (currentStage.getStage() == STAGE_TREETOP_ANGLE) {
			//Treetop angle must be positive
			if(accelerometerAngle >= 0) {
				angleTreetop = accelerometerAngle;
				Toast.makeText(this, String.format(getString(R.string.toast_angle_treetop), angleTreetop), Toast.LENGTH_SHORT).show();

				//change programme stage
				currentStage.setStage(STAGE_TREE_BOTTOM_ANGLE);
				takePicture(R.id.imageViewTreetop);
			} else {
				Toast.makeText(this, getString(R.string.toast_angle_treetop_invalid), Toast.LENGTH_SHORT).show();
			}
		} else if (currentStage.getStage() == STAGE_TREE_BOTTOM_ANGLE){
			if(accelerometerAngle < 0) {
				angleTreeBottom = accelerometerAngle;
				Toast.makeText(this, String.format(getString(R.string.toast_angle_tree_bottom), angleTreeBottom), Toast.LENGTH_SHORT).show();
				takePicture(R.id.imageViewTreeBottom);
				//change programme stage
				currentStage.setStage(STAGE_CALCULATE_TREE_HEIGHT);
			} else {
				Toast.makeText(this, getString(R.string.toast_angle_tree_bottom_invalid), Toast.LENGTH_SHORT).show();
			}
		}	
	}

	/**
	 * Reset angles readings
	 * @param v
	 */
	public void onClickResetAngle(View v) {
		Log.d(TAG, "onClickResetAngle");

		if (currentStage.getStage() == STAGE_TREE_BOTTOM_ANGLE) {
			currentStage.setStage(STAGE_TREETOP_ANGLE);
			angleTreetop = INVALID_ANGLE;
			Toast.makeText(this, getString(R.string.toast_angle_treetop_reset), Toast.LENGTH_SHORT).show();

		} else if (currentStage.getStage() == STAGE_CALCULATE_TREE_HEIGHT){
			currentStage.setStage(STAGE_TREE_BOTTOM_ANGLE);
			angleTreeBottom = INVALID_ANGLE;
			Toast.makeText(this, getString(R.string.toast_angle_tree_bottom_reset), Toast.LENGTH_SHORT).show();
		}
	}

	/**
	 * Calculate the total height of tree
	 * and reset angles readings
	 * @param v
	 */
	public void onClickCalculateHeight(View v) {
		Log.d(TAG, "onClickCalculateHeight");
		angleTreetop = Math.abs(angleTreetop);
		angleTreeBottom = Math.abs(angleTreeBottom);
		heightTree = Math.abs(heightCamera*((Math.tan(Math.toRadians(angleTreetop))/Math.tan(Math.toRadians(angleTreeBottom))) + 1));

		sensorManager.unregisterListener(this);
		currentStage.setStage(STAGE_END);
	}

	public void onClickReset(View v) {
		Log.d(TAG, "onClickReset");
		currentStage.setStage(STAGE_HEIGHT_INPUT);
	}

	/**
	 * Gets the ID of requested camera
	 * @param facingCamera: whether facing front or back
	 * @return cameraId: ID of camera requested
	 */
	private int getFacingCameraId(int facingCamera) {
		Log.d(TAG, "getFacingCameraId");

		int cameraId = CAMERA_ID_NOT_SET;
		int cameras = Camera.getNumberOfCameras();
		Camera.CameraInfo cameraInfo = new Camera.CameraInfo();

		for (int i = 0 ; i < cameras ; i++) {
			Camera.getCameraInfo(i, cameraInfo);
			// is camera in list of cameras in device the same as the one requested?
			if(cameraInfo.facing == facingCamera) {
				cameraId = i;
				break;
			}
		}
		return cameraId;
	}

	/**
	 * Gets ID of back facing camera
	 * @return backFacingCameraId
	 */
	private int getBackFacingCameraId() {
		Log.d(TAG, "getBackFacingCameraId");
		if(backFacingCameraId == CAMERA_ID_NOT_SET) {
			backFacingCameraId = getFacingCameraId(Camera.CameraInfo.CAMERA_FACING_BACK);
		}
		return backFacingCameraId;
	}

	/**
	 * Opens the selected camera
	 * and displays its preview on the activity
	 */
	private void openSelectedCamera() {
		Log.d(TAG, "openSelectedCamera");
		releaseSelectedCamera(); //to ensure no cameras are open

		if(selectedCameraId != CAMERA_ID_NOT_SET) {
			try {
				selectedCamera = Camera.open(selectedCameraId);

				CameraPreview cameraPreview = (CameraPreview) findViewById(R.id.cameraPreview);
				cameraPreview.connectCamera(selectedCamera, selectedCameraId);

				//configure camera zoom
				seekBarZoom = (SeekBar) findViewById(R.id.seekBarZoom);
				if (selectedCamera.getParameters().isZoomSupported()) {
					seekBarZoom.setMax(selectedCamera.getParameters().getMaxZoom());
					seekBarZoom.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

						@Override
						public void onStopTrackingTouch(SeekBar seekBar) {

						}

						@Override
						public void onStartTrackingTouch(SeekBar seekBar) {

						}

						@Override
						public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
							Camera.Parameters params = selectedCamera.getParameters();
							params.setZoom(progress);
							selectedCamera.setParameters(params);

						}
					});
				} else {
					seekBarZoom.setVisibility(View.INVISIBLE);
				}

			} catch (Exception e) { 
				Log.e(TAG, "exception", e);
				Toast.makeText(this, getString(R.string.toast_camera_open_error), Toast.LENGTH_SHORT).show();
			}
		}
	}

	/**
	 * Releases the selected camera object, and resets it
	 */
	private void releaseSelectedCamera() {
		Log.d(TAG, "releaseSelectedCamera");
		if(selectedCamera != null) {
			CameraPreview cameraPreview = (CameraPreview) findViewById(R.id.cameraPreview);
			cameraPreview.releaseCamera();
			selectedCamera.release();
			selectedCamera = null;
		}
	}

	/**
	 * Loads picture from preview into UI imageView with specified ID 
	 * @param imageViewID: ID of ImageView to load image into
	 */
	private void takePicture(final int imageViewID) {
		Log.d(TAG, "takePicture");
		selectedCamera.takePicture(null, null, new Camera.PictureCallback() {

			@Override
			public void onPictureTaken(byte[] data, Camera camera) {
				Point point = new Point();
				getWindowManager().getDefaultDisplay().getSize(point);

				ImageView imageViewTreetop = (ImageView) findViewById(imageViewID);
				Bitmap bitmapImage = BitmapFactory.decodeByteArray(data, 0, data.length);
				Matrix matrix = new Matrix();
				switch(getRotation()) {
				case Surface.ROTATION_0:
					matrix.postRotate(90);
					break;
				case Surface.ROTATION_180:
					matrix.postRotate(270);
					break;
				case Surface.ROTATION_270:
					matrix.postRotate(180);
					break;
				}
				bitmapImage = Bitmap.createBitmap(bitmapImage, 0, 0, bitmapImage.getWidth(), bitmapImage.getHeight(), matrix, false);
				bitmapImage = Bitmap.createScaledBitmap(bitmapImage, point.x/4, point.y/4, false);

				imageViewTreetop.getLayoutParams().height = bitmapImage.getHeight();
				imageViewTreetop.getLayoutParams().width = bitmapImage.getWidth();
				imageViewTreetop.setImageBitmap(bitmapImage);
				selectedCamera.startPreview();
			}
		});
	}

	/**
	 * Calculate the angle between two dimensions in degrees
	 * <b>axis</b>: either x-axis or y-axis
	 * <b>axis</b>: z-axis
	 * @param axis
	 * @param depth
	 * @return the angle between dimensions in degrees 
	 */
	private double calculateAngle(float axis, float depth) {
		Log.d(TAG, "calculateAngle");
		return Math.toDegrees(Math.atan(depth/axis));
	}

	/**
	 * Get the current rotation of device
	 * @return (0: portrait, 90: landscape, 180: reverse portrait, 270: reverse landscape)
	 */
	private int getRotation(){
		Log.d(TAG, "getRotation");
		return getWindowManager().getDefaultDisplay().getRotation();
	}

	/**
	 * Initializes accelerometer sensor and register listener
	 */
	private void setupAccelerometer() {
		Log.d(TAG, "setupAccelerometer");
		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
	}

	/**
	 * Creates a dialog to input camera height
	 * The value of camera height is positive double, and can't be zero
	 * The unit for camera height is cm
	 */
	private void setupCameraHeight() {
		Log.d(TAG, "setupCameraHeight");

		final Dialog dialogCameraHeight = new Dialog(this, R.style.myInstructionDialog);
		dialogCameraHeight.setContentView(R.layout.dialog_custom_person_height);
		dialogCameraHeight.setTitle(getString(R.string.dialog_camera_height_title));

		Button button = (Button) dialogCameraHeight.findViewById(R.id.buttonConfrimHeight);
		button.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View view) {
				//verify correct input
				try {
					EditText editTextHeight = (EditText) dialogCameraHeight.findViewById(R.id.editTextYourHeight);
					heightCamera = Double.parseDouble(editTextHeight.getText().toString());

					if (heightCamera > 0) {
						dialogCameraHeight.dismiss();

						CheckBox checkBoxInstruction = (CheckBox) dialogCameraHeight.findViewById(R.id.checkBoxInstructions);
						if (checkBoxInstruction.isChecked()){
							isInstructionEnabled = true;
						}
						
						//debugging text
						textViewCameraHeight.setText(String.format("Camera Height: %.2f", heightCamera));
						
						//change programme stage
						currentStage.setStage(STAGE_TREETOP_ANGLE);
					} else {
						Toast.makeText(MathActivity.this, getString(R.string.dialog_toast_height_below_zero), Toast.LENGTH_SHORT).show();
					}
				} catch (NumberFormatException e) {
					Toast.makeText(MathActivity.this, getString(R.string.dialog_toast_height_error), Toast.LENGTH_SHORT).show();
					Log.e(TAG, "exception", e);
				}
			}
		});
		dialogCameraHeight.setCancelable(false);
		dialogCameraHeight.show();
	}

	/**
	 * Update user interface according to current stage of programme
	 */
	private void updateUI() {
		Log.d(TAG, "updateUI");
		switch(currentStage.getStage()) {

		case STAGE_HEIGHT_INPUT:
			//setup buttons
			((Button) findViewById(R.id.buttonUndoAngle)).setEnabled(false);
			((Button) findViewById(R.id.buttonReadAngle)).setEnabled(false);
			((Button) findViewById(R.id.buttonCalculateHeight)).setEnabled(false);
			break;

		case STAGE_TREETOP_ANGLE: 
			//setup buttons
			((Button) findViewById(R.id.buttonUndoAngle)).setEnabled(false);
			((Button) findViewById(R.id.buttonReadAngle)).setEnabled(true);
			((Button) findViewById(R.id.buttonCalculateHeight)).setEnabled(false);

			//hide treetop and tree bottom imageViews and their texts
			((ImageView) findViewById(R.id.imageViewTreetop)).setVisibility(View.INVISIBLE);
			((ImageView) findViewById(R.id.imageViewTreeBottom)).setVisibility(View.INVISIBLE);

			((TextView) findViewById(R.id.textViewTreetop)).setVisibility(View.INVISIBLE);
			((TextView) findViewById(R.id.textViewTreeBottom)).setVisibility(View.INVISIBLE);

			//show sky gradient
			((ImageView) findViewById(R.id.imageViewSky)).setVisibility(View.VISIBLE);
			((ImageView) findViewById(R.id.imageViewGrass)).setVisibility(View.INVISIBLE);

			//show clouds and animate them
			((ImageView) findViewById(R.id.imageViewCloud1)).setVisibility(View.VISIBLE);
			((ImageView) findViewById(R.id.imageViewCloud2)).setVisibility(View.VISIBLE);
			((ImageView) findViewById(R.id.imageViewCloud1)).startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.animation_cloud));
			((ImageView) findViewById(R.id.imageViewCloud2)).startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.animation_cloud));

			//show person
			if (isInstructionEnabled) {
				((ImageView) findViewById(R.id.imageViewPerson)).setVisibility(View.VISIBLE);
			}
			break;

		case STAGE_TREE_BOTTOM_ANGLE: 
			//setup buttons
			((Button) findViewById(R.id.buttonUndoAngle)).setEnabled(true);
			((Button) findViewById(R.id.buttonReadAngle)).setEnabled(true);
			((Button) findViewById(R.id.buttonCalculateHeight)).setEnabled(false);

			//show treetop and hide tree bottom imageViews and their texts
			((ImageView) findViewById(R.id.imageViewTreetop)).setVisibility(View.VISIBLE);
			((ImageView) findViewById(R.id.imageViewTreeBottom)).setVisibility(View.INVISIBLE);

			((TextView) findViewById(R.id.textViewTreetop)).setVisibility(View.VISIBLE);
			((TextView) findViewById(R.id.textViewTreeBottom)).setVisibility(View.INVISIBLE);

			//hide sky and show grass
			((ImageView) findViewById(R.id.imageViewSky)).setVisibility(View.INVISIBLE);
			((ImageView) findViewById(R.id.imageViewGrass)).setVisibility(View.VISIBLE);

			//hide clouds and stop animation
			((ImageView) findViewById(R.id.imageViewCloud1)).setVisibility(View.INVISIBLE);
			((ImageView) findViewById(R.id.imageViewCloud2)).setVisibility(View.INVISIBLE);
			((ImageView) findViewById(R.id.imageViewCloud1)).clearAnimation();
			((ImageView) findViewById(R.id.imageViewCloud2)).clearAnimation();


			//show zoom seek bar
			((SeekBar) findViewById(R.id.seekBarZoom)).setVisibility(View.VISIBLE);

			//show cross
			((View) findViewById(R.id.horizontal_cross)).setVisibility(View.VISIBLE);
			((View) findViewById(R.id.vertical_cross)).setVisibility(View.VISIBLE);

			//show person
			if (isInstructionEnabled) {
				((ImageView) findViewById(R.id.imageViewPerson)).setVisibility(View.VISIBLE);
			}

			//debugging text
			textViewFirstAngle.setText(String.format("angle treetop = %.2f",angleTreetop));
			break;

		case STAGE_CALCULATE_TREE_HEIGHT: 	
			//setup buttons
			((Button) findViewById(R.id.buttonUndoAngle)).setEnabled(true);
			((Button) findViewById(R.id.buttonReadAngle)).setEnabled(false);
			((Button) findViewById(R.id.buttonCalculateHeight)).setEnabled(true);

			//show treetop and tree bottom imageViews and their texts
			((ImageView) findViewById(R.id.imageViewTreetop)).setVisibility(View.VISIBLE);
			((ImageView) findViewById(R.id.imageViewTreeBottom)).setVisibility(View.VISIBLE);

			((TextView) findViewById(R.id.textViewTreetop)).setVisibility(View.VISIBLE);
			((TextView) findViewById(R.id.textViewTreeBottom)).setVisibility(View.VISIBLE);

			//hide sky and grass
			((ImageView) findViewById(R.id.imageViewSky)).setVisibility(View.INVISIBLE);
			((ImageView) findViewById(R.id.imageViewGrass)).setVisibility(View.INVISIBLE);

			//hide zoom seek bar
			((SeekBar) findViewById(R.id.seekBarZoom)).setVisibility(View.INVISIBLE);

			//hide cross
			((View) findViewById(R.id.horizontal_cross)).setVisibility(View.INVISIBLE);
			((View) findViewById(R.id.vertical_cross)).setVisibility(View.INVISIBLE);

			//hide person
			((ImageView) findViewById(R.id.imageViewPerson)).setVisibility(View.INVISIBLE);


			//debugging text
			textViewSecondAngle.setText(String.format("angle tree bottom = %.2f", angleTreeBottom));
			break;
		case STAGE_END:
			//setup buttons
			((Button) findViewById(R.id.buttonUndoAngle)).setVisibility(View.INVISIBLE);
			((Button) findViewById(R.id.buttonReadAngle)).setVisibility(View.INVISIBLE);
			((Button) findViewById(R.id.buttonCalculateHeight)).setVisibility(View.INVISIBLE);

			//show tree height
			textViewAngleNum.setText(String.format(getString(R.string.text_view_math_tree_height), heightTree));
			textViewAngleNum.setTextColor(Color.YELLOW);

			//show person
			if (isInstructionEnabled) {
				((ImageView) findViewById(R.id.imageViewPerson)).setVisibility(View.VISIBLE);
			}

			//debugging text
			textViewFormula.setText(String.format("%.2f*((%.2f/%.2f)+1)", heightCamera, angleTreetop, angleTreeBottom));
			textViewTreeHeight.setText(String.format("Tree Height = %.2f", heightTree));
			break;
		default:
			return;
		}
	}


	/**
	 * Display instructions using a custom dialog.
	 * Depends on the current application stage
	 * @param isEnabled: flag to show or hide instruction dialogs
	 */
	private void showInsturctions() {
		Log.d(TAG, "showInsturctions");

		final ImageView imageViewPerson = (ImageView) findViewById(R.id.imageViewPerson);

		//No instructions if user has disabled them OR current stage is height input or tree height calculation
		if (!isInstructionEnabled || currentStage.getStage() == STAGE_HEIGHT_INPUT || currentStage.getStage() == STAGE_CALCULATE_TREE_HEIGHT) {
			imageViewPerson.setOnTouchListener(null);
			return;
		}

		imageViewPerson.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {

				imageViewPerson.setVisibility(View.INVISIBLE);

				String dialogTitle;
				int dialogLayoutID;
				final Dialog dialogInstruction = new Dialog(MathActivity.this, R.style.myInstructionDialog);

				switch(currentStage.getStage()) {
				case STAGE_TREETOP_ANGLE: 
					dialogTitle = getString(R.string.dialog_treetop_title);
					dialogLayoutID = R.layout.dialog_custom_math_treetop;
					break;

				case STAGE_TREE_BOTTOM_ANGLE:
					dialogTitle = getString(R.string.dialog_tree_bottom_title);
					dialogLayoutID = R.layout.dialog_custom_math_tree_bottom;
					break;

				case STAGE_END: 
					dialogTitle = getString(R.string.dialog_math_how_title);
					dialogLayoutID = R.layout.dialog_custom_math_how;	
					break;

				default:
					return false;
				}

				dialogInstruction.setContentView(dialogLayoutID);
				dialogInstruction.setTitle(dialogTitle);
				Button button = (Button) dialogInstruction.findViewById(R.id.buttonOkay);
				button.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						dialogInstruction.dismiss();
					}
				});
				dialogInstruction.show();
				return false;
			}
		});
	}

	/**
	 * Sets the visibility of debugging textViews
	 * @param isEnabled: flag to show or hide debugging information
	 */
	private void showDebuggingInfo(boolean isEnabled) {
		Log.d(TAG, "showDebuggingInfo");
		if (isEnabled) {
			textViewCameraHeight.setVisibility(View.VISIBLE);

			textViewX.setVisibility(View.VISIBLE);
			textViewY.setVisibility(View.VISIBLE);
			textViewZ.setVisibility(View.VISIBLE);
			textViewAngle.setVisibility(View.VISIBLE);

			textViewFirstAngle.setVisibility(View.VISIBLE);
			textViewSecondAngle.setVisibility(View.VISIBLE);

			textViewFormula.setVisibility(View.VISIBLE);
			textViewTreeHeight.setVisibility(View.VISIBLE);

		} else {
			textViewCameraHeight.setVisibility(View.INVISIBLE);

			textViewX.setVisibility(View.INVISIBLE);
			textViewY.setVisibility(View.INVISIBLE);
			textViewZ.setVisibility(View.INVISIBLE);
			textViewAngle.setVisibility(View.INVISIBLE);

			textViewFirstAngle.setVisibility(View.INVISIBLE);
			textViewSecondAngle.setVisibility(View.INVISIBLE);

			textViewFormula.setVisibility(View.INVISIBLE);
			textViewTreeHeight.setVisibility(View.INVISIBLE);
		}
	}



	//	---------------- Activity Methods ---------------- //

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		Log.d(TAG, "onSaveInstanceState");
		//gets the selected camera ID from previous state
		outState.putInt(SELETED_CAMERA_ID_KEY, selectedCameraId);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate");
		setContentView(R.layout.activity_math);
		//hide navigation bar
		getWindow().getDecorView().setSystemUiVisibility(
				View.SYSTEM_UI_FLAG_LAYOUT_STABLE
				| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
				| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
				| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
				| View.SYSTEM_UI_FLAG_FULLSCREEN
				| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

		//initializations
		backFacingCameraId = CAMERA_ID_NOT_SET;
		selectedCameraId = CAMERA_ID_NOT_SET;

		heightCamera = 0;
		heightTree = 0;
		angleTreetop = INVALID_ANGLE;
		angleTreeBottom = INVALID_ANGLE;

		currentStage = new ProgramStage(STAGE_HEIGHT_INPUT);
		currentStage.setListener(this);

		isInstructionEnabled = true;
		isDebuggingEnabled = false;

		textViewAngleNum = (TextView)findViewById(R.id.textViewAngleNum);

		//debugging textViews
		textViewCameraHeight = (TextView)findViewById(R.id.textViewCameraHeight);
		textViewX = (TextView)findViewById(R.id.textViewX);
		textViewY = (TextView)findViewById(R.id.textViewY);
		textViewZ = (TextView)findViewById(R.id.textViewZ);
		textViewAngle = (TextView)findViewById(R.id.textViewAngle);
		textViewFirstAngle = (TextView) findViewById(R.id.textViewTreetopAngle);
		textViewSecondAngle = (TextView) findViewById(R.id.textViewTreeBottomAngle);
		textViewFormula = (TextView) findViewById(R.id.textViewFormula);
		textViewTreeHeight = (TextView) findViewById(R.id.textViewTotalHeight);


		//check for camera feature
		PackageManager pm = getPackageManager();
		hasCamera = pm.hasSystemFeature(PackageManager.FEATURE_CAMERA);
		hasFrontCamera = pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT);

		if (!(hasCamera || hasFrontCamera)) {
			Toast.makeText(this, getString(R.string.toast_camera_not_supported_error), Toast.LENGTH_SHORT).show();
		} else {
			//sets back facing camera as default for selected camera
			selectedCameraId = getBackFacingCameraId();
			//checks if the program has a saved state
			if (savedInstanceState != null) {
				selectedCameraId = savedInstanceState.getInt(SELETED_CAMERA_ID_KEY);
			}
			openSelectedCamera();
		}

		//accelerometer setup
		setupAccelerometer();

		//disable debugging information
		showDebuggingInfo(false);

		//camera height setup
		setupCameraHeight();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.math, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();

		switch(id) {
		case R.id.action_instruction:
			isInstructionEnabled = !isInstructionEnabled;
			return true;
		case R.id.action_debugging_information:
			isDebuggingEnabled = !isDebuggingEnabled;
			showDebuggingInfo(isDebuggingEnabled);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		Log.d(TAG, "onConfigurationChanged");
		super.onConfigurationChanged(newConfig);
		openSelectedCamera();
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
		releaseSelectedCamera(); //to allow other applications to use selected camera
		sensorManager.unregisterListener(this);
	}

	@Override
	protected void onResume() {
		Log.d(TAG, "onResume");
		super.onResume();
		openSelectedCamera(); //to restore control of selected camera resource
		sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
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

	//	---------------- Interface Methods ---------------- //

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		Log.d(TAG, "onAccuracyChanged");

	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		Log.d(TAG, "onSensorChanged");

		float valueX = event.values[0];
		float valueY = event.values[1];
		float valueZ = -1 * event.values[2]; //multiply by -1 to obtain Z actual readings for device on its back

		//Log.d(TAG, ""+System.currentTimeMillis()+","+event.values[0]+","+event.values[1]+","+(event.values[2]));
		int rotation = getRotation();
		switch (rotation) {
		case Surface.ROTATION_0:
			//portrait
			accelerometerAngle = calculateAngle(valueY, valueZ);
			break;
		case Surface.ROTATION_90:
			//landscape
			accelerometerAngle = calculateAngle(valueX, valueZ);
			break;
		case Surface.ROTATION_180:
			//reverse portrait
			accelerometerAngle = calculateAngle(-1*valueY, valueZ);
			break;
		default:
			//reverse landscape
			accelerometerAngle = calculateAngle(-1*valueX, valueZ);
			break;
		}
		textViewAngleNum.setText(String.format(getString(R.string.text_view_math_angle), accelerometerAngle));

		//debugging text
		textViewX.setText("acceleration X = "+Float.toString(valueX));
		textViewY.setText("acceleration Y = "+Float.toString(valueY));
		textViewZ.setText("acceleration Z = "+Float.toString(valueZ));
		textViewAngle.setText(String.format("Current angle = %.2f",accelerometerAngle));
	}

	@Override
	public void onStageChanged() {
		Log.d(TAG, "onStageChanged");
		showInsturctions();
		updateUI();
	}
}
