package aporia.su.events.impl;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import aporia.su.events.api.events.Event;

public class Event3D implements Event {

    public MatrixStack stack;
    public VertexConsumerProvider buffer;

   public Event3D(MatrixStack stack, VertexConsumerProvider buffer) {
       this.stack = stack;
       this.buffer = buffer;
   }

}