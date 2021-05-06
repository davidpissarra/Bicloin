package pt.tecnico.rec.domain;

public class RecIsUserBikedUp {
    
    private boolean isUserBikedUp;
    private Integer tag;

    public RecIsUserBikedUp(boolean isUserBikedUp, Integer tag) {
        this.isUserBikedUp = isUserBikedUp;
        this.tag = tag;
    }

    public boolean getIsUserBikedUp() {
        return isUserBikedUp;
    }

    public void setIsUserBikedUp(boolean isUserBikedUp) {
        this.isUserBikedUp = isUserBikedUp;
    }

    public Integer getTag() {
        return tag;
    }

    public void setTag(Integer tag) {
        this.tag = tag;
    }

}
