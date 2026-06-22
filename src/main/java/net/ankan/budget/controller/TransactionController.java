package net.ankan.budget.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.ankan.budget.dto.TransactionDto;
import net.ankan.budget.dto.TransactionSummaryDto;
import net.ankan.budget.entity.TransactionType;
import net.ankan.budget.service.impl.TransactionServiceImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionServiceImpl transactionService;

    @GetMapping
    public ResponseEntity<List<TransactionDto>> getAll(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) Long categoryId) {
        return ResponseEntity.ok(transactionService.getAll(month, year, type, categoryId));
    }

    @GetMapping("/summary")
    public ResponseEntity<TransactionSummaryDto> getSummary(
            @RequestParam int month,
            @RequestParam int year) {
        return ResponseEntity.ok(transactionService.getSummary(month, year));
    }

    @PostMapping
    public ResponseEntity<TransactionDto> create(@Valid @RequestBody TransactionDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TransactionDto> update(@PathVariable Long id,
                                                  @Valid @RequestBody TransactionDto dto) {
        return ResponseEntity.ok(transactionService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        transactionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
