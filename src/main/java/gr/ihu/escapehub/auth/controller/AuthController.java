package gr.ihu.escapehub.auth.controller;

import java.security.Principal;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("demo")
public class AuthController {
    @GetMapping("/api/csrf")
    public CsrfToken csrf(CsrfToken token) { return token; }
    @GetMapping("/api/me")
    public Map<String, String> me(Principal principal) { return Map.of("username", principal.getName()); }
}
