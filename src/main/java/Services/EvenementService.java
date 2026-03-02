package Services;

import Entities.Evenement;
import Services.interfaces.IGenericService;
import Utiles.MyDB;
import enums.CategorieEvenement;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class EvenementService implements IGenericService<Evenement> {

    private final Connection cnx = MyDB.getConnection();

    @Override
    public void add(Evenement e) throws Exception {
        String sql = "INSERT INTO evenement (titre, description, categorie_evt, date_event, lieu, nb_places, statut, image_url) "
                +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, e.getTitre());
            ps.setString(2, e.getDescription());
            ps.setString(3, e.getCategorieEvt() == null ? null : e.getCategorieEvt().name());
            ps.setTimestamp(4, e.getDateEvent() == null ? null : Timestamp.valueOf(e.getDateEvent()));
            ps.setString(5, e.getLieu());
            ps.setInt(6, e.getNbPlaces());
            ps.setString(7, e.getStatut());
            ps.setString(8, e.getImageUrl());
            ps.executeUpdate();
        }
    }

    @Override
    public void update(Evenement e) throws Exception {
        String sql = "UPDATE evenement SET titre=?, description=?, categorie_evt=?, date_event=?, lieu=?, nb_places=?, statut=?, image_url=? "
                +
                "WHERE id_evenement=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, e.getTitre());
            ps.setString(2, e.getDescription());
            ps.setString(3, e.getCategorieEvt() == null ? null : e.getCategorieEvt().name());
            ps.setTimestamp(4, e.getDateEvent() == null ? null : Timestamp.valueOf(e.getDateEvent()));
            ps.setString(5, e.getLieu());
            ps.setInt(6, e.getNbPlaces());
            ps.setString(7, e.getStatut());
            ps.setString(8, e.getImageUrl());
            ps.setInt(9, e.getIdEvenement());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(int id) throws Exception {
        String sql = "DELETE FROM evenement WHERE id_evenement=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public List<Evenement> getAll() throws Exception {
        String sql = "SELECT * FROM evenement ORDER BY id_evenement DESC";
        List<Evenement> list = new ArrayList<>();
        try (Statement st = cnx.createStatement();
                ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    @Override
    public Evenement getById(int id) throws Exception {
        String sql = "SELECT * FROM evenement WHERE id_evenement=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return map(rs);
            }
        }
        return null;
    }

    private Evenement map(ResultSet rs) throws SQLException {
        Evenement e = new Evenement();
        e.setIdEvenement(rs.getInt("id_evenement"));
        e.setTitre(rs.getString("titre"));
        e.setDescription(rs.getString("description"));

        String cat = rs.getString("categorie_evt");
        if (cat != null) {
            try {
                e.setCategorieEvt(CategorieEvenement.valueOf(cat));
            } catch (Exception ignored) {
                e.setCategorieEvt(null);
            }
        }

        Timestamp ts = rs.getTimestamp("date_event");
        e.setDateEvent(ts == null ? null : ts.toLocalDateTime());

        e.setLieu(rs.getString("lieu"));
        e.setNbPlaces(rs.getInt("nb_places"));
        e.setStatut(rs.getString("statut"));
        e.setImageUrl(rs.getString("image_url"));
        return e;
    }
}
