package com.proyecto.netsit_demo.controlador;

import com.proyecto.netsit_demo.modelo.Usuario;
import com.proyecto.netsit_demo.modelo.Rol;
import com.proyecto.netsit_demo.repositorio.UsuarioRepository;
import com.proyecto.netsit_demo.repositorio.RolRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashSet;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;

    @GetMapping
    public ResponseEntity<List<Usuario>> obtenerUsuarios() {
        return ResponseEntity.ok(usuarioRepository.findAll());
    }
    
    @GetMapping("/estado")
    public ResponseEntity<String> estado() {
        return ResponseEntity.ok("OK");
    }

    @PostMapping("/crear")
    public ResponseEntity<Usuario> crearUsuario(
            @RequestBody Map<String, Object> datos) {

        Usuario usuario = new Usuario();

        usuario.setNombre((String) datos.get("nombre"));
        usuario.setApellido((String) datos.get("apellido"));
        usuario.setEmail((String) datos.get("email"));
        usuario.setUsername((String) datos.get("username"));
        usuario.setPassword((String) datos.get("password"));

        Object activo = datos.get("activo");

        if (activo != null) {
            usuario.setActivo((Boolean) activo);
        } else {
            usuario.setActivo(true);
        }

        Object roles = datos.get("roles");

        if (roles instanceof List<?> lista && !lista.isEmpty()) {

            String nombreRol = String.valueOf(lista.get(0));

            Rol rol = rolRepository.findByNombre(nombreRol)
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "El rol no existe: " + nombreRol
                            )
                    );

            usuario.setRoles(new HashSet<>());
            usuario.getRoles().add(rol);
        }

        Usuario nuevoUsuario = usuarioRepository.save(usuario);

        return ResponseEntity.ok(nuevoUsuario);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarUsuario(
            @PathVariable Long id) {

        if (!usuarioRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        usuarioRepository.deleteById(id);

        return ResponseEntity.noContent().build();
    }
}