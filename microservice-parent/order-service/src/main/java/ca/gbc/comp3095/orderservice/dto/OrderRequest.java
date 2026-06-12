package ca.gbc.comp3095.orderservice.dto;

import java.util.List;

public record OrderRequest(List<OrderLineItemDto> items) {}
