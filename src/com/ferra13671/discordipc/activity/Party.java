package com.ferra13671.discordipc.activity;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * The team the user is in.
 *
 * @param id team id.
 * @param currentSize current number of people in the party.
 * @param maxSize maximum number of people in the party.
 */
public record Party(String id, int currentSize, int maxSize) {

    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("id", this.id);
        JsonArray sizeArray = new JsonArray();
        sizeArray.add(this.currentSize);
        sizeArray.add(this.maxSize);
        jsonObject.add("size", sizeArray);

        return jsonObject;
    }
}
