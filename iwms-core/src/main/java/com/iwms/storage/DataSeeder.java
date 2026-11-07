package com.iwms.storage;

import com.iwms.model.*;
import com.iwms.repository.Repository;
import com.iwms.service.InventoryService;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Seeds demonstration data for initial system bootstrapping.
 * Generates warehouse topology (Warehouse -> Zones -> Bins) and over 200 catalog products
 * across Perishable, Electronic, Fragile, and Bulk categories with initial batch inventory.
 */
public class DataSeeder {

    private final InventoryService inventoryService;
    private final Repository<Product, String> productRepository;

    public DataSeeder(InventoryService inventoryService, Repository<Product, String> productRepository) {
        this.inventoryService = Objects.requireNonNull(inventoryService, "inventoryService cannot be null");
        this.productRepository = Objects.requireNonNull(productRepository, "productRepository cannot be null");
    }

    public synchronized void seedIfEmpty() throws Exception {
        if (productRepository.count() > 0) {
            return; // Already seeded, maintain idempotency
        }

        // 1. Seed Warehouse Topology
        Warehouse warehouse = createWarehouseTopology();
        inventoryService.addWarehouse(warehouse);

        // 2. Seed Perishable Products (~55 products)
        seedPerishables();

        // 3. Seed Electronic Products (~55 products)
        seedElectronics();

        // 4. Seed Fragile Products (~45 products)
        seedFragile();

        // 5. Seed Bulk Products (~50 products)
        seedBulk();

        // 6. Seed Initial Inbound Stock
        seedInitialStock();
    }

    private Warehouse createWarehouseTopology() {
        Warehouse wh = new Warehouse("WH-MAIN", "Central Fulfillment Hub", "100 Industrial Parkway, Metro Logistics Park");

        // Zone A: Ambient general goods
        Zone zoneA = new Zone("ZONE-A", "Ambient Storage Zone", Category.GENERAL);
        for (int i = 1; i <= 6; i++) {
            zoneA.addBin(new Bin("BIN-A" + i, "A-0" + i, 500, 1000.0));
        }

        // Zone B: Cold Storage
        Zone zoneB = new Zone("ZONE-B", "Cold Storage Refrigeration Zone", Category.PERISHABLE);
        for (int i = 1; i <= 4; i++) {
            zoneB.addBin(new Bin("BIN-B" + i, "B-0" + i, 300, 500.0));
        }

        // Zone C: High Security Electronics Vault
        Zone zoneC = new Zone("ZONE-C", "High-Security Electronics Vault", Category.ELECTRONICS);
        for (int i = 1; i <= 4; i++) {
            zoneC.addBin(new Bin("BIN-C" + i, "C-0" + i, 400, 600.0));
        }

        wh.addZone(zoneA);
        wh.addZone(zoneB);
        wh.addZone(zoneC);

        return wh;
    }

    private void registerAndSave(Product product) throws Exception {
        productRepository.save(product);
        inventoryService.registerProduct(product);
    }

