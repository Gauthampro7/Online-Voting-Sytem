package com.dsu.onlinevoting;

// MessageDigest is Java's built-in library for hashing — we use it for SHA-256 password hashing
import java.security.MessageDigest;
// java.sql.* gives us everything for JDBC: Connection, Statement, PreparedStatement, ResultSet
import java.sql.*;
// Scanner reads text typed by the user in the terminal
import java.util.Scanner;

/**
 * Online National Polling System - CLI Version
 *
 * This is a single-file Java application that connects to a MySQL database
 * and runs as a terminal (command-line) program. No web browser needed.
 *
 * How to run:
 *   1. Start MySQL (MariaDB) on port 3306
 *   2. Run this class from Eclipse: Right-click -> Run As -> Java Application
 *
 * Default admin login: username=admin  password=admin123
 */
public class OnlineVotingApp {

    // ─── DATABASE CONNECTION SETTINGS ─────────────────────────────────────────
    // These are the credentials to connect to MySQL. 'root' is the default admin user.
    private static final String DB_USER = "root";
    private static final String DB_PASS = "";  // Leave blank if MySQL has no root password

    // DB_URL_BOOT: connects WITHOUT specifying a database.
    // We use this only once at startup to run: CREATE DATABASE IF NOT EXISTS online_voting
    private static final String DB_URL_BOOT = "jdbc:mysql://localhost:3306/?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";

    // DB_URL: connects WITH 'online_voting' already selected as the active database.
    // All normal queries (INSERT, SELECT, etc.) use this connection.
    private static final String DB_URL = "jdbc:mysql://localhost:3306/online_voting?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";

    // 'conn' is the single shared database connection used throughout the entire program
    private static Connection conn;

    // 'sc' is the Scanner that reads whatever the user types in the terminal
    private static final Scanner sc = new Scanner(System.in);

    // ─── ENTRY POINT ──────────────────────────────────────────────────────────
    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║   ONLINE NATIONAL POLLING SYSTEM     ║");
        System.out.println("╚══════════════════════════════════════╝");

        try {
            // STEP 1 — Connect without a database to safely create it if it doesn't exist yet.
            // We can't connect to 'online_voting' if it doesn't exist, so we use the boot URL first.
            try (Connection bootstrap = DriverManager.getConnection(DB_URL_BOOT, DB_USER, DB_PASS);
                 Statement st = bootstrap.createStatement()) {
                st.executeUpdate("CREATE DATABASE IF NOT EXISTS online_voting");
            }

            // STEP 2 — Now reconnect properly with the database name in the URL.
            // From this point on, all SQL queries know which database to use.
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);

