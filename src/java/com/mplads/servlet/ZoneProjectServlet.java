package com.mplads.servlet;

import com.mplads.dao.ZoneDAO;
import com.mplads.model.Project;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * GET /api/zone-projects?level=High|Medium|Low
 * Returns projects (same JSON fields as /api/projects) whose ML risk_level matches.
 */
@WebServlet("/api/zone-projects")
public class ZoneProjectServlet extends HttpServlet {

    private ZoneDAO dao = new ZoneDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String level = request.getParameter("level");
        String lv = level == null ? "" : level.trim().toLowerCase();
        if (!lv.equals("high") && !lv.equals("medium") && !lv.equals("low")) {
            response.setStatus(400);
            response.getWriter().print("{\"error\":\"level must be High, Medium or Low\"}");
            return;
        }

        List<Project> projects;
        try {
            projects = dao.getByRiskLevel(lv);
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(500);
            response.getWriter().print("{\"error\":\"Could not load zone projects\"}");
            return;
        }

        PrintWriter out = response.getWriter();
        out.print("[");
        for (int i = 0; i < projects.size(); i++) {
            Project p = projects.get(i);
            out.print("{");
            out.print("\"ml_id\":" + p.getMlResultId() + ",");
            out.print("\"project_id\":" + p.getProjectId() + ",");
            out.print("\"mp_name\":\"" + escapeJson(p.getMpName()) + "\",");
            out.print("\"work_\":\"" + escapeJson(p.getWork()) + "\",");
            out.print("\"state\":\"" + escapeJson(p.getState()) + "\",");
            out.print("\"constituency\":\"" + escapeJson(p.getConstituency()) + "\",");
            out.print("\"Date_\":\"" + escapeJson(p.getDate()) + "\",");
            out.print("\"allocation_amount\":" + p.getAllocationAmount() + ",");
            out.print("\"ida_approval\":\"" + escapeJson(p.getIdaApproval()) + "\",");
            out.print("\"project_status\":\"" + escapeJson(p.getProjectStatus()) + "\",");
            out.print("\"house\":\"" + escapeJson(p.getHouse()) + "\"");
            out.print("}");
            if (i < projects.size() - 1) out.print(",");
        }
        out.print("]");
    }

    private String escapeJson(String data) {
        if (data == null) return "";
        return data.replace("\\", "\\\\").replace("\"", "\\\"")
                   .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}