    private void seedPerishables() throws Exception {
        String[] dairyItems = {
                "Organic Whole Milk 1L", "Skimmed Milk 1L", "Greek Yogurt Plain 500g", "Greek Yogurt Blueberry 500g",
                "Cheddar Cheese Block 250g", "Mozzarella Ball 200g", "Heavy Whipping Cream 500ml", "Salted Butter 250g",
                "Unsalted Cultured Butter 250g", "Cottage Cheese 400g", "Brie Wheel 200g", "Parmigiano Reggiano 200g",
                "Oat Milk Unsweetened 1L", "Almond Milk Vanilla 1L", "Soy Milk Natural 1L"
        };
        for (int i = 0; i < dairyItems.length; i++) {
            String sku = String.format("PER-DAIRY-%03d", i + 1);
            registerAndSave(PerishableProduct.builder()
                    .sku(sku)
                    .name(dairyItems[i])
                    .category(Category.PERISHABLE)
                    .unitPrice(3.50 + (i * 0.40))
                    .costPrice(2.00 + (i * 0.25))
                    .minStockLevel(25)
                    .shelfLifeDays(14 + (i % 10))
                    .storageTempCelsius(4.0)
                    .requiresRefrigeration(true)
                    .build());
        }

        String[] produceItems = {
                "Honeycrisp Apples 1kg", "Cavendish Bananas 1kg", "Baby Spinach 250g", "Romaine Lettuce Hearts 3pk",
                "Organic Carrots 1kg", "English Cucumbers 2pk", "Roma Tomatoes 1kg", "Avocados Hass 4pk",
                "Fresh Strawberries 400g", "Blueberries Pint", "Raspberries 170g", "Broccoli Crowns 500g",
                "Yellow Bell Peppers 3pk", "Red Seedless Grapes 1kg", "Lemons Bag 1kg"
        };
        for (int i = 0; i < produceItems.length; i++) {
            String sku = String.format("PER-PROD-%03d", i + 1);
            registerAndSave(PerishableProduct.builder()
                    .sku(sku)
                    .name(produceItems[i])
                    .category(Category.PERISHABLE)
                    .unitPrice(2.80 + (i * 0.35))
                    .costPrice(1.50 + (i * 0.20))
                    .minStockLevel(30)
                    .shelfLifeDays(7 + (i % 7))
                    .storageTempCelsius(5.0)
                    .requiresRefrigeration(true)
                    .build());
        }

        String[] meatSeafood = {
                "Chicken Breast Fillets 1kg", "Chicken Thighs Boneless 1kg", "Grass-Fed Ground Beef 500g",
                "Ribeye Steak Prime 400g", "Pork Loin Chops 800g", "Atlantic Salmon Fillet 500g",
                "Black Tiger Shrimp 400g", "Fresh Tilapia Fillets 500g", "Turkey Bacon 375g",
                "Smoked Pork Sausage 450g", "Ground Turkey Lean 500g", "Beef Sirloin Roast 1.2kg",
                "Wild Cod Portions 454g", "Sea Scallops 350g"
        };
        for (int i = 0; i < meatSeafood.length; i++) {
            String sku = String.format("PER-MEAT-%03d", i + 1);
            registerAndSave(PerishableProduct.builder()
                    .sku(sku)
                    .name(meatSeafood[i])
                    .category(Category.PERISHABLE)
                    .unitPrice(8.99 + (i * 0.75))
                    .costPrice(5.50 + (i * 0.50))
                    .minStockLevel(20)
                    .shelfLifeDays(5 + (i % 5))
                    .storageTempCelsius(2.0)
                    .requiresRefrigeration(true)
                    .build());
        }

        String[] bakery = {
                "Artisan Sourdough Loaf 600g", "Whole Grain Bread 500g", "French Brioche Buns 6pk",
                "Butter Croissants 4pk", "Blueberry Muffins 4pk", "Bagels Plain 6pk",
                "Ciabatta Rolls 4pk", "English Muffins 6pk", "Cinnamon Raisin Bread 450g", "Gluten-Free White Loaf 400g",
                "Pita Pocket Bread 6pk"
        };
        for (int i = 0; i < bakery.length; i++) {
            String sku = String.format("PER-BAKE-%03d", i + 1);
            registerAndSave(PerishableProduct.builder()
                    .sku(sku)
                    .name(bakery[i])
                    .category(Category.PERISHABLE)
                    .unitPrice(3.99 + (i * 0.30))
                    .costPrice(2.10 + (i * 0.15))
                    .minStockLevel(15)
                    .shelfLifeDays(4 + (i % 4))
                    .storageTempCelsius(18.0)
                    .requiresRefrigeration(false)
                    .build());
        }
    }

