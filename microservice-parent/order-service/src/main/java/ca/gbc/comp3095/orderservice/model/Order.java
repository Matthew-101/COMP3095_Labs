package ca.gbc.comp3095.orderservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "t_orders")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String orderNumber;

    @OneToMany( mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true )
    private List<OrderLineItem> orderLineItems = new ArrayList<>();

}
