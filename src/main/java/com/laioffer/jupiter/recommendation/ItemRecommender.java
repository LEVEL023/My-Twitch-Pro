package com.laioffer.jupiter.recommendation;

import com.laioffer.jupiter.db.MySQLConnection;
import com.laioffer.jupiter.entity.Game;
import com.laioffer.jupiter.entity.Item;
import com.laioffer.jupiter.entity.ItemType;
import com.laioffer.jupiter.external.TwitchClient;
import com.laioffer.jupiter.external.TwitchException;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ItemRecommender {

    private static final int DEFAULT_GAME_LIMIT = 3;
    private static final int DEFAULT_PER_GAME_RECOMMENDATION_LIMIT = 10;
    private static final int DEFAULT_TOTAL_RECOMMENDATION_LIMIT = 20;

    private List<Item> recommendByTopGames(ItemType type, List<Game> topGames) throws RecommendationException {
        List<Item> recommendedItems = new ArrayList<>();
        TwitchClient client = new TwitchClient();
        topGames = topGames.subList(0, DEFAULT_GAME_LIMIT);
        outerLoop:
        for (Game game : topGames) {
            List<Item> items = client.searchByType(game.getId(), type, DEFAULT_PER_GAME_RECOMMENDATION_LIMIT);
            for (Item item : items) {
                if (recommendedItems.size() == DEFAULT_TOTAL_RECOMMENDATION_LIMIT) {
                     break outerLoop;
                }
                recommendedItems.add(item);
            }
        }
        return recommendedItems;
    }

    private List<Item> recommendByFavoriteHistory(ItemType type, List<String> favoriteGameIds, Set<String> favoriteItemIds) {

        Map<String, Long> favoriteGameIdByCount = favoriteGameIds.parallelStream().collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        // Function.identity() means group by game_id, function(x){return x}
        // Sort by count
        List<Map.Entry<String, Long>> sortedFavoriteGameIdListByCount = new ArrayList<>(
                favoriteGameIdByCount.entrySet());
        sortedFavoriteGameIdListByCount.sort((Map.Entry<String, Long> e1, Map.Entry<String, Long> e2) -> Long
                .compare(e2.getValue(), e1.getValue()));

        if (sortedFavoriteGameIdListByCount.size() > DEFAULT_GAME_LIMIT) {
            sortedFavoriteGameIdListByCount = sortedFavoriteGameIdListByCount.subList(0, DEFAULT_GAME_LIMIT);
        }
        // Search
        List<Item> recommendedItems = new ArrayList<>();
        TwitchClient client = new TwitchClient();
        outerLoop:
        for (Map.Entry<String, Long> favoriteGameId: sortedFavoriteGameIdListByCount) {
            List<Item> items = client.searchByType(favoriteGameId.getKey(), type, DEFAULT_PER_GAME_RECOMMENDATION_LIMIT);
            for (Item item : items) {
                if (recommendedItems.size() == DEFAULT_TOTAL_RECOMMENDATION_LIMIT) {
                    break outerLoop;
                }
                if (!favoriteItemIds.contains(item.getId())) {
                    recommendedItems.add(item);
                }
            }
        }
        return recommendedItems;
    }

    public Map<String, List<Item>> recommendItemsByUser(String userId) throws RecommendationException {
        Map<String, List<Item>> recommendedItemMap = new HashMap<>();
        MySQLConnection connection = new MySQLConnection();
        Set<String> favoriteItemIds = connection.getFavoriteItemIds(userId);
        Map<String, List<String>> favoriteGameIds = connection.getFavoriteGameIds(favoriteItemIds);
        connection.close();
        for (Map.Entry<String, List<String>> entry : favoriteGameIds.entrySet()) {
            if (entry.getValue().size() == 0) {
                TwitchClient client = new TwitchClient();
                List<Game> topGames;
                try {
                    topGames = client.topGames(DEFAULT_GAME_LIMIT);
                } catch (TwitchException e) {
                    throw new RecommendationException("Failed to get game data for recommendation");
                }
                recommendedItemMap.put(entry.getKey(), recommendByTopGames(ItemType.valueOf(entry.getKey()), topGames));
            } else {
                recommendedItemMap.put(entry.getKey(), recommendByFavoriteHistory(ItemType.valueOf(entry.getKey()), entry.getValue(), favoriteItemIds));
            }
        }
        return recommendedItemMap;
    }

    public Map<String, List<Item>> recommendItemsByDefault() throws RecommendationException {
        Map<String, List<Item>> recommendedItemMap = new HashMap<>();
        TwitchClient client = new TwitchClient();
        List<Game> topGames;
        try {
            topGames = client.topGames(DEFAULT_GAME_LIMIT);
        } catch (TwitchException e) {
            throw new RecommendationException("Failed to get game data for recommendation");
        }

        for (ItemType type : ItemType.values()) {
            recommendedItemMap.put(type.toString(), recommendByTopGames(type, topGames));
        }
        return recommendedItemMap;
    }
}
