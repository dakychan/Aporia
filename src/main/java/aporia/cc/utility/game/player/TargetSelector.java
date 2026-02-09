package aporia.cc.utility.game.player;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import aporia.cc.Aporia;
import aporia.cc.client.modules.impl.combat.AntiBot;
import aporia.cc.utility.interfaces.IClient;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TargetSelector implements IClient {
    final PointFinder pointFinder = new PointFinder();
    LivingEntity currentTarget;
    Stream<LivingEntity> potentialTargets;

    public TargetSelector() {
        this.currentTarget = null;
    }

    public void lockTarget(LivingEntity target) {
        if (this.currentTarget == null) {
            this.currentTarget = target;
        }
    }

    public void releaseTarget() {
        this.currentTarget = null;
    }

    public void validateTarget(Predicate<LivingEntity> predicate) {
        findFirstMatch(predicate).ifPresent(this::lockTarget);

        if (this.currentTarget != null && !predicate.test(this.currentTarget)) {
            releaseTarget();
        }
    }

    public void searchTargets(Iterable<Entity> entities, float maxDistance,  boolean ignoreWalls) {
        if (currentTarget != null && (!pointFinder.hasValidPoint(currentTarget, maxDistance, ignoreWalls))) {
            releaseTarget();
        }

        this.potentialTargets = createStreamFromEntities(entities, maxDistance, ignoreWalls);
    }


    private Stream<LivingEntity> createStreamFromEntities(Iterable<Entity> entities, float maxDistance,  boolean ignoreWalls) {
        return StreamSupport.stream(entities.spliterator(), false)
                .filter(LivingEntity.class::isInstance)
                .map(LivingEntity.class::cast)
                .filter(entity -> pointFinder.hasValidPoint(entity, maxDistance, ignoreWalls) )
                .sorted(Comparator.comparingDouble(entity -> entity.distanceTo(mc.player)));
    }

    private Optional<LivingEntity> findFirstMatch(Predicate<LivingEntity> predicate) {
        return this.potentialTargets.filter(predicate).findFirst();
    }

    @RequiredArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    public static class EntityFilter {
        List<String> targetSettings;

        public boolean isValid(LivingEntity entity) {
            if (isLocalPlayer(entity)) return false;
            if (isInvalidHealth(entity)) return false;
            if (isBotPlayer(entity)) return false;

            return isValidEntityType(entity);
        }

        private boolean isLocalPlayer(LivingEntity entity) {
            return entity == mc.player;
        }

        private boolean isInvalidHealth(LivingEntity entity) {
            return !entity.isAlive() || entity.getHealth() <= 0;
        }

        private boolean isBotPlayer(LivingEntity entity) {
            return entity instanceof PlayerEntity player && AntiBot.INSTANCE.isBot(player);
        }

        private boolean isNakedPlayer(LivingEntity entity) {
            return entity.isPlayer();
        }

        private boolean isValidEntityType(LivingEntity entity) {

            if (entity instanceof PlayerEntity player) {
                if ( Aporia.getInstance().getFriendManager().isFriend(player.getGameProfile().getName())) {
                    return false;
                }
                return targetSettings.contains("Игроков");
            }
            if (entity instanceof MobEntity) {
                return targetSettings.contains("Враждебных");
            }
            if (entity instanceof AnimalEntity) {
                return targetSettings.contains("Мирных");
            }
            return !(entity instanceof ArmorStandEntity);
        }
    }
}

