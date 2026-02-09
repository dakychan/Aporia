package zenith.zov.base.font;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Fonts {

    // voice:.idea/1746466857699.wav

    public final MsdfFont BOLD = MsdfFont.builder().atlas("bold").data("bold").build();
    public final MsdfFont MEDIUM = MsdfFont.builder().atlas("medium").data("medium").build();

    public final MsdfFont REGULAR = MsdfFont.builder().atlas("regular").data("regular").build();
    public final MsdfFont SEMIBOLD = MsdfFont.builder().atlas("semibold").data("semibold").build();
    public final MsdfFont ROUND_BOLD = MsdfFont.builder().atlas("roundbold").data("roundbold").build();
    public final MsdfFont ICONS = MsdfFont.builder().atlas("icons").data("icons").build();

}