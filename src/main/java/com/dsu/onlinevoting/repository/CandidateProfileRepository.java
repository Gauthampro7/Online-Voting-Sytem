package com.dsu.onlinevoting.repository;

import com.dsu.onlinevoting.model.CandidateProfile;
import com.dsu.onlinevoting.model.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CandidateProfileRepository extends JpaRepository<CandidateProfile, Long> {
    List<CandidateProfile> findByStatus(VerificationStatus status);
    List<CandidateProfile> findByConstituencyAndStatus(String constituency, VerificationStatus status);
}
