package com.ferra13671.discordipc;

import com.ferra13671.discordipc.activity.ActivityInfo;
import com.ferra13671.discordipc.connection.Connection;
import com.ferra13671.discordipc.connection.packet.S2CPacket;
import com.ferra13671.discordipc.connection.packet.impl.c2s.HandsnakePacket;
import com.ferra13671.discordipc.connection.packet.impl.c2s.SetActivityPacket;
import com.ferra13671.discordipc.connection.packet.impl.s2c.CloseConnectionPacket;
import com.ferra13671.discordipc.connection.packet.impl.s2c.DispatchPacket;
import com.ferra13671.discordipc.connection.packet.impl.s2c.ErrorPacket;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.lang.management.ManagementFactory;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Discord IPC Main Class
 */
public class DiscordIPC {
    private Connection connection;
    @NonNull
    @Setter
    private Runnable onReady = () -> {};
    @NonNull
    @Setter
    private BiConsumer<Integer, String> onError = (code, message) -> System.err.println("Discord IPC error " + code + " with message: " + message);
    private final OnChangeHolder<ActivityInfo> activityInfo = new OnChangeHolder<>(
            new ActivityInfo(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    System.currentTimeMillis() / 1000,
                    null,
                    null,
                    null
            ),
            info -> updateActivity()
    );
    /**
     * Whether it is currently possible to send activity information or not.
     */
    @Getter
    private boolean dispatch = false;
    /**
     * -- GETTER --
     *  Returns information about the Discord account or null if there is no connection to Discord at the moment.
     */
    @Getter
    private IPCUser user;

    /**
     * Starts an RPC with the specified application ID.
     *
     * @param appId application ID.
     * @return whether a connection was created with the local Discord application or not.
     */
    public boolean start(long appId) {
        this.connection = Connection.open(this::onPacket);
        if (this.connection == null)
            return false;

        this.connection.write(new HandsnakePacket(appId, 1));

        return true;
    }

    /**
     * Returns whether the connection to the local discord is valid or not.
     *
     * @return whether the connection to the local discord is valid or not.
     */
    public boolean isConnected() {
        return this.connection != null;
    }

    /**
     * Stops the current thread until the connection is waiting for packets to be sent.
     */
    public void waitDispatch() {
        while (!this.dispatch)
            Thread.yield();
    }

    /**
     * Stops RPC.
     */
    public void stop() {
        if (this.connection != null) {
            this.connection.close();

            this.dispatch = false;
            this.connection = null;
            this.user = null;
        }
    }

    /**
     * Invokes update activity information.
     *
     * @param updateConsumer the action called when activity information is updated.
     */
    public void updateActivity(Function<ActivityInfo, ActivityInfo> updateConsumer) {
        this.activityInfo.setValue(updateConsumer.apply(this.activityInfo.getValue()));
    }

    /**
     * Sends activity information to the local Discord.
     * Called automatically when activity information changes.
     */
    public void updateActivity() {
        if (isDispatch())
            this.connection.write(new SetActivityPacket(getPID(), this.activityInfo.getValue()));
    }

    private void onPacket(S2CPacket p) {
        if (p instanceof CloseConnectionPacket packet)
            onCloseConnection(packet);

        if (p instanceof ErrorPacket packet)
            onErrorPacket(packet);

        if (p instanceof DispatchPacket packet)
            onDispatch(packet);
    }

    private void onCloseConnection(CloseConnectionPacket packet) {
        this.onError.accept(packet.code(), packet.message());

        stop();
    }

    private void onErrorPacket(ErrorPacket packet) {
        this.onError.accept(packet.code(), packet.message());
    }

    private void onDispatch(DispatchPacket packet) {
        this.dispatch = true;
        this.user = packet.discordUser();

        this.onReady.run();

        updateActivity();
    }

    /**
     * Returns the PID of the program.
     *
     * @return PID of the program.
     */
    private static int getPID() {
        String pr = ManagementFactory.getRuntimeMXBean().getName();
        return Integer.parseInt(pr.substring(0, pr.indexOf('@')));
    }
}
