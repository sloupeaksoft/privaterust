package com.rustprivate;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Config(modid = RustPrivate.MODID, name = RustPrivate.MODID)
public class PrivateConfig {

    @Config.Comment("Tool cupboard protection radius in blocks (X, Y and Z)")
    @Config.RangeInt(min = 1, max = 64)
    public static int radius = 15;

    @Config.Comment("Server operators in creative mode ignore the protection")
    public static boolean opsBypassInCreative = true;

    @Config.Comment("Blocks that NOT authorized players may still place inside a protected zone (raid explosives)")
    public static String[] allowedBlocks = new String[]{
            "minerustaddons:c4",
            "minerustaddons:c4down",
            "minerustaddons:sachel",
            "minerustaddons:sacheldown"
    };

    @Config.Comment("Building tools (building plans, hammers) that NOT authorized players cannot use inside a protected zone")
    public static String[] buildTools = new String[]{
            "minerustaddons:buildingplan",
            "minerustaddons:buildingplantrianglefoundation",
            "minerustaddons:otherblueprint",
            "minerustaddons:pieceblueprint",
            "minerustaddons:blueprint2",
            "minerustaddons:hammer",
            "minerustaddons:repairhammerinwood",
            "minerustaddons:repairhammerinstone",
            "minerustaddons:repairhammerinmetal",
            "minerustaddons:repairhammerinmhq"
    };

    @Config.Comment("Registry name of the tool cupboard (bottom block)")
    public static String cupboardBlock = "minerustaddons:privatecupboard";

    @Config.Comment("Registry name of the tool cupboard upper (bounding box) block")
    public static String cupboardUpperBlock = "minerustaddons:woodcupboardprivate";

    @Config.Comment("Registry name of the sleeping bag block")
    public static String sleepingBagBlock = "minerustaddons:sleepingbag";

    @Mod.EventBusSubscriber(modid = RustPrivate.MODID)
    public static class Sync {
        @SubscribeEvent
        public static void onChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
            if (RustPrivate.MODID.equals(event.getModID())) {
                ConfigManager.sync(RustPrivate.MODID, Config.Type.INSTANCE);
            }
        }
    }
}
