package com.mplads.dao;

import com.mplads.DBConnection;
import com.mplads.model.Project;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProjectDAO {

    public List<Project> getAllProjects() {
        List<Project> projects = new ArrayList<>();
        String sql = "SELECT * FROM projectt";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Project p = new Project();
                p.setProjectId(rs.getInt("project_id"));
                p.setMpName(rs.getString("mp_name"));
                p.setWork(rs.getString("work_"));
                p.setState(rs.getString("state"));
                p.setConstituency(rs.getString("constituency"));
                p.setIda(rs.getString("ida"));
                p.setDate(rs.getString("Date_"));
                p.setAllocationAmount(rs.getInt("allocation_amount"));
                p.setIdaApproval(rs.getString("ida_approval"));
                p.setProjectStatus(rs.getString("project_status"));
                p.setHouse(rs.getString("house"));

                projects.add(p);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return projects;
    }

    public Project getProjectById(String id) {
        String sql = "SELECT * FROM projectt WHERE project_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            // project_id is int in DB now, so parse the string ID
            ps.setInt(1, Integer.parseInt(id));
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                Project p = new Project();
                p.setProjectId(rs.getInt("project_id"));
                p.setMpName(rs.getString("mp_name"));
                p.setWork(rs.getString("work_"));
                p.setState(rs.getString("state"));
                p.setConstituency(rs.getString("constituency"));
                p.setIda(rs.getString("ida"));
                p.setDate(rs.getString("Date_"));
                p.setAllocationAmount(rs.getInt("allocation_amount"));
                p.setIdaApproval(rs.getString("ida_approval"));
                p.setProjectStatus(rs.getString("project_status"));
                p.setHouse(rs.getString("house"));

                return p;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}