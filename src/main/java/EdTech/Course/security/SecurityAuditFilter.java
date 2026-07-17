package EdTech.Course.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class SecurityAuditFilter extends OncePerRequestFilter {

    private static final Logger auditLogger = LoggerFactory.getLogger(SecurityAuditFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated() && !authentication.getPrincipal().equals("anonymousUser")) {
            String username = authentication.getName(); // Usually mapped to the 'sub' claim in JWT
            String authorities = authentication.getAuthorities().toString();
            String method = request.getMethod();
            String uri = request.getRequestURI();

            auditLogger.info("SECURITY_AUDIT: User '{}' with authorities '{}' accessed {} {}", 
                    username, authorities, method, uri);
        }

        filterChain.doFilter(request, response);
    }
}
