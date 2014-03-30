package com.heavyplayer.audioplayerrecorder.sample.obj;

public class Item {
	private long mId;

	public Item(long id) {
		mId = id;
	}

	public long getId() {
		return mId;
	}

	public String getFileName() {
		return "filename"+mId;
	}

	@Override
	public String toString() {
		return "Item "+mId;
	}
}
