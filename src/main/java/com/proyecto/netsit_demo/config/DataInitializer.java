package com.proyecto.netsit_demo.config;

import com.proyecto.netsit_demo.modelo.Rol;
import com.proyecto.netsit_demo.modelo.Usuario;
import com.proyecto.netsit_demo.repositorio.RolRepository;
import com.proyecto.netsit_demo.repositorio.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {

        // 1. Crear roles si no existen
        Rol rolAdmin = rolRepository.findByNombre("ROLE_ADMIN")
                .orElseGet(() -> rolRepository.save(new Rol(null, "ROLE_ADMIN")));

        rolRepository.findByNombre("ROLE_USER")
        .orElseGet(() -> rolRepository.save(new Rol(null, "ROLE_USER")));

        if (usuarioRepository.findByUsername("admin").isEmpty()) {
            Usuario admin = new Usuario();
            admin.setNombre("Administrador");
            admin.setApellido("Sistema");
            admin.setEmail("admin@netsit.com");
            admin.setUsername("admin");

            // Contraseña tomada de variable de entorno; fallback solo para desarrollo local
            String defaultPassword = System.getenv("ADMIN_DEFAULT_PASSWORD");
            if (defaultPassword == null || defaultPassword.isBlank()) {
                defaultPassword = "admin123"; // fallback solo para desarrollo local
            }
            admin.setPassword(passwordEncoder.encode(defaultPassword));

            admin.setActivo(true);
            admin.setRoles(Set.of(rolAdmin));

            usuarioRepository.save(admin);
            System.out.println(">>> Usuario 'admin' creado automáticamente con éxito.");
        }
    }
}