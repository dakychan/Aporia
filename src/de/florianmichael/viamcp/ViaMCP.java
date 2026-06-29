package de.florianmichael.viamcp;

import de.florianmichael.vialoadingbase.ViaLoadingBase;

import java.io.File;

public class ViaMCP {
    public final static int NATIVE_VERSION = 774;
    public static ViaMCP INSTANCE;

    public static void create() {
        INSTANCE = new ViaMCP();
    }

    public ViaMCP() {
        ViaLoadingBase.ViaLoadingBaseBuilder.create()
                .runDirectory(new File("ViaMCP"))
                .nativeVersion(NATIVE_VERSION)
                .build();
    }
}
