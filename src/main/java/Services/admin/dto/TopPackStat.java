package Services.admin.dto;

import java.math.BigDecimal;

public class TopPackStat {
    private int packId;
    private String packName;
    private String typePack;
    private long inscriptions;
    private BigDecimal revenue;

    public TopPackStat() {}

    public TopPackStat(int packId, String packName, String typePack, long inscriptions, BigDecimal revenue) {
        this.packId = packId;
        this.packName = packName;
        this.typePack = typePack;
        this.inscriptions = inscriptions;
        this.revenue = revenue;
    }

    public int getPackId() { return packId; }
    public void setPackId(int packId) { this.packId = packId; }

    public String getPackName() { return packName; }
    public void setPackName(String packName) { this.packName = packName; }

    public String getTypePack() { return typePack; }
    public void setTypePack(String typePack) { this.typePack = typePack; }

    public long getInscriptions() { return inscriptions; }
    public void setInscriptions(long inscriptions) { this.inscriptions = inscriptions; }

    public BigDecimal getRevenue() { return revenue; }
    public void setRevenue(BigDecimal revenue) { this.revenue = revenue; }

    @Override
    public String toString() {
        return packName + " (" + typePack + ") | inscriptions=" + inscriptions + " | revenue=" + revenue;
    }
}
