package com.readbright.app;

import android.content.Context;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ContinuousLearningEngine {

    private final DatabaseHelper dbHelper;
    private final String username;
    private final String historyKey;

    public ContinuousLearningEngine(Context context, String username, String historyKey) {
        this.dbHelper = DatabaseHelper.getInstance(context);
        this.username = (username == null || username.trim().length() == 0) ? "guest_user" : username.trim();
        this.historyKey = (historyKey == null || historyKey.length() == 0) ? "default_key" : historyKey;
    }

    private static class ItemData {
        String content;
        long lastUsed;
        ItemData(String content, long lastUsed) {
            this.content = content;
            this.lastUsed = lastUsed;
        }
    }

    public String[] getFreshItems(String[] sourceItems) {
        List<String> result = getFreshItems(sourceItems, (sourceItems != null) ? sourceItems.length : 0);
        return result.toArray(new String[0]);
    }

    public List<String> getFreshItems(String[] sourceItems, int amount) {
        // 1. Mastered items (Excluded permanently)
        List<String> mastered = dbHelper.getAIHistory(username, historyKey);
        Set<String> masteredSet = new HashSet<String>();
        for (String item : mastered) {
            if (item != null) masteredSet.add(item.trim().toLowerCase());
        }

        // 2. Usage timestamps
        Map<String, Long> usageMap = dbHelper.getUsageHistory(username, historyKey);

        // 3. Prepare the candidate pool
        List<ItemData> pool = new ArrayList<ItemData>();
        if (sourceItems != null) {
            for (String item : sourceItems) {
                if (item == null || item.trim().length() == 0) continue;
                String clean = item.trim().toLowerCase();
                if (!masteredSet.contains(clean)) {
                    Long val = usageMap.get(clean);
                    pool.add(new ItemData(item.trim(), (val != null) ? val : 0));
                }
            }
        }

        // 4. SMART SORT: Oldest or never seen first
        Collections.sort(pool, new Comparator<ItemData>() {
            @Override
            public int compare(ItemData a, ItemData b) {
                return (a.lastUsed < b.lastUsed) ? -1 : ((a.lastUsed == b.lastUsed) ? 0 : 1);
            }
        });

        // 5. Select batch and SHUFFLE for variety
        int takeCount = Math.min(pool.size(), Math.max(amount, 20));
        List<String> selection = new ArrayList<String>();
        for (int i = 0; i < takeCount; i++) {
            String content = pool.get(i).content;
            selection.add(content);
            // AUTO-MARK AS SEEN: This ensures rotation across sessions
            markAsSeen(content);
        }
        Collections.shuffle(selection);

        int limit = (amount <= 0) ? selection.size() : Math.min(amount, selection.size());
        return new ArrayList<String>(selection.subList(0, limit));
    }

    public void markCompleted(String item) {
        if (item == null || item.trim().length() == 0) return;
        dbHelper.addToAIHistory(username, item.trim().toLowerCase(), historyKey);
    }

    public void markAsSeen(String item) {
        if (item == null || item.trim().length() == 0) return;
        dbHelper.markAsSeen(username, item.trim().toLowerCase(), historyKey);
    }

    public void clearProgress() {
        dbHelper.clearAIHistory(username, historyKey);
    }
}
