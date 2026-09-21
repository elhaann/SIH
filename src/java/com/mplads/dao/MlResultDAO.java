package com.mplads.dao;

import com.mplads.DBConnection;
import com.mplads.model.MlResult;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Reads the pre-computed analysis (risk score, anomaly score, recommendation)
 * from the mplads_ml_results table. Nothing is written or re-calculated here.
 */
public class MlResultDAO {

    // The table has no separate "recommendation" column, so the recommendation
    // text is the anomaly_explanation column.
    private static final String SQL_BY_ID =
            "SELECT id, risk_score, risk_level, anomaly_score, anomaly_explanation "
          + "FROM mplads_ml_results WHERE id = ?";

    /** @return the row with this id, or null if there is none */
    public MlResult getById(long id) throws Exception {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_BY_ID)) {

            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                MlResult r = new MlResult();
                r.setId(rs.getLong("id"));
                r.setRiskScore(rs.getInt("risk_score"));
                r.setRiskLevel(rs.getString("risk_level"));
                r.setAnomalyScore(rs.getDouble("anomaly_score"));
                r.setRecommendation(rs.getString("anomaly_explanation"));
                return r;
            }
        }
    }
}