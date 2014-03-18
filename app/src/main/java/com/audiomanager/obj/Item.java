package com.audiomanager.obj;

public class Item {
	private long mId;

	public Item(long id) {
		mId = id;
	}

	public long getId() {
		return mId;
	}

	@Override
	public String toString() {
		return "Item "+mId;
	}
}
