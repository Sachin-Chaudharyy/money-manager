package com.project.moneymanager.controller;

import com.project.moneymanager.dto.AuthDTO;
import com.project.moneymanager.dto.ProfileDTO;
import com.project.moneymanager.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping
public class ProfileController {

    private final ProfileService profileService;

    @Value("${money.manager.frontend.url}")
    private String frontendUrl;

    @PostMapping("/register")
    public ResponseEntity<ProfileDTO> registerProfile(@RequestBody ProfileDTO profileDTO) {
        ProfileDTO registeredProfile = profileService.registerProfile(profileDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(registeredProfile);
    }

    @GetMapping("/activate")
    public ResponseEntity<Void> activateProfile(@RequestParam String token) {
        boolean isActivated = profileService.activateProfile(token);
        String redirectUrl = isActivated
                ? frontendUrl + "login?activated=true"
                : frontendUrl + "/login?activated=false";
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(redirectUrl));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody AuthDTO authDTO) {
        try{
            if(!profileService.isAccoutActive(authDTO.getEmail())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                        "message", "Account is not active. Please activate your account first."
                ));
            }
            Map<String, Object> response = profileService.authenticateAndGenerateToken(authDTO);
            return ResponseEntity.ok(response);
        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "message" , e.getMessage()
            ));
        }
    }

    @GetMapping("/test")
    public String test() {
        return "test sucks";
    }

    @GetMapping("/profile")
    public ResponseEntity<ProfileDTO> getPublicProfile() {
        ProfileDTO profileDTO = profileService.getPublicProfile(null);
        return  ResponseEntity.status(HttpStatus.OK).body(profileDTO);
    }
}
