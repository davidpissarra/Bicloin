package pt.tecnico.rec.domain;

public class RecBalance {
    
    private Integer balance;
    private Integer tag;

    public RecBalance(Integer balance, Integer tag) {
        this.balance = balance;
        this.tag = tag;
    }

    public Integer getBalance() {
        return balance;
    }

    public void setBalance(Integer balance) {
        this.balance = balance;
    }

    public Integer getTag() {
        return tag;
    }

    public void setTag(Integer tag) {
        this.tag = tag;
    }

}
