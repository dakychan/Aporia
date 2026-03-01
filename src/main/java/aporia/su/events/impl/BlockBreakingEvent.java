package aporia.su.events.impl;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import aporia.su.events.api.events.Event;

public record BlockBreakingEvent(BlockPos blockPos, Direction direction) implements Event {}
