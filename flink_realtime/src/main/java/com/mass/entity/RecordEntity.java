package com.mass.entity;

public class RecordEntity {
    private int userId;
    private int itemId;
    private long time;

    public RecordEntity() { }

    public RecordEntity(int userId, int itemId, long time) {
        this.userId = userId;
        this.itemId = itemId;
        this.time = time;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getItemId() {
        return itemId;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public String toString() {
        return String.format("user: %5d, item: %5d, time: %5d", userId, itemId, time);
    }
}
