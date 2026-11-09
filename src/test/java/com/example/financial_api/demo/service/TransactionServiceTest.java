package com.example.financial_api.demo.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.financial_api.demo.entity.transaction.*;
import com.example.financial_api.demo.entity.user.User;
import com.example.financial_api.demo.repository.TransactionRepository;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {
    @Mock
    TransactionRepository repository;
    @InjectMocks
    TransactionService service;
    User owner;

    @BeforeEach
    void setup() {
        owner = new User();
        owner.setId(1L);
    }

    Transaction transaction(double amount, Type type) {
        return new Transaction(LocalDate.of(2024, 2, 15), "description", amount, owner, type);
    }

    void month(List<Transaction> entries) {
        when(repository.findByUserIdAndDateBetween(
                        1L, LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 29)))
                .thenReturn(entries);
    }

    @Test
    void summaryCountsAllTransactionsAndCalculatesAverage() {
        month(
                List.of(
                        transaction(100.0, Type.INCOME),
                        transaction(20.0, Type.EXPENSE),
                        transaction(30.0, Type.EXPENSE)));
        SummaryDTO result = service.monthSummary(1L, 2024, 2);
        assertAll(
                () -> assertEquals(100.0, result.getTotalIncome()),
                () -> assertEquals(50.0, result.getTotalExpense()),
                () -> assertEquals(50.0, result.getBalance()),
                () -> assertEquals(3, result.getTransactionCount()),
                () -> assertEquals(100.0, result.getBiggestTransaction()),
                () -> assertEquals(50.0, result.getAverageTransaction()));
    }

    @Test
    void emptySummaryReturnsZeros() {
        month(List.of());
        SummaryDTO result = service.monthSummary(1L, 2024, 2);
        assertAll(
                () -> assertEquals(0.0, result.getTotalIncome()),
                () -> assertEquals(0.0, result.getTotalExpense()),
                () -> assertEquals(0.0, result.getBalance()),
                () -> assertEquals(0, result.getTransactionCount()),
                () -> assertEquals(0.0, result.getBiggestTransaction()),
                () -> assertEquals(0.0, result.getAverageTransaction()));
    }

    @ParameterizedTest
    @EnumSource(Type.class)
    void summaryWithSingleType(Type type) {
        month(List.of(transaction(12, type)));
        SummaryDTO result = service.monthSummary(1L, 2024, 2);
        assertEquals(12.0, result.getAverageTransaction());
        assertEquals(type == Type.INCOME ? 12.0 : -12.0, result.getBalance());
    }

    @Test
    void decimalArithmeticDoesNotAccumulateBinaryRounding() {
        month(
                List.of(
                        transaction(0.1, Type.INCOME),
                        transaction(0.2, Type.INCOME),
                        transaction(0.3, Type.EXPENSE)));
        assertEquals(0.0, service.monthSummary(1L, 2024, 2).getBalance());
    }

    @Test
    void createUsesAuthenticatedOwner() {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        Transaction result =
                service.createTransaction(LocalDate.now(), "Lunch", 10.0, owner, Type.EXPENSE);
        assertSame(owner, result.getUser());
        assertEquals(10.0, result.getAmount());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(doubles = {0, -1, 0.001, 100000000, Double.NaN, Double.POSITIVE_INFINITY})
    void rejectsInvalidAmounts(Double amount) {
        assertEquals(
                400,
                assertThrows(
                                ResponseStatusException.class,
                                () ->
                                        service.createTransaction(
                                                LocalDate.now(),
                                                "Lunch",
                                                amount,
                                                owner,
                                                Type.EXPENSE))
                        .getStatusCode()
                        .value());
        verifyNoInteractions(repository);
    }

    @Test
    void rejectsMissingDateDescriptionAndType() {
        assertThrows(
                ResponseStatusException.class,
                () -> service.createTransaction(null, "Lunch", 10.0, owner, Type.EXPENSE));
        assertThrows(
                ResponseStatusException.class,
                () -> service.createTransaction(LocalDate.now(), " ", 10.0, owner, Type.EXPENSE));
        assertThrows(
                ResponseStatusException.class,
                () -> service.createTransaction(LocalDate.now(), "Lunch", 10.0, owner, null));
        verifyNoInteractions(repository);
    }

    @ParameterizedTest
    @CsvSource({"2024,0", "2024,13", "0,1", "10000,1"})
    void rejectsInvalidPeriods(int year, int month) {
        assertEquals(
                400,
                assertThrows(
                                ResponseStatusException.class,
                                () -> service.monthSummary(1L, year, month))
                        .getStatusCode()
                        .value());
        verifyNoInteractions(repository);
    }

    @Test
    void updatePreservesOwnerIdAndCreationDate() {
        Transaction stored = transaction(20, Type.EXPENSE);
        stored.setId(8L);
        stored.setCreatedAt(LocalDate.of(2020, 1, 1));
        when(repository.findById(8L)).thenReturn(Optional.of(stored));
        when(repository.save(stored)).thenReturn(stored);
        Transaction input = transaction(30, Type.INCOME);
        input.setUser(new User());
        input.setId(99L);
        Transaction result = service.updateTransaction(8L, input, 1L);
        assertEquals(LocalDate.of(2020, 1, 1), result.getCreatedAt());
        assertSame(owner, result.getUser());
        assertEquals(8L, result.getId());
        assertEquals(30.0, result.getAmount());
        assertEquals(Type.INCOME, result.getType());
    }

    @Test
    void rejectsUpdateAndDeletionByAnotherUser() {
        when(repository.findById(8L)).thenReturn(Optional.of(transaction(20, Type.EXPENSE)));
        User attacker = new User();
        attacker.setId(2L);
        assertEquals(
                403,
                assertThrows(
                                ResponseStatusException.class,
                                () ->
                                        service.updateTransaction(
                                                8L, transaction(10, Type.INCOME), 2L))
                        .getStatusCode()
                        .value());
        assertEquals(
                403,
                assertThrows(
                                ResponseStatusException.class,
                                () -> service.deleteTransaction(8L, attacker))
                        .getStatusCode()
                        .value());
        verify(repository, never()).save(any());
        verify(repository, never()).delete(any());
    }

    @Test
    void missingTransactionReturns404() {
        when(repository.findById(8L)).thenReturn(Optional.empty());
        assertEquals(
                404,
                assertThrows(
                                ResponseStatusException.class,
                                () ->
                                        service.updateTransaction(
                                                8L, transaction(1, Type.EXPENSE), 1L))
                        .getStatusCode()
                        .value());
        assertEquals(
                404,
                assertThrows(
                                ResponseStatusException.class,
                                () -> service.deleteTransaction(8L, owner))
                        .getStatusCode()
                        .value());
    }

    @Test
    void ownerCanDelete() {
        Transaction stored = transaction(20, Type.EXPENSE);
        when(repository.findById(8L)).thenReturn(Optional.of(stored));
        assertEquals(204, service.deleteTransaction(8L, owner).getStatusCode().value());
        verify(repository).delete(stored);
    }

    @Test
    void optionalTypeListsAllAndExplicitTypeFilters() {
        when(repository.findByUserId(1L)).thenReturn(List.of(transaction(20, Type.INCOME)));
        when(repository.findByUserIdAndType(1L, Type.EXPENSE)).thenReturn(List.of());
        assertEquals(1, service.filterTransactionsByType(1L, null).size());
        assertTrue(service.filterTransactionsByType(1L, Type.EXPENSE).isEmpty());
    }
}
