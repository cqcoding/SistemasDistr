package com.example.ProjetoSD;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

public class AuthFilter implements Filter {
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        HttpSession session = httpRequest.getSession(false);
        
        String path = httpRequest.getRequestURI();
        
        // URLs que não precisam de autenticação
        if (path.equals("/login") || path.equals("/login-submit") || path.equals("/")) {
            chain.doFilter(request, response);
            return;
        }
        
        // Verifica se o usuário está logado
        if (session != null) {
            User user = (User) session.getAttribute("user");
            if (user != null && user.isLoggedIn()) {
                chain.doFilter(request, response);
                return;
            }
        }
        
        // Redireciona para a página de login se não estiver autenticado
        httpResponse.sendRedirect("/login");
    }
} 