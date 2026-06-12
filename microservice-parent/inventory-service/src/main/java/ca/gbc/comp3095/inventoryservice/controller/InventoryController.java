package ca.gbc.comp3095.inventoryservice.controller;


import ca.gbc.comp3095.inventoryservice.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private  final InventoryService inventoryService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    //http://localhost:8083/api/inventory?skuCode=123abc&quantity=5
    public boolean isInStock(@RequestParam String skuCode, @RequestParam Integer quantity) {
        return inventoryService.isInStock(skuCode, quantity);
    }


}
