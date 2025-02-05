package app.web.controller;

import app.subscription.service.SubscriptionService;
import app.user.model.User;
import app.user.service.UserService;
import app.web.dto.SubscriptionHistory;
import app.web.dto.UpgradeOption;
import app.web.dto.UpgradeRequest;
import app.web.dto.UpgradeResult;
import app.web.mapper.DtoMapper;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static app.security.SessionInterceptor.USER_ID_FROM_SESSION;

@Controller
@RequiredArgsConstructor
@RequestMapping("/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    private final UserService userService;

    @GetMapping("/history")
    public ModelAndView getUserSubscriptions(HttpSession session) {

        UUID userId = (UUID) session.getAttribute(USER_ID_FROM_SESSION);
        User user = userService.getById(userId);

        List<SubscriptionHistory> subscriptionHistory = subscriptionService.getHistory(user.getId())
                .stream()
                .map(DtoMapper::toSubscriptionHistory)
                .toList();

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject("user", user);
        modelAndView.addObject("historyList", subscriptionHistory);
        modelAndView.setViewName("subscription-history");

        return modelAndView;
    }

    @GetMapping
    public ModelAndView getUpgradePage(HttpSession session) {

        UUID userId = (UUID) session.getAttribute(USER_ID_FROM_SESSION);
        User user = userService.getById(userId);

        Map<String, UpgradeOption> upgradeOptions = subscriptionService.getUpgradeOptions(user.getId());

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject("user", user);
        modelAndView.addObject("upgradeOptions", upgradeOptions);
        modelAndView.addObject("upgradeRequest", UpgradeRequest.builder().build());
        modelAndView.setViewName("upgrade");

        return modelAndView;
    }

    @PostMapping
    public ModelAndView upgrade(UpgradeRequest request, HttpSession session) {

        UUID userId = (UUID) session.getAttribute(USER_ID_FROM_SESSION);
        User user = userService.getById(userId);

        UpgradeResult upgradeResult = subscriptionService.upgrade(user, request);

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject("user", user);
        modelAndView.addObject("upgradeResult", upgradeResult);
        modelAndView.setViewName("upgrade-result");

        return modelAndView;
    }

}

