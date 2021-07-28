package com.laioffer.jupiter.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class FavoriteRequestBody {
    private final Item favoriteItem;

    @JsonCreator // because we are not using Builder pattern here
    public FavoriteRequestBody(@JsonProperty("favorite") Item favoriteItem) { // single direction: only from Json String to FavoriteRequestBody object
        this.favoriteItem = favoriteItem;
    }

    public Item getFavoriteItem() {
        return favoriteItem;
    }
}
