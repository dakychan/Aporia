package com.ferra13671.discordipc;

import com.google.gson.JsonObject;

import java.net.URI;

public record IPCUser(String id, String username, String discriminator, String avatar, boolean bot, String flags, int premium_type) {

    /**
     * Returns the InputStream and the user's avatar type, or null if an error occurred.
     *
     * @return InputStream and the user's avatar type, or null if an error occurred.
     */
    public UserAvatar getAvatarImage() {
        try {
            if (this.avatar.startsWith("a_"))
                return new UserAvatar(new URI(String.format("https://cdn.discordapp.com/avatars/%s/%s.gif", this.id, this.avatar)).toURL().openStream(), AvatarType.Gif);
            else
                return new UserAvatar(new URI(String.format("https://cdn.discordapp.com/avatars/%s/%s.png", this.id, this.avatar)).toURL().openStream(), AvatarType.Image);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static IPCUser fromJson(JsonObject jsonObject) {
        return new IPCUser(
                jsonObject.get("id").getAsString(),
                jsonObject.get("username").getAsString(),
                jsonObject.get("discriminator").getAsString(),
                jsonObject.get("avatar").getAsString(),
                jsonObject.get("bot").getAsBoolean(),
                jsonObject.get("flags").getAsString(),
                jsonObject.get("premium_type").getAsInt()
        );
    }
}
