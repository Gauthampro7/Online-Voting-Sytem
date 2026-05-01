package com.dsu.onlinevoting.service;

import com.dsu.onlinevoting.model.*;
import com.dsu.onlinevoting.repository.UserRepository;
import com.dsu.onlinevoting.repository.VoterProfileRepository;
import com.dsu.onlinevoting.repository.CandidateProfileRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final VoterProfileRepository voterProfileRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, VoterProfileRepository voterProfileRepository, CandidateProfileRepository candidateProfileRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.voterProfileRepository = voterProfileRepository;
        this.candidateProfileRepository = candidateProfileRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User registerUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    @Transactional
    public void submitVoterApplication(String username, String address, String constituency, String identityDocumentPath) {
        User user = findByUsername(username);
        if (user != null && user.getRole() == Role.VOTER) {
            VoterProfile profile = new VoterProfile();
            profile.setUser(user);
            profile.setAddress(address);
            profile.setConstituency(constituency);
            profile.setIdentityDocumentPath(identityDocumentPath);
            profile.setStatus(VerificationStatus.PENDING);
            voterProfileRepository.save(profile);
        }
    }

    @Transactional
    public void submitCandidateApplication(String username, String party, String constituency, String manifesto) {
        User user = findByUsername(username);
        if (user != null && user.getRole() == Role.CANDIDATE) {
            CandidateProfile profile = new CandidateProfile();
            profile.setUser(user);
            profile.setParty(party);
            profile.setConstituency(constituency);
            profile.setManifesto(manifesto);
            profile.setStatus(VerificationStatus.PENDING);
            candidateProfileRepository.save(profile);
        }
    }

    public List<VoterProfile> getPendingVoterApplications() {
        return voterProfileRepository.findByStatus(VerificationStatus.PENDING);
    }

    public List<CandidateProfile> getPendingCandidateApplications() {
        return candidateProfileRepository.findByStatus(VerificationStatus.PENDING);
    }

    @Transactional
    public void updateVoterStatus(Long profileId, VerificationStatus status) {
        voterProfileRepository.findById(profileId).ifPresent(profile -> {
            profile.setStatus(status);
            voterProfileRepository.save(profile);
        });
    }

    @Transactional
    public void updateCandidateStatus(Long profileId, VerificationStatus status) {
        candidateProfileRepository.findById(profileId).ifPresent(profile -> {
            profile.setStatus(status);
            candidateProfileRepository.save(profile);
        });
    }
}
