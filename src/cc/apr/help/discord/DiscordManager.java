package cc.apr.help.discord;

import aporia.cc.Logger;
import aporia.cc.user.UserData;
import aporia.cc.user.UserData.UserDataClass;
import dev.firstdark.rpc.DiscordRpc;
import dev.firstdark.rpc.enums.ErrorCode;
import dev.firstdark.rpc.handlers.DiscordEventHandler;
import dev.firstdark.rpc.models.DiscordJoinRequest;
import dev.firstdark.rpc.models.DiscordRichPresence;
import dev.firstdark.rpc.models.User;
import dev.firstdark.rpc.models.DiscordRichPresence.RPCButton;
import lombok.Generated;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.resources.Identifier;

public class DiscordManager {
   private final DiscordManager.DiscordDaemonThread discordDaemonThread = new DiscordManager.DiscordDaemonThread();
   private boolean running = false;
   private DiscordManager.DiscordInfo info = new DiscordManager.DiscordInfo("Unknown", "", "");
   private Identifier avatarId;
   private DiscordRpc discordRpc;
   UserDataClass userData = UserData.getUserData();
   private long sessionStartTime = 0L;

   public void init() {
      try {
         this.sessionStartTime = System.currentTimeMillis() / 1000L;
         this.discordRpc = new DiscordRpc();
         DiscordEventHandler handler = new DiscordEventHandler() {
            public void ready(User user) {
               cc.aprAporia.getDiscordManager()
                  .setInfo(
                     new DiscordManager.DiscordInfo(
                        user.getUsername(), "https://cdn.discordapp.com/avatars/" + user.getUserId() + "/" + user.getAvatar() + ".png", user.getUserId()
                     )
                  );
               DiscordManager.this.updatePresence("В меню");
            }

            public void disconnected(ErrorCode errorCode, String message) {
               Logger.INSTANCE.info("Discord RPC disconnected: " + errorCode + " - " + message);
            }

            public void errored(ErrorCode errorCode, String message) {
               Logger.INSTANCE.error("Discord RPC error: " + errorCode + " - " + message, null);
            }

            public void joinGame(String joinSecret) {
            }

            public void spectateGame(String spectateSecret) {
            }

            public void joinRequest(DiscordJoinRequest joinRequest) {
            }
         };
         this.discordRpc.init("1471901603287142421", handler, false);
         this.running = true;
         this.discordDaemonThread.start();
      } catch (Exception var2) {
         Logger.INSTANCE.error("Failed to initialize Discord RPC: " + var2.getMessage(), var2);
      }
   }

   private String detectGameState() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level == null) {
         return "В меню";
      } else if (mc.isLocalServer()) {
         return "Одиночная игра";
      } else {
         ServerData serverData = mc.getCurrentServer();
         return serverData != null ? serverData.ip : "В игре";
      }
   }

   public void updatePresence(String state) {
      if (this.running && this.discordRpc != null) {
         DiscordRichPresence presence = DiscordRichPresence.builder()
            .startTimestamp(this.sessionStartTime)
            .state(state)
            .details("UUID : " + this.userData.getUuid())
            .largeImageKey("aporia")
            .button(RPCButton.of("Discord", "https://discord.gg/TPdfGKs7B3"))
            .build();
         this.discordRpc.updatePresence(presence);
      }
   }

   public void updateGameState() {
      if (this.running && this.discordRpc != null) {
         String state = this.detectGameState();
         this.updatePresence(state);
      }
   }

   public void stopRPC() {
      if (this.discordRpc != null) {
         this.discordRpc.shutdown();
      }

      this.running = false;
   }

   public boolean isRunning() {
      return this.running;
   }

   @Generated
   public void setRunning(boolean running) {
      this.running = running;
   }

   @Generated
   public void setInfo(DiscordManager.DiscordInfo info) {
      this.info = info;
   }

   @Generated
   public void setAvatarId(Identifier avatarId) {
      this.avatarId = avatarId;
   }

   @Generated
   public void setDiscordRpc(DiscordRpc discordRpc) {
      this.discordRpc = discordRpc;
   }

   @Generated
   public void setUserData(UserDataClass userData) {
      this.userData = userData;
   }

   @Generated
   public void setSessionStartTime(long sessionStartTime) {
      this.sessionStartTime = sessionStartTime;
   }

   @Generated
   public DiscordManager.DiscordDaemonThread getDiscordDaemonThread() {
      return this.discordDaemonThread;
   }

   @Generated
   public DiscordManager.DiscordInfo getInfo() {
      return this.info;
   }

   @Generated
   public Identifier getAvatarId() {
      return this.avatarId;
   }

   @Generated
   public DiscordRpc getDiscordRpc() {
      return this.discordRpc;
   }

   @Generated
   public UserDataClass getUserData() {
      return this.userData;
   }

   @Generated
   public long getSessionStartTime() {
      return this.sessionStartTime;
   }

   private class DiscordDaemonThread extends Thread {
      @Override
      public void run() {
         this.setName("Discord-RPC");

         try {
            for (; cc.aprAporia.getDiscordManager().isRunning(); Thread.sleep(15000L)) {
               if (DiscordManager.this.discordRpc != null) {
                  DiscordManager.this.discordRpc.runCallbacks();
               }
            }
         } catch (Exception var2) {
            Logger.INSTANCE.error("Discord RPC thread error: " + var2.getMessage(), var2);
            DiscordManager.this.stopRPC();
         }

         super.run();
      }
   }

   public record DiscordInfo(String userName, String avatarUrl, String userId) {
   }
}
