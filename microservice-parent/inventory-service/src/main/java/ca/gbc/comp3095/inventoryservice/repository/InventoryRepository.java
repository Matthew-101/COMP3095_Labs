package ca.gbc.comp3095.inventoryservice.repository;

import ca.gbc.comp3095.inventoryservice.model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryRepository extends JpaRepository<Inventory,Integer> {

    /**
     * Custom query generated using Spring Data JPA syntax.
     * The way this works ...
     *    Based on  the method name, Spring Data JPA automatically derives the appropriate SQL (query) SQL to check
     *    if an inventory item with a given SKU code exists and whether its quantity is greater than passed in
     *    specified amount
     *
     *
     *    SELECT CASE WHEN COUNT(i) > 0 THEN TRUE ELSE FALSE END
     *    FROM t_inventory
     *    WHERE i.sku_code = ?1  AND i.quantity >= ?2;
     *
     */
     boolean existsBySkuCodeAndQuantityGreaterThanEqual(String skuCode, Integer quantity);
}
