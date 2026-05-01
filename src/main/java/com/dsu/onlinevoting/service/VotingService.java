package com.dsu.onlinevoting.service;

import com.dsu.onlinevoting.model.User;
import com.dsu.onlinevoting.model.Vote;
import com.dsu.onlinevoting.model.CandidateProfile;
import com.dsu.onlinevoting.model.VerificationStatus;
import com.dsu.onlinevoting.repository.VoteRepository;
import com.dsu.onlinevoting.repository.CandidateProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class VotingService {

    private final VoteRepository voteRepository;
    private final CandidateProfileRepository candidateProfileRepository;

    public VotingService(VoteRepository voteRepository, CandidateProfileRepository candidateProfileRepository) {
        this.voteRepository = voteRepository;
        this.candidateProfileRepository = candidateProfileRepository;
    }

    public boolean hasVoted(User voter) {
        return voteRepository.findByVoter(voter).isPresent();
    }

    @Transactional
    public boolean castVote(User voter, User candidate) {
        if (!hasVoted(voter)) {
            Vote vote = new Vote(voter, candidate);
            voteRepository.save(vote);
            return true;
        }
        return false;
    }

    public List<CandidateProfile> getApprovedCandidatesForConstituency(String constituency) {
        return candidateProfileRepository.findByConstituencyAndStatus(constituency, VerificationStatus.APPROVED);
    }
    
    public List<CandidateProfile> getAllApprovedCandidates() {
        return candidateProfileRepository.findByStatus(VerificationStatus.APPROVED);
    }

    public long getVoteCountForCandidate(User candidate) {
        return voteRepository.countByCandidate(candidate);
    }

    public Map<String, Long> getResults() {
        List<CandidateProfile> approvedCandidates = candidateProfileRepository.findByStatus(VerificationStatus.APPROVED);
        return approvedCandidates.stream()
                .collect(Collectors.toMap(
                        c -> c.getUser().getName() + " (" + c.getParty() + ")",
                        c -> voteRepository.countByCandidate(c.getUser())
                ));
    }
}
