package so.aporia.utils.user.render.ui.mainmenu

import com.mojang.serialization.Lifecycle
import net.minecraft.client.ClientRecipeBook
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.multiplayer.ClientPacketListener
import net.minecraft.client.multiplayer.CommonListenerCookie
import net.minecraft.client.multiplayer.LevelLoadTracker
import net.minecraft.client.player.LocalPlayer
import net.minecraft.client.telemetry.TelemetryEventSender
import net.minecraft.client.telemetry.WorldSessionTelemetryManager
import net.minecraft.core.BlockPos
import net.minecraft.core.Holder
import net.minecraft.core.HolderSet
import net.minecraft.core.Registry
import net.minecraft.core.RegistryAccess
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.network.Connection
import net.minecraft.network.protocol.PacketFlow
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.server.ServerLinks
import net.minecraft.stats.StatsCounter
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Input
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.biome.BiomeGenerationSettings
import net.minecraft.world.level.biome.BiomeSpecialEffects
import net.minecraft.world.level.biome.MobSpawnSettings
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.*
import net.minecraft.world.level.chunk.status.ChunkStatus
import net.minecraft.world.level.dimension.DimensionType
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import java.util.function.BooleanSupplier
import java.util.UUID

class SceneWorld {

    private val mc = Minecraft.getInstance()
    lateinit var level: ClientLevel
    lateinit var cameraEntity: Entity

    var camX = 0.0; var camY = 4.0; var camZ = -8.0
    var camYaw = 25f; var camPitch = -15f

    val blockMap = mutableMapOf<BlockPos, BlockState>()
    private var sceneChunkSource: SceneChunkSource? = null
    private var dummyPlayer: LocalPlayer? = null
    private var setupOk = false

    init {
        val ok = initWorld()
        setupOk = ok
    }

    private fun initWorld(): Boolean {
        val biome = Biome.BiomeBuilder()
            .hasPrecipitation(true)
            .temperature(0.8f)
            .downfall(0.4f)
            .specialEffects(BiomeSpecialEffects.Builder()
                .waterColor(0x3F76E4)
                .grassColorOverride(0x7EB346)
                .foliageColorOverride(0x59AE30)
                .build())
            .mobSpawnSettings(MobSpawnSettings.EMPTY)
            .generationSettings(BiomeGenerationSettings.EMPTY)
            .build()

        val regMap = HashMap<ResourceKey<out Registry<*>>, Registry<*>>()
        regMap[Registries.BLOCK] = BuiltInRegistries.BLOCK
        regMap[Registries.ITEM] = BuiltInRegistries.ITEM
        regMap[Registries.FLUID] = BuiltInRegistries.FLUID
        regMap[Registries.ENTITY_TYPE] = BuiltInRegistries.ENTITY_TYPE
        regMap[Registries.MOB_EFFECT] = BuiltInRegistries.MOB_EFFECT

        val biomeKey = ResourceKey.createRegistryKey<Biome>(Identifier.withDefaultNamespace("worldgen/biome"))
        val biomeRegistry = net.minecraft.core.MappedRegistry<Biome>(biomeKey, Lifecycle.stable(), false)
        regMap[Registries.BIOME] = biomeRegistry

        val registryAccess = RegistryAccess.ImmutableRegistryAccess(regMap).freeze()

        val conn = Connection(PacketFlow.CLIENTBOUND)
        val gameProfile = com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "SceneCamera")

        val cookie: CommonListenerCookie = try {
            val ctor = CommonListenerCookie::class.java.constructors.first()
            @Suppress("UNCHECKED_CAST")
            ctor.newInstance(
                LevelLoadTracker(),
                gameProfile,
                WorldSessionTelemetryManager(TelemetryEventSender.DISABLED, false, null, null),
                registryAccess,
                net.minecraft.world.flag.FeatureFlagSet.of(),
                null, null, null,
                java.util.Map.of<Identifier, ByteArray>(),
                null, java.util.Map.of<String, String>(),
                ServerLinks.EMPTY,
                java.util.Map.of<UUID, net.minecraft.client.multiplayer.PlayerInfo>(),
                false
            ) as CommonListenerCookie
        } catch (_: Exception) { return false }

        val dummyListener = object : ClientPacketListener(mc, conn, cookie) {
            override fun registryAccess(): RegistryAccess.Frozen = registryAccess
        }

        try {
            val f = ClientPacketListener::class.java.getDeclaredField("registryAccess")
            f.isAccessible = true
            f.set(dummyListener, registryAccess)
        } catch (_: Exception) { return false }

        val dimType = DimensionType(
            false, true, false, 1.0,
            DimensionType.MIN_Y, DimensionType.Y_SIZE, DimensionType.Y_SIZE,
            net.minecraft.tags.BlockTags.INFINIBURN_OVERWORLD,
            0.0f,
            DimensionType.MonsterSettings(net.minecraft.util.valueproviders.ConstantInt.of(0), 0),
            DimensionType.Skybox.OVERWORLD,
            DimensionType.CardinalLightType.DEFAULT,
            net.minecraft.world.attribute.EnvironmentAttributeMap.EMPTY,
            HolderSet.empty()
        )
        val dimTypeHolder = Holder.direct<DimensionType>(dimType)
        val levelData = ClientLevel.ClientLevelData(net.minecraft.world.Difficulty.PEACEFUL, false, false)

        val lvl = try {
            ClientLevel(
                dummyListener, levelData, Level.OVERWORLD, dimTypeHolder,
                4, 0, mc.levelRenderer, false, 0L, 63
            )
        } catch (_: Exception) { return false }

