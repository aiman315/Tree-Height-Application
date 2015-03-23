package com.amb12u.treeheight;


/**
 * Class to hold programme stage
 * @author Aiman
 *
 */
public class ProgramStage{

	private int stage;
	private IntStageListener stageListener;

	public ProgramStage() {
		stage = 0;
	}

	public ProgramStage(int stage) {
		this.stage = stage;
	}

	public void setStage(int stage) {
		this.stage = stage;
		if (stageListener != null) {
			stageListener.onStageChanged();
		}
	}

	public int getStage() {
		return stage;
	}

	public void setListener(IntStageListener stageListener) {
		this.stageListener = stageListener;
	}

	public String toString() {
		String stageString = "Stage is (";
		switch (stage) {
		case 0:
			stageString += "STAGE_HEIGHT_INPUT)";
			break;
		case 1:
			stageString += "STAGE_TREETOP_ANGLE)";
			break;
		case 2:
			stageString += "STAGE_TREE_BOTTOM_ANGLE)";
			break;
		case 3:
			stageString += "STAGE_CALCULATE_TREE_HEIGHT)";
			break;
		default:
			stageString += "not recognised)";
			break;
		}
		return stageString;
	}
}
