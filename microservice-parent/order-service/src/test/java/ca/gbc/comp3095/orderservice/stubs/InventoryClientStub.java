package ca.gbc.comp3095.orderservice.stubs;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;

public class InventoryClientStub {

    public static void stubInventoryCall(String skuCode, Integer quantity) {

        stubFor(get(urlEqualTo("/api/inventory?skuCode=" + skuCode + "&quantity=" + quantity))
                .willReturn(aResponse()
                        .withStatus(200) //The HTTP Status expected for GET communication success
                        .withHeader("Content-Type", "application/json") //JSON expectation
                        .withBody("true")));  //The mock response body indicating the item is in stock

    }

}
