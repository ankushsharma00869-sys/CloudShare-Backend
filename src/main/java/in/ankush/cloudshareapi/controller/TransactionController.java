package in.ankush.cloudshareapi.controller;

import in.ankush.cloudshareapi.document.PaymentTransaction;
import in.ankush.cloudshareapi.document.User;
import in.ankush.cloudshareapi.repository.PaymentTransactionRepository;
import in.ankush.cloudshareapi.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final UserService userService;

    public TransactionController(PaymentTransactionRepository paymentTransactionRepository, UserService userService) {
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<?> getUserTransaction(){
        User currentUser = userService.getCurrentUser();

        List<PaymentTransaction> transactions = paymentTransactionRepository
                .findByUserIdAndStatusOrderByTransactionDateDesc(currentUser.getId(), "SUCCESS");

        return ResponseEntity.ok(transactions);
    }
}
