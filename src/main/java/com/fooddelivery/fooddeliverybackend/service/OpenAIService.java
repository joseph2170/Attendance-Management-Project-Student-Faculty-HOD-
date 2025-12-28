package com.fooddelivery.fooddeliverybackend.service;

import org.springframework.stereotype.Service;

@Service
public class OpenAIService {

    // This is now a rule-based chatbot for your Food Delivery website
	public String getAIResponse(String userQuery) {

	    // ================= EMPTY / WELCOME =================
	    if (userQuery == null || userQuery.trim().isEmpty()) {
	        return """
	        🤖 Welcome to FOODIE 🍽️

	        Type:
	        🥬 Veg Menu
	        🍗 Non-Veg Menu
	        🍛 Meals
	        🛒 Order Help
	        🚚 Delivery Info
	        """;
	    }

	    String msg = userQuery.toLowerCase();

	    // ================= GREETING =================
	    if (msg.contains("hi") || msg.contains("hello") || msg.contains("hey")) {
	        return """
	        👋 Hello! Welcome to FOODIE 🍽️
	        Ask for:
	        🥬 Veg Menu
	        🍗 Non-Veg Menu
	        🍛 Biryani | 🍕 Pizza
	        """;
	    }

	    // ================= VEG MENU =================
	    if (msg.contains("veg menu")) {
	        return """
	        🥬 VEG MENU

	        🍽️ South Indian:
	        • Idli – ₹15
	        • Masala Dosa – ₹60
	        • Curd Rice – ₹50
	        • Pongal – ₹50
	        • Aloo Paratha – ₹40

	        🍛 Main Course:
	        • Paneer Butter Masala – ₹120
	        • Palak Paneer – ₹110
	        • Veg Biryani – ₹100
	        • Veg Pulao – ₹100

	        🥡 Chinese:
	        • Veg Fried Rice – ₹90
	        • Veg Noodles – ₹90
	        • Veg Manchurian – ₹90

	        🥪 Snacks:
	        • Veg Cutlet – ₹70
	        • Veg Roll – ₹70
	        • Veg Frankie – ₹90
	        • Cheese Sandwich – ₹100

	        🍕 Pizza:
	        • Margherita Pizza – ₹180
	        • Veg Pizza – ₹200
	        """;
	    }

	    // ================= NON VEG MENU =================
	    if (msg.contains("non veg menu") || msg.contains("non-veg")) {
	        return """
	        🍗 NON-VEG MENU

	        🍛 Biryani:
	        • Chicken Biryani – ₹150
	        • Mutton Biryani – ₹250

	        🍗 Chicken Items:
	        • Chicken 65 – ₹150
	        • Butter Chicken – ₹170
	        • Chicken Tikka – ₹160
	        • Chicken Lollipop – ₹140
	        • Grilled Chicken – ₹180

	        🥡 Chinese:
	        • Chicken Fried Rice – ₹130
	        • Chicken Noodles – ₹120
	        • Egg Fried Rice – ₹90

	        🥙 Rolls:
	        • Chicken Roll – ₹140
	        • Chicken Frankie – ₹130
	        • Chicken Shawarma – ₹120

	        🐟 Sea Food:
	        • Fish Fry – ₹180
	        • Fish Curry – ₹160
	        • Prawn Fry – ₹230
	        """;
	    }

	    // ================= MEALS =================
	    if (msg.contains("meals")) {
	        return "🍛 South Indian Meals – ₹90 (Rice, Sambar, Rasam, Curry, Poriyal)";
	    }

	    // ================= PIZZA =================
	    if (msg.contains("pizza")) {
	        return """
	        🍕 Pizza Menu:
	        • Margherita Pizza – ₹180
	        • Veg Pizza – ₹200
	        • Chicken Pizza – ₹220
	        """;
	    }

	    // ================= BIRYANI =================
	    if (msg.contains("biryani")) {
	        return """
	        🍛 Biryani Prices:
	        • Veg – ₹100
	        • Chicken – ₹150
	        • Mutton – ₹250
	        """;
	    }

	    // ================= PAYMENT =================
	    if (msg.contains("payment")) {
	        return "💳 Payment Methods: UPI, Cash on Delivery, Debit & Credit Cards";
	    }

	    // ================= DELIVERY =================
	    if (msg.contains("delivery") || msg.contains("time")) {
	        return "🚚 Delivery within 20–30 minutes inside city limits.";
	    }

	    // ================= ORDER =================
	    if (msg.contains("order")) {
	        return """
	        🛒 How to Order:
	        1️⃣ Select food
	        2️⃣ Add to cart
	        3️⃣ Checkout
	        4️⃣ Enjoy 😋
	        """;
	    }

	    // ================= CANCEL =================
	    if (msg.contains("cancel")) {
	        return "❌ Orders can be cancelled within 3 minutes after placing.";
	    }

	    // ================= THANK YOU =================
	    if (msg.contains("thank")) {
	        return "😊 You're welcome! Happy eating 😋";
	    }

	    // ================= GOODBYE =================
	    if (msg.contains("bye")) {
	        return "👋 Thank you for choosing FOODIE! Visit again 😊";
	    }

	    // ================= DEFAULT =================
	    return """
	    🤖 I can help with:
	    🥬 Veg Menu
	    🍗 Non-Veg Menu
	    🍛 Biryani | 🍕 Pizza
	    🚚 Delivery | 💳 Payment
	    """;
	}

}
