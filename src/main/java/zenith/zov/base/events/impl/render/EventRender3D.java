package zenith.zov.base.events.impl.render;


import com.darkmagician6.eventapi.events.Event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
;


@Getter
@AllArgsConstructor
public final class EventRender3D implements Event {

    private final MatrixStack matrix;
    private final float partialTicks;
}
