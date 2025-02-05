package app.web.controller;

import app.transaction.model.Transaction;
import app.transaction.service.TransactionService;
import app.user.model.User;
import app.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final UserService userService;

    @Autowired
    public TransactionController(TransactionService transactionService, UserService userService) {
        this.transactionService = transactionService;
        this.userService = userService;
    }

    @GetMapping
    public ModelAndView getAllTransactions() {

        List<Transaction> transactions = transactionService.getAllByOwnerId(UUID.fromString("a97fdd4f-b7fd-40ce-9943-1be1d022a966"));
        ModelAndView modelAndView = new ModelAndView("transactions");
        modelAndView.addObject("transactions", transactions);

        return modelAndView;
    }
}
