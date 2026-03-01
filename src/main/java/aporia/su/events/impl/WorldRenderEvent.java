package aporia.su.events.impl;

import lombok.*;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.util.math.MatrixStack;
import aporia.su.events.api.events.Event;

@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@Getter
public class WorldRenderEvent implements Event {
    MatrixStack stack;
    float partialTicks;
}
