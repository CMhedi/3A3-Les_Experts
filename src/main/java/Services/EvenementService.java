package Services;

import Entities.Evenement;
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
        String sql = "INSERT INTO evenement (titre, description, categorie_evt, date_event, lieu, nb_places, statut, image_url, prix_event) "
                +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, e.getTitre());
            ps.setString(2, e.getDescription());
            ps.setString(3, e.getCategorieEvt() == null ? null : e.getCategorieEvt().name());
            ps.setTimestamp(4, e.getDateEvent() == null ? null : Timestamp.valueOf(e.getDateEvent()));
            ps.setString(5, e.getLieu());
            ps.setInt(6, e.getNbPlaces());
            ps.setString(7, e.getStatut());
            ps.setString(8, e.getImageUrl());
            ps.setDouble(9, e.getPrix());
            ps.executeUpdate();
        }
    }

    @Override
    public void update(Evenement e) throws Exception {
        String sql = "UPDATE evenement SET titre=?, description=?, categorie_evt=?, date_event=?, lieu=?, nb_places=?, statut=?, image_url=?, prix_event=? "
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
            ps.setDouble(9, e.getPrix());
            ps.setInt(10, e.getIdEvenement());
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
        e.setPrix(rs.getDouble("prix_event"));
        return e;
    }

    public List<Evenement> getRecommendedEvents(int idUser) throws Exception {
        List<Evenement> allEvents = getAll();
        ReservationEvenementService resService = new ReservationEvenementService();
        java.util.Map<String, Integer> catInterests = resService.getUserInterestCategories(idUser);
        java.util.Map<String, Integer> locInterests = resService.getUserInterestLocations(idUser);
        java.util.Map<Integer, Integer> globalPop = resService.getGlobalPopularityMap();
        java.util.Map<Integer, Double> avgRatings = resService.getAverageRatingsMap();

        for (Evenement e : allEvents) {
            double score = 0;
            String cardCat = e.getCategorieEvt() != null ? e.getCategorieEvt().name() : "";

            // 1. Personal Matching (Category) - Weight 35%
            if (catInterests.containsKey(cardCat)) {
                score += catInterests.get(cardCat) * 18;
            }

            // 2. Location Affinity - Weight 10%
            if (locInterests.containsKey(e.getLieu())) {
                score += locInterests.get(e.getLieu()) * 10;
            }

            // 3. Social Proof (Popularity) - Weight 15%
            int sold = globalPop.getOrDefault(e.getIdEvenement(), 0);
            score += Math.min(25, (sold / 5.0) * 8);

            // 4. SATISFACTION (Actual User Ratings) - CRUCIAL Weight 25%
            double rating = avgRatings.getOrDefault(e.getIdEvenement(), 0.0);
            if (rating >= 4.0) {
                score += (rating * 8); // Gros boost pour le 4* et 5*
            }

            // 5. Urgency & Scarcity
            if (e.getNbPlaces() > 0 && e.getNbPlaces() < 10) {
                score += 15;
            }

            // 6. Temporal Relevance
            if (e.getDateEvent() != null) {
                java.time.Duration duration = java.time.Duration.between(LocalDateTime.now(), e.getDateEvent());
                if (!duration.isNegative() && duration.toDays() < 7) {
                    score += 15;
                }
            }

            e.setRelevanceScore(score);
        }

        // Sort by relevanceScore DESC
        allEvents.sort((e1, e2) -> Double.compare(e2.getRelevanceScore(), e1.getRelevanceScore()));
        return allEvents;
    }
}
