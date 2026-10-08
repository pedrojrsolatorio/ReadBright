package com.readbright.app;

import android.content.Context;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ContinuousWordEngine {

    private final DatabaseHelper dbHelper;
    private final String username;
    private final String historyKey;

    public ContinuousWordEngine(Context context, String username, String historyKey) {
        this.dbHelper = DatabaseHelper.getInstance(context);
        this.username = (username == null || username.trim().length() == 0) ? "guest_user" : username.trim();
        this.historyKey = (historyKey == null || historyKey.length() == 0) ? "general_words" : historyKey;
    }

    // --- EXPANDED 500+ WORD BANK ---
    private final List<String> WORD_BANK = Arrays.asList(
            "ant", "ape", "baboon", "badger", "bat", "bear", "bee", "bison", "boar", "buffalo",
            "butterfly", "camel", "cat", "cheetah", "chicken", "chimpanzee", "cow", "crab", "crocodile", "deer",
            "dog", "dolphin", "donkey", "duck", "eagle", "elephant", "fish", "fly", "fox", "frog",
            "giraffe", "goat", "goldfish", "gorilla", "hamster", "hippopotamus", "horse", "hyena", "kangaroo", "koala",
            "leopard", "lion", "lizard", "llama", "monkey", "mouse", "octopus", "ostrich", "owl", "panda",
            "parrot", "penguin", "pig", "rabbit", "rat", "rhino", "shark", "sheep", "snake", "spider",
            "swan", "tiger", "toad", "turkey", "turtle", "whale", "wolf", "worm", "zebra", "yak",
            "apple", "apricot", "banana", "blackberry", "blueberry", "bread", "broccoli", "cabbage", "cake", "carrot",
            "cheese", "cherry", "chicken", "chocolate", "coconut", "coffee", "corn", "cucumber", "egg", "eggplant",
            "fig", "garlic", "grape", "honey", "ice cream", "juice", "lemon", "lettuce", "mango", "milk",
            "mushroom", "onion", "orange", "papaya", "pasta", "peach", "pear", "peas", "pineapple", "pizza",
            "plum", "potato", "pumpkin", "rice", "salad", "sandwich", "strawberry", "tomato", "watermelon", "yogurt",
            "bacon", "bean", "beef", "biscuit", "butter", "candy", "cookie", "donut", "flour", "grape",
            "ham", "hamburger", "jelly", "muffin", "noodle", "nut", "oil", "olive", "pancake", "pepper",
            "pickle", "pie", "salt", "sausage", "soup", "sugar", "tea", "toast", "vanilla", "wheat",
            "alarm", "anchor", "armchair", "axe", "bag", "ball", "balloon", "basket", "bed", "bell",
            "belt", "bench", "bicycle", "blanket", "boat", "book", "bottle", "bowl", "box", "broom",
            "brush", "bucket", "button", "camera", "candle", "cap", "car", "card", "carpet", "chair",
            "clock", "cloud", "coat", "comb", "computer", "cup", "curtain", "desk", "door", "drum",
            "eraser", "fan", "feather", "fence", "flag", "flashlight", "flower", "fork", "glass", "glove",
            "hammer", "hat", "helmet", "hook", "house", "jar", "key", "kite", "knife", "ladder",
            "lamp", "leaf", "lock", "magnet", "map", "mirror", "mobile", "money", "nail", "needle",
            "notebook", "oven", "padlock", "pan", "paper", "pen", "pencil", "phone", "pillow", "plate",
            "pot", "purse", "radio", "ring", "rope", "ruler", "saddle", "saw", "scissors", "shelf",
            "shoe", "shovel", "soap", "sock", "spoon", "star", "table", "tent", "towel", "umbrella",
            "vase", "wallet", "watch", "wheel", "whistle", "window", "wire", "wood", "wool", "yarn",
            "zipper", "brick", "bridge", "cabin", "chain", "chest", "coin", "cork", "engine", "faucet",
            "frame", "furnace", "gate", "glass", "glove", "hammer", "handle", "hanger", "hinge", "iron",
            "beach", "bridge", "cave", "city", "cliff", "desert", "earth", "farm", "field", "forest",
            "garden", "hill", "island", "jungle", "lake", "mountain", "ocean", "park", "path", "pond",
            "river", "road", "rock", "sea", "sky", "soil", "stone", "stream", "sun", "tree",
            "valley", "village", "volcano", "waterfall", "world", "yard", "canyon", "campsite", "factory", "hospital",
            "hotel", "library", "market", "museum", "office", "school", "shop", "station", "theater", "university",
            "air", "bush", "coast", "continent", "country", "creek", "dust", "fire", "grass", "ground",
            "heat", "ice", "land", "moon", "mud", "nature", "peak", "planet", "rain", "sand",
            "shadow", "shell", "shore", "snow", "space", "star", "storm", "thunder", "tide", "wave",
            "angry", "beautiful", "big", "bitter", "black", "blue", "brave", "bright", "brown", "busy",
            "calm", "cheap", "clean", "clear", "clever", "cold", "cool", "dark", "deep", "dirty",
            "dry", "early", "easy", "empty", "fast", "fat", "fine", "flat", "fresh", "full",
            "funny", "glad", "good", "great", "green", "grey", "happy", "hard", "heavy", "high",
            "hot", "huge", "hungry", "kind", "large", "late", "light", "little", "long", "loud",
            "low", "lucky", "new", "nice", "old", "poor", "pretty", "quick", "quiet", "red",
            "rich", "rough", "round", "sad", "safe", "salty", "sharp", "short", "slow", "small",
            "smooth", "soft", "sour", "strong", "sweet", "tall", "thick", "thin", "tiny", "tough",
            "add", "agree", "allow", "answer", "appear", "arrive", "ask", "bake", "bath", "be",
            "beat", "become", "begin", "behave", "believe", "belong", "bend", "bet", "bid", "bite",
            "blow", "boil", "borrow", "break", "bring", "build", "burn", "buy", "call", "can",
            "care", "carry", "catch", "cause", "change", "charge", "check", "choose", "clean", "climb",
            "close", "come", "compare", "complain", "cook", "copy", "cost", "count", "cover", "cry",
            "cut", "dance", "deal", "decide", "deliver", "describe", "design", "destroy", "die", "dig",
            "do", "draw", "dream", "drink", "drive", "drop", "eat", "end", "enjoy", "enter",
            "exist", "expect", "explain", "fail", "fall", "fear", "feel"
    );

    private static class WordData {
        String word;
        long lastUsed;
        WordData(String word, long lastUsed) { this.word = word; this.lastUsed = lastUsed; }
    }

    /**
     * Get fresh items prioritized by those never seen or seen longest ago.
     */
    public List<String> getNextWords(int numberOfWords) {
        // 1. Get Mastered words (Exclude permanently)
        List<String> mastered = dbHelper.getAIHistory(username, historyKey);
        Set<String> masteredSet = new HashSet<String>();
        for (String w : mastered) { if (w != null) masteredSet.add(w.trim().toLowerCase()); }

        // 2. Get Usage history (Timestamps)
        Map<String, Long> usageMap = dbHelper.getUsageHistory(username, historyKey);

        // 3. Prepare the pool
        List<WordData> pool = new ArrayList<WordData>();
        for (String word : WORD_BANK) {
            String clean = word.trim().toLowerCase();
            if (!masteredSet.contains(clean)) {
                Long val = usageMap.get(clean);
                pool.add(new WordData(word, (val != null) ? val : 0));
            }
        }

        // 4. SMART SORT: Priorities never seen (0), then oldest timestamp
        Collections.sort(pool, new Comparator<WordData>() {
            @Override
            public int compare(WordData a, WordData b) {
                return (a.lastUsed < b.lastUsed) ? -1 : ((a.lastUsed == b.lastUsed) ? 0 : 1);
            }
        });

        // 5. Select batch and SHUFFLE slightly for variety
        int takeCount = Math.min(pool.size(), Math.max(numberOfWords * 2, 40));
        List<String> selection = new ArrayList<String>();
        for (int i = 0; i < takeCount; i++) {
            String w = pool.get(i).word;
            selection.add(w);
            // CRITICAL: Mark as seen immediately so it doesn't cycle back in the next session
            markWordAsSeen(w);
        }
        Collections.shuffle(selection);

        int limit = Math.min(numberOfWords, selection.size());
        return new ArrayList<String>(selection.subList(0, limit));
    }

    public int getTotalWordCount() {
        Set<String> distinct = new HashSet<String>();
        for (String w : WORD_BANK) {
            if (w != null && w.trim().length() > 0) distinct.add(w.trim().toLowerCase());
        }
        return distinct.size();
    }

    public int getCompletedWordCount() {
        Set<String> completed = new HashSet<String>();
        for (String w : dbHelper.getAIHistory(username, historyKey)) {
            if (w != null && w.trim().length() > 0) completed.add(w.trim().toLowerCase());
        }
        return completed.size();
    }

    public void markWordCompleted(String word) {
        if (word == null || word.trim().length() == 0) return;
        dbHelper.addToAIHistory(username, word.trim().toLowerCase(), historyKey);
    }

    public void markWordAsSeen(String word) {
        if (word == null || word.trim().length() == 0) return;
        dbHelper.markAsSeen(username, word.trim().toLowerCase(), historyKey);
    }

    public void clearProgress() {
        dbHelper.clearAIHistory(username, historyKey);
    }
}
