package pt.tecnico.bicloin.hub.domain;


public class StationDistance implements Comparable<StationDistance> {
    private String abrev;
    private Integer distance;

    public StationDistance(String abrev, Integer distance) {
        this.abrev = abrev;
        this.distance = distance;
    }

    public String getAbrev() {
        return abrev;
    }

    public Integer getDistance() {
        return distance;
    }

    @Override
    public int compareTo(StationDistance d) {
        return distance.compareTo(d.getDistance());
    }
}
