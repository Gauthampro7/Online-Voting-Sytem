package com.dsu.onlinevoting;

import java.security.MessageDigest;
import java.sql.*;
import java.util.Scanner;

/**
 * Online National Polling System - CLI Version
 * Single-file application using raw JDBC + MySQL.
 * Run with: java -jar target/onlinevoting-1.0.jar
 */
public class OnlineVotingApp {

    // ─── DATABASE CONFIG ──────────────────────────────────────────────────────
    private static final String DB_URL  = "jdbc:mysql://localhost:3306/?useSSL=false&serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "";  // Change if your root has a password

    private static Connection conn;
    private static final Scanner sc = new Scanner(System.in);

    // ─── ENTRY POINT ──────────────────────────────────────────────────────────
    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║   ONLINE NATIONAL POLLING SYSTEM     ║");
        System.out.println("╚══════════════════════════════════════╝");

        try {
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
            setupDatabase();
            System.out.println("[OK] Connected to MySQL.\n");
        } catch (SQLException e) {
            System.out.println("[ERROR] Cannot connect to MySQL: " + e.getMessage());
            System.out.println("Make sure MySQL is running on port 3306.");
            return;
        }

        // Main login/register loop
        while (true) {
            System.out.println("1. Login");
            System.out.println("2. Register");
            System.out.println("3. Exit");
            System.out.print("> ");
            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1" -> login();
                case "2" -> register();
                case "3" -> { System.out.println("Goodbye!"); return; }
                default  -> System.out.println("Invalid choice.\n");
            }
        }
    }

    // ─── AUTH ─────────────────────────────────────────────────────────────────

    private static void login() {
        System.out.print("Username: ");
        String username = sc.nextLine().trim();
        System.out.print("Password: ");
        String password = sc.nextLine().trim();

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id, name, role FROM users WHERE username = ? AND password_hash = ?")) {
            ps.setString(1, username);
            ps.setString(2, sha256(password));
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                int id       = rs.getInt("id");
                String name  = rs.getString("name");
                String role  = rs.getString("role");
                System.out.println("\nWelcome, " + name + "! [" + role + "]\n");

                switch (role) {
                    case "ADMIN"         -> adminMenu(id);
                    case "FIELD_OFFICER" -> officerMenu(id);
                    case "VOTER"         -> voterMenu(id, username);
                    case "CANDIDATE"     -> candidateMenu(id, username);
                    default              -> System.out.println("Unknown role.");
                }
            } else {
                System.out.println("Invalid username or password.\n");
            }
        } catch (SQLException e) {
            System.out.println("[DB ERROR] " + e.getMessage());
        }
    }

    private static void register() {
        System.out.print("Full Name: ");
        String name = sc.nextLine().trim();
        System.out.print("Username: ");
        String username = sc.nextLine().trim();
        System.out.print("Password: ");
        String password = sc.nextLine().trim();
        System.out.println("Role (VOTER / CANDIDATE / FIELD_OFFICER / ADMIN): ");
        System.out.print("> ");
        String role = sc.nextLine().trim().toUpperCase();

        if (!role.matches("VOTER|CANDIDATE|FIELD_OFFICER|ADMIN")) {
            System.out.println("Invalid role.\n");
            return;
        }

        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO users (username, password_hash, name, role) VALUES (?, ?, ?, ?)")) {
            ps.setString(1, username);
            ps.setString(2, sha256(password));
            ps.setString(3, name);
            ps.setString(4, role);
            ps.executeUpdate();
            System.out.println("Registered successfully! You can now log in.\n");
        } catch (SQLException e) {
            if (e.getMessage().contains("Duplicate")) {
                System.out.println("Username already taken. Try another.\n");
            } else {
                System.out.println("[DB ERROR] " + e.getMessage());
            }
        }
    }

    // ─── ADMIN MENU ───────────────────────────────────────────────────────────

    private static void adminMenu(int adminId) {
        while (true) {
            System.out.println("──── ADMIN MENU ────");
            System.out.println("1. View Pending Voter Applications");
            System.out.println("2. Approve/Reject Voter Application");
            System.out.println("3. View Pending Candidate Applications");
            System.out.println("4. Approve/Reject Candidate Application");
            System.out.println("5. View Live Election Results");
            System.out.println("6. Logout");
            System.out.print("> ");
            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1" -> viewPendingVoters();
                case "2" -> approveReject("voter_profiles");
                case "3" -> viewPendingCandidates();
                case "4" -> approveReject("candidate_profiles");
                case "5" -> viewResults();
                case "6" -> { System.out.println("Logged out.\n"); return; }
                default  -> System.out.println("Invalid choice.");
            }
        }
    }

    // ─── FIELD OFFICER MENU ───────────────────────────────────────────────────

    private static void officerMenu(int officerId) {
        while (true) {
            System.out.println("──── FIELD OFFICER MENU ────");
            System.out.println("1. View Pending Voter Applications");
            System.out.println("2. Approve/Reject Voter Application");
            System.out.println("3. Logout");
            System.out.print("> ");
            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1" -> viewPendingVoters();
                case "2" -> approveReject("voter_profiles");
                case "3" -> { System.out.println("Logged out.\n"); return; }
                default  -> System.out.println("Invalid choice.");
            }
        }
    }

    // ─── VOTER MENU ───────────────────────────────────────────────────────────

    private static void voterMenu(int voterId, String username) {
        while (true) {
            System.out.println("──── VOTER MENU ────");
            System.out.println("1. Apply for Voter ID");
            System.out.println("2. View My Application Status");
            System.out.println("3. View Approved Candidates");
            System.out.println("4. Cast Vote");
            System.out.println("5. Post on Forum");
            System.out.println("6. View Forum");
            System.out.println("7. Logout");
            System.out.print("> ");
            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1" -> applyVoterID(voterId);
                case "2" -> viewMyVoterStatus(voterId);
                case "3" -> viewApprovedCandidates();
                case "4" -> castVote(voterId);
                case "5" -> postForum(voterId);
                case "6" -> viewForum();
                case "7" -> { System.out.println("Logged out.\n"); return; }
                default  -> System.out.println("Invalid choice.");
            }
        }
    }

    // ─── CANDIDATE MENU ───────────────────────────────────────────────────────

    private static void candidateMenu(int candidateId, String username) {
        while (true) {
            System.out.println("──── CANDIDATE MENU ────");
            System.out.println("1. Apply for Candidacy");
            System.out.println("2. View My Application Status");
            System.out.println("3. Post on Forum");
            System.out.println("4. View Forum");
            System.out.println("5. Logout");
            System.out.print("> ");
            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1" -> applyCandidate(candidateId);
                case "2" -> viewMyCandidateStatus(candidateId);
                case "3" -> postForum(candidateId);
                case "4" -> viewForum();
                case "5" -> { System.out.println("Logged out.\n"); return; }
                default  -> System.out.println("Invalid choice.");
            }
        }
    }

    // ─── FEATURE IMPLEMENTATIONS ──────────────────────────────────────────────

    private static void applyVoterID(int userId) {
        // Check if already applied
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id FROM voter_profiles WHERE user_id = ?")) {
            ps.setInt(1, userId);
            if (ps.executeQuery().next()) {
                System.out.println("You have already submitted a Voter ID application.\n");
                return;
            }
        } catch (SQLException e) { System.out.println("[DB ERROR] " + e.getMessage()); return; }

        System.out.print("Enter your Constituency: ");
        String constituency = sc.nextLine().trim();
        System.out.print("Enter your Address: ");
        String address = sc.nextLine().trim();

        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO voter_profiles (user_id, constituency, address, status) VALUES (?, ?, ?, 'PENDING')")) {
            ps.setInt(1, userId);
            ps.setString(2, constituency);
            ps.setString(3, address);
            ps.executeUpdate();
            System.out.println("Voter ID application submitted! Status: PENDING.\n");
        } catch (SQLException e) { System.out.println("[DB ERROR] " + e.getMessage()); }
    }

    private static void viewMyVoterStatus(int userId) {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT constituency, status FROM voter_profiles WHERE user_id = ?")) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                System.out.println("Constituency: " + rs.getString("constituency"));
                System.out.println("Status      : " + rs.getString("status") + "\n");
            } else {
                System.out.println("No application found. Please apply first.\n");
            }
        } catch (SQLException e) { System.out.println("[DB ERROR] " + e.getMessage()); }
    }

    private static void applyCandidate(int userId) {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id FROM candidate_profiles WHERE user_id = ?")) {
            ps.setInt(1, userId);
            if (ps.executeQuery().next()) {
                System.out.println("You have already submitted a candidacy application.\n");
                return;
            }
        } catch (SQLException e) { System.out.println("[DB ERROR] " + e.getMessage()); return; }

        System.out.print("Party Name: ");
        String party = sc.nextLine().trim();
        System.out.print("Constituency: ");
        String constituency = sc.nextLine().trim();
        System.out.print("Manifesto (brief): ");
        String manifesto = sc.nextLine().trim();

        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO candidate_profiles (user_id, party, constituency, manifesto, status) VALUES (?, ?, ?, ?, 'PENDING')")) {
            ps.setInt(1, userId);
            ps.setString(2, party);
            ps.setString(3, constituency);
            ps.setString(4, manifesto);
            ps.executeUpdate();
            System.out.println("Candidacy application submitted! Status: PENDING.\n");
        } catch (SQLException e) { System.out.println("[DB ERROR] " + e.getMessage()); }
    }

    private static void viewMyCandidateStatus(int userId) {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT party, constituency, status FROM candidate_profiles WHERE user_id = ?")) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                System.out.println("Party       : " + rs.getString("party"));
                System.out.println("Constituency: " + rs.getString("constituency"));
                System.out.println("Status      : " + rs.getString("status") + "\n");
            } else {
                System.out.println("No candidacy application found.\n");
            }
        } catch (SQLException e) { System.out.println("[DB ERROR] " + e.getMessage()); }
    }

    private static void viewPendingVoters() {
        System.out.println("\n── Pending Voter Applications ──");
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                "SELECT vp.id, u.name, u.username, vp.constituency, vp.address " +
                "FROM voter_profiles vp JOIN users u ON u.id = vp.user_id " +
                "WHERE vp.status = 'PENDING'")) {
            boolean any = false;
            while (rs.next()) {
                any = true;
                System.out.printf("[ID:%d] %-20s (@%s) | %s | %s%n",
                    rs.getInt("id"), rs.getString("name"), rs.getString("username"),
                    rs.getString("constituency"), rs.getString("address"));
            }
            if (!any) System.out.println("No pending applications.");
        } catch (SQLException e) { System.out.println("[DB ERROR] " + e.getMessage()); }
        System.out.println();
    }

    private static void viewPendingCandidates() {
        System.out.println("\n── Pending Candidate Applications ──");
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                "SELECT cp.id, u.name, u.username, cp.party, cp.constituency, cp.manifesto " +
                "FROM candidate_profiles cp JOIN users u ON u.id = cp.user_id " +
                "WHERE cp.status = 'PENDING'")) {
            boolean any = false;
            while (rs.next()) {
                any = true;
                System.out.printf("[ID:%d] %-20s (@%s) | %s | %s%n",
                    rs.getInt("id"), rs.getString("name"), rs.getString("username"),
                    rs.getString("party"), rs.getString("constituency"));
                System.out.println("       Manifesto: " + rs.getString("manifesto"));
            }
            if (!any) System.out.println("No pending applications.");
        } catch (SQLException e) { System.out.println("[DB ERROR] " + e.getMessage()); }
        System.out.println();
    }

    private static void approveReject(String table) {
        System.out.print("Enter Application ID: ");
        String idStr = sc.nextLine().trim();
        System.out.print("Action (APPROVE / REJECT): ");
        String action = sc.nextLine().trim().toUpperCase();

        String status = action.equals("APPROVE") ? "APPROVED" : action.equals("REJECT") ? "REJECTED" : null;
        if (status == null) { System.out.println("Invalid action.\n"); return; }

        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE " + table + " SET status = ? WHERE id = ?")) {
            ps.setString(1, status);
            ps.setInt(2, Integer.parseInt(idStr));
            int rows = ps.executeUpdate();
            System.out.println(rows > 0 ? "Done! Status set to " + status + ".\n" : "ID not found.\n");
        } catch (SQLException | NumberFormatException e) {
            System.out.println("[ERROR] " + e.getMessage());
        }
    }

    private static void viewApprovedCandidates() {
        System.out.println("\n── Approved Candidates ──");
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                "SELECT cp.id, u.name, cp.party, cp.constituency, cp.manifesto " +
                "FROM candidate_profiles cp JOIN users u ON u.id = cp.user_id " +
                "WHERE cp.status = 'APPROVED'")) {
            boolean any = false;
            while (rs.next()) {
                any = true;
                System.out.printf("[ID:%d] %-20s | %s | %s%n",
                    rs.getInt("id"), rs.getString("name"),
                    rs.getString("party"), rs.getString("constituency"));
                System.out.println("       Manifesto: " + rs.getString("manifesto"));
            }
            if (!any) System.out.println("No approved candidates yet.");
        } catch (SQLException e) { System.out.println("[DB ERROR] " + e.getMessage()); }
        System.out.println();
    }

    private static void castVote(int voterId) {
        // Check voter is approved
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT status FROM voter_profiles WHERE user_id = ?")) {
            ps.setInt(1, voterId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                System.out.println("You need to apply for a Voter ID first.\n"); return;
            }
            if (!rs.getString("status").equals("APPROVED")) {
                System.out.println("Your Voter ID is not approved yet.\n"); return;
            }
        } catch (SQLException e) { System.out.println("[DB ERROR] " + e.getMessage()); return; }

        // Check already voted
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id FROM votes WHERE voter_id = ?")) {
            ps.setInt(1, voterId);
            if (ps.executeQuery().next()) {
                System.out.println("You have already cast your vote!\n"); return;
            }
        } catch (SQLException e) { System.out.println("[DB ERROR] " + e.getMessage()); return; }

        viewApprovedCandidates();
        System.out.print("Enter Candidate ID to vote for: ");
        String idStr = sc.nextLine().trim();

        // Validate candidate exists and is approved
        try (PreparedStatement validate = conn.prepareStatement(
                "SELECT user_id FROM candidate_profiles WHERE id = ? AND status = 'APPROVED'")) {
            validate.setInt(1, Integer.parseInt(idStr));
            ResultSet rs = validate.executeQuery();
            if (!rs.next()) {
                System.out.println("Invalid candidate ID.\n"); return;
            }
            int candidateUserId = rs.getInt("user_id");

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO votes (voter_id, candidate_id) VALUES (?, ?)")) {
                ps.setInt(1, voterId);
                ps.setInt(2, candidateUserId);
                ps.executeUpdate();
                System.out.println("Vote cast successfully! Thank you.\n");
            }
        } catch (SQLException | NumberFormatException e) {
            System.out.println("[ERROR] " + e.getMessage());
        }
    }

    private static void viewResults() {
        System.out.println("\n──── LIVE ELECTION RESULTS ────");
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                "SELECT u.name, cp.party, cp.constituency, COUNT(v.id) AS votes " +
                "FROM candidate_profiles cp " +
                "JOIN users u ON u.id = cp.user_id " +
                "LEFT JOIN votes v ON v.candidate_id = cp.user_id " +
                "WHERE cp.status = 'APPROVED' " +
                "GROUP BY cp.id ORDER BY votes DESC")) {
            int rank = 1;
            boolean any = false;
            while (rs.next()) {
                any = true;
                System.out.printf("#%d  %-20s | %s | %s | Votes: %d%n",
                    rank++, rs.getString("name"), rs.getString("party"),
                    rs.getString("constituency"), rs.getInt("votes"));
            }
            if (!any) System.out.println("No approved candidates yet.");
        } catch (SQLException e) { System.out.println("[DB ERROR] " + e.getMessage()); }
        System.out.println();
    }

    private static void postForum(int userId) {
        System.out.print("Your message: ");
        String content = sc.nextLine().trim();
        if (content.isEmpty()) { System.out.println("Empty message.\n"); return; }

        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO forum_posts (user_id, content) VALUES (?, ?)")) {
            ps.setInt(1, userId);
            ps.setString(2, content);
            ps.executeUpdate();
            System.out.println("Posted!\n");
        } catch (SQLException e) { System.out.println("[DB ERROR] " + e.getMessage()); }
    }

    private static void viewForum() {
        System.out.println("\n──── DISCUSSION FORUM ────");
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                "SELECT u.name, fp.content, fp.posted_at " +
                "FROM forum_posts fp JOIN users u ON u.id = fp.user_id " +
                "ORDER BY fp.posted_at DESC LIMIT 20")) {
            boolean any = false;
            while (rs.next()) {
                any = true;
                System.out.printf("[%s] %s: %s%n",
                    rs.getTimestamp("posted_at"), rs.getString("name"), rs.getString("content"));
            }
            if (!any) System.out.println("No posts yet. Be the first!");
        } catch (SQLException e) { System.out.println("[DB ERROR] " + e.getMessage()); }
        System.out.println();
    }

    // ─── DATABASE SETUP (runs on first launch) ────────────────────────────────

    private static void setupDatabase() throws SQLException {
        try (Statement st = conn.createStatement()) {
            // Create the database if it doesn't exist
            st.executeUpdate("CREATE DATABASE IF NOT EXISTS online_voting");
            st.executeUpdate("USE online_voting");

            // Update connection to use the database
            conn.setCatalog("online_voting");

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS users (
                    id            INT AUTO_INCREMENT PRIMARY KEY,
                    username      VARCHAR(100) NOT NULL UNIQUE,
                    password_hash VARCHAR(64)  NOT NULL,
                    name          VARCHAR(200) NOT NULL,
                    role          ENUM('VOTER','CANDIDATE','FIELD_OFFICER','ADMIN') NOT NULL
                )""");

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS voter_profiles (
                    id           INT AUTO_INCREMENT PRIMARY KEY,
                    user_id      INT NOT NULL UNIQUE,
                    constituency VARCHAR(200) NOT NULL,
                    address      TEXT,
                    status       ENUM('PENDING','APPROVED','REJECTED') NOT NULL DEFAULT 'PENDING',
                    FOREIGN KEY (user_id) REFERENCES users(id)
                )""");

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS candidate_profiles (
                    id           INT AUTO_INCREMENT PRIMARY KEY,
                    user_id      INT NOT NULL UNIQUE,
                    party        VARCHAR(200) NOT NULL,
                    constituency VARCHAR(200) NOT NULL,
                    manifesto    TEXT,
                    status       ENUM('PENDING','APPROVED','REJECTED') NOT NULL DEFAULT 'PENDING',
                    FOREIGN KEY (user_id) REFERENCES users(id)
                )""");

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS votes (
                    id           INT AUTO_INCREMENT PRIMARY KEY,
                    voter_id     INT NOT NULL UNIQUE,
                    candidate_id INT NOT NULL,
                    voted_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (voter_id)     REFERENCES users(id),
                    FOREIGN KEY (candidate_id) REFERENCES users(id)
                )""");

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS forum_posts (
                    id        INT AUTO_INCREMENT PRIMARY KEY,
                    user_id   INT NOT NULL,
                    content   TEXT NOT NULL,
                    posted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (user_id) REFERENCES users(id)
                )""");

            // Create a default admin account (password: admin123) if none exists
            st.executeUpdate("""
                INSERT IGNORE INTO users (username, password_hash, name, role)
                VALUES ('admin', '%s', 'System Administrator', 'ADMIN')
                """.formatted(sha256("admin123")));
        }
    }

    // ─── UTILITY: SHA-256 PASSWORD HASH ──────────────────────────────────────

    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
