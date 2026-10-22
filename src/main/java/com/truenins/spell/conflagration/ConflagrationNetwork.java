package com.truenins.spell.conflagration;

import com.truenins.TrueNinsMod;
import com.truenins.entity.ConflagrationEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.ArrayList;
import java.util.UUID;
import java.util.function.Supplier;

/** Dimension-wide playback, including players outside entity tracking distance. S2C only. */
@Mod.EventBusSubscriber(modid = TrueNinsMod.MODID)
public final class ConflagrationNetwork {
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(TrueNinsMod.MODID, "conflagration"), () -> "2", "2"::equals, "2"::equals);
    private ConflagrationNetwork() {}
    public static void init() {
        CHANNEL.messageBuilder(Scene.class, 0, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(Scene::write).decoder(Scene::read).consumerMainThread(Scene::handle).add();
    }
    public record Scene(ResourceLocation dimension, ConflagrationPlan plan, boolean stop) {
        void write(FriendlyByteBuf b) {
            b.writeResourceLocation(dimension); b.writeBoolean(stop); b.writeUUID(plan.id());
            b.writeLong(plan.start()); vector(b, plan.center()); b.writeFloat(plan.blastRadius()); b.writeVarInt(plan.strikes().size());
            for (var s : plan.strikes()) {
                vector(b,s.from()); vector(b,s.to()); b.writeVarInt(s.launch()); b.writeVarInt(s.flight());
                b.writeFloat(s.size()); b.writeInt(s.seed()); b.writeBoolean(s.giant());
            }
        }
        static Scene read(FriendlyByteBuf b) {
            var dimension=b.readResourceLocation(); boolean stop=b.readBoolean(); UUID id=b.readUUID();
            long start=b.readLong(); Vec3 center=vector(b); float radius=b.readFloat(); int count=b.readVarInt();
            if(!Float.isFinite(radius)||radius<4||radius>32)throw new IllegalArgumentException("Invalid meteor blast radius");
            if(count<0||count>64)throw new IllegalArgumentException("Invalid meteor plan size");
            var strikes=new ArrayList<ConflagrationPlan.Strike>(count);
            for(int i=0;i<count;i++) strikes.add(new ConflagrationPlan.Strike(vector(b),vector(b),
                b.readVarInt(),b.readVarInt(),b.readFloat(),b.readInt(),b.readBoolean()));
            return new Scene(dimension,new ConflagrationPlan(id,start,center,radius,strikes),stop);
        }
        static void handle(Scene scene,Supplier<NetworkEvent.Context> context) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.truenins.client.conflagration.ConflagrationScene.receive(scene));
            context.get().setPacketHandled(true);
        }
    }
    private static Vec3 vector(FriendlyByteBuf b){return new Vec3(b.readDouble(),b.readDouble(),b.readDouble());}
    private static void vector(FriendlyByteBuf b,Vec3 v){b.writeDouble(v.x);b.writeDouble(v.y);b.writeDouble(v.z);}
    public static void broadcast(ServerLevel level,ConflagrationPlan plan,boolean stop) {
        CHANNEL.send(PacketDistributor.DIMENSION.with(level::dimension),new Scene(level.dimension().location(),plan,stop));
    }
    @SubscribeEvent public static void playerTick(TickEvent.PlayerTickEvent event) {
        if(event.phase!=TickEvent.Phase.END||!(event.player instanceof ServerPlayer p))return;
        // Dimension / login detection lives on the server thread. Re-sync after respawn as well.
        var tag=p.getPersistentData(); String dim=p.level().dimension().location().toString();
        if(!dim.equals(tag.getString("TNConflagrationDimension"))||p.tickCount==10) {
            tag.putString("TNConflagrationDimension",dim);
            for(var entity:p.serverLevel().getAllEntities())if(entity instanceof ConflagrationEntity c&&c.plan()!=null)
                CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new Scene(p.level().dimension().location(),c.plan(),false));
        }
    }
}
