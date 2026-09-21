package com.mplads.servlet;

import com.mplads.dao.MlResultDAO;
import com.mplads.model.MlResult;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * GET /api/ml-result?id=123
 * Used by the "Run Analysis" button for Red / Yellow / Green Zone projects.
 * Returns the stored values from mplads_ml_results:
 * {"risk_score":85,"risk_level":"High","anomaly_score":0.62,"recommendation":"..."}
 *
 * The id is mplads_ml_results.id (sent by the zone pages as "ml_id").
 */
@WebServlet("/api/ml-result")
public class MlResultServlet extends HttpServlet {

    private MlResultDAO dao = new MlResultDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String idParam = request.getParameter("id");
        long id;
        try {
            id = Long.parseLong(idParam == null ? "" : idParam.trim());
        } catch (NumberFormatException e) {
            response.setStatus(400);
            response.getWriter().print("{\"error\":\"A numeric id is required\"}");
            return;
        }

        MlResult r;
        try {
            r = dao.getById(id);
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(500);
            response.getWriter().print("{\"error\":\"Could not load the analysis from the database\"}");
            return;
        }

        if (r == null) {
            response.setStatus(404);
            response.getWriter().print("{\"error\":\"No analysis found for this project\"}");
            return;
        }

        double anomaly = r.getAnomalyScore();
        String anomalyJson = (Double.isNaN(anomaly) || Double.isInfinite(anomaly))
                ? "null" : String.valueOf(anomaly);

        response.getWriter().print("{"
                + "\"id\":" + r.getId() + ","
                + "\"risk_score\":" + r.getRiskScore() + ","
                + "\"risk_level\":\"" + escapeJson(r.getRiskLevel()) + "\","
                + "\"anomaly_score\":" + anomalyJson + ","
                + "\"recommendation\":\"" + escapeJson(r.getRecommendation()) + "\""
                + "}");
    }

    private String escapeJson(String data) {
        if (data == null) return "";
        return data.replace("\\", "\\\\").replace("\"", "\\\"")
                   .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}