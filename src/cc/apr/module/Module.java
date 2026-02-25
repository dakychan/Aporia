package cc.apr.module;

import java.util.ArrayList;
import java.util.List;
import cc.apr.event.impl.EventSystemImpl;
import cc.apr.event.impl.ModuleToggleEvent;
import cc.apr.ui.notify.Notify;

public abstract class Module {
   private final String name;
   private final String description;
   private final Module.Category category;
   private final int defaultBind;
   private boolean enabled;
   private final List<Module.Setting<?>> settings = new ArrayList<>();

   public Module(String name, String description, Module.Category category) {
      this(name, description, category, -1);
   }

   public Module(String name, String description, Module.Category category, int defaultBind) {
      this.name = name;
      this.description = description;
      this.category = category;
      this.defaultBind = defaultBind;
      this.enabled = false;
   }

   protected void addSetting(Module.Setting<?> setting) {
      this.settings.add(setting);
   }

   public List<Module.Setting<?>> getSettings() {
      return this.settings;
   }

   public void toggle() {
      this.toggle(false);
   }

   public void toggle(boolean fromGui) {
      this.setEnabled(!this.enabled, fromGui);
   }

   public void setEnabled(boolean enabled) {
      this.setEnabled(enabled, false);
   }

   public void setEnabled(boolean enabled, boolean fromGui) {
      if (this.enabled != enabled) {
         this.enabled = enabled;
         if (enabled) {
            this.onEnable();
         } else {
            this.onDisable();
         }

         EventSystemImpl.getInstance().fire(new ModuleToggleEvent(this, enabled));
         if (!fromGui) {
            Notify.Manager.getInstance().showNotification(this.name + (enabled ? " включен" : " выключен"), Notify.NotificationType.MODULE);
         }
      }
   }

   public abstract void onEnable();

   public abstract void onDisable();

   public abstract void onTick();

   public String getName() {
      return this.name;
   }

   public String getDescription() {
      return this.description;
   }

   public Module.Category getCategory() {
      return this.category;
   }

   public int getDefaultBind() {
      return this.defaultBind;
   }

   public boolean isEnabled() {
      return this.enabled;
   }

   public static class BooleanSetting extends Module.Setting<Boolean> {
      public BooleanSetting(String name, boolean defaultValue) {
         super(name, defaultValue);
      }
   }

   public static class C {
      public static final Module.Category COMBAT = Module.Category.COMBAT;
      public static final Module.Category MOVEMENT = Module.Category.MOVEMENT;
      public static final Module.Category VISUALS = Module.Category.VISUALS;
      public static final Module.Category PLAYER = Module.Category.PLAYER;
      public static final Module.Category MISC = Module.Category.MISC;
   }

   public static enum Category {
      COMBAT("Combat"),
      MOVEMENT("Movement"),
      VISUALS("Visuals"),
      PLAYER("Player"),
      MISC("Miscellaneous");

      private final String displayName;

      private Category(String displayName) {
         this.displayName = displayName;
      }

      public String getDisplayName() {
         return this.displayName;
      }
   }

   public static class ModeSetting extends Module.Setting<String> {
      private final String[] modes;

      public ModeSetting(String name, String defaultValue, String... modes) {
         super(name, defaultValue);
         this.modes = modes;
      }

      public String[] getModes() {
         return this.modes;
      }

      public void cycle() {
         String current = this.getValue();

         for (int i = 0; i < this.modes.length; i++) {
            if (this.modes[i].equals(current)) {
               this.setValue(this.modes[(i + 1) % this.modes.length]);
               return;
            }
         }

         if (this.modes.length > 0) {
            this.setValue(this.modes[0]);
         }
      }
   }

   public static class NumberSetting extends Module.Setting<Double> {
      private final double min;
      private final double max;
      private final double step;

      public NumberSetting(String name, double defaultValue, double min, double max, double step) {
         super(name, defaultValue);
         this.min = min;
         this.max = max;
         this.step = step;
      }

      public void setValue(Double value) {
         super.setValue(Math.max(this.min, Math.min(this.max, value)));
      }

      public double getMin() {
         return this.min;
      }

      public double getMax() {
         return this.max;
      }

      public double getStep() {
         return this.step;
      }
   }

   public abstract static class Setting<T> {
      private final String name;
      private T value;

      public Setting(String name, T defaultValue) {
         this.name = name;
         this.value = defaultValue;
      }

      public String getName() {
         return this.name;
      }

      public T getValue() {
         return this.value;
      }

      public void setValue(T value) {
         this.value = value;
      }
   }

   public static class StringSetting extends Module.Setting<String> {
      public StringSetting(String name, String defaultValue) {
         super(name, defaultValue);
      }
   }
}
