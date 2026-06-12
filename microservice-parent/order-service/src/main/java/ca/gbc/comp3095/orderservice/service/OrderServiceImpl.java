package ca.gbc.comp3095.orderservice.service;

import ca.gbc.comp3095.orderservice.client.InventoryClient;
import ca.gbc.comp3095.orderservice.dto.OrderLineItemDto;
import ca.gbc.comp3095.orderservice.dto.OrderRequest;
import ca.gbc.comp3095.orderservice.model.Order;
import ca.gbc.comp3095.orderservice.model.OrderLineItem;
import ca.gbc.comp3095.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient;

    @Override
    public void placeOrder(OrderRequest orderRequest) {

        boolean allInStock = orderRequest.items().stream()
                .allMatch( item -> inventoryClient.isInStock(item.skuCode(), item.quantity()));

        //If any item is out fo stock, find it, and surface a clear error
        if(!allInStock){
            OrderLineItemDto outOfStock = orderRequest.items().stream()
                    .filter(item -> !inventoryClient.isInStock(item.skuCode(), item.quantity()))
                    .findFirst()
                    .orElseThrow();
            throw new Error("Product with SkuCode " +  outOfStock.skuCode() + " has insufficient stock to fulfill this order");
        }

        Order order = Order.builder()
                .orderNumber(UUID.randomUUID().toString())
                .build();

        List<OrderLineItem> orderLineItems = orderRequest.items().stream()
                .map( dto ->  OrderLineItem.builder()
                        .skuCode(dto.skuCode())
                        .price(dto.price())
                        .quantity(dto.quantity())
                        .order(order)
                        .build())
                .toList();

        order.setOrderLineItems(orderLineItems);
        orderRepository.save(order);

    }

}
