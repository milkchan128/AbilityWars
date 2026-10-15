package daybreak.abilitywar.utils.base.minecraft.nms.paper_1_21_11;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.google.common.collect.ImmutableMultimap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Avatar;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.utils.base.minecraft.SkinInfo;
import daybreak.abilitywar.utils.base.minecraft.nms.IDummy;
import daybreak.abilitywar.utils.base.minecraft.nms.IHologram;
import daybreak.abilitywar.utils.base.minecraft.nms.paper_1_21_11.network.EmptyNetworkHandler;
import daybreak.abilitywar.utils.base.minecraft.nms.paper_1_21_11.network.EmptyNetworkManager;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class DummyImpl extends ServerPlayer implements IDummy {

	private static GameProfile createProfile(final SkinInfo skinInfo) {
		return new GameProfile(UUID.randomUUID(), skinInfo.getName(), new PropertyMap(ImmutableMultimap.of(
				"textures", new Property("textures", skinInfo.getValue(), skinInfo.getSignature()))));
	}

	private final JavaPlugin plugin = AbilityWar.getPlugin();
	private final IHologram hologram;
	private double elapsedSeconds = 1, damages = 0;
	private int untilReset = -1;

	public DummyImpl(final MinecraftServer server, final ServerLevel world, final Location location, final SkinInfo skinInfo) {
		super(server, world, createProfile(skinInfo), ClientInformation.createDefault());
		this.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
		this.setPos(location.getX(), location.getY(), location.getZ());
		this.hologram = new HologramImpl(world.getWorld(), location.getX(), location.getY() + 2, location.getZ(), IDLE_MESSAGE);
		this.connection = new EmptyNetworkHandler(server, new EmptyNetworkManager(PacketFlow.CLIENTBOUND), this);
		this.getEntityData().set(Avatar.DATA_PLAYER_MODE_CUSTOMISATION, SKIN_BIT_LAYER);
		new BukkitRunnable() {
			@Override
			public void run() {
				if (!isRemoved()) world.addFreshEntity(DummyImpl.this, SpawnReason.CUSTOM);
			}
		}.runTaskLater(AbilityWar.getPlugin(), 2L);
		((Entity) this).setPos(location.getX(), location.getY(), location.getZ());
	}

	public DummyImpl(final MinecraftServer server, final ServerLevel world, final Location location) {
		this(server, world, location, DEFAULT_SKIN);
	}

	@Override
	public void tick() {
		if (!isAlive() || hologram.isUnregistered() || !plugin.isEnabled()) {
			remove();
			return;
		}
		hologram.teleport(((Level) ((Entity) this).level()).getWorld(), ((Entity) this).getX(), ((Entity) this).getY() + 2, ((Entity) this).getZ(), ((Entity) this).getYRot(), ((Entity) this).getXRot());
		if (untilReset > 0) {
			elapsedSeconds += .05;
			if (--untilReset <= 0) {
				this.untilReset = -1;
				this.elapsedSeconds = 1;
				this.damages = 0;
			}
		}
		super.tick();
	}

	@Override
	public void addDamage(final double damage) {
		this.untilReset = TICKS_TO_RESET;
		this.damages += damage;
		updateHologram();
	}

	@Override
	public void display(final Player player) {
		final ServerGamePacketListenerImpl playerConnection = ((CraftPlayer) player).getHandle().connection;
		playerConnection.send(ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(java.util.List.of(this)));
		playerConnection.send(new ClientboundAddEntityPacket(this, 0, BlockPos.containing(position())));
		playerConnection.send(new ClientboundSetEntityDataPacket(getId(), getEntityData().getNonDefaultValues()));
		hologram.display(player);
	}

	private void updateHologram() {
		if (hologram.isUnregistered()) {
			return;
		}
		if (untilReset > 0) {
			hologram.setText("§7DPS§8: §f" + floor(damages / elapsedSeconds) + " §f| §7합계§8: §f" + floor(damages));
		} else {
			hologram.setText(IDLE_MESSAGE);
		}
	}

	private double floor(final double a) {
		return Math.floor(a * 1000) / 1000;
	}

	@Override
	public CraftPlayer getBukkitEntity() {
		return super.getBukkitEntity();
	}

	@Override
	public UUID getUniqueID() {
		return ((Entity) this).getUUID();
	}

	@Override
	public void remove() {
		if (isRemoved() && hologram.isUnregistered()) return;
		((Entity) this).discard();
		((ServerChunkCache) ((LevelAccessor) ((Entity) this).level()).getChunkSource()).removeEntity(this);
		if (!hologram.isUnregistered()) {
			hologram.unregister();
		}
		final ClientboundRemoveEntitiesPacket packet = new ClientboundRemoveEntitiesPacket(((Entity) this).getId());
		for (CraftPlayer player : ((CraftServer) Bukkit.getServer()).getOnlinePlayers()) {
			((ServerCommonPacketListenerImpl) player.getHandle().connection).send(packet);
			player.getHandle().connection.send(new ClientboundPlayerInfoRemovePacket(java.util.List.of(getUUID())));
		}
		connection.connection.channel.close();
	}

}
