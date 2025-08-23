package com.killiann.briefsaas.service;

import com.killiann.briefsaas.entity.User;
import com.killiann.briefsaas.exception.NotFoundException;
import com.killiann.briefsaas.repository.ClientRepository;
import com.killiann.briefsaas.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ClientRepository clientRepository;

    public User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    @Transactional
    public void delete(User user) {
        if (!userRepository.existsById(user.getId())) {
            throw new NotFoundException("User not found");
        }
        clientRepository.deleteByOwner(user);
        userRepository.deleteById(user.getId());
    }
}