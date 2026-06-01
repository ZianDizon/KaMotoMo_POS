package com.kamotomo.pos.utils;

import com.kamotomo.pos.models.CartItem;
import java.util.ArrayList;
import java.util.List;

public class CartSession {
    private static CartSession instance;
    private List<CartItem> cart;

    private CartSession() {
        this.cart = new ArrayList<>();
    }

    public static CartSession getInstance() {
        if (instance == null) {
            instance = new CartSession();
        }
        return instance;
    }

    public List<CartItem> getCart() {
        return cart;
    }

    public void clearCart() {
        cart.clear();
    }
}