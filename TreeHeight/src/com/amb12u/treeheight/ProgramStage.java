package com.amb12u.treeheight;



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
}
