package de.maxhenkel.voicechat.mixin;

import io.netty.channel.Channel;

public interface ConnectionAccessor {
    public Channel getChannel();
}
