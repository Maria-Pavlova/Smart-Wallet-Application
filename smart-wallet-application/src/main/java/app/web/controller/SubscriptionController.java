package app.web.controller;

import app.user.model.User;
import app.user.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.UUID;

@Controller
@RequestMapping("/subscriptions")
public class SubscriptionController {

    private final UserService userService;

    public SubscriptionController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String getUpgradePage() {
        return "upgrade";
    }

    @GetMapping("/history")
    public ModelAndView getUserSubscriptions() {

        User user = userService.getById(UUID.fromString("4a30406f-6862-4a7d-98b4-9ecea741a23a"));
        ModelAndView modelAndView = new ModelAndView("subscription-history");
        modelAndView.addObject("user", user);

        return modelAndView;
    }

}

