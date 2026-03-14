package com.ljj.campusrun.run;

public final class RunMetricCalculator {

    private static final double EARTH_RADIUS_METERS = 6371000D;

    private RunMetricCalculator() {
    }

    public static int averagePaceSeconds(double distanceKm, int durationSeconds) {
        if (distanceKm <= 0 || durationSeconds <= 0) {
            return 0;
        }
        return (int) Math.round(durationSeconds / distanceKm);
    }

    public static int calories(double distanceKm) {
        if (distanceKm <= 0) {
            return 0;
        }
        return (int) Math.round(distanceKm * 60);
    }

    public static double distanceMeters(double latitude1, double longitude1, double latitude2, double longitude2) {
        double lat1 = Math.toRadians(latitude1);
        double lat2 = Math.toRadians(latitude2);
        double deltaLat = Math.toRadians(latitude2 - latitude1);
        double deltaLng = Math.toRadians(longitude2 - longitude1);
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
    }
}
