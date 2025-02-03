package app.web.controller;

import app.user.model.User;
import app.user.service.UserService;
import app.web.dto.UserEditRequest;
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

import java.util.UUID;

import static app.web.controller.IndexController.USER_ID_FROM_SESSION;

@Controller
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{id}/profile")
    public ModelAndView getProfileMenu(@PathVariable UUID id){
        User user = userService.getById(id);
        ModelAndView modelAndView = new ModelAndView("profile-menu");
        modelAndView.addObject("user", user);
        modelAndView.addObject("userEditRequest", DtoMapper.mapToUserEditRequest(user));
        return modelAndView;
    }

//    @GetMapping("/profile") //TODO When pass session topic
//    public ModelAndView getProfile(HttpSession session) {
//
//        UUID userId = (UUID) session.getAttribute(USER_ID_FROM_SESSION);
//        User user = userService.getById(userId);
//
//        ModelAndView modelAndView = new ModelAndView();
//        modelAndView.addObject("user", user);
//        modelAndView.addObject("userEditRequest", UserEditRequest.buildFromUser(user));
//        modelAndView.setViewName("profile-menu");
//
//        return modelAndView;
//    }

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
}
