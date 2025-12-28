package com.fooddelivery.fooddeliverybackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.fooddelivery.fooddeliverybackend.model.Food;
import org.springframework.stereotype.Repository;

@Repository
public interface FoodRepository extends JpaRepository<Food, Integer> {
    // JpaRepository already provides:
    // - findAll()
    // - save()
    // - findById()
    // - deleteById()
}
