package com.mass.entity;

import java.util.List;

public class UserConsumed {
    public int userId;
    public List<Integer> items;
    public long windowEnd;

    public static UserConsumed of(int userId, List<Integer> items, long windowEnd) {
        UserConsumed result = new UserConsumed();
        result.userId = userId;
        result.items = items;
        result.windowEnd = windowEnd;
        return result;
    }

    public int getUserId() {
        return userId;
    }

    public List<Integer> getItems() {
        return items;
    }

    public long getWindowEnd() {
        return windowEnd;
    }

    public String toString() {
        return String.format("userId:%5d, itemConsumed:%6s, windowEnd:%13s",
                userId, items, windowEnd);
    }
}

