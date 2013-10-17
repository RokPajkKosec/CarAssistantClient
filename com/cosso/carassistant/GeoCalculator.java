package com.cosso.carassistant;

/**
 * @author Rok Pajk Kosec
 * Class used for calculation geospacial values (distance, heading, etc.)
 */
public class GeoCalculator {
    private static final double R = 6372800; // In meters
	
    public static double initialBearing(double lat1, double long1, double lat2, double long2)
    {
        return (bearing(lat1, long1, lat2, long2) + 360.0) % 360;
    }

    public static double finalBearing(double lat1, double long1, double lat2, double long2)
    {
        return (bearing(lat2, long2, lat1, long1) + 180.0) % 360;
    }

    private static double bearing(double lat1, double long1, double lat2, double long2)
    {
        double degToRad = Math.PI / 180.0;
        double phi1 = lat1 * degToRad;
        double phi2 = lat2 * degToRad;
        double lam1 = long1 * degToRad;
        double lam2 = long2 * degToRad;

        return Math.atan2(Math.sin(lam2-lam1)*Math.cos(phi2),
            Math.cos(phi1)*Math.sin(phi2) - Math.sin(phi1)*Math.cos(phi2)*Math.cos(lam2-lam1)
        ) * 180/Math.PI;
    }
    
    public static double haversine(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);
 
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.sin(dLon / 2) * Math.sin(dLon / 2) * Math.cos(lat1) * Math.cos(lat2);
        double c = 2 * Math.asin(Math.sqrt(a));
        return R * c;
    }
}