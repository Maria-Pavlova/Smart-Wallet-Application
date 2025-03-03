package app.web.controller;

import app.security.RequireAdminRole;
import app.security.SessionInterceptor;
import app.user.model.User;
import app.user.service.UserService;
import app.web.dto.UserEditRequest;
import app.web.dto.UserInformation;
import app.web.mapper.DtoMapper;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.UUID;

import static app.web.controller.IndexController.USER_ID_FROM_SESSION;

@Controller
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

//    @GetMapping("/{id}/profile")
//    public ModelAndView getProfileMenu(@PathVariable UUID id){
//        User user = userService.getById(id);
//        ModelAndView modelAndView = new ModelAndView("profile-menu");
//        modelAndView.addObject("user", user);
//        modelAndView.addObject("userEditRequest", DtoMapper.mapToUserEditRequest(user));
//        return modelAndView;
//    }

    @GetMapping("/profile")
    public ModelAndView getProfile(HttpSession session) {

        UUID userId = (UUID) session.getAttribute(USER_ID_FROM_SESSION);
        User user = userService.getById(userId);

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject("user", user);
        modelAndView.addObject("userEditRequest", DtoMapper.toUserEditRequest(user));
        modelAndView.setViewName("profile-menu");

        return modelAndView;
    }

    @PutMapping("/profile")
    public ModelAndView updateProfile(HttpSession session, @Valid UserEditRequest userEditRequest, BindingResult bindingResult) {

        UUID userId = (UUID) session.getAttribute(USER_ID_FROM_SESSION);
        User user = userService.editUser(userId, userEditRequest);

        if (bindingResult.hasErrors()) {
            ModelAndView modelAndView = new ModelAndView();
            modelAndView.addObject("user", user);
            modelAndView.addObject("userEditRequest", userEditRequest);
            modelAndView.setViewName("profile-menu");
            return modelAndView;
        }

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject("user", user);
        modelAndView.setViewName("redirect:/home");

        return modelAndView;
    }

    @GetMapping
    @RequireAdminRole
    public ModelAndView getAllUsers(HttpSession session) {

        UUID userId = (UUID) session.getAttribute(SessionInterceptor.USER_ID_FROM_SESSION);
        User loggedUser = userService.getById(userId);

        List<UserInformation> users = userService.getAllUsers().stream()
                .map(DtoMapper::toUserInformation)
                .toList();

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject("user", loggedUser);
        modelAndView.addObject("users", users);
        modelAndView.setViewName("users");

        return modelAndView;
    }

    @PutMapping("/{id}/status")
    @RequireAdminRole
    public String switchUserStatus(@PathVariable UUID id) {

        userService.switchStatus(id);

        return "redirect:/users";
    }

    @PutMapping("/{id}/role")
    @RequireAdminRole
    public String switchUserRole(@PathVariable UUID id) {

        userService.switchRole(id);

        return "redirect:/users";
    }
}
