package com.mplads.servlet;

import com.mplads.dao.ProjectDAO;
import com.mplads.dao.ZoneDAO;
import com.mplads.model.Project;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

/**
 * GET /api/analyze?id=PROJECT_ID[&level=High|Medium|Low]
 * Does NOT run the ML model. It returns the stored result for the project
 * from the mplads_ml_results table (risk_score, risk_level, anomaly_score, recommendation).
 */
@WebServlet("/api/analyze")
public class AnalyzeServlet extends HttpServlet {

    private ProjectDAO projectDAO = new ProjectDAO();
    private ZoneDAO zoneDAO = new ZoneDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String id = request.getParameter("id");
        String level = request.getParameter("level");   // optional: zone the user came from

        if (id == null || id.isEmpty()) {
            response.setStatus(400);
            response.getWriter().print("{\"error\":\"Project ID required\"}");
            return;
        }

        Project project = projectDAO.getProjectById(id);
        if (project == null) {
            response.setStatus(404);
            response.getWriter().print("{\"error\":\"Project not found\"}");
            return;
        }

        try {
            Map<String, Object> r = zoneDAO.getMlResult(project, level);
            if (r == null) {
                response.setStatus(404);
                response.getWriter().print("{\"error\":\"No risk result found for this project in mplads_ml_results\"}");
                return;
            }

            String riskLevel = String.valueOf(r.get("risk_level"));
            String recommendation = r.get("recommendation") == null ? "" : String.valueOf(r.get("recommendation")).trim();
            if (recommendation.isEmpty()) {
                if (riskLevel.equalsIgnoreCase("high")) recommendation = "Requires priority verification.";
                else if (riskLevel.equalsIgnoreCase("medium")) recommendation = "Requires further review.";
                else recommendation = "Project looks normal. No anomalies detected.";
            }

            response.getWriter().print("{"
                    + "\"risk_score\":" + r.get("risk_score") + ","
                    + "\"risk_level\":\"" + escapeJson(riskLevel) + "\","
                    + "\"anomaly_score\":" + r.get("anomaly_score") + ","
                    + "\"recommendation\":\"" + escapeJson(recommendation) + "\""
                    + "}");

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(500);
            response.getWriter().print("{\"error\":\"Could not load the risk result from the database\"}");
        }
    }

    private String escapeJson(String data) {
        if (data == null) return "";
        return data.replace("\\", "\\\\").replace("\"", "\\\"")
                   .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}