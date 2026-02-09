package zenith.zov.client.modules.api.setting;


import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;

import java.util.function.Supplier;

@Getter
public abstract class Setting   {
    protected final String name;


    @Setter
    protected Supplier<Boolean> visible;

    public Setting(String name){
        this.name = name;
        this.setVisible(() -> true);
    }
    public abstract void safe( JsonObject propertiesObject);

    public abstract void load( JsonObject propertiesObject);
    public boolean isVisible() {
        return visible.get();
    }
}

