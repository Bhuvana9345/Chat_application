package com.linguachat.service;

import com.linguachat.dto.UserResponse;
import com.linguachat.entity.User;
import com.linguachat.exception.AppException;
import com.linguachat.repository.MessageRepository;
import com.linguachat.repository.ReactionRepository;
import com.linguachat.repository.TranslationRepository;
import com.linguachat.repository.UserRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PresenceService presenceService;
    private final MessageRepository messageRepository;
    private final ReactionRepository reactionRepository;
    private final TranslationRepository translationRepository;

    public UserService(UserRepository userRepository,
                       PresenceService presenceService,
                       MessageRepository messageRepository,
                       ReactionRepository reactionRepository,
                       TranslationRepository translationRepository) {
        this.userRepository = userRepository;
        this.presenceService = presenceService;
        this.messageRepository = messageRepository;
        this.reactionRepository = reactionRepository;
        this.translationRepository = translationRepository;
    }

    public User getByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));
    }

    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));
    }

    public List<UserResponse> listUsers(String currentUsername, String search) {
        User current = getByUsername(currentUsername);
        List<User> users = (search == null || search.isBlank())
                ? userRepository.findByIdNotOrderByUsernameAsc(current.getId())
                : userRepository.findByUsernameContainingIgnoreCaseAndIdNot(search, current.getId());

        return users.stream().map(this::toResponse).toList();
    }

    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getPreferredLanguage(),
                presenceService.isOnline(user.getUsername())
        );
    }

    @Transactional
    public void deleteAccount(String username) {
        User user = getByUsername(username);
        reactionRepository.deleteForMessagesInvolvingUser(user.getId());
        reactionRepository.deleteByUserIdNative(user.getId());
        translationRepository.deleteForMessagesInvolvingUser(user.getId());
        messageRepository.deleteMessagesInvolvingUser(user.getId());
        userRepository.delete(user);
    }
}
