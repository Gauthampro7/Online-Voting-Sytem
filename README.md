# Online National Polling System

**Developed by: Gautham Saiju & Mahima Kumari**

A fully functional, aesthetic, and responsive Online Voting System developed for a college project. This system allows for end-to-end election management including voter registration, candidate nomination, live voting, and real-time result calculation.

## Tech Stack
- **Backend:** Java 17, Spring Boot 3, Spring Security, Spring MVC
- **Database:** Spring Data JPA, Hibernate, H2 Database (with MySQL configuration ready)
- **Frontend:** HTML5, Thymeleaf, Vanilla CSS (Premium Glassmorphism Design)

## Features & Roles
The system supports 4 distinct roles:
1. **General Public (Voter):** Can apply for a Voter ID, cast their vote once approved, and view the forums.
2. **Candidates:** Can apply for candidacy, publish a manifesto, and engage with voters.
3. **Field Officers:** Responsible for verifying and approving voter identity applications.
4. **Administrators (Election Commission):** Approve candidate applications and monitor live election results.

Additionally, a **Discussion Forum** allows all registered users to communicate.

## How to Run the Project Locally

### Prerequisites
- Java 17 or higher installed on your machine.
- An IDE like IntelliJ IDEA, Eclipse, or VS Code (with Java extensions).

### Running with H2 (Default & Recommended for Presentation)
The project is currently configured to use an in-memory **H2 database**. This means you **do not** need to install MySQL to run and showcase the project. The database will automatically start when the application runs and reset when you stop it.

1. Open the project folder `Online Voting Sytem` in your IDE.
2. Let the IDE resolve the Maven dependencies from the `pom.xml` file.
3. Locate the main class: `src/main/java/com/dsu/onlinevoting/OnlineVotingApplication.java`.
4. Right-click and select **Run 'OnlineVotingApplication'**.
5. Open your web browser and go to: `http://localhost:8080`.

### Running with MySQL (Optional)
If you specifically want to run the project with MySQL:
1. Ensure MySQL server is running locally on port `3306`.
2. Create a database named `online_voting`.
3. Open `src/main/resources/application.properties`.
4. Comment out the "Database Configuration (H2)" section.
5. Uncomment the "Database Configuration (MySQL)" section and verify your username/password.
6. Run the application from your IDE.

## Testing the Flow
To fully demonstrate the system, follow these steps:
1. **Register Users:** Register 4 accounts, one for each role (`VOTER`, `CANDIDATE`, `FIELD_OFFICER`, `ADMIN`).
2. **Voter Flow:** Login as the Voter, fill out the Voter ID application form.
3. **Field Officer Flow:** Login as the Field Officer, go to the dashboard, and `Approve` the pending voter application.
4. **Candidate Flow:** Login as the Candidate, fill out the candidacy application.
5. **Admin Flow:** Login as the Admin, go to "Manage Candidates" and `Approve` the candidate.
6. **Voting Flow:** Login as the Voter again. You can now access the "Vote" tab. Cast your vote for the approved candidate.
7. **Results Flow:** Login as the Admin, go to "Live Results" to see the vote tallied in real-time.

---
*Built as a comprehensive Java college project to demonstrate modern web application development.*
