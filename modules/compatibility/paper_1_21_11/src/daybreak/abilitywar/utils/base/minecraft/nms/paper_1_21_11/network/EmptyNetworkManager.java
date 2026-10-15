package daybreak.abilitywar.utils.base.minecraft.nms.paper_1_21_11.network;


import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.Packet;

import javax.annotation.Nullable;
import java.net.InetSocketAddress;

public class EmptyNetworkManager extends Connection {


	public EmptyNetworkManager(PacketFlow flag) {
		super(flag);
		this.channel = new EmbeddedChannel();
		this.address = new InetSocketAddress("127.0.0.1", 0);
	}

	@Override
	public void flushChannel() {}

	@Override
	public boolean isConnecting() {
		return false;
	}

	@Override
	public void send(Packet<?> packet) {}

	@Override
	public void send(Packet<?> packet, @Nullable ChannelFutureListener packetsendlistener) {}

	@Override
	public void send(Packet<?> packet, @Nullable ChannelFutureListener packetsendlistener, boolean flag) {}

	@Override
	public void setListenerForServerboundHandshake(PacketListener packetlistener) {}

	@Override
	public boolean isConnected() {
		return true;
	}

}
