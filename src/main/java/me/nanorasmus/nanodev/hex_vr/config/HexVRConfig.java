package me.nanorasmus.nanodev.hex_vr.config;

import at.petrak.hexcasting.xplat.IXplatAbstractions;
import me.nanorasmus.nanodev.hex_vr.HexVR;
import me.nanorasmus.nanodev.hex_vr.HexVRClient;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import me.shedaniel.autoconfig.serializer.PartitioningSerializer;

public class HexVRConfig extends PartitioningSerializer.GlobalData {
    @ConfigEntry.Category("client")
    @ConfigEntry.Gui.TransitiveObject
    private final HexVRClientConfig client = new HexVRClientConfig();

    public static HexVRConfig setup() {
        AutoConfig.register(HexVRConfig.class, PartitioningSerializer.wrap(JanksonConfigSerializer::new));
        var instance = AutoConfig.getConfigHolder(HexVRConfig.class).getConfig();

        if (IXplatAbstractions.INSTANCE.isPhysicalClient()) {
            HexVRClient.config = instance.client;
        }

        return instance;
    }

    @Config(name = HexVR.MOD_ID)
    public static final class HexVRClientConfig implements ConfigData {
        public double gridSize = 0.2;
        public double snappingDistance = 0.1;
        public double backTrackDistance = 0.08;

        @Override
        public void validatePostLoad() {
            // Make sure they're all above zero
            gridSize = Math.max(gridSize, 0);
            snappingDistance = Math.max(snappingDistance, 0);
            backTrackDistance = Math.max(backTrackDistance, 0);

            // Make sure none of them are out of bounds in relation to gridSize;
            snappingDistance = Math.min(snappingDistance, gridSize);
            backTrackDistance = Math.min(backTrackDistance, snappingDistance);
        }
    }
}


