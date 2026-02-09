package zenith.zov.utility.mixin.client;

import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Scoreboard.class)
public abstract class ScoreboarMixin {
    @Shadow public abstract @Nullable Team getScoreHolderTeam(String scoreHolderName);

    @Inject(method = "removeScoreHolderFromTeam",at = @At(value = "HEAD"), cancellable = true)
    public void remove(String scoreHolderName, Team team, CallbackInfo ci){
        if (this.getScoreHolderTeam(scoreHolderName) != team) { //REALLyworld донатная помойка ебанная в рот ее ебал
            ci.cancel();
        }

    }
}
