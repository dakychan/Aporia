package net.minecraft.client.data.models.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;




public class DelegatedModel implements ModelInstance {
   private final Identifier parent;

   public DelegatedModel(Identifier p_453243_) {
      this.parent = p_453243_;
   }

   public JsonElement get() {
      JsonObject jsonobject = new JsonObject();
      jsonobject.addProperty("parent", this.parent.toString());
      return jsonobject;
   }
}
