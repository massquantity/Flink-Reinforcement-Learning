package com.mass.entity;

public class TopItemEntity {
    private int itemId;
    private int counts;
    private long windowEnd;

    public static TopItemEntity of(Integer itemId, long end, Long count) {
        TopItemEntity res = new TopItemEntity();
        res.setCounts(count.intValue());
        res.setItemId(itemId);
        res.setWindowEnd(end);
        return res;
    }

    public long getWindowEnd() {
        return windowEnd;
    }

    public void setWindowEnd(long windowEnd) {
        this.windowEnd = windowEnd;
    }


    public int getItemId() {
        return itemId;
    }

    public void setItemId(int productId) {
        this.itemId = productId;
    }

    public int getCounts() {
        return counts;
    }

    public void setCounts(int actionTimes) {
        this.counts = actionTimes;
    }

    public String toString() {
        return String.format("TopItemEntity: itemId:%5d, count:%3d, windowEnd: %5d", itemId, counts, windowEnd);
    }
}

