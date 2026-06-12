console.log("==================================================");
console.log("Starting MongoDB initialization script...");
console.log("==================================================");

// Switch to product-service database
db = db.getSiblingDB("product-service");

// Create application user
// UPDATE lowercase 'c'
db.createUser({
    user: "product_user",
    pwd: "product_password",
    roles: [
        {
            role: "readWrite",
            db: "product-service"
        }
    ]
});

// Insert seed data into products collection
//Change to 'products'
db.products.insertMany([
    {
        name: "Samsung 55inch TV",
        description: "Samsung 55inch TV - Smart TV - Model 2026",
        price: 1300
    },
    {
        name: "LG 60inch TV",
        description: "LG TV - QLED TV - Model 2026",
        price: 2000
    },
    {
        name: "Apple MacBook Pro 16inch",
        description: "MacBook Pro 16 inch - 24GB unified memory - 512GB SSD",
        price: 3000
    }
]);

console.log("==================================================");
console.log("MongoDB initialization completed successfully.");
console.log("==================================================");