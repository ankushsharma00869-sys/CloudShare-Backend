package in.ankush.cloudshareapi.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import in.ankush.cloudshareapi.document.PaymentTransaction;
import in.ankush.cloudshareapi.document.User;
import in.ankush.cloudshareapi.dto.PaymentDTO;
import in.ankush.cloudshareapi.dto.PaymentVerificationDTO;
import in.ankush.cloudshareapi.repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final UserService userService;
    private final UserCreditsService userCreditsService;
    private final PaymentTransactionRepository paymentTransactionRepository;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorPayKeySecret;

    public PaymentDTO createOrder(PaymentDTO paymentDTO) {
        try {
            User currentUser = userService.getCurrentUser();
            String userId = currentUser.getId();

            RazorpayClient razorpayClient = new RazorpayClient(razorpayKeyId, razorPayKeySecret);

            // ✅ Amount backend decide karta hai (secure)
            int amount = 0;
            switch (paymentDTO.getPlanId()) {
                case "premium":
                    amount = 50000;   // ₹500
                    break;
                case "ultimate":
                    amount = 250000;  // ₹2500
                    break;
                default:
                    throw new RuntimeException("Invalid plan: " + paymentDTO.getPlanId());
            }

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amount);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "order_" + System.currentTimeMillis());

            Order order = razorpayClient.orders.create(orderRequest);
            String orderId = order.get("id").toString();

            PaymentTransaction transaction = PaymentTransaction.builder()
                    .userId(userId)
                    .orderId(orderId)
                    .planId(paymentDTO.getPlanId())
                    .amount(amount)
                    .status("PENDING")
                    .transactionDate(LocalDateTime.now())
                    .userEmail(currentUser.getEmail())
                    .userName(currentUser.getFirstName() + " " + currentUser.getLastName())
                    .build();

            paymentTransactionRepository.save(transaction);

            // ✅ KEY FIX: razorpayKeyId response mein bhejo
            // Frontend pe hardcode mat karo - yahan se lo
            return PaymentDTO.builder()
                    .orderId(orderId)
                    .amount(amount)
                    .razorpayKeyId(razorpayKeyId)
                    .success(true)
                    .message("Order created successfully")
                    .build();

        } catch (Exception e) {
            e.printStackTrace();
            return PaymentDTO.builder()
                    .success(false)
                    .message("Error creating order: " + e.getMessage())
                    .build();
        }
    }

    public PaymentDTO verifyPayment(PaymentVerificationDTO request) {
        try {
            User currentUser = userService.getCurrentUser();
            String userId = currentUser.getId();

            String data = request.getRazorpay_order_id() + "|" + request.getRazorpay_payment_id();
            String generatedSignature = generateHmacSha256Signature(data, razorPayKeySecret);

            if (!generatedSignature.equals(request.getRazorpay_signature())) {
                updateTransactionStatus(request.getRazorpay_order_id(), "FAILED",
                        request.getRazorpay_payment_id(), null);
                return PaymentDTO.builder()
                        .success(false)
                        .message("Payment signature verification failed")
                        .build();
            }

            int creditsToAdd = 0;
            String plan = "BASIC";

            switch (request.getPlanId()) {
                case "premium":
                    creditsToAdd = 500;
                    plan = "PREMIUM";
                    break;
                case "ultimate":
                    creditsToAdd = 5000;
                    plan = "ULTIMATE";
                    break;
            }

            if (creditsToAdd > 0) {
                userCreditsService.addCredits(userId, creditsToAdd, plan);
                updateTransactionStatus(request.getRazorpay_order_id(), "SUCCESS",
                        request.getRazorpay_payment_id(), creditsToAdd);

                return PaymentDTO.builder()
                        .success(true)
                        .message("Payment verified and credits added successfully")
                        .credits(userCreditsService.getUserCredits(userId).getCredits())
                        .build();
            } else {
                updateTransactionStatus(request.getRazorpay_order_id(), "FAILED",
                        request.getRazorpay_payment_id(), null);
                return PaymentDTO.builder()
                        .success(false)
                        .message("Invalid plan selected")
                        .build();
            }

        } catch (Exception e) {
            try {
                updateTransactionStatus(request.getRazorpay_order_id(), "ERROR",
                        request.getRazorpay_payment_id(), null);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return PaymentDTO.builder()
                    .success(false)
                    .message("Error verifying payment: " + e.getMessage())
                    .build();
        }
    }

    private void updateTransactionStatus(String orderId, String status,
                                          String paymentId, Integer creditsToAdd) {
        // ✅ FIX: findAll() ki jagah findByOrderId (fast & efficient)
        paymentTransactionRepository.findByOrderId(orderId)
                .ifPresent(transaction -> {
                    transaction.setStatus(status);
                    transaction.setPaymentId(paymentId);
                    if (creditsToAdd != null) {
                        transaction.setCreditsAdded(creditsToAdd);
                    }
                    paymentTransactionRepository.save(transaction);
                });
    }

    private String generateHmacSha256Signature(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(data.getBytes());

            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                String s = Integer.toHexString(0xff & b);
                if (s.length() == 1) hex.append('0');
                hex.append(s);
            }
            return hex.toString();

        } catch (Exception e) {
            throw new RuntimeException("Error generating signature", e);
        }
    }
}
