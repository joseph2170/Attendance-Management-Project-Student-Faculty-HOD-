package com.fooddelivery.fooddeliverybackend.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recommendation")
public class RecommendationController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/top")
    public List<Map<String, Object>> getTopFoods() {
        String sql = "SELECT f.food_id, f.name, SUM(oi.quantity) AS total_orders " +
                     "FROM food_items f JOIN order_items oi ON f.food_id = oi.food_id " +
                     "GROUP BY f.food_id ORDER BY total_orders DESC LIMIT 5";
        return jdbcTemplate.queryForList(sql);
    }
}
