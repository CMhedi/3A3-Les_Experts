package Services.admin.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class AdminOverview {
    private int total;
    private int confirmed;
    private int pending;
    private int canceled;

    private BigDecimal totalRevenue;      // CONFIRMED only
    private double revenueTrendPercent;   // مقارنة بالفترة اللي قبلها

    private List<TopPackStat> topPacks = new ArrayList<>();
    private List<DailyStat> dailySeries = new ArrayList<>();

    public AdminOverview() {}

    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }

    public int getConfirmed() { return confirmed; }
    public void setConfirmed(int confirmed) { this.confirmed = confirmed; }

    public int getPending() { return pending; }
    public void setPending(int pending) { this.pending = pending; }

    public int getCanceled() { return canceled; }
    public void setCanceled(int canceled) { this.canceled = canceled; }

    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }

    public double getRevenueTrendPercent() { return revenueTrendPercent; }
    public void setRevenueTrendPercent(double revenueTrendPercent) { this.revenueTrendPercent = revenueTrendPercent; }

    public List<TopPackStat> getTopPacks() { return topPacks; }
    public void setTopPacks(List<TopPackStat> topPacks) { this.topPacks = topPacks; }

    public List<DailyStat> getDailySeries() { return dailySeries; }
    public void setDailySeries(List<DailyStat> dailySeries) { this.dailySeries = dailySeries; }
}
