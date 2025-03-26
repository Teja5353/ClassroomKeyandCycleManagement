package com.example.ckcm.controller;

import com.example.ckcm.entities.Cycle;
import com.example.ckcm.repositories.CycleRepository;
import com.example.ckcm.services.CycleService;
import com.example.ckcm.util.QrCodeGenerator;
import com.google.zxing.WriterException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/cycles")
public class CycleController {

    private static final String QR_CODE_DIR = "src/main/resources/static/qrcodes/";

    @Autowired
    private CycleService cycleService;

    @Autowired
    private QrCodeGenerator qrCodeGenerator;
    @Autowired
    private CycleRepository cycleRepository;

    // ✅ Register cycle and return QR code URL
    @PostMapping("/register")
    public ResponseEntity<?> saveCycle(@RequestBody Map<String, String> cycleData) throws IOException, WriterException {
        Cycle cycle = cycleService.registerCycle(cycleData.get("cycleId"), cycleData.get("location"), cycleData.get("gearType"));
        return ResponseEntity.ok(Map.of("qrCodeUrl", "http://localhost:8082/cycles/download?filename=" + cycle.getQrCode()));
    }

    // ✅ Serve QR code for download
    @GetMapping("/download")
    public ResponseEntity<Resource> downloadQrCode(@RequestParam String filename) {
        try {
            Path filePath = Paths.get(QR_CODE_DIR).resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_PNG)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllCycles() {
        System.out.println(cycleService.getAll());
        return ResponseEntity.ok(cycleService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Cycle> getCycleById(@PathVariable String id) {
        return cycleService.getCycleByCycleId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    @PostMapping("/delete/{cycleId}")
    public ResponseEntity<?> deleteCycle(@PathVariable String cycleId){
        cycleService.deleteCycle(cycleId);
        return ResponseEntity.ok(Map.of("message", "Cycle deleted successfully"));
    }
    @GetMapping("/get-cycles/{email}")
    public ResponseEntity<?> getCycles(@PathVariable String email){
        return ResponseEntity.ok(cycleRepository.findByBorrowedBy(email));
    }
}