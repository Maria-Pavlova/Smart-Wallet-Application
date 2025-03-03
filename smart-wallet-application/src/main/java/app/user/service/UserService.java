package app.user.service;

import app.exception.DomainException;
import app.subscription.model.Subscription;
import app.subscription.service.SubscriptionService;
import app.user.model.User;
import app.user.model.UserRole;
import app.user.repository.UserRepository;
import app.wallet.service.WalletService;
import app.web.dto.LoginRequest;
import app.web.dto.RegisterRequest;
import app.web.dto.UserEditRequest;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SubscriptionService subscriptionService;
    private final WalletService walletService;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, SubscriptionService subscriptionService, WalletService walletService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.subscriptionService = subscriptionService;
        this.walletService = walletService;
    }

    @Transactional
    public User register(RegisterRequest registerRequest) {
        Optional<User> userOptional = userRepository.findByUsername(registerRequest.getUsername());

        if (userOptional.isPresent()) {
            throw new DomainException("Username [%s} already exist!".formatted(registerRequest.getUsername()));
        }
        User user = initialiseUser(registerRequest);
       subscriptionService.createDefaultDescription(user);
        walletService.creatNewWallet(user);
        userRepository.save(user);
        log.info("Registered user with username [%s]".formatted(user.getUsername()));
        return user;

    }

    public User login(LoginRequest loginRequest) {
        Optional<User> optionalUser = userRepository.findByUsername(loginRequest.getUsername());
        if (optionalUser.isEmpty()) {
            throw new DomainException("Username or password are incorrect!");
        }
        User user = optionalUser.get();
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new DomainException("Username or password are incorrect!");
        }
        return user;
    }

    public User editUser(UUID userId, UserEditRequest userEditRequest) {

        User user = getById(userId);

        user.setFirstName(userEditRequest.getFirstName().trim());
        user.setLastName(userEditRequest.getLastName().trim());
        user.setEmail(userEditRequest.getEmail().trim());
        user.setProfilePicture(userEditRequest.getProfilePicture().trim());
        user.setUpdatedOn(LocalDateTime.now());

        return userRepository.save(user);
    }

    private User initialiseUser(RegisterRequest request) {
        return User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .country(request.getCountry())
                .role(UserRole.USER)
                .isActive(true)
                .createdOn(LocalDateTime.now())
                .updatedOn(LocalDateTime.now())
                .build();
    }

    public User getById(UUID id) {
        return userRepository.findById(id).orElseThrow(()-> new DomainException("User with id [%s] does not exist"));
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public void switchStatus(UUID id) {
        User user = getById(id);
        user.setActive(!user.isActive());
        user.setUpdatedOn(LocalDateTime.now());
        userRepository.save(user);
    }

    public void switchRole(UUID id) {
        User user = getById(id);
        if (user.getRole() == UserRole.USER){
            user.setRole(UserRole.ADMIN);
        }
        if (user.getRole() == UserRole.ADMIN){
            user.setRole(UserRole.USER);
        }
        user.setUpdatedOn(LocalDateTime.now());
        userRepository.save(user);
    }
}
