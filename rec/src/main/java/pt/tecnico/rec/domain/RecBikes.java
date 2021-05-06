package pt.tecnico.rec.domain;

public class RecBikes {

    private Integer bikes;
    private Integer tag;

    public RecBikes(Integer bikes, Integer tag) {
        this.bikes = bikes;
        this.tag = tag;
    }

    public Integer getBikes() {
        return bikes;
    }

    public void setBikes(Integer bikes) {
        this.bikes = bikes;
    }

    public Integer getTag() {
        return tag;
    }

    public void setTag(Integer tag) {
        this.tag = tag;
    }

}
