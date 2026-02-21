package Services.admin.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class DailyStat {
    private LocalDate date;
    private int inscriptions;
    private BigDecimal revenue;

    public DailyStat() {}

    public DailyStat(LocalDate date, int inscriptions, BigDecimal revenue) {
        this.date = date;
        this.inscriptions = inscriptions;
        this.revenue = revenue;
    }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public int getInscriptions() { return inscriptions; }
    public void setInscriptions(int inscriptions) { this.inscriptions = inscriptions; }

    public BigDecimal getRevenue() { return revenue; }
    public void setRevenue(BigDecimal revenue) { this.revenue = revenue; }

    @Override
    public String toString() {
        return date + " | inscriptions=" + inscriptions + " | revenue=" + revenue;
    }
}
