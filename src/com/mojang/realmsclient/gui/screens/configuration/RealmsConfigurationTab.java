package com.mojang.realmsclient.gui.screens.configuration;

import com.mojang.realmsclient.dto.RealmsServer;




public interface RealmsConfigurationTab {
   void updateData(RealmsServer var1);

   default void onSelected(RealmsServer p_405812_) {
   }

   default void onDeselected(RealmsServer p_406570_) {
   }
}
