import net.minecraft.client.main.Main;

private static final String index = "32";

void main(String[] args) throws IOException {
    List<String> launchArgs = new ArrayList<>(Arrays.asList(
            "--version", "26.2-snapshot-2",
            "--accessToken", "0",
            "--assetsDir", findAssets(),
            "--username", "kumeka",
            "--assetIndex", index,
            "--gameDir", gameDir()
    ));
    System.out.println("Arguments: " + launchArgs);
    launchArgs.addAll(Arrays.asList(args));
    Main.main(launchArgs.toArray(String[]::new));
}

private static String gameDir() throws IOException {
    File run = new File("run");
    return (run.exists() && run.isDirectory() ? run : new File(".")).getCanonicalPath();
}

private static String findAssets() throws IOException {
    String env = System.getenv("assetDirectory");
    if (env != null && hasIndex(new File(env))) return new File(env).getCanonicalPath();

    // .minecraft/assets has the full objects/ structure
    File mcAssets = new File(getMCDir(), "assets");
    if (hasIndex(mcAssets)) return mcAssets.getCanonicalPath();

    // Try relative to project root (works when CWD is run/)
    File root = new File(".").getCanonicalFile().getParentFile();
    if (root != null) {
        File srcAssets = new File(root, "src/resources/assets");
        if (hasIndex(srcAssets)) return srcAssets.getCanonicalPath();
    }

    // Try relative to CWD
    if (hasIndex(new File("src/resources/assets"))) return new File("src/resources/assets").getCanonicalPath();
    if (hasIndex(new File("run/assets"))) return new File("run/assets").getCanonicalPath();

    File cur = new File(".").getCanonicalFile();
    for (File d = cur; d != null; d = d.getParentFile()) {
        if ("versions".equals(d.getName())) {
            File build = new File(d.getParentFile(), "build/assets");
            if (hasIndex(build)) return build.getCanonicalPath();
            break;
        }
    }

    File localBuild = new File(cur, "build/assets");
    if (hasIndex(localBuild)) return localBuild.getCanonicalPath();

    System.err.println("Warning: Asset index " + index + " not found. Using fallback.");
    return new File("assets").getCanonicalPath();
}

private static boolean hasIndex(File root) {
    return root.isDirectory() && new File(root, "indexes/" + index + ".json").isFile();
}

private static File getMCDir() {
    return switch (OS.getCurrent()) {
        case OSX -> new File(System.getProperty("user.home") + "/Library/Application Support/minecraft");
        case WINDOWS -> {
            String appdata = System.getenv("APPDATA");
            yield new File(appdata != null ? appdata : System.getProperty("user.home"), ".minecraft");
        }
        default -> new File(System.getProperty("user.home"), ".minecraft");
    };
}

private enum OS {
    WINDOWS("win"), LINUX("linux", "unix"), OSX("osx", "mac"), UNKNOWN;

    private final String[] keys;

    OS(String... keys) {
        this.keys = keys;
    }

    static OS getCurrent() {
        String prop = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
        for (OS os : OS.values())
            for (String key : os.keys)
                if (prop.contains(key)) return os;
        return UNKNOWN;
    }
}
