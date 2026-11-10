package com.rustprivate;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

@Mod(
        modid = RustPrivate.MODID,
        name = "Rust Private (Tool Cupboard + Sleeping bag)",
        version = "1.0.0",
        acceptedMinecraftVersions = "[1.12.2]",
        dependencies = "required-after:minerustaddons"
)
public class RustPrivate {
    public static final String MODID = "rustprivate";

    @SidedProxy(clientSide = "com.rustprivate.ClientProxy", serverSide = "com.rustprivate.CommonProxy")
    public static CommonProxy proxy;

    public static SimpleNetworkWrapper NET;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        NET = NetworkRegistry.INSTANCE.newSimpleChannel(MODID);
        NET.registerMessage(MsgOpen.Handler.class, MsgOpen.class, 0, Side.CLIENT);
        NET.registerMessage(MsgAction.Handler.class, MsgAction.class, 1, Side.SERVER);
        MinecraftForge.EVENT_BUS.register(new PrivateHandler());
    }
}
