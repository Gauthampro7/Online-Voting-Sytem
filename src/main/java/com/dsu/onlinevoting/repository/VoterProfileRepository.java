package com.dsu.onlinevoting.repository;

import com.dsu.onlinevoting.model.VoterProfile;
import com.dsu.onlinevoting.model.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface VoterProfileRepository extends JpaRepository<VoterProfile, Long> {
    List<VoterProfile> findByStatus(VerificationStatus status);
}
