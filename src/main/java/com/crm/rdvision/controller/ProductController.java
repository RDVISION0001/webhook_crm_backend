package com.crm.rdvision.controller;

import com.crm.rdvision.Exception.BussinessException;
import com.crm.rdvision.common.EndPointReference;
import com.crm.rdvision.dto.ProductDto;
import com.crm.rdvision.dto.ProductDtoForInvoice;
import com.crm.rdvision.dto.SuccessResponse;
import com.crm.rdvision.entity.*;
import com.crm.rdvision.repository.*;
import com.crm.rdvision.utility.Constants;
import com.crm.rdvision.utility.EnglishConstants;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@RestController
@CrossOrigin
@RequestMapping("/product/")
public class ProductController {
    @Autowired
    ProductRepo productRepo;
    @Autowired
    ModelMapper modelMapper;
    @Autowired
    ProductImageRepo productImageRepo;
    @Autowired
    ProductPriceRepo productPriceRepo;

    @Autowired
    ProductFixedPriceListRepo productFixedPriceListRepo;
    @Autowired
    ProductOrderRepo productOrderRepo;

    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);
    @PostMapping(EndPointReference.CREATE_PRODUCT)
    public Map<String, Object> createProduct(@RequestBody ProductDto productDto) throws com.avanse.core.exception.TechnicalException, BussinessException {
        logger.info("Create Product Called");
        Map<String, Object> map = new HashMap<>();
        Product product = null;
        try {

            product = productRepo.save(modelMapper.map(productDto,Product.class));

        } catch (Exception e) {
            throw new com.avanse.core.exception.TechnicalException(Constants.TECHNICAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, Constants.SPACE);
        }
        map.put(Constants.ID, product.getProductId());
        map.put(Constants.SUCCESS, new SuccessResponse(Constants.SUCCESS));
        map.put(Constants.ERROR, null);

        return map;
    }

    @PostMapping("/addproduct")
    public ResponseEntity<?> addProduct(@RequestBody Product productRequest) {
        try {
            List<ProductImages> images = productRequest.getImageListInByte();
            productRequest.setImageListInByte(null);
            Product savedProduct = productRepo.save(productRequest);
            for (ProductImages image : images) {
                image.setProduct(savedProduct);
            }
            productImageRepo.saveAll(images);

            return ResponseEntity.ok("Product Added Successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error adding product: " + e.getMessage());
        }
    }

    @GetMapping(EndPointReference.GET_ALL_PRODUCT)
    public Map<String, Object> getAllProducts() throws com.avanse.core.exception.TechnicalException, BussinessException {
        logger.info("Get All Products Called");
        Map<String, Object> map = new HashMap<>();
        List<Product> products= productRepo.findAll();
        map.put(Constants.DTO_LIST, products);
        map.put(Constants.ERROR, null);
        map.put(Constants.SUCCESS, new SuccessResponse(Constants.SUCCESS));
        return map;
    }
    @GetMapping(EndPointReference.GET_PRODUCT)
    public Map<String, Object> getproductById(@PathVariable Integer productId) throws com.avanse.core.exception.TechnicalException, BussinessException {
        logger.info("Get Product By Product Id Called");
        Map<String, Object> map = new HashMap<>();
        Product product = this.productRepo.findById(productId).orElseThrow(() -> new BussinessException(HttpStatus.NOT_FOUND, EnglishConstants.PRODUCT_NOT_FOUND_WITH_PRODUCT_ID + productId));
        map.put(Constants.DTO_LIST, product);
        map.put(Constants.ERROR, null);
        map.put(Constants.SUCCESS, new SuccessResponse(Constants.SUCCESS));
        return map;
    }
    //    Upload CSV products
    @PostMapping(EndPointReference.Upload_CSV_Products)
    public ResponseEntity<String> ProductCsv(@RequestBody Map<String,String> object) {
        logger.info("Uploading products please wait...");
        String[] tableRows = object.get("csvStringData").split("\n");
        int batchSize = 10000;
        List<Product> uploadProduct = new ArrayList<>();

        try {
            for (String row : tableRows) {
                String[] tableColumns = row.split(",");
                Product product = new Product();
                product.setName(tableColumns[0]);
                product.setComposition(tableColumns[2]);
                product.setBrand(tableColumns[3]);
                product.setTreatment(tableColumns[4]);
                product.setPackagingSize(tableColumns[5]);
                uploadProduct.add(product);
                if (uploadProduct.size() >= batchSize) {
                    productRepo.saveAll(new ArrayList<>(uploadProduct));
                    uploadProduct.clear();
                }
            }

            // Save any remaining products
            if (!uploadProduct.isEmpty()) {
                productRepo.saveAll(uploadProduct);
            }

            logger.info("Uploaded successfully");
            return ResponseEntity.ok("CSV products uploaded successfully");

        } catch (Exception e) {
            logger.error("Error processing CSV data", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing CSV data");
        }
    }

    @PostMapping("/addImage")
    public void receiveData(@RequestBody List<ProductImages> productImages) {
     productImageRepo.saveAll(productImages);
    }

    @DeleteMapping("/deleteproduct/{productId}")
    public ResponseEntity<String> deleteProductById(@PathVariable int productId) {
        if (productRepo.existsById(productId)) {  // Check if the product exists
            productRepo.deleteById(productId);  // Delete the product
            return ResponseEntity.ok("Product deleted successfully");  // Return success message
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Product not found");  // Return error if product not found
        }
    }

    @GetMapping("/getproductsbynamecontaining/{text}")
    public List<Product> getProductByNameCOntaining(@PathVariable String text){
        return productRepo.findByNameContaining(text);
    }

    @PutMapping("/updateCatgoryAndPrice")
    public void updateCategoryAndPriceOfProduct(@RequestBody Map<String,String> map){
        Product product=productRepo.findByProductId(Integer.parseInt(map.get("productId")));
    }

    @PostMapping("/addprices")
    public ResponseEntity<String> addPricesOfProduct(@RequestBody List<ProductsPrice> prices) {
        try {
            productPriceRepo.saveAll(prices);
            return ResponseEntity.ok("Prices added successfully!");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while saving prices: " + e.getMessage());
        }
    }

    @GetMapping("/getProductPrices/{productId}")
    public List<ProductsPrice> getAllPricesOfProducts(@PathVariable int productId){
        Product product=new Product();
        product.setProductId(productId);
        List<ProductsPrice> list = productPriceRepo.findByProduct(product);
        for(int i=0;i<list.size();i++){
            list.get(i).setProduct(null);
        }
        return list;
    }
    @PostMapping("/getProductPrices")
    public List<ProductsPrice> getAllPriceWithTicketId(@RequestBody Map<String,String> map){
        Product product=new Product();
        product.setProductId(Integer.parseInt(map.get("productId")));
        String ticketId =map.get("ticketId");
        List<ProductsPrice> list = productPriceRepo.findByProductAndTicketId(product,ticketId);
        for(int i=0;i<list.size();i++){
            list.get(i).setProduct(null);
        }
        return list;
    }

    @PutMapping("/updateCategory")
    public void updateCategory(@RequestBody Product product){
        Product product1=productRepo.findByProductId(product.getProductId());
        product1.setCategory(product.getCategory());
        productRepo.save(product1);
    }

    @DeleteMapping("/delete/{id}")
    public void deletePriceListById(@PathVariable long id){
        productPriceRepo.deleteById(id);
    }

    @PostMapping("/addFixedPriceList")
    public void addPriceListOfProduct(@RequestBody List<ProductFixedPriceList> productFixedPriceLists){

        for (ProductFixedPriceList productFixedPriceList : productFixedPriceLists) {
            System.out.println(productFixedPriceList.getPrice());
            Product product = productRepo.findByProductId(productFixedPriceList.getProduct().getProductId());
            String input = product.getName();
            String result = generateProductCode(input);
            productFixedPriceList.setProductCode(result +"."+productFixedPriceList.getQuantity()+productFixedPriceList.getUnit().charAt(0)+".pack" + "."+product.getBrand());
            System.out.println(productFixedPriceList.getProductCode());
            productFixedPriceList.setPricePerPill((double) productFixedPriceList.getPrice() / productFixedPriceList.getQuantity());
        }
        productFixedPriceListRepo.saveAll(productFixedPriceLists);
    }
    public static String generateProductCode(String input) {
        String[] parts = input.split(" ");  // Split by spaces
        StringBuilder result = new StringBuilder();

        // Loop through the parts and take the first letter of each word (except the last one)
        for (int i = 0; i < parts.length - 1; i++) {
            result.append(parts[i].substring(0, 1).toLowerCase());  // Add first letter of each part
        }

        // Extract the numeric part (strength) from the last element
        String strength = parts[parts.length - 1].replaceAll("[^0-9]", "");  // Remove any non-numeric characters

        // Append the numeric strength to the result
        result.append(strength);

        return result.toString();
    }

    @PostMapping("/updateProductDetails")
    public String updateProductInformation(@RequestBody Product product){
        Product product1 =productRepo.findByProductId(product.getProductId());
        product1.setTreatment(product.getTreatment());
        product1.setPackagingType(product.getPackagingType());
        product1.setPackagingSize(product.getPackagingSize());
        product1.setName(product.getName());
        product1.setGenericName(product.getGenericName());
        product1.setBrand(product.getBrand());
        product1.setStrength(product.getStrength());
        productRepo.save(product1);
        return "Updated";
    }

    @GetMapping("/getAllProductForInvoice")
    public  Map<String, Object> gteAllProductsForInvoicesAndQuotations(){
        Map<String, Object> map = new HashMap<>();
        List<ProductDtoForInvoice> products= productRepo.findAllProductsWithSingleImage();;
        map.put(Constants.DTO_LIST, products);
        map.put(Constants.ERROR, null);
        map.put(Constants.SUCCESS, new SuccessResponse(Constants.SUCCESS));
        return map;
    }
@PutMapping("/updateProductOrders")
    public ResponseEntity<List<ProductOrder>> updateProductOrders(@RequestBody List<ProductOrder> productOrders){
        return new ResponseEntity<>(productOrderRepo.saveAll(productOrders),HttpStatus.ACCEPTED);
}


}
