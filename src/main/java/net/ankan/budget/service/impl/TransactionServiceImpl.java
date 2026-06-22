package net.ankan.budget.service.impl;

import lombok.RequiredArgsConstructor;
import net.ankan.budget.dto.TransactionDto;
import net.ankan.budget.dto.TransactionSummaryDto;
import net.ankan.budget.entity.*;
import net.ankan.budget.exception.ResourceNotFoundException;
import net.ankan.budget.mapper.TransactionMapper;
import net.ankan.budget.repository.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransactionServiceImpl {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final TransactionMapper transactionMapper;

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public List<TransactionDto> getAll(Integer month, Integer year, TransactionType type, Long categoryId) {
        User user = getCurrentUser();
        Category category = (categoryId != null)
                ? categoryRepository.findByIdAndUser(categoryId, user)
                  .orElseThrow(() -> new ResourceNotFoundException("Category not found"))
                : null;
        return transactionRepository.findFiltered(user, month, year, type, category)
                .stream().map(transactionMapper::toDto).toList();
    }

    @Transactional
    public TransactionDto create(TransactionDto dto) {
        User user = getCurrentUser();
        Category category = categoryRepository.findByIdAndUser(dto.getCategoryId(), user)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        Transaction transaction = Transaction.builder()
                .user(user)
                .category(category)
                .type(dto.getType())
                .amount(dto.getAmount())
                .note(dto.getNote())
                .date(dto.getDate())
                .build();
        return transactionMapper.toDto(transactionRepository.save(transaction));
    }

    @Transactional
    public TransactionDto update(Long id, TransactionDto dto) {
        User user = getCurrentUser();
        Transaction transaction = transactionRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));
        Category category = categoryRepository.findByIdAndUser(dto.getCategoryId(), user)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        transaction.setCategory(category);
        transaction.setType(dto.getType());
        transaction.setAmount(dto.getAmount());
        transaction.setNote(dto.getNote());
        transaction.setDate(dto.getDate());
        return transactionMapper.toDto(transactionRepository.save(transaction));
    }

    @Transactional
    public void delete(Long id) {
        User user = getCurrentUser();
        Transaction transaction = transactionRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));
        transactionRepository.delete(transaction);
    }

    public TransactionSummaryDto getSummary(int month, int year) {
        User user = getCurrentUser();
        BigDecimal income = transactionRepository
                .sumByUserAndTypeAndMonthAndYear(user, TransactionType.INCOME, month, year);
        BigDecimal expense = transactionRepository
                .sumByUserAndTypeAndMonthAndYear(user, TransactionType.EXPENSE, month, year);
        if (income  == null) income  = BigDecimal.ZERO;
        if (expense == null) expense = BigDecimal.ZERO;
        return new TransactionSummaryDto(month, year, income, expense, income.subtract(expense));
    }
}