    private void seedElectronics() throws Exception {
        String[] computeDevices = {
                "ProBook Laptop 15.6 inch i7", "UltraThin Laptop 14 inch M3", "Business Desktop Tower Core i5",
                "Workstation Mini PC Ryzen 7", "Tablet Pro 11 inch 128GB", "Tablet Lite 10.2 inch 64GB",
                "27 inch 4K IPS Monitor", "32 inch Curved QHD Monitor", "24 inch Full HD Office Display",
                "Dual Monitor Arm Heavy Duty", "Docking Station USB-C Triple 4K", "Smart Keyboard Wireless Backlit",
                "Ergonomic Optical Mouse", "Wireless Trackpad Multi-Touch", "Mechanical Gaming Keyboard RGB"
        };
        for (int i = 0; i < computeDevices.length; i++) {
            String sku = String.format("ELE-COMP-%03d", i + 1);
            registerAndSave(ElectronicProduct.builder()
                    .sku(sku)
                    .name(computeDevices[i])
                    .category(Category.ELECTRONICS)
                    .unitPrice(120.0 + (i * 65.0))
                    .costPrice(80.0 + (i * 45.0))
                    .minStockLevel(8)
                    .warrantyMonths(24)
                    .voltage(i < 6 ? 19 : 110)
                    .hasLithiumBattery(i < 6)
                    .build());
        }

        String[] networkMobile = {
                "Enterprise Wi-Fi 6 Router", "Gigabit Managed Switch 24-Port", "Gigabit PoE Switch 8-Port",
                "Mesh Wi-Fi System 3-Pack", "Outdoor Access Point High-Gain", "4G LTE Industrial Backup Gateway",
                "5G Rugged Android Smartphone", "Handheld Barcode Scanner Terminal", "Bluetooth Thermal Label Printer",
                "Industrial Barcode Ring Scanner", "RFID UHF Reader Handheld", "Smart Watch Cellular 45mm",
                "Noise Cancelling Over-Ear Headphones", "Wireless Earbuds IPX7", "Conference Speakerphone USB-C"
        };
        for (int i = 0; i < networkMobile.length; i++) {
            String sku = String.format("ELE-NET-%03d", i + 1);
            registerAndSave(ElectronicProduct.builder()
                    .sku(sku)
                    .name(networkMobile[i])
                    .category(Category.ELECTRONICS)
                    .unitPrice(95.0 + (i * 30.0))
                    .costPrice(60.0 + (i * 20.0))
                    .minStockLevel(10)
                    .warrantyMonths(12)
                    .voltage(12)
                    .hasLithiumBattery(i >= 6)
                    .build());
        }

        String[] storageComponents = {
                "NVMe M.2 SSD 1TB Gen4", "NVMe M.2 SSD 2TB Gen4", "SATA 2.5 inch SSD 500GB",
                "Enterprise HDD 8TB 7200RPM", "Enterprise HDD 16TB SATA", "DDR5 Desktop RAM 32GB Kit",
                "DDR4 Laptop RAM 16GB", "External Rugged SSD 1TB USB-C", "USB 3.2 Flash Drive 256GB",
                "ATX Power Supply 750W Gold", "Modular Power Supply 850W Platinum", "AIO Liquid CPU Cooler 240mm",
                "Case Fan 120mm PWM 3-Pack", "Thermal Compound High Performance 4g"
        };
        for (int i = 0; i < storageComponents.length; i++) {
            String sku = String.format("ELE-STOR-%03d", i + 1);
            registerAndSave(ElectronicProduct.builder()
                    .sku(sku)
                    .name(storageComponents[i])
                    .category(Category.ELECTRONICS)
                    .unitPrice(45.0 + (i * 20.0))
                    .costPrice(28.0 + (i * 14.0))
                    .minStockLevel(15)
                    .warrantyMonths(36)
                    .voltage(12)
                    .hasLithiumBattery(false)
                    .build());
        }

        String[] accessories = {
                "USB-C to HDMI 4K Cable 2m", "Cat6 Snagless Ethernet Cable 15m", "DisplayPort 1.4 Cable 2m",
                "Thunderbolt 4 Cable 1m 40Gbps", "Fast Charger 65W GaN USB-C", "Multi-Port Desktop Charger 100W",
                "Heavy-Duty Surge Protector 8-Outlet", "UPS Battery Backup 1500VA", "UPS Battery Backup 750VA",
                "Webcam 4K Ultra HD Autofocus", "Boom Arm Studio Microphone USB"
        };
        for (int i = 0; i < accessories.length; i++) {
            String sku = String.format("ELE-ACC-%03d", i + 1);
            registerAndSave(ElectronicProduct.builder()
                    .sku(sku)
                    .name(accessories[i])
                    .category(Category.ELECTRONICS)
                    .unitPrice(25.0 + (i * 12.0))
                    .costPrice(14.0 + (i * 7.0))
                    .minStockLevel(20)
                    .warrantyMonths(12)
                    .voltage(110)
                    .hasLithiumBattery(i == 7 || i == 8)
                    .build());
        }
    }

