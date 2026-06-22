package net.ankan.budget.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.ankan.budget.dto.BudgetDto;
import net.ankan.budget.dto.BudgetStatusDto;
import net.ankan.budget.service.impl.BudgetServiceImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetServiceImpl budgetService;

    @GetMapping
    public ResponseEntity<List<BudgetDto>> getAll(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(budgetService.getAll(month, year));
    }

    @GetMapping("/status")
    public ResponseEntity<List<BudgetStatusDto>> getStatus(
            @RequestParam int month,
            @RequestParam int year) {
        return ResponseEntity.ok(budgetService.getStatus(month, year));
    }

    @PostMapping
    public ResponseEntity<BudgetDto> create(@Valid @RequestBody BudgetDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(budgetService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BudgetDto> update(@PathVariable Long id,
                                             @Valid @RequestBody BudgetDto dto) {
        return ResponseEntity.ok(budgetService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        budgetService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
