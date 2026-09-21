package com.mplads.servlet;

import java.io.IOException;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.WebServlet;
import java.sql.*;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    // =====================================================
    // DATABASE CONNECTION
    // =====================================================

    private static final String URL =
            "jdbc:mysql://mysql-d1e32da-elhanyasir81-f053.g.aivencloud.com:13489/MPLADs?ssl-mode=REQUIRED";

    private static final String USER =
            "avnadmin";

    private static final String PASSWORD =
            "AVNS_DszyK_UqyhVhh_T2zX2";


    // =====================================================
    // FIXED ADMIN CREDENTIALS
    // =====================================================

    private static final String ADMIN_EMAIL =
            "admin@gmail.com";

    private static final String ADMIN_PASSWORD =
            "admin123";


    @Override
    protected void doPost(HttpServletRequest req,
                           HttpServletResponse res)
            throws IOException, ServletException {

        // -------------------------------------------------
        // GET EMAIL AND PASSWORD
        // -------------------------------------------------

        String email = req.getParameter("email");
        String password = req.getParameter("password");


        // -------------------------------------------------
        // CHECK EMPTY VALUES
        // -------------------------------------------------

        if (email == null || email.trim().isEmpty()
                || password == null || password.trim().isEmpty()) {

            res.sendRedirect(
                    req.getContextPath() + "/login.html"
            );

            return;
        }

        email = email.trim();


        // =================================================
        // ADMIN LOGIN
        // =================================================

      if (email.equals(ADMIN_EMAIL)
        && password.equals(ADMIN_PASSWORD)) {

    HttpSession session = req.getSession();

    session.setAttribute(
            "admin",
            email
    );

    session.setAttribute(
            "userType",
            "ADMIN"
    );

    res.sendRedirect(
            req.getContextPath()
            + "/admin-dashboard.html"
    );

    return;
}


        // =================================================
        // NORMAL OFFICIAL LOGIN
        // =================================================

        String sql =
                "SELECT * FROM official_users " +
                "WHERE official_email = ? " +
                "AND password_hash = ?";


        try {

            // -------------------------------------------------
            // LOAD MYSQL DRIVER
            // -------------------------------------------------

            Class.forName(
                    "com.mysql.cj.jdbc.Driver"
            );


            // -------------------------------------------------
            // CONNECT TO DATABASE
            // -------------------------------------------------

            Connection con =
                    DriverManager.getConnection(
                            URL,
                            USER,
                            PASSWORD
                    );


            // -------------------------------------------------
            // PREPARED STATEMENT
            // -------------------------------------------------

            PreparedStatement stmt =
                    con.prepareStatement(sql);

            stmt.setString(
                    1,
                    email
            );

            stmt.setString(
                    2,
                    password
            );


            // -------------------------------------------------
            // EXECUTE
            // -------------------------------------------------

            ResultSet rs =
                    stmt.executeQuery();


            // =================================================
            // OFFICIAL LOGIN SUCCESS
            // =================================================

            if (rs.next()) {

                HttpSession session =
                        req.getSession();

                session.setAttribute(
                        "officialEmail",
                        rs.getString(
                                "official_email"
                        )
                );

                session.setAttribute(
                        "officialName",
                        rs.getString(
                                "full_name"
                        )
                );

                session.setAttribute(
                        "officialId",
                        rs.getString(
                                "official_id"
                        )
                );

                session.setAttribute(
                        "userType",
                        "OFFICIAL"
                );


                // Redirect to official dashboard

                res.sendRedirect(
                        req.getContextPath() +
                        "/dashboard.html"
                );

            } else {

                // Invalid official login

                res.setContentType(
                        "text/html"
                );

                res.getWriter().println(
                        "<script>" +
                        "alert('Invalid email or password');" +
                        "window.location='" +
                        req.getContextPath() +
                        "/login.html';" +
                        "</script>"
                );
            }


            // -------------------------------------------------
            // CLOSE RESOURCES
            // -------------------------------------------------

            rs.close();
            stmt.close();
            con.close();


        } catch (Exception e) {

            e.printStackTrace();

            res.setContentType(
                    "text/html"
            );

            res.getWriter().println(
                    "<script>" +
                    "alert('Database connection error');" +
                    "window.location='" +
                    req.getContextPath() +
                    "/login.html';" +
                    "</script>"
            );
        }
    }
}