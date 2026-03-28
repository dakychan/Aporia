package com.ferra13671.discordipc.activity;

import com.google.gson.JsonObject;

/**
 * A button displayed below the activity.
 *
 * @param text button text.
 * @param url the link the user will be redirected to when they click the button.
 */
public record Button(String text, String url) {

    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("label", this.text);
        jsonObject.addProperty("url", this.url);

        return jsonObject;
    }
}
