package com.tiscon.jackson;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;

public class Json2Java {
    public static int main(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Model model = mapper.readValue(json.toString(), Model.class);
            int distance = model.routes.get(0).legs.get(0).distance.value;
            return distance;
        } catch (IOException e) {
            System.out.println(e);
            return -1;
        }
    }
}