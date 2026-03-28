package com.ferra13671.discordipc.activity;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.With;

/**
 * Information about activity displayed in Discord.
 *
 * @param details description of the activity.
 * @param state the state of the activity (for example, a user action, or the id of a command if you created one).
 * @param largeImage path to the large image (image id in the application assets/link to the image).
 * @param largeText text displayed when hovering over a large image.
 * @param smallImage path to the small image (image id in the application assets/link to the image).
 * @param smallText text displayed when hovering over a small image.
 * @param startTime activity start time.
 * @param party team.
 * @param button1 first button displayed below the activity.
 * @param button2 second button displayed below the activity.
 */
@With
public record ActivityInfo(String details, String state, String largeImage, String largeText, String smallImage, String smallText, Long startTime, Party party, Button button1, Button button2) {

    /**
     * Converts activity information into a json object.
     *
     * @return json object storing activity information.
     */
    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();

        if (this.details != null)
            jsonObject.addProperty("details", this.details);
        if (this.state != null)
            jsonObject.addProperty("state", this.state);

        jsonObject.add("assets", getAssetsObject());
        jsonObject.add("timestamps", getTimestampsObject());

        if (this.party != null)
            jsonObject.add("party", this.party.toJson());

        if (this.button1 != null || this.button2 != null)
            jsonObject.add("buttons", getButtonsArray());

        jsonObject.addProperty("instance", false);

        return jsonObject;
    }

    private JsonObject getAssetsObject() {
        JsonObject jsonObject = new JsonObject();

        if (this.largeImage != null)
            jsonObject.addProperty("large_image", this.largeImage);
        if (this.largeText != null)
            jsonObject.addProperty("large_text", this.largeText);
        if (this.smallImage != null)
            jsonObject.addProperty("small_image", this.smallImage);
        if (this.smallText != null)
            jsonObject.addProperty("small_text", this.smallText);

        return jsonObject;
    }

    private JsonObject getTimestampsObject() {
        JsonObject jsonObject = new JsonObject();

        if (this.startTime != null)
            jsonObject.addProperty("start", this.startTime);

        return jsonObject;
    }

    private JsonArray getButtonsArray() {
        JsonArray jsonArray = new JsonArray();

        if (this.button1 != null)
            jsonArray.add(this.button1.toJson());
        if (this.button2 != null)
            jsonArray.add(this.button2.toJson());

        return jsonArray;
    }
}