        level = lvl

        // Replace chunkSource
        val cs = SceneChunkSource(lvl)
        try {
            val f = ClientLevel::class.java.getDeclaredField("chunkSource")
            f.isAccessible = true
            f.set(lvl, cs)
        } catch (_: Exception) { return false }

        sceneChunkSource = cs

        // Camera entity — use LocalPlayer so mc.player can be set without NPE
        val player = LocalPlayer(
            mc, lvl, dummyListener,
            StatsCounter(), ClientRecipeBook(), Input.EMPTY, false
        )
        player.setPos(camX, camY, camZ)
        cameraEntity = player
        dummyPlayer = player

        rebuildBlocks()
        return true
    }

    fun rebuildBlocks() {
        blockMap.clear()
        for (x in -3 until 3) {
            for (z in -3 until 3) {
                blockMap[BlockPos(x, 0, z)] = Blocks.GRASS_BLOCK.defaultBlockState()
            }
        }
        blockMap[BlockPos(0, 1, 0)] = Blocks.STONE.defaultBlockState()
        blockMap[BlockPos(0, 2, 0)] = Blocks.STONE.defaultBlockState()
        blockMap[BlockPos(0, 3, 0)] = Blocks.GOLD_BLOCK.defaultBlockState()
        blockMap[BlockPos(0, -1, 0)] = Blocks.BEDROCK.defaultBlockState()

        sceneChunkSource!!.updateBlocks(blockMap)
    }

    fun updateBlocks(blocks: Map<BlockPos, BlockState>) {
        blockMap.clear()
        blockMap.putAll(blocks)
        sceneChunkSource!!.updateBlocks(blockMap)
    }

    fun render(delta: Float) {
        if (!setupOk) return
        val oldLevel = mc.level
        val oldPlayer = mc.player
        val oldCameraEntity = mc.cameraEntity

        try {
            mc.level = level
            mc.player = dummyPlayer
            mc.cameraEntity = cameraEntity

            cameraEntity.setPos(camX, camY, camZ)
            cameraEntity.yRot = camYaw
            cameraEntity.xRot = camPitch

            mc.gameRenderer.mainCamera.setup(
                level, cameraEntity, false, false, delta
            )

            val dt = object : DeltaTracker {
                override fun getGameTimeDeltaTicks() = delta
                override fun getGameTimeDeltaPartialTick(paused: Boolean) = delta
                override fun getRealtimeDeltaTicks() = delta
            }

            mc.gameRenderer.renderLevel(dt)
        } catch (_: Exception) {}
        finally {
            mc.levelRenderer.setLevel(oldLevel)
            mc.level = oldLevel
            mc.player = oldPlayer
            mc.cameraEntity = oldCameraEntity
        }
    }

    fun close() {
        try { level.close() } catch (_: Exception) {}
        mc.levelRenderer.setLevel(null)
        mc.levelRenderer.allChanged()
    }

    // ============================================================
    // Custom ChunkSource
    // ============================================================

    private class SceneChunkSource(
        private val level: Level
    ) : ChunkSource() {
        private val chunks = mutableMapOf<Long, SceneChunk>()

        fun updateBlocks(blocks: Map<BlockPos, BlockState>) {
            chunks.clear()
            val byChunk = mutableMapOf<Long, MutableList<Pair<BlockPos, BlockState>>>()
            for ((pos, state) in blocks) {
                val key = ChunkPos.asLong(pos.x shr 4, pos.z shr 4)
                byChunk.getOrPut(key) { mutableListOf() }.add(pos to state)
            }
            for ((key, entries) in byChunk) {
                val cp = ChunkPos(key)
                val chunk = SceneChunk(level, cp)
                for ((pos, state) in entries) {
                    chunk.placeBlock(pos, state)
                }
                chunks[key] = chunk
            }
        }

        override fun getChunk(x: Int, z: Int, status: ChunkStatus, load: Boolean): ChunkAccess? {
            return chunks[ChunkPos.asLong(x, z)]
        }

        override fun getLevel(): BlockGetter = level

        override fun tick(hasTimeLeft: BooleanSupplier, tickLightEngine: Boolean) {}
        override fun gatherStats() = "SceneChunkSource"
        override fun getLoadedChunksCount() = chunks.size
        override fun getLightEngine() = level.lightEngine
        override fun close() {}
    }

    private class SceneChunk(level: Level, pos: ChunkPos)
        : EmptyLevelChunk(level, pos, Holder.direct(createPlainBiome()))
    {
        private val placedBlocks = mutableMapOf<BlockPos, BlockState>()

        fun placeBlock(pos: BlockPos, state: BlockState) { placedBlocks[pos] = state }

        override fun getBlockState(pos: BlockPos): BlockState {
            return placedBlocks[pos] ?: Blocks.AIR.defaultBlockState()
        }
        override fun isEmpty() = placedBlocks.isEmpty()
    }

    companion object {
        private val PLAIN_BIOME = Biome.BiomeBuilder()
            .hasPrecipitation(true)
            .temperature(0.8f)
            .downfall(0.4f)
            .specialEffects(BiomeSpecialEffects.Builder()
                .waterColor(0x3F76E4)
                .grassColorOverride(0x7EB346)
                .foliageColorOverride(0x59AE30)
                .build())
            .mobSpawnSettings(MobSpawnSettings.EMPTY)
            .generationSettings(BiomeGenerationSettings.EMPTY)
            .build()

        fun createPlainBiome() = PLAIN_BIOME
    }
}
