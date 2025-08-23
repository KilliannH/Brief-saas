package com.killiann.briefsaas.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.killiann.briefsaas.entity.User;
import com.killiann.briefsaas.exception.NotFoundException;
import com.killiann.briefsaas.repository.UserRepository;
import com.killiann.briefsaas.service.StripeService;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.billingportal.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/stripe")
public class StripeController {

    private static final Logger log = LoggerFactory.getLogger(StripeController.class);

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
                ObjectMapper mapper = new ObjectMapper();
                Session session = null;

                // Tentative via désérialisation
                EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
                if (deserializer.getObject().isPresent()) {
                    Object obj = deserializer.getObject().get();
                    if (obj instanceof Session s) {
                        session = s;
                        log.info("✅ Session désérialisée : {}", session.getId());
                    }
                }

                // Fallback via JSON brut
                if (session == null) {
                    try {
                        String json = event.getData().getObject().toJson();
                        JsonNode node = mapper.readTree(json);
                        String sessionId = node.get("id").asText();
                        session = Session.retrieve(sessionId);
                        log.info("↩️ Session récupérée via fallback: {}", sessionId);
                    } catch (Exception e) {
                        log.error("❌ Impossible de récupérer la session via JSON", e);
                    }
                }

                // Mise à jour utilisateur
                if (session != null && session.getCustomerEmail() != null) {
                    Session finalSession = session;

                    if (session.getSubscription() != null) {
                        Subscription stripeSub = null;
                        try {
                            stripeSub = Subscription.retrieve(session.getSubscription());
                        } catch (StripeException e) {
                            throw new RuntimeException(e);
                        }

                        // Récupère l'ID du plan (priceId)
                        String priceId = stripeSub.getItems().getData().get(0).getPrice().getId();

                        userRepository.findByEmail(session.getCustomerEmail()).ifPresent(user -> {
                            user.setSubscriptionActive(true);
                            user.setStripeCustomerId(finalSession.getCustomer());
                            user.setStripeSubscriptionId(finalSession.getSubscription());
                            user.setCurrentPriceId(priceId);
                            userRepository.save(user);
                            log.info("✅ Utilisateur mis à jour : {}", user.getEmail());
                        });
                    } else {
                        log.warn("❗ Subscription null");
                    }
                } else {
                    log.warn("❗ Session null ou email manquant");
                }
            }

            case "customer.subscription.deleted" -> {
                ObjectMapper mapper = new ObjectMapper();
                Subscription subscription = null;

                // Tentative de désérialisation
                EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
                if (deserializer.getObject().isPresent()) {
                    Object obj = deserializer.getObject().get();
                    if (obj instanceof Subscription s) {
                        subscription = s;
                        log.info("✅ Subscription désérialisée : {}", s.getId());
                    }
                }

                // Fallback JSON brut
                if (subscription == null) {
                    try {
                        String json = event.getData().getObject().toJson();
                        JsonNode node = mapper.readTree(json);
                        String subscriptionId = node.get("id").asText();
                        subscription = Subscription.retrieve(subscriptionId);
                        log.info("↩️ Subscription récupérée via fallback : {}", subscriptionId);
                    } catch (Exception e) {
                        log.error("❌ Erreur fallback récupération Subscription", e);
                    }
                }

                // Mise à jour BDD
                if (subscription != null) {
                    String customerId = subscription.getCustomer();
                    userRepository.findByStripeCustomerId(customerId).ifPresent(user -> {
                        user.setSubscriptionActive(false);
                        user.setCurrentPriceId(null);
                        user.setStripeSubscriptionId(null);
                        userRepository.save(user);
                        log.info("🚫 Abonnement désactivé pour utilisateur {}", user.getEmail());
                    });
                } else {
                    log.warn("❗ Subscription toujours null après tentative de fallback");
                }
            }

            case "customer.subscription.updated" -> {
                ObjectMapper mapper = new ObjectMapper();
                Subscription subscription = null;

                // Tentative de désérialisation directe
                EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
                if (deserializer.getObject().isPresent()) {
                    Object obj = deserializer.getObject().get();
                    if (obj instanceof Subscription s) {
                        subscription = s;
                        log.info("✅ Subscription désérialisée (update) : {}", s.getId());
                    }
                }

                // Fallback via JSON brut
                if (subscription == null) {
                    try {
                        String json = event.getData().getObject().toJson();
                        JsonNode node = mapper.readTree(json);
                        String subscriptionId = node.get("id").asText();
                        subscription = Subscription.retrieve(subscriptionId);
                        log.info("↩️ Subscription récupérée via fallback (update) : {}", subscriptionId);
                    } catch (Exception e) {
                        log.error("❌ Erreur fallback récupération Subscription (update)", e);
                    }
                }

                // Mise à jour de l’utilisateur
                if (subscription != null) {
                    String stripeCustomerId = subscription.getCustomer();
                    boolean cancelAtPeriodEnd = subscription.getCancelAtPeriodEnd();
                    boolean active = "active".equals(subscription.getStatus());

                    Subscription finalSubscription = subscription;
                    userRepository.findByStripeCustomerId(stripeCustomerId).ifPresent(user -> {
                        user.setSubscriptionActive(active);
                        user.setCancelAtPeriodEnd(cancelAtPeriodEnd);
                        user.setSubscriptionEndAt(
                                Instant.ofEpochSecond(finalSubscription.getCurrentPeriodEnd())
                        );
                        user.setCurrentPriceId(finalSubscription.getItems().getData().get(0).getPrice().getId()); // facultatif
                        user.setStripeSubscriptionId(finalSubscription.getId());
                        userRepository.save(user);

                        log.info("🔄 Utilisateur {} mis à jour (update)", user.getEmail());
                    });
                } else {
                    log.warn("❗ Subscription toujours null après fallback (update)");
                }
            }
        }

        return ResponseEntity.ok("Webhook reçu");
    }

    @PostMapping("/portal")
    public ResponseEntity<Map<String, String>> createPortal(@AuthenticationPrincipal User user) throws StripeException {
        SessionCreateParams params = SessionCreateParams.builder()
                .setCustomer(user.getStripeCustomerId())
                .setReturnUrl("https://brief-mate.com/account")
                .build();

        com.stripe.model.billingportal.Session portalSession = com.stripe.model.billingportal.Session.create(params);
        String url = portalSession.getUrl();

        return ResponseEntity.ok(Map.of("url", url));
    }
}