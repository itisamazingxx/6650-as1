package com.itisamazing.servlet;

import com.google.gson.Gson;
import com.itisamazing.model.Status;
import java.io.IOException;
import java.io.PrintWriter;
import io.swagger.annotations.*;
import javax.servlet.annotation.WebServlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Api(value = "Skiers API", tags = {"skiers"})
@WebServlet("/skiers/*")
public class SkierServlet extends HttpServlet {

    @ApiOperation(value = "Write a new lift ride for the skier",
            notes = "Stores new lift ride details in the data store",
            response = Status.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Lift ride recorded successfully"),
            @ApiResponse(code = 400, message = "Invalid request parameters"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Process the request and handle URL parsing, validation, and response generation
     */
    private void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        Gson gson = new Gson();

        try {
            String path = request.getPathInfo();  // Expect: /1/seasons/2024/days/5/skiers/10
            if (path == null || path.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.write(gson.toJson(new Status(false, "Missing path parameters")));
                return;
            }

            String[] parts = path.split("/");
            if (parts.length != 8 || !"seasons".equals(parts[2]) || !"days".equals(parts[4])) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.write(gson.toJson(new Status(false, "Invalid URL format")));
                return;
            }

            // Correctly parse URL path variables
            int resortID = Integer.parseInt(parts[1]);
            String seasonID = parts[3];
            int dayID = Integer.parseInt(parts[5]);
            int skierID = Integer.parseInt(parts[7]); // Fixed parsing issue

            response.setStatus(HttpServletResponse.SC_OK);
            out.write(gson.toJson(new Status(true, "Lift ride recorded successfully for skier " + skierID)));

        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write(gson.toJson(new Status(false, "Invalid numeric format in URL")));
        } finally {
            out.flush();
            out.close();
        }
    }
}


