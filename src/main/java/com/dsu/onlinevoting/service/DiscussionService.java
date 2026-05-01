package com.dsu.onlinevoting.service;

import com.dsu.onlinevoting.model.DiscussionPost;
import com.dsu.onlinevoting.model.User;
import com.dsu.onlinevoting.repository.DiscussionPostRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DiscussionService {

    private final DiscussionPostRepository repository;

    public DiscussionService(DiscussionPostRepository repository) {
        this.repository = repository;
    }

    public List<DiscussionPost> getAllPosts() {
        return repository.findAllByOrderByTimestampDesc();
    }

    @Transactional
    public void addPost(User user, String content) {
        repository.save(new DiscussionPost(user, content));
    }
}
