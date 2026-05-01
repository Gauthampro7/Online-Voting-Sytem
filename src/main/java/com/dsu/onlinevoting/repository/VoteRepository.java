package com.dsu.onlinevoting.repository;

import com.dsu.onlinevoting.model.Vote;
import com.dsu.onlinevoting.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface VoteRepository extends JpaRepository<Vote, Long> {
    Optional<Vote> findByVoter(User voter);
    List<Vote> findByCandidate(User candidate);
    long countByCandidate(User candidate);
}
