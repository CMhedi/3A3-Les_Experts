package Entities;

public class Planning {

    private int idPlanning;
    private String periode;
    private String description;

    public Planning() {}

    public int getIdPlanning() { return idPlanning; }
    public void setIdPlanning(int idPlanning) { this.idPlanning = idPlanning; }

    public String getPeriode() { return periode; }
    public void setPeriode(String periode) { this.periode = periode; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    @Override
    public String toString() {
        return periode;
    }
}
