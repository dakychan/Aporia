package zenith.zov.base.events.impl.input;


import com.darkmagician6.eventapi.events.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;

import static zenith.zov.utility.interfaces.IMinecraft.mc;


@Getter
@AllArgsConstructor
public final class EventKey implements Event {
    private final int action;
    private final int keyCode;
    public boolean is(int keyCode) {return keyCode == this.keyCode;}
    public boolean isKeyDown(int key) {
        return isKeyDown(key, mc.currentScreen == null);
    }

    public boolean isKeyDown(int key, boolean screen) {
        return this.keyCode == key && action == 1 && screen;
    }

    public boolean isKeyReleased(int key) {
        return isKeyReleased(key, mc.currentScreen == null);
    }

    public boolean isKeyReleased(int key, boolean screen) {
        return this.keyCode == key && action == 0 && screen;
    }
}