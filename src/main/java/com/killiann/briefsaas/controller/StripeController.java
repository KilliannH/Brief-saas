package com.killiann.briefsaas.controller;

import com.killiann.briefsaas.entity.User;
import com.killiann.briefsaas.repository.UserRepository;
import com.killiann.briefsaas.service.StripeService;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.SubscriptionUpdateParams;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/stripe")
public class StripeController {

    @Value("${stripe.webhook.secret}")
    private String endpointSecret;

    private final StripeService stripeService;
    private final UserRepository userRepository;

    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(Authentication auth, @RequestBody Map<String, String> req) {
        try {
            User user = userRepository.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

            String priceId = req.get("priceId");
            String sessionUrl = stripeService.createCheckoutSession(user.getEmail(), priceId);

            return ResponseEntity.ok(Map.of("url", sessionUrl));
        } catch (StripeException e) {
            return ResponseEntity.status(500).body("Erreur Stripe : " + e.getMessage());
        }
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleStripeWebhook(@RequestBody String payload,
                                                      @RequestHeader("Stripe-Signature") String sigHeader) {
        Event event;

        try {
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Signature invalide");
        }

        switch (event.getType()) {

            case "checkout.session.completed" -> {
                Session session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
                if (session != null && session.getCustomerEmail() != null) {
                    userRepository.findByEmail(session.getCustomerEmail()).ifPresent(user -> {
                        user.setSubscriptionActive(true);
                        user.setStripeCustomerId(session.getCustomer());
                        user.setStripeSubscriptionId(session.getSubscription());
                        userRepository.save(user);
                    });
                }
            }

            case "customer.subscription.deleted" -> {
                Subscription subscription = (Subscription) event.getDataObjectDeserializer().getObject().orElse(null);
                if (subscription != null) {
                    String customerId = subscription.getCustomer();
                    userRepository.findByStripeCustomerId(customerId).ifPresent(user -> {
                        user.setSubscriptionActive(false);
                        user.setStripeSubscriptionId(null);
                        userRepository.save(user);
                    });
                }
            }
        }

        return ResponseEntity.ok("Webhook reçu");
    }

    @PostMapping("/upgrade")
    public ResponseEntity<?> upgradePlan(Authentication auth, @RequestBody Map<String, String> req) {
        try {
            User user = userRepository.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (user.getStripeSubscriptionId() == null) {
                return ResponseEntity.badRequest().body("Aucun abonnement actif.");
            }

            String newPriceId = req.get("priceId");

            Subscription subscription = Subscription.retrieve(user.getStripeSubscriptionId());

            SubscriptionUpdateParams params = SubscriptionUpdateParams.builder()
                    .setCancelAtPeriodEnd(false)
                    .addItem(SubscriptionUpdateParams.Item.builder()
                            .setId(subscription.getItems().getData().get(0).getId())
                            .setPrice(newPriceId)
                            .build())
                    .build();

            subscription.update(params);

            return ResponseEntity.ok("Abonnement mis à jour.");
        } catch (StripeException e) {
            return ResponseEntity.status(500).body("Erreur Stripe : " + e.getMessage());
        }
    }
}