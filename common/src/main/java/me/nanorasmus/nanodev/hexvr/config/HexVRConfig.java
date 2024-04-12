package me.nanorasmus.nanodev.hexvr.config;

public class HexVRConfig {
    public static HexVRConfigClient client = new HexVRConfigClient();

    public static class HexVRConfigClient {
        public double gridSize = 0.2;
        public double snappingDistance = 0.12;
        public double backTrackDistance = 0.09;
        public boolean patternsAlwaysVisible = true;

        public void overrideValues(double gridSize, double snappingDistance, double backTrackDistance, boolean patternsAlwaysVisible) {
            this.gridSize = gridSize;
            this.snappingDistance = snappingDistance;
            this.backTrackDistance = backTrackDistance;
            this.patternsAlwaysVisible = patternsAlwaysVisible;
        }
    }
}
