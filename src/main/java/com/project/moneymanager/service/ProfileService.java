package com.project.moneymanager.service;

import com.project.moneymanager.dto.AuthDTO;
import com.project.moneymanager.dto.ProfileDTO;
import com.project.moneymanager.entity.ProfileEntity;
import com.project.moneymanager.repository.ProfileRepository;
import com.project.moneymanager.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.UUID;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.http.javanet.NetHttpTransport;
import java.security.GeneralSecurityException;
import java.io.IOException;
import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileService {

//    @Autowired
    private final ProfileRepository profileRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @Value("${app.activation.url}")
    private String activationURL;

    @Value("${google.oauth.client-id}")
    private String googleClientId;

    public ProfileDTO registerProfile(ProfileDTO profileDTO) {
        if (profileRepository.findByEmail(profileDTO.getEmail()).isPresent()) {
            throw new RuntimeException("Email already registered");
        }
        ProfileEntity newProfile = toEntity(profileDTO);
        newProfile.setActivationToken(UUID.randomUUID().toString());
        newProfile = profileRepository.save(newProfile);

        String activationLink = activationURL + "/api/v1.0/activate?token=" + newProfile.getActivationToken();
        emailService.sendEmail(
                newProfile.getEmail(),
                "Activate your Money Manager account",
                "Click on the following link to activate your Money Manager account: " + activationLink
        );

        return toDTO(newProfile);
    }

    public Map<String, Object> authenticateWithGoogle(String idToken) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(), GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken googleIdToken = verifier.verify(idToken);
            if (googleIdToken == null) {
                throw new RuntimeException("Invalid Google token");
            }

            GoogleIdToken.Payload payload = googleIdToken.getPayload();
            String email = payload.getEmail();
            String fullName = (String) payload.get("name");
            String pictureUrl = (String) payload.get("picture");

            ProfileEntity profile = profileRepository.findByEmail(email)
                    .orElseGet(() -> {
                        ProfileEntity newProfile = ProfileEntity.builder()
                                .fullName(fullName)
                                .email(email)
                                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                                .profileImageUrl(pictureUrl)
                                .build();
                        newProfile.setIsActive(true); // Google already verified the email, active right away
                        ProfileEntity savedProfile = profileRepository.save(newProfile);

                        String subject = "Welcome to Money Manager!";
                        String body = "Hi " + savedProfile.getFullName() + ",<br><br>"
                                + "Welcome to Money Manager! We're excited to have you onboard.<br><br>"
                                + "Start tracking your income and expenses today and take control of your finances.<br><br>"
                                + "Best regards,<br>Money Manager Team";
                        sendEmailSafely(savedProfile.getEmail(), subject, body);

                        return savedProfile;
                    });

            String token = jwtUtil.generateToken(profile.getEmail());
            return Map.of(
                    "token", token,
                    "user", toDTO(profile)
            );

        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException("Google authentication failed: " + e.getMessage());
        }
    }

    public ProfileEntity toEntity(ProfileDTO profileDTO) {
        return ProfileEntity.builder()
                .id(profileDTO.getId())
                .fullName(profileDTO.getFullName())
                .email(profileDTO.getEmail())
                .password(passwordEncoder.encode(profileDTO.getPassword()))
                .profileImageUrl(profileDTO.getProfileImageUrl())
                .createdAt(profileDTO.getCreatedAt())
                .updatedAt(profileDTO.getUpdatedAt())
                .build();
    }

    public ProfileDTO toDTO(ProfileEntity profileEntity) {
        return ProfileDTO.builder()
                .id(profileEntity.getId())
                .fullName(profileEntity.getFullName())
                .email(profileEntity.getEmail())
                .profileImageUrl(profileEntity.getProfileImageUrl())
                .createdAt(profileEntity.getCreatedAt())
                .updatedAt(profileEntity.getUpdatedAt())
                .build();

    }

    private void sendEmailSafely(String to, String subject, String body) {
        try {
            emailService.sendEmail(to, subject, body);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    public boolean activateProfile(String activationToken) {
        return profileRepository.findByActivationToken(activationToken)
                .map(profile -> {
                    profile.setIsActive(true);
                    profileRepository.save(profile);

                    // send welcome email now that the account is actually active
                    String subject = "Welcome to Money Manager!";
                    String body = "Hi " + profile.getFullName() + ",<br><br>"
                            + "Your Money Manager account is now active! We're excited to have you onboard.<br><br>"
                            + "Start tracking your income and expenses today and take control of your finances.<br><br>"
                            + "Best regards,<br>Money Manager Team";
                    sendEmailSafely(profile.getEmail(), subject, body);

                    return true;
                })
                .orElse(false);
    }

    public boolean isAccoutActive(String email){
        return profileRepository.findByEmail(email)
                .map(ProfileEntity::getIsActive)
                .orElse(false);
    }

    public ProfileEntity getCurrentProfile(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return profileRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException("Profile not found with email: "+ authentication.getName()));
    }

    public ProfileDTO getPublicProfile(String email){
        ProfileEntity currentUser = null;
        if(email == null){
            currentUser = getCurrentProfile();
        }else {
            currentUser  = profileRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("Profile not found with email: "+ email));
        }
        return toDTO(currentUser);
    }

    public Map<String, Object> authenticateAndGenerateToken(AuthDTO authDTO) {
        try{
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(authDTO.getEmail(), authDTO.getPassword()));
            String token = jwtUtil.generateToken(authDTO.getEmail());
            return Map.of(
                    "token", token,
                    "user", getPublicProfile(authDTO.getEmail())
                    );
        }catch(Exception e){
            throw new RuntimeException("Invalid email or password");
        }
    }
}
