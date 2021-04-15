package pt.tecnico.bicloin.hub.domain;


import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

public class Haversine {
    
    private static int distance(float userLatitude, float userLongitude, float stationLatitude, float stationLongitude) {
        double pi = Math.PI;
        double userLatitudeRad = userLatitude * pi / 180;
        double userLongitudeRad = userLongitude * pi / 180;
        double stationLatitudeRad = stationLatitude * pi / 180;
        double stationLongitudeRad = stationLongitude * pi / 180;

        double r = 6371000;

        double a = Math.pow(Math.sin((stationLatitudeRad - userLatitudeRad) / 2), 2);
        
        double b1 = Math.cos(userLatitudeRad) * Math.cos(stationLatitudeRad);
        double b2 = Math.pow(Math.sin((stationLongitudeRad - userLongitudeRad) / 2), 2);
        
        return (int) (2 * r * Math.sqrt(a + b1 * b2));
    }

    public static List<StationDistance> getClosestStations(float latitude, float longitude, List<Station> stations, Integer nStations) {
        List<StationDistance> distances = new ArrayList<>();
        stations.forEach(station -> distances
                            .add(new StationDistance(station.getAbrev(), distance(latitude, longitude, station.getLatitude(), station.getLongitude()))));
        Collections.sort(distances);
        if(distances.size() <= nStations) {
            return distances;
        }
        return distances.subList(0, nStations);
    }

    public static boolean inRangeStation(float userLatitude, float userLongitude, float stationLatitude, float stationLongitude) {
        Integer maxDistance = 200;
        return distance(userLatitude, userLongitude, stationLatitude, stationLongitude) < maxDistance ? true : false;
    }

}
