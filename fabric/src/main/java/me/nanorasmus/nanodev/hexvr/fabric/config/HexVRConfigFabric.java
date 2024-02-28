package me.nanorasmus.nanodev.hexvr.fabric.config;

import at.petrak.hexcasting.xplat.IXplatAbstractions;
import me.nanorasmus.nanodev.hexvr.config.HexVRConfig;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import me.shedaniel.autoconfig.serializer.PartitioningSerializer;

@Config(name = "HexVR")
public class HexVRConfigFabric extends PartitioningSerializer.GlobalData {
    @ConfigEntry.Category("client")
    @ConfigEntry.Gui.TransitiveObject
    private final HexVRConfigClientFabric client = new HexVRConfigClientFabric();

    public static void setup() {
        AutoConfig.register(HexVRConfigFabric.class, PartitioningSerializer.wrap(JanksonConfigSerializer::new));
        var instance = AutoConfig.getConfigHolder(HexVRConfigFabric.class).getConfig();

        if (IXplatAbstractions.INSTANCE.isPhysicalClient()) {
            HexVRConfigClientFabric client = instance.client;
            HexVRConfig.client.overrideValues(client.gridSize, client.snappingDistance, client.backTrackDistance, client.patternsAlwaysVisible);
        }

    }

    @Config(name = "client")
    public static final class HexVRConfigClientFabric implements ConfigData {
        public double gridSize = 0.2;
        public double snappingDistance = 0.12;
        public double backTrackDistance = 0.09;

        public boolean patternsAlwaysVisible = true;

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