    private void seedFragile() throws Exception {
        String[] dinnerware = {
                "Porcelain Dinner Plate 10.5 inch 6pk", "Porcelain Salad Plate 8 inch 6pk", "Ceramic Cereal Bowls 6pk",
                "Fine Bone China Teacup and Saucer Set", "Stoneware Pasta Bowls 4pk", "Ceramic Serving Platter Oval 16 inch",
                "Porcelain Soup Tureen with Ladle", "Earthenware Ramekins Set of 8", "Stoneware Coffee Mugs 4pk",
                "Porcelain Gravy Boat with Tray", "Handmade Ceramic Butter Dish", "Ceramic Baking Dish Rectangular 3qt",
                "Stoneware Pie Pan 9 inch", "Porcelain Dip Bowl Set 3pk", "Porcelain Espresso Cups Set of 4"
        };
        for (int i = 0; i < dinnerware.length; i++) {
            String sku = String.format("FRG-DIN-%03d", i + 1);
            registerAndSave(FragileProduct.builder()
                    .sku(sku)
                    .name(dinnerware[i])
                    .category(Category.FRAGILE)
                    .unitPrice(28.0 + (i * 4.5))
                    .costPrice(14.0 + (i * 2.5))
                    .minStockLevel(12)
                    .fragilityIndex(7)
                    .maxStackHeight(2)
                    .build());
        }

        String[] glassware = {
                "Crystal Bordeaux Wine Glasses 6pk", "Crystal Champagne Flutes 6pk", "Whiskey Old Fashioned Tumblers 4pk",
                "Handblown Glass Wine Decanter 1.5L", "Double-Wall Insulated Highball Glasses 4pk", "Beer Pint Glasses 6pk",
                "Cocktail Coupe Glasses 4pk", "Glass Pitcher with Infuser 2L", "Stemless Wine Glasses 8pk",
                "Cognac Snifters Crystal 2pk", "Martini Glasses Classic 4pk", "Glass Carafe with Cork Lid 1L",
                "Shot Glasses Heavy Base Set of 6", "Margarita Glasses Set of 4", "Crystal Rocks Glasses Heavy 4pk"
        };
        for (int i = 0; i < glassware.length; i++) {
            String sku = String.format("FRG-GLAS-%03d", i + 1);
            registerAndSave(FragileProduct.builder()
                    .sku(sku)
                    .name(glassware[i])
                    .category(Category.FRAGILE)
                    .unitPrice(35.0 + (i * 5.0))
                    .costPrice(18.0 + (i * 3.0))
                    .minStockLevel(10)
                    .fragilityIndex(9)
                    .maxStackHeight(1)
                    .build());
        }

        String[] decorLighting = {
                "Crystal Table Chandelier 3-Tier", "Blown Glass Pendant Lamp Shade", "Modern Ceramic Table Lamp Base",
                "Floor Mirror Beveled Glass 65x24", "Wall Mirror Round Brass Frame 30 inch", "Crystal Flower Vase Fluted 12 inch",
                "Artisan Glass Terrarium Geometric", "Porcelain Decorative Urn 18 inch", "Glass Hurricane Candle Lantern 14 inch",
                "Stained Glass Window Panel 20x20", "Ceramic Sculptural Bust Matte White", "Glass Centerpiece Bowl Ripple 16 inch",
                "Vintage Mercury Glass Lamp 22 inch", "Hand-Painted Porcelain Tea Set", "Mosaic Glass Wall Sconce 12 inch"
        };
        for (int i = 0; i < decorLighting.length; i++) {
            String sku = String.format("FRG-DEC-%03d", i + 1);
            registerAndSave(FragileProduct.builder()
                    .sku(sku)
                    .name(decorLighting[i])
                    .category(Category.FRAGILE)
                    .unitPrice(60.0 + (i * 12.0))
                    .costPrice(32.0 + (i * 7.0))
                    .minStockLevel(6)
                    .fragilityIndex(8)
                    .maxStackHeight(1)
                    .build());
        }
    }

