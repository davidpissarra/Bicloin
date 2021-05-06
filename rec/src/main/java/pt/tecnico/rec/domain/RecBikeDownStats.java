package pt.tecnico.rec.domain;

public class RecBikeDownStats {

    private Integer bikeDownStats;
    private Integer tag;

    public RecBikeDownStats(Integer bikeDownStats, Integer tag) {
        this.bikeDownStats = bikeDownStats;
        this.tag = tag;
    }

    public Integer getBikeDownStats() {
        return bikeDownStats;
    }

    public void setBikeDownStats(Integer bikeDownStats) {
        this.bikeDownStats = bikeDownStats;
    }

    public Integer getTag() {
        return tag;
    }

    public void setTag(Integer tag) {
        this.tag = tag;
    }
    
}
