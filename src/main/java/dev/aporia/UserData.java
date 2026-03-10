package dev.aporia;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Random;

public class UserData {
    private static final String CONFIG_DIR = "sorray";
    private static final String USER_DATA_FILE = "user.dat";

    public static String getSystemUsername() {
        String username = null;
        
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("linux")) {
            username = getLinuxUsername();
        } else if (os.contains("win")) {
            username = getWindowsUsername();
        } else if (os.contains("mac")) {
            username = getMacUsername();
        }

        if (username == null || username.isBlank()) {
            return generateDefaultUsername();
        }
        
        String lowerUsername = username.toLowerCase();
        if (List.of("root", "admin", "administrator", "sudo").contains(lowerUsername)) {
            return generateDefaultUsername();
        }
        
        return username;
    }

    private static String getLinuxUsername() {
        try {
            Process process = new ProcessBuilder("whoami").start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line = reader.readLine();
                return line != null ? line.trim() : null;
            }
        } catch (Exception e) {
            return System.getProperty("user.name");
        }
    }

    private static String getWindowsUsername() {
        try {
            Process process = new ProcessBuilder("whoami").start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
                String line = reader.readLine();
                if (line != null) {
                    String trimmed = line.trim();
                    int backslashIndex = trimmed.indexOf('\\');
                    if (backslashIndex >= 0) {
                        return trimmed.substring(backslashIndex + 1);
                    }
                    return trimmed;
                }
                return null;
            }
        } catch (Exception e) {
            return System.getProperty("user.name");
        }
    }

    private static String getMacUsername() {
        return System.getProperty("user.name");
    }

    private static String generateDefaultUsername() {
        Random random = new Random();
        return "User" + random.nextInt(1000, 9999);
    }

    public static UserRole getUserRole(String username) {
        String lowerUsername = username.toLowerCase();
        switch (lowerUsername) {
            case "daky_chan":
            case "dusky2":
            case "kotay":
                return UserRole.DEVELOPER;
            default:
                return UserRole.USER;
        }
    }

    public static String getUserUUID(String username) {
        return UserGenerator.generateCompressedNumericUUID(username);
    }

    public static String getHardwareId() {
        String systemId = UserGenerator.generateSystemHardwareId();
        if (systemId != null && !systemId.isBlank()) {
            return systemId;
        }
        return UserGenerator.generateHardwareId();
    }

    public static UserDataClass getUserData() {
        String username = getSystemUsername();
        String uuid = getUserUUID(username);
        UserRole role = getUserRole(username);
        String hardwareId = getHardwareId();

        return new UserDataClass(username, uuid, role, hardwareId);
    }

    public static class UserDataClass {
        private final String username;
        private final String uuid;
        private final UserRole role;
        private final String hardwareId;

        public UserDataClass(String username, String uuid, UserRole role, String hardwareId) {
            this.username = username;
            this.uuid = uuid;
            this.role = role;
            this.hardwareId = hardwareId;
        }

        public String getUsername() {
            return username;
        }

        public String getUuid() {
            return uuid;
        }

        public UserRole getRole() {
            return role;
        }

        public String getHardwareId() {
            return hardwareId;
        }

        @Override
        public String toString() {
            return "UserDataClass{" +
                    "username='" + username + '\'' +
                    ", uuid='" + uuid + '\'' +
                    ", role=" + role +
                    ", hardwareId='" + hardwareId + '\'' +
                    '}';
        }
    }
    public enum UserRole {
        USER,
        DEVELOPER,
        ADMIN,
        CONTRIBUTOR
    }
}
