package net.ankan.budget.mapper;

import net.ankan.budget.dto.TransactionDto;
import net.ankan.budget.entity.Transaction;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {

    public TransactionDto toDto(Transaction t) {
        TransactionDto dto = new TransactionDto();
        dto.setId(t.getId());
        dto.setCategoryId(t.getCategory().getId());
        dto.setCategoryName(t.getCategory().getName());
        dto.setType(t.getType());
        dto.setAmount(t.getAmount());
        dto.setNote(t.getNote());
        dto.setDate(t.getDate());
        dto.setCreatedAt(t.getCreatedAt());
        return dto;
    }
}
