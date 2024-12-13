package com.crm.rdvision.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentLink;
import com.stripe.model.Price;
import com.stripe.model.Product;
import com.stripe.model.checkout.Session;
import com.stripe.param.PaymentLinkCreateParams;
import com.stripe.param.PriceCreateParams;
import com.stripe.param.ProductCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.stereotype.Service;

@Service
public class StripeService {

    private final String testApiKey = "sk_test_51KpHlnSAxOboMMomFsqqmBj2Edg2GwjVKLMCY0KWcvWrpufIoMKXptuXcA9VXAqbbCANe7azFPk42zR1MMmhQYek00AuZVnrt9";
    private final String liveApiKey = "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";

    public StripeService() {
        // Default environment is test, can change this dynamically in the createCheckoutSession method
        setStripeApiKey(false); // Initialize Stripe with the test API key by default
    }

    // Method to set the API key dynamically
    private void setStripeApiKey(boolean isLive) {
        if (isLive) {
            Stripe.apiKey = liveApiKey; // Use the live API key
        } else {
            Stripe.apiKey = testApiKey; // Use the test API key
        }
    }

    // Method to create a checkout session with an invoiceId passed as metadata
    public Session createCheckoutSession(String ticketId, String currency, double totalPayment, boolean isLive) throws StripeException {
        // Set the API key based on the environment (test or live)
        setStripeApiKey(isLive);

        // Define the line items and payment details for the checkout session
        SessionCreateParams.LineItem.PriceData.ProductData productData =
                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                        .setName("rDvisioN")
                        .build();

        SessionCreateParams.LineItem.PriceData priceData =
                SessionCreateParams.LineItem.PriceData.builder()
                        .setCurrency(currency.toLowerCase())
                        .setUnitAmount((long) (totalPayment * 100)) // Convert to smallest currency unit
                        .setProductData(productData)
                        .build();

        SessionCreateParams.LineItem lineItem =
                SessionCreateParams.LineItem.builder()
                        .setPriceData(priceData)
                        .setQuantity(1L)
                        .build();

        // Add metadata with your custom invoiceId
        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl("https://crm.rdvision.in/success/" + ticketId)
                .setCancelUrl("https://crm.rdvision.in/failed_payment/" + ticketId)
                .addLineItem(lineItem)
                .putMetadata("ticketId", ticketId) // Attach invoiceId to the session metadata
                .build();

        // Create and return the session
        return Session.create(params);
    }


    public String createPaymentLink(Long priceAmount, String currency, String productCode, String ticketId, String productName) throws StripeException {
        // 1. Create a product
        Stripe.apiKey=testApiKey;
        ProductCreateParams productParams = ProductCreateParams.builder()
                .setName(productCode)
                .build();
        Product product = Product.create(productParams);

        // 2. Create a price object
        PriceCreateParams priceParams = PriceCreateParams.builder()
                .setUnitAmount(priceAmount)
                .setCurrency(currency)
                .setProduct(product.getId())
                .build();
        Price price = Price.create(priceParams);

        // 3. Create a payment link with metadata
        PaymentLinkCreateParams paymentLinkParams = PaymentLinkCreateParams.builder()
                .addLineItem(PaymentLinkCreateParams.LineItem.builder()
                        .setPrice(price.getId())
                        .setQuantity(1L)
                        .build())
                .putMetadata("productCode", productCode) // Add metadata to PaymentLink
                .putMetadata("ticketId", ticketId)      // Add metadata to PaymentLink
                .build();
        PaymentLink paymentLink = PaymentLink.create(paymentLinkParams);

        // Return the payment link URL
        return paymentLink.getUrl();
    }


}
