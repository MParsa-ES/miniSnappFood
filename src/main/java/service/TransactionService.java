package service;

import dao.*;
import dto.MessageDto;
import dto.TransactionDTO;
import entity.*;
import service.exception.AdminServiceExceptions;
import service.exception.OrderServiceExceptions;
import service.exception.UserNotFoundException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TransactionService {

    private final UserDAO userDAO;
    private final RestaurantDAO restaurantDAO;
    private final FoodItemDAO foodItemDAO;
    private final OrderDAO orderDAO;
    private final RatingDAO ratingDAO;
    private final TransactionDAO transactionDAO;

    public TransactionService(UserDAO userDAO, RestaurantDAO restaurantDAO, FoodItemDAO foodItemDAO, OrderDAO orderDAO, RatingDAO ratingDAO, TransactionDAO transactionDAO) {
        this.userDAO = userDAO;
        this.restaurantDAO = restaurantDAO;
        this.foodItemDAO = foodItemDAO;
        this.orderDAO = orderDAO;
        this.ratingDAO = ratingDAO;
        this.transactionDAO = transactionDAO;
    }

    public TransactionDTO.PaymentResponseDTO payment(TransactionDTO.PaymentRequestDTO request, String phone) throws OrderServiceExceptions.NotEnoughBalance {

        User user = userDAO.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Order order = orderDAO.findOrderById(request.getOrder_id())
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getCustomer().equals(user)) {
            throw new OrderServiceExceptions.UserIsNotOwnerOfOrder("User is not owner of the order");
        }

        if (transactionDAO.hasSuccessfulTransaction(request.getOrder_id())) {
            throw new OrderServiceExceptions.InvalidOrderState("A successful payment for this order already exists");
        }

        OrderStatus status = order.getStatus();
        switch (status) {
            case WAITING_VENDOR:
            case FINDING_COURIER:
            case ON_THE_WAY:
            case COMPLETED:
                throw new OrderServiceExceptions.InvalidOrderState("This order has already been processed and cannot be paid again");

            case CANCELLED:
            case UNPAID_AND_CANCELLED:
                throw new OrderServiceExceptions.InvalidOrderState("This order has been cancelled and cannot be paid");
        }


        try {
            if (request.getMethod().equals(TransactionMethod.WALLET)) {
                if (user.getWalletBalance().compareTo(order.getTotalPrice()) < 0) {
                    throw new OrderServiceExceptions.NotEnoughBalance("Not enough balance");
                }

                user.setWalletBalance(user.getWalletBalance().subtract(order.getTotalPrice()));
                userDAO.update(user);
            }

            order.setStatus(OrderStatus.WAITING_VENDOR);
            orderDAO.update(order);

            Transaction transaction = new Transaction(user, order, request.getMethod(), TransactionStatus.SUCCESS);
            transactionDAO.save(transaction);

            return new TransactionDTO.PaymentResponseDTO(
                    transaction.getId(),
                    order.getId(),
                    user.getId(),
                    transaction.getMethod().toString(),
                    transaction.getStatus().toString()
            );
        } catch (Exception paymentError) {

            Transaction failedTransaction = new Transaction(user, order, request.getMethod(), TransactionStatus.FAILED);
            transactionDAO.save(failedTransaction);

            order.setStatus(OrderStatus.UNPAID_AND_CANCELLED);
            orderDAO.update(order);

            throw new RuntimeException(paymentError.getMessage(), paymentError);
        }

    }

    public BigDecimal getWalletBalance(String phone) {

        User user = userDAO.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return user.getWalletBalance();

    }

    public MessageDto TopUpWallet(BigDecimal amount, String phone) {

        User user = userDAO.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("User not found"));

        BigDecimal walletBalance = user.getWalletBalance();
        walletBalance = walletBalance.add(amount);
        user.setWalletBalance(walletBalance);
        userDAO.update(user);

        return new MessageDto("Wallet topped up successfully");

    }

    public TransactionDTO.TransactionsList getUserTransactions(String phone) {

        User user = userDAO.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Transaction> transactions = transactionDAO.getUserTransactions(user.getId());

        Set<TransactionDTO.PaymentResponseDTO> response = new HashSet<>();

        for (Transaction transaction : transactions) {

            response.add(new TransactionDTO.PaymentResponseDTO(
                    transaction.getId(),
                    transaction.getOrder().getId(),
                    user.getId(),
                    transaction.getMethod().toString(),
                    transaction.getStatus().toString()
            ));

        }

        return new TransactionDTO.TransactionsList(response);

    }

    public TransactionDTO.TransactionsList searchTransactions(String adminUsername, String search, String user, String method, String status) {

        User admin = userDAO.findByPhone(adminUsername).orElseThrow(
                () -> new UserNotFoundException("User not found")
        );

        if (!admin.getRole().equals(Role.ADMIN)) {
            throw new AdminServiceExceptions.UserNotAdminException("You are not admin");
        }

        List<Transaction> transactions = transactionDAO.searchTransactions(search, user, method, status);

        Set<TransactionDTO.PaymentResponseDTO> response = new HashSet<>();

        for (Transaction transaction : transactions) {
            response.add(new TransactionDTO.PaymentResponseDTO(
                    transaction.getId(),
                    transaction.getOrder().getId(),
                    transaction.getUser().getId(),
                    transaction.getMethod().toString(),
                    transaction.getStatus().toString()
            ));
        }

        return new TransactionDTO.TransactionsList(response);

    }

}