            // Create all tables (if they don't already exist) and seed the default admin account
            setupDatabase();
            System.out.println("[OK] Connected to MySQL.\n");

        } catch (SQLException e) {
            // If anything goes wrong during connection, print the reason and stop the program
            System.out.println("[ERROR] Cannot connect to MySQL: " + e.getMessage());
            System.out.println("Make sure MySQL is running on port 3306.");
            return;
        }

        // ── MAIN MENU LOOP ───────────────────────────────────────────────────────
        // This loop keeps running until the user chooses Exit (option 3).
        // It's the starting screen that every user sees before logging in.
        while (true) {
            System.out.println("1. Login");
            System.out.println("2. Register");
            System.out.println("3. Exit");
            System.out.print("> ");
            String choice = sc.nextLine().trim(); // Read the user's typed input

            // Route to the correct action based on what was typed
            switch (choice) {
                case "1" -> login();    // Go to login flow
                case "2" -> register(); // Go to registration flow
                case "3" -> { System.out.println("Goodbye!"); return; } // Exit program
                default  -> System.out.println("Invalid choice.\n");
            }
        }
    }

    // ─── LOGIN ─────────────────────────────────────────────────────────────────

    private static void login() {
        // Ask the user to type their username and password
        System.out.print("Username: ");
        String username = sc.nextLine().trim();
        System.out.print("Password: ");
        String password = sc.nextLine().trim();

        // PreparedStatement is used instead of plain Statement to prevent SQL Injection attacks.
        // The '?' placeholders are filled in safely by ps.setString(), not by string concatenation.
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id, name, role FROM users WHERE username = ? AND password_hash = ?")) {

            ps.setString(1, username);
            ps.setString(2, sha256(password)); // Hash the password before comparing — never store/compare plain text
            ResultSet rs = ps.executeQuery();   // Execute the SELECT and get the results

            if (rs.next()) {
                // rs.next() returns true if at least one row was found — meaning credentials matched
                int id      = rs.getInt("id");
                String name = rs.getString("name");
                String role = rs.getString("role");
                System.out.println("\nWelcome, " + name + "! [" + role + "]\n");

                // Route to the correct dashboard based on the user's role
                switch (role) {
                    case "ADMIN"         -> adminMenu(id);
                    case "FIELD_OFFICER" -> officerMenu(id);
                    case "VOTER"         -> voterMenu(id, username);
                    case "CANDIDATE"     -> candidateMenu(id, username);
                    default              -> System.out.println("Unknown role.");
                }
            } else {
                // No row found — wrong username or password
                System.out.println("Invalid username or password.\n");
            }
        } catch (SQLException e) {
            System.out.println("[DB ERROR] " + e.getMessage());
        }
    }

    // ─── REGISTER ─────────────────────────────────────────────────────────────

    private static void register() {
        // Collect the new user's details from the terminal
        System.out.print("Full Name: ");
        String name = sc.nextLine().trim();
        System.out.print("Username: ");
        String username = sc.nextLine().trim();
        System.out.print("Password: ");
        String password = sc.nextLine().trim();
        System.out.println("Role (VOTER / CANDIDATE / FIELD_OFFICER / ADMIN): ");
        System.out.print("> ");
        // toUpperCase() ensures 'voter', 'Voter', 'VOTER' are all accepted the same way
        String role = sc.nextLine().trim().toUpperCase();

        // Validate that only one of the four allowed role strings was entered
        if (!role.matches("VOTER|CANDIDATE|FIELD_OFFICER|ADMIN")) {
            System.out.println("Invalid role.\n");
            return;
        }

        // INSERT the new user into the database.
        // The password is hashed with SHA-256 before saving — we NEVER store passwords in plain text.
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO users (username, password_hash, name, role) VALUES (?, ?, ?, ?)")) {
            ps.setString(1, username);
            ps.setString(2, sha256(password)); // Always store the hash, not the real password
            ps.setString(3, name);
            ps.setString(4, role);
            ps.executeUpdate();
            System.out.println("Registered successfully! You can now log in.\n");
        } catch (SQLException e) {
            // The 'username' column has a UNIQUE constraint in the DB, so duplicates are caught here
            if (e.getMessage().contains("Duplicate")) {
                System.out.println("Username already taken. Try another.\n");
            } else {
                System.out.println("[DB ERROR] " + e.getMessage());
            }
        }
    }

    // ─── ADMIN MENU ─────────────────────────────────────────────────────────────
    // The Admin is the Election Commission — they have the highest privileges:
    // approving voters, approving candidates, and seeing live results.

    private static void adminMenu(int adminId) {
        // This while(true) loop keeps the admin inside their menu until they choose Logout.
        // Every role menu in this program works the same way.
        while (true) {
            System.out.println("\u2500\u2500\u2500\u2500 ADMIN MENU \u2500\u2500\u2500\u2500");
            System.out.println("1. View Pending Voter Applications");
            System.out.println("2. Approve/Reject Voter Application");
            System.out.println("3. View Pending Candidate Applications");
            System.out.println("4. Approve/Reject Candidate Application");
            System.out.println("5. View Live Election Results");
            System.out.println("6. Logout");
            System.out.print("> ");
            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1" -> viewPendingVoters();                  // Shows all voters waiting for approval
                case "2" -> approveReject("voter_profiles");      // Updates a voter's status
                case "3" -> viewPendingCandidates();              // Shows all candidates waiting for approval
                case "4" -> approveReject("candidate_profiles");  // Updates a candidate's status
                case "5" -> viewResults();                        // Shows live vote counts
                case "6" -> { System.out.println("Logged out.\n"); return; } // Exit the menu loop
                default  -> System.out.println("Invalid choice.");
            }
        }
    }

    // ─── FIELD OFFICER MENU ───────────────────────────────────────────────────────
    // The Field Officer's only job is to verify citizen identity — they can only
    // see and approve/reject voter ID applications. They cannot touch candidates or results.

    private static void officerMenu(int officerId) {
        while (true) {
            System.out.println("\u2500\u2500\u2500\u2500 FIELD OFFICER MENU \u2500\u2500\u2500\u2500");
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

    // ─── VOTER MENU ─────────────────────────────────────────────────────────────────
    // A Voter must first apply for a Voter ID (approved by Field Officer) before they can vote.

    private static void voterMenu(int voterId, String username) {
        while (true) {
            System.out.println("\u2500\u2500\u2500\u2500 VOTER MENU \u2500\u2500\u2500\u2500");
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
                case "1" -> applyVoterID(voterId);         // Submit a voter ID application
                case "2" -> viewMyVoterStatus(voterId);    // Check if application was approved
                case "3" -> viewApprovedCandidates();      // See who is running
                case "4" -> castVote(voterId);             // Cast a vote (once only)
                case "5" -> postForum(voterId);            // Write a message on the forum
                case "6" -> viewForum();                   // Read forum messages
                case "7" -> { System.out.println("Logged out.\n"); return; }
                default  -> System.out.println("Invalid choice.");
            }
        }
    }

    // ─── CANDIDATE MENU ────────────────────────────────────────────────────────────
    // A Candidate applies to run in an election. Their application must be APPROVED by Admin.

    private static void candidateMenu(int candidateId, String username) {
        while (true) {
            System.out.println("\u2500\u2500\u2500\u2500 CANDIDATE MENU \u2500\u2500\u2500\u2500");
            System.out.println("1. Apply for Candidacy");
            System.out.println("2. View My Application Status");
            System.out.println("3. Post on Forum");
            System.out.println("4. View Forum");
            System.out.println("5. Logout");
            System.out.print("> ");
            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1" -> applyCandidate(candidateId);       // Submit party + manifesto
                case "2" -> viewMyCandidateStatus(candidateId); // Check approval status
                case "3" -> postForum(candidateId);            // Post on the discussion forum
                case "4" -> viewForum();                       // Read forum messages
                case "5" -> { System.out.println("Logged out.\n"); return; }
                default  -> System.out.println("Invalid choice.");
            }
        }
    }

    // ─── FEATURE IMPLEMENTATIONS ──────────────────────────────────────────────

    // Lets a voter submit a Voter ID application with their address & constituency.
    // A voter can only apply ONCE — we check for an existing row first.
    // Status starts as PENDING until a Field Officer approves it.
    private static void applyVoterID(int userId) {
        // Guard: stop if they already have an application in the DB
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

        // Save the new application. Status is hardcoded to 'PENDING' in the SQL.
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO voter_profiles (user_id, constituency, address, status) VALUES (?, ?, ?, 'PENDING')")) {
            ps.setInt(1, userId);
            ps.setString(2, constituency);
            ps.setString(3, address);
            ps.executeUpdate();
            System.out.println("Voter ID application submitted! Status: PENDING.\n");
        } catch (SQLException e) { System.out.println("[DB ERROR] " + e.getMessage()); }
    }

    // Shows the voter's current application status (PENDING / APPROVED / REJECTED).
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

    // Lets a candidate submit their party name, constituency, and manifesto.
    // Same one-application guard as voter — checked before collecting input.
    private static void applyCandidate(int userId) {
        // Guard: only one application per candidate allowed
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

        // Insert into candidate_profiles with status PENDING
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

    // Shows the candidate's current application status.
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

    // Queries all voter_profiles rows with status='PENDING' and prints them.
    // Uses a JOIN to get the user's name from the users table.
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

    // Same pattern as viewPendingVoters but for candidates. Shows party & manifesto too.
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

    // Shared approve/reject method used for BOTH voter and candidate tables.
    // The 'table' parameter is passed in from the menu (e.g. "voter_profiles").
    // Uses a ternary operator: if action is APPROVE -> APPROVED, if REJECT -> REJECTED.
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

    // Lists all candidates whose status is APPROVED so a voter can pick one to vote for.
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

    // The most critical method — enforces three rules before saving a vote:
    // 1. Voter must have an APPROVED Voter ID.
    // 2. Voter must not have voted already (votes table has UNIQUE on voter_id).
    // 3. The candidate ID entered must exist and be APPROVED.
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

    // Shows a live leaderboard. Uses LEFT JOIN so candidates with 0 votes still appear.
    // COUNT(v.id) counts how many votes each candidate has received.
    // ORDER BY votes DESC puts the winner at the top.
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

    // Saves a text message from the logged-in user into the forum_posts table.
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

    // Fetches the latest 20 forum posts, newest first, and prints them with timestamps.
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

    // ─── DATABASE SETUP ────────────────────────────────────────────────────────
    // Runs once at startup. Creates all tables if they don't already exist.
    // 'IF NOT EXISTS' is key — it means this is safe to run every time without wiping data.
    // Also inserts the default admin account using INSERT IGNORE (skips if already there).
    private static void setupDatabase() throws SQLException {
        // conn is already connected to the online_voting database at this point
        try (Statement st = conn.createStatement()) {
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

            // Seed a default admin account (password: admin123)
            st.executeUpdate("""
                INSERT IGNORE INTO users (username, password_hash, name, role)
                VALUES ('admin', '%s', 'System Administrator', 'ADMIN')
                """.formatted(sha256("admin123")));
        }
    }

    // ─── SHA-256 PASSWORD HASHING ─────────────────────────────────────────────
    // Converts a plain-text password into a fixed-length 64-character hex string.
    // SHA-256 is a one-way function — you cannot reverse it to get the original password.
    // This means even if the database is stolen, real passwords are safe.
    private static String sha256(String input) {
        try {
            // Get the SHA-256 hashing algorithm from Java's built-in crypto library
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            // Compute the hash as a raw array of bytes
            byte[] hash = md.digest(input.getBytes("UTF-8"));
            // Convert each byte to a 2-character hex string (e.g. byte 255 -> "ff")
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString(); // Return the final 64-character hex hash
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
