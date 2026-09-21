package com.mplads.dao;

import com.mplads.DBConnection;
import com.mplads.model.Project;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Reads zone projects from the ML results table (mplads_ml_results).
 * Red = High risk, Yellow = Medium risk, Green = Low risk.
 *
 * The ML table has no project_id / approval column, so each ML row is linked to its
 * record in the `projects` table (in Java, not in SQL) to get project_id (for the
 * View button) and ida_approval.
 */
public class ZoneDAO {

    private static final String SQL_ZONE_ROWS =
            "SELECT m.mp_name, m.work, m.house, m.state, m.constituency, "
          + "       m.recommended_date, m.allocation_amount, m.status "
          + "FROM mplads_ml_results m "
          + "WHERE LOWER(TRIM(m.risk_level)) = ? "
          + "ORDER BY m.id";

    /** @param level "high", "medium" or "low" (any letter case) */
    public List<Project> getByRiskLevel(String level) throws Exception {
        String wanted = level.trim().toLowerCase();
        List<Project> rows = readZoneRows(wanted);

        int linked = 0;
        try {
            Index idx = getIndex();
            for (Project row : rows) {
                Project hit = find(idx, row);
                if (hit != null) {
                    row.setProjectId(hit.getProjectId());
                    row.setIdaApproval(hit.getIdaApproval());
                    linked++;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();   // rows are still returned, just without View links
        }
        System.out.println("[ZoneDAO] " + wanted + " risk: " + rows.size()
                + " rows, " + linked + " linked to projects table");
        return rows;
    }

    /** Number of records per risk level: keys "high", "medium", "low". */
    public Map<String, Integer> getZoneCounts() throws Exception {
        Map<String, Integer> counts = new HashMap<>();
        counts.put("high", 0);
        counts.put("medium", 0);
        counts.put("low", 0);

        String sql = "SELECT LOWER(TRIM(risk_level)) AS lv, COUNT(*) AS c "
                   + "FROM mplads_ml_results GROUP BY LOWER(TRIM(risk_level))";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String lv = rs.getString("lv");
                if (lv != null && counts.containsKey(lv)) {
                    counts.put(lv, rs.getInt("c"));
                }
            }
        }
        return counts;
    }

    /**
     * Finds the mplads_ml_results row that belongs to one project (matched the same way as the
     * zone pages: MP + work + amount, with state / constituency / date used to pick the best one).
     * If several rows match, the one whose risk_level equals {@code level} (may be null) wins.
     * Returns keys: risk_score, risk_level, anomaly_score, recommendation - or null if none found.
     */
    public Map<String, Object> getMlResult(Project p, String level) throws Exception {
        String sql = "SELECT * FROM mplads_ml_results m "
                   + "WHERE LOWER(TRIM(m.mp_name)) = ? AND ROUND(m.allocation_amount) = ?";
        Map<String, Object> best = null;
        int bestScore = -1;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, norm(p.getMpName()));
            ps.setInt(2, p.getAllocationAmount());

            try (ResultSet rs = ps.executeQuery()) {
                // the explanation column name is cut off in Workbench, so find it by prefix
                java.sql.ResultSetMetaData md = rs.getMetaData();
                String explCol = null;
                for (int i = 1; i <= md.getColumnCount(); i++) {
                    String n = md.getColumnLabel(i);
                    if (n.toLowerCase(Locale.ROOT).startsWith("anomaly_explanat")) explCol = n;
                }

                while (rs.next()) {
                    if (!normWork(rs.getString("work")).equals(normWork(p.getWork()))) continue;

                    int score = 0;
                    if (norm(rs.getString("state")).equals(norm(p.getState()))) score++;
                    if (norm(rs.getString("constituency")).equals(norm(p.getConstituency()))) score++;
                    if (norm(rs.getString("recommended_date")).equals(norm(p.getDate()))) score++;
                    String lv = rs.getString("risk_level");
                    if (level != null && lv != null && lv.trim().equalsIgnoreCase(level.trim())) score += 10;

                    if (score > bestScore) {
                        bestScore = score;
                        best = new HashMap<>();
                        best.put("risk_score", rs.getInt("risk_score"));
                        best.put("risk_level", lv == null ? "" : lv.trim());
                        best.put("anomaly_score", rs.getDouble("anomaly_score"));
                        best.put("recommendation", explCol == null ? "" : rs.getString(explCol));
                    }
                }
            }
        }
        return best;
    }

    private List<Project> readZoneRows(String wanted) throws Exception {
        List<Project> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_ZONE_ROWS)) {

            ps.setString(1, wanted);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Project p = new Project();
                    p.setMpName(rs.getString("mp_name"));
                    p.setWork(rs.getString("work"));
                    p.setState(rs.getString("state"));
                    p.setConstituency(rs.getString("constituency"));
                    p.setDate(rs.getString("recommended_date"));
                    BigDecimal amt = rs.getBigDecimal("allocation_amount");
                    p.setAllocationAmount(amt == null ? 0 : amt.setScale(0, RoundingMode.HALF_UP).intValue());
                    p.setProjectStatus(rs.getString("status"));
                    p.setHouse(rs.getString("house"));
                    list.add(p);
                }
            }
        }
        return list;
    }

    // ------------------------------------------------------------------
    // Linking ML rows to the projects table
    // ------------------------------------------------------------------

    /** Two lookup maps built from the projects table; the lowest project_id wins. */
    static final class Index {
        final Map<String, Project> strong = new HashMap<>();  // MP + work + amount + state + constituency + date
        final Map<String, Project> weak = new HashMap<>();    // MP + work + amount
    }

    private static final long CACHE_MS = 5 * 60 * 1000;
    private static Index cachedIndex;
    private static long cachedAt = 0;

    private static synchronized Index getIndex() {
        long now = System.currentTimeMillis();
        if (cachedIndex != null && now - cachedAt < CACHE_MS) {
            return cachedIndex;
        }
        List<Project> all = new ProjectDAO().getAllProjects();
        Index idx = buildIndex(all);
        if (!all.isEmpty()) {      // do not cache a failed / empty load
            cachedIndex = idx;
            cachedAt = now;
        }
        return idx;
    }

    static Index buildIndex(List<Project> all) {
        Index idx = new Index();
        for (Project p : all) {
            putLowest(idx.weak, weakKey(p), p);
            putLowest(idx.strong, strongKey(p), p);
        }
        return idx;
    }

    private static void putLowest(Map<String, Project> map, String key, Project p) {
        Project cur = map.get(key);
        if (cur == null || p.getProjectId() < cur.getProjectId()) {
            map.put(key, p);
        }
    }

    /** Best match first (all fields), then MP + work + amount. Null if nothing matches. */
    static Project find(Index idx, Project row) {
        Project hit = idx.strong.get(strongKey(row));
        if (hit == null) {
            hit = idx.weak.get(weakKey(row));
        }
        return hit;
    }

    private static String weakKey(Project p) {
        return norm(p.getMpName()) + "\u001f" + normWork(p.getWork()) + "\u001f" + p.getAllocationAmount();
    }

    private static String strongKey(Project p) {
        return weakKey(p) + "\u001f" + norm(p.getState()) + "\u001f"
                + norm(p.getConstituency()) + "\u001f" + norm(p.getDate());
    }

    /** Trim, collapse spaces, ignore letter case. */
    private static String norm(String s) {
        if (s == null) return "";
        return s.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    /** The ML table stores work as varchar(500), so compare only the first 500 characters. */
    private static String normWork(String s) {
        String n = norm(s);
        return n.length() > 500 ? n.substring(0, 500) : n;
    }
}