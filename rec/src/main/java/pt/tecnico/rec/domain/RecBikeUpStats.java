package pt.tecnico.rec.domain;

public class RecBikeUpStats {

    private Integer bikeUpStats;
    private Integer tag;

    public RecBikeUpStats(Integer bikeUpStats, Integer tag) {
        this.bikeUpStats = bikeUpStats;
        this.tag = tag;
    }

    public Integer getBikeUpStats() {
        return bikeUpStats;
    }

    public void setBikeUpStats(Integer bikeUpStats) {
        this.bikeUpStats = bikeUpStats;
    }

    public Integer getTag() {
        return tag;
    }

    public void setTag(Integer tag) {
        this.tag = tag;
    }
    
}
