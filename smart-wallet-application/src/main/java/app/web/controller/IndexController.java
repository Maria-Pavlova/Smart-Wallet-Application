package app.web.controller;

import app.user.model.User;
import app.user.service.UserService;
import app.web.dto.LoginRequest;
import app.web.dto.RegisterRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class IndexController {

    public static final String USER_ID_FROM_SESSION = "user_id";
    private final UserService userService;

    @GetMapping("/")
    public String getIndexPage() {
        return "index";
    }

    @GetMapping("/register")
    public ModelAndView getRegisterForm(){
        ModelAndView modelAndView = new ModelAndView("register");
        modelAndView.addObject("registerRequest", new RegisterRequest());
        return modelAndView;
    }

    @PostMapping("/register")
    public ModelAndView registerUser(@Valid RegisterRequest registerRequest,
                                     BindingResult bindingResult, HttpSession session){

        if (bindingResult.hasErrors()) {
            return new ModelAndView("register");
        }
        User registeredUser =
                userService.register(registerRequest);
        activateUserSession(session, registeredUser.getId());
        ModelAndView modelAndView = new ModelAndView("redirect:/login");
        modelAndView.addObject("user", registeredUser);
        return modelAndView;
    }

    @GetMapping("/login")
    public ModelAndView getLoginPage() {

        ModelAndView modelAndView = new ModelAndView("login");
        modelAndView.addObject("loginRequest", new LoginRequest());

        return modelAndView;
    }

    @PostMapping("/login")
    public ModelAndView login(@Valid LoginRequest loginRequest, BindingResult result, HttpSession session) {

        if (result.hasErrors()) {
            return new ModelAndView("login");
        }
        User loggedUser = userService.login(loginRequest);
        session.setAttribute(USER_ID_FROM_SESSION, loggedUser.getId());

        return getHomePageForUser(loggedUser);
    }



    @GetMapping("/home")
    public ModelAndView getHomePage(HttpSession session) {

        UUID userId = (UUID) session.getAttribute(USER_ID_FROM_SESSION);
        User user = userService.getById(userId);

        ModelAndView modelAndView = new ModelAndView("home");
        modelAndView.addObject("user", user);

        return modelAndView;

    }

    @GetMapping("/logout")
    public String getLogout(HttpSession session) {

        session.invalidate();
        return "redirect:/";
    }

    private ModelAndView getHomePageForUser(User user) {

        ModelAndView modelAndView = new ModelAndView("redirect:/home");
        modelAndView.addObject("user", user);

        return modelAndView;
    }

    private void activateUserSession(HttpSession session, UUID userId) {

        session.setAttribute(USER_ID_FROM_SESSION, userId);
        session.setMaxInactiveInterval(30*60);
    }
}