    private void seedBulk() throws Exception {
        String[] grainsCommodities = {
                "Premium Basmati Rice 25kg Sack", "Jasmine Fragrant Rice 20kg Sack", "All-Purpose Baking Flour 25kg Bag",
                "Whole Wheat Stoneground Flour 20kg", "Fine Granulated Cane Sugar 25kg Bag", "Brown Cane Sugar 20kg Bag",
                "Rolled Oats Whole Grain 20kg Sack", "Raw Green Coffee Beans Arabica 30kg", "Yellow Cornmeal Fine 20kg",
                "Dry Pinto Beans Bulk 25kg", "Dry Black Turtle Beans 25kg", "Red Kidney Beans 25kg Sack",
                "Green Lentils Whole 20kg", "Chickpeas Garbanzo Beans 25kg", "Coarse Sea Salt Food Grade 25kg",
                "Iodized Fine Table Salt 25kg", "Bulk Unrefined Sea Salt 50kg"
        };
        for (int i = 0; i < grainsCommodities.length; i++) {
            String sku = String.format("BLK-GRN-%03d", i + 1);
            registerAndSave(BulkProduct.builder()
                    .sku(sku)
                    .name(grainsCommodities[i])
                    .category(Category.BULK)
                    .unitPrice(38.0 + (i * 3.5))
                    .costPrice(24.0 + (i * 2.2))
                    .minStockLevel(15)
                    .weightKg(20.0 + (i % 30))
                    .volumeCubicMeters(0.04)
                    .unitOfMeasure("SACK")
                    .build());
        }

        String[] industrialSupplies = {
                "Industrial Corrugated Boxes 18x14x12 Bundle of 25", "Heavy Duty Corrugated Boxes 24x18x18 Bundle of 20",
                "Biodegradable Packing Peanuts 14 Cu Ft Bag", "Poly Bubble Roll 24 inch x 250 ft",
                "Clear Stretch Film 18 inch x 1500 ft 4pk", "Heavy Duty Strapping Kit Poly 3000 ft",
                "Wood Shipping Pallets GMA Standard 48x40 (Stack of 10)", "Plastic Pallet Heavy Duty Rackable",
                "Thermal Shipping Labels 4x6 Roll 1000ct Box of 4", "Reinforced Gummed Paper Tape 3 inch x 450 ft 6pk",
                "Poly Tubing Roll 12 inch x 1000 ft", "Desiccant Silica Gel Packs 100g Pouch of 50",
                "Steel Banding Roll 3/4 inch x 1000 ft", "Absorbent Floor Spill Granules 20kg Bag",
                "Industrial Hand Wrap Dispenser Tool", "Heavy Duty Packing Paper 24 inch Roll 30lb",
                "Corrugated Edge Protectors 36 inch Bundle of 50"
        };
        for (int i = 0; i < industrialSupplies.length; i++) {
            String sku = String.format("BLK-IND-%03d", i + 1);
            registerAndSave(BulkProduct.builder()
                    .sku(sku)
                    .name(industrialSupplies[i])
                    .category(Category.BULK)
                    .unitPrice(45.0 + (i * 5.0))
                    .costPrice(26.0 + (i * 3.0))
                    .minStockLevel(20)
                    .weightKg(15.0 + (i * 1.5))
                    .volumeCubicMeters(0.12)
                    .unitOfMeasure("BUNDLE")
                    .build());
        }

        String[] buildingRawMaterials = {
                "Portland Cement Type I/II 42.5kg Bag", "Masonry Mortar Mix 30kg Bag", "Concrete Sand Coarse 25kg Bag",
                "Gravel Aggregate 20mm 25kg Bag", "Gypsum Joint Compound Ready-Mix 20kg Pail",
                "Ceramic Tile Adhesive Thin-Set 20kg", "Interior Primer Sealer 20L Pail",
                "Exterior Acrylic Latex Paint White 20L", "Spunbond Geotextile Fabric 4m x 50m Roll",
                "Galvanized Common Nails 3.5 inch 25kg Box", "Drywall Screws Coarse Thread 25kg Box",
                "Self-Leveling Underlayment 25kg Bag", "Refractory Fire Mortar 15kg Tub",
                "Epoxy Grout Commercial Grade 10kg Kit", "Bitumen Waterproofing Membrane 10m Roll",
                "Glass Mineral Wool Insulation Roll 15m2"
        };
        for (int i = 0; i < buildingRawMaterials.length; i++) {
            String sku = String.format("BLK-MAT-%03d", i + 1);
            registerAndSave(BulkProduct.builder()
                    .sku(sku)
                    .name(buildingRawMaterials[i])
                    .category(Category.BULK)
                    .unitPrice(22.0 + (i * 4.0))
                    .costPrice(14.0 + (i * 2.5))
                    .minStockLevel(25)
                    .weightKg(25.0 + (i % 20))
                    .volumeCubicMeters(0.05)
                    .unitOfMeasure("BAG")
                    .build());
        }
    }

