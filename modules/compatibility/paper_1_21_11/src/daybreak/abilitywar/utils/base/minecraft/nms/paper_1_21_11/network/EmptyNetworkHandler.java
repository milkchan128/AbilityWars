package daybreak.abilitywar.utils.base.minecraft.nms.paper_1_21_11.network;


import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.player.Player;

public class EmptyNetworkHandler extends ServerGamePacketListenerImpl {

	public EmptyNetworkHandler(MinecraftServer minecraftServer, Connection networkManager, ServerPlayer entityPlayer) {
		super(minecraftServer, networkManager, entityPlayer, CommonListenerCookie.createInitial(entityPlayer.getGameProfile(), false));
	}

	@Override
	public void send(Packet<?> packet) {
	}
}
