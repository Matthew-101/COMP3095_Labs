package ca.gbc.comp3095.orderservice.stubs;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class InventoryClientStub {

    public static void stubInventoryCall(String skuCode, Integer quantity){

        stubFor(get(urlEqualTo("/api/inventory?skuCode=" + skuCode + "&quantity=" + quantity))
                .willReturn(aResponse()
                        .withStatus(200)     //The status code we want our stub to return
                        .withHeader("Content-Type", "application/json")  // Response header to specify JSON content
                        .withBody("true")    // The desired mock response we want (indicating the item is in stock)
                ));

    }



}
