package in.ankush.cloudshareapi.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "payment-transactions")
public class PaymentTransaction {

    @Id
    private String id;
    private String userId;

    // ✅ FIX: orderId pe index lagao - findByOrderId fast hoga
    @Indexed
    private String orderId;

    private String paymentId;
    private String planId;
    private int amount;
    private String currency;
    private int creditsAdded;
    private String status;
    private LocalDateTime transactionDate;
    private String userEmail;
    private String userName;
}
