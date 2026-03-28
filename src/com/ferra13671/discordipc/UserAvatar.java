package com.ferra13671.discordipc;

import java.io.InputStream;

public record UserAvatar(InputStream inputStream, AvatarType avatarType) {
}
