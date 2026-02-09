package zenith.zov.client.modules.api;

import lombok.Getter;

@Getter
public enum Category {
    COMBAT("Combat","0"),
    MOVEMENT("Movement", "1"),
    PLAYER("Player", "2"),
    RENDER("Render", "3"),
    MISC("Misc", "4"),
    THEMES("Themes", "G");
    @Getter
    private final String name;
    private final String icon;





    Category(String name, String icon) {
        this.name = name;
        this.icon = icon;


    }

}
