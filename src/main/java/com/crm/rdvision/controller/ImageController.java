package com.crm.rdvision.controller;
import com.crm.rdvision.entity.EmailContentImages;
import com.crm.rdvision.entity.Product;
import com.crm.rdvision.entity.ProductImages;
import com.crm.rdvision.repository.EmailContentImageRepo;
import com.crm.rdvision.repository.ProductImageRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.InputStreamResource;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/images")
public class ImageController {

    @Autowired
    ProductImageRepo productImageRepo;

    @Autowired
    EmailContentImageRepo emailContentImageRepo;

    // Simulated data source (replace with actual data fetching logic)
    private Map<String, byte[]> imageStorage = Map.of(
            "1", new byte[]{ /* Example image byte data */ },
            "2", new byte[]{ /* Another example image byte data */ }
    );

    @GetMapping("/image/{imageId}")
    public ResponseEntity<InputStreamResource> getImage(@PathVariable int imageId) throws IOException {
        // Fetch the image data from the data source using the imageId
        System.out.println("Getting images ");
        byte[] imageBytes = productImageRepo.findByImageId(imageId).getImageData();

        if (imageBytes == null || imageBytes.length == 0) {
            return ResponseEntity.notFound().build(); // Return 404 if image not found
        }

        // Convert byte[] to InputStream
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(imageBytes);

        // Set response headers and return the image
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_PNG_VALUE) // Set correct content type (image/png)
                .contentLength(imageBytes.length)
                .body(new InputStreamResource(byteArrayInputStream)); // Serve the image as a resource
    }
    @GetMapping("/getTempImage/{id}")
    public ResponseEntity<InputStreamResource> getTempImages(@PathVariable int id) throws IOException {
        byte[] imageBytes = emailContentImageRepo.findByImageId(id).getImageData();

        if (imageBytes == null || imageBytes.length == 0) {
            return ResponseEntity.notFound().build(); // Return 404 if image not found
        }

        // Convert byte[] to InputStream
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(imageBytes);

        // Set response headers and return the image
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_PNG_VALUE) // Set correct content type (image/png)
                .contentLength(imageBytes.length)
                .body(new InputStreamResource(byteArrayInputStream)); // Serve the image as a resource
    }

    @PostMapping("/addEmailImage")
    public ResponseEntity<EmailContentImages> addEmailContentEmailWithTitle(@RequestBody EmailContentImages emailContentImages){
        return new ResponseEntity<>(emailContentImageRepo.save(emailContentImages), HttpStatus.CREATED);
    }

    @GetMapping("/getAllimages")
    public List<EmailContentImages> getAllemailTemplateImages(){
        return emailContentImageRepo.findAll();
    }

    @PutMapping("/updateNewImage")
    public void updateOldeImageToNew(@RequestBody EmailContentImages emailContentImages){
        emailContentImageRepo.save(emailContentImages);
    }
    @GetMapping("/getProductImage/{id}")
    public ResponseEntity<InputStreamResource> getProductImageByProductId(@PathVariable int id) throws IOException {
        if(productImageRepo.findFirstImageByProductId(id).size()>0){
            byte[] imageBytes = productImageRepo.findFirstImageByProductId(id).get(0).getImageData();

            if (imageBytes == null || imageBytes.length == 0) {
                return ResponseEntity.notFound().build(); // Return 404 if image not found
            }

            // Convert byte[] to InputStream
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(imageBytes);

            // Set response headers and return the image
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_PNG_VALUE) // Set correct content type (image/png)
                    .contentLength(imageBytes.length)
                    .body(new InputStreamResource(byteArrayInputStream)); // Serve the image as a resource
        }else {
            return null;
        }
    }
    @GetMapping("/getProductImages")
    public Map<Integer, List<Integer>> getAllProductImages() {
        Map<Integer, List<Integer>> idWithProductId = new HashMap<>();

        // Get all image data grouped by productId and imageId
        List<Object[]> objects = productImageRepo.findAllImageIdsGroupedByProduct();

        for (Object[] row : objects) {
            Integer productIdFromDb = (Integer) row[0]; // Product ID
            Integer imageId = (Integer) row[1]; // Image ID

            // Group imageIds by productId
            idWithProductId.computeIfAbsent(productIdFromDb, k -> new ArrayList<>()).add(imageId);
        }

        return idWithProductId; // Return the map containing all products with their imageIds
    }

    @DeleteMapping("/deleteImageById/{imageId}")
    public void deleteProductImageById(@PathVariable long imageId){
        productImageRepo.deleteById(imageId);
    }


}
