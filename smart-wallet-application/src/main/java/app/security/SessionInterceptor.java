package app.security;

import app.user.model.User;
import app.user.model.UserRole;
import app.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;
import java.util.UUID;

@Component
public class SessionInterceptor implements HandlerInterceptor {

    public static final Set<String> UNAUTHENTICATED_ENDPOINTS = Set.of("/login", "/register", "/", "/error");
    public static final String USER_ID_FROM_SESSION = "user_id";

    @Autowired
    private UserService userService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String endpoint = request.getServletPath();
        if (UNAUTHENTICATED_ENDPOINTS.contains(endpoint)) {
            // no session
            return true;
        }

        if (request.getSession(false) == null || request.getSession(false).getAttribute(USER_ID_FROM_SESSION) == null) {
            response.sendRedirect("/login");
            return false;
        }

        HttpSession session = request.getSession(false);
        UUID userIdFromSession = (UUID) session.getAttribute(USER_ID_FROM_SESSION);
        User user = userService.getById(userIdFromSession);

        if (!user.isActive()) {

            session.invalidate();
            response.sendRedirect("/");
            return false;
        }



        return true;
    }
}