    private void seedInitialStock() throws Exception {
        // Inbound initial stock for a set of fast movers
        inventoryService.stockIn("PER-DAIRY-001", "BATCH-M2026-01", "BIN-B1", 120, LocalDate.now().plusDays(12), "Initial stock inbound", "admin");
        inventoryService.stockIn("PER-DAIRY-002", "BATCH-M2026-02", "BIN-B1", 80, LocalDate.now().plusDays(10), "Initial stock inbound", "admin");
        inventoryService.stockIn("PER-PROD-001", "BATCH-P2026-01", "BIN-B2", 150, LocalDate.now().plusDays(8), "Initial stock inbound", "admin");
        inventoryService.stockIn("PER-MEAT-001", "BATCH-MT2026-01", "BIN-B3", 60, LocalDate.now().plusDays(5), "Initial stock inbound", "admin");

        inventoryService.stockIn("ELE-COMP-001", "BATCH-EL2026-01", "BIN-C1", 40, null, "Initial stock inbound", "admin");
        inventoryService.stockIn("ELE-COMP-002", "BATCH-EL2026-02", "BIN-C1", 25, null, "Initial stock inbound", "admin");
        inventoryService.stockIn("ELE-NET-001", "BATCH-NT2026-01", "BIN-C2", 35, null, "Initial stock inbound", "admin");
        inventoryService.stockIn("ELE-STOR-001", "BATCH-ST2026-01", "BIN-C3", 90, null, "Initial stock inbound", "admin");

        inventoryService.stockIn("FRG-DIN-001", "BATCH-FR2026-01", "BIN-A1", 45, null, "Initial stock inbound", "admin");
        inventoryService.stockIn("FRG-GLAS-001", "BATCH-FR2026-02", "BIN-A2", 30, null, "Initial stock inbound", "admin");
        inventoryService.stockIn("BLK-GRN-001", "BATCH-BL2026-01", "BIN-A3", 100, null, "Initial stock inbound", "admin");
        inventoryService.stockIn("BLK-IND-001", "BATCH-BL2026-02", "BIN-A4", 75, null, "Initial stock inbound", "admin");
    }
